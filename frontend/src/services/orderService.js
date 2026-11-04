import apiClient from './apiClient'

/**
 * Service quản lý Đơn hàng (Order Module).
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH) & Đỗ Hoàng Anh [DHA] (TSK-D08-DHA)
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
   * @param {Object} [params={}] { page, size, sortBy, sortDir }
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
   * Hủy đơn hàng (khi còn ở trạng thái PENDING hoặc CONFIRMED) kèm lý do hủy (note/reason)
   * @param {number|string} id ID đơn hàng
   * @param {string|Object} [reasonOrData] Chuỗi lý do hủy hoặc object { reason: string }
   */
  cancelOrder: async (id, reasonOrData) => {
    const payload =
      typeof reasonOrData === 'string'
        ? { reason: reasonOrData }
        : reasonOrData || {}
    const response = await apiClient.patch(`/orders/${id}/cancel`, payload)
    return response.data
  },

  // ================= ADMIN APIS =================

  /**
   * Lấy danh sách toàn bộ đơn hàng trong hệ thống (dành cho Admin)
   * @param {Object} [params={}] { page, size, status, search, sortBy, sortDir }
   */
  getAdminOrders: async (params = {}) => {
    const response = await apiClient.get('/admin/orders', { params })
    return response.data
  },

  /**
   * Lấy chi tiết đơn hàng dành cho Admin
   * @param {number|string} id
   */
  getAdminOrderById: async (id) => {
    const response = await apiClient.get(`/admin/orders/${id}`)
    return response.data
  },

  /**
   * Cập nhật trạng thái đơn hàng (dành cho Admin)
   * Quy trình chuẩn: PENDING → CONFIRMED → PROCESSING → SHIPPING → COMPLETED / CANCELLED
   * @param {number|string} id
   * @param {string} status Trạng thái mới (CONFIRMED, PROCESSING, SHIPPING, COMPLETED, CANCELLED)
   */
  updateAdminOrderStatus: async (id, status) => {
    const response = await apiClient.patch(`/admin/orders/${id}/status`, { status })
    return response.data
  },
}

export default orderService
