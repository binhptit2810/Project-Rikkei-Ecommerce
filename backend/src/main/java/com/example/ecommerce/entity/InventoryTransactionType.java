package com.example.ecommerce.entity;

/**
 * Loại giao dịch tồn kho (khớp với ENUM trong 01-schema.sql):
 * ORDER_PLACED    - Trừ kho khi khách đặt hàng thành công
 * ORDER_CANCELLED - Hoàn trả tồn kho khi đơn hàng bị hủy
 * MANUAL_ADJUST   - Admin điều chỉnh tồn kho thủ công
 * INITIAL_STOCK   - Nhập kho ban đầu khi khởi tạo
 */
public enum InventoryTransactionType {
    ORDER_PLACED("Trừ tồn kho khi đặt hàng"),
    ORDER_CANCELLED("Hoàn tồn kho do hủy đơn"),
    MANUAL_ADJUST("Admin điều chỉnh tồn kho thủ công"),
    INITIAL_STOCK("Nhập kho ban đầu");

    private final String description;

    InventoryTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
