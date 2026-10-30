import React, { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  Search,
  SlidersHorizontal,
  ArrowUpDown,
  PackageOpen,
  X,
  ChevronLeft,
  ChevronRight,
  ArrowRight,
  ShoppingBag,
  Sparkles,
} from 'lucide-react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const ProductListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()

  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

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

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const activeCategoryObj = categories.find((c) => c.id.toString() === selectedCategory)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full">
      {/* Page Header */}
      <div className="mb-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 pb-6 border-b border-slate-200">
          <div>
            <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-orange-600 mb-1">
              <Sparkles className="w-4 h-4" />
              <span>Cửa hàng trực tuyến</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              {activeCategoryObj ? activeCategoryObj.name : 'Tất cả sản phẩm'}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              {loading ? (
                'Đang tìm kiếm...'
              ) : (
                <span>
                  Hiển thị <span className="font-semibold text-slate-900">{totalElements}</span> sản phẩm chất lượng
                  {keyword && (
                    <span>
                      {' '}cho từ khóa "<span className="text-orange-600 font-semibold">{keyword}</span>"
                    </span>
                  )}
                </span>
              )}
            </p>
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-md w-full">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm sản phẩm theo tên..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-10 pr-9 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-sm bg-white shadow-2xs transition-all"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>
            <button
              type="submit"
              className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm rounded-xl shadow-2xs transition-colors cursor-pointer"
            >
              Tìm
            </button>
          </form>
        </div>

        {/* Filter & Sorting toolbar */}
        <div className="mt-5 flex flex-wrap items-center justify-between gap-4">
          {/* Category Filter Pills */}
          <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none flex-1 min-w-0">
            <button
              onClick={() => handleCategorySelect('')}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                selectedCategory === ''
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
              }`}
            >
              Tất cả danh mục
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => handleCategorySelect(cat.id.toString())}
                className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                  selectedCategory === cat.id.toString()
                    ? 'bg-orange-600 text-white shadow-2xs'
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
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-red-600 px-2.5 py-1.5 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
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
                className="text-xs font-medium border border-slate-200 rounded-xl px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 cursor-pointer shadow-2xs"
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
        <div className="py-24 flex justify-center">
          <LoadingSpinner />
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 text-red-700 p-6 rounded-2xl text-center text-sm my-8">
          <p className="font-semibold">{error}</p>
          <button
            onClick={fetchProducts}
            className="mt-3 px-4 py-2 bg-white border border-red-200 text-red-700 text-xs font-bold rounded-xl hover:bg-red-100 transition-colors"
          >
            Thử tải lại
          </button>
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-2xl border border-dashed border-slate-200 shadow-2xs my-4">
          <div className="w-16 h-16 rounded-2xl bg-orange-50 text-orange-600 flex items-center justify-center mx-auto mb-4">
            <PackageOpen className="w-8 h-8" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 mb-1">Không tìm thấy sản phẩm nào</h3>
          <p className="text-slate-500 text-sm max-w-md mx-auto mb-6">
            Thử thay đổi từ khóa tìm kiếm, điều chỉnh bộ lọc hoặc chọn danh mục khác.
          </p>
          <button
            onClick={handleClearFilters}
            className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-xl shadow-2xs transition-colors cursor-pointer"
          >
            Xem tất cả sản phẩm
          </button>
        </div>
      ) : (
        <>
          {/* Products Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((item) => {
              const isOutOfStock = item.stockQuantity <= 0
              return (
                <Link
                  key={item.id}
                  to={`/products/${item.id}`}
                  className="group bg-white rounded-2xl border border-slate-200/80 shadow-2xs hover:shadow-card hover:border-orange-300 transition-all duration-200 overflow-hidden flex flex-col"
                >
                  {/* Product Image */}
                  <div className="aspect-square w-full bg-slate-50 relative overflow-hidden flex items-center justify-center">
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        onError={(e) => {
                          e.target.onerror = null
                          e.target.src = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'
                        }}
                      />
                    ) : (
                      <div className="flex flex-col items-center justify-center text-slate-300">
                        <ShoppingBag className="w-12 h-12 stroke-[1.25] text-slate-300 mb-1" />
                        <span className="text-xs text-slate-400">Hình ảnh minh họa</span>
                      </div>
                    )}

                    {/* Category Badge */}
                    {item.categoryName && (
                      <span className="absolute top-3 left-3 bg-white/90 text-slate-800 text-[11px] font-semibold px-2.5 py-1 rounded-lg backdrop-blur-xs border border-slate-200/60 shadow-2xs">
                        {item.categoryName}
                      </span>
                    )}

                    {/* Stock Status Badge */}
                    {isOutOfStock ? (
                      <span className="absolute top-3 right-3 bg-red-600 text-white text-[10px] font-bold px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-2xs">
                        Hết hàng
                      </span>
                    ) : (
                      <span className="absolute top-3 right-3 bg-emerald-600 text-white text-[10px] font-semibold px-2.5 py-1 rounded-lg shadow-2xs">
                        Còn {item.stockQuantity}
                      </span>
                    )}
                  </div>

                  {/* Product Info */}
                  <div className="p-4 flex flex-col flex-1 justify-between">
                    <div>
                      <h3 className="font-bold text-slate-900 text-sm group-hover:text-orange-600 transition-colors line-clamp-2 mb-1.5 leading-snug">
                        {item.name}
                      </h3>
                      <p className="text-slate-500 text-xs line-clamp-2 mb-3 leading-relaxed">
                        {item.description || 'Sản phẩm chính hãng với tiêu chuẩn chất lượng cao.'}
                      </p>
                    </div>

                    <div className="pt-3 border-t border-slate-100 flex items-center justify-between mt-auto">
                      <div>
                        <span className="text-[11px] text-slate-400 block font-medium">Giá bán</span>
                        <span className="text-base font-extrabold text-orange-600 tabular-nums">
                          {formatPrice(item.price)}
                        </span>
                      </div>

                      <span className="text-xs font-semibold text-orange-600 group-hover:translate-x-1 transition-transform inline-flex items-center gap-1">
                        Chi tiết <ArrowRight className="w-3.5 h-3.5" />
                      </span>
                    </div>
                  </div>
                </Link>
              )
            })}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="mt-12 flex items-center justify-center gap-2">
              <button
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors bg-white shadow-2xs"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Trước</span>
              </button>

              <div className="flex items-center gap-1">
                {Array.from({ length: totalPages }, (_, idx) => {
                  const isCurrent = idx === page
                  return (
                    <button
                      key={idx}
                      onClick={() => handlePageChange(idx)}
                      className={`w-9 h-9 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                        isCurrent
                          ? 'bg-orange-600 text-white shadow-2xs'
                          : 'text-slate-700 bg-white hover:bg-slate-100 border border-slate-200'
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
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors bg-white shadow-2xs"
              >
                <span>Sau</span>
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default ProductListPage
