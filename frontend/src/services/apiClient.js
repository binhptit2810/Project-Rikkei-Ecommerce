import axios from 'axios'

/**
 * Quản lý Access Token thuần in-memory (không lưu vào localStorage/sessionStorage để phòng chống tấn công XSS).
 */
let inMemoryAccessToken = null

export const setInMemoryToken = (token) => {
  inMemoryAccessToken = token || null
}

export const getInMemoryToken = () => inMemoryAccessToken

/**
 * Chuẩn hóa Base URL để đảm bảo luôn có tiền tố /v1
 */
const getNormalizedBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const trimmed = envUrl.replace(/\/+$/, '')
  return trimmed.endsWith('/v1') ? trimmed : `${trimmed}/v1`
}

/**
 * Axios client cấu hình base URL và bật withCredentials để tự động gửi/nhận cookie HttpOnly chứa Refresh Token.
 */
const apiClient = axios.create({
  baseURL: getNormalizedBaseUrl(),
  timeout: 10000,
  withCredentials: true, // Bắt buộc để gửi kèm cookie refresh_token HttpOnly an toàn
  headers: {
    'Content-Type': 'application/json',
  },
})

// ============================================================================
// REQUEST INTERCEPTOR: Tự động đính kèm Access Token vào Authorization Header
// ============================================================================
apiClient.interceptors.request.use(
  (config) => {
    // Không đính kèm Bearer token cho các endpoint công khai (Login, Register, Refresh, Forgot/Reset Password)
    const isPublicAuthEndpoint =
      config.url?.includes('/auth/login') ||
      config.url?.includes('/auth/register') ||
      config.url?.includes('/auth/refresh') ||
      config.url?.includes('/auth/forgot-password') ||
      config.url?.includes('/auth/reset-password')

    if (!isPublicAuthEndpoint) {
      const token = inMemoryAccessToken
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ============================================================================
// CƠ CHẾ SINGLE-FLIGHT REFRESH QUEUE & XỬ LÝ HẾT HẠN PHIÊN ĐĂNG NHẬP (401)
// ============================================================================
let isRefreshing = false
let failedQueue = []

/**
 * Giải phóng hàng đợi: Chạy lại các request đang chờ nếu refresh thành công,
 * hoặc reject tất cả nếu refresh thất bại.
 */
const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

/**
 * Xử lý khi phiên làm việc hết hạn hoặc Refresh Token thất bại:
 * 1. Xóa sạch Access Token trong memory
 * 2. Xóa toàn bộ dữ liệu lưu trữ local/session storage
 * 3. Bắn event 'auth:expired' để cập nhật Store (Zustand useAuthStore)
 * 4. Tự động chuyển hướng (redirect) người dùng về trang /login
 */
export const handleAuthExpired = () => {
  setInMemoryToken(null)

  if (typeof window !== 'undefined') {
    try {
      localStorage.clear()
      sessionStorage.clear()
    } catch (e) {
      // Tránh lỗi bảo mật khi chạy trong iframe hoặc sandbox
    }

    // Bắn CustomEvent để useAuthStore đồng bộ trạng thái đăng xuất
    window.dispatchEvent(new CustomEvent('auth:expired'))

    // Tự động chuyển hướng về /login (tránh lặp vô hạn nếu đang ở trang đăng nhập)
    if (
      window.location.pathname !== '/login' &&
      window.location.pathname !== '/admin/login'
    ) {
      window.location.href = '/login'
    }
  }
}

// ============================================================================
// RESPONSE INTERCEPTOR: Bắt lỗi 401, tự động Refresh Token và Retry request
// ============================================================================
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (!originalRequest) {
      return Promise.reject(error)
    }

    // Xác định các endpoint xác thực (không thực hiện refresh lặp)
    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh') ||
      originalRequest.url?.includes('/auth/logout') ||
      originalRequest.url?.includes('/auth/forgot-password') ||
      originalRequest.url?.includes('/auth/reset-password')

    // BƯỚC 1: Nếu gặp lỗi 401 Unauthorized và không phải request từ auth endpoint
    if (error.response?.status === 401 && !isAuthEndpoint) {
      // BƯỚC 2: Kiểm tra cờ _retry để tránh vòng lặp vô hạn (chỉ retry tối đa 1 lần)
      if (originalRequest._retry) {
        handleAuthExpired()
        return Promise.reject(error)
      }

      // BƯỚC 3: Nếu đang có tiến trình làm mới token chạy ngầm (Single-Flight Pattern)
      // Cho request hiện tại vào hàng đợi (failedQueue) chờ kết quả token mới
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest._retry = true
            originalRequest.headers.Authorization = `Bearer ${token}`
            return apiClient(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      // BƯỚC 4: Đánh dấu bắt đầu tiến trình Refresh Token
      originalRequest._retry = true
      isRefreshing = true

      return new Promise((resolve, reject) => {
        // Gọi API POST /auth/refresh (tự động kèm Cookie HttpOnly refreshToken)
        apiClient
          .post('/auth/refresh')
          .then((res) => {
            const authData = res.data?.data
            const newAccessToken = authData?.accessToken

            if (newAccessToken) {
              // BƯỚC 5: Refresh thành công -> Cập nhật token và retry lại toàn bộ hàng đợi
              setInMemoryToken(newAccessToken)

              // Bắn event để useAuthStore đồng bộ state in-memory
              window.dispatchEvent(
                new CustomEvent('auth:refreshed', { detail: authData })
              )

              processQueue(null, newAccessToken)
              originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
              resolve(apiClient(originalRequest))
            } else {
              throw new Error('Phản hồi refresh không chứa Access Token hợp lệ')
            }
          })
          .catch((refreshErr) => {
            // BƯỚC 6: Refresh thất bại (Refresh token hết hạn hoặc không hợp lệ)
            processQueue(refreshErr, null)
            handleAuthExpired()
            reject(refreshErr)
          })
          .finally(() => {
            isRefreshing = false
          })
      })
    }

    return Promise.reject(error)
  }
)

export default apiClient
