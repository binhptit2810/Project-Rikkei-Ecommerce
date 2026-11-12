package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Integer stockQuantity;
    private Boolean active;
    private Double ratingAverage;
    private Integer totalReviews;
    private Long totalSold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product, Long totalSold) {
        ProductResponse res = fromEntity(product);
        if (res != null) {
            res.setTotalSold(totalSold);
        }
        return res;
    }

    public static ProductResponse fromProjection(BestSellerProjection p) {
        if (p == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(p.getProductId())
                .name(p.getProductName())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .stockQuantity(p.getStockQuantity())
                .active(p.getActive())
                .ratingAverage(p.getRatingAverage() != null ? p.getRatingAverage() : 0.0)
                .totalReviews(p.getTotalReviews() != null ? p.getTotalReviews() : 0)
                .totalSold(p.getTotalSold() != null ? p.getTotalSold() : 0L)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        Long catId = null;
        String catName = null;
        if (product.getCategory() != null) {
            catId = product.getCategory().getId();
            catName = product.getCategory().getName();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(catId)
                .categoryName(catName)
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .ratingAverage(product.getRatingAverage() != null ? product.getRatingAverage() : 0.0)
                .totalReviews(product.getTotalReviews() != null ? product.getTotalReviews() : 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
