import apiClient from './apiClient'

/**
 * Service quản lý Thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
export const paymentService = {
  /**
   * Xử lý thanh toán cho đơn hàng (COD hoặc SIMULATED)
   * @param {number|string} orderId
   * @param {Object} paymentData { paymentMethod, simulateSuccess, transactionNote }
   */
  processPayment: async (orderId, paymentData) => {
    const response = await apiClient.post(`/payments/${orderId}/pay`, paymentData)
    return response.data
  },

  /**
   * Tra cứu thông tin thanh toán của đơn hàng
   * @param {number|string} orderId
   */
  getPaymentByOrderId: async (orderId) => {
    const response = await apiClient.get(`/payments/order/${orderId}`)
    return response.data
  },
}

export default paymentService
