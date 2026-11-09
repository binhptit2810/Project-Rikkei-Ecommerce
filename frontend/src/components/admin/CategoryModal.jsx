import React, { useState, useEffect, useRef } from 'react'
import {
  Tags,
  X,
  AlertCircle,
  Eye,
  UploadCloud,
  CheckCircle2,
  Link2,
  Trash2,
} from 'lucide-react'
import categoryService from '../../services/categoryService'
import uploadService from '../../services/uploadService'
import LoadingSpinner from '../common/LoadingSpinner'

/**
 * Modal Thêm / Chỉnh sửa Danh mục dành cho Admin (CategoryModal).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D09-DDM] Hoàn thiện Giao diện Quản trị Danh mục
 *
 * @param {boolean} isOpen - Trạng thái hiển thị modal
 * @param {Function} onClose - Hàm đóng modal
 * @param {Object|null} category - Dữ liệu danh mục cần sửa (null nếu là thêm mới)
 * @param {Function} onSuccess - Callback được gọi khi thêm/sửa thành công
 * @param {Function} onError - Callback hiển thị thông báo lỗi
 */
export const CategoryModal = ({
  isOpen,
  onClose,
  category = null,
  onSuccess,
  onError,
}) => {
  const isEditing = Boolean(category && category.id)

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    imageUrl: '',
    active: true,
  })

  // Validation errors
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Quản lý upload ảnh từ máy tính hoặc URL
  const [imageTab, setImageTab] = useState('upload') // 'upload' | 'url'
  const [isUploadingImage, setIsUploadingImage] = useState(false)
  const [uploadSuccessMessage, setUploadSuccessMessage] = useState(null)
  const fileInputRef = useRef(null)

  const handleFileUpload = async (file) => {
    if (!file) return

    if (!file.type.match(/^image\/(jpeg|png|webp|gif)$/i)) {
      setFormErrors((prev) => ({
        ...prev,
        imageUrl: 'Chỉ chấp nhận tệp hình ảnh (.jpg, .jpeg, .png, .webp, .gif)',
      }))
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      setFormErrors((prev) => ({
        ...prev,
        imageUrl: 'Dung lượng ảnh vượt quá 10MB. Vui lòng chọn ảnh nhỏ hơn.',
      }))
      return
    }

    setIsUploadingImage(true)
    setFormErrors((prev) => {
      const copy = { ...prev }
      delete copy.imageUrl
      return copy
    })
    setUploadSuccessMessage(null)

    try {
      const res = await uploadService.uploadImage(file)
      if (res?.data?.url) {
        setFormData((prev) => ({ ...prev, imageUrl: res.data.url }))
        setUploadSuccessMessage('Đã tải ảnh lên máy tính thành công!')
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải ảnh lên máy chủ. Vui lòng thử lại.'
      setFormErrors((prev) => ({ ...prev, imageUrl: msg }))
    } finally {
      setIsUploadingImage(false)
    }
  }

  const handleFileChange = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      handleFileUpload(file)
    }
    e.target.value = ''
  }

  const handleClearImage = () => {
    setFormData((prev) => ({ ...prev, imageUrl: '' }))
    setUploadSuccessMessage(null)
    setFormErrors((prev) => {
      const copy = { ...prev }
      delete copy.imageUrl
      return copy
    })
  }

  // Khởi tạo hoặc nạp lại form khi mở modal / đổi category
  useEffect(() => {
    if (!isOpen) return

    if (category) {
      // Chế độ Chỉnh sửa danh mục
      setFormData({
        name: category.name || '',
        description: category.description || '',
        imageUrl: category.imageUrl || '',
        active: category.active !== false,
      })
    } else {
      // Chế độ Thêm danh mục mới
      setFormData({
        name: '',
        description: '',
        imageUrl: '',
        active: true,
      })
    }
    setFormErrors({})
  }, [isOpen, category])

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

  // Validate form dữ liệu
  const validateForm = () => {
    const errors = {}

    // 1. Tên danh mục bắt buộc, không được để trống hoặc chỉ có khoảng trắng
    if (!formData.name || !formData.name.trim()) {
      errors.name = 'Tên danh mục không được để trống'
    }

    // 2. URL hình ảnh nếu có phải bắt đầu bằng http://, https:// hoặc /
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
      description: formData.description.trim() || null,
      imageUrl: formData.imageUrl.trim() || null,
      active: Boolean(formData.active),
    }

    try {
      if (isEditing) {
        // Cập nhật danh mục: PUT /api/v1/admin/categories/{id}
        await categoryService.updateCategory(category.id, payload)
        if (onSuccess) {
          onSuccess(`Đã cập nhật danh mục "${payload.name}" (#${category.id}) thành công!`)
        }
      } else {
        // Thêm danh mục mới: POST /api/v1/admin/categories
        const res = await categoryService.createCategory(payload)
        const newCategory = res?.data
        if (onSuccess) {
          onSuccess(`Đã thêm mới danh mục "${payload.name}" thành công!`, newCategory)
        }
      }
      onClose()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi lưu danh mục. Vui lòng thử lại.'
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
        aria-labelledby="category-modal-title"
        className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-lg overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-200"
      >
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/60">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
              <Tags className="w-5 h-5" />
            </div>
            <div>
              <h3 id="category-modal-title" className="text-base font-bold text-slate-900">
                {isEditing ? `Chỉnh sửa danh mục #${category.id}` : 'Thêm danh mục mới'}
              </h3>
              <p className="text-xs text-slate-500">
                {isEditing
                  ? 'Cập nhật tên, mô tả và trạng thái kích hoạt danh mục.'
                  : 'Tạo nhóm danh mục mới để phân loại hàng hóa trên hệ thống.'}
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
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Tên danh mục */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
              Tên danh mục <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              placeholder="Ví dụ: Thời trang nam, Thiết bị điện tử..."
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
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

          {/* Hình ảnh danh mục (Upload từ máy tính hoặc Nhập URL) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Hình ảnh / Icon danh mục
              </label>
              {/* Tab chuyển đổi cách thêm ảnh */}
              <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg border border-slate-200">
                <button
                  type="button"
                  onClick={() => setImageTab('upload')}
                  className={`flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                    imageTab === 'upload'
                      ? 'bg-white text-orange-600 shadow-xs'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                >
                  <UploadCloud className="w-3.5 h-3.5" />
                  <span>Upload máy tính</span>
                </button>
                <button
                  type="button"
                  onClick={() => setImageTab('url')}
                  className={`flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                    imageTab === 'url'
                      ? 'bg-white text-orange-600 shadow-xs'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                >
                  <Link2 className="w-3.5 h-3.5" />
                  <span>Nhập URL</span>
                </button>
              </div>
            </div>

            {/* Input file ẩn */}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/png,image/jpeg,image/webp,image/gif"
              onChange={handleFileChange}
              className="hidden"
            />

            {imageTab === 'upload' ? (
              <div className="space-y-2">
                {formData.imageUrl ? (
                  /* Khi đã có ảnh */
                  <div className="flex items-center gap-3 p-3 bg-slate-50 border border-slate-200 rounded-xl">
                    <div className="w-14 h-14 rounded-lg bg-white border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                      <img
                        src={formData.imageUrl}
                        alt="Ảnh danh mục"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.currentTarget.onerror = null
                          e.currentTarget.src =
                            'https://images.unsplash.com/photo-1544816155-12df9643f363?w=100&auto=format&fit=crop&q=60'
                        }}
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-bold text-slate-800 truncate">
                        {formData.imageUrl.startsWith('/api') || formData.imageUrl.startsWith('/')
                          ? formData.imageUrl.split('/').pop()
                          : formData.imageUrl}
                      </p>
                      <p className="text-[11px] text-emerald-600 font-medium flex items-center gap-1 mt-0.5">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Ảnh hợp lệ và đã sẵn sàng</span>
                      </p>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={isUploadingImage}
                        className="px-2.5 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
                      >
                        Đổi ảnh
                      </button>
                      <button
                        type="button"
                        onClick={handleClearImage}
                        disabled={isUploadingImage}
                        className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                        title="Xóa ảnh"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ) : (
                  /* Vùng kéo thả / chọn file từ máy tính */
                  <div
                    onClick={() => !isUploadingImage && fileInputRef.current?.click()}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault()
                      if (!isUploadingImage && e.dataTransfer.files?.[0]) {
                        handleFileUpload(e.dataTransfer.files[0])
                      }
                    }}
                    className={`border-2 border-dashed rounded-2xl p-4 text-center cursor-pointer transition-all flex flex-col items-center justify-center gap-2 ${
                      isUploadingImage
                        ? 'bg-slate-50 border-slate-300 pointer-events-none'
                        : 'border-slate-300 hover:border-orange-500 bg-slate-50/60 hover:bg-white'
                    }`}
                  >
                    {isUploadingImage ? (
                      <div className="flex flex-col items-center gap-2 py-2">
                        <LoadingSpinner size="md" className="text-orange-600" />
                        <span className="text-xs font-semibold text-slate-600">
                          Đang tải ảnh từ máy tính lên máy chủ...
                        </span>
                      </div>
                    ) : (
                      <>
                        <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center text-orange-600">
                          <UploadCloud className="w-5 h-5" />
                        </div>
                        <div>
                          <p className="text-xs font-bold text-slate-800">
                            Nhấn để chọn ảnh danh mục từ máy tính
                          </p>
                          <p className="text-[11px] text-slate-400 mt-0.5">
                            Hoặc kéo thả file ảnh PNG, JPG, WEBP (Tối đa 10MB)
                          </p>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            ) : (
              /* Tab Nhập URL ảnh */
              <div className="flex gap-3 items-start">
                <div className="flex-1">
                  <input
                    type="url"
                    placeholder="https://images.unsplash.com/photo-..."
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                    className={`w-full px-3.5 py-2 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                      formErrors.imageUrl
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-300 focus:ring-orange-500/20 focus:border-orange-500'
                    }`}
                  />
                </div>
                {/* Khung xem trước ảnh */}
                <div
                  className="w-11 h-10 rounded-xl bg-slate-100 border border-slate-300 overflow-hidden flex items-center justify-center shrink-0"
                  title="Khung xem trước hình ảnh"
                >
                  {formData.imageUrl ? (
                    <img
                      src={formData.imageUrl}
                      alt="Xem trước"
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.currentTarget.onerror = null
                        e.currentTarget.src =
                          'https://images.unsplash.com/photo-1544816155-12df9643f363?w=100&auto=format&fit=crop&q=60'
                      }}
                    />
                  ) : (
                    <Eye className="w-4 h-4 text-slate-400" />
                  )}
                </div>
              </div>
            )}

            {/* Thông báo lỗi / thành công */}
            {formErrors.imageUrl && (
              <p className="text-xs text-red-600 flex items-center gap-1 mt-1">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{formErrors.imageUrl}</span>
              </p>
            )}
            {uploadSuccessMessage && (
              <p className="text-xs text-emerald-600 flex items-center gap-1 mt-1">
                <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                <span>{uploadSuccessMessage}</span>
              </p>
            )}
          </div>

          {/* Mô tả danh mục */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
              Mô tả chi tiết danh mục
            </label>
            <textarea
              rows={3}
              placeholder="Mô tả phạm vi hoặc đối tượng các sản phẩm thuộc danh mục này..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition-all text-slate-800"
            />
          </div>

          {/* Trạng thái kích hoạt (Checkbox / Toggle) */}
          <div className="pt-1">
            <label className="inline-flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={formData.active}
                onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                className="w-4 h-4 text-orange-600 rounded-sm focus:ring-orange-500 cursor-pointer"
              />
              <span className="text-sm font-semibold text-slate-700">
                {formData.active ? 'Đang kích hoạt (Hiển thị)' : 'Tạm ngưng (Tạm ẩn)'}
              </span>
            </label>
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
              className="inline-flex items-center gap-2 px-5 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting && <LoadingSpinner size="sm" className="text-white" />}
              <span>
                {isSubmitting
                  ? 'Đang lưu...'
                  : isEditing
                  ? 'Cập nhật'
                  : 'Lưu danh mục'}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CategoryModal

