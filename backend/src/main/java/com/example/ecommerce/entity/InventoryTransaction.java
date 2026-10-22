package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `inventory_transactions` trong MySQL.
 * Ghi lại lịch sử biến động số lượng tồn kho:
 * - ORDER_PLACED: change_quantity < 0 (xuất kho khi tạo đơn)
 * - ORDER_CANCELLED: change_quantity > 0 (hoàn kho khi hủy đơn)
 * - MANUAL_ADJUST: admin điều chỉnh
 * - INITIAL_STOCK: nhập kho ban đầu
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "change_quantity", nullable = false)
    private Integer changeQuantity;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(name = "order_id")
    private Long orderId;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
