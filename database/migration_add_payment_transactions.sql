-- =============================================================================
-- migration_add_payment_transactions.sql
-- Thêm bảng payment_transactions lưu lịch sử các lần thử thanh toán (Payment Attempts)
-- Phục vụ quản lý Retry thanh toán VNPay và đối soát IPN Webhook độc lập [TSK-D12-CCT]
-- Cập nhật Phase 1.1: Bổ sung trường reconciliation_status quản lý Overpayment và Refund Pending
-- =============================================================================

USE ecommerce_db;

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id            BIGINT NOT NULL,
    order_id              BIGINT NOT NULL,
    txn_ref               VARCHAR(64) NOT NULL UNIQUE,
    vnp_transaction_no    VARCHAR(64),
    amount                DECIMAL(15, 2) NOT NULL,
    bank_code             VARCHAR(20),
    payment_method        ENUM('COD', 'SIMULATED', 'VNPAY') NOT NULL DEFAULT 'VNPAY',
    status                ENUM('UNPAID', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'UNPAID',
    reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    response_code         VARCHAR(10),
    transaction_status    VARCHAR(10),
    transaction_note      VARCHAR(255),
    pay_date              DATETIME,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_tx_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_tx_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_payment_tx_payment_id (payment_id),
    INDEX idx_payment_tx_order_id (order_id),
    INDEX idx_payment_tx_reconciliation_status (reconciliation_status),
    UNIQUE INDEX idx_payment_tx_txn_ref (txn_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Hỗ trợ nâng cấp nếu bảng payment_transactions đã tồn tại từ trước:
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'payment_transactions' 
    AND COLUMN_NAME = 'reconciliation_status');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE payment_transactions ADD COLUMN reconciliation_status VARCHAR(30) NOT NULL DEFAULT \'NONE\' AFTER status, ADD INDEX idx_payment_tx_reconciliation_status (reconciliation_status)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
