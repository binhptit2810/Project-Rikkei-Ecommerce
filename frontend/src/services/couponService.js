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

  // ==========================================
  // ADMIN QUẢN TRỊ MÃ GIẢM GIÁ (VOUCHER CRUD)
  // Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
  // ==========================================

  /**
   * Lấy toàn bộ danh sách mã giảm giá cho Admin (tất cả trạng thái)
   */
  getAdminCoupons: async () => {
    const response = await apiClient.get('/admin/coupons')
    return response.data
  },

  /**
   * Lấy chi tiết mã giảm giá theo ID
   * @param {number|string} id
   */
  getAdminCouponById: async (id) => {
    const response = await apiClient.get(`/admin/coupons/${id}`)
    return response.data
  },

  /**
   * Tạo mới mã giảm giá
   * @param {Object} data
   */
  createCoupon: async (data) => {
    const response = await apiClient.post('/admin/coupons', data)
    return response.data
  },

  /**
   * Cập nhật thông tin mã giảm giá
   * @param {number|string} id
   * @param {Object} data
   */
  updateCoupon: async (id, data) => {
    const response = await apiClient.put(`/admin/coupons/${id}`, data)
    return response.data
  },

  /**
   * Xóa vĩnh viễn mã giảm giá khỏi hệ thống
   * @param {number|string} id
   */
  deleteCoupon: async (id) => {
    const response = await apiClient.delete(`/admin/coupons/${id}`)
    return response.data
  },

  /**
   * Bật/tắt nhanh trạng thái kích hoạt mã giảm giá (Active/Inactive)
   * @param {number|string} id
   */
  toggleCouponStatus: async (id) => {
    const response = await apiClient.patch(`/admin/coupons/${id}/toggle-status`)
    return response.data
  },
}

export default couponService

