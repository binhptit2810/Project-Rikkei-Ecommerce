import React, { useState, useEffect, useCallback, useRef } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import {
  Search,
  ArrowUpDown,
  PackageOpen,
  X,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
} from 'lucide-react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ProductCard from '../../components/common/ProductCard'
import { useCartStore } from '../../store/useCartStore'

// Skeleton card cho hiệu ứng tải sản phẩm
const ProductCardSkeleton = () => (
  <div className="bg-white rounded-lg border border-zinc-200 shadow-sm overflow-hidden flex flex-col justify-between animate-pulse">
    <div>
      <div className="aspect-square w-full bg-zinc-100" />
      <div className="p-4 space-y-2.5">
        <div className="h-4 bg-zinc-200 rounded w-3/4" />
        <div className="h-3 bg-zinc-100 rounded w-full" />
        <div className="h-3 bg-zinc-100 rounded w-1/2" />
      </div>
    </div>
    <div className="p-4 pt-3 border-t border-zinc-100 flex items-center justify-between gap-2 mt-auto">
      <div className="space-y-1">
        <div className="h-2.5 bg-zinc-100 rounded w-10" />
        <div className="h-4 bg-zinc-200 rounded w-20" />
      </div>
      <div className="h-8 w-20 bg-zinc-200 rounded-md" />
    </div>
  </div>
)

export const ProductListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const addToCart = useCartStore((state) => state.addToCart)
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()

  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Toast Notification state
  const [toast, setToast] = useState(null)
  const toastTimeoutRef = useRef(null)

  // Pagination state
  const [page, setPage] = useState(parseInt(searchParams.get('page') || '0', 10))
  const [pageSize] = useState(12)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Filter & Search state
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [searchInput, setSearchInput] = useState(searchParams.get('keyword') || '')
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('categoryId') || '')
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'createdAt')
  const [sortDir, setSortDir] = useState(searchParams.get('sortDir') || 'desc')

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

  // Load active categories for filter
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await categoryService.getCategories()
        if (res.success && res.data) {
          setCategories(res.data)
        }
      } catch (err) {
        console.error('Không thể tải danh mục:', err)
      }
    }
    fetchCategories()
  }, [])

  // Load products based on filter, keyword, page, sort
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
      if (res.success && res.data) {
        setProducts(res.data.content || [])
        setTotalPages(res.data.totalPages || 0)
        setTotalElements(res.data.totalElements || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải sản phẩm:', err)
      setError(err.response?.data?.message || 'Không thể tải danh sách sản phẩm')
    } finally {
      setLoading(false)
    }
  }, [keyword, selectedCategory, page, pageSize, sortBy, sortDir])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Sync state to URL
  const updateUrl = (newKeyword, newCatId, newPage, newSortBy, newSortDir) => {
    const p = {}
    if (newKeyword) p.keyword = newKeyword
    if (newCatId) p.categoryId = newCatId
    if (newPage > 0) p.page = newPage.toString()
    if (newSortBy !== 'createdAt') p.sortBy = newSortBy
    if (newSortDir !== 'desc') p.sortDir = newSortDir
    setSearchParams(p)
  }

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setKeyword(searchInput)
    setPage(0)
    updateUrl(searchInput, selectedCategory, 0, sortBy, sortDir)
  }

  const handleCategorySelect = (catId) => {
    const updated = catId === selectedCategory ? '' : catId
    setSelectedCategory(updated)
    setPage(0)
    updateUrl(keyword, updated, 0, sortBy, sortDir)
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
    updateUrl(keyword, selectedCategory, 0, newSortBy, newSortDir)
  }

  const handlePageChange = (newPage) => {
    setPage(newPage)
    updateUrl(keyword, selectedCategory, newPage, sortBy, sortDir)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleClearFilters = () => {
    setKeyword('')
    setSearchInput('')
    setSelectedCategory('')
    setPage(0)
    setSortBy('createdAt')
    setSortDir('desc')
    setSearchParams({})
  }

  const activeCategoryObj = categories.find((c) => c.id.toString() === selectedCategory)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full relative">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 max-w-sm w-full animate-in slide-in-from-bottom-2">
          <div className="bg-white rounded-lg shadow-md border border-zinc-200 p-4 flex items-start gap-3">
            {toast.type === 'error' ? (
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            )}
            <div className="flex-1 min-w-0">
              <p className="text-xs font-medium text-zinc-900 leading-snug">
                {toast.message}
              </p>
              {toast.type === 'success' && (
                <Link
                  to="/cart"
                  className="inline-flex items-center gap-1 text-xs font-medium text-zinc-950 underline underline-offset-4 hover:text-zinc-700 mt-1 transition-colors"
                >
                  <span>Xem giỏ hàng</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              )}
            </div>
            <button
              type="button"
              onClick={() => setToast(null)}
              className="text-zinc-400 hover:text-zinc-700 p-1 rounded hover:bg-zinc-100 transition-colors cursor-pointer"
              aria-label="Đóng thông báo"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Page Header */}
      <div className="mb-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-6 border-b border-zinc-200">
          <div>
            <div className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wider text-zinc-500 mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Cửa hàng trực tuyến</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-semibold text-zinc-950 tracking-tight">
              {activeCategoryObj ? activeCategoryObj.name : 'Tất cả sản phẩm'}
            </h1>
            <p className="text-zinc-500 text-sm mt-1">
              {loading ? (
                'Đang tìm kiếm...'
              ) : (
                <span>
                  Hiển thị <span className="font-medium text-zinc-900">{totalElements}</span> sản phẩm
                  {keyword && (
                    <span>
                      {' '}cho từ khóa "<span className="text-zinc-900 font-medium">{keyword}</span>"
                    </span>
                  )}
                </span>
              )}
            </p>
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm sản phẩm theo tên..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-8 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-700 cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
            <button
              type="submit"
              className="inline-flex items-center justify-center h-9 px-4 rounded-md bg-zinc-900 text-zinc-50 hover:bg-zinc-800 text-sm font-medium transition-colors shadow-sm cursor-pointer"
            >
              Tìm kiếm
            </button>
          </form>
        </div>

        {/* Filter & Sorting toolbar */}
        <div className="mt-4 flex flex-wrap items-center justify-between gap-4">
          {/* Category Filter Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none flex-1 min-w-0">
            <button
              onClick={() => handleCategorySelect('')}
              className={`px-3 py-1.5 rounded-md text-xs font-medium whitespace-nowrap transition-colors cursor-pointer ${
                selectedCategory === ''
                  ? 'bg-zinc-900 text-zinc-50 shadow-sm'
                  : 'bg-zinc-100 text-zinc-700 hover:bg-zinc-200/80 border border-zinc-200'
              }`}
            >
              Tất cả danh mục
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => handleCategorySelect(cat.id.toString())}
                className={`px-3 py-1.5 rounded-md text-xs font-medium whitespace-nowrap transition-colors cursor-pointer ${
                  selectedCategory === cat.id.toString()
                    ? 'bg-zinc-900 text-zinc-50 shadow-sm'
                    : 'bg-zinc-100 text-zinc-700 hover:bg-zinc-200/80 border border-zinc-200'
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
                className="inline-flex items-center gap-1 text-xs font-medium text-zinc-500 hover:text-zinc-900 px-2 py-1 rounded hover:bg-zinc-100 transition-colors cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
                <span>Xóa lọc</span>
              </button>
            )}

            <div className="flex items-center gap-2">
              <ArrowUpDown className="w-3.5 h-3.5 text-zinc-400" />
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
                className="h-8 text-xs font-medium border border-zinc-200 rounded-md px-2.5 bg-white text-zinc-800 shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 cursor-pointer"
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
        <div className="space-y-6">
          <div className="flex items-center justify-center gap-2.5 py-4 text-xs font-medium text-zinc-600 bg-zinc-50 rounded-lg border border-zinc-200">
            <LoadingSpinner size="sm" className="text-zinc-900" />
            <span>Đang tải danh sách sản phẩm...</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {Array.from({ length: 8 }).map((_, idx) => (
              <ProductCardSkeleton key={idx} />
            ))}
          </div>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-700 p-6 rounded-lg text-center text-sm my-8">
          <p className="font-medium">{error}</p>
          <button
            onClick={fetchProducts}
            className="mt-3 px-3 py-1.5 bg-white border border-red-200 text-red-700 text-xs font-medium rounded-md hover:bg-red-50 transition-colors cursor-pointer"
          >
            Thử tải lại
          </button>
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-lg border border-dashed border-zinc-200 shadow-xs my-4">
          <div className="w-12 h-12 rounded-lg bg-zinc-100 text-zinc-700 flex items-center justify-center mx-auto mb-3 border border-zinc-200">
            <PackageOpen className="w-6 h-6" />
          </div>
          <h3 className="text-base font-semibold text-zinc-900 mb-1">Không tìm thấy sản phẩm nào</h3>
          <p className="text-zinc-500 text-xs max-w-md mx-auto mb-5">
            {keyword || selectedCategory
              ? 'Không có sản phẩm nào phù hợp với bộ lọc hiện tại. Thử thay đổi từ khóa tìm kiếm hoặc chọn danh mục khác.'
              : 'Hiện tại chưa có sản phẩm nào trong hệ thống.'}
          </p>
          <button
            type="button"
            onClick={handleClearFilters}
            className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 text-xs font-medium rounded-md shadow-sm transition-colors cursor-pointer inline-flex items-center gap-1.5"
          >
            <span>Xóa bộ lọc / Xem tất cả sản phẩm</span>
          </button>
        </div>
      ) : (
        <>
          {/* Products Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((item) => (
              <ProductCard
                key={item.id}
                product={item}
                onAddToCart={handleAddToCart}
              />
            ))}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-12 flex items-center justify-center gap-1.5">
              <button
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-md border border-zinc-200 text-xs font-medium text-zinc-700 hover:bg-zinc-100 disabled:opacity-40 disabled:pointer-events-none transition-colors bg-white shadow-xs cursor-pointer"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Trước</span>
              </button>

              <div className="flex items-center gap-1">
                {Array.from({ length: totalPages }, (_, idx) => {
                  const isCurrent = idx === page
                  return (
                    <button
                      key={idx}
                      onClick={() => handlePageChange(idx)}
                      className={`w-8 h-8 rounded-md text-xs font-medium transition-colors cursor-pointer ${
                        isCurrent
                          ? 'bg-zinc-900 text-zinc-50 shadow-sm border border-zinc-900'
                          : 'text-zinc-700 bg-white hover:bg-zinc-100 border border-zinc-200'
                      }`}
                    >
                      {idx + 1}
                    </button>
                  )
                })}
              </div>

              <button
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= totalPages - 1}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-md border border-zinc-200 text-xs font-medium text-zinc-700 hover:bg-zinc-100 disabled:opacity-40 disabled:pointer-events-none transition-colors bg-white shadow-xs cursor-pointer"
              >
                <span>Sau</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
