-- =============================================================================
-- migration_add_vnpay_method.sql
-- Thêm phương thức thanh toán VNPAY vào enum payment_method trong bảng payments
-- =============================================================================

USE ecommerce_db;

ALTER TABLE payments 
MODIFY COLUMN payment_method ENUM('COD', 'SIMULATED', 'VNPAY') NOT NULL;

