import axios from 'axios'

/**
 * Quản lý Access Token trong memory (không lưu vào localStorage để chống XSS).
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
 * Axios client cấu hình base URL và bật withCredentials để gửi/nhận cookie HttpOnly.
 */
const apiClient = axios.create({
  baseURL: getNormalizedBaseUrl(),
  timeout: 10000,
  withCredentials: true, // Bắt buộc để gửi/nhận cookie refresh_token HttpOnly
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request Interceptor: tự động gắn Bearer Token từ memory
apiClient.interceptors.request.use(
  (config) => {
    const isPublicAuthEndpoint =
      config.url?.includes('/auth/login') ||
      config.url?.includes('/auth/register') ||
      config.url?.includes('/auth/refresh')

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

// Trạng thái cho Single-Flight Refresh Queue
let isRefreshing = false
let failedQueue = []

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

// Response Interceptor: xử lý 401 với cơ chế Single-Flight Refresh và retry tối đa 1 lần
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (!originalRequest) {
      return Promise.reject(error)
    }

    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh') ||
      originalRequest.url?.includes('/auth/logout')

    // Nếu lỗi 401 và không phải từ các endpoint xác thực
    if (error.response?.status === 401 && !isAuthEndpoint) {
      // Nếu request này đã từng retry 1 lần -> không retry nữa để tránh vòng lặp vô hạn
      if (originalRequest._retry) {
        setInMemoryToken(null)
        window.dispatchEvent(new CustomEvent('auth:expired'))
        return Promise.reject(error)
      }

      // Nếu đang có một request refresh khác đang chạy (Single-Flight)
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

      originalRequest._retry = true
      isRefreshing = true

      return new Promise((resolve, reject) => {
        apiClient
          .post('/auth/refresh')
          .then((res) => {
            const authData = res.data?.data
            const newAccessToken = authData?.accessToken

            if (newAccessToken) {
              setInMemoryToken(newAccessToken)
              // Bắn event để useAuthStore cập nhật state in-memory
              window.dispatchEvent(
                new CustomEvent('auth:refreshed', { detail: authData })
              )
              processQueue(null, newAccessToken)
              originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
              resolve(apiClient(originalRequest))
            } else {
              throw new Error('Không nhận được Access Token mới')
            }
          })
          .catch((refreshErr) => {
            processQueue(refreshErr, null)
            setInMemoryToken(null)
            window.dispatchEvent(new CustomEvent('auth:expired'))
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
