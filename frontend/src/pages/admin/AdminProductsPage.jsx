import React, { useState, useEffect, useCallback } from 'react'
import productService from '../../services/productService'
import categoryService from '../../services/categoryService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const AdminProductsPage = () => {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  // Filters & Pagination
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [selectedStatus, setSelectedStatus] = useState('ALL') // 'ALL' | 'ACTIVE' | 'INACTIVE'

  // Modal State for Add / Edit
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    imageUrl: '',
    categoryId: '',
    stockQuantity: '0',
    active: true,
  })
  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Modal State for Delete
  const [deletingProduct, setDeletingProduct] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Load categories for dropdown
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await categoryService.getAdminCategories()
        if (res.success && res.data) {
          setCategories(res.data)
        }
      } catch (err) {
        console.error('Không thể tải danh mục:', err)
      }
    }
    fetchCategories()
  }, [])

  // Load products
  const fetchProducts = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        page,
        size: pageSize,
        sortBy: 'createdAt',
        sortDir: 'desc',
      }
      if (searchTerm.trim()) params.keyword = searchTerm.trim()
      if (selectedCategory) params.categoryId = selectedCategory
      if (selectedStatus === 'ACTIVE') params.active = true
      if (selectedStatus === 'INACTIVE') params.active = false

      const res = await productService.getAdminProducts(params)
      if (res.success && res.data) {
        setProducts(res.data.content || [])
        setTotalPages(res.data.totalPages || 1)
        setTotalElements(res.data.totalElements || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách sản phẩm:', err)
      setError(err.response?.data?.message || 'Không thể tải danh sách sản phẩm')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, searchTerm, selectedCategory, selectedStatus])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  const notifySuccess = (msg) => {
    setSuccessMessage(msg)
    setTimeout(() => setSuccessMessage(null), 4000)
  }

  // Handle open Create Modal
  const handleOpenCreateModal = () => {
    setEditingProduct(null)
    setFormData({
      name: '',
      description: '',
      price: '',
      imageUrl: '',
      categoryId: categories.length > 0 ? categories[0].id.toString() : '',
      stockQuantity: '0',
      active: true,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Handle open Edit Modal
  const handleOpenEditModal = (product) => {
    setEditingProduct(product)
    setFormData({
      name: product.name,
      description: product.description || '',
      price: product.price.toString(),
      imageUrl: product.imageUrl || '',
      categoryId: product.categoryId ? product.categoryId.toString() : '',
      stockQuantity: product.stockQuantity != null ? product.stockQuantity.toString() : '0',
      active: product.active,
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  // Validate form
  const validateForm = () => {
    const errs = {}
    if (!formData.name.trim()) {
      errs.name = 'Tên sản phẩm không được để trống'
    } else if (formData.name.length > 200) {
      errs.name = 'Tên sản phẩm tối đa 200 ký tự'
    }

    if (!formData.price || isNaN(formData.price) || Number(formData.price) < 0) {
      errs.price = 'Giá sản phẩm phải là số lớn hơn hoặc bằng 0'
    }

    if (formData.stockQuantity === '' || isNaN(formData.stockQuantity) || Number(formData.stockQuantity) < 0) {
      errs.stockQuantity = 'Số lượng tồn kho không được âm'
    }

    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  // Handle Submit Form (Add or Edit)
  const handleSubmitForm = async (e) => {
    e.preventDefault()
    if (!validateForm()) return

    setIsSubmitting(true)
    try {
      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim() || null,
        price: Number(formData.price),
        imageUrl: formData.imageUrl.trim() || null,
        categoryId: formData.categoryId ? Number(formData.categoryId) : null,
        stockQuantity: Number(formData.stockQuantity),
        active: formData.active,
      }

      if (editingProduct) {
        await productService.updateProduct(editingProduct.id, payload)
        notifySuccess(`Cập nhật sản phẩm "${payload.name}" thành công!`)
      } else {
        await productService.createProduct(payload)
        notifySuccess(`Thêm sản phẩm mới "${payload.name}" thành công!`)
      }

      setIsModalOpen(false)
      fetchProducts()
    } catch (err) {
      console.error('Lỗi lưu sản phẩm:', err)
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi lưu sản phẩm'
      setFormErrors((prev) => ({ ...prev, api: msg }))
    } finally {
      setIsSubmitting(false)
    }
  }

  // Handle Delete (Soft delete)
  const handleConfirmDelete = async () => {
    if (!deletingProduct) return
    setIsDeleting(true)
    try {
      await productService.deleteProduct(deletingProduct.id)
      notifySuccess(`Đã ẩn sản phẩm "${deletingProduct.name}" thành công!`)
      setDeletingProduct(null)
      fetchProducts()
    } catch (err) {
      console.error('Lỗi khi xóa sản phẩm:', err)
      setError(err.response?.data?.message || 'Không thể xóa sản phẩm')
    } finally {
      setIsDeleting(false)
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  // Calculate statistics
  const stats = {
    total: totalElements,
    active: products.filter((p) => p.active).length,
    outOfStock: products.filter((p) => p.stockQuantity <= 0).length,
  }

  return (
    <div className="space-y-6">
      {/* Header & Quick Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              Quản lý sản phẩm
            </h1>
            <span className="bg-orange-100 text-orange-700 text-xs font-bold px-2.5 py-0.5 rounded-full">
              {totalElements} sản phẩm
            </span>
          </div>
          <p className="text-slate-500 text-xs sm:text-sm">
            Thêm mới, chỉnh sửa thông tin, giá bán, danh mục và cập nhật số lượng tồn kho.
          </p>
        </div>

        <button
          onClick={handleOpenCreateModal}
          className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 active:scale-95 text-white font-bold text-sm rounded-xl shadow-sm transition-all flex items-center justify-center gap-2 cursor-pointer shrink-0"
        >
          <span>➕</span>
          <span>Thêm sản phẩm</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm font-medium flex items-center justify-between shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <span>✅</span>
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

      {/* Search & Filter Toolbar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Search Input */}
        <div className="relative w-full md:w-80">
          <input
            type="text"
            placeholder="Tìm theo tên sản phẩm..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setPage(0)
            }}
            className="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500 bg-slate-50/50"
          />
          <span className="absolute left-3 top-2.5 text-slate-400 text-xs">🔍</span>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Category Filter */}
          <select
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value)
              setPage(0)
            }}
            className="text-xs border border-slate-200 rounded-xl px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-orange-500 cursor-pointer"
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          {/* Status Filter */}
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value)
              setPage(0)
            }}
            className="text-xs border border-slate-200 rounded-xl px-3 py-2 bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-orange-500 cursor-pointer"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang kinh doanh</option>
            <option value="INACTIVE">Đã ẩn / Tạm ngừng</option>
          </select>
        </div>
      </div>

      {/* Products Data Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="py-20 flex justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <div className="p-8 text-center text-red-600 text-sm">
            {error}
          </div>
        ) : products.length === 0 ? (
          <div className="py-16 text-center text-slate-400">
            <span className="text-4xl block mb-2">📦</span>
            <p className="text-sm font-semibold text-slate-700">Chưa có sản phẩm nào</p>
            <p className="text-xs text-slate-400 mt-1">Bấm nút "Thêm sản phẩm" ở trên để tạo sản phẩm đầu tiên.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3 px-4">Ảnh & Tên sản phẩm</th>
                  <th className="py-3 px-4">Danh mục</th>
                  <th className="py-3 px-4">Giá bán</th>
                  <th className="py-3 px-4">Tồn kho</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
                {products.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/70 transition-colors">
                    {/* Image & Name */}
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-11 h-11 rounded-lg bg-slate-100 border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                          {item.imageUrl ? (
                            <img
                              src={item.imageUrl}
                              alt={item.name}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.target.onerror = null
                                e.target.src = 'https://placehold.co/100x100?text=SP'
                              }}
                            />
                          ) : (
                            <span className="text-slate-400 text-lg">🛍️</span>
                          )}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 line-clamp-1 max-w-[220px]">
                            {item.name}
                          </div>
                          <div className="text-[10px] text-slate-400">
                            ID: #{item.id}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Category */}
                    <td className="py-3 px-4">
                      {item.categoryName ? (
                        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
                          {item.categoryName}
                        </span>
                      ) : (
                        <span className="text-slate-400 italic">Chưa phân loại</span>
                      )}
                    </td>

                    {/* Price */}
                    <td className="py-3 px-4 font-bold text-orange-600">
                      {formatPrice(item.price)}
                    </td>

                    {/* Stock (Thống nhất với Đặng Thanh Bình) */}
                    <td className="py-3 px-4">
                      {item.stockQuantity <= 0 ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-red-100 text-red-700">
                          Hết hàng ({item.stockQuantity})
                        </span>
                      ) : item.stockQuantity < 10 ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-700">
                          Sắp hết ({item.stockQuantity})
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-100 text-emerald-700">
                          {item.stockQuantity} sp
                        </span>
                      )}
                    </td>

                    {/* Active Status */}
                    <td className="py-3 px-4">
                      {item.active ? (
                        <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700">
                          <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                          Đang bán
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-slate-400">
                          <span className="w-2 h-2 rounded-full bg-slate-400"></span>
                          Đã ẩn
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleOpenEditModal(item)}
                          className="px-2.5 py-1 text-slate-600 hover:text-orange-600 hover:bg-orange-50 font-semibold rounded-lg transition-colors cursor-pointer"
                          title="Sửa sản phẩm"
                        >
                          ✏️ Sửa
                        </button>
                        <button
                          onClick={() => setDeletingProduct(item)}
                          className="px-2.5 py-1 text-slate-400 hover:text-red-600 hover:bg-red-50 font-semibold rounded-lg transition-colors cursor-pointer"
                          title="Ẩn / Xóa sản phẩm"
                        >
                          🗑️ Ẩn
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              Trang {page + 1} / {totalPages} (Tổng {totalElements} sản phẩm)
            </span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Trước
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal: Thêm / Sửa Sản Phẩm */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-slate-100 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-6">
              <h3 className="text-lg font-black text-slate-900">
                {editingProduct ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}
              </h3>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold p-1 cursor-pointer"
              >
                ✕
              </button>
            </div>

            {formErrors.api && (
              <div className="mb-4 p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                {formErrors.api}
              </div>
            )}

            <form onSubmit={handleSubmitForm} className="space-y-4">
              {/* Tên sản phẩm */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Tên sản phẩm <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ví dụ: Áo sơ mi nam Oxford..."
                  className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none focus:ring-2 focus:ring-orange-500 ${
                    formErrors.name ? 'border-red-400 bg-red-50/30' : 'border-slate-200'
                  }`}
                />
                {formErrors.name && (
                  <p className="text-[11px] text-red-600 mt-1">{formErrors.name}</p>
                )}
              </div>

              {/* Danh mục */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Danh mục
                </label>
                <select
                  value={formData.categoryId}
                  onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500 bg-white cursor-pointer"
                >
                  <option value="">-- Chưa chọn danh mục --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Giá và Tồn kho (Cùng hàng) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Giá bán (VNĐ) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="1000"
                    value={formData.price}
                    onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                    placeholder="199000"
                    className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none focus:ring-2 focus:ring-orange-500 ${
                      formErrors.price ? 'border-red-400 bg-red-50/30' : 'border-slate-200'
                    }`}
                  />
                  {formErrors.price && (
                    <p className="text-[11px] text-red-600 mt-1">{formErrors.price}</p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Số lượng tồn kho <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={formData.stockQuantity}
                    onChange={(e) => setFormData({ ...formData, stockQuantity: e.target.value })}
                    placeholder="50"
                    className={`w-full px-3.5 py-2.5 rounded-xl border text-xs focus:outline-none focus:ring-2 focus:ring-orange-500 ${
                      formErrors.stockQuantity ? 'border-red-400 bg-red-50/30' : 'border-slate-200'
                    }`}
                  />
                  {formErrors.stockQuantity && (
                    <p className="text-[11px] text-red-600 mt-1">{formErrors.stockQuantity}</p>
                  )}
                </div>
              </div>

              {/* Đường dẫn ảnh */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Đường dẫn ảnh (Image URL)
                </label>
                <input
                  type="url"
                  value={formData.imageUrl}
                  onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  placeholder="https://example.com/image.jpg"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500"
                />
              </div>

              {/* Mô tả */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Mô tả sản phẩm
                </label>
                <textarea
                  rows="3"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Nhập thông tin chi tiết về chất liệu, nguồn gốc, kích cỡ..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500"
                ></textarea>
              </div>

              {/* Trạng thái Active */}
              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="activeCheck"
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  className="w-4 h-4 text-orange-600 rounded border-slate-300 focus:ring-orange-500 cursor-pointer"
                />
                <label htmlFor="activeCheck" className="text-xs font-semibold text-slate-700 cursor-pointer">
                  Kích hoạt kinh doanh (Hiển thị cho khách hàng xem và mua)
                </label>
              </div>

              {/* Actions Buttons */}
              <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  disabled={isSubmitting}
                  className="px-4 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2.5 text-xs font-bold text-white bg-orange-600 hover:bg-orange-700 rounded-xl shadow-sm transition-all cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting ? 'Đang lưu...' : editingProduct ? 'Cập nhật' : 'Tạo sản phẩm'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Xác nhận Ẩn / Xóa Sản Phẩm */}
      {deletingProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100 text-center">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xl mx-auto mb-3">
              ⚠️
            </div>
            <h3 className="text-base font-black text-slate-900 mb-1">
              Ẩn sản phẩm này?
            </h3>
            <p className="text-xs text-slate-500 mb-6">
              Sản phẩm <strong>"{deletingProduct.name}"</strong> sẽ được chuyển sang trạng thái ẩn (inactive). Khách hàng sẽ không còn tìm thấy hoặc mua được sản phẩm này.
            </p>

            <div className="flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setDeletingProduct(null)}
                disabled={isDeleting}
                className="px-4 py-2.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="px-5 py-2.5 text-xs font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl shadow-sm transition-all cursor-pointer disabled:opacity-50"
              >
                {isDeleting ? 'Đang xử lý...' : 'Xác nhận ẩn'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminProductsPage
