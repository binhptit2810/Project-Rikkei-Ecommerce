package com.example.ecommerce.entity;

/**
 * Trạng thái đơn hàng:
 * PENDING    - Chờ xác nhận (khách có thể hủy)
 * CONFIRMED  - Đã xác nhận (khách có thể hủy)
 * PROCESSING - Đang đóng gói / chuẩn bị hàng (không thể hủy)
 * SHIPPING   - Đang giao hàng (không thể hủy)
 * COMPLETED  - Đã giao thành công
 * CANCELLED  - Đã hủy (hoàn tồn kho)
 */
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPING("Đang giao hàng"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
