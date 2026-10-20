import { create } from 'zustand'
import authService from '../services/authService'

// Helper lấy dữ liệu lưu trữ từ localStorage
const getInitialUser = () => {
  try {
    const raw = localStorage.getItem('user_info')
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

const getInitialToken = () => {
  return localStorage.getItem('access_token') || null
}

export const useAuthStore = create((set, get) => ({
  user: getInitialUser(),
  accessToken: getInitialToken(),
  isAuthenticated: Boolean(getInitialToken() && getInitialUser()),
  isLoading: false,
  error: null,

  /**
   * Đăng nhập
   */
  login: async (credentials) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.login(credentials)
      const { accessToken, user } = res.data

      localStorage.setItem('access_token', accessToken)
      localStorage.setItem('user_info', JSON.stringify(user))

      set({
        user,
        accessToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      })
      return { success: true, user }
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Đăng nhập không thành công'
      set({ isLoading: false, error: message })
      throw new Error(message)
    }
  },

  /**
   * Đăng ký tài khoản mới (tự động đăng nhập sau khi đăng ký thành công)
   */
  register: async (formData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.register(formData)
      const { accessToken, user } = res.data

      localStorage.setItem('access_token', accessToken)
      localStorage.setItem('user_info', JSON.stringify(user))

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
   * Lấy thông tin hồ sơ mới nhất từ backend
   */
  fetchProfile: async () => {
    if (!get().accessToken) return
    try {
      const res = await authService.getCurrentUser()
      const user = res.data
      localStorage.setItem('user_info', JSON.stringify(user))
      set({ user, isAuthenticated: true })
      return user
    } catch (err) {
      if (err.response?.status === 401) {
        get().logout()
      }
    }
  },

  /**
   * Cập nhật thông tin hồ sơ (fullName, phone, address)
   */
  updateProfile: async (updateData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.updateProfile(updateData)
      const updatedUser = res.data
      localStorage.setItem('user_info', JSON.stringify(updatedUser))
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
   * Đổi mật khẩu
   */
  changePassword: async (passwordData) => {
    set({ isLoading: true, error: null })
    try {
      const res = await authService.changePassword(passwordData)
      set({ isLoading: false, error: null })
      return { success: true, message: res.message }
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Đổi mật khẩu thất bại'
      set({ isLoading: false, error: message })
      throw new Error(message)
    }
  },

  /**
   * Đăng xuất
   */
  logout: () => {
    localStorage.removeItem('access_token')
    localStorage.removeItem('user_info')
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      error: null,
      isLoading: false,
    })
  },

  clearError: () => set({ error: null }),
}))

// Lắng nghe sự kiện token hết hạn từ apiClient interceptor
if (typeof window !== 'undefined') {
  window.addEventListener('auth:expired', () => {
    useAuthStore.getState().logout()
  })
}
