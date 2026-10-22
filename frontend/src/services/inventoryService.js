import apiClient from './apiClient'

/**
 * Service quản lý Tồn kho (Inventory Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
export const inventoryService = {
  /**
   * Lấy danh sách sản phẩm và tồn kho dành cho Admin
   * @param {Object} params { keyword, categoryId, active, page, size, sortBy, sortDir }
   */
  getInventory: async (params = {}) => {
    const response = await apiClient.get('/admin/inventory', { params })
    return response.data
  },

  /**
   * Admin điều chỉnh tăng/giảm tồn kho thủ công
   * @param {number|string} productId
   * @param {Object} data { changeQuantity, note }
   */
  adjustStock: async (productId, data) => {
    const response = await apiClient.post(`/admin/inventory/${productId}/adjust`, data)
    return response.data
  },

  /**
   * Lấy toàn bộ lịch sử biến động kho trong hệ thống
   */
  getAllTransactions: async () => {
    const response = await apiClient.get('/admin/inventory/transactions')
    return response.data
  },

  /**
   * Lấy lịch sử biến động kho của 1 sản phẩm cụ thể
   * @param {number|string} productId
   */
  getProductTransactions: async (productId) => {
    const response = await apiClient.get(`/admin/inventory/${productId}/transactions`)
    return response.data
  },
}

export default inventoryService
