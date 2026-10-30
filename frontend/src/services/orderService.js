import apiClient from './apiClient'

/**
 * Service quản lý các yêu cầu Đơn hàng (Order).
 * Phụ trách: Vũ Đức Huy Hoàng (Cart & Order Module).
 */
export const orderService = {
  /**
   * Tạo đơn hàng mới từ giỏ hàng
   * @param {Object} orderData { recipientName, recipientPhone, shippingAddress, paymentMethod, note, items: [{ productId, quantity }] }
   */
  createOrder: async (orderData) => {
    const response = await apiClient.post('/orders', orderData)
    return response.data
  },

  /**
   * Lấy danh sách lịch sử đơn hàng của người dùng hiện tại
   */
  getMyOrders: async () => {
    const response = await apiClient.get('/orders/my')
    return response.data
  },

  /**
   * Lấy chi tiết đơn hàng theo ID
   * @param {number|string} orderId
   */
  getOrderById: async (orderId) => {
    const response = await apiClient.get(`/orders/${orderId}`)
    return response.data
  },

  /**
   * Hủy đơn hàng (chỉ cho phép khi đơn ở trạng thái PENDING hoặc CONFIRMED)
   * @param {number|string} orderId
   * @param {string} reason Lý do hủy đơn
   */
  cancelOrder: async (orderId, reason = '') => {
    const response = await apiClient.patch(`/orders/${orderId}/cancel`, { reason })
    return response.data
  },

  /**
   * Dành cho Admin: Lấy danh sách tất cả các đơn hàng
   */
  getAdminOrders: async () => {
    const response = await apiClient.get('/admin/orders')
    return response.data
  },

  /**
   * Dành cho Admin: Cập nhật trạng thái đơn hàng
   * @param {number|string} orderId
   * @param {string} status PENDING | CONFIRMED | PROCESSING | SHIPPING | COMPLETED | CANCELLED
   */
  updateAdminOrderStatus: async (orderId, status) => {
    const response = await apiClient.patch(`/admin/orders/${orderId}/status`, { status })
    return response.data
  },
}

export default orderService
