-- =============================================================================
-- 02-sample-data.sql — Dữ liệu mẫu cho môi trường development
-- CẢNH BÁO: Không chạy file này trong môi trường production
-- =============================================================================

USE ecommerce_db;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

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
    '$2a$12$DeJXKsMNyJICotlNotxyRu/9P8LFnQlt5E96Z6V2Vx.eNtRMjdfUC',
    -- Mật khẩu: Admin@123 (BCrypt cost=12)
    'Quản trị viên',
    '0900000001',
    'Hà Nội, Việt Nam',
    'ADMIN',
    TRUE
),
(
    'customer1@example.com',
    '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK',
    -- Mật khẩu: Customer@123
    'Nguyễn Văn An',
    '0912345678',
    '123 Đường Lê Lợi, Quận 1, TP.HCM',
    'CUSTOMER',
    TRUE
),
(
    'customer2@example.com',
    '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK',
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
INSERT INTO categories (name, description, image_url, active) VALUES
('Thời trang',  'Áo, quần, giày dép và phụ kiện thời trang', 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=600&auto=format&fit=crop&q=80', TRUE),
('Điện tử',     'Điện thoại, máy tính, tai nghe và thiết bị điện tử', 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&auto=format&fit=crop&q=80', TRUE),
('Gia dụng',    'Đồ dùng nhà bếp, nội thất và thiết bị gia đình', 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80', TRUE),
('Sách',        'Sách giáo khoa, tiểu thuyết, kỹ năng sống', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80', TRUE),
('Thể thao',    'Dụng cụ thể thao, đồ tập gym và outdoor', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80', TRUE);

-- =============================================================================
-- Sản phẩm mẫu
-- =============================================================================
INSERT INTO products (name, description, price, image_url, category_id, stock_quantity, active) VALUES
-- Thời trang (category_id = 1)
('Áo thun nam basic Cotton', 'Áo thun cotton 100%, thoáng mát, dễ phối đồ. Có nhiều màu sắc đa dạng.', 199000, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80', 1, 50, TRUE),
('Quần jean nam slim fit', 'Quần jean co giãn nhẹ, dáng slim fit hiện đại, phù hợp đi làm và đi chơi.', 450000, 'https://images.unsplash.com/photo-1542272604-780c96856592?w=600&auto=format&fit=crop&q=80', 1, 30, TRUE),
('Áo khoác hoodie nữ', 'Hoodie lót bông ấm áp, có mũ trùm và túi chuột, phù hợp mùa thu đông.', 350000, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop&q=80', 1, 25, TRUE),
('Giày thể thao nam', 'Giày thể thao đế cao su chống trơn trượt, đệm EVA siêu êm chân, phù hợp chạy bộ và tập gym.', 850000, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80', 1, 20, TRUE),
-- Điện tử (category_id = 2)
('Tai nghe Bluetooth không dây', 'Tai nghe over-ear, âm thanh Hi-Fi sống động, chống ồn chủ động ANC, pin trâu 30 giờ.', 1200000, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80', 2, 15, TRUE),
('Bàn phím cơ gaming TKL', 'Switch cơ quang học nhạy bén, đèn nền LED RGB tùy biến, layout 87 phím nhỏ gọn, kết nối Type-C.', 1500000, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600&auto=format&fit=crop&q=80', 2, 8, TRUE),
('Chuột gaming wireless', 'Cảm biến quang học 16000 DPI, kết nối không dây siêu tốc 2.4GHz, trọng lượng siêu nhẹ 65g.', 750000, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80', 2, 12, TRUE),
('Webcam 1080p Full HD', 'Webcam độ phân giải Full HD 1080p 60fps, micro kép lọc tạp âm, ống kính tự động lấy nét.', 550000, 'https://images.unsplash.com/photo-1588508065123-287b28e013da?w=600&auto=format&fit=crop&q=80', 2, 20, TRUE),
-- Gia dụng (category_id = 3)
('Bình nước giữ nhiệt 500ml', 'Inox 304 cao cấp an toàn sức khỏe, cách nhiệt chân không 2 lớp, giữ nóng 12h, giữ lạnh 24h.', 350000, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80', 3, 40, TRUE),
('Nồi cơm điện 1.8L', 'Công suất 700W nấu nhanh chín đều, lòng nồi phủ men chống dính siêu bền, nhiều chế độ nấu tự động.', 780000, 'https://images.unsplash.com/photo-1544233726-9f1d2b27be8b?w=600&auto=format&fit=crop&q=80', 3, 18, TRUE),
-- Sách (category_id = 4)
('Clean Code - Robert C. Martin', 'Sách kinh điển về nghệ thuật viết mã sạch, dễ đọc và dễ bảo trì cho lập trình viên chuyên nghiệp.', 180000, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80', 4, 35, TRUE),
('Lập trình Spring Boot 3 từ A đến Z', 'Giáo trình toàn diện về xây dựng kiến trúc Microservices và RESTful API với Spring Boot 3, Java 21.', 250000, 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80', 4, 22, TRUE),
-- Thể thao (category_id = 5)
('Bộ tạ tay 2kg x 2 cái', 'Tạ tay cao cấp bọc cao su chống trầy xước sàn nhà, tay cầm vân kim cương chống trơn hiệu quả.', 290000, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80', 5, 0, TRUE),  -- Hết hàng
('Thảm yoga TPE 6mm', 'Chất liệu TPE nguyên sinh thân thiện môi trường, chống trơn trượt 2 mặt, êm ái bảo vệ khớp gối.', 380000, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600&auto=format&fit=crop&q=80', 5, 15, TRUE);

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
