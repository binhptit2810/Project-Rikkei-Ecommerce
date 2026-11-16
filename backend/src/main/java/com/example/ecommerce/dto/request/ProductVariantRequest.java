package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận thông tin biến thể kích cỡ sản phẩm từ giao diện Admin.
 * [TSK-D14-VDHH]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private Long id;

    @NotBlank(message = "Kích cỡ sản phẩm không được để trống")
    private String size;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được nhỏ hơn 0")
    private Integer stockQuantity;

    private String sku;
}
