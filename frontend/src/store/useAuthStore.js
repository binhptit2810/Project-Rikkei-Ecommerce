import { create } from 'zustand'
import authService from '../services/authService'
import { setInMemoryToken } from '../services/apiClient'

export const useAuthStore = create((set, get) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  isInitializing: true, // Cờ khởi tạo ứng dụng (khôi phục phiên sau khi F5/reload)
  isLoading: false,
  error: null,
  lockReason: null,

  /**
   * Khởi tạo và khôi phục phiên người dùng khi ứng dụng khởi chạy / reload trang (App Bootstrapping).
   * Gọi POST /api/v1/auth/refresh để lấy lại Access Token vào memory thông qua cookie HttpOnly.
   */
  initAuth: async () => {
    set({ isInitializing: true, error: null })
    try {
      const res = await authService.refreshToken()
      const { accessToken, user } = res.data

      setInMemoryToken(accessToken)

      set({
        user,
        accessToken,
        isAuthenticated: true,
        isInitializing: false,
        error: null,
      })
    } catch (err) {
      // Nếu không có cookie hoặc token hết hạn, đưa về trạng thái khách vãng lai
      setInMemoryToken(null)
      set({
        user: null,
        accessToken: null,
        isAuthenticated: false,
        isInitializing: false,
        error: null,
      })
    }
  },

  /**
   * Đăng nhập bằng email & mật khẩu.
   * Access Token được lưu thuần in-memory (không lưu localStorage).
   */
  login: async (credentials) => {
    set({ isLoading: true, error: null, lockReason: null })
    try {
      const res = await authService.login(credentials)
      const { accessToken, user } = res.data

      setInMemoryToken(accessToken)

      set({
        user,
        accessToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        lockReason: null,
      })
      return { success: true, user }
    } catch (err) {
      const errResponse = err.response?.data
      const message =
        errResponse?.message || err.message || 'Đăng nhập không thành công'
      const reason =
        errResponse?.data?.reason || errResponse?.data?.lockReason || null

      set({
        isLoading: false,
        error: message,
        lockReason: reason,
      })
      throw err
    }
  },

  /**
   * Đăng ký tài khoản mới.
   */
  register: async (formData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.register(formData)
      const { accessToken, user } = res.data

      setInMemoryToken(accessToken)

      set({
        user,
        accessToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      })
      return { success: true, message: res.message, user }
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Đăng ký không thành công'
      set({ isLoading: false, error: message })
      throw new Error(message)
    }
  },

  /**
   * Lấy thông tin hồ sơ mới nhất từ backend.
   */
  fetchProfile: async () => {
    if (!get().accessToken) return
    try {
      const res = await authService.getCurrentUser()
      const user = res.data
      set({ user, isAuthenticated: true })
      return user
    } catch (err) {
      if (err.response?.status === 401) {
        get().logout()
      }
    }
  },

  /**
   * Cập nhật thông tin hồ sơ (fullName, phone, address).
   */
  updateProfile: async (updateData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.updateProfile(updateData)
      const updatedUser = res.data
      set({ user: updatedUser, isLoading: false, error: null })
      return { success: true, message: res.message, user: updatedUser }
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Cập nhật thất bại'
      set({ isLoading: false, error: message })
      throw new Error(message)
    }
  },

  /**
   * Đổi mật khẩu: khi thành công, thu hồi tất cả phiên và yêu cầu đăng nhập lại.
   */
  changePassword: async (passwordData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.changePassword(passwordData)
      // Thu hồi phiên local ngay lập tức
      get().logout()
      set({ isLoading: false, error: null })
      return {
        success: true,
        message:
          res.message || 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.',
      }
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Đổi mật khẩu thất bại'
      set({ isLoading: false, error: message })
      throw new Error(message)
    }
  },

  /**
   * Đăng xuất tài khoản: gọi API thu hồi phiên Redis và xóa cookie trên client.
   */
  logout: async () => {
    setInMemoryToken(null)
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      error: null,
      lockReason: null,
      isLoading: false,
    })

    try {
      await authService.logout()
    } catch (e) {
      // Bỏ qua lỗi mạng khi logout, vì client memory đã xóa sạch và cookie được backend xóa
    }
  },

  clearError: () => set({ error: null, lockReason: null }),
}))

// Lắng nghe sự kiện token hết hạn từ apiClient interceptor
if (typeof window !== 'undefined') {
  window.addEventListener('auth:expired', () => {
    setInMemoryToken(null)
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    })
  })

  // Lắng nghe sự kiện refresh thành công từ single-flight queue
  window.addEventListener('auth:refreshed', (event) => {
    const authData = event.detail
    if (authData?.accessToken) {
      useAuthStore.setState((state) => ({
        accessToken: authData.accessToken,
        user: authData.user || state.user,
        isAuthenticated: true,
      }))
    }
  })
}
