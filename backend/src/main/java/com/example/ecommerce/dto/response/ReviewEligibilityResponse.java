package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi trạng thái điều kiện được phép gửi đánh giá của người dùng.
 * Phụ trách: Đặng Thanh Bình, Đỗ Đức Mạnh, Cao Chí Thiện
 * Tiêu chí: Chỉ khách hàng đã đăng nhập, đã mua sản phẩm và đơn hàng ở trạng thái COMPLETED
 * (giao hàng thành công) mới được phép gửi đánh giá.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEligibilityResponse {

    /**
     * Có đủ điều kiện để tạo mới đánh giá hay không (đã mua, đã giao thành công và chưa từng đánh giá).
     */
    private boolean canReview;

    /**
     * Khách hàng đã từng mua sản phẩm này trong đơn hàng COMPLETED (giao thành công) hay chưa.
     */
    private boolean hasPurchased;

    /**
     * Khách hàng đã từng gửi đánh giá cho sản phẩm này hay chưa.
     */
    private boolean alreadyReviewed;

    /**
     * Lý do / thông điệp giải thích điều kiện đánh giá.
     */
    private String reason;

    /**
     * ID của bản ghi đánh giá hiện có nếu đã từng đánh giá.
     */
    private Long myReviewId;

    /**
     * Chi tiết bản ghi đánh giá hiện có nếu đã từng đánh giá.
     */
    private ReviewResponse myReview;
}

