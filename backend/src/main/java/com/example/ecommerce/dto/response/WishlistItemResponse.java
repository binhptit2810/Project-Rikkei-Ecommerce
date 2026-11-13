package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho một sản phẩm trong danh sách yêu thích (Wishlist Item).
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String imageUrl;
    private Double ratingAverage;
    private Integer stockQuantity;
    private LocalDateTime addedAt;

    public static WishlistItemResponse fromEntity(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }

        Product product = wishlist.getProduct();
        if (product == null) {
            return null;
        }

        return WishlistItemResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .discountPrice(product.getPrice())
                .imageUrl(product.getImageUrl())
                .ratingAverage(product.getRatingAverage() != null ? product.getRatingAverage() : 0.0)
                .stockQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}

