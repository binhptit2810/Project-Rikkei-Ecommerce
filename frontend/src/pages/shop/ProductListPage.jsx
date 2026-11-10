import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import {
  Search,
  ArrowUpDown,
  PackageOpen,
  X,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from 'lucide-react'
import { motion, useReducedMotion } from 'framer-motion'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ProductCard from '../../components/common/ProductCard'
import Pagination from '../../components/common/Pagination'
import { useCartStore } from '../../store/useCartStore'

/**
 * Skeleton card hiển thị trạng thái đang tải (Loading State).
 */
const ProductCardSkeleton = () => (
  <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden flex flex-col justify-between animate-pulse">
    <div>
      <div className="aspect-square w-full bg-slate-100" />
      <div className="p-5 space-y-3">
        <div className="h-3 bg-slate-200 rounded-full w-1/3" />
        <div className="h-4 bg-slate-200 rounded-full w-4/5" />
        <div className="h-3 bg-slate-100 rounded-full w-1/2" />
      </div>
    </div>
    <div className="p-5 pt-0">
      <div className="h-10 bg-slate-100 rounded-full w-full" />
    </div>
  </div>
)

/**
 * Trang Danh sách Sản phẩm (Shop / ProductListPage).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D10-DDM] Loại bỏ Mock Data Module Sản phẩm & Kiểm thử Phân trang Dữ liệu lớn
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

  // Toast Notification state
  const [toast, setToast] = useState(null)
  const toastTimeoutRef = useRef(null)

  // Pagination state (đồng bộ với URL searchParams)
  const [page, setPage] = useState(parseInt(searchParams.get('page') || '0', 10))
  const [pageSize, setPageSize] = useState(parseInt(searchParams.get('size') || '12', 10))
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Filter & Search state
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [searchInput, setSearchInput] = useState(searchParams.get('keyword') || '')
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '')
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'createdAt')
  const [sortDir, setSortDir] = useState(searchParams.get('sortDir') || 'desc')
  const shouldReduceMotion = useReducedMotion()

  const listContainerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: shouldReduceMotion ? 0 : 0.05,
      },
    },
  }

  const listItemVariants = {
    hidden: { opacity: 0, y: shouldReduceMotion ? 0 : 16 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.35, ease: 'easeOut' },
    },
  }

  // Hiển thị Toast Notification và tự động ẩn sau 3 giây
  const showToast = (message, type = 'success') => {
    if (toastTimeoutRef.current) {
      clearTimeout(toastTimeoutRef.current)
    }
    setToast({ message, type })
    toastTimeoutRef.current = setTimeout(() => {
      setToast(null)
    }, 3000)
  }

  // Cleanup timeout khi unmount
  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current) {
        clearTimeout(toastTimeoutRef.current)
      }
    }
  }, [])

  // Xử lý thêm vào giỏ hàng
  const handleAddToCart = async (product) => {
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
      const res = await addToCart(product, 1)
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
      } else {
        showToast(`Đã thêm "${product.name}" vào giỏ hàng!`, 'success')
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

  // Tải danh sách sản phẩm từ backend API (100% dữ liệu thực, không fallback mock data)
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
  }, [keyword, selectedCategory, page, pageSize, sortBy, sortDir])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Đồng bộ trạng thái bộ lọc & phân trang vào URL search params
  const updateUrl = (newKeyword, newCatId, newPage, newSize, newSortBy, newSortDir) => {
    const p = {}
    if (newKeyword) p.keyword = newKeyword
    if (newCatId) p.categoryId = newCatId
    if (newPage > 0) p.page = newPage.toString()
    if (newSize !== 12) p.size = newSize.toString()
    if (newSortBy !== 'createdAt') p.sortBy = newSortBy
    if (newSortDir !== 'desc') p.sortDir = newSortDir
    setSearchParams(p)
  }

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setKeyword(searchInput)
    setPage(0)
    updateUrl(searchInput, selectedCategory, 0, pageSize, sortBy, sortDir)
  }

  const handleCategorySelect = (catId) => {
    const updated = catId === selectedCategory ? '' : catId
    setSelectedCategory(updated)
    setPage(0)
    updateUrl(keyword, updated, 0, pageSize, sortBy, sortDir)
  }

  const handleSortChange = (e) => {
    const val = e.target.value
    let newSortBy = 'createdAt'
    let newSortDir = 'desc'

    if (val === 'price_asc') {
      newSortBy = 'price'
      newSortDir = 'asc'
    } else if (val === 'price_desc') {
      newSortBy = 'price'
      newSortDir = 'desc'
    } else if (val === 'name_asc') {
      newSortBy = 'name'
      newSortDir = 'asc'
    }

    setSortBy(newSortBy)
    setSortDir(newSortDir)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, pageSize, newSortBy, newSortDir)
  }

  // Đổi trang
  const handlePageChange = (newPage) => {
    setPage(newPage)
    updateUrl(keyword, selectedCategory, newPage, pageSize, sortBy, sortDir)
  }

  // Đổi kích thước trang (Page Size)
  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize)
    setPage(0)
    updateUrl(keyword, selectedCategory, 0, newSize, sortBy, sortDir)
  }

  // Xóa toàn bộ bộ lọc
  const handleClearFilters = () => {
    setKeyword('')
    setSearchInput('')
    setSelectedCategory('')
    setPage(0)
    setPageSize(12)
    setSortBy('createdAt')
    setSortDir('desc')
    setSearchParams({})
  }

  const activeCategoryObj = categories.find((c) => c.id.toString() === selectedCategory)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex-1 w-full relative animate-in fade-in duration-200">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 max-w-sm w-full animate-in slide-in-from-bottom-2">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200 p-4 flex items-start gap-3">
            {toast.type === 'error' ? (
              <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
            ) : (
              <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
            )}
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-slate-900 leading-snug">
                {toast.message}
              </p>
              {toast.type === 'success' && (
                <Link
                  to="/cart"
                  className="inline-flex items-center gap-1 text-xs font-bold text-blue-600 hover:text-blue-700 mt-1 transition-colors"
                >
                  <span>Xem giỏ hàng</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              )}
            </div>
            <button
              type="button"
              onClick={() => setToast(null)}
              className="text-slate-400 hover:text-slate-700 p-1 rounded-full hover:bg-slate-100 transition-colors cursor-pointer"
              aria-label="Đóng thông báo"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Page Header */}
      <div className="mb-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-6 border-b border-slate-200/90">
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-50 border border-orange-200/80 text-orange-600 text-xs font-bold uppercase tracking-wider mb-2">
              <Sparkles className="w-3.5 h-3.5" />
              <span>RIKKEI STORE</span>
            </div>
            <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {activeCategoryObj ? activeCategoryObj.name : 'Khám phá sản phẩm'}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              {loading ? (
                'Đang tìm kiếm sản phẩm từ hệ thống...'
              ) : (
                <span>
                  Hiển thị <span className="font-bold text-slate-900">{totalElements}</span> sản phẩm công nghệ chính hãng
                  {keyword && (
                    <span>
                      {' '}cho từ khóa "<span className="text-blue-600 font-bold">{keyword}</span>"
                    </span>
                  )}
                </span>
              )}
            </p>
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Tìm sản phẩm theo tên..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="flex h-11 w-full rounded-full border border-slate-200 bg-slate-50 hover:bg-white pl-4 pr-10 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20"
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
              className="inline-flex items-center justify-center h-11 px-5 rounded-full bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold transition-all shadow-sm hover:shadow-md cursor-pointer shrink-0"
            >
              Tìm kiếm
            </button>
          </form>
        </div>

        {/* Filter & Sorting toolbar */}
        <div className="mt-5 flex flex-wrap items-center justify-between gap-4">
          {/* Category Filter Pills */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none flex-1 min-w-0">
            <button
              onClick={() => handleCategorySelect('')}
              className={`px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                selectedCategory === ''
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
              }`}
            >
              Tất cả
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => handleCategorySelect(cat.id.toString())}
                className={`px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  selectedCategory === cat.id.toString()
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>

          {/* Sort dropdown & active filter count */}
          <div className="flex items-center gap-3">
            {(selectedCategory || keyword) && (
              <button
                onClick={handleClearFilters}
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 px-3 py-1.5 rounded-full hover:bg-slate-100 transition-colors cursor-pointer border border-slate-200"
              >
                <X className="w-3.5 h-3.5" />
                <span>Xóa lọc</span>
              </button>
            )}

            <div className="flex items-center gap-2">
              <ArrowUpDown className="w-3.5 h-3.5 text-slate-400" />
              <select
                value={
                  sortBy === 'price' && sortDir === 'asc'
                    ? 'price_asc'
                    : sortBy === 'price' && sortDir === 'desc'
                    ? 'price_desc'
                    : sortBy === 'name'
                    ? 'name_asc'
                    : 'created_desc'
                }
                onChange={handleSortChange}
                className="h-9 text-xs font-bold border border-slate-200 rounded-full px-3.5 bg-white text-slate-800 shadow-xs focus:outline-none focus:border-blue-500 cursor-pointer"
              >
                <option value="created_desc">Mới nhất</option>
                <option value="price_asc">Giá: Thấp đến Cao</option>
                <option value="price_desc">Giá: Cao đến Thấp</option>
                <option value="name_asc">Tên: A-Z</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Product Content Area */}
      {loading ? (
        /* Loading State: Skeleton Cards */
        <div className="space-y-6">
          <div className="flex items-center justify-center gap-2.5 py-4 text-xs font-medium text-slate-600 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
            <LoadingSpinner size="sm" className="text-blue-600" />
            <span>Đang tải danh sách sản phẩm từ hệ thống...</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {Array.from({ length: pageSize > 12 ? 12 : pageSize }).map((_, idx) => (
              <ProductCardSkeleton key={idx} />
            ))}
          </div>
        </div>
      ) : error ? (
        /* Error State */
        <div className="bg-red-50 border border-red-200 text-red-700 p-8 rounded-3xl text-center text-sm my-8">
          <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-3">
            <AlertCircle className="w-6 h-6" />
          </div>
          <p className="font-bold">{error}</p>
          <button
            onClick={fetchProducts}
            className="mt-4 px-5 py-2.5 bg-white border border-red-200 text-red-700 text-xs font-bold rounded-full hover:bg-red-50 transition-colors cursor-pointer shadow-xs inline-flex items-center gap-1.5"
          >
            <span>Thử tải lại</span>
          </button>
        </div>
      ) : products.length === 0 ? (
        /* Empty State */
        <div className="text-center py-24 bg-white rounded-3xl border border-dashed border-slate-200 shadow-xs my-4">
          <div className="w-14 h-14 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto mb-3">
            <PackageOpen className="w-7 h-7" />
          </div>
          <h3 className="text-lg font-extrabold text-slate-900 mb-1">
            Không tìm thấy sản phẩm nào phù hợp
          </h3>
          <p className="text-slate-500 text-xs max-w-md mx-auto mb-6">
            {keyword || selectedCategory
              ? 'Không có sản phẩm nào phù hợp với bộ lọc hiện tại. Thử thay đổi từ khóa tìm kiếm hoặc chọn danh mục khác.'
              : 'Hiện tại chưa có sản phẩm nào trong hệ thống gian hàng.'}
          </p>
          <button
            type="button"
            onClick={handleClearFilters}
            className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-full shadow-sm hover:shadow-md transition-all cursor-pointer inline-flex items-center gap-2"
          >
            <span>Xóa bộ lọc / Xem tất cả sản phẩm</span>
          </button>
        </div>
      ) : (
        /* Data Grid & Pagination */
        <>
          <motion.div
            variants={listContainerVariants}
            initial="hidden"
            animate="visible"
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
          >
            {products.map((item) => (
              <ProductCard
                key={item.id}
                product={item}
                onAddToCart={handleAddToCart}
                variants={listItemVariants}
              />
            ))}
          </motion.div>

          {/* Phân trang chuẩn với component Pagination */}
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
  )
}

export default ProductListPage
