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
-- 6. Đánh giá sản phẩm mẫu (Bảng reviews) [TSK-D13-DDM]
-- Tối thiểu 60 bản ghi, rải đều trên 14 sản phẩm và 11 khách hàng (user_id 2 -> 12).
-- Phân bố số sao hình chuông thực tế:
--   - 5 sao : 32 reviews (53.3%)
--   - 4 sao : 14 reviews (23.3%)
--     => Nhóm 4-5 sao: 46 reviews (76.7%)
--   - 3 sao :  8 reviews (13.3%)
--   - 2 sao :  4 reviews ( 6.7%)
--   - 1 sao :  2 reviews ( 3.3%)
--     => Nhóm 1-2 sao:  6 reviews (10.0%)
-- Tất cả các cặp (product_id, user_id) đều DUY NHẤT để không phạm uk_reviews_product_user.
-- =============================================================================
INSERT IGNORE INTO reviews (product_id, user_id, rating, comment, created_at, updated_at) VALUES
-- --- Sản phẩm #1: Áo thun nam basic Cotton (5 đánh giá: 5, 5, 4, 5, 3 sao) ---
(1,  2, 5, 'Áo mặc cực kỳ mát và nhẹ, chất cotton 100% sờ rất mướt tay. Giặt máy vài lần không thấy xù lông hay dão cổ áo.', '2026-10-10 14:22:15', '2026-10-10 14:22:15'),
(1,  3, 5, 'Form áo basic dễ mặc, tôn dáng. Màu sắc bên ngoài y hệt như hình shop chụp. Rất đáng tiền!', '2026-10-15 09:10:40', '2026-10-15 09:10:40'),
(1,  4, 4, 'Chất vải tốt, đường may chắc chắn. Size L mình mặc hơi rộng xíu nhưng mặc thoải mái đi cafe dạo phố.', '2026-10-22 16:45:00', '2026-10-22 16:45:00'),
(1,  5, 5, 'Giao hàng hỏa tốc trong ngày. Shop đóng gói túi zip kỹ càng, áo thơm tho sạch sẽ. Sẽ mua thêm màu khác.', '2026-11-01 11:30:20', '2026-11-01 11:30:20'),
(1,  6, 3, 'Chất lượng áo tạm ổn trong tầm giá 200k. Vải có hơi mỏng một chút so với kỳ vọng nhưng mặc mùa hè vẫn mát.', '2026-11-05 18:20:10', '2026-11-05 18:20:10'),

-- --- Sản phẩm #2: Quần jean nam slim fit (4 đánh giá: 5, 4, 5, 2 sao) ---
(2,  3, 5, 'Quần co giãn cực tốt, ngồi xuống hay chạy xe máy đều thoải mái không bị bó tức đùi. Màu wash rất nam tính.', '2026-10-12 10:15:30', '2026-10-12 10:15:30'),
(2,  4, 4, 'Form slim fit ôm vừa phải, chất jean dày dặn. Tuy nhiên ống quần hơi dài với chiều cao 1m68 nên phải đi lên lai một chút.', '2026-10-18 15:40:00', '2026-10-18 15:40:00'),
(2,  5, 5, 'Khóa kéo YKK mượt mà, đinh tán kim loại chắc chắn. Mua đợt áp mã voucher FREESHIP giá quá hời.', '2026-10-28 20:05:12', '2026-10-28 20:05:12'),
(2,  7, 2, 'Màu quần thực tế sẫm màu hơn hình một tone. Giặt lần đầu bị phai ra nước xanh khá nhiều, mọi người nên giặt riêng nhé.', '2026-11-06 08:50:45', '2026-11-06 08:50:45'),

-- --- Sản phẩm #3: Áo khoác hoodie nữ (4 đánh giá: 5, 5, 4, 3 sao) ---
(3,  2, 5, 'Lớp lót nỉ bông siêu ấm, mũ trùm đầu to che gió tốt. Màu pastel nhẹ nhàng xinh xắn lắm luôn!', '2026-10-14 11:20:00', '2026-10-14 11:20:00'),
(3,  6, 5, 'Form áo rộng rãi kiểu Hàn Quốc dễ thương. Mặc đi học hay đi làm mùa đông ở miền Bắc bao ấm áp.', '2026-10-25 13:35:10', '2026-10-25 13:35:10'),
(3,  8, 4, 'Áo dày dặn, ấm áp. Túi kangaroo phía trước may đều và rộng, để vừa điện thoại iPhone Pro Max thoải mái.', '2026-11-02 16:15:25', '2026-11-02 16:15:25'),
(3,  9, 3, 'Khóa kéo hơi khít, kéo lúc đầu hơi sượng. Áo mới mở ra có mùi nilon nhẹ, giặt qua một lần là hết.', '2026-11-08 19:40:30', '2026-11-08 19:40:30'),

-- --- Sản phẩm #4: Giày thể thao nam (5 đánh giá: 5, 5, 4, 5, 3 sao) ---
(4,  4, 5, 'Đế EVA siêu nhẹ và đàn hồi tốt! Mình chạy bộ 5km mỗi sáng không hề bị đau gót hay mỏi bàn chân.', '2026-10-11 07:45:15', '2026-10-11 07:45:15'),
(4,  5, 5, 'Thiết kế thể thao khỏe khoắn, phối màu tinh tế. Đế bám sàn gym rất tốt không bị trơn trượt.', '2026-10-20 18:30:40', '2026-10-20 18:30:40'),
(4,  7, 4, 'Giày đi êm chân, thoáng khí. Lót giày tháo rời được tiện vệ sinh. Size giày chuẩn form châu Á.', '2026-10-29 14:10:00', '2026-10-29 14:10:00'),
(4, 10, 5, 'Hàng xịn xò full box kèm dây dự phòng. Mua tặng sinh nhật anh trai, anh khen tấm tắc.', '2026-11-03 10:00:00', '2026-11-03 10:00:00'),
(4, 11, 3, 'Giày đi êm nhưng phần mũi hơi cứng một chút trong 2 ngày đầu, đi quen thì mềm ra bình thường.', '2026-11-09 21:15:10', '2026-11-09 21:15:10'),

-- --- Sản phẩm #5: Tai nghe Bluetooth không dây (5 đánh giá: 5, 5, 4, 5, 1 sao) ---
(5,  2, 5, 'Âm thanh chất lượng đỉnh chóp! Chống ồn chủ động ANC triệt tiêu tiếng ồn văn phòng xuất sắc. Pin dùng cả tuần mới phải sạc.', '2026-10-16 09:30:00', '2026-10-16 09:30:00'),
(5,  5, 5, 'Bass sâu chắc nịch, mid ngọt ngào nghe ballad rất phê. Đệm tai bằng da protein êm ái đeo 3 tiếng không bị đau vành tai.', '2026-10-24 16:50:20', '2026-10-24 16:50:20'),
(5,  8, 4, 'Kết nối bluetooth 5.3 cực nhanh và ổn định, chuyển đổi giữa laptop và điện thoại mượt mà không bị ngắt quãng.', '2026-10-31 11:20:15', '2026-10-31 11:20:15'),
(5,  9, 5, 'Đáng từng xu bát gạo! Có app chỉnh EQ đa dạng, mic đàm thoại gọi họp online rõ ràng không lẫn tạp âm.', '2026-11-04 15:40:00', '2026-11-04 15:40:00'),
(5, 12, 1, 'Hộp hàng bị cấn móp trong quá trình vận chuyển, mở ra tai nghe bên trái bị rè khi bật chống ồn ANC. Đã liên hệ CSKH hỗ trợ đổi mới.', '2026-11-10 17:05:00', '2026-11-10 17:05:00'),

-- --- Sản phẩm #6: Bàn phím cơ gaming TKL (4 đánh giá: 5, 4, 2, 5 sao) ---
(6,  3, 5, 'Switch gõ rất nảy và đầm tay, keycap PBT Doubleshot nhám mịn không lo bám mồ hôi. Dải LED RGB rực rỡ có nhiều chế độ đẹp mắt.', '2026-10-17 21:10:00', '2026-10-17 21:10:00'),
(6,  6, 4, 'Layout 87 phím nhỏ gọn tiết kiệm diện tích bàn làm việc, rê chuột thoải mái khi chơi game FPS. Dây cáp Type-C bọc dù chắc chắn.', '2026-10-26 14:00:00', '2026-10-26 14:00:00'),
(6, 10, 2, 'Tiếng switch clicky hơi to hơn mình tưởng, dùng ban đêm trong phòng ngủ dễ ảnh hưởng người khác. Ai thích êm nên cân nhắc switch red.', '2026-11-02 22:30:15', '2026-11-02 22:30:15'),
(6, 11, 5, 'Build cứng cáp nặng tay, plate kim loại vững chãi gõ không bị flex. Shop tư vấn nhiệt tình đúng nhu cầu.', '2026-11-07 16:20:00', '2026-11-07 16:20:00'),

-- --- Sản phẩm #7: Chuột gaming wireless (4 đánh giá: 5, 4, 5, 3 sao) ---
(7,  4, 5, 'Chuột siêu nhẹ chỉ 65g cầm lướt gió cực sướng. Mắt đọc cảm biến 16k DPI di chuột chuẩn từng pixel trong CS:GO và Valorant.', '2026-10-19 17:15:30', '2026-10-19 17:15:30'),
(7,  7, 4, 'Thời lượng pin ấn tượng, dùng cả tuần làm việc văn phòng mới sạc lại. Form cầm ôm tay kiểu ergo vừa vặn.', '2026-10-27 10:45:00', '2026-10-27 10:45:00'),
(7,  8, 5, 'Feet chuột PTFE trơn tru không cần dán thêm. Không dây 2.4GHz không hề có cảm giác delay chút nào.', '2026-11-03 13:10:20', '2026-11-03 13:10:20'),
(7, 12, 3, 'Chuột ngon trong tầm giá nhưng phần mềm cài đặt macro trên Windows thỉnh thoảng bị lỗi nhận diện cổng USB.', '2026-11-08 20:00:00', '2026-11-08 20:00:00'),

-- --- Sản phẩm #8: Webcam 1080p Full HD (4 đánh giá: 4, 5, 5, 3 sao) ---
(8,  2, 4, 'Chất lượng hình ảnh 1080p sắc nét, màu sắc tự nhiên khi đủ sáng. Kẹp màn hình linh hoạt gắn được cả trên tripod.', '2026-10-13 15:30:00', '2026-10-13 15:30:00'),
(8,  5, 5, 'Micro kép bắt âm thanh trong trẻo, học trực tuyến và họp qua Google Meet tiện lợi không cần đeo tai nghe riêng.', '2026-10-23 09:20:10', '2026-10-23 09:20:10'),
(8,  9, 5, 'Cắm dây USB vào là máy tự nhận driver ngay lập tức, có nắp gạt che ống kính vật lý bảo mật riêng tư an toàn.', '2026-10-30 14:40:00', '2026-10-30 14:40:00'),
(8, 10, 3, 'Trong phòng ánh sáng yếu thì hình ảnh có bị nhiễu hạt nhẹ một chút, cần có đèn bàn hỗ trợ để đạt hình ảnh đẹp nhất.', '2026-11-06 11:15:40', '2026-11-06 11:15:40'),

-- --- Sản phẩm #9: Bình nước giữ nhiệt 500ml (4 đánh giá: 4, 2, 5, 5 sao) ---
(9,  3, 4, 'Bình đẹp tinh tế, lớp sơn tĩnh điện nhám mịn cầm chắc tay. Giữ đá lạnh từ sáng tới tối đá vẫn chưa tan hết.', '2026-10-21 08:15:00', '2026-10-21 08:15:00'),
(9,  6, 2, 'Nắp bình đậy bị kênh nhẹ một bên nếu không vặn cẩn thận, mang bỏ balo nằm ngang thỉnh thoảng bị rỉ vài giọt nước.', '2026-10-29 16:30:20', '2026-10-29 16:30:20'),
(9,  7, 5, 'Chất liệu inox 304 không có mùi hôi kim loại khi uống nước nóng. Dung tích 500ml vừa vặn mang đi làm hằng ngày.', '2026-11-04 10:20:00', '2026-11-04 10:20:00'),
(9, 11, 5, 'Được tặng kèm cọ rửa bình chuyên dụng rất chu đáo. Giữ ấm trà thảo mộc thơm ngon suốt buổi làm việc.', '2026-11-09 14:50:15', '2026-11-09 14:50:15'),

-- --- Sản phẩm #10: Nồi cơm điện 1.8L (4 đánh giá: 5, 5, 4, 3 sao) ---
(10, 4, 5, 'Nấu cơm cực kỳ dẻo và ngọt hạt! Lòng nồi chống dính dày dặn, rửa sạch nhanh chóng không bị dính cháy đáy nồi.', '2026-10-18 12:00:00', '2026-10-18 12:00:00'),
(10, 8, 5, 'Dung tích 1.8L vừa vặn cho gia đình 4-5 người. Nhiều chức năng tiện ích như nấu cháo, hấp rau củ, làm bánh bông lan.', '2026-10-28 17:25:30', '2026-10-28 17:25:30'),
(10, 9, 4, 'Nồi gia nhiệt nhanh tiết kiệm điện năng. Thiết kế màu trắng sang trọng làm nổi bật gian bếp hiện đại.', '2026-11-05 13:40:10', '2026-11-05 13:40:10'),
(10, 12, 3, 'Nồi nấu ngon nhưng dây nguồn hơi ngắn (tầm 1 mét), ổ cắm xa thì phải dùng thêm ổ nối dài.', '2026-11-10 19:10:00', '2026-11-10 19:10:00'),

-- --- Sản phẩm #11: Clean Code - Robert C. Martin (4 đánh giá: 5, 5, 5, 4 sao) ---
(11, 2, 5, 'Cuốn sách bắt buộc phải đọc của mọi lập trình viên! Tác giả phân tích cặn kẽ từ cách đặt tên biến, tách hàm cho đến xử lý ngoại lệ.', '2026-10-08 19:30:00', '2026-10-08 19:30:00'),
(11, 3, 5, 'Sách in giấy vàng ngà chống mỏi mắt, mực in sắc nét không bị nhòe. Đóng gói bọc bìa carton chống cong gập góc sách cẩn thận.', '2026-10-19 14:15:20', '2026-10-19 14:15:20'),
(11, 6, 5, 'Đọc xong cuốn này tư duy viết code và refactoring của mình nâng lên một tầm cao mới. Đề xuất nhiệt liệt cho các bạn dev trẻ!', '2026-10-31 09:00:00', '2026-10-31 09:00:00'),
(11, 10, 4, 'Nội dung sách quá hay và giá trị. Bản dịch tiếng Việt dễ hiểu, một vài thuật ngữ chuyên ngành vẫn giữ nguyên tiếng Anh chuẩn xác.', '2026-11-07 11:25:40', '2026-11-07 11:25:40'),

-- --- Sản phẩm #12: Lập trình Spring Boot 3 từ A đến Z (5 đánh giá: 5, 5, 5, 4, 5 sao) ---
(12, 4, 5, 'Bộ tài liệu cực kỳ thực chiến! Hướng dẫn chi tiết từ Spring Data JPA, Hibernate, Spring Security 6 cho đến Docker hóa ứng dụng.', '2026-10-15 20:45:00', '2026-10-15 20:45:00'),
(12, 5, 5, 'Ví dụ minh họa rất sát với dự án thực tế doanh nghiệp. Đã áp dụng kiến trúc bài bản vào đồ án tốt nghiệp đạt điểm A+.', '2026-10-25 15:10:15', '2026-10-25 15:10:15'),
(12, 7, 5, 'Tác giả giải thích sâu về cơ chế Spring Bean lifecycle và tối ưu truy vấn N+1 problem rất dễ nắm bắt.', '2026-11-02 08:30:00', '2026-11-02 08:30:00'),
(12, 11, 4, 'Sách bìa cứng trang trọng, lộ trình học từ cơ bản đến nâng cao rõ ràng. Tặng kèm source code demo trên GitHub rất tiện theo dõi.', '2026-11-06 14:15:30', '2026-11-06 14:15:30'),
(12, 12, 5, 'Cuốn cẩm nang không thể thiếu cho các bạn Java Backend developer trong năm 2026. Mua đợt sale còn được freeship.', '2026-11-10 10:20:00', '2026-11-10 10:20:00'),

-- --- Sản phẩm #13: Bộ tạ tay 2kg x 2 cái (4 đánh giá: 4, 3, 5, 1 sao) ---
(13, 8, 4, 'Tạ tay cầm đầm, lớp bọc cao su dày dặn đặt xuống sàn gạch không sợ bị sứt mẻ. Phù hợp cho chị em tập cardio tại nhà.', '2026-10-22 17:00:00', '2026-10-22 17:00:00'),
(13, 9, 3, 'Tạ mới bóc hộp có mùi cao su hơi nồng, để ngoài ban công thoáng gió 2 hôm thì mùi bay bớt. Tập luyện ổn định.', '2026-10-30 11:45:10', '2026-10-30 11:45:10'),
(13, 10, 5, 'Màu sắc trẻ trung bắt mắt, tay nắm vân nhám chống trơn mồ hôi tốt. Dù hết hàng nhưng đợt trước mua được rất ưng ý.', '2026-11-04 16:30:00', '2026-11-04 16:30:00'),
(13, 2, 1, 'Giao nhầm cho mình tạ 1.5kg thay vì tạ 2kg. Đã bấm nút khiếu nại trả hàng hoàn tiền.', '2026-11-09 09:15:45', '2026-11-09 09:15:45'),

-- --- Sản phẩm #14: Thảm yoga TPE 6mm (4 đánh giá: 5, 5, 4, 2 sao) ---
(14, 3, 5, 'Thảm dày 6mm bảo vệ đầu gối và khuỷu tay rất êm khi tập plank hay asana khó. Mặt dưới bám sàn gỗ siêu chắc chắn.', '2026-10-20 07:30:00', '2026-10-20 07:30:00'),
(14, 6, 5, 'Chất liệu TPE không thấm mồ hôi, dễ lau chùi sau mỗi buổi tập. Tặng kèm dây đeo vai tiện lợi mang đến phòng tập.', '2026-10-28 18:20:15', '2026-10-28 18:20:15'),
(14, 7, 4, 'Độ đàn hồi cao, cuộn lại mở ra không bị quăn mép thảm. Màu xanh ngọc bên ngoài nhìn rất sang chảnh.', '2026-11-03 14:10:00', '2026-11-03 14:10:00'),
(14, 11, 2, 'Thảm có độ bám ổn nhưng khi tay ra nhiều mồ hôi thì vẫn có cảm giác bị trượt nhẹ. Người nhiều mồ hôi nên mua thêm khăn trải thảm.', '2026-11-08 16:45:20', '2026-11-08 16:45:20');

-- =============================================================================
-- 7. TỰ ĐỘNG ĐỒNG BỘ RATING TRÊN BẢNG products [TSK-D13-DDM]
-- Cập nhật rating_average (làm tròn 1 chữ số thập phân) và total_reviews
-- trực tiếp từ dữ liệu reviews thực tế vừa nạp ở trên.
-- =============================================================================
UPDATE products p
JOIN (
    SELECT 
        product_id,
        ROUND(AVG(rating), 1) AS avg_rating,
        COUNT(id) AS review_count
    FROM reviews
    GROUP BY product_id
) r ON p.id = r.product_id
SET 
    p.rating_average = r.avg_rating,
    p.total_reviews  = r.review_count;

-- Kiểm tra lại sản phẩm không có đánh giá (nếu có) để đảm bảo không bị NULL
UPDATE products
SET 
    rating_average = COALESCE(rating_average, 0.0),
    total_reviews  = COALESCE(total_reviews, 0)
WHERE rating_average IS NULL OR total_reviews IS NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- Xác nhận kết quả nạp dữ liệu mẫu
-- =============================================================================
SELECT 'Nạp dữ liệu mẫu Đánh giá (reviews) & Mã giảm giá (coupons) thành công!' AS result,
       (SELECT COUNT(*) FROM coupons) AS total_coupons,
       (SELECT COUNT(*) FROM reviews) AS total_reviews,
       (SELECT COUNT(*) FROM products WHERE total_reviews > 0) AS rated_products;
