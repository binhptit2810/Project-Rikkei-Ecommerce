import apiClient from './apiClient'

/**
 * Service quản lý Mã giảm giá (Coupon / Voucher Module).
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
export const couponService = {
  /**
   * Kiểm tra điều kiện áp mã và tính toán số tiền giảm giá chính xác tại server
   * @param {string} code Mã voucher
   * @param {number} orderValue Giá trị tạm tính của đơn hàng
   */
  validateCoupon: async (code, orderValue) => {
    const response = await apiClient.post('/coupons/validate', {
      code: code ? code.trim().toUpperCase() : '',
      orderValue,
    })
    return response.data
  },

  /**
   * Lấy danh sách các mã giảm giá đang hoạt động
   */
  getActiveCoupons: async () => {
    const response = await apiClient.get('/coupons/active')
    return response.data
  },

  /**
   * Tra cứu thông tin chi tiết một coupon
   * @param {string} code
   */
  getCouponByCode: async (code) => {
    const response = await apiClient.get(`/coupons/${code}`)
    return response.data
  },
}

export default couponService

