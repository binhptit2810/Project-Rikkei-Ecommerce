import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Package,
  Plus,
  RefreshCw,
  Search,
  Filter,
  Edit2,
  Trash2,
  AlertCircle,
  CheckCircle2,
  X,
  Image as ImageIcon,
  ArrowUpDown,
  AlertTriangle,
} from 'lucide-react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ProductModal from '../../components/admin/ProductModal'
import Pagination from '../../components/common/Pagination'

/**
 * Trang Quản lý Sản phẩm dành cho Admin (AdminProductsPage).
 * Tuyến đường (Route): /admin/products
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D10-DDM] Loại bỏ Mock Data Module Sản phẩm & Kiểm thử Phân trang Dữ liệu lớn
 *
 * Tính năng chính:
 * 1. Hiển thị danh sách sản phẩm với phân trang dữ liệu lớn, tìm kiếm từ khóa, lọc danh mục, sắp xếp.
 * 2. 100% dữ liệu thực từ REST API Admin (GET /api/v1/admin/products), không mock data hay fallback data.
 * 3. Thêm mới và Chỉnh sửa thông tin sản phẩm thông qua ProductModal chuẩn mực.
 * 4. Ẩn / Xóa mềm sản phẩm với hộp thoại xác nhận an toàn.
 * 5. Tích hợp Pagination component hỗ trợ đổi kích thước trang (10, 20, 50).
 */
export const AdminProductsPage = () => {
  // --- Dữ liệu sản phẩm & danh mục ---
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  // --- Trạng thái phân trang & bộ lọc ---
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortDir, setSortDir] = useState('desc')

  // --- Trạng thái giao diện (Loading, Error, Thông báo) ---
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // --- Trạng thái Modal Thêm / Sửa Sản phẩm (ProductModal) ---
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null) // null = Thêm mới, object = Sửa

  // --- Trạng thái Modal Xác nhận Xóa / Ẩn ---
  const [deletingProduct, setDeletingProduct] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Hàm định dạng tiền tệ VNĐ chuẩn
  const formatVND = (amount) => {
    if (amount === null || amount === undefined || isNaN(amount)) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount)
  }

  // Tải danh mục phục vụ bộ lọc và form
  const fetchCategories = async () => {
    try {
      const res = await categoryService.getAdminCategories()
      if (res?.data) {
        if (Array.isArray(res.data)) {
          setCategories(res.data)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setCategories(res.data.content)
        }
      }
    } catch (err) {
      console.warn('Không thể tải danh sách danh mục qua admin endpoint, thử public endpoint:', err)
      try {
        const publicRes = await categoryService.getCategories()
        if (publicRes?.data && Array.isArray(publicRes.data)) {
          setCategories(publicRes.data)
        }
      } catch (pubErr) {
        console.error('Không thể tải danh mục:', pubErr)
      }
    }
  }

  // Tải danh sách sản phẩm từ backend API thực tế
  const fetchProducts = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    const params = {
      page,
      size: pageSize,
      keyword: keyword.trim() || undefined,
      categoryId: selectedCategory || undefined,
      sortBy,
      sortDir,
    }

    try {
      const res = await productService.getAdminProducts(params)

      if (res?.data) {
        if (Array.isArray(res.data)) {
          setProducts(res.data)
          setTotalElements(res.data.length)
          setTotalPages(Math.ceil(res.data.length / pageSize) || 1)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setProducts(res.data.content)
          setTotalElements(res.data.totalElements ?? res.data.content.length)
          setTotalPages(res.data.totalPages ?? 1)
        } else {
          setProducts([])
          setTotalElements(0)
          setTotalPages(0)
        }
      } else {
        setProducts([])
        setTotalElements(0)
        setTotalPages(0)
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách sản phẩm từ máy chủ hệ thống.'
      setError(msg)
      setProducts([])
      setTotalElements(0)
      setTotalPages(0)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, selectedCategory, sortBy, sortDir])

  // Khởi tạo dữ liệu
  useEffect(() => {
    fetchCategories()
  }, [])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  // Tự động đóng toast sau 4 giây
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [toastMessage])

  // Map ID danh mục sang tên danh mục hiển thị
  const categoryMap = useMemo(() => {
    const map = new Map()
    categories.forEach((cat) => {
      map.set(cat.id, cat.name)
    })
    return map
  }, [categories])

  // Mở modal Thêm mới
  const handleOpenCreateModal = () => {
    setEditingProduct(null)
    setIsModalOpen(true)
  }

  // Mở modal Sửa
  const handleOpenEditModal = (product) => {
    setEditingProduct(product)
    setIsModalOpen(true)
  }

  // Đóng modal
  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingProduct(null)
  }

  // Callback thành công từ ProductModal
  const handleModalSuccess = (msg) => {
    setToastMessage({
      type: 'success',
      text: msg,
    })
    fetchProducts()
  }

  // Callback lỗi từ ProductModal
  const handleModalError = (errMsg) => {
    setToastMessage({
      type: 'error',
      text: errMsg,
    })
  }

  // Xác nhận Ẩn / Xóa mềm
  const handleConfirmDelete = async () => {
    if (!deletingProduct) return

    setIsDeleting(true)
    try {
      await productService.deleteProduct(deletingProduct.id)
      setToastMessage({
        type: 'success',
        text: `Đã ẩn sản phẩm "${deletingProduct.name}" (#${deletingProduct.id}) thành công!`,
      })
      setDeletingProduct(null)
      fetchProducts()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể ẩn sản phẩm. Vui lòng thử lại.'
      setToastMessage({ type: 'error', text: msg })
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full animate-in fade-in duration-200">
      {/* Toast thông báo */}
      {toastMessage && (
        <div
          className={`fixed top-20 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg border text-sm font-semibold transition-all animate-in slide-in-from-top-2 ${
            toastMessage.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          )}
          <span>{toastMessage.text}</span>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="p-1 hover:bg-slate-200/50 rounded-lg text-slate-500 cursor-pointer ml-2"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 1. Header Trang & Thao tác chính */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 tracking-tight">
              Quản lý sản phẩm
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold badge-neutral uppercase tracking-wider">
              <Package className="w-3.5 h-3.5" /> {totalElements} sản phẩm
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý danh sách hàng hóa, giá bán, số lượng tồn kho và thông tin hiển thị trên gian hàng.
          </p>
        </div>

        {/* Nút hành động */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchProducts}
            disabled={isLoading}
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-zinc-950 shadow-xs transition-all cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách sản phẩm"
          >
            <RefreshCw
              className={`w-4 h-4 ${isLoading ? 'animate-spin text-zinc-900' : 'text-slate-500'}`}
            />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            onClick={handleOpenCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm sản phẩm</span>
          </button>
        </div>
      </div>

      {/* 2. Thanh tìm kiếm & bộ lọc */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs my-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Tìm kiếm theo tên hoặc mô tả */}
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo tên sản phẩm..."
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value)
              setPage(0)
            }}
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-slate-800"
          />
        </div>

        {/* Lọc theo danh mục */}
        <div className="relative">
          <Filter className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <select
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value)
              setPage(0)
            }}
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-slate-800 cursor-pointer appearance-none"
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>

        {/* Sắp xếp */}
        <div className="relative">
          <ArrowUpDown className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <select
            value={`${sortBy}-${sortDir}`}
            onChange={(e) => {
              const [sb, sd] = e.target.value.split('-')
              setSortBy(sb)
              setSortDir(sd)
              setPage(0)
            }}
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-slate-800 cursor-pointer appearance-none"
          >
            <option value="createdAt-desc">Mới nhất</option>
            <option value="createdAt-asc">Cũ nhất</option>
            <option value="price-asc">Giá tăng dần</option>
            <option value="price-desc">Giá giảm dần</option>
            <option value="name-asc">Tên (A-Z)</option>
            <option value="name-desc">Tên (Z-A)</option>
          </select>
        </div>

        {/* Nút đặt lại bộ lọc */}
        {(keyword || selectedCategory) && (
          <div className="flex items-center">
            <button
              type="button"
              onClick={() => {
                setKeyword('')
                setSelectedCategory('')
                setPage(0)
              }}
              className="w-full py-2 px-3 text-xs font-semibold text-slate-600 hover:text-zinc-950 bg-slate-100 hover:bg-zinc-100 rounded-xl transition-colors cursor-pointer flex items-center justify-center gap-1.5"
            >
              <X className="w-3.5 h-3.5" />
              <span>Xóa bộ lọc</span>
            </button>
          </div>
        )}
      </div>

      {/* 3. Bảng dữ liệu sản phẩm */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden">
        {/* Thông báo lỗi nếu có */}
        {error && (
          <div className="p-4 bg-red-50 border-b border-red-200 text-red-800 text-sm flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
            <span className="flex-1">{error}</span>
            <button
              type="button"
              onClick={fetchProducts}
              className="text-xs font-bold underline hover:text-red-950 cursor-pointer"
            >
              Thử lại
            </button>
          </div>
        )}

        {/* Trạng thái tải dữ liệu */}
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-500">
            <LoadingSpinner size="lg" className="text-zinc-900 mb-3" />
            <p className="text-sm font-medium">Đang tải danh sách sản phẩm...</p>
          </div>
        ) : products.length === 0 ? (
          /* Trạng thái rỗng */
          <div className="py-16 text-center px-4">
            <div className="w-16 h-16 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-4">
              <Package className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800">
              Không tìm thấy sản phẩm nào
            </h3>
            <p className="text-sm text-slate-500 max-w-md mx-auto mt-1 mb-5">
              {keyword || selectedCategory
                ? 'Không có sản phẩm nào khớp với bộ lọc tìm kiếm hiện tại.'
                : 'Chưa có sản phẩm nào trong hệ thống. Hãy thêm sản phẩm đầu tiên.'}
            </p>
            {keyword || selectedCategory ? (
              <button
                type="button"
                onClick={() => {
                  setKeyword('')
                  setSelectedCategory('')
                  setPage(0)
                }}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl transition-all cursor-pointer"
              >
                Xóa bộ lọc
              </button>
            ) : (
              <button
                type="button"
                onClick={handleOpenCreateModal}
                className="inline-flex items-center gap-2 px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 text-xs font-semibold rounded-xl shadow-xs transition-all cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Thêm sản phẩm mới</span>
              </button>
            )}
          </div>
        ) : (
          /* Bảng dữ liệu */
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4 w-16">#ID</th>
                  <th className="py-3.5 px-4 min-w-[240px]">Sản phẩm</th>
                  <th className="py-3.5 px-4 min-w-[140px]">Danh mục</th>
                  <th className="py-3.5 px-4 min-w-[130px] text-right">Giá bán</th>
                  <th className="py-3.5 px-4 min-w-[100px] text-center">Tồn kho</th>
                  <th className="py-3.5 px-4 min-w-[120px] text-center">Trạng thái</th>
                  <th className="py-3.5 px-4 w-28 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {products.map((product) => {
                  const isActive = product.active !== false
                  const categoryName =
                    product.categoryName ||
                    (product.category && product.category.name) ||
                    categoryMap.get(product.categoryId) ||
                    'Chưa phân loại'

                  return (
                    <tr
                      key={product.id}
                      className="hover:bg-slate-50/60 transition-colors group"
                    >
                      {/* Cột 1: ID */}
                      <td className="py-3.5 px-4 font-mono text-xs font-semibold text-slate-400 tabular-nums">
                        #{product.id}
                      </td>

                      {/* Cột 2: Sản phẩm (Hình ảnh + Tên + Mô tả) */}
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200/80 overflow-hidden shrink-0 flex items-center justify-center">
                            {product.imageUrl ? (
                              <img
                                src={product.imageUrl}
                                alt={product.name}
                                className="w-full h-full object-cover"
                                onError={(e) => {
                                  e.currentTarget.onerror = null
                                  e.currentTarget.src =
                                    'https://images.unsplash.com/photo-1560343090-f0409e92791a?w=100&auto=format&fit=crop&q=60'
                                }}
                              />
                            ) : (
                              <ImageIcon className="w-5 h-5 text-slate-300" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-slate-800 truncate group-hover:text-zinc-950 transition-colors">
                              {product.name}
                            </p>
                            {product.description && (
                              <p className="text-xs text-slate-500 truncate max-w-xs mt-0.5">
                                {product.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Cột 3: Danh mục */}
                      <td className="py-3.5 px-4 text-xs font-medium text-slate-600">
                        {categoryName}
                      </td>

                      {/* Cột 4: Giá bán */}
                      <td className="py-3.5 px-4 text-right font-bold text-slate-800 tabular-nums">
                        {formatVND(product.price)}
                      </td>

                      {/* Cột 5: Tồn kho */}
                      <td className="py-3.5 px-4 text-center tabular-nums">
                        <span
                          className={`inline-block px-2 py-0.5 rounded-md text-xs font-semibold ${
                            (product.stockQuantity ?? 0) > 10
                              ? 'bg-slate-100 text-slate-700'
                              : (product.stockQuantity ?? 0) > 0
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-red-100 text-red-700'
                          }`}
                        >
                          {product.stockQuantity ?? 0} SP
                        </span>
                      </td>

                      {/* Cột 6: Trạng thái */}
                      <td className="py-3.5 px-4 text-center">
                        {isActive ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700 border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            Đang bán
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 border border-slate-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
                            Đã ẩn
                          </span>
                        )}
                      </td>

                      {/* Cột 7: Thao tác */}
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(product)}
                            className="p-1.5 text-slate-500 hover:text-zinc-950 hover:bg-zinc-100 rounded-lg transition-colors cursor-pointer"
                            title="Chỉnh sửa sản phẩm"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingProduct(product)}
                            className="p-1.5 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Ẩn / Xóa mềm sản phẩm"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Phân trang dữ liệu lớn chuẩn hóa với component Pagination */}
        <div className="p-4 bg-slate-50/60 border-t border-slate-200">
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(newSize) => {
              setPageSize(newSize)
              setPage(0)
            }}
            pageSizeOptions={[10, 20, 50]}
            theme="zinc"
            scrollToTop={false}
          />
        </div>
      </div>

      {/* ==================================================================== */}
      {/* 4. Modal Thêm / Chỉnh sửa Sản phẩm (ProductModal) */}
      {/* ==================================================================== */}
      <ProductModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        product={editingProduct}
        categories={categories}
        onSuccess={handleModalSuccess}
        onError={handleModalError}
      />

      {/* ==================================================================== */}
      {/* 5. Modal Xác nhận Ẩn / Xóa mềm Sản phẩm */}
      {/* ==================================================================== */}
      {deletingProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
            onClick={() => !isDeleting && setDeletingProduct(null)}
          />

          <div className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-md p-6 z-10 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-4">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-slate-900 text-center">
              Xác nhận ẩn sản phẩm
            </h3>
            <p className="text-sm text-slate-500 text-center mt-2 leading-relaxed">
              Bạn có chắc chắn muốn ẩn sản phẩm{' '}
              <strong className="text-slate-800 font-semibold">
                "{deletingProduct.name}"
              </strong>{' '}
              (Mã: #{deletingProduct.id})?
            </p>
            <p className="text-xs text-zinc-600 bg-zinc-100 border border-zinc-200 rounded-md p-3 mt-3 text-center">
              Sản phẩm sẽ chuyển sang trạng thái đã ẩn (xóa mềm) và không còn hiển thị cho khách hàng đặt mua.
            </p>

            <div className="mt-6 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setDeletingProduct(null)}
                disabled={isDeleting}
                className="px-4 py-2 border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Hủy thao tác
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-2 px-5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50"
              >
                {isDeleting && (
                  <LoadingSpinner size="sm" className="text-white" />
                )}
                <span>{isDeleting ? 'Đang xử lý...' : 'Xác nhận ẩn'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminProductsPage
