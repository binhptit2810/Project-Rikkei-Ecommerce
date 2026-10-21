-- Script sửa lỗi phông chữ tiếng Việt (Mojibake) và cập nhật ảnh thật từ mạng cho sản phẩm
USE ecommerce_db;

SET NAMES utf8mb4;

-- 1. Sửa lỗi phông chữ bảng categories
UPDATE categories SET name = 'Thời trang', description = 'Áo, quần, giày dép và phụ kiện thời trang', image_url = 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=600&auto=format&fit=crop&q=80' WHERE id = 1;
UPDATE categories SET name = 'Điện tử', description = 'Điện thoại, máy tính, tai nghe và thiết bị điện tử', image_url = 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&auto=format&fit=crop&q=80' WHERE id = 2;
UPDATE categories SET name = 'Gia dụng', description = 'Đồ dùng nhà bếp, nội thất và thiết bị gia đình', image_url = 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80' WHERE id = 3;
UPDATE categories SET name = 'Sách', description = 'Sách giáo khoa, tiểu thuyết, kỹ năng sống', image_url = 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80' WHERE id = 4;
UPDATE categories SET name = 'Thể thao', description = 'Dụng cụ thể thao, đồ tập gym và outdoor', image_url = 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80' WHERE id = 5;

-- 2. Sửa lỗi phông chữ và bổ sung ảnh thật cho các sản phẩm
UPDATE products SET 
    name = 'Áo thun nam basic Cotton', 
    description = 'Áo thun cotton 100%, thoáng mát, dễ phối đồ. Có nhiều màu sắc đa dạng.', 
    price = 199000, 
    image_url = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 50,
    active = TRUE
WHERE id = 1;

UPDATE products SET 
    name = 'Quần jean nam slim fit', 
    description = 'Quần jean co giãn nhẹ, dáng slim fit hiện đại, phù hợp đi làm và đi chơi.', 
    price = 450000, 
    image_url = 'https://images.unsplash.com/photo-1542272604-780c96856592?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 30,
    active = TRUE
WHERE id = 2;

UPDATE products SET 
    name = 'Áo khoác hoodie nữ', 
    description = 'Hoodie lót bông ấm áp, có mũ trùm và túi chuột, phù hợp mùa thu đông.', 
    price = 350000, 
    image_url = 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 25,
    active = TRUE
WHERE id = 3;

UPDATE products SET 
    name = 'Giày thể thao nam', 
    description = 'Giày thể thao đế cao su chống trơn trượt, đệm EVA siêu êm chân, phù hợp chạy bộ và tập gym.', 
    price = 850000, 
    image_url = 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 20,
    active = TRUE
WHERE id = 4;

UPDATE products SET 
    name = 'Tai nghe Bluetooth không dây', 
    description = 'Tai nghe over-ear, âm thanh Hi-Fi sống động, chống ồn chủ động ANC, pin trâu 30 giờ.', 
    price = 1200000, 
    image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 15,
    active = TRUE
WHERE id = 5;

UPDATE products SET 
    name = 'Bàn phím cơ gaming TKL', 
    description = 'Switch cơ quang học nhạy bén, đèn nền LED RGB tùy biến, layout 87 phím nhỏ gọn, kết nối Type-C.', 
    price = 1500000, 
    image_url = 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 8,
    active = TRUE
WHERE id = 6;

UPDATE products SET 
    name = 'Chuột gaming wireless', 
    description = 'Cảm biến quang học 16000 DPI, kết nối không dây siêu tốc 2.4GHz, trọng lượng siêu nhẹ 65g.', 
    price = 750000, 
    image_url = 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 12,
    active = TRUE
WHERE id = 7;

UPDATE products SET 
    name = 'Webcam 1080p Full HD', 
    description = 'Webcam độ phân giải Full HD 1080p 60fps, micro kép lọc tạp âm, ống kính tự động lấy nét.', 
    price = 550000, 
    image_url = 'https://images.unsplash.com/photo-1588508065123-287b28e013da?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 20,
    active = TRUE
WHERE id = 8;

UPDATE products SET 
    name = 'Bình nước giữ nhiệt 500ml', 
    description = 'Inox 304 cao cấp an toàn sức khỏe, cách nhiệt chân không 2 lớp, giữ nóng 12h, giữ lạnh 24h.', 
    price = 350000, 
    image_url = 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 40,
    active = TRUE
WHERE id = 9;

UPDATE products SET 
    name = 'Nồi cơm điện 1.8L', 
    description = 'Công suất 700W nấu nhanh chín đều, lòng nồi phủ men chống dính siêu bền, nhiều chế độ nấu tự động.', 
    price = 780000, 
    image_url = 'https://images.unsplash.com/photo-1544233726-9f1d2b27be8b?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 18,
    active = TRUE
WHERE id = 10;

UPDATE products SET 
    name = 'Clean Code - Robert C. Martin', 
    description = 'Sách kinh điển về nghệ thuật viết mã sạch, dễ đọc và dễ bảo trì cho lập trình viên chuyên nghiệp.', 
    price = 180000, 
    image_url = 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 35,
    active = TRUE
WHERE id = 11;

UPDATE products SET 
    name = 'Lập trình Spring Boot 3 từ A đến Z', 
    description = 'Giáo trình toàn diện về xây dựng kiến trúc Microservices và RESTful API với Spring Boot 3, Java 21.', 
    price = 250000, 
    image_url = 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 22,
    active = TRUE
WHERE id = 12;

UPDATE products SET 
    name = 'Bộ tạ tay 2kg x 2 cái', 
    description = 'Tạ tay cao cấp bọc cao su chống trầy xước sàn nhà, tay cầm vân kim cương chống trơn hiệu quả.', 
    price = 290000, 
    image_url = 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 0,
    active = TRUE
WHERE id = 13;

UPDATE products SET 
    name = 'Thảm yoga TPE 6mm', 
    description = 'Chất liệu TPE nguyên sinh thân thiện môi trường, chống trơn trượt 2 mặt, êm ái bảo vệ khớp gối.', 
    price = 380000, 
    image_url = 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600&auto=format&fit=crop&q=80',
    stock_quantity = 15,
    active = TRUE
WHERE id = 14;

-- Thay thế sản phẩm troll ID 15 thành sản phẩm thời trang chỉn chu
UPDATE products SET 
    name = 'Áo sơ mi nam công sở Oxford', 
    description = 'Áo sơ mi dài tay dệt sợi Oxford cao cấp, đứng form, thấm hút mồ hôi tốt, chống nhăn tự nhiên.', 
    price = 299000, 
    image_url = 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80',
    category_id = 1,
    stock_quantity = 45,
    active = TRUE
WHERE id = 15;
