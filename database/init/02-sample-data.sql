-- =============================================================================
-- 02-sample-data.sql — Dữ liệu mẫu cho môi trường development & kiểm thử
-- Task ID: [TSK-D13-DDM] Nạp Dữ Liệu Mẫu Đánh Giá & Voucher Phong Phú
-- Phụ trách: Đỗ Đức Mạnh (DDM)
--
-- Thứ tự nạp bảng:
--   1. users
--   2. categories
--   3. products
--   4. inventory_transactions
--   5. coupons
--   6. reviews
--   7. UPDATE products (đồng bộ rating_average và total_reviews)
--
-- Đảm bảo tính Idempotent: chạy lại nhiều lần không sinh lỗi trùng lặp dữ liệu.
-- =============================================================================

USE ecommerce_db;

SET FOREIGN_KEY_CHECKS = 0;
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- =============================================================================
-- 1. Tài khoản mẫu (Bảng users)
-- Mật khẩu mặc định:
--   - Admin: Admin@123 ($2a$12$DeJXKsMNyJICotlNotxyRu/9P8LFnQlt5E96Z6V2Vx.eNtRMjdfUC)
--   - Customer: Customer@123 ($2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK)
-- =============================================================================
INSERT IGNORE INTO users (id, email, password, full_name, phone, address, role, active, email_verified, email_verified_at, created_at, updated_at) VALUES
(1,  'admin@eshop.com',       '$2a$12$DeJXKsMNyJICotlNotxyRu/9P8LFnQlt5E96Z6V2Vx.eNtRMjdfUC', 'Quản trị viên',   '0900000001', 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy, Hà Nội',       'ADMIN',    TRUE, TRUE, '2026-10-01 08:00:00', '2026-10-01 08:00:00', '2026-10-01 08:00:00'),
(2,  'customer1@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Nguyễn Văn An',   '0912345678', '123 Đường Lê Lợi, Bến Nghé, Quận 1, TP.HCM',       'CUSTOMER', TRUE, TRUE, '2026-10-02 09:15:00', '2026-10-02 09:15:00', '2026-10-02 09:15:00'),
(3,  'customer2@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Trần Thị Bình',   '0987654321', '456 Đường Nguyễn Huệ, Phường 2, Quận 3, TP.HCM',    'CUSTOMER', TRUE, TRUE, '2026-10-03 10:20:00', '2026-10-03 10:20:00', '2026-10-03 10:20:00'),
(4,  'customer3@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Lê Hoàng Long',   '0934567890', '78 Đường Bạch Đằng, Hải Châu 1, Hải Châu, Đà Nẵng', 'CUSTOMER', TRUE, TRUE, '2026-10-05 14:00:00', '2026-10-05 14:00:00', '2026-10-05 14:00:00'),
(5,  'customer4@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Phạm Minh Tuấn', '0945678901', '12 Phố Cầu Đất, Ngô Quyền, Hải Phòng',              'CUSTOMER', TRUE, TRUE, '2026-10-08 11:30:00', '2026-10-08 11:30:00', '2026-10-08 11:30:00'),
(6,  'customer5@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Hoàng Thùy Linh', '0956789012', '90 Đại lộ Hòa Bình, Ninh Kiều, Cần Thơ',            'CUSTOMER', TRUE, TRUE, '2026-10-10 16:45:00', '2026-10-10 16:45:00', '2026-10-10 16:45:00'),
(7,  'customer6@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Vũ Đức Anh',     '0967890123', '234 Đường Kim Mã, Ba Đình, Hà Nội',                'CUSTOMER', TRUE, TRUE, '2026-10-12 08:30:00', '2026-10-12 08:30:00', '2026-10-12 08:30:00'),
(8,  'customer7@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Đỗ Thị Mai',      '0978901234', '56 Đường Hạ Long, Bãi Cháy, TP. Hạ Long, Quảng Ninh','CUSTOMER', TRUE, TRUE, '2026-10-15 13:10:00', '2026-10-15 13:10:00', '2026-10-15 13:10:00'),
(9,  'customer8@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Bùi Quốc Huy',   '0989012345', '89 Đường Đồng Khởi, Tân Hiệp, Biên Hòa, Đồng Nai', 'CUSTOMER', TRUE, TRUE, '2026-10-18 15:20:00', '2026-10-18 15:20:00', '2026-10-18 15:20:00'),
(10, 'customer9@example.com', '$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Ngô Phương Thảo', '0990123456', '15 Đường Lý Thái Tổ, Võ Cường, TP. Bắc Ninh',      'CUSTOMER', TRUE, TRUE, '2026-10-20 17:05:00', '2026-10-20 17:05:00', '2026-10-20 17:05:00'),
(11, 'customer10@example.com','$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Đinh Gia Bảo',   '0901234567', '345 Đường Quang Trung, Phường 10, Gò Vấp, TP.HCM', 'CUSTOMER', TRUE, TRUE, '2026-10-22 19:40:00', '2026-10-22 19:40:00', '2026-10-22 19:40:00'),
(12, 'customer11@example.com','$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK', 'Võ Ngọc Ánh',    '0911223344', '67 Đường Yersin, Phú Cường, Thủ Dầu Một, Bình Dương','CUSTOMER', TRUE, TRUE, '2026-10-25 09:50:00', '2026-10-25 09:50:00', '2026-10-25 09:50:00');

-- =============================================================================
-- 2. Danh mục sản phẩm mẫu (Bảng categories)
-- =============================================================================
INSERT IGNORE INTO categories (id, name, description, image_url, active) VALUES
(1, 'Thời trang', 'Áo, quần, giày dép và phụ kiện thời trang nam nữ cao cấp',       'https://images.unsplash.com/photo-1445205170230-053b83016050?w=600&auto=format&fit=crop&q=80', TRUE),
(2, 'Điện tử',    'Điện thoại, máy tính, tai nghe và thiết bị công nghệ hiện đại',  'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&auto=format&fit=crop&q=80', TRUE),
(3, 'Gia dụng',   'Đồ dùng nhà bếp thông minh, nội thất và thiết bị gia đình',     'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80', TRUE),
(4, 'Sách',       'Sách công nghệ thông tin, lập trình viên, kinh tế và kỹ năng',   'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80', TRUE),
(5, 'Thể thao',   'Dụng cụ tập thể thao, yoga, phụ kiện gym và dã ngoại outdoor',  'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80', TRUE);

-- =============================================================================
-- 3. Sản phẩm mẫu (Bảng products)
-- rating_average và total_reviews sẽ được cập nhật đồng bộ ở cuối script
-- =============================================================================
INSERT IGNORE INTO products (id, name, description, price, image_url, category_id, stock_quantity, active, rating_average, total_reviews) VALUES
-- Thời trang (category_id = 1)
(1,  'Áo thun nam basic Cotton',    'Áo thun cotton 100%, thoáng mát, thấm hút mồ hôi, dễ phối đồ, giặt không phai màu.', 199000, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80', 1, 50, TRUE, 0.0, 0),
(2,  'Quần jean nam slim fit',      'Quần jean co giãn nhẹ, form dáng slim fit hiện đại, đường may tỉ mỉ, phù hợp đi làm và đi chơi.', 450000, 'https://images.unsplash.com/photo-1542272604-780c96856592?w=600&auto=format&fit=crop&q=80', 1, 30, TRUE, 0.0, 0),
(3,  'Áo khoác hoodie nữ',          'Hoodie nỉ bông dày dặn ấm áp, có mũ trùm đầu và túi kangaroo tiện lợi, phù hợp mùa thu đông.', 350000, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop&q=80', 1, 25, TRUE, 0.0, 0),
(4,  'Giày thể thao nam',           'Giày sneaker đế cao su non chống trượt, đệm EVA siêu êm chân, phù hợp chạy bộ và tập gym.', 850000, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80', 1, 20, TRUE, 0.0, 0),
-- Điện tử (category_id = 2)
(5,  'Tai nghe Bluetooth không dây','Tai nghe over-ear cao cấp, âm thanh Hi-Fi sống động, chống ồn chủ động ANC, pin trâu 30 giờ.', 1200000, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80', 2, 15, TRUE, 0.0, 0),
(6,  'Bàn phím cơ gaming TKL',      'Switch cơ quang học độ nhạy cao, đèn nền RGB 16.8 triệu màu tùy biến, layout 87 phím nhỏ gọn Type-C.', 1500000, 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600&auto=format&fit=crop&q=80', 2, 8, TRUE, 0.0, 0),
(7,  'Chuột gaming wireless',       'Cảm biến quang học chính xác 16.000 DPI, kết nối không dây 2.4GHz không độ trễ, trọng lượng siêu nhẹ 65g.', 750000, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80', 2, 12, TRUE, 0.0, 0),
(8,  'Webcam 1080p Full HD',        'Độ phân giải Full HD 1080p 60fps mượt mà, micro kép khử tiếng ồn thông minh, tự động cân bằng sáng.', 550000, 'https://images.unsplash.com/photo-1588508065123-287b28e013da?w=600&auto=format&fit=crop&q=80', 2, 20, TRUE, 0.0, 0),
-- Gia dụng (category_id = 3)
(9,  'Bình nước giữ nhiệt 500ml',   'Ruột inox 304 cao cấp an toàn thực phẩm, cách nhiệt chân không 2 lớp, giữ nóng 12h, giữ lạnh 24h.', 350000, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80', 3, 40, TRUE, 0.0, 0),
(10, 'Nồi cơm điện 1.8L',           'Công suất 700W gia nhiệt tuần hoàn, lòng nồi hợp kim phủ men chống dính siêu bền, đa chế độ nấu.', 780000, 'https://images.unsplash.com/photo-1544233726-9f1d2b27be8b?w=600&auto=format&fit=crop&q=80', 3, 18, TRUE, 0.0, 0),
-- Sách (category_id = 4)
(11, 'Clean Code - Robert C. Martin','Sách kinh điển về nghệ thuật viết mã nguồn sạch, dễ đọc và dễ bảo trì cho kỹ sư phần mềm chuyên nghiệp.', 180000, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80', 4, 35, TRUE, 0.0, 0),
(12, 'Lập trình Spring Boot 3 từ A đến Z','Giáo trình toàn diện về xây dựng kiến trúc Microservices, RESTful API và Cloud Native với Spring Boot 3 & Java 21.', 250000, 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80', 4, 22, TRUE, 0.0, 0),
-- Thể thao (category_id = 5)
(13, 'Bộ tạ tay 2kg x 2 cái',       'Tạ tay đúc đặc bọc cao su cao cấp chống trầy xước sàn nhà, vân kim cương chống trơn trượt hiệu quả.', 290000, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80', 5, 0, TRUE, 0.0, 0),
(14, 'Thảm yoga TPE 6mm',           'Chất liệu TPE nguyên sinh thân thiện môi trường, vân sóng chống trượt 2 mặt, êm ái bảo vệ khớp xương.', 380000, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600&auto=format&fit=crop&q=80', 5, 15, TRUE, 0.0, 0);

-- =============================================================================
-- 4. Lịch sử tồn kho ban đầu (Bảng inventory_transactions)
-- =============================================================================
INSERT IGNORE INTO inventory_transactions (id, product_id, change_quantity, stock_after, transaction_type, note, created_at) VALUES
(1,  1,  50,  50, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(2,  2,  30,  30, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(3,  3,  25,  25, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(4,  4,  20,  20, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(5,  5,  15,  15, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(6,  6,   8,   8, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(7,  7,  12,  12, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(8,  8,  20,  20, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(9,  9,  40,  40, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(10, 10, 18,  18, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(11, 11, 35,  35, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(12, 12, 22,  22, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00'),
(13, 13,  0,   0, 'INITIAL_STOCK', 'Nhập kho ban đầu — tạm hết hàng', '2026-10-01 08:00:00'),
(14, 14, 15,  15, 'INITIAL_STOCK', 'Nhập kho ban đầu', '2026-10-01 08:00:00');

-- =============================================================================
-- 4.1 Biến thể sản phẩm theo kích cỡ (Bảng product_variants) [TSK-D14-VDHH]
-- =============================================================================
INSERT IGNORE INTO product_variants (id, product_id, size, stock_quantity, sku, created_at, updated_at) VALUES
-- Áo thun nam basic Cotton (product_id = 1, tổng tồn 50)
(1,  1, 'S',   10, 'PROD-1-S',   NOW(), NOW()),
(2,  1, 'M',   15, 'PROD-1-M',   NOW(), NOW()),
(3,  1, 'L',   15, 'PROD-1-L',   NOW(), NOW()),
(4,  1, 'XL',  10, 'PROD-1-XL',  NOW(), NOW()),
(5,  1, 'XXL',  0, 'PROD-1-XXL', NOW(), NOW()), -- Hết hàng phục vụ test UI disabled

-- Quần jean nam slim fit (product_id = 2, tổng tồn 30)
(6,  2, '29',   5, 'PROD-2-29',  NOW(), NOW()),
(7,  2, '30',  10, 'PROD-2-30',  NOW(), NOW()),
(8,  2, '31',  10, 'PROD-2-31',  NOW(), NOW()),
(9,  2, '32',   5, 'PROD-2-32',  NOW(), NOW()),
(10, 2, '33',   0, 'PROD-2-33',  NOW(), NOW()), -- Hết hàng

-- Áo khoác hoodie nữ (product_id = 3, tổng tồn 25)
(11, 3, 'S',    5, 'PROD-3-S',   NOW(), NOW()),
(12, 3, 'M',   10, 'PROD-3-M',   NOW(), NOW()),
(13, 3, 'L',   10, 'PROD-3-L',   NOW(), NOW()),
(14, 3, 'XL',   0, 'PROD-3-XL',  NOW(), NOW()), -- Hết hàng

-- Giày thể thao nam (product_id = 4, tổng tồn 20)
(15, 4, '39',   4, 'PROD-4-39',  NOW(), NOW()),
(16, 4, '40',   6, 'PROD-4-40',  NOW(), NOW()),
(17, 4, '41',   6, 'PROD-4-41',  NOW(), NOW()),
(18, 4, '42',   4, 'PROD-4-42',  NOW(), NOW()),
(19, 4, '43',   0, 'PROD-4-43',  NOW(), NOW()); -- Hết hàng

-- =============================================================================
-- 5. Mã giảm giá phong phú (Bảng coupons) [TSK-D13-DDM]
-- Đáp ứng đầy đủ các loại voucher thực tế:
--   - WELCOME10 : Giảm 10%, tối đa 50k, đơn tối thiểu 200k
--   - FREESHIP  : Giảm cố định 30k (Free Shipping), đơn tối thiểu 150k
--   - TECH50K   : Giảm thẳng 50k, đơn tối thiểu 500k
--   - BIGSALE20 : Giảm 20%, tối đa 200k, đơn tối thiểu 1.000k
--   - VIPMEMBER : Giảm thẳng 100k cho khách thân thiết, đơn tối thiểu 800k
--   - GIAM50K   : Giảm thẳng 50k, đơn tối thiểu 300k
--   - EXPIRED20 : Voucher đã hết hạn (phục vụ test validation giỏ hàng)
--   - MAXOUT    : Voucher đã hết lượt dùng (phục vụ test validation giỏ hàng)
-- =============================================================================
INSERT INTO coupons (code, discount_type, discount_value, min_order_value, max_discount, usage_limit, used_count, expire_at, active, created_at, updated_at) VALUES
('WELCOME10', 'PERCENT', 10.00,  200000.00,  50000.00, 200, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('FREESHIP',  'FIXED',   30000.00, 150000.00, NULL,     500, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('TECH50K',   'FIXED',   50000.00, 500000.00, NULL,     150, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('BIGSALE20', 'PERCENT', 20.00, 1000000.00, 200000.00, 100, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('VIPMEMBER', 'FIXED',  100000.00, 800000.00, NULL,     100, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('GIAM50K',   'FIXED',   50000.00, 300000.00, NULL,     100, 0, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00'),
('EXPIRED20', 'PERCENT', 20.00,  100000.00, 100000.00,  50, 0, '2026-11-01 00:00:00', TRUE, '2026-10-01 00:00:00', '2026-10-01 00:00:00'),
('MAXOUT',    'FIXED',   20000.00, 100000.00, NULL,       5, 5, '2026-12-31 23:59:59', TRUE, '2026-11-01 00:00:00', '2026-11-01 00:00:00')
ON DUPLICATE KEY UPDATE
    discount_type   = VALUES(discount_type),
    discount_value  = VALUES(discount_value),
    min_order_value = VALUES(min_order_value),
    max_discount    = VALUES(max_discount),
    usage_limit     = VALUES(usage_limit),
    used_count      = VALUES(used_count),
    expire_at       = VALUES(expire_at),
    active          = VALUES(active);

-- =============================================================================
-- 6. Đánh giá sản phẩm (Bảng reviews)
-- Không nạp dữ liệu đánh giá mẫu để đảm bảo 100% dữ liệu đánh giá là do người dùng thật tạo.
-- =============================================================================

-- =============================================================================
-- 7. KHỞI TẠO GIÁ TRỊ RATING MẶC ĐỊNH CHO TẤT CẢ SẢN PHẨM
-- Đảm bảo toàn bộ sản phẩm ban đầu có rating_average = 0.0 và total_reviews = 0
-- =============================================================================
UPDATE products
SET 
    rating_average = 0.0,
    total_reviews  = 0;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- Xác nhận kết quả nạp dữ liệu mẫu
-- =============================================================================
SELECT 'Nạp dữ liệu mẫu & Mã giảm giá (coupons) thành công!' AS result,
       (SELECT COUNT(*) FROM coupons) AS total_coupons,
       (SELECT COUNT(*) FROM reviews) AS total_reviews,
       (SELECT COUNT(*) FROM products WHERE total_reviews > 0) AS rated_products;
