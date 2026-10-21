import apiClient from './apiClient'

/**
 * Service quản lý Danh mục (Category Service).
 * Tuân theo tài liệu API hệ thống (docs/API.md):
 *   - GET /api/v1/categories             — Danh sách danh mục active
 *   - GET /api/v1/categories/{id}        — Chi tiết danh mục
 *   - GET /api/v1/admin/categories       — Toàn bộ danh mục (Admin)
 *   - POST /api/v1/admin/categories      — Thêm danh mục mới (Admin)
 *   - PUT /api/v1/admin/categories/{id}  — Sửa danh mục (Admin)
 *   - DELETE /api/v1/admin/categories/{id} — Xóa danh mục (Admin)
 */
export const categoryService = {
  /**
   * Lấy danh sách danh mục đang hoạt động (Public).
   * @returns {Promise<Object>} ApiResponse<CategoryResponse[]>
   */
  async getCategories() {
    const response = await apiClient.get('/categories')
    return response.data
  },

  /**
   * Lấy thông tin chi tiết một danh mục theo ID.
   * @param {number|string} id - ID danh mục
   * @returns {Promise<Object>} ApiResponse<CategoryResponse>
   */
  async getCategoryById(id) {
    const response = await apiClient.get(`/categories/${id}`)
    return response.data
  },

  /**
   * Lấy danh sách toàn bộ danh mục cho Admin (active và inactive).
   * @returns {Promise<Object>} ApiResponse<CategoryResponse[]>
   */
  async getAdminCategories() {
    const response = await apiClient.get('/admin/categories')
    return response.data
  },

  /**
   * Thêm danh mục mới (Yêu cầu quyền ADMIN).
   * @param {Object} categoryData - { name, description, imageUrl, active }
   * @returns {Promise<Object>} ApiResponse<CategoryResponse>
   */
  async createCategory(categoryData) {
    const response = await apiClient.post('/admin/categories', categoryData)
    return response.data
  },

  /**
   * Cập nhật danh mục (Yêu cầu quyền ADMIN).
   * @param {number|string} id - ID danh mục
   * @param {Object} categoryData - Dữ liệu cập nhật
   * @returns {Promise<Object>} ApiResponse<CategoryResponse>
   */
  async updateCategory(id, categoryData) {
    const response = await apiClient.put(`/admin/categories/${id}`, categoryData)
    return response.data
  },

  /**
   * Xóa danh mục (Yêu cầu quyền ADMIN).
   * @param {number|string} id - ID danh mục
   * @returns {Promise<Object>} ApiResponse
   */
  async deleteCategory(id) {
    const response = await apiClient.delete(`/admin/categories/${id}`)
    return response.data
  },
}

export default categoryService
