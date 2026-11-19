import React, { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import {
  Search,
  PackageOpen,
  X,
  Sparkles,
  AlertCircle,
  ListFilter,
  Filter,
  ChevronDown,
  ChevronRight,
  ChevronLeft,
  Star,
  RotateCcw,
  Check,
  SlidersHorizontal,
} from 'lucide-react'
import { motion, useReducedMotion } from 'framer-motion'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ProductCard from '../../components/common/ProductCard'
import Pagination from '../../components/common/Pagination'
import { useCartStore } from '../../store/useCartStore'
import { useToastStore } from '../../store/useToastStore'

/**
 * Skeleton card hiển thị trạng thái đang tải (Loading State).
 */
const ProductCardSkeleton = () => (
  <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden flex flex-col justify-between animate-pulse">
    <div>
      <div className="aspect-square w-full bg-slate-100" />
      <div className="p-4 sm:p-5 space-y-3">
        <div className="h-3 bg-slate-200 rounded-full w-1/3" />
        <div className="h-4 bg-slate-200 rounded-full w-4/5" />
        <div className="h-3 bg-slate-100 rounded-full w-1/2" />
      </div>
    </div>
    <div className="p-4 sm:p-5 pt-0">
      <div className="h-9 bg-slate-100 rounded-full w-full" />
    </div>
  </div>
)

/**
 * Trang Danh sách Sản phẩm (Shop / ProductListPage).
 * Tích hợp bộ lọc đa tiêu chí chuẩn E-Commerce (All Categories + Search Filter bên trái, Sort bar bên trên).
 */
export const ProductListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const addToCart = useCartStore((state) => state.addToCart)
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()

  // Dữ liệu sản phẩm & danh mục 100% từ API thật
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Pagination state (đồng bộ với URL searchParams)
  const [page, setPage] = useState(parseInt(searchParams.get('page') || '0', 10))
  const [pageSize, setPageSize] = useState(parseInt(searchParams.get('size') || '12', 10))
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Filter & Search state
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [searchInput, setSearchInput] = useState(searchParams.get('keyword') || '')
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '')
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'ratingAverage')
  const [sortDir, setSortDir] = useState(searchParams.get('sortDir') || 'desc')

  // Bộ lọc khoảng giá, đánh giá và tồn kho
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') || '')
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') || '')
  const [minPriceInput, setMinPriceInput] = useState(searchParams.get('minPrice') || '')
  const [maxPriceInput, setMaxPriceInput] = useState(searchParams.get('maxPrice') || '')
  const [minRating, setMinRating] = useState(searchParams.get('minRating') || '')
  const [inStockOnly, setInStockOnly] = useState(searchParams.get('inStock') === 'true')

  // UI state
  const [isPriceDropdownOpen, setIsPriceDropdownOpen] = useState(false)
  const [showMobileFilters, setShowMobileFilters] = useState(false)
  const priceDropdownRef = useRef(null)
  const shouldReduceMotion = useReducedMotion()

  // Đóng dropdown giá khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (priceDropdownRef.current && !priceDropdownRef.current.contains(event.target)) {
        setIsPriceDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const listContainerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: shouldReduceMotion ? 0 : 0.04,
      },
    },
  }

  const listItemVariants = {
    hidden: { opacity: 0, y: shouldReduceMotion ? 0 : 12 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.3, ease: 'easeOut' },
    },
  }

  // Hiển thị Toast Notification
  const showToast = (message, type = 'success') => {
    useToastStore.getState().showToast({
      title: type === 'error' ? 'Có lỗi xảy ra' : 'Đã thêm vào giỏ hàng!',
      message,
      type,
      duration: 2000,
      link: type === 'success' ? { to: '/cart', label: 'Xem giỏ hàng →' } : null,
    })
  }

  // Xử lý thêm vào giỏ hàng (hỗ trợ đầy đủ biến thể kích cỡ)
  const handleAddToCart = async (product, quantity = 1, selectedSize = null, variant = null) => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: '/products' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }

    try {
      const productPayload = {
        ...product,
        variants: (Array.isArray(product.variants) && product.variants.length > 0)
          ? product.variants
          : (variant ? [variant] : []),
        variantId: variant?.id || product.variantId || null,
      }
      const res = await addToCart(productPayload, quantity, selectedSize)
      if (res?.requireLogin) {
        navigate('/login', {
          state: {
            from: { pathname: '/products' },
            message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
          },
        })
        return
      }
      if (res && res.success === false) {
        showToast(res.message || 'Sản phẩm đã hết hàng', 'error')
      }
    } catch (err) {
      console.error('Lỗi khi thêm giỏ hàng:', err)
      showToast('Có lỗi xảy ra khi thêm vào giỏ hàng', 'error')
    }
  }

  // Tải danh mục thực tế từ backend
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await categoryService.getCategories()
        if (res && res.success && res.data) {
          setCategories(res.data)
        } else if (Array.isArray(res)) {
          setCategories(res)
        } else {
          setCategories([])
        }
      } catch (err) {
        console.error('Không thể tải danh mục:', err)
        setCategories([])
      }
    }
    fetchCategories()
  }, [])

  // Tải danh sách sản phẩm từ backend API (100% dữ liệu thực)
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        page,
        size: pageSize,
        sortBy,
        sortDir,
      }
      if (keyword.trim()) params.keyword = keyword.trim()
      if (selectedCategory) params.categoryId = selectedCategory
      if (minPrice !== '' && !isNaN(minPrice)) params.minPrice = minPrice
      if (maxPrice !== '' && !isNaN(maxPrice)) params.maxPrice = maxPrice

      const res = await productService.getProducts(params)
      if (res && res.success && res.data) {
        setProducts(res.data.content || [])
        setTotalPages(res.data.totalPages || 0)
        setTotalElements(res.data.totalElements || 0)
      } else if (res && res.content) {
        setProducts(res.content || [])
        setTotalPages(res.totalPages || 0)
        setTotalElements(res.totalElements || 0)
      } else {
        setProducts([])
        setTotalPages(0)
        setTotalElements(0)
      }
    } catch (err) {
      console.error('Lỗi khi tải sản phẩm:', err)
      setError(
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách sản phẩm từ máy chủ. Vui lòng thử lại.'
      )
      setProducts([])
      setTotalPages(0)
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [keyword, selectedCategory, minPrice, maxPrice, page, pageSize, sortBy, sortDir])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Đồng bộ trạng thái bộ lọc & phân trang vào URL search params
  const updateUrl = (newKeyword, newCatId, newPage, newSize, newSortBy, newSortDir, newMinPrice, newMaxPrice, newMinRating, newInStock) => {
    const p = {}
    if (newKeyword) p.keyword = newKeyword
    if (newCatId) p.categoryId = newCatId
    if (newPage > 0) p.page = newPage.toString()
    if (newSize !== 12) p.size = newSize.toString()
    if (newSortBy && newSortBy !== 'createdAt') p.sortBy = newSortBy
    if (newSortDir && newSortDir !== 'desc') p.sortDir = newSortDir
    if (newMinPrice !== '' && newMinPrice != null) p.minPrice = newMinPrice.toString()
    if (newMaxPrice !== '' && newMaxPrice != null) p.maxPrice = newMaxPrice.toString()
    if (newMinRating) p.minRating = newMinRating.toString()
    if (newInStock) p.inStock = 'true'
    setSearchParams(p)

    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    if (document.documentElement) document.documentElement.scrollTop = 0
    if (document.body) document.body.scrollTop = 0
  }

  // Đồng bộ URL searchParams khi người dùng tương tác chuyển link
  useEffect(() => {
    const urlCatId = searchParams.get('categoryId') || ''
    const urlKeyword = searchParams.get('keyword') || ''
    const urlPage = parseInt(searchParams.get('page') || '0', 10)
    const urlSize = parseInt(searchParams.get('size') || '12', 10)
    const urlSortBy = searchParams.get('sortBy') || 'ratingAverage'
    const urlSortDir = searchParams.get('sortDir') || 'desc'
    const urlMinPrice = searchParams.get('minPrice') || ''
    const urlMaxPrice = searchParams.get('maxPrice') || ''
    const urlMinRating = searchParams.get('minRating') || ''
    const urlInStock = searchParams.get('inStock') === 'true'

    setSelectedCategory((prev) => (prev !== urlCatId ? urlCatId : prev))
    setKeyword((prev) => (prev !== urlKeyword ? urlKeyword : prev))
    setSearchInput((prev) => (prev !== urlKeyword ? urlKeyword : prev))
    setPage((prev) => (prev !== urlPage ? urlPage : prev))
    setPageSize((prev) => (prev !== urlSize ? urlSize : prev))
    setSortBy((prev) => (prev !== urlSortBy ? urlSortBy : prev))
    setSortDir((prev) => (prev !== urlSortDir ? urlSortDir : prev))
    setMinPrice((prev) => (prev !== urlMinPrice ? urlMinPrice : prev))
    setMaxPrice((prev) => (prev !== urlMaxPrice ? urlMaxPrice : prev))
    setMinPriceInput((prev) => (prev !== urlMinPrice ? urlMinPrice : prev))
    setMaxPriceInput((prev) => (prev !== urlMaxPrice ? urlMaxPrice : prev))
    setMinRating((prev) => (prev !== urlMinRating ? urlMinRating : prev))
    setInStockOnly((prev) => (prev !== urlInStock ? urlInStock : prev))
  }, [searchParams])

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setKeyword(searchInput)
    setPage(0)
    updateUrl(searchInput, selectedCategory, 0, pageSize, sortBy, sortDir, minPrice, maxPrice, minRating, inStockOnly)
  }

  const handleCategorySelect = (catId) => {
    const updated = catId === selectedCategory ? '' : catId
    setSelectedCategory(updated)
    setPage(0)
    updateUrl(keyword, updated, 0, pageSize, sortBy, sortDir, minPrice, maxPrice, minRating, inStockOnly)
  }

  // Xác định tab sắp xếp đang kích hoạt
  const getActiveSortTab = () => {
    if (sortBy === 'price') {
      return sortDir === 'asc' ? 'price_asc' : 'price_desc'
    }
    if (sortBy === 'totalReviews') return 'top_sales'
    if (sortBy === 'createdAt') return 'latest'
    return 'popular' // mặc định Phổ biến (ratingAverage)
  }

  // Chọn tab sắp xếp (Popular, Latest, Top Sales, Price Asc/Desc)
  const handleSelectSortTab = (tabKey) => {
    let newSortBy = 'ratingAverage'
    let newSortDir = 'desc'

    if (tabKey === 'popular') {
      newSortBy = 'ratingAverage'
      newSortDir = 'desc'
    } else if (tabKey === 'latest') {
      newSortBy = 'createdAt'
      newSortDir = 'desc'
    } else if (tabKey === 'top_sales') {
      newSortBy = 'totalReviews'
      newSortDir = 'desc'
    } else if (tabKey === 'price_asc') {
      newSortBy = 'price'
      newSortDir = 'asc'
    } else if (tabKey === 'price_desc') {
      newSortBy = 'price'
      newSortDir = 'desc'
    }

    setSortBy(newSortBy)
    setSortDir(newSortDir)
    setPage(0)
    setIsPriceDropdownOpen(false)
    updateUrl(keyword, selectedCategory, 0, pageSize, newSortBy, newSortDir, minPrice, maxPrice, minRating, inStockOnly)
  }

  // Áp dụng bộ lọc khoảng giá
  const handleApplyPriceFilter = (e) => {
    e?.preventDefault()
    const cleanMin = minPriceInput ? minPriceInput.toString().replace(/\D/g, '') : ''
    const cleanMax = maxPriceInput ? maxPriceInput.toString().replace(/\D/g, '') : ''

    setMinPrice(cleanMin)
    setMaxPrice(cleanMax)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, pageSize, sortBy, sortDir, cleanMin, cleanMax, minRating, inStockOnly)
  }

  // Lọc theo đánh giá sao
  const handleRatingSelect = (starVal) => {
    const updated = minRating === starVal.toString() ? '' : starVal.toString()
    setMinRating(updated)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, pageSize, sortBy, sortDir, minPrice, maxPrice, updated, inStockOnly)
  }

  // Lọc sản phẩm còn hàng
  const handleToggleInStock = () => {
    const updated = !inStockOnly
    setInStockOnly(updated)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, pageSize, sortBy, sortDir, minPrice, maxPrice, minRating, updated)
  }

  // Đổi trang
  const handlePageChange = (newPage) => {
    if (newPage < 0 || (totalPages > 0 && newPage >= totalPages)) return
    setPage(newPage)
    updateUrl(keyword, selectedCategory, newPage, pageSize, sortBy, sortDir, minPrice, maxPrice, minRating, inStockOnly)
  }

  // Đổi kích thước trang (Page Size)
  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, newSize, sortBy, sortDir, minPrice, maxPrice, minRating, inStockOnly)
  }

  // Xóa toàn bộ bộ lọc
  const handleClearFilters = () => {
    setKeyword('')
    setSearchInput('')
    setSelectedCategory('')
    setMinPrice('')
    setMaxPrice('')
    setMinPriceInput('')
    setMaxPriceInput('')
    setMinRating('')
    setInStockOnly(false)
    setPage(0)
    setPageSize(12)
    setSortBy('ratingAverage')
    setSortDir('desc')
    setSearchParams({})
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
    if (document.documentElement) document.documentElement.scrollTop = 0
    if (document.body) document.body.scrollTop = 0
  }

  // Lọc bổ sung phía client cho minRating & inStock (nếu có chọn)
  let displayedProducts = products
  if (minRating) {
    const ratingThreshold = parseFloat(minRating)
    displayedProducts = displayedProducts.filter((p) => Number(p.ratingAverage || 0) >= ratingThreshold)
  }
  if (inStockOnly) {
    displayedProducts = displayedProducts.filter((p) => Number(p.stockQuantity || 0) > 0)
  }

  const activeSort = getActiveSortTab()
  const activeCategoryObj = categories.find((c) => c.id.toString() === selectedCategory)
  const isAnyFilterActive = Boolean(selectedCategory || keyword || minPrice || maxPrice || minRating || inStockOnly)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full relative animate-in fade-in duration-200">
      {/* Page Header & Search Bar */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4 pb-5 border-b border-slate-200/90">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-red-50 border border-red-200/70 text-red-600 text-xs font-bold uppercase tracking-wider mb-1.5">
            <Sparkles className="w-3.5 h-3.5" />
            <span>RIKKEI STORE GEAR</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            {activeCategoryObj ? activeCategoryObj.name : 'Khám phá sản phẩm'}
          </h1>
          <p className="text-slate-500 text-xs sm:text-sm mt-0.5">
            {loading ? (
              'Đang tải danh sách sản phẩm...'
            ) : (
              <span>
                Tìm thấy <span className="font-bold text-slate-900">{totalElements}</span> sản phẩm chính hãng
                {keyword && (
                  <span>
                    {' '}cho từ khóa "<span className="text-red-500 font-bold">{keyword}</span>"
                  </span>
                )}
              </span>
            )}
          </p>
        </div>

        {/* Thanh tìm kiếm */}
        <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="Tìm sản phẩm theo tên..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="flex h-10 w-full rounded-full border border-slate-200 bg-slate-50 hover:bg-white pl-4 pr-10 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-red-500 focus:bg-white focus:ring-2 focus:ring-red-500/20"
            />
            {searchInput ? (
              <button
                type="button"
                onClick={() => setSearchInput('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 cursor-pointer"
                title="Xóa từ khóa"
              >
                <X className="w-4 h-4" />
              </button>
            ) : (
              <Search className="w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            )}
          </div>
          <button
            type="submit"
            className="inline-flex items-center justify-center h-10 px-5 rounded-full bg-red-500 hover:bg-red-600 text-white text-xs font-bold transition-all shadow-xs hover:shadow-md cursor-pointer shrink-0"
          >
            Tìm kiếm
          </button>
        </form>
      </div>

      {/* Nút bật bộ lọc trên Mobile */}
      <div className="lg:hidden mb-4 flex items-center justify-between">
        <button
          onClick={() => setShowMobileFilters(!showMobileFilters)}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white border border-slate-200 text-xs font-bold text-slate-700 shadow-xs hover:bg-slate-50 cursor-pointer"
        >
          <SlidersHorizontal className="w-4 h-4 text-red-500" />
          <span>Bộ lọc & Danh mục</span>
          {isAnyFilterActive && (
            <span className="w-2 h-2 rounded-full bg-red-500" />
          )}
        </button>

        {isAnyFilterActive && (
          <button
            onClick={handleClearFilters}
            className="text-xs text-red-500 font-semibold hover:underline"
          >
            Xóa tất cả bộ lọc
          </button>
        )}
      </div>

      {/* Layout 2 cột: Cột trái (All Categories + Search Filter), Cột phải (Sort Toolbar + Product Grid) */}
      <div className="grid grid-cols-1 lg:grid-cols-4 xl:grid-cols-5 gap-6 lg:gap-8 items-start">
        
        {/* ========================================================================= */}
        {/* CỘT TRÁI: SIDEBAR BỘ LỌC (CHUẨN SHOPEE) */}
        {/* ========================================================================= */}
        <aside
          className={`space-y-6 ${
            showMobileFilters ? 'block mb-6' : 'hidden lg:block'
          }`}
        >
          {/* PHẦN 1: TẤT CẢ DANH MỤC (ALL CATEGORIES) */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-xs">
            <h3 className="flex items-center gap-2 font-black text-slate-900 text-sm uppercase tracking-wide pb-3.5 border-b border-slate-100">
              <ListFilter className="w-4 h-4 text-slate-700" />
              <span>Tất cả danh mục</span>
            </h3>

            <div className="mt-3 space-y-1 text-sm">
              <button
                type="button"
                onClick={() => handleCategorySelect('')}
                className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left transition-colors cursor-pointer ${
                  selectedCategory === ''
                    ? 'font-bold text-red-600 bg-red-50/70'
                    : 'text-slate-700 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span className="flex items-center gap-1.5">
                  {selectedCategory === '' && <span className="text-red-500 text-xs">▶</span>}
                  <span>Tất cả sản phẩm</span>
                </span>
                <span className="text-xs text-slate-400 font-normal">({totalElements})</span>
              </button>

              {categories.map((cat) => {
                const isSelected = selectedCategory === cat.id.toString()
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => handleCategorySelect(cat.id.toString())}
                    className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left transition-colors cursor-pointer ${
                      isSelected
                        ? 'font-bold text-red-600 bg-red-50/70'
                        : 'text-slate-700 hover:text-slate-900 hover:bg-slate-50'
                    }`}
                  >
                    <span className="flex items-center gap-1.5 truncate pr-2">
                      {isSelected && <span className="text-red-500 text-xs">▶</span>}
                      <span className="truncate">{cat.name}</span>
                    </span>
                    <ChevronRight className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-red-500' : 'text-slate-300'}`} />
                  </button>
                )
              })}
            </div>
          </div>

          {/* PHẦN 2: BỘ LỌC TÌM KIẾM (SEARCH FILTER) */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-xs space-y-5">
            <div className="flex items-center justify-between pb-3.5 border-b border-slate-100">
              <h3 className="flex items-center gap-2 font-black text-slate-900 text-sm uppercase tracking-wide">
                <Filter className="w-4 h-4 text-slate-700" />
                <span>Bộ lọc tìm kiếm</span>
              </h3>
              {isAnyFilterActive && (
                <button
                  type="button"
                  onClick={handleClearFilters}
                  className="text-xs text-red-500 hover:text-red-700 font-semibold cursor-pointer flex items-center gap-1"
                  title="Xóa tất cả bộ lọc"
                >
                  <RotateCcw className="w-3 h-3" />
                  <span>Xóa</span>
                </button>
              )}
            </div>

            {/* Khoảng giá (Price Range) */}
            <div>
              <p className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2.5">
                Khoảng giá (VNĐ)
              </p>
              <form onSubmit={handleApplyPriceFilter} className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <input
                      type="number"
                      min="0"
                      placeholder="₫ TỪ"
                      value={minPriceInput}
                      onChange={(e) => setMinPriceInput(e.target.value)}
                      className="w-full text-xs h-9 px-2.5 rounded-lg border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:border-red-500 transition-colors"
                    />
                  </div>
                  <span className="text-slate-400 font-bold">-</span>
                  <div className="relative flex-1">
                    <input
                      type="number"
                      min="0"
                      placeholder="₫ ĐẾN"
                      value={maxPriceInput}
                      onChange={(e) => setMaxPriceInput(e.target.value)}
                      className="w-full text-xs h-9 px-2.5 rounded-lg border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:border-red-500 transition-colors"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full py-2 bg-red-500 hover:bg-red-600 text-white font-bold text-xs uppercase tracking-wider rounded-lg transition-colors shadow-xs cursor-pointer"
                >
                  Áp dụng
                </button>
              </form>
            </div>

            {/* Lọc theo Đánh giá sao (Rating) */}
            <div className="pt-3 border-t border-slate-100">
              <p className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2.5">
                Đánh giá
              </p>
              <div className="space-y-1.5">
                {[5, 4, 3].map((starCount) => {
                  const isChecked = minRating === starCount.toString()
                  return (
                    <button
                      key={starCount}
                      type="button"
                      onClick={() => handleRatingSelect(starCount)}
                      className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-xs cursor-pointer transition-colors ${
                        isChecked
                          ? 'bg-amber-50 font-bold text-slate-900 border border-amber-200/80'
                          : 'hover:bg-slate-50 text-slate-700'
                      }`}
                    >
                      <div className="flex items-center gap-1 text-amber-400">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            className={`w-3.5 h-3.5 ${
                              i < starCount
                                ? 'fill-amber-400 text-amber-400'
                                : 'fill-slate-100 text-slate-300'
                            }`}
                          />
                        ))}
                        {starCount < 5 && (
                          <span className="text-slate-600 text-[11px] font-normal ml-1">
                            trở lên
                          </span>
                        )}
                      </div>
                      {isChecked && <Check className="w-3.5 h-3.5 text-red-500 font-bold" />}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Tình trạng hàng (In Stock) */}
            <div className="pt-3 border-t border-slate-100">
              <p className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2.5">
                Tình trạng
              </p>
              <label className="flex items-center gap-2.5 text-xs text-slate-700 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={inStockOnly}
                  onChange={handleToggleInStock}
                  className="w-4 h-4 rounded border-slate-300 text-red-600 focus:ring-red-500 cursor-pointer"
                />
                <span>Chỉ hiện sản phẩm còn hàng</span>
              </label>
            </div>

            {/* Nút Xóa tất cả bộ lọc */}
            {isAnyFilterActive && (
              <div className="pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={handleClearFilters}
                  className="w-full py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Xóa tất cả bộ lọc</span>
                </button>
              </div>
            )}
          </div>
        </aside>

        {/* ========================================================================= */}
        {/* CỘT PHẢI: THANH SẮP XẾP (SORT BAR) & LƯỚI SẢN PHẨM */}
        {/* ========================================================================= */}
        <div className="lg:col-span-3 xl:col-span-4 space-y-6">

          {/* THANH SẮP XẾP CHUẨN SHOPEE (SORT TOOLBAR) */}
          <div className="bg-slate-100/90 rounded-xl p-3 sm:p-3.5 flex flex-wrap items-center justify-between gap-3 border border-slate-200/70 shadow-xs">
            {/* Nhóm nút bên trái: Sắp xếp theo: Phổ biến | Mới nhất | Bán chạy | Giá ∨ */}
            <div className="flex flex-wrap items-center gap-2 text-xs sm:text-sm">
              <span className="text-slate-600 font-medium mr-1.5 shrink-0 hidden sm:inline">
                Sắp xếp theo
              </span>

              {/* Nút Phổ biến (Popular) */}
              <button
                type="button"
                onClick={() => handleSelectSortTab('popular')}
                className={`px-4 py-2 rounded-lg font-bold transition-all cursor-pointer shadow-xs ${
                  activeSort === 'popular'
                    ? 'bg-red-500 hover:bg-red-600 text-white shadow-red-500/20'
                    : 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200/80'
                }`}
              >
                Phổ biến
              </button>

              {/* Nút Mới nhất (Latest) */}
              <button
                type="button"
                onClick={() => handleSelectSortTab('latest')}
                className={`px-4 py-2 rounded-lg font-bold transition-all cursor-pointer shadow-xs ${
                  activeSort === 'latest'
                    ? 'bg-red-500 hover:bg-red-600 text-white shadow-red-500/20'
                    : 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200/80'
                }`}
              >
                Mới nhất
              </button>

              {/* Nút Bán chạy (Top Sales) */}
              <button
                type="button"
                onClick={() => handleSelectSortTab('top_sales')}
                className={`px-4 py-2 rounded-lg font-bold transition-all cursor-pointer shadow-xs ${
                  activeSort === 'top_sales'
                    ? 'bg-red-500 hover:bg-red-600 text-white shadow-red-500/20'
                    : 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200/80'
                }`}
              >
                Bán chạy
              </button>

              {/* Dropdown Giá (Price Dropdown) */}
              <div className="relative" ref={priceDropdownRef}>
                <button
                  type="button"
                  onClick={() => setIsPriceDropdownOpen(!isPriceDropdownOpen)}
                  className={`px-3.5 py-2 rounded-lg font-medium transition-all cursor-pointer flex items-center justify-between gap-3 min-w-[155px] shadow-xs ${
                    activeSort.startsWith('price')
                      ? 'bg-red-50 text-red-600 border border-red-300 font-bold'
                      : 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200/80'
                  }`}
                >
                  <span>
                    {activeSort === 'price_asc'
                      ? 'Giá: Thấp đến Cao'
                      : activeSort === 'price_desc'
                      ? 'Giá: Cao đến Thấp'
                      : 'Giá'}
                  </span>
                  <ChevronDown className={`w-4 h-4 transition-transform ${isPriceDropdownOpen ? 'rotate-180' : ''}`} />
                </button>

                {isPriceDropdownOpen && (
                  <div className="absolute left-0 mt-1.5 w-48 bg-white border border-slate-200 rounded-xl shadow-lg z-30 py-1 overflow-hidden animate-in fade-in duration-150">
                    <button
                      type="button"
                      onClick={() => handleSelectSortTab('price_asc')}
                      className={`w-full px-4 py-2.5 text-left text-xs sm:text-sm hover:bg-slate-50 transition-colors flex items-center justify-between cursor-pointer ${
                        activeSort === 'price_asc' ? 'text-red-600 font-bold bg-red-50/50' : 'text-slate-700'
                      }`}
                    >
                      <span>Giá: Từ thấp đến cao</span>
                      {activeSort === 'price_asc' && <Check className="w-3.5 h-3.5 text-red-600" />}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSelectSortTab('price_desc')}
                      className={`w-full px-4 py-2.5 text-left text-xs sm:text-sm hover:bg-slate-50 transition-colors flex items-center justify-between cursor-pointer ${
                        activeSort === 'price_desc' ? 'text-red-600 font-bold bg-red-50/50' : 'text-slate-700'
                      }`}
                    >
                      <span>Giá: Từ cao đến thấp</span>
                      {activeSort === 'price_desc' && <Check className="w-3.5 h-3.5 text-red-600" />}
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Nhóm điều khiển phân trang nhanh bên phải: 1/4 < > */}
            <div className="flex items-center gap-3 text-xs font-semibold ml-auto">
              <div className="text-slate-600">
                <span className="text-red-500 font-black text-sm">{page + 1}</span>
                <span className="text-slate-400">/{totalPages > 0 ? totalPages : 1}</span>
              </div>
              <div className="flex items-center rounded-lg border border-slate-200 bg-white overflow-hidden shadow-xs">
                <button
                  type="button"
                  onClick={() => handlePageChange(page - 1)}
                  disabled={page <= 0}
                  className="w-8 h-8 flex items-center justify-center text-slate-600 hover:bg-slate-100 disabled:opacity-30 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors cursor-pointer border-r border-slate-200"
                  title="Trang trước"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => handlePageChange(page + 1)}
                  disabled={totalPages === 0 || page >= totalPages - 1}
                  className="w-8 h-8 flex items-center justify-center text-slate-600 hover:bg-slate-100 disabled:opacity-30 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors cursor-pointer"
                  title="Trang sau"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* KHU VỰC LƯỚI SẢN PHẨM */}
          {loading ? (
            /* Loading State */
            <div className="space-y-6">
              <div className="flex items-center justify-center gap-2.5 py-4 text-xs font-medium text-slate-600 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
                <LoadingSpinner size="sm" className="text-red-500" />
                <span>Đang tải danh sách sản phẩm từ hệ thống...</span>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5">
                {Array.from({ length: pageSize > 12 ? 12 : pageSize }).map((_, idx) => (
                  <ProductCardSkeleton key={idx} />
                ))}
              </div>
            </div>
          ) : error ? (
            /* Error State */
            <div className="bg-red-50 border border-red-200 text-red-700 p-8 rounded-3xl text-center text-sm my-6">
              <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-3">
                <AlertCircle className="w-6 h-6" />
              </div>
              <p className="font-bold">{error}</p>
              <button
                type="button"
                onClick={fetchProducts}
                className="mt-4 px-5 py-2.5 bg-white border border-red-200 text-red-700 text-xs font-bold rounded-full hover:bg-red-50 transition-colors cursor-pointer shadow-xs inline-flex items-center gap-1.5"
              >
                <span>Thử tải lại</span>
              </button>
            </div>
          ) : displayedProducts.length === 0 ? (
            /* Empty State */
            <div className="text-center py-20 bg-white rounded-3xl border border-dashed border-slate-200 shadow-xs my-4">
              <div className="w-14 h-14 rounded-2xl bg-red-50 text-red-500 flex items-center justify-center mx-auto mb-3">
                <PackageOpen className="w-7 h-7" />
              </div>
              <h3 className="text-lg font-extrabold text-slate-900 mb-1">
                Không tìm thấy sản phẩm nào phù hợp
              </h3>
              <p className="text-slate-500 text-xs max-w-md mx-auto mb-6">
                {isAnyFilterActive
                  ? 'Không có sản phẩm nào phù hợp với bộ lọc hiện tại. Thử thay đổi khoảng giá, từ khóa tìm kiếm hoặc chọn danh mục khác.'
                  : 'Hiện tại chưa có sản phẩm nào trong hệ thống gian hàng.'}
              </p>
              <button
                type="button"
                onClick={handleClearFilters}
                className="px-6 py-2.5 bg-red-500 hover:bg-red-600 text-white text-xs font-bold rounded-full shadow-sm hover:shadow-md transition-all cursor-pointer inline-flex items-center gap-2"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Xóa bộ lọc / Xem tất cả sản phẩm</span>
              </button>
            </div>
          ) : (
            /* Data Grid & Phân trang chính */
            <>
              <motion.div
                variants={listContainerVariants}
                initial="hidden"
                animate="visible"
                className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-5"
              >
                {displayedProducts.map((item) => (
                  <ProductCard
                    key={item.id}
                    product={item}
                    onAddToCart={handleAddToCart}
                    variants={listItemVariants}
                  />
                ))}
              </motion.div>

              {/* Phân trang đầy đủ ở chân trang */}
              <div className="mt-10 pt-6 border-t border-slate-200">
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  pageSize={pageSize}
                  onPageChange={handlePageChange}
                  onPageSizeChange={handlePageSizeChange}
                  pageSizeOptions={[12, 24, 48]}
                  theme="blue"
                  scrollToTop={true}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default ProductListPage