import apiClient from './apiClient'

/**
 * Service quản lý các yêu cầu Giỏ hàng (Cart).
 * Gọi API backend và xử lý mềm dẻo nếu Backend chưa hoàn thiện endpoint.
 */
export const cartService = {
  /**
   * Gửi request thêm sản phẩm vào giỏ hàng lên backend.
   * @param {number|string} productId ID sản phẩm
   * @param {number} quantity Số lượng
   */
  addToCart: async (productId, quantity = 1) => {
    try {
      const response = await apiClient.post('/cart/items', { productId, quantity })
      return response.data
    } catch (error) {
      console.warn(
        '[cartService] Gửi request POST /cart/items:',
        error?.response?.status ? `HTTP ${error.response.status}` : error.message
      )
      return null
    }
  },

  /**
   * Lấy giỏ hàng từ backend nếu có phiên đăng nhập
   */
  getCart: async () => {
    try {
      const response = await apiClient.get('/cart')
      return response.data
    } catch (error) {
      console.warn('[cartService] GET /cart error:', error?.message)
      return null
    }
  },

  /**
   * Cập nhật số lượng sản phẩm trong giỏ hàng
   */
  updateCartItem: async (productId, quantity) => {
    try {
      const response = await apiClient.put(`/cart/items/${productId}`, { quantity })
      return response.data
    } catch (error) {
      console.warn('[cartService] PUT /cart/items error:', error?.message)
      return null
    }
  },

  /**
   * Xóa sản phẩm khỏi giỏ hàng trên server
   */
  removeFromCart: async (productId) => {
    try {
      const response = await apiClient.delete(`/cart/items/${productId}`)
      return response.data
    } catch (error) {
      console.warn('[cartService] DELETE /cart/items error:', error?.message)
      return null
    }
  },
}

export default cartService
