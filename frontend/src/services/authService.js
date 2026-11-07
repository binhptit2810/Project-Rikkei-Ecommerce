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

  /**
   * Yêu cầu mã OTP đặt lại mật khẩu qua email (TSK-D10-CCT).
   * POST /api/v1/auth/forgot-password
   */
  async forgotPassword(data) {
    const response = await apiClient.post('/auth/forgot-password', data)
    return response.data // ApiResponse<Void>
  },

  /**
   * Gửi mã OTP xác thực email trước khi tạo tài khoản.
   * POST /api/v1/auth/send-registration-otp
   */
  async sendRegistrationOtp(data) {
    const response = await apiClient.post('/auth/send-registration-otp', data)
    return response.data // ApiResponse<Void>
  },

  /**
   * Xác thực OTP và đặt lại mật khẩu mới (TSK-D10-CCT).
   * POST /api/v1/auth/reset-password
   */
  async resetPassword(data) {
    const response = await apiClient.post('/auth/reset-password', data)
    return response.data // ApiResponse<Void>
  },

  /**
   * Xác thực email bằng mã OTP 6 chữ số khi đăng ký tài khoản.
   * POST /api/v1/auth/verify-email
   */
  async verifyEmail(data) {
    const response = await apiClient.post('/auth/verify-email', data)
    return response.data // ApiResponse<AuthResponse>
  },

  /**
   * Gửi lại mã OTP xác thực email (Rate limit 60s).
   * POST /api/v1/auth/resend-verification
   */
  async resendVerification(data) {
    const response = await apiClient.post('/auth/resend-verification', data)
    return response.data // ApiResponse<Void>
  },
}

export default authService
