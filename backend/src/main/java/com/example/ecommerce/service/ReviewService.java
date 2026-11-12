package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service xử lý nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] & [TSK-D14-DDM] Xây Dựng API Chỉnh Sửa, Xóa Đánh Giá & Admin Phản Hồi Đánh Giá
 */
public interface ReviewService {

    /**
     * Lấy danh sách đánh giá của sản phẩm có phân trang, mặc định sắp xếp mới nhất trước.
     *
     * @param productId ID sản phẩm
     * @param pageable  Thông tin phân trang và sắp xếp
     * @return PageResponse chứa danh sách ReviewResponse
     */
    PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

    /**
     * Thống kê điểm đánh giá trung bình và số lượng đánh giá theo từng mức sao (1-5 sao).
     *
     * @param productId ID sản phẩm
     * @return ProductRatingSummaryResponse
     */
    ProductRatingSummaryResponse getProductRatingSummary(Long productId);

    /**
     * Tạo đánh giá mới cho sản phẩm.
     * Ràng buộc:
     * - Người dùng phải đăng nhập.
     * - Chỉ khách hàng đã mua sản phẩm và đơn hàng ở trạng thái COMPLETED mới được đánh giá.
     * - Mỗi người dùng chỉ được đánh giá 1 lần duy nhất trên 1 sản phẩm.
     * - Tự động tính toán lại ratingAverage và totalReviews cập nhật vào Product.
     *
     * @param productId ID sản phẩm
     * @param request   Dữ liệu đánh giá (rating, comment)
     * @return ReviewResponse
     */
    ReviewResponse createReview(Long productId, ReviewCreateRequest request);

    /**
     * Chỉnh sửa đánh giá sản phẩm (Chính chủ người dùng).
     *
     * @param productId ID sản phẩm
     * @param reviewId  ID đánh giá cần chỉnh sửa
     * @param request   Dữ liệu cập nhật (rating, comment)
     * @return ReviewResponse
     */
    ReviewResponse updateReview(Long productId, Long reviewId, ReviewRequest request);

    /**
     * Xóa đánh giá sản phẩm (Chính chủ hoặc Quản trị viên ADMIN).
     *
     * @param productId ID sản phẩm
     * @param reviewId  ID đánh giá cần xóa
     */
    void deleteReview(Long productId, Long reviewId);

    /**
     * Quản trị viên phản hồi trực tiếp đánh giá của khách hàng.
     *
     * @param reviewId ID đánh giá
     * @param request  Nội dung phản hồi từ Admin
     * @return ReviewResponse
     */
    ReviewResponse replyReview(Long reviewId, AdminReplyRequest request);

    /**
     * Lấy toàn bộ danh sách đánh giá của hệ thống dành cho Admin quản lý.
     *
     * @param pageable  Thông tin phân trang và sắp xếp
     * @param productId Lọc theo ID sản phẩm (tùy chọn)
     * @param rating    Lọc theo số sao (tùy chọn)
     * @param isHidden  Lọc theo trạng thái ẩn/hiện (tùy chọn)
     * @return PageResponse chứa danh sách ReviewResponse
     */
    PageResponse<ReviewResponse> getAllReviewsForAdmin(Pageable pageable, Long productId, Integer rating, Boolean isHidden);

    /**
     * Xóa đánh giá trực tiếp bởi Admin theo reviewId.
     *
     * @param reviewId ID đánh giá cần xóa
     */
    void deleteReviewByAdmin(Long reviewId);

    /**
     * Bật / Tắt trạng thái hiển thị của đánh giá (Ẩn / Hiện).
     *
     * @param reviewId ID đánh giá
     * @return ReviewResponse sau khi cập nhật
     */
    ReviewResponse toggleReviewVisibility(Long reviewId);
}

