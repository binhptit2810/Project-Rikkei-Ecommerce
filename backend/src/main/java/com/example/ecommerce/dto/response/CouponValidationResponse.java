package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload trả về kết quả kiểm tra mã giảm giá và số tiền chiết khấu chính xác.
 *
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {

    private String code;
    private DiscountType discountType;
    private String discountTypeDescription;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;  // Số tiền thực tế được giảm (bảo vệ ở server)
    private BigDecimal orderValue;      // Giá trị tạm tính trước khi giảm
    private BigDecimal finalAmount;     // Số tiền cuối cùng sau khi giảm (orderValue - discountAmount)
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private LocalDateTime expireAt;
    private String message;
    private boolean valid;
}

