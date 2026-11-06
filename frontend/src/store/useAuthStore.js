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
   * Hỗ trợ đối chiếu expectedRole ('CUSTOMER' hoặc 'ADMIN') để ngăn chặn đăng nhập sai cổng.
   * Access Token được lưu thuần in-memory (không lưu localStorage).
   */
  login: async (credentials, expectedRole = null) => {
    set({ isLoading: true, error: null, lockReason: null })
    try {
      // Gửi portal lên Backend để kiểm soát bảo mật chặt chẽ ngay từ server
      const payload = {
        ...credentials,
        portal: expectedRole || credentials.portal || undefined,
      }
      const res = await authService.login(payload)
      const { accessToken, user } = res.data

      // Đối chiếu role thực tế từ Backend phản hồi với portal mong đợi (phòng thủ đa lớp)
      if (expectedRole && user?.role !== expectedRole) {
        // Thu hồi token ngay lập tức ở frontend và backend để ngăn phiên sai quyền
        setInMemoryToken(null)
        try {
          await authService.logout()
        } catch (_) {}

        // Nếu ở cổng CUSTOMER mà tài khoản là ADMIN: trả về thông báo chung, tự nhiên, tuyệt đối KHÔNG tiết lộ role
        let errorMessage = 'Email hoặc mật khẩu không chính xác.'
        if (expectedRole === 'ADMIN') {
          errorMessage = 'Tài khoản không có quyền truy cập khu vực Quản trị.'
        }

        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          isLoading: false,
          error: errorMessage,
          lockReason: null,
        })
        const roleError = new Error(errorMessage)
        roleError.isRoleMismatch = true
        throw roleError
      }

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
      if (err.isRoleMismatch) {
        throw err
      }
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
      throw err
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
   * Xóa sạch trạng thái xác thực và dọn dẹp storage (localStorage, sessionStorage).
   * Được gọi khi hết hạn token, lỗi 401 không thể refresh hoặc khi người dùng đăng xuất.
   */
  clearAuth: () => {
    setInMemoryToken(null)
    if (typeof window !== 'undefined') {
      try {
        localStorage.clear()
        sessionStorage.clear()
      } catch (e) {
        // Bỏ qua nếu môi trường chặn truy cập storage
      }
    }
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      error: null,
      lockReason: null,
      isLoading: false,
    })
  },

  /**
   * Đăng xuất tài khoản: gọi API thu hồi phiên Redis và xóa cookie trên client.
   */
  logout: async () => {
    get().clearAuth()

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
    useAuthStore.getState().clearAuth()
    // Tự động chuyển hướng về /login nếu không ở trang đăng nhập
    if (
      window.location.pathname !== '/login' &&
      window.location.pathname !== '/admin/login'
    ) {
      window.location.href = '/login'
    }
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
