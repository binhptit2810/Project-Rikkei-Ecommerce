import React, { useState, useEffect, useCallback } from 'react'
import {
  Package,
  X,
  AlertCircle,
  Eye,
} from 'lucide-react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../common/LoadingSpinner'

/**
 * Modal Thêm / Chỉnh sửa Sản phẩm dành cho Admin (ProductModal).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D07-DDM] Xây dựng Modal Form Thêm/Sửa Sản phẩm trên Giao diện Admin
 *
 * @param {boolean} isOpen - Trạng thái hiển thị modal
 * @param {Function} onClose - Hàm đóng modal
 * @param {Object|null} product - Dữ liệu sản phẩm cần sửa (null nếu là thêm mới)
 * @param {Array} categories - Danh sách danh mục (nếu có từ cha truyền vào)
 * @param {Function} onSuccess - Callback được gọi khi thêm/sửa thành công
 * @param {Function} onError - Callback hiển thị thông báo lỗi
 */
export const ProductModal = ({
  isOpen,
  onClose,
  product = null,
  categories: initialCategories = [],
  onSuccess,
  onError,
}) => {
  const isEditing = Boolean(product && product.id)

  // Danh mục sản phẩm cho dropdown select
  const [categories, setCategories] = useState(initialCategories)
  const [isLoadingCategories, setIsLoadingCategories] = useState(false)

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    price: '',
    stockQuantity: '',
    imageUrl: '',
    description: '',
    active: true,
  })

  // Validation errors
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Định dạng tiền tệ VNĐ hiển thị tức thì
  const formatVND = (amount) => {
    if (amount === null || amount === undefined || amount === '' || isNaN(amount)) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount)
  }

  // Tải danh mục nếu cha chưa truyền hoặc rỗng
  const loadCategories = useCallback(async () => {
    if (initialCategories && initialCategories.length > 0) {
      setCategories(initialCategories)
      return
    }

    setIsLoadingCategories(true)
    try {
      let res
      try {
        res = await categoryService.getAdminCategories()
      } catch (adminErr) {
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
      console.warn('ProductModal: Không thể tải danh sách danh mục:', err)
    } finally {
      setIsLoadingCategories(false)
    }
  }, [initialCategories])

  // Khởi tạo hoặc nạp lại form khi mở modal / đổi product
  useEffect(() => {
    if (!isOpen) return

    loadCategories()

    if (product) {
      // Chế độ Chỉnh sửa sản phẩm
      setFormData({
        name: product.name || '',
        categoryId: product.categoryId ? String(product.categoryId) : '',
        price: product.price !== undefined && product.price !== null ? String(product.price) : '',
        stockQuantity: product.stockQuantity !== undefined && product.stockQuantity !== null ? String(product.stockQuantity) : '',
        imageUrl: product.imageUrl || '',
        description: product.description || '',
        active: product.active !== false,
      })
    } else {
      // Chế độ Thêm sản phẩm mới
      setFormData({
        name: '',
        categoryId: '',
        price: '',
        stockQuantity: '0',
        imageUrl: '',
        description: '',
        active: true,
      })
    }
    setFormErrors({})
  }, [isOpen, product, loadCategories])

  // Lắng nghe phím ESC để đóng modal an toàn
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && !isSubmitting) {
        onClose()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isSubmitting, onClose])

  // Validate form dữ liệu theo đúng yêu cầu nghiệp vụ
  const validateForm = () => {
    const errors = {}

    // 1. Tên sản phẩm bắt buộc
    if (!formData.name || !formData.name.trim()) {
      errors.name = 'Tên sản phẩm không được để trống'
    }

    // 2. Danh mục bắt buộc chọn
    if (!formData.categoryId) {
      errors.categoryId = 'Vui lòng chọn danh mục hợp lệ'
    }

    // 3. Giá bán yêu cầu > 0
    const priceNum = Number(formData.price)
    if (formData.price === '' || formData.price === null || isNaN(priceNum) || priceNum <= 0) {
      errors.price = 'Giá sản phẩm phải lớn hơn 0'
    }

    // 4. Số lượng tồn kho yêu cầu >= 0
    const stockNum = Number(formData.stockQuantity)
    if (
      formData.stockQuantity === '' ||
      formData.stockQuantity === null ||
      isNaN(stockNum) ||
      stockNum < 0
    ) {
      errors.stockQuantity = 'Số lượng tồn kho không được âm'
    }

    // 5. URL hình ảnh nếu có phải hợp lệ
    if (formData.imageUrl && !formData.imageUrl.match(/^(https?:\/\/|\/)/i)) {
      errors.imageUrl = 'Đường dẫn ảnh phải bắt đầu bằng http://, https:// hoặc /'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  // Submit form Thêm / Sửa
  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validateForm() || isSubmitting) return

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
      if (isEditing) {
        // Cập nhật sản phẩm: PUT /api/v1/admin/products/{id}
        await productService.updateProduct(product.id, payload)
        if (onSuccess) {
          onSuccess(`Đã cập nhật sản phẩm "${payload.name}" (#${product.id}) thành công!`)
        }
      } else {
        // Thêm sản phẩm mới: POST /api/v1/admin/products
        const res = await productService.createProduct(payload)
        const newProduct = res?.data
        if (onSuccess) {
          onSuccess(`Đã thêm mới sản phẩm "${payload.name}" thành công!`, newProduct)
        }
      }
      onClose()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi lưu sản phẩm. Vui lòng thử lại.'
      if (onError) {
        onError(msg)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
      {/* Lớp phủ Backdrop - click ra ngoài để đóng */}
      <div
        className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
        onClick={() => !isSubmitting && onClose()}
        aria-hidden="true"
      />

      {/* Hộp thoại Modal */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-modal-title"
        className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-2xl overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-200"
      >
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/60">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-zinc-100 text-zinc-900 flex items-center justify-center shrink-0">
              <Package className="w-5 h-5" />
            </div>
            <div>
              <h3 id="product-modal-title" className="text-base font-bold text-slate-900">
                {isEditing ? `Chỉnh sửa sản phẩm #${product.id}` : 'Thêm sản phẩm mới'}
              </h3>
              <p className="text-xs text-slate-500">
                {isEditing
                  ? 'Cập nhật thông tin chi tiết, giá bán và trạng thái kinh doanh.'
                  : 'Nhập thông tin sản phẩm để niêm yết lên hệ thống gian hàng.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer disabled:opacity-40"
            title="Đóng (ESC)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body: Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Tên sản phẩm */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
              Tên sản phẩm <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              placeholder="Ví dụ: Áo thun nam Rikkei Cotton Premium"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.name
                  ? 'border-red-500 focus:ring-red-500/20'
                  : 'border-slate-300 focus:ring-zinc-950/20 focus:border-zinc-900'
                }`}
            />
            {formErrors.name && (
              <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>{formErrors.name}</span>
              </p>
            )}
          </div>

          {/* Danh mục & Trạng thái kinh doanh */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Danh mục <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                disabled={isLoadingCategories}
                className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all cursor-pointer ${formErrors.categoryId
                    ? 'border-red-500 focus:ring-red-500/20'
                    : 'border-slate-300 focus:ring-zinc-950/20 focus:border-zinc-900'
                  }`}
              >
                <option value="">
                  {isLoadingCategories ? '-- Đang tải danh mục... --' : '-- Chọn danh mục --'}
                </option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name} {cat.active === false ? '(Đã ẩn)' : ''}
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
                Trạng thái kinh doanh
              </label>
              <div className="flex items-center gap-3 pt-1">
                <label className="inline-flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    className="w-4 h-4 text-zinc-900 rounded-sm focus:ring-zinc-950 cursor-pointer"
                  />
                  <span className="text-sm font-semibold text-slate-700">
                    {formData.active ? 'Đang bán (Hiển thị)' : 'Tạm ngưng (Đã ẩn)'}
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
                step="1"
                placeholder="Ví dụ: 250000"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all tabular-nums ${formErrors.price
                    ? 'border-red-500 focus:ring-red-500/20'
                    : 'border-slate-300 focus:ring-zinc-950/20 focus:border-zinc-900'
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
                Số lượng tồn kho ban đầu <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                min="0"
                step="1"
                placeholder="Ví dụ: 100"
                value={formData.stockQuantity}
                onChange={(e) => setFormData({ ...formData, stockQuantity: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all tabular-nums ${formErrors.stockQuantity
                    ? 'border-red-500 focus:ring-red-500/20'
                    : 'border-slate-300 focus:ring-zinc-950/20 focus:border-zinc-900'
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

          {/* URL Hình ảnh & Image Preview tức thì */}
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
                  onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.imageUrl
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-300 focus:ring-zinc-950/20 focus:border-zinc-900'
                    }`}
                />
                {formErrors.imageUrl && (
                  <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    <span>{formErrors.imageUrl}</span>
                  </p>
                )}
              </div>
              {/* Khung xem trước ảnh tức thì */}
              <div
                className="w-12 h-10 rounded-xl bg-slate-100 border border-slate-300 overflow-hidden flex items-center justify-center shrink-0"
                title="Khung xem trước hình ảnh"
              >
                {formData.imageUrl ? (
                  <img
                    src={formData.imageUrl}
                    alt="Xem trước hình ảnh sản phẩm"
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
              Mô tả chi tiết sản phẩm
            </label>
            <textarea
              rows={3}
              placeholder="Mô tả chất liệu, kiểu dáng, tính năng nổi bật..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-slate-800"
            />
          </div>

          {/* Modal Footer Buttons */}
          <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer disabled:opacity-40"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 px-5 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting && <LoadingSpinner size="sm" className="text-white" />}
              <span>
                {isSubmitting
                  ? 'Đang lưu...'
                  : isEditing
                    ? 'Cập nhật'
                    : 'Lưu sản phẩm'}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ProductModal

