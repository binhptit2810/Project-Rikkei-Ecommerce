-- =============================================================================
-- Migration: Bổ sung xác thực email cho người dùng (Email OTP Verification)
-- Phiên bản: 20261107_email_otp
-- Thiết kế: Idempotent, State-Machine Marker (schema_migrations),
-- an toàn trước gián đoạn (Failure Recovery), không phụ thuộc timestamp cố định,
-- không hardcode tên database, tương thích 100% MySQL 8.
-- =============================================================================

-- 1. Đảm bảo bảng quản lý phiên bản migration (schema_migrations) tồn tại
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(50) NOT NULL PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    target_max_id BIGINT NULL,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Kiểm tra xem migration này đã hoàn tất trước đó hay chưa
SET @migration_version =
    _utf8mb4'20261107_email_otp' COLLATE utf8mb4_unicode_ci;
SET @is_completed = (
    SELECT COUNT(*) FROM schema_migrations 
    WHERE version = @migration_version AND status = 'COMPLETED'
);

-- 3. Cơ chế State-Machine Marker: Đóng băng ranh giới ID TRƯỚC KHI sửa đổi bảng users
-- Đảm bảo bất kỳ tài khoản mới nào đăng ký trong hoặc sau khi ALTER TABLE đều có ID > target_max_id,
-- loại trừ hoàn toàn race condition backfill nhầm tài khoản mới.
SET @has_pending_record = (
    SELECT COUNT(*) FROM schema_migrations 
    WHERE version = @migration_version AND status = 'PENDING'
);

-- Nếu lần đầu chạy (chưa COMPLETED và chưa có PENDING):
-- Đóng băng mốc ID lớn nhất hiện tại của tập tài khoản legacy
SET @current_max_user_id = (SELECT IFNULL(MAX(id), 0) FROM users);

SET @init_marker = (SELECT IF(
    @is_completed = 0 AND @has_pending_record = 0,
    "INSERT INTO schema_migrations (version, description, status, target_max_id, executed_at) VALUES ('20261107_email_otp', 'Add email verification columns and backfill legacy accounts', 'PENDING', @current_max_user_id, CURRENT_TIMESTAMP)",
    "SELECT 1"
));
PREPARE stmtInitMarker FROM @init_marker;
EXECUTE stmtInitMarker;
DEALLOCATE PREPARE stmtInitMarker;

-- Lấy target_max_id từ bảng marker (đảm bảo tính bất biến kể cả khi chạy lại sau sự cố gián đoạn)
SET @frozen_max_id = (
    SELECT target_max_id FROM schema_migrations 
    WHERE version = @migration_version
);

-- 4. Bổ sung các cột xác thực email vào bảng users nếu chưa tồn tại
-- Sử dụng INFORMATION_SCHEMA.COLUMNS và DATABASE() động để không phụ thuộc tên schema cố định
SET @dbname = DATABASE();
SET @tablename = 'users';
SET @columnname = 'email_verified';

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
        TABLE_SCHEMA = @dbname
        AND TABLE_NAME = @tablename
        AND COLUMN_NAME = @columnname
);

SET @preparedStatement = (SELECT IF(
    @col_exists > 0,
    "SELECT 1",
    "ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER active, ADD COLUMN email_verified_at DATETIME NULL AFTER email_verified"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 5. Thực hiện backfill nếu chưa COMPLETED
-- Giới hạn nghiêm ngặt id <= @frozen_max_id: chỉ cập nhật tài khoản tồn tại trước khi chạy migration
SET @execute_backfill = (SELECT IF(
    @is_completed = 0 AND @frozen_max_id IS NOT NULL,
    "UPDATE users SET email_verified = TRUE, email_verified_at = CURRENT_TIMESTAMP WHERE id <= @frozen_max_id AND (email_verified IS NULL OR email_verified = FALSE)",
    "SELECT 1"
));
PREPARE stmtBackfill FROM @execute_backfill;
EXECUTE stmtBackfill;
DEALLOCATE PREPARE stmtBackfill;

-- 6. Đánh dấu migration hoàn tất thành công (COMPLETED)
SET @finalize_marker = (SELECT IF(
    @is_completed = 0 AND @frozen_max_id IS NOT NULL,
    "UPDATE schema_migrations SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP WHERE version = '20261107_email_otp'",
    "SELECT 1"
));
PREPARE stmtFinalize FROM @finalize_marker;
EXECUTE stmtFinalize;
DEALLOCATE PREPARE stmtFinalize;
