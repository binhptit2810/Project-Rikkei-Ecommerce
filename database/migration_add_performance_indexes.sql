-- =============================================================================
-- migration_add_performance_indexes.sql
-- Đánh index tối ưu hiệu năng theo yêu cầu kỹ thuật:
-- 1. coupon_code trên bảng orders & coupons (phục vụ áp dụng mã giảm giá và đối soát đơn hàng)
-- 2. product_id trên bảng reviews (phục vụ truy vấn đánh giá sản phẩm & tính điểm trung bình)
-- 3. vnp_txn_ref trên bảng payments (phục vụ đối soát IPN và tra cứu trạng thái thanh toán VNPay)
--
-- Phụ trách: Đặng Thanh Bình (DTB)
-- Phối hợp: Cao Chí Thiện (CCT)
-- Tiêu chí nghiệm thu (DoD): Tốc độ query API < 50ms
-- =============================================================================

USE ecommerce_db;

-- -----------------------------------------------------------------------------
-- 1. BẢNG ORDERS: Bổ sung cột coupon_code, discount_amount (nếu chưa có) & index
-- -----------------------------------------------------------------------------
SET @col_coupon_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'orders' 
      AND COLUMN_NAME = 'coupon_code'
);
SET @sql_coupon = IF(@col_coupon_exists = 0, 
    'ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50) AFTER total_amount', 
    'SELECT 1'
);
PREPARE stmt_coupon FROM @sql_coupon;
EXECUTE stmt_coupon;
DEALLOCATE PREPARE stmt_coupon;

SET @col_discount_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'orders' 
      AND COLUMN_NAME = 'discount_amount'
);
SET @sql_discount = IF(@col_discount_exists = 0, 
    'ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(15, 2) DEFAULT 0 AFTER coupon_code', 
    'SELECT 1'
);
PREPARE stmt_discount FROM @sql_discount;
EXECUTE stmt_discount;
DEALLOCATE PREPARE stmt_discount;

SET @idx_orders_coupon = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'orders' 
      AND INDEX_NAME = 'idx_orders_coupon_code'
);
SET @sql_idx_coupon = IF(@idx_orders_coupon = 0, 
    'CREATE INDEX idx_orders_coupon_code ON orders (coupon_code)', 
    'SELECT 1'
);
PREPARE stmt_idx_coupon FROM @sql_idx_coupon;
EXECUTE stmt_idx_coupon;
DEALLOCATE PREPARE stmt_idx_coupon;

SET @idx_orders_status_created = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'orders' 
      AND INDEX_NAME = 'idx_orders_status_created'
);
SET @sql_idx_status_created = IF(@idx_orders_status_created = 0, 
    'CREATE INDEX idx_orders_status_created ON orders (order_status, created_at)', 
    'SELECT 1'
);
PREPARE stmt_idx_status_created FROM @sql_idx_status_created;
EXECUTE stmt_idx_status_created;
DEALLOCATE PREPARE stmt_idx_status_created;

-- -----------------------------------------------------------------------------
-- 2. BẢNG COUPONS: Đảm bảo index idx_coupons_code trên cột code
-- -----------------------------------------------------------------------------
SET @idx_coupons_code = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'coupons' 
      AND INDEX_NAME = 'idx_coupons_code'
);
SET @sql_idx_coupons_code = IF(@idx_coupons_code = 0, 
    'CREATE INDEX idx_coupons_code ON coupons (code)', 
    'SELECT 1'
);
PREPARE stmt_idx_coupons_code FROM @sql_idx_coupons_code;
EXECUTE stmt_idx_coupons_code;
DEALLOCATE PREPARE stmt_idx_coupons_code;

-- -----------------------------------------------------------------------------
-- 3. BẢNG REVIEWS: Index product_id & composite index (product_id, created_at)
-- -----------------------------------------------------------------------------
SET @idx_reviews_prod = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'reviews' 
      AND INDEX_NAME = 'idx_reviews_product'
);
SET @sql_idx_reviews_prod = IF(@idx_reviews_prod = 0, 
    'CREATE INDEX idx_reviews_product ON reviews (product_id)', 
    'SELECT 1'
);
PREPARE stmt_idx_reviews_prod FROM @sql_idx_reviews_prod;
EXECUTE stmt_idx_reviews_prod;
DEALLOCATE PREPARE stmt_idx_reviews_prod;

SET @idx_reviews_prod_created = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'reviews' 
      AND INDEX_NAME = 'idx_reviews_product_created'
);
SET @sql_idx_reviews_prod_created = IF(@idx_reviews_prod_created = 0, 
    'CREATE INDEX idx_reviews_product_created ON reviews (product_id, created_at)', 
    'SELECT 1'
);
PREPARE stmt_idx_reviews_prod_created FROM @sql_idx_reviews_prod_created;
EXECUTE stmt_idx_reviews_prod_created;
DEALLOCATE PREPARE stmt_idx_reviews_prod_created;

-- -----------------------------------------------------------------------------
-- 4. BẢNG PAYMENTS: Thêm cột vnp_txn_ref (nếu chưa có), đồng bộ dữ liệu & index
-- -----------------------------------------------------------------------------
SET @col_vnp_ref_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'payments' 
      AND COLUMN_NAME = 'vnp_txn_ref'
);
SET @sql_vnp_ref = IF(@col_vnp_ref_exists = 0, 
    'ALTER TABLE payments ADD COLUMN vnp_txn_ref VARCHAR(64) AFTER transaction_code', 
    'SELECT 1'
);
PREPARE stmt_vnp_ref FROM @sql_vnp_ref;
EXECUTE stmt_vnp_ref;
DEALLOCATE PREPARE stmt_vnp_ref;

-- Đồng bộ vnp_txn_ref từ transaction_code nếu đang rỗng
UPDATE payments 
SET vnp_txn_ref = transaction_code 
WHERE vnp_txn_ref IS NULL AND transaction_code IS NOT NULL;

SET @idx_payments_vnp = (
    SELECT COUNT(*) FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'payments' 
      AND INDEX_NAME = 'idx_payments_vnp_txn_ref'
);
SET @sql_idx_payments_vnp = IF(@idx_payments_vnp = 0, 
    'CREATE INDEX idx_payments_vnp_txn_ref ON payments (vnp_txn_ref)', 
    'SELECT 1'
);
PREPARE stmt_idx_payments_vnp FROM @sql_idx_payments_vnp;
EXECUTE stmt_idx_payments_vnp;
DEALLOCATE PREPARE stmt_idx_payments_vnp;
