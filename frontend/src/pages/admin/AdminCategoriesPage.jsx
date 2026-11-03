import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Tags,
  Plus,
  RefreshCw,
  Search,
  Edit2,
  Trash2,
  AlertCircle,
  CheckCircle2,
  X,
  Image as ImageIcon,
  AlertTriangle,
  Eye,
} from 'lucide-react'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang Quản lý Danh mục Sản phẩm dành cho Admin (AdminCategoriesPage).
 * Tuyến đường (Route): /admin/categories
 * Phong cách: shadcn/ui New York - Neutral/Zinc Monochrome, đồng bộ hoàn hảo toàn hệ thống.
 */
export const AdminCategoriesPage = () => {
  // --- Dữ liệu danh mục ---
  const [categories, setCategories] = useState([])
  const [searchTerm, setSearchTerm] = useState('')

  // --- Trạng thái giao diện ---
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // --- Trạng thái Modal Thêm / Sửa ---
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null) // null = Thêm mới, object = Sửa
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    imageUrl: '',
    active: true,
  })
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // --- Trạng thái Modal Xác nhận Xóa / Ẩn ---
  const [deletingCategory, setDeletingCategory] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Tải danh sách danh mục với fallback
  const fetchCategories = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      let res
      let isFallback = false

      try {
        res = await categoryService.getAdminCategories()
      } catch (adminErr) {
        const status = adminErr.response?.status
        if (status === 404 || status === 403 || !adminErr.response) {
          isFallback = true
          res = await categoryService.getCategories()
        } else {
          throw adminErr
        }
      }

      if (res?.data) {
        if (Array.isArray(res.data)) {
          setCategories(res.data)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setCategories(res.data.content)
        } else {
          setCategories([])
        }
      } else {
        setCategories([])
      }

      if (isFallback) {
        console.info('AdminCategoriesPage: Đang kết nối fallback qua GET /api/v1/categories')
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách danh mục từ hệ thống.'
      setError(msg)
      setCategories([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

  // Tự động đóng toast sau 4 giây
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [toastMessage])

  // Lọc dữ liệu danh mục phía client theo từ khóa
  const filteredCategories = useMemo(() => {
    if (!categories || categories.length === 0) return []
    const term = searchTerm.trim().toLowerCase()
    if (!term) return categories

    return categories.filter((cat) => {
      const matchName = cat.name && cat.name.toLowerCase().includes(term)
      const matchDesc =
        cat.description && cat.description.toLowerCase().includes(term)
      return matchName || matchDesc
    })
  }, [categories, searchTerm])

  // Mở modal Thêm mới
  const handleOpenCreateModal = () => {
    setEditingCategory(null)
    setFormData({
      name: '',
      description: '',
      imageUrl: '',
      active: true,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Mở modal Sửa
  const handleOpenEditModal = (category) => {
    setEditingCategory(category)
    setFormData({
      name: category.name || '',
      description: category.description || '',
      imageUrl: category.imageUrl || '',
      active: category.active !== false,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Đóng modal
  const handleCloseModal = () => {
    if (isSubmitting) return
    setIsModalOpen(false)
    setEditingCategory(null)
    setFormErrors({})
  }

  // Validate form
  const validateForm = () => {
    const errors = {}

    if (!formData.name.trim()) {
      errors.name = 'Vui lòng nhập tên danh mục'
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
      description: formData.description.trim() || null,
      imageUrl: formData.imageUrl.trim() || null,
      active: Boolean(formData.active),
    }

    try {
      if (editingCategory) {
        await categoryService.updateCategory(editingCategory.id, payload)
        setToastMessage({
          type: 'success',
          text: `Đã cập nhật danh mục "${payload.name}" thành công!`,
        })
      } else {
        await categoryService.createCategory(payload)
        setToastMessage({
          type: 'success',
          text: `Đã tạo danh mục "${payload.name}" thành công!`,
        })
      }
      setIsModalOpen(false)
      fetchCategories()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi lưu danh mục. Vui lòng thử lại.'
      setToastMessage({ type: 'error', text: msg })
    } finally {
      setIsSubmitting(false)
    }
  }

  // Xác nhận Ẩn / Xóa danh mục
  const handleConfirmDelete = async () => {
    if (!deletingCategory) return

    setIsDeleting(true)
    try {
      await categoryService.deleteCategory(deletingCategory.id)
      setToastMessage({
        type: 'success',
        text: `Đã ẩn / xóa danh mục "${deletingCategory.name}" (#${deletingCategory.id}) thành công!`,
      })
      setDeletingCategory(null)
      fetchCategories()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể xóa danh mục. Vui lòng thử lại.'
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
          className={`fixed top-20 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-md shadow-md border text-sm font-medium transition-all ${
            toastMessage.type === 'success'
              ? 'bg-zinc-900 border-zinc-800 text-zinc-50'
              : 'bg-red-50 border-red-200 text-red-900'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          )}
          <span>{toastMessage.text}</span>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-zinc-200 cursor-pointer ml-2"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 1. Tiêu đề Trang & Nút hành động */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-zinc-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-zinc-950 tracking-tight">
              Quản lý danh mục
            </h1>
            <span className="badge-neutral">
              <Tags className="w-3.5 h-3.5" /> {categories.length} danh mục
            </span>
          </div>
          <p className="text-sm text-zinc-500 mt-1">
            Quản lý cây phân loại sản phẩm, ảnh đại diện và trạng thái hiển thị trên toàn bộ gian hàng.
          </p>
        </div>

        {/* Nút thao tác */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchCategories}
            disabled={isLoading}
            className="btn-outline"
            title="Tải lại danh sách danh mục"
          >
            <RefreshCw
              className={`w-4 h-4 ${isLoading ? 'animate-spin text-zinc-900' : 'text-zinc-500'}`}
            />
            <span>Làm mới</span>
          </button>

          <button
            type="button"
            onClick={handleOpenCreateModal}
            className="btn-primary"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm danh mục</span>
          </button>
        </div>
      </div>

      {/* 2. Thanh tìm kiếm */}
      <div className="bg-white p-3.5 rounded-lg border border-zinc-200 shadow-sm my-6 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:max-w-md">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo tên hoặc mô tả danh mục..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-standard pl-9"
          />
        </div>

        {searchTerm && (
          <button
            type="button"
            onClick={() => setSearchTerm('')}
            className="btn-secondary text-xs py-1.5 px-3"
          >
            <X className="w-3.5 h-3.5" />
            <span>Xóa tìm kiếm</span>
          </button>
        )}
      </div>

      {/* 3. Bảng dữ liệu danh mục */}
      <div className="bg-white rounded-lg border border-zinc-200 shadow-sm overflow-hidden">
        {error && (
          <div className="p-4 bg-red-50 border-b border-red-200 text-red-900 text-sm flex items-center gap-3">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span className="flex-1">{error}</span>
            <button
              type="button"
              onClick={fetchCategories}
              className="text-xs font-semibold underline hover:text-red-950 cursor-pointer"
            >
              Thử lại
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-zinc-500">
            <LoadingSpinner size="lg" className="text-zinc-900 mb-3" />
            <p className="text-sm font-medium">Đang tải danh sách danh mục...</p>
          </div>
        ) : filteredCategories.length === 0 ? (
          <div className="py-16 text-center px-4">
            <div className="w-12 h-12 rounded-lg bg-zinc-100 text-zinc-500 flex items-center justify-center mx-auto mb-3">
              <Tags className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-semibold text-zinc-900">
              Không tìm thấy danh mục nào
            </h3>
            <p className="text-xs text-zinc-500 max-w-md mx-auto mt-1 mb-4">
              {searchTerm
                ? 'Không có danh mục nào khớp với từ khóa tìm kiếm của bạn.'
                : 'Chưa có danh mục nào trong hệ thống. Hãy tạo danh mục đầu tiên.'}
            </p>
            {searchTerm ? (
              <button
                type="button"
                onClick={() => setSearchTerm('')}
                className="btn-secondary text-xs"
              >
                Xóa tìm kiếm
              </button>
            ) : (
              <button
                type="button"
                onClick={handleOpenCreateModal}
                className="btn-primary text-xs"
              >
                <Plus className="w-4 h-4" />
                <span>Thêm danh mục mới</span>
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-50/80 border-b border-zinc-200 text-xs font-semibold text-zinc-600">
                  <th className="py-3 px-4 w-16">#ID</th>
                  <th className="py-3 px-4 w-20 text-center">Hình ảnh</th>
                  <th className="py-3 px-4 min-w-[200px]">Tên danh mục</th>
                  <th className="py-3 px-4 min-w-[260px]">Mô tả</th>
                  <th className="py-3 px-4 min-w-[120px] text-center">Trạng thái</th>
                  <th className="py-3 px-4 w-28 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-200 text-xs sm:text-sm">
                {filteredCategories.map((category) => {
                  const isActive = category.active !== false

                  return (
                    <tr
                      key={category.id}
                      className="hover:bg-zinc-50/80 transition-colors"
                    >
                      <td className="py-3 px-4 font-mono text-xs text-zinc-400 tabular-nums">
                        #{category.id}
                      </td>

                      <td className="py-3 px-4 text-center">
                        <div className="w-9 h-9 rounded-md bg-zinc-100 border border-zinc-200 overflow-hidden mx-auto flex items-center justify-center">
                          {category.imageUrl ? (
                            <img
                              src={category.imageUrl}
                              alt={category.name}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.currentTarget.onerror = null
                                e.currentTarget.src =
                                  'https://images.unsplash.com/photo-1544816155-12df9643f363?w=100&auto=format&fit=crop&q=60'
                              }}
                            />
                          ) : (
                            <ImageIcon className="w-4 h-4 text-zinc-300" />
                          )}
                        </div>
                      </td>

                      <td className="py-3 px-4 font-medium text-zinc-950">
                        {category.name}
                      </td>

                      <td className="py-3 px-4 text-xs text-zinc-500 line-clamp-2">
                        {category.description || 'Chưa có mô tả'}
                      </td>

                      <td className="py-3 px-4 text-center">
                        {isActive ? (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                            Hoạt động
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-zinc-100 text-zinc-600 border border-zinc-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-zinc-400"></span>
                            Đã ẩn
                          </span>
                        )}
                      </td>

                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(category)}
                            className="p-1.5 text-zinc-500 hover:text-zinc-950 hover:bg-zinc-100 rounded-md transition-colors cursor-pointer"
                            title="Chỉnh sửa danh mục"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingCategory(category)}
                            className="p-1.5 text-zinc-500 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors cursor-pointer"
                            title="Ẩn / Xóa danh mục"
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
      </div>

      {/* 4. Modal Thêm / Chỉnh sửa Danh mục */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
          <div
            className="fixed inset-0 bg-zinc-950/60 backdrop-blur-xs transition-opacity"
            onClick={handleCloseModal}
          />

          <div className="relative bg-white rounded-lg shadow-xl border border-zinc-200 w-full max-w-lg overflow-hidden z-10 animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-zinc-200 flex items-center justify-between bg-zinc-50/70">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                  <Tags className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-zinc-950">
                    {editingCategory
                      ? `Chỉnh sửa danh mục #${editingCategory.id}`
                      : 'Thêm danh mục mới'}
                  </h3>
                  <p className="text-xs text-zinc-500">
                    {editingCategory
                      ? 'Cập nhật tên, mô tả và trạng thái danh mục.'
                      : 'Tạo nhóm danh mục mới để phân loại hàng hóa trên hệ thống.'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCloseModal}
                disabled={isSubmitting}
                className="p-1.5 text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 rounded-md transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitForm} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-700 mb-1.5">
                  Tên danh mục <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Thời trang nam, Thiết bị điện tử..."
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  className={`input-standard ${
                    formErrors.name ? 'border-red-500 focus-visible:ring-red-500' : ''
                  }`}
                />
                {formErrors.name && (
                  <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    <span>{formErrors.name}</span>
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-700 mb-1.5">
                  URL Hình ảnh biểu trưng
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
                      className={`input-standard ${
                        formErrors.imageUrl ? 'border-red-500 focus-visible:ring-red-500' : ''
                      }`}
                    />
                    {formErrors.imageUrl && (
                      <p className="text-xs text-red-600 mt-1 flex items-center gap-1">
                        <AlertCircle className="w-3.5 h-3.5" />
                        <span>{formErrors.imageUrl}</span>
                      </p>
                    )}
                  </div>
                  <div className="w-10 h-9 rounded-md bg-zinc-100 border border-zinc-200 overflow-hidden flex items-center justify-center shrink-0">
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
                      <Eye className="w-4 h-4 text-zinc-400" />
                    )}
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-700 mb-1.5">
                  Mô tả danh mục
                </label>
                <textarea
                  rows={3}
                  placeholder="Mô tả phạm vi hoặc đối tượng các sản phẩm thuộc danh mục này..."
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  className="flex w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm shadow-sm placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 resize-none text-zinc-900"
                />
              </div>

              <div className="pt-1">
                <label className="inline-flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.active}
                    onChange={(e) =>
                      setFormData({ ...formData, active: e.target.checked })
                    }
                    className="w-4 h-4 text-zinc-900 rounded border-zinc-300 focus:ring-zinc-950 cursor-pointer"
                  />
                  <span className="text-xs sm:text-sm font-medium text-zinc-800">
                    {formData.active ? 'Hoạt động công khai' : 'Tạm ẩn danh mục'}
                  </span>
                </label>
              </div>

              <div className="pt-4 border-t border-zinc-200 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  disabled={isSubmitting}
                  className="btn-outline"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="btn-primary"
                >
                  {isSubmitting && (
                    <LoadingSpinner size="sm" className="text-zinc-50" />
                  )}
                  <span>
                    {isSubmitting
                      ? 'Đang lưu...'
                      : editingCategory
                      ? 'Cập nhật'
                      : 'Tạo danh mục'}
                  </span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 5. Modal Xác nhận Ẩn / Xóa Danh mục */}
      {deletingCategory && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div
            className="fixed inset-0 bg-zinc-950/60 backdrop-blur-xs transition-opacity"
            onClick={() => !isDeleting && setDeletingCategory(null)}
          />

          <div className="relative bg-white rounded-lg shadow-xl border border-zinc-200 w-full max-w-md p-6 z-10 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-10 h-10 rounded-md bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-3">
              <AlertTriangle className="w-5 h-5" />
            </div>

            <h3 className="text-base font-semibold text-zinc-950 text-center">
              Xác nhận ẩn / xóa danh mục
            </h3>
            <p className="text-xs sm:text-sm text-zinc-600 text-center mt-2 leading-relaxed">
              Bạn có chắc chắn muốn ẩn hoặc xóa danh mục{' '}
              <strong className="text-zinc-950 font-medium">
                "{deletingCategory.name}"
              </strong>{' '}
              (Mã: #{deletingCategory.id})?
            </p>
            <p className="text-xs text-zinc-600 bg-zinc-100 border border-zinc-200 rounded-md p-3 mt-3 text-center">
              Các sản phẩm thuộc danh mục này có thể cần được phân loại lại trước khi ẩn danh mục.
            </p>

            <div className="mt-5 flex items-center justify-center gap-2">
              <button
                type="button"
                onClick={() => setDeletingCategory(null)}
                disabled={isDeleting}
                className="btn-outline"
              >
                Hủy thao tác
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="btn-destructive"
              >
                {isDeleting && (
                  <LoadingSpinner size="sm" className="text-white" />
                )}
                <span>{isDeleting ? 'Đang xử lý...' : 'Xác nhận xóa'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminCategoriesPage
