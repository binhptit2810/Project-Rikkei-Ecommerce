-- =============================================================================
-- 01-schema.sql — Khởi tạo schema database cho E-commerce Platform
-- MySQL 8.0+
-- Thực thi file này trước file 02-sample-data.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

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
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at DATETIME,
    lock_reason  VARCHAR(500),
    locked_at    DATETIME,
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
    rating_average DECIMAL(3, 1) NOT NULL DEFAULT 0.0,
    total_reviews  INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_products_category (category_id),
    INDEX idx_products_name (name),
    INDEX idx_products_active (active),
    INDEX idx_products_price (price),
    INDEX idx_products_created (created_at),
    INDEX idx_products_active_filter (active, category_id, price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng product_variants — Biến thể sản phẩm theo kích cỡ (Size) [TSK-D14-VDHH]
-- Quản lý số lượng tồn kho riêng biệt theo kích cỡ sản phẩm
-- =============================================================================
CREATE TABLE IF NOT EXISTS product_variants (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    size           VARCHAR(50) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    sku            VARCHAR(100) NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_variant_size UNIQUE (product_id, size),
    CONSTRAINT uk_product_variant_sku UNIQUE (sku),
    INDEX idx_product_variants_product (product_id),
    INDEX idx_product_variants_sku (sku)
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
    coupon_code      VARCHAR(50),
    discount_amount  DECIMAL(15, 2) DEFAULT 0,
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
    INDEX idx_orders_created (created_at),
    INDEX idx_orders_status_created (order_status, created_at),
    INDEX idx_orders_coupon_code (coupon_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng order_items — Chi tiết đơn hàng
-- unit_price & product_name: snapshot giá và tên tại thời điểm đặt hàng
-- =============================================================================
CREATE TABLE IF NOT EXISTS order_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    -- Snapshot thời điểm đặt hàng — không phụ thuộc vào products table
    product_name  VARCHAR(200) NOT NULL,
    selected_size VARCHAR(50),
    unit_price    DECIMAL(15, 2) NOT NULL CHECK (unit_price >= 0),
    quantity      INT NOT NULL CHECK (quantity > 0),
    subtotal      DECIMAL(15, 2) NOT NULL CHECK (subtotal >= 0),

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
    transaction_code  VARCHAR(64) UNIQUE,        -- Mã giao dịch định dạng PAY-YYYYMMDD-XXXX
    vnp_txn_ref       VARCHAR(64),               -- Mã tham chiếu giao dịch VNPay (đối soát & IPN)
    payment_method    ENUM('COD', 'SIMULATED', 'VNPAY') NOT NULL,
    status            ENUM('UNPAID','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'UNPAID',
    amount            DECIMAL(15, 2) NOT NULL CHECK (amount >= 0),
    transaction_note  VARCHAR(255),              -- Ghi chú kết quả mô phỏng
    paid_at           DATETIME,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_transaction_code (transaction_code),
    INDEX idx_payments_vnp_txn_ref (vnp_txn_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng payment_transactions — Lịch sử các lần thử thanh toán (Payment Attempts)
-- =============================================================================
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

-- =============================================================================
-- Bảng inventory_transactions — Lịch sử thay đổi tồn kho
-- change_quantity: dương = nhập kho, âm = xuất kho
-- =============================================================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT NOT NULL,
    change_quantity  INT NOT NULL,               -- Có thể âm (xuất kho)
    stock_after      INT NOT NULL CHECK (stock_after >= 0),
    transaction_type ENUM('ORDER_PLACED','ORDER_CANCELLED','MANUAL_ADJUST','INITIAL_STOCK','MANUAL_IMPORT','MANUAL_EXPORT')
                     NOT NULL,
    order_id         BIGINT,                     -- NULL nếu không liên quan đơn hàng
    note             VARCHAR(255),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_inv_product (product_id),
    INDEX idx_inv_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng coupons — Mã giảm giá / Khuyến mãi
-- discount_type: PERCENT (giảm %) hoặc FIXED (giảm số tiền cố định)
-- =============================================================================
CREATE TABLE IF NOT EXISTS coupons (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50) NOT NULL UNIQUE,
    discount_type    ENUM('PERCENT', 'FIXED') NOT NULL,
    discount_value   DECIMAL(15, 2) NOT NULL CHECK (discount_value > 0),
    min_order_value  DECIMAL(15, 2) DEFAULT 0 CHECK (min_order_value >= 0),
    max_discount     DECIMAL(15, 2) CHECK (max_discount IS NULL OR max_discount >= 0),
    usage_limit      INT DEFAULT NULL CHECK (usage_limit IS NULL OR usage_limit >= 0),
    used_count       INT NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    expire_at        DATETIME,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_coupons_code (code),
    INDEX idx_coupons_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng reviews — Đánh giá & Nhận xét sản phẩm [TSK-D11-DDM]
-- rating: 1 -> 5 sao
-- =============================================================================
CREATE TABLE IF NOT EXISTS reviews (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    user_id        BIGINT NOT NULL,
    rating         INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment        TEXT,
    admin_reply    TEXT,
    admin_reply_at DATETIME,
    is_visible     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_reviews_product_user UNIQUE (product_id, user_id),
    INDEX idx_reviews_product (product_id),
    INDEX idx_reviews_product_created (product_id, created_at),
    INDEX idx_reviews_product_visible (product_id, is_visible),
    INDEX idx_reviews_visible (is_visible),
    INDEX idx_reviews_user (user_id),
    INDEX idx_reviews_rating (rating),
    INDEX idx_reviews_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- Bảng review_images — Hình ảnh đính kèm Đánh giá sản phẩm [TSK-D14-CCT]
-- Tối đa 3 ảnh/đánh giá, lưu trữ đường dẫn ảnh tương đối trên Local Storage
-- =============================================================================
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
