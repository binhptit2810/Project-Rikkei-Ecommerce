package com.example.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Interface Projection cho kết quả truy vấn Best Seller (Sản phẩm bán chạy nhất).
 * Dùng cho Spring Data JPA Native SQL / JPQL.
 *
 * Phụ trách: Đặng Thanh Bình (DTB)
 */
public interface BestSellerProjection {

    Long getProductId();

    String getProductName();

    String getDescription();

    BigDecimal getPrice();

    String getImageUrl();

    Long getCategoryId();

    String getCategoryName();

    Integer getStockQuantity();

    Boolean getActive();

    Double getRatingAverage();

    Integer getTotalReviews();

    Long getTotalSold();

    default BigDecimal getTotalRevenue() {
        return null;
    }

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}

