-- =============================================================================
-- migration_fix_product_images.sql
-- Sửa lại hình ảnh cho các sản phẩm bị nhầm ảnh đồng hồ (photo-1523275335684-37898b6baf30)
-- =============================================================================

USE ecommerce_db;

-- Thời trang
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Áo sơ mi Oxford%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Quần tây nam%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Áo khoác Bomber%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Chân váy xếp ly%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Áo len cổ lọ%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Đầm suông%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Quần short kaki%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1560343090-f0409e92791a?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Giày lười da bò%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Thắt lưng da%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Áo polo thể thao%';

-- Điện tử
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Màn hình Gaming%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Loa Bluetooth%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Ổ cứng SSD%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Củ sạc nhanh%' OR name LIKE '%Cáp sạc%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1586953208448-b95a79798f07?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Giá đỡ kiêm sạc%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1544652478-6653e09f18a2?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Hub chuyển đổi%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Micro thu âm%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Đèn LED kẹp%';

-- Gia dụng
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Nồi chiên không dầu%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Máy xay sinh tố%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Máy hút bụi%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Bàn là hơi nước%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Bộ hộp thủy tinh%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Ấm đun nước%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1618941716939-553df3c6c278?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Quạt tích điện%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Đèn bàn học%';

-- Sách
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Refactoring%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Designing Data-Intensive Applications%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Hành trình về Phương Đông%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Đắc Nhân Tâm%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Nhà Giả Kim%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Tư Duy Nhanh Và Chậm%';

-- Thể thao
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Dây nhảy thể lực%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1519861531473-9200262188bf?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Bóng rổ%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Quả bóng đá%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1530549387789-4c1017266635?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Kính bơi%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Con lăn tập cơ bụng%';

UPDATE products SET image_url = 'https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=600&auto=format&fit=crop&q=80'
WHERE name LIKE '%Bộ dây kháng lực%';

