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
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  Eye,
  AlertTriangle,
} from 'lucide-react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang Quản lý Sản phẩm dành cho Admin (AdminProductsPage).
 * Tuyến đường (Route): /admin/products
 *
 * Tính năng chính:
 * 1. Hiển thị danh sách sản phẩm với phân trang, tìm kiếm từ khóa, lọc danh mục, sắp xếp.
 * 2. Tự động chuyển đổi dự phòng (Fallback) từ /api/v1/admin/products sang /api/v1/products nếu endpoint admin chưa sẵn sàng.
 * 3. Thêm mới sản phẩm với modal xác thực dữ liệu đầu vào chặt chẽ.
 * 4. Chỉnh sửa thông tin sản phẩm và cập nhật thời gian thực.
 * 5. Ẩn / Xóa mềm sản phẩm với hộp thoại xác nhận an toàn.
 * 6. Giao diện chuẩn phong cách Tailwind CSS của Rikkei E-commerce (slate, orange-600, tabular-nums).
 */
export const AdminProductsPage = () => {
  // --- Dữ liệu sản phẩm & danh mục ---
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  // --- Trạng thái phân trang & bộ lọc ---
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortDir, setSortDir] = useState('desc')

  // --- Trạng thái giao diện (Loading, Error, Thông báo) ---
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // --- Trạng thái Modal Thêm / Sửa ---
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null) // null = Thêm mới, object = Sửa
  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    price: '',
    stockQuantity: '',
    imageUrl: '',
    description: '',
    active: true,
  })
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

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
  const fetchCategories = useCallback(async () => {
    try {
      let res
      try {
        res = await categoryService.getAdminCategories()
      } catch (adminErr) {
        // Fallback sang endpoint public nếu admin endpoint chưa hỗ trợ
        res = await categoryService.getCategories()
      }

      if (res?.data) {
        if (Array.isArray(res.data)) {
          setCategories(res.data)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setCategories(res.data.content)
        }
      }
    } catch (err) {
      console.warn('Không thể tải danh sách danh mục:', err)
    }
  }, [])

  // Tải danh sách sản phẩm với fallback an toàn
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
      let res
      let isFallback = false

      try {
        // 1. Thử gọi API quản trị dành cho Admin
        res = await productService.getAdminProducts(params)
      } catch (adminErr) {
        const status = adminErr.response?.status
        if (status === 404 || status === 403 || !adminErr.response) {
          // 2. Tự động fallback sang API public để luôn có dữ liệu sản phẩm thật
          isFallback = true
          res = await productService.getProducts(params)
        } else {
          throw adminErr
        }
      }

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
          setTotalPages(1)
        }
      } else {
        setProducts([])
        setTotalElements(0)
        setTotalPages(1)
      }

      if (isFallback) {
        // Thông tin hữu ích khi backend đang dùng public API
        console.info('AdminProductsPage: Đang kết nối fallback qua GET /api/v1/products')
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách sản phẩm từ hệ thống.'
      setError(msg)
      setProducts([])
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, selectedCategory, sortBy, sortDir])

  // Khởi tạo dữ liệu
  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

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
    setFormData({
      name: '',
      categoryId: categories.length > 0 ? String(categories[0].id) : '',
      price: '',
      stockQuantity: '',
      imageUrl: '',
      description: '',
      active: true,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Mở modal Sửa
  const handleOpenEditModal = (product) => {
    setEditingProduct(product)
    setFormData({
      name: product.name || '',
      categoryId: product.categoryId ? String(product.categoryId) : '',
      price: product.price !== undefined ? String(product.price) : '',
      stockQuantity:
        product.stockQuantity !== undefined ? String(product.stockQuantity) : '0',
      imageUrl: product.imageUrl || '',
      description: product.description || '',
      active: product.active !== false,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Đóng modal
  const handleCloseModal = () => {
    if (isSubmitting) return
    setIsModalOpen(false)
    setEditingProduct(null)
    setFormErrors({})
  }

  // Validate form trước khi submit
  const validateForm = () => {
    const errors = {}

    if (!formData.name.trim()) {
      errors.name = 'Vui lòng nhập tên sản phẩm'
    }

    if (!formData.categoryId) {
      errors.categoryId = 'Vui lòng chọn danh mục cho sản phẩm'
    }

    const priceNum = Number(formData.price)
    if (formData.price === '' || isNaN(priceNum) || priceNum <= 0) {
      errors.price = 'Giá bán phải là số lớn hơn 0'
    }

    const stockNum = Number(formData.stockQuantity)
    if (formData.stockQuantity === '' || isNaN(stockNum) || stockNum < 0 || !Number.isInteger(stockNum)) {
      errors.stockQuantity = 'Số lượng tồn kho phải là số nguyên không âm (≥ 0)'
    }

    if (formData.imageUrl && !formData.imageUrl.match(/^(https?:\/\/|\/)/i)) {
      errors.imageUrl = 'Đường dẫn ảnh phải bắt đầu bằng http://, https:// hoặc /'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  // Submit form Thêm / Sửa
  const handleSubmitForm = async (e) => {
    e.preventDefault()
    if (!validateForm()) return

    setIsSubmitting(true)
    const payload = {
      name: formData.name.trim(),
      categoryId: Number(formData.categoryId),
      price: Number(formData.price),
      stockQuantity: Number(formData.stockQuantity),
      imageUrl: formData.imageUrl.trim() || null,
      description: formData.description.trim() || null,
      active: Boolean(formData.active),
    }

    try {
      if (editingProduct) {
        // Cập nhật sản phẩm
        await productService.updateProduct(editingProduct.id, payload)
        setToastMessage({
          type: 'success',
          text: `Đã cập nhật sản phẩm "${payload.name}" thành công!`,
        })
      } else {
        // Thêm sản phẩm mới
        await productService.createProduct(payload)
        setToastMessage({
          type: 'success',
          text: `Đã thêm sản phẩm "${payload.name}" thành công!`,
        })
      }
      setIsModalOpen(false)
      fetchProducts()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi lưu sản phẩm. Vui lòng thử lại.'
      setToastMessage({ type: 'error', text: msg })
    } finally {
      setIsSubmitting(false)
    }
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
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-orange-100 text-orange-700 border border-orange-200 uppercase tracking-wider">
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
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-orange-600 shadow-xs transition-all cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách sản phẩm"
          >
            <RefreshCw
              className={`w-4 h-4 ${isLoading ? 'animate-spin text-orange-600' : 'text-slate-500'}`}
            />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            onClick={handleOpenCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer"
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
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all text-slate-800"
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
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all text-slate-800 cursor-pointer appearance-none"
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
            className="w-full pl-9 pr-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all text-slate-800 cursor-pointer appearance-none"
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
              className="w-full py-2 px-3 text-xs font-semibold text-slate-600 hover:text-orange-600 bg-slate-100 hover:bg-orange-50 rounded-xl transition-colors cursor-pointer flex items-center justify-center gap-1.5"
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

        {/* Trạng thái đang tải */}
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-500">
            <LoadingSpinner size="lg" className="text-orange-600 mb-3" />
            <p className="text-sm font-medium">Đang tải danh sách sản phẩm...</p>
          </div>
        ) : products.length === 0 ? (
          /* Trạng thái không có sản phẩm */
          <div className="py-16 text-center px-4">
            <div className="w-16 h-16 rounded-2xl bg-orange-50 text-orange-500 flex items-center justify-center mx-auto mb-4">
              <Package className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800">
              Không tìm thấy sản phẩm nào
            </h3>
            <p className="text-sm text-slate-500 max-w-md mx-auto mt-1 mb-5">
              {keyword || selectedCategory
                ? 'Không có sản phẩm nào khớp với tiêu chí tìm kiếm hoặc bộ lọc hiện tại.'
                : 'Hệ thống chưa có sản phẩm nào. Hãy bấm nút thêm sản phẩm để bắt đầu đăng bán.'}
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
                className="inline-flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white text-xs font-semibold rounded-xl shadow-xs transition-all cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                <span>Thêm sản phẩm mới</span>
              </button>
            )}
          </div>
        ) : (
          /* Bảng dữ liệu sản phẩm */
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4 w-14">#ID</th>
                  <th className="py-3.5 px-4 min-w-[260px]">Sản phẩm</th>
                  <th className="py-3.5 px-4 min-w-[130px]">Danh mục</th>
                  <th className="py-3.5 px-4 min-w-[130px] text-right">Giá bán</th>
                  <th className="py-3.5 px-4 min-w-[110px] text-center">Tồn kho</th>
                  <th className="py-3.5 px-4 min-w-[120px] text-center">Trạng thái</th>
                  <th className="py-3.5 px-4 w-28 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {products.map((product) => {
                  const categoryName =
                    product.categoryName ||
                    categoryMap.get(product.categoryId) ||
                    'Chung'
                  const isOutOfStock = (product.stockQuantity ?? 0) <= 0
                  const isLowStock =
                    !isOutOfStock && (product.stockQuantity ?? 0) <= 10
                  const isActive = product.active !== false

                  return (
                    <tr
                      key={product.id}
                      className="hover:bg-slate-50/60 transition-colors group"
                    >
                      {/* Mã ID */}
                      <td className="py-3 px-4 font-mono text-xs font-semibold text-slate-400 tabular-nums">
                        #{product.id}
                      </td>

                      {/* Hình ảnh & Tên sản phẩm */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200/80 overflow-hidden shrink-0 flex items-center justify-center relative">
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
                          <div className="min-w-0 flex-1">
                            <div className="font-bold text-slate-800 line-clamp-1 group-hover:text-orange-600 transition-colors">
                              {product.name}
                            </div>
                            <div className="text-xs text-slate-400 line-clamp-1 mt-0.5">
                              {product.description || 'Chưa có mô tả chi tiết'}
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Danh mục */}
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                          {categoryName}
                        </span>
                      </td>

                      {/* Giá bán */}
                      <td className="py-3 px-4 text-right font-extrabold text-slate-900 tabular-nums">
                        {formatVND(product.price)}
                      </td>

                      {/* Tồn kho */}
                      <td className="py-3 px-4 text-center">
                        {isOutOfStock ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-700 border border-red-200 tabular-nums">
                            <AlertCircle className="w-3 h-3" /> Hết hàng (0)
                          </span>
                        ) : isLowStock ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-700 border border-amber-200 tabular-nums">
                            <AlertTriangle className="w-3 h-3" /> Còn {product.stockQuantity}
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 tabular-nums">
                            {product.stockQuantity} SP
                          </span>
                        )}
                      </td>

                      {/* Trạng thái */}
                      <td className="py-3 px-4 text-center">
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

                      {/* Thao tác */}
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(product)}
                            className="p-1.5 text-slate-500 hover:text-orange-600 hover:bg-orange-50 rounded-lg transition-colors cursor-pointer"
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

        {/* Phân trang (Pagination) */}
        {totalPages > 1 && (
          <div className="p-4 bg-slate-50/60 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
            <div>
              Hiển thị{' '}
              <span className="font-semibold text-slate-800">
                {page * pageSize + 1}
              </span>{' '}
              -{' '}
              <span className="font-semibold text-slate-800">
                {Math.min((page + 1) * pageSize, totalElements)}
              </span>{' '}
              trong tổng số{' '}
              <span className="font-semibold text-slate-800">{totalElements}</span>{' '}
              sản phẩm
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                title="Trang trước"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <span className="font-semibold text-slate-700 px-2 tabular-nums">
                Trang {page + 1} / {totalPages}
              </span>

              <button
                type="button"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
                title="Trang sau"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ==================================================================== */}
      {/* 4. Modal Thêm / Chỉnh sửa Sản phẩm */}
      {/* ==================================================================== */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
          {/* Lớp phủ Backdrop */}
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
            onClick={handleCloseModal}
          />

          {/* Hộp thoại Modal */}
          <div className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/60">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
                  <Package className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    {editingProduct
                      ? `Chỉnh sửa sản phẩm #${editingProduct.id}`
                      : 'Thêm sản phẩm mới'}
                  </h3>
                  <p className="text-xs text-slate-500">
                    {editingProduct
                      ? 'Cập nhật thông tin chi tiết và trạng thái hiển thị.'
                      : 'Nhập thông tin sản phẩm để niêm yết lên hệ thống thương mại.'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCloseModal}
                disabled={isSubmitting}
                className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body: Form */}
            <form onSubmit={handleSubmitForm} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              {/* Tên sản phẩm */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Tên sản phẩm <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Áo thun nam Rikkei Cotton Premium"
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                    formErrors.name
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                  }`}
                />
                {formErrors.name && (
                  <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    <span>{formErrors.name}</span>
                  </p>
                )}
              </div>

              {/* Danh mục & Trạng thái */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Danh mục <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) =>
                      setFormData({ ...formData, categoryId: e.target.value })
                    }
                    className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all cursor-pointer ${
                      formErrors.categoryId
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                    }`}
                  >
                    <option value="">-- Chọn danh mục --</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {cat.name}
                      </option>
                    ))}
                  </select>
                  {formErrors.categoryId && (
                    <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{formErrors.categoryId}</span>
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Trạng thái hiển thị
                  </label>
                  <div className="flex items-center gap-3 pt-1">
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={formData.active}
                        onChange={(e) =>
                          setFormData({ ...formData, active: e.target.checked })
                        }
                        className="w-4 h-4 text-orange-600 rounded-sm focus:ring-orange-500 cursor-pointer"
                      />
                      <span className="text-sm font-semibold text-slate-700">
                        {formData.active ? 'Đang mở bán' : 'Tạm ẩn sản phẩm'}
                      </span>
                    </label>
                  </div>
                </div>
              </div>

              {/* Giá bán & Số lượng tồn kho */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Giá bán (VNĐ) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="1"
                    placeholder="Ví dụ: 250000"
                    value={formData.price}
                    onChange={(e) =>
                      setFormData({ ...formData, price: e.target.value })
                    }
                    className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all tabular-nums ${
                      formErrors.price
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                    }`}
                  />
                  {formErrors.price ? (
                    <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{formErrors.price}</span>
                    </p>
                  ) : (
                    formData.price && (
                      <p className="text-xs text-slate-500 mt-1 font-semibold tabular-nums">
                        = {formatVND(formData.price)}
                      </p>
                    )
                  )}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Số lượng tồn kho <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="0"
                    placeholder="Ví dụ: 100"
                    value={formData.stockQuantity}
                    onChange={(e) =>
                      setFormData({ ...formData, stockQuantity: e.target.value })
                    }
                    className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all tabular-nums ${
                      formErrors.stockQuantity
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                    }`}
                  />
                  {formErrors.stockQuantity && (
                    <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{formErrors.stockQuantity}</span>
                    </p>
                  )}
                </div>
              </div>

              {/* Đường dẫn hình ảnh & Xem trước */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  URL Hình ảnh sản phẩm
                </label>
                <div className="flex gap-3 items-start">
                  <div className="flex-1">
                    <input
                      type="url"
                      placeholder="https://images.unsplash.com/photo-..."
                      value={formData.imageUrl}
                      onChange={(e) =>
                        setFormData({ ...formData, imageUrl: e.target.value })
                      }
                      className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        formErrors.imageUrl
                          ? 'border-red-500 focus:ring-red-500/20'
                          : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                      }`}
                    />
                    {formErrors.imageUrl && (
                      <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                        <AlertCircle className="w-3.5 h-3.5" />
                        <span>{formErrors.imageUrl}</span>
                      </p>
                    )}
                  </div>
                  {/* Khung xem trước ảnh */}
                  <div className="w-12 h-10 rounded-xl bg-slate-100 border border-slate-300 overflow-hidden flex items-center justify-center shrink-0">
                    {formData.imageUrl ? (
                      <img
                        src={formData.imageUrl}
                        alt="Xem trước"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.onerror = null
                          e.currentTarget.src =
                            'https://images.unsplash.com/photo-1560343090-f0409e92791a?w=100&auto=format&fit=crop&q=60'
                        }}
                      />
                    ) : (
                      <Eye className="w-4 h-4 text-slate-400" />
                    )}
                  </div>
                </div>
              </div>

              {/* Mô tả chi tiết */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Mô tả sản phẩm
                </label>
                <textarea
                  rows={3}
                  placeholder="Mô tả chất liệu, kiểu dáng, tính năng nổi bật..."
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  className="w-full px-3.5 py-2 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all text-slate-800"
                />
              </div>

              {/* Modal Footer Buttons */}
              <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  disabled={isSubmitting}
                  className="px-4 py-2 border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-2 px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting && (
                    <LoadingSpinner size="sm" className="text-white" />
                  )}
                  <span>
                    {isSubmitting
                      ? 'Đang lưu...'
                      : editingProduct
                      ? 'Cập nhật'
                      : 'Tạo sản phẩm'}
                  </span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
            <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-xl p-3 mt-3 text-center">
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
