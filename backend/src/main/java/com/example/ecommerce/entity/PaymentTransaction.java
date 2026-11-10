package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `payment_transactions` trong MySQL.
 * Ghi nhận chi tiết từng lần thử thanh toán (Payment Attempt) của đơn hàng,
 * đặc biệt là các giao dịch qua Cổng thanh toán VNPay.
 *
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT - Phase 1)
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_tx_txn_ref", columnList = "txn_ref", unique = true),
        @Index(name = "idx_payment_tx_payment_id", columnList = "payment_id"),
        @Index(name = "idx_payment_tx_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_tx_reconciliation_status", columnList = "reconciliation_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 64)
    private String txnRef;

    @Column(name = "vnp_transaction_no", length = 64)
    private String vnpTransactionNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;

    @Column(name = "transaction_note", length = 255)
    private String transactionNote;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PaymentStatus.UNPAID;
        }
        if (this.reconciliationStatus == null) {
            this.reconciliationStatus = ReconciliationStatus.NONE;
        }
        if (this.paymentMethod == null) {
            this.paymentMethod = PaymentMethod.VNPAY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

