import apiClient from './apiClient'

/**
 * Service quản lý Thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
export const paymentService = {
  /**
   * Xử lý thanh toán cho đơn hàng (COD hoặc SIMULATED)
   * @param {number|string} orderId
   * @param {Object} paymentData { paymentMethod, simulateSuccess, transactionNote, status }
   */
  processPayment: async (orderId, paymentData) => {
    const response = await apiClient.post(`/payments/order/${orderId}`, paymentData)
    return response.data
  },

  /**
   * Mô phỏng thanh toán trực tuyến thành công (PAID) hoặc thất bại (FAILED)
   * @param {number|string} orderId
   * @param {boolean} simulateSuccess true nếu thành công, false nếu thất bại
   * @param {string} [transactionNote]
   */
  simulatePayment: async (orderId, simulateSuccess = true, transactionNote = '') => {
    const response = await apiClient.post(`/payments/order/${orderId}`, {
      paymentMethod: 'SIMULATED',
      simulateSuccess,
      transactionNote: transactionNote || (simulateSuccess ? 'Mô phỏng thanh toán trực tuyến thành công' : 'Mô phỏng thanh toán thất bại'),
    })
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

  /**
   * Tra cứu thông tin thanh toán theo mã giao dịch PAY-*
   * @param {string} transactionCode
   */
  getPaymentByTransactionCode: async (transactionCode) => {
    const response = await apiClient.get(`/payments/transaction/${transactionCode}`)
    return response.data
  },

  /**
   * Tạo URL thanh toán VNPay Sandbox
   * @param {number|string} orderId
   * @param {string} [bankCode] Ví dụ: NCB, VNPAYQR
   * @param {string} [orderInfo] Ghi chú đơn hàng
   */
  createVNPayPayment: async (orderId, bankCode = '', orderInfo = '') => {
    const response = await apiClient.post('/payments/create-vnpay', {
      orderId,
      bankCode: bankCode || undefined,
      orderInfo: orderInfo || undefined,
    })
    return response.data
  },

  /**
   * Xác thực và xử lý kết quả callback trả về từ VNPay
   * @param {Object} queryParams
   */
  handleVNPayCallback: async (queryParams) => {
    const response = await apiClient.get('/payments/vnpay-callback', { params: queryParams })
    return response.data
  },
}

export default paymentService
