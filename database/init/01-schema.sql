-- =============================================================================
-- 01-schema.sql — Khởi tạo schema database cho E-commerce Platform
-- MySQL 8.0+
-- Thực thi file này trước file 02-sample-data.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;

-- =============================================================================
-- Bảng users — Tài khoản người dùng
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,          -- BCrypt hash
    full_name    VARCHAR(100) NOT NULL,
    phone        VARCHAR(15),
    address      TEXT,
    role         ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng categories — Danh mục sản phẩm
-- =============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT,
    image_url    VARCHAR(500),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng products — Sản phẩm
-- stock_quantity: số lượng tồn kho hiện tại
-- =============================================================================
CREATE TABLE IF NOT EXISTS products (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    price          DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
    image_url      VARCHAR(500),
    category_id    BIGINT,
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_products_category (category_id),
    INDEX idx_products_name (name),
    INDEX idx_products_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng orders — Đơn hàng
-- order_code: mã đơn duy nhất hiển thị cho người dùng (ví dụ: ORD-20241015-0001)
-- =============================================================================
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code       VARCHAR(20) NOT NULL UNIQUE,
    user_id          BIGINT NOT NULL,
    -- Thông tin giao hàng: lưu snapshot tại thời điểm đặt hàng
    recipient_name   VARCHAR(100) NOT NULL,
    recipient_phone  VARCHAR(15) NOT NULL,
    shipping_address TEXT NOT NULL,
    total_amount     DECIMAL(15, 2) NOT NULL CHECK (total_amount >= 0),
    order_status     ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPING','COMPLETED','CANCELLED')
                     NOT NULL DEFAULT 'PENDING',
    payment_status   ENUM('UNPAID','PAID','FAILED','REFUNDED')
                     NOT NULL DEFAULT 'UNPAID',
    note             TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng order_items — Chi tiết đơn hàng
-- unit_price & product_name: snapshot giá và tên tại thời điểm đặt hàng
-- =============================================================================
CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    -- Snapshot thời điểm đặt hàng — không phụ thuộc vào products table
    product_name VARCHAR(200) NOT NULL,
    unit_price   DECIMAL(15, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INT NOT NULL CHECK (quantity > 0),
    subtotal     DECIMAL(15, 2) NOT NULL CHECK (subtotal >= 0),

    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders(id)   ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng payments — Thanh toán
-- Mỗi đơn hàng có tối đa 1 bản ghi thanh toán
-- Không lưu thông tin thẻ ngân hàng thật
-- =============================================================================
CREATE TABLE IF NOT EXISTS payments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT NOT NULL UNIQUE,
    payment_method    ENUM('COD', 'SIMULATED') NOT NULL,
    status            ENUM('UNPAID','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'UNPAID',
    amount            DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    transaction_note  VARCHAR(255),              -- Ghi chú kết quả mô phỏng
    paid_at           DATETIME,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng inventory_transactions — Lịch sử thay đổi tồn kho
-- change_quantity: dương = nhập kho, âm = xuất kho
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT NOT NULL,
    change_quantity  INT NOT NULL,               -- Có thể âm (xuất kho)
    stock_after      INT NOT NULL CHECK (stock_after >= 0),
    transaction_type ENUM('ORDER_PLACED','ORDER_CANCELLED','MANUAL_ADJUST','INITIAL_STOCK')
                     NOT NULL,
    order_id         BIGINT,                     -- NULL nếu không liên quan đơn hàng
    note             VARCHAR(255),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_inv_product (product_id),
    INDEX idx_inv_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
