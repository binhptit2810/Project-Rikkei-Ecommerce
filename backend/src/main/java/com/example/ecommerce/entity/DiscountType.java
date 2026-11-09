package com.example.ecommerce.entity;

/**
 * Loại giảm giá của mã Coupon:
 * - PERCENT: Giảm theo tỷ lệ phần trăm (%) trên tổng tiền đơn hàng.
 * - FIXED: Giảm một số tiền cố định (VNĐ).
 *
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
public enum DiscountType {
    PERCENT("Giảm theo phần trăm (%)"),
    FIXED("Giảm số tiền cố định (VNĐ)");

    private final String description;

    DiscountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

