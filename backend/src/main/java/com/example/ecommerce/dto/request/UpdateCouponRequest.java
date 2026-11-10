package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO cập nhật mã giảm giá (Admin Update Coupon).
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCouponRequest {

    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã giảm giá chỉ được chứa chữ cái, chữ số, dấu gạch dưới hoặc gạch ngang")
    private String code;

    private DiscountType discountType;

    @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Mức giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    @Min(value = 1, message = "Giới hạn số lần sử dụng phải lớn hơn 0")
    private Integer usageLimit;

    private LocalDateTime expireAt;

    private Boolean active;
}
