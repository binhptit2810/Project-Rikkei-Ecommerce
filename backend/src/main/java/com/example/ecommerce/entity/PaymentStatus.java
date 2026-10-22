package com.example.ecommerce.entity;

/**
 * Trạng thái thanh toán của đơn hàng.
 */
public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thanh toán thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
