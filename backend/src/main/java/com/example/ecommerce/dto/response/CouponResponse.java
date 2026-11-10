package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO trả về thông tin chi tiết mã giảm giá (Coupon Response).
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private DiscountType discountType;
    private String discountTypeDescription;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime expireAt;
    private Boolean active;
    private Boolean isExpired;
    private Boolean isUsageLimitReached;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CouponResponse fromEntity(Coupon coupon) {
        if (coupon == null) {
            return null;
        }

        String typeDesc = coupon.getDiscountType() != null
                ? coupon.getDiscountType().getDescription()
                : "";

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountTypeDescription(typeDesc)
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscount(coupon.getMaxDiscount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .expireAt(coupon.getExpireAt())
                .active(coupon.getActive())
                .isExpired(coupon.isExpired())
                .isUsageLimitReached(coupon.isUsageLimitReached())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
