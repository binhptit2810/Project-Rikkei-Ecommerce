import apiClient from './apiClient'

/**
 * Service quản lý Đánh giá & Nhận xét sản phẩm (Review Service).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D12-DDM] Xây Dựng Giao Diện Đánh Giá Sản Phẩm & Thẻ Sản Phẩm Trong Chat
 *
 * Tích hợp trực tiếp với Backend ReviewController (/api/v1/products/{productId}/reviews):
 * - GET  /api/v1/products/{productId}/reviews        — Lấy danh sách đánh giá có phân trang (Public)
 * - GET  /api/v1/products/{productId}/reviews/summary — Lấy thống kê số sao trung bình & phân bố (Public)
 * - POST /api/v1/products/{productId}/reviews        — Gửi đánh giá mới (Cần đăng nhập)
 */
export const reviewService = {
  /**
   * Lấy danh sách đánh giá của sản phẩm có phân trang.
   * @param {number|string} productId ID sản phẩm
   * @param {number} [page=0] Số trang (bắt đầu từ 0)
   * @param {number} [size=5] Số lượng review mỗi trang
   * @param {string} [sortBy='createdAt'] Tiêu chí sắp xếp
   * @param {string} [sortDir='desc'] Hướng sắp xếp (asc|desc)
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ReviewResponse>
   */
  async getProductReviews(productId, page = 0, size = 5, sortBy = 'createdAt', sortDir = 'desc') {
    const response = await apiClient.get(`/products/${productId}/reviews`, {
      params: { page, size, sortBy, sortDir },
    })
    return response.data
  },

  /**
   * Lấy dữ liệu thống kê đánh giá (điểm trung bình, tổng số đánh giá, phân bố từng mức sao).
   * @param {number|string} productId ID sản phẩm
   * @returns {Promise<Object>} ApiResponse chứa ProductRatingSummaryResponse
   */
  async getProductRatingSummary(productId) {
    const response = await apiClient.get(`/products/${productId}/reviews/summary`)
    return response.data
  },

  /**
   * Gửi đánh giá mới cho sản phẩm (Yêu cầu đăng nhập, đã mua hàng và đơn COMPLETED).
   * @param {number|string} productId ID sản phẩm
   * @param {Object} reviewData { rating: number (1-5), comment: string }
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async createReview(productId, reviewData) {
    const response = await apiClient.post(`/products/${productId}/reviews`, reviewData)
    return response.data
  },

  /**
   * Chỉnh sửa đánh giá sản phẩm (Chính chủ người dùng).
   * @param {number|string} productId ID sản phẩm
   * @param {number|string} reviewId ID đánh giá
   * @param {Object} reviewData { rating: number (1-5), comment: string }
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async updateReview(productId, reviewId, reviewData) {
    const response = await apiClient.put(`/products/${productId}/reviews/${reviewId}`, reviewData)
    return response.data
  },

  /**
   * Xóa đánh giá sản phẩm (Chính chủ hoặc Quản trị viên).
   * @param {number|string} productId ID sản phẩm
   * @param {number|string} reviewId ID đánh giá
   * @returns {Promise<Object>} ApiResponse
   */
  async deleteReview(productId, reviewId) {
    const response = await apiClient.delete(`/products/${productId}/reviews/${reviewId}`)
    return response.data
  },

  /**
   * Quản trị viên phản hồi trực tiếp đánh giá của khách hàng.
   * @param {number|string} reviewId ID đánh giá
   * @param {Object} replyData { reply: string }
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async replyReview(reviewId, replyData) {
    const response = await apiClient.post(`/admin/reviews/${reviewId}/reply`, replyData)
    return response.data
  },

  /**
   * Tải lên từ 1 đến 3 ảnh đính kèm cho đánh giá (Customer - [TSK-D14-CCT]).
   * @param {number|string} reviewId ID đánh giá
   * @param {File[]|File} files Mảng các tệp File hoặc một File đơn
   * @returns {Promise<Object>} ApiResponse chứa danh sách ReviewImageResponse
   */
  async uploadReviewImages(reviewId, files) {
    const formData = new FormData()
    if (Array.isArray(files)) {
      files.forEach((f) => formData.append('files', f))
    } else if (files) {
      formData.append('files', files)
    }

    const response = await apiClient.post(`/reviews/${reviewId}/images`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  },

  /**
   * Lấy danh sách tất cả đánh giá phục vụ Quản trị viên (Admin - [TSK-D14-CCT]).
   * @param {number} [page=0] Số trang
   * @param {number} [size=10] Kích thước trang
   * @param {boolean|null} [isVisible=null] Lọc theo trạng thái hiển thị
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ReviewResponse>
   */
  async getAdminReviews(page = 0, size = 10, isVisible = null) {
    const params = { page, size }
    if (isVisible !== null && isVisible !== undefined && isVisible !== '') {
      params.isVisible = isVisible
    }
    const response = await apiClient.get('/admin/reviews', { params })
    return response.data
  },

  /**
   * Chuyển đổi trạng thái hiển thị (ẩn / hiện) của đánh giá (Admin - [TSK-D14-CCT]).
   * @param {number|string} reviewId ID đánh giá
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse với isVisible mới
   */
  async toggleReviewVisibility(reviewId) {
    const response = await apiClient.put(`/admin/reviews/${reviewId}/toggle-status`)
    return response.data
  },

  /**
   * Xóa vĩnh viễn đánh giá sản phẩm và dọn tệp ảnh khỏi storage (Admin - [TSK-D14-CCT]).
   * @param {number|string} reviewId ID đánh giá
   * @returns {Promise<Object>} ApiResponse
   */
  async deleteReviewByAdmin(reviewId) {
    const response = await apiClient.delete(`/admin/reviews/${reviewId}`)
    return response.data
  },
}

export default reviewService

