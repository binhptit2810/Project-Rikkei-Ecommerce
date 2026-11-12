package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// [TSK-D06-CCT]
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    /** Kích cỡ sản phẩm được chọn [TSK-D14-VDHH]. */
    private String selectedSize;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng đặt phải lớn hơn hoặc bằng 1")
    @Max(value = 1000, message = "Số lượng đặt tối đa 1000 sản phẩm cho mỗi món")
    private Integer quantity;
}
