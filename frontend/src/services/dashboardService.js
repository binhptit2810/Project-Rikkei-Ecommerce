import apiClient from './apiClient'

/**
 * Service quản lý số liệu thống kê Bảng điều khiển Quản trị viên (Admin Dashboard).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 * Phối hợp: Vũ Đức Huy Hoàng
 */
export const dashboardService = {
  /**
   * Lấy số liệu thống kê tổng quan hệ thống:
   * - Tổng số đơn hàng (totalOrders)
   * - Doanh thu thực tế (actualRevenue / totalRevenue)
   * - Đơn chờ duyệt (pendingOrders)
   * - Danh sách sản phẩm có nguy cơ hết hàng (lowStockProducts, stock_quantity <= 5)
   *
   * @param {number} [lowStockThreshold=5] Ngưỡng cảnh báo tồn kho (mặc định 5)
   * @returns {Promise<Object>}
   */
  getStats: async (lowStockThreshold = 5) => {
    const response = await apiClient.get('/admin/dashboard/stats', {
      params: { lowStockThreshold },
    })
    return response.data
  },

  /**
   * Truy vấn riêng danh sách các sản phẩm có nguy cơ hết hàng.
   *
   * @param {number} [threshold=5]
   * @returns {Promise<Object>}
   */
  getLowStockProducts: async (threshold = 5) => {
    const response = await apiClient.get('/admin/dashboard/low-stock', {
      params: { threshold },
    })
    return response.data
  },

  /**
   * Truy vấn riêng danh sách Top sản phẩm bán chạy nhất tháng.
   *
   * @param {number} [limit=5] Số lượng sản phẩm muốn lấy
   * @returns {Promise<Object>}
   */
  getTopSellingProducts: async (limit = 5) => {
    const response = await apiClient.get('/admin/dashboard/top-products', {
      params: { limit },
    })
    return response.data
  },
}

export default dashboardService

