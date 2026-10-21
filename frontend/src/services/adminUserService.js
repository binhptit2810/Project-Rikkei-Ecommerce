import apiClient from './apiClient'

/**
 * Service quản lý người dùng dành cho Admin (Admin User Management).
 * 
 * Tuân theo tài liệu API hệ thống (docs/API.md):
 *   - GET /api/v1/admin/users — Lấy danh sách người dùng (Admin)
 *   - GET /api/v1/users/me    — Lấy thông tin tài khoản hiện tại
 */
export const adminUserService = {
  /**
   * Lấy danh sách người dùng trong hệ thống (yêu cầu quyền ADMIN).
   * @param {Object} params - Các tham số phân trang & lọc: { page, size, keyword, role, active, sortBy, sortDir }
   * @returns {Promise<Object>} ApiResponse chứa Page hoặc mảng người dùng
   */
  async getUsers(params = {}) {
    const response = await apiClient.get('/admin/users', { params })
    return response.data
  },

  /**
   * Lấy thông tin hồ sơ của Admin đang đăng nhập.
   * Endpoint thực tế hiện có trên Backend: GET /api/v1/users/me
   * @returns {Promise<Object>} ApiResponse<UserResponse>
   */
  async getCurrentAdmin() {
    const response = await apiClient.get('/users/me')
    return response.data
  },

  /**
   * Lấy thông tin chi tiết của một người dùng theo ID (nếu Backend hỗ trợ sau này).
   * @param {number|string} id - ID người dùng
   * @returns {Promise<Object>} ApiResponse<UserResponse>
   */
  async getUserById(id) {
    const response = await apiClient.get(`/admin/users/${id}`)
    return response.data
  },

  /**
   * Khóa tài khoản người dùng kèm lý do bắt buộc (yêu cầu quyền ADMIN).
   * @param {number|string} id - ID người dùng
   * @param {string} reason - Lý do khóa tài khoản
   * @returns {Promise<Object>} ApiResponse<UserResponse>
   */
  async lockUser(id, reason) {
    const response = await apiClient.patch(`/admin/users/${id}/lock`, { reason })
    return response.data
  },

  /**
   * Mở khóa tài khoản người dùng (yêu cầu quyền ADMIN).
   * @param {number|string} id - ID người dùng
   * @returns {Promise<Object>} ApiResponse<UserResponse>
   */
  async unlockUser(id) {
    const response = await apiClient.patch(`/admin/users/${id}/unlock`)
    return response.data
  },
}

export default adminUserService
