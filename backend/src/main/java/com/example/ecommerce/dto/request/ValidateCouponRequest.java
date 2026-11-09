package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload kiểm tra tính hợp lệ và điều kiện áp dụng mã giảm giá khi checkout.
 *
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    private String code;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị đơn hàng phải lớn hơn 0")
    private BigDecimal orderValue;
}

