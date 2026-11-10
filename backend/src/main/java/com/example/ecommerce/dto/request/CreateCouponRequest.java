package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO tạo mới mã giảm giá (Admin Create Coupon).
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCouponRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã giảm giá chỉ được chứa chữ cái, chữ số, dấu gạch dưới hoặc gạch ngang")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống (PERCENT hoặc FIXED)")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Mức giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    @Min(value = 1, message = "Giới hạn số lần sử dụng phải lớn hơn 0")
    private Integer usageLimit;

    private LocalDateTime expireAt;

    @Builder.Default
    private Boolean active = true;
}
