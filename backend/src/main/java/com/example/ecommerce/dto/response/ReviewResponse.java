package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * DTO phản hồi thông tin chi tiết một đánh giá sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM] & [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
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
    private Boolean isVisible;
    private List<ReviewImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }

        Long prodId = review.getProduct() != null ? review.getProduct().getId() : null;
        Long uId = review.getUser() != null ? review.getUser().getId() : null;
        String fullName = review.getUser() != null ? review.getUser().getFullName() : null;

        List<ReviewImageResponse> imgResponses = (review.getImages() != null)
                ? review.getImages().stream().map(ReviewImageResponse::fromEntity).toList()
                : Collections.emptyList();

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
                .isVisible(review.getIsVisible())
                .images(imgResponses)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}

