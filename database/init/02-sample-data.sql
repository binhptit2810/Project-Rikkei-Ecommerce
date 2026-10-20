-- =============================================================================
-- 02-sample-data.sql — Dữ liệu mẫu cho môi trường development
-- CẢNH BÁO: Không chạy file này trong môi trường production
-- =============================================================================

USE ecommerce_db;

-- =============================================================================
-- Tài khoản mẫu
-- Mật khẩu: Admin@123 (BCrypt hash — chỉ dùng cho development)
-- Để tạo hash mới: https://bcrypt-generator.com/ với cost factor 12
--
-- QUAN TRỌNG: Đổi mật khẩu ngay khi deploy lên môi trường thực
-- =============================================================================
INSERT INTO users (email, password, full_name, phone, address, role, active) VALUES
(
    'admin@eshop.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYp9Cdi.xJiT7e2',
    -- Mật khẩu: Admin@123 (BCrypt cost=12)
    'Quản trị viên',
    '0900000001',
    'Hà Nội, Việt Nam',
    'ADMIN',
    TRUE
),
(
    'customer1@example.com',
    '$2a$12$eA9Bk8FxL7nYqV6LxXGGh.rTZ9mHiL6M7GqFbXMnT.eZ5FO2/8fDi',
    -- Mật khẩu: Customer@123
    'Nguyễn Văn An',
    '0912345678',
    '123 Đường Lê Lợi, Quận 1, TP.HCM',
    'CUSTOMER',
    TRUE
),
(
    'customer2@example.com',
    '$2a$12$eA9Bk8FxL7nYqV6LxXGGh.rTZ9mHiL6M7GqFbXMnT.eZ5FO2/8fDi',
    -- Mật khẩu: Customer@123
    'Trần Thị Bình',
    '0987654321',
    '456 Đường Nguyễn Huệ, Quận 3, TP.HCM',
    'CUSTOMER',
    TRUE
);

-- =============================================================================
-- Danh mục sản phẩm mẫu
-- =============================================================================
INSERT INTO categories (name, description, active) VALUES
('Thời trang',  'Áo, quần, giày dép và phụ kiện thời trang', TRUE),
('Điện tử',     'Điện thoại, máy tính, tai nghe và thiết bị điện tử', TRUE),
('Gia dụng',    'Đồ dùng nhà bếp, nội thất và thiết bị gia đình', TRUE),
('Sách',        'Sách giáo khoa, tiểu thuyết, kỹ năng sống', TRUE),
('Thể thao',    'Dụng cụ thể thao, đồ tập gym và outdoor', TRUE);

-- =============================================================================
-- Sản phẩm mẫu
-- =============================================================================
INSERT INTO products (name, description, price, category_id, stock_quantity, active) VALUES
-- Thời trang (category_id = 1)
('Áo thun nam basic Cotton', 'Áo thun cotton 100%, thoáng mát, dễ phối đồ. Có nhiều màu sắc.', 199000, 1, 50, TRUE),
('Quần jean nam slim fit', 'Quần jean co giãn nhẹ, dáng slim fit hiện đại, phù hợp đi làm và đi chơi.', 450000, 1, 30, TRUE),
('Áo khoác hoodie nữ', 'Hoodie lót bông ấm, túi chuột, phù hợp mùa lạnh.', 350000, 1, 25, TRUE),
('Giày thể thao nam', 'Giày thể thao đế cao su chống trơn, đế EVA êm chân, phù hợp chạy bộ.', 850000, 1, 20, TRUE),
-- Điện tử (category_id = 2)
('Tai nghe Bluetooth không dây', 'Tai nghe over-ear, âm thanh Hi-Fi, chống ồn chủ động, pin 30 giờ.', 1200000, 2, 15, TRUE),
('Bàn phím cơ gaming TKL', 'Switch blue, đèn LED RGB, layout 87 phím, cổng USB-C.', 1500000, 2, 8, TRUE),
('Chuột gaming wireless', 'Chuột không dây 4000 DPI, pin 70 giờ, trọng lượng nhẹ.', 750000, 2, 12, TRUE),
('Webcam 1080p Full HD', 'Webcam tích hợp micro chống ồn, phù hợp học online và họp video.', 550000, 2, 20, TRUE),
-- Gia dụng (category_id = 3)
('Bình nước giữ nhiệt 500ml', 'Thân thép không gỉ 304, giữ nóng 12h, giữ lạnh 24h.', 350000, 3, 40, TRUE),
('Nồi cơm điện 1.8L', 'Công suất 500W, lòng nồi phủ chống dính 5 lớp, hẹn giờ nấu.', 780000, 3, 18, TRUE),
-- Sách (category_id = 4)
('Clean Code - Robert C. Martin', 'Sách kinh điển về viết code sạch, dễ đọc và bảo trì. Bản dịch tiếng Việt.', 180000, 4, 35, TRUE),
('Lập trình Spring Boot 3 từ A đến Z', 'Hướng dẫn xây dựng REST API với Spring Boot 3 và Java 17+, kèm bài tập.', 250000, 4, 22, TRUE),
-- Thể thao (category_id = 5)
('Bộ tạ tay 2kg x 2 cái', 'Tạ tay bọc cao su, tay cầm chống trơn, phù hợp tập gym tại nhà.', 290000, 5, 0, TRUE),  -- Hết hàng
('Thảm yoga TPE 6mm', 'Thảm yoga chống trơn, dày 6mm, kích thước 183x61cm, kèm túi đựng.', 380000, 5, 15, TRUE);

-- =============================================================================
-- Lịch sử tồn kho ban đầu (INITIAL_STOCK) cho mỗi sản phẩm
-- =============================================================================
INSERT INTO inventory_transactions (product_id, change_quantity, stock_after, transaction_type, note) VALUES
(1,  50,  50, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(2,  30,  30, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(3,  25,  25, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(4,  20,  20, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(5,  15,  15, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(6,   8,   8, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(7,  12,  12, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(8,  20,  20, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(9,  40,  40, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(10, 18,  18, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(11, 35,  35, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(12, 22,  22, 'INITIAL_STOCK', 'Nhập kho ban đầu'),
(13,  0,   0, 'INITIAL_STOCK', 'Nhập kho ban đầu — hết hàng'),
(14, 15,  15, 'INITIAL_STOCK', 'Nhập kho ban đầu');

-- Xác nhận dữ liệu đã được nạp thành công
SELECT 'Schema và dữ liệu mẫu đã được khởi tạo thành công!' AS message;
SELECT COUNT(*) AS total_users FROM users;
SELECT COUNT(*) AS total_categories FROM categories;
SELECT COUNT(*) AS total_products FROM products;
