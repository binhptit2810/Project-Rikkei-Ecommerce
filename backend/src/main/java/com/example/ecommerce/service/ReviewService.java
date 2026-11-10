package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service xử lý nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
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
}

