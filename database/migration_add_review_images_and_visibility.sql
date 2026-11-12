-- =============================================================================
-- Migration: Bổ sung Quản lý ảnh đánh giá & Trạng thái ẩn/hiện Review [TSK-D14-CCT]
-- Phiên bản: 20261112_review_images_and_visibility
-- Tác giả: Cao Chí Thiện (CCT)
-- Đặc điểm: Idempotent (chạy nhiều lần an toàn), không làm mất dữ liệu review cũ,
-- tương thích MySQL 8.0, tự động gán is_visible = TRUE cho toàn bộ review hiện tại.
-- =============================================================================

-- 1. Bổ sung cột is_visible vào bảng reviews nếu chưa có
SET @dbname = DATABASE();
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
      AND TABLE_NAME = 'reviews' 
      AND COLUMN_NAME = 'is_visible'
);

SET @sql_add_col = IF(
    @col_exists = 0,
    'ALTER TABLE reviews ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE AFTER admin_reply_at',
    'SELECT "Column is_visible already exists on reviews"'
);
PREPARE stmt_add_col FROM @sql_add_col;
EXECUTE stmt_add_col;
DEALLOCATE PREPARE stmt_add_col;

-- 2. Đảm bảo tất cả review hiện có đều có is_visible = TRUE (không bị ẩn sau khi cập nhật)
UPDATE reviews SET is_visible = TRUE WHERE is_visible IS NULL;

-- 3. Bổ sung composite index (product_id, is_visible) để tối ưu truy vấn review công khai
SET @idx_prod_vis_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = @dbname 
      AND TABLE_NAME = 'reviews' 
      AND INDEX_NAME = 'idx_reviews_product_visible'
);

SET @sql_add_idx_prod_vis = IF(
    @idx_prod_vis_exists = 0,
    'CREATE INDEX idx_reviews_product_visible ON reviews (product_id, is_visible)',
    'SELECT "Index idx_reviews_product_visible already exists"'
);
PREPARE stmt_add_idx_prod_vis FROM @sql_add_idx_prod_vis;
EXECUTE stmt_add_idx_prod_vis;
DEALLOCATE PREPARE stmt_add_idx_prod_vis;

-- 4. Bổ sung index is_visible
SET @idx_vis_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = @dbname 
      AND TABLE_NAME = 'reviews' 
      AND INDEX_NAME = 'idx_reviews_visible'
);

SET @sql_add_idx_vis = IF(
    @idx_vis_exists = 0,
    'CREATE INDEX idx_reviews_visible ON reviews (is_visible)',
    'SELECT "Index idx_reviews_visible already exists"'
);
PREPARE stmt_add_idx_vis FROM @sql_add_idx_vis;
EXECUTE stmt_add_idx_vis;
DEALLOCATE PREPARE stmt_add_idx_vis;

-- 5. Tạo bảng review_images nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS review_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    image_path  VARCHAR(500) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    INDEX idx_review_images_review (review_id),
    INDEX idx_review_images_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Ghi nhận vào schema_migrations nếu bảng này tồn tại
SET @table_sm_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_SCHEMA = @dbname 
      AND TABLE_NAME = 'schema_migrations'
);

SET @sql_record_sm = IF(
    @table_sm_exists > 0,
    'INSERT INTO schema_migrations (version, description, status, executed_at, completed_at) VALUES (\'20261112_review_images_and_visibility\', \'Add is_visible to reviews and create review_images table\', \'COMPLETED\', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE completed_at = CURRENT_TIMESTAMP, status = \'COMPLETED\'',
    'SELECT "Table schema_migrations not present, skipping version recording"'
);
PREPARE stmt_record_sm FROM @sql_record_sm;
EXECUTE stmt_record_sm;
DEALLOCATE PREPARE stmt_record_sm;

SELECT 'Migration [TSK-D14-CCT] 20261112_review_images_and_visibility completed successfully!' AS status;
