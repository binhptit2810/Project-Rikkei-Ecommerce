import axios from 'axios'

/**
 * Chuẩn hóa Base URL để đảm bảo luôn có tiền tố /v1
 * Nếu biến môi trường là '/api' thì tự động chuẩn hóa thành '/api/v1'
 */
const getNormalizedBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const trimmed = envUrl.replace(/\/+$/, '')
  return trimmed.endsWith('/v1') ? trimmed : `${trimmed}/v1`
}

/**
 * Axios client cấu hình base URL từ biến môi trường hoặc proxy Vite.
 * Tự động gắn JWT Token vào header Authorization và xử lý lỗi 401.
 */
const apiClient = axios.create({
  baseURL: getNormalizedBaseUrl(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request Interceptor: tự động gắn Bearer Token nếu đã đăng nhập (bỏ qua cho endpoint auth public)
apiClient.interceptors.request.use(
  (config) => {
    const isPublicAuthEndpoint =
      config.url?.includes('/auth/login') ||
      config.url?.includes('/auth/register')

    if (!isPublicAuthEndpoint) {
      const token = localStorage.getItem('access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor: xử lý khi token hết hạn hoặc lỗi xác thực
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint =
      error.config?.url?.includes('/auth/login') ||
      error.config?.url?.includes('/auth/register')

    // Nếu nhận 401 từ các API được bảo vệ (không phải màn đăng nhập/đăng ký)
    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('user_info')
      window.dispatchEvent(new CustomEvent('auth:expired'))
    }
    return Promise.reject(error)
  }
)

export default apiClient
