-- =============================================================================
-- Migration: Thêm phương thức thanh toán SEPAY (VietQR SePay)
-- =============================================================================
ALTER TABLE payments
MODIFY COLUMN payment_method ENUM('COD', 'SIMULATED', 'VNPAY', 'SEPAY') NOT NULL;

ALTER TABLE payment_transactions
MODIFY COLUMN payment_method ENUM('COD', 'SIMULATED', 'VNPAY', 'SEPAY') NOT NULL DEFAULT 'SEPAY';
