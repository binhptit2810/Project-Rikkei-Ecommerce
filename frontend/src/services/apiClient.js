import axios from 'axios'

/**
 * Axios client cấu hình base URL từ biến môi trường hoặc proxy.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export default apiClient
