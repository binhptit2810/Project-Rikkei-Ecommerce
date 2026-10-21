import apiClient from './apiClient'

/**
 * Service xử lý các lời gọi API liên quan đến Authentication và User Profile.
 * Các endpoint tuân theo đúng tài liệu docs/API.md.
 */
export const authService = {
  /**
   * Đăng ký tài khoản mới.
   * POST /api/v1/auth/register
   */
  async register(data) {
    const response = await apiClient.post('/auth/register', data)
    return response.data // ApiResponse<AuthResponse>
  },

  /**
   * Đăng nhập người dùng.
   * POST /api/v1/auth/login
   */
  async login(credentials) {
    const response = await apiClient.post('/auth/login', credentials)
    return response.data // ApiResponse<AuthResponse>
  },

  /**
   * Lấy thông tin người dùng đang đăng nhập.
   * GET /api/v1/users/me
   */
  async getCurrentUser() {
    const response = await apiClient.get('/users/me')
    return response.data // ApiResponse<UserResponse>
  },

  /**
   * Cập nhật hồ sơ người dùng hiện tại (partial update).
   * PUT /api/v1/users/me
   */
  async updateProfile(data) {
    const response = await apiClient.put('/users/me', data)
    return response.data // ApiResponse<UserResponse>
  },

  /**
   * Đổi mật khẩu tài khoản hiện tại.
   * PUT /api/v1/users/me/password
   */
  async changePassword(data) {
    const response = await apiClient.put('/users/me/password', data)
    return response.data // ApiResponse<Void>
  },

  /**
   * Làm mới Access Token bằng cookie HttpOnly.
   * POST /api/v1/auth/refresh
   */
  async refreshToken() {
    const response = await apiClient.post('/auth/refresh')
    return response.data // ApiResponse<AuthResponse>
  },

  /**
   * Đăng xuất tài khoản (thu hồi phiên trong Redis và xóa cookie).
   * POST /api/v1/auth/logout
   */
  async logout() {
    const response = await apiClient.post('/auth/logout')
    return response.data // ApiResponse<Void>
  },
}

export default authService
