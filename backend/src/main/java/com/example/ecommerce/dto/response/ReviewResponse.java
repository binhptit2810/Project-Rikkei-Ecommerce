package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết một đánh giá sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private Long userId;
    private String userName;
    private String userFullName;
    private String userAvatar;
    private Integer rating;
    private String comment;
    private String adminReply;
    private LocalDateTime adminReplyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }

        Long prodId = review.getProduct() != null ? review.getProduct().getId() : null;
        Long uId = review.getUser() != null ? review.getUser().getId() : null;
        String fullName = review.getUser() != null ? review.getUser().getFullName() : null;

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(prodId)
                .userId(uId)
                .userName(fullName)
                .userFullName(fullName)
                .userAvatar(null)
                .rating(review.getRating())
                .comment(review.getComment())
                .adminReply(review.getAdminReply())
                .adminReplyAt(review.getAdminReplyAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

