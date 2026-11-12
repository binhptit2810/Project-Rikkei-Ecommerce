package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.ReviewImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin hình ảnh đính kèm của một đánh giá.
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageResponse {

    private Long id;
    private Long reviewId;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static ReviewImageResponse fromEntity(ReviewImage reviewImage) {
        if (reviewImage == null) {
            return null;
        }

        Long rId = reviewImage.getReview() != null ? reviewImage.getReview().getId() : null;

        return ReviewImageResponse.builder()
                .id(reviewImage.getId())
                .reviewId(rId)
                .imageUrl(reviewImage.getImageUrl())
                .createdAt(reviewImage.getCreatedAt())
                .build();
    }
}
