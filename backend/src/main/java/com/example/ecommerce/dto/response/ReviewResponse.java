package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.ReviewImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO phản hồi thông tin chi tiết một đánh giá sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long userId;
    private String userName;
    private String userFullName;
    private String userAvatar;
    private Integer rating;
    private String comment;
    private List<String> images;
    private Boolean isHidden;
    private Boolean isVisible;
    private String adminReply;
    private LocalDateTime adminReplyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) {
            return null;
        }

        Long prodId = review.getProduct() != null ? review.getProduct().getId() : null;
        String prodName = review.getProduct() != null ? review.getProduct().getName() : null;
        String prodImg = review.getProduct() != null ? review.getProduct().getImageUrl() : null;
        Long uId = review.getUser() != null ? review.getUser().getId() : null;
        String fullName = review.getUser() != null ? review.getUser().getFullName() : null;

        List<String> imgList = new ArrayList<>();
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            for (ReviewImage img : review.getImages()) {
                if (img != null && img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    imgList.add(img.getImageUrl().trim());
                }
            }
        }

        boolean hidden = Boolean.TRUE.equals(review.getIsHidden()) || Boolean.FALSE.equals(review.getIsVisible());
        boolean visible = !hidden;

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(prodId)
                .productName(prodName)
                .productImage(prodImg)
                .userId(uId)
                .userName(fullName)
                .userFullName(fullName)
                .userAvatar(null)
                .rating(review.getRating())
                .comment(review.getComment())
                .images(imgList)
                .isHidden(hidden)
                .isVisible(visible)
                .adminReply(review.getAdminReply())
                .adminReplyAt(review.getAdminReplyAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
