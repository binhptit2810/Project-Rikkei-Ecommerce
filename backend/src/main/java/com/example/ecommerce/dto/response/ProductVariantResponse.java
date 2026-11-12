package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi thông tin biến thể kích cỡ sản phẩm.
 * [TSK-D14-VDHH]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private String size;
    private Integer stockQuantity;
    private String sku;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .stockQuantity(variant.getStockQuantity())
                .sku(variant.getSku())
                .build();
    }
}

