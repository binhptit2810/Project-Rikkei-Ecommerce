import apiClient from './apiClient'

/**
 * Service Quản lý Tồn kho (Admin Inventory Service).
 * Tích hợp đầy đủ các API quản trị tồn kho và nhật ký biến động kho từ Backend:
 *   - GET  /api/v1/admin/products              — Lấy danh sách sản phẩm quản trị (kèm số lượng tồn kho)
 *   - POST /api/v1/admin/inventory/{id}/adjust — Điều chỉnh tồn kho thủ công (Nhập / Xuất kho kèm lý do)
 *   - GET  /api/v1/admin/inventory/transactions — Tra cứu lịch sử biến động kho toàn hệ thống (phân trang & lọc)
 *   - GET  /api/v1/admin/inventory/{id}/transactions — Tra cứu lịch sử biến động kho của 1 sản phẩm cụ thể
 *
 * Phụ trách: Đỗ Hoàng Anh (Senior Frontend Engineer)
 * Task ID: [TSK-D09-DHA] Xây dựng Giao diện Quản trị Kho hàng (AdminInventoryPage)
 */
export const inventoryService = {
  /**
   * Lấy danh sách sản phẩm phục vụ quản lý tồn kho (hỗ trợ tìm kiếm, lọc danh mục, phân trang).
   * Tự động fallback sang API public /products nếu endpoint /admin/products gặp sự cố.
   *
   * @param {Object} params - { keyword, categoryId, active, page, size, sortBy, sortDir }
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ProductResponse>
   */
  async getInventoryProducts(params = {}) {
    try {
      const response = await apiClient.get('/admin/products', { params })
      return response.data
    } catch (adminErr) {
      // Fallback sang endpoint public nếu endpoint admin trả 404 / 403
      const status = adminErr.response?.status
      if (status === 404 || status === 403 || !adminErr.response) {
        const response = await apiClient.get('/products', { params })
        return response.data
      }
      throw adminErr
    }
  },

  /**
   * Điều chỉnh tồn kho thủ công cho một sản phẩm (Nhập kho hoặc Xuất kho).
   *
   * @param {number|string} productId - ID của sản phẩm cần điều chỉnh
   * @param {Object} payload - Dữ liệu điều chỉnh:
   *   @param {number} payload.changeQuantity - Số lượng thay đổi (dương = Nhập kho, âm = Xuất kho)
   *   @param {'MANUAL_IMPORT'|'MANUAL_EXPORT'|'MANUAL_ADJUST'} [payload.type] - Loại giao dịch
   *   @param {string} payload.reason - Lý do điều chỉnh (tối đa 255 ký tự)
   *   @param {string} [payload.note] - Ghi chú bổ sung
   * @returns {Promise<Object>} ApiResponse<InventoryTransactionResponse>
   */
  async adjustStock(productId, payload) {
    const response = await apiClient.post(`/admin/inventory/${productId}/adjust`, {
      changeQuantity: payload.changeQuantity,
      type: payload.type,
      reason: payload.reason || payload.note,
      note: payload.note || payload.reason,
    })
    return response.data
  },

  /**
   * Tra cứu danh sách lịch sử biến động kho trên toàn hệ thống có bộ lọc và phân trang.
   *
   * @param {Object} params - Tham số bộ lọc và phân trang:
   *   @param {number|string} [params.productId] - Lọc theo ID sản phẩm
   *   @param {string} [params.type] - Lọc theo loại biến động (ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT, MANUAL_EXPORT, INITIAL_STOCK)
   *   @param {string} [params.startDate] - Thời điểm bắt đầu (ISO 8601)
   *   @param {string} [params.endDate] - Thời điểm kết thúc (ISO 8601)
   *   @param {number} [params.page=0] - Số trang (0-indexed)
   *   @param {number} [params.size=10] - Số phần tử mỗi trang
   *   @param {string} [params.sortBy='createdAt'] - Cột sắp xếp
   *   @param {string} [params.sortDir='desc'] - Hướng sắp xếp (asc / desc)
   * @returns {Promise<Object>} ApiResponse<PageResponse<InventoryTransactionResponse>>
   */
  async getTransactions(params = {}) {
    const response = await apiClient.get('/admin/inventory/transactions', { params })
    return response.data
  },

  /**
   * Tra cứu nhanh lịch sử biến động kho của riêng một sản phẩm cụ thể.
   *
   * @param {number|string} productId - ID sản phẩm cần xem lịch sử
   * @returns {Promise<Object>} ApiResponse<List<InventoryTransactionResponse>>
   */
  async getProductTransactions(productId) {
    const response = await apiClient.get(`/admin/inventory/${productId}/transactions`)
    return response.data
  },
}

export default inventoryService
