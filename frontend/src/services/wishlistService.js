import apiClient from './apiClient'

/**
 * Service gọi API Phân hệ Sản phẩm yêu thích (Wishlist Service).
 * Base URL: /api/v1/wishlist
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
export const wishlistService = {
  /**
   * Lấy danh sách sản phẩm đã yêu thích của người dùng hiện tại kèm thông tin chi tiết.
   * GET /api/v1/wishlist
   * @returns {Promise<Array>} Danh sách WishlistItemResponse
   */
  async getWishlist() {
    const response = await apiClient.get('/wishlist')
    return response.data?.data || response.data || []
  },

  /**
   * Lấy danh sách các Product IDs mà người dùng hiện tại đã thả tim.
   * GET /api/v1/wishlist/ids
   * @returns {Promise<Array<number>>} Danh sách ID sản phẩm [1, 2, 5, ...]
   */
  async getWishlistProductIds() {
    const response = await apiClient.get('/wishlist/ids')
    return response.data?.data || response.data || []
  },

  /**
   * Bật/Tắt yêu thích một sản phẩm (Thêm nếu chưa có, Xóa nếu đã có).
   * POST /api/v1/wishlist/toggle/{productId}
   * @param {number|string} productId - ID sản phẩm
   * @returns {Promise<{ liked: boolean, message: string }>} Kết quả toggle
   */
  async toggleWishlist(productId) {
    const response = await apiClient.post(`/wishlist/toggle/${productId}`)
    return response.data?.data || response.data
  },
}

export default wishlistService

