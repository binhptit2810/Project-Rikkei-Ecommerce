package com.example.ecommerce.entity;

/**
 * Trạng thái đối soát và xử lý hoàn tiền cho từng lần thử thanh toán (Payment Attempt).
 * Phục vụ phát hiện thanh toán thừa (Overpayment) và đơn hàng đã hủy cần hoàn tiền (TSK-D12-CCT Phase 1.1).
 */
public enum ReconciliationStatus {
    NONE("Bình thường"),
    REFUND_PENDING("Chờ đối soát hoàn tiền"),
    OVERPAYMENT("Thanh toán trùng thừa"),
    REFUNDED("Đã hoàn tiền");

    private final String description;

    ReconciliationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

