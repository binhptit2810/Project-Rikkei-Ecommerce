import apiClient from './apiClient'

/**
 * Service quản lý Đơn hàng (Order Module).
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 */
export const orderService = {
  /**
   * Tạo đơn hàng mới từ giỏ hàng.
   * Lưu ý: Giá và tổng tiền được tính toán an toàn tại Backend, client chỉ gửi danh sách { productId, quantity }
   *
   * @param {Object} orderData { recipientName, recipientPhone, shippingAddress, paymentMethod, note, items }
   */
  createOrder: async (orderData) => {
    const response = await apiClient.post('/orders', orderData)
    return response.data
  },

  /**
   * Lấy danh sách lịch sử đơn hàng của người dùng hiện tại
   * @param {Object} [params={}]
   */
  getMyOrders: async (params = {}) => {
    const response = await apiClient.get('/orders/my', { params })
    return response.data
  },

  /**
   * Xem chi tiết đơn hàng theo ID
   * @param {number|string} id
   */
  getOrderById: async (id) => {
    const response = await apiClient.get(`/orders/${id}`)
    return response.data
  },

  /**
   * Hủy đơn hàng (khi còn ở trạng thái PENDING hoặc CONFIRMED)
   * @param {number|string} id
   */
  cancelOrder: async (id) => {
    const response = await apiClient.patch(`/orders/${id}/cancel`)
    return response.data
  },
}

export default orderService
