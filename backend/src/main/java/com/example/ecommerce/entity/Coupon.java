package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `coupons` trong MySQL.
 * Quản lý các chương trình mã giảm giá / khuyến mãi tại giỏ hàng và thanh toán.
 *
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.code != null) {
            this.code = this.code.trim().toUpperCase();
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.usedCount == null) {
            this.usedCount = 0;
        }
        if (this.minOrderValue == null) {
            this.minOrderValue = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.code != null) {
            this.code = this.code.trim().toUpperCase();
        }
    }

    /**
     * Kiểm tra xem mã giảm giá đã hết hạn sử dụng chưa.
     */
    public boolean isExpired() {
        return this.expireAt != null && LocalDateTime.now().isAfter(this.expireAt);
    }

    /**
     * Kiểm tra xem mã giảm giá đã đạt giới hạn số lần sử dụng chưa.
     */
    public boolean isUsageLimitReached() {
        return this.usageLimit != null && this.usedCount != null && this.usedCount >= this.usageLimit;
    }
}

