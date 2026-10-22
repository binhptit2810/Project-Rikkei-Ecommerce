import React, { useState, useEffect } from 'react'
import {
  Plus,
  CheckCircle2,
  Search,
  Tags,
  Edit2,
  Trash2,
  AlertTriangle,
  X,
  Layers,
} from 'lucide-react'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const AdminCategoriesPage = () => {
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  // Search filter
  const [searchTerm, setSearchTerm] = useState('')

  // Modal State for Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    imageUrl: '',
    active: true,
  })
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Modal State for Delete
  const [deletingCategory, setDeletingCategory] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchCategories = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await categoryService.getAdminCategories()
      if (res.success && res.data) {
        setCategories(res.data)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh mục:', err)
      setError(err.response?.data?.message || 'Không thể tải danh sách danh mục')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchCategories()
  }, [])

  const notifySuccess = (msg) => {
    setSuccessMessage(msg)
    setTimeout(() => setSuccessMessage(null), 4000)
  }

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

  const handleOpenEditModal = (cat) => {
    setEditingCategory(cat)
    setFormData({
      name: cat.name,
      description: cat.description || '',
      imageUrl: cat.imageUrl || '',
      active: cat.active,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  const validateForm = () => {
    const errs = {}
    if (!formData.name.trim()) {
      errs.name = 'Tên danh mục không được để trống'
    } else if (formData.name.length > 100) {
      errs.name = 'Tên danh mục không vượt quá 100 ký tự'
    }
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmitForm = async (e) => {
    e.preventDefault()
    if (!validateForm()) return

    setIsSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim() || null,
        imageUrl: formData.imageUrl.trim() || null,
        active: formData.active,
      }

      if (editingCategory) {
        await categoryService.updateCategory(editingCategory.id, payload)
        notifySuccess(`Cập nhật danh mục "${payload.name}" thành công!`)
      } else {
        await categoryService.createCategory(payload)
        notifySuccess(`Tạo danh mục mới "${payload.name}" thành công!`)
      }

      setIsModalOpen(false)
      fetchCategories()
    } catch (err) {
      console.error('Lỗi lưu danh mục:', err)
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi lưu danh mục'
      setFormErrors((prev) => ({ ...prev, api: msg }))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleConfirmDelete = async () => {
    if (!deletingCategory) return
    setIsDeleting(true)
    try {
      await categoryService.deleteCategory(deletingCategory.id)
      notifySuccess(`Đã xóa / chuyển danh mục "${deletingCategory.name}" sang không hoạt động thành công!`)
      setDeletingCategory(null)
      fetchCategories()
    } catch (err) {
      console.error('Lỗi khi xóa danh mục:', err)
      setError(err.response?.data?.message || 'Không thể xóa danh mục')
    } finally {
      setIsDeleting(false)
    }
  }

  const filteredCategories = categories.filter((c) =>
    c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.description && c.description.toLowerCase().includes(searchTerm.toLowerCase()))
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-2xs">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight">
              Quản lý danh mục
            </h1>
            <span className="bg-orange-50 text-orange-700 text-xs font-bold px-2.5 py-0.5 rounded-full border border-orange-200/60 tabular-nums">
              {categories.length} danh mục
            </span>
          </div>
          <p className="text-slate-500 text-xs sm:text-sm">
            Tạo mới, chỉnh sửa thông tin hoặc quản trị trạng thái danh mục sản phẩm.
          </p>
        </div>

        <button
          onClick={handleOpenCreateModal}
          className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 active:scale-[0.99] text-white font-bold text-xs rounded-xl shadow-2xs transition-all flex items-center justify-center gap-2 cursor-pointer shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm danh mục</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs sm:text-sm font-medium flex items-center justify-between shadow-2xs animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-700 font-bold text-xs hover:underline cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-2xs flex items-center justify-between">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo tên danh mục..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 bg-slate-50/50"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        {loading ? (
          <div className="py-20 flex justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <div className="p-8 text-center text-red-600 text-xs">
            {error}
          </div>
        ) : filteredCategories.length === 0 ? (
          <div className="py-16 text-center text-slate-400">
            <div className="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center mx-auto mb-3">
              <Tags className="w-6 h-6" />
            </div>
            <p className="text-sm font-semibold text-slate-700">Chưa có danh mục nào</p>
            <p className="text-xs text-slate-400 mt-1">Bấm nút "Thêm danh mục" ở trên để bắt đầu.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/70 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Tên danh mục</th>
                  <th className="py-3.5 px-4">Mô tả</th>
                  <th className="py-3.5 px-4">Trạng thái</th>
                  <th className="py-3.5 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
                {filteredCategories.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                    {/* Name & Icon */}
                    <td className="py-3.5 px-4 font-bold text-slate-900">
                      <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-lg bg-slate-100 text-slate-600 flex items-center justify-center shrink-0">
                          <Tags className="w-3.5 h-3.5" />
                        </div>
                        <div>
                          <div>{item.name}</div>
                          <div className="text-[10px] text-slate-400 font-normal">ID: #{item.id}</div>
                        </div>
                      </div>
                    </td>

                    {/* Description */}
                    <td className="py-3.5 px-4 text-slate-500 max-w-xs truncate">
                      {item.description || 'Chưa có mô tả'}
                    </td>

                    {/* Active */}
                    <td className="py-3.5 px-4">
                      {item.active ? (
                        <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700">
                          <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                          Hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-slate-400">
                          <span className="w-2 h-2 rounded-full bg-slate-400"></span>
                          Đang ẩn
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleOpenEditModal(item)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-slate-600 hover:text-orange-600 hover:bg-orange-50 font-semibold rounded-lg transition-colors cursor-pointer"
                        >
                          <Edit2 className="w-3 h-3" />
                          <span>Sửa</span>
                        </button>
                        <button
                          onClick={() => setDeletingCategory(item)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-slate-400 hover:text-red-600 hover:bg-red-50 font-semibold rounded-lg transition-colors cursor-pointer"
                        >
                          <Trash2 className="w-3 h-3" />
                          <span>Xóa</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal: Thêm / Sửa Danh Mục */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 sm:p-7 shadow-dropdown border border-slate-200">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-6">
              <h3 className="text-base font-bold text-slate-900">
                {editingCategory ? 'Chỉnh sửa danh mục' : 'Thêm danh mục mới'}
              </h3>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {formErrors.api && (
              <div className="mb-4 p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                {formErrors.api}
              </div>
            )}

            <form onSubmit={handleSubmitForm} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Tên danh mục <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ví dụ: Thiết bị công nghệ..."
                  className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 ${
                    formErrors.name ? 'border-red-400 bg-red-50/30' : 'border-slate-200'
                  }`}
                />
                {formErrors.name && (
                  <p className="text-[11px] text-red-600 mt-1">{formErrors.name}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Mô tả danh mục
                </label>
                <textarea
                  rows="3"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Mô tả các sản phẩm thuộc danh mục này..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 resize-none"
                ></textarea>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Đường dẫn ảnh đại diện (Image URL)
                </label>
                <input
                  type="url"
                  value={formData.imageUrl}
                  onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  placeholder="https://example.com/category.jpg"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="activeCatCheck"
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  className="w-4 h-4 text-orange-600 rounded border-slate-300 focus:ring-orange-500 cursor-pointer"
                />
                <label htmlFor="activeCatCheck" className="text-xs font-semibold text-slate-700 cursor-pointer">
                  Kích hoạt danh mục (Hiển thị cho khách hàng chọn)
                </label>
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  disabled={isSubmitting}
                  className="px-4 py-2.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-orange-600 hover:bg-orange-700 rounded-xl shadow-2xs transition-all cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting ? 'Đang lưu...' : editingCategory ? 'Cập nhật' : 'Tạo danh mục'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Xác nhận Xóa Danh Mục */}
      {deletingCategory && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/40 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-dropdown border border-slate-200 text-center">
            <div className="w-12 h-12 rounded-xl bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-3">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-slate-900 mb-1">
              Xóa danh mục "{deletingCategory.name}"?
            </h3>
            <p className="text-xs text-slate-500 mb-6 leading-relaxed">
              Nếu danh mục đã có sản phẩm liên kết, hệ thống sẽ tự động chuyển sang trạng thái <strong>ẩn</strong> để bảo toàn dữ liệu sản phẩm. Nếu chưa có sản phẩm nào, danh mục sẽ được xóa hoàn toàn.
            </p>

            <div className="flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setDeletingCategory(null)}
                disabled={isDeleting}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="px-5 py-2 text-xs font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl shadow-2xs transition-all cursor-pointer disabled:opacity-50"
              >
                {isDeleting ? 'Đang xử lý...' : 'Xác nhận xóa'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminCategoriesPage
