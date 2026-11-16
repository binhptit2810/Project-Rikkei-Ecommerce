-- =============================================================================
-- Migration: Bổ sung Quản lý Avatar & Liên kết Tài khoản Google OAuth2 [TSK-D15-CCT]
-- Phiên bản: 20261113_google_oauth2_and_avatar
-- Tác giả: Cao Chí Thiện (CCT)
-- Đặc điểm: Idempotent (chạy nhiều lần an toàn), tương thích MySQL 8.0+,
-- không làm ảnh hưởng dữ liệu người dùng cũ, không xóa hoặc làm rỗng mật khẩu.
-- =============================================================================

-- 1. Bổ sung cột avatar_url vào bảng users nếu chưa tồn tại
SET @dbname = DATABASE();
SET @col_avatar_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = @dbname 
      AND TABLE_NAME = 'users' 
      AND COLUMN_NAME = 'avatar_url'
);

SET @sql_add_avatar = IF(
    @col_avatar_exists = 0,
    'ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL AFTER address',
    'SELECT "Column avatar_url already exists on users"'
);
PREPARE stmt_add_avatar FROM @sql_add_avatar;
EXECUTE stmt_add_avatar;
DEALLOCATE PREPARE stmt_add_avatar;

-- 2. Tạo bảng user_oauth_accounts lưu định danh OAuth2 (Google Sub, Provider)
CREATE TABLE IF NOT EXISTS user_oauth_accounts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    provider     VARCHAR(50) NOT NULL,
    provider_id  VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_oauth_accounts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_oauth_provider_id UNIQUE (provider, provider_id),
    INDEX idx_user_oauth_user_id (user_id),
    INDEX idx_user_oauth_provider_id (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

