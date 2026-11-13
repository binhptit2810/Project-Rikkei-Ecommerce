import apiClient from './apiClient'
import uploadService from './uploadService'

/**
 * Service quản lý Đánh giá & Nhận xét sản phẩm (Review Service).
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
 *
 * Tích hợp toàn diện với ReviewController & AdminReviewController:
 * - Customer APIs:
 *   + GET    /api/v1/products/{productId}/reviews          — Lấy danh sách đánh giá có phân trang
 *   + GET    /api/v1/products/{productId}/reviews/summary  — Lấy thống kê số sao & phân bố
 *   + POST   /api/v1/products/{productId}/reviews          — Gửi đánh giá mới (kèm tối đa 3 ảnh)
 *   + PUT    /api/v1/products/{productId}/reviews/{id}     — Chỉnh sửa đánh giá (kèm ảnh)
 *   + DELETE /api/v1/products/{productId}/reviews/{id}     — Xóa đánh giá
 *   + POST   /api/v1/reviews/upload                        — Tải ảnh đánh giá lên máy chủ (DHA)
 *   + POST   /api/v1/reviews/{reviewId}/images             — Tải ảnh đính kèm theo review (CCT)
 *
 * - Admin APIs:
 *   + GET    /api/v1/admin/reviews                         — Lấy toàn bộ đánh giá có phân trang & lọc
 *   + POST   /api/v1/admin/reviews/{id}/reply              — Phản hồi đánh giá của khách
 *   + PUT    /api/v1/admin/reviews/{id}/reply              — Cập nhật câu phản hồi đánh giá
 *   + PATCH  /api/v1/admin/reviews/{id}/visibility         — Bật/Tắt trạng thái ẩn/hiện đánh giá
 *   + PUT    /api/v1/admin/reviews/{id}/toggle-status      — Bật/Tắt trạng thái ẩn/hiện đánh giá
 *   + DELETE /api/v1/admin/reviews/{id}                    — Xóa đánh giá (Admin)
 */
export const reviewService = {
  // =========================================================================
  // 1. DÀNH CHO KHÁCH HÀNG (CUSTOMER APIS)
  // =========================================================================

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
   * Kiểm tra điều kiện đánh giá sản phẩm của người dùng hiện tại (đã mua & giao hàng thành công, đã đánh giá chưa).
   * @param {number|string} productId ID sản phẩm
   * @returns {Promise<Object>} ApiResponse chứa { canReview, hasPurchased, alreadyReviewed, reason, myReviewId, myReview }
   */
  async checkReviewEligibility(productId) {
    const response = await apiClient.get(`/products/${productId}/reviews/eligibility`)
    return response.data
  },

  /**
   * Gửi đánh giá mới cho sản phẩm (kèm danh sách ảnh tối đa 3 ảnh).
   * @param {number|string} productId ID sản phẩm
   * @param {Object} reviewData { rating: number (1-5), comment: string, images?: string[] }
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
   * @param {Object} reviewData { rating: number (1-5), comment: string, images?: string[] }
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
   * Tải 1 ảnh đánh giá lên máy chủ.
   * @param {File} file
   * @returns {Promise<string>}
   */
  async uploadReviewImage(file) {
    return uploadService.uploadReviewImage(file)
  },

  /**
   * Tải lên danh sách nhiều ảnh đánh giá (tối đa 3 ảnh).
   * Hỗ trợ 2 chữ ký hàm:
   * 1. uploadReviewImages(files): Trả về mảng url string[] (DHA)
   * 2. uploadReviewImages(reviewId, files): Đính kèm trực tiếp vào reviewId (CCT)
   */
  async uploadReviewImages(arg1, arg2) {
    // Nếu gọi theo dạng: uploadReviewImages(reviewId, files)
    if (arg2 !== undefined) {
      const reviewId = arg1
      const files = arg2
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
    }

    // Nếu gọi theo dạng: uploadReviewImages(files)
    const files = arg1
    return uploadService.uploadReviewImages(files)
  },

  // =========================================================================
  // 2. DÀNH CHO QUẢN TRỊ VIÊN (ADMIN REVIEW APIS)
  // =========================================================================

  /**
   * Lấy toàn bộ danh sách đánh giá của hệ thống cho Admin (có phân trang & bộ lọc).
   * @param {Object} params { page, size, sortBy, sortDir, productId, rating, isHidden, isVisible }
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ReviewResponse>
   */
  async getAllReviewsForAdmin(params = {}) {
    const {
      page = 0,
      size = 10,
      sortBy = 'createdAt',
      sortDir = 'desc',
      productId = null,
      rating = null,
      isHidden = null,
      isVisible = null,
    } = params

    const cleanParams = { page, size, sortBy, sortDir }
    if (productId != null) cleanParams.productId = productId
    if (rating != null) cleanParams.rating = rating
    if (isHidden != null) cleanParams.isHidden = isHidden
    if (isVisible != null) cleanParams.isVisible = isVisible

    const response = await apiClient.get('/admin/reviews', { params: cleanParams })
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
   * Admin gửi phản hồi trực tiếp cho đánh giá của khách hàng.
   * @param {number|string} reviewId ID đánh giá
   * @param {Object} replyData { reply: string }
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async replyReview(reviewId, replyData) {
    const response = await apiClient.post(`/admin/reviews/${reviewId}/reply`, replyData)
    return response.data
  },

  /**
   * Admin cập nhật câu phản hồi đánh giá.
   * @param {number|string} reviewId ID đánh giá
   * @param {Object} replyData { reply: string }
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async updateReplyReview(reviewId, replyData) {
    const response = await apiClient.put(`/admin/reviews/${reviewId}/reply`, replyData)
    return response.data
  },

  /**
   * Admin chuyển đổi trạng thái ẩn / hiện của đánh giá (toggle visibility).
   * @param {number|string} reviewId ID đánh giá
   * @returns {Promise<Object>} ApiResponse chứa ReviewResponse
   */
  async toggleReviewVisibility(reviewId) {
    const response = await apiClient.patch(`/admin/reviews/${reviewId}/visibility`)
    return response.data
  },

  /**
   * Admin xóa đánh giá trực tiếp theo reviewId.
   * @param {number|string} reviewId ID đánh giá
   * @param {number|string} [productId] ID sản phẩm (tùy chọn fallback)
   * @returns {Promise<Object>} ApiResponse
   */
  async deleteReviewByAdmin(reviewId, productId = null) {
    try {
      const response = await apiClient.delete(`/admin/reviews/${reviewId}`)
      return response.data
    } catch (err) {
      if (productId) {
        return this.deleteReview(productId, reviewId)
      }
      throw err
    }
  },
}

export default reviewService
