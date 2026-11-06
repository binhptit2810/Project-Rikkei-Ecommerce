-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: ecommerce_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_categories_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Thời trang','Áo, quần, giày dép và phụ kiện thời trang','https://images.unsplash.com/photo-1445205170230-053b83016050?w=600&auto=format&fit=crop&q=80',1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(2,'Điện tử','Điện thoại, máy tính, tai nghe và thiết bị điện tử','https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&auto=format&fit=crop&q=80',1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(3,'Gia dụng','Đồ dùng nhà bếp, nội thất và thiết bị gia đình','https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80',1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(4,'Sách','Sách giáo khoa, tiểu thuyết, kỹ năng sống','https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80',1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(5,'Thể thao','Dụng cụ thể thao, đồ tập gym và outdoor','https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=80',1,'2026-11-02 21:45:18','2026-11-02 21:45:18');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_transactions`
--

DROP TABLE IF EXISTS `inventory_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `change_quantity` int NOT NULL,
  `stock_after` int NOT NULL,
  `transaction_type` enum('INITIAL_STOCK','MANUAL_ADJUST','ORDER_CANCELLED','ORDER_PLACED','MANUAL_IMPORT','MANUAL_EXPORT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inv_product` (`product_id`),
  KEY `idx_inv_created` (`created_at`),
  CONSTRAINT `fk_inv_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
  CONSTRAINT `inventory_transactions_chk_1` CHECK ((`stock_after` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_transactions`
--

LOCK TABLES `inventory_transactions` WRITE;
/*!40000 ALTER TABLE `inventory_transactions` DISABLE KEYS */;
INSERT INTO `inventory_transactions` VALUES (1,1,50,50,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(2,2,30,30,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(3,3,25,25,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(4,4,20,20,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(5,5,15,15,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(6,6,8,8,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(7,7,12,12,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(8,8,20,20,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(9,9,40,40,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(10,10,18,18,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(11,11,35,35,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(12,12,22,22,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18'),(13,13,0,0,'INITIAL_STOCK',NULL,'Nhập kho ban đầu — hết hàng','2026-11-02 21:45:18'),(14,14,15,15,'INITIAL_STOCK',NULL,'Nhập kho ban đầu','2026-11-02 21:45:18');
/*!40000 ALTER TABLE `inventory_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_price` decimal(15,2) NOT NULL,
  `quantity` int NOT NULL,
  `subtotal` decimal(15,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  KEY `idx_order_items_product` (`product_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `order_items_chk_1` CHECK ((`unit_price` >= 0)),
  CONSTRAINT `order_items_chk_2` CHECK ((`quantity` > 0)),
  CONSTRAINT `order_items_chk_3` CHECK ((`subtotal` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `recipient_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recipient_phone` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `shipping_address` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `order_status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING','PROCESSING','SHIPPING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_status` enum('FAILED','PAID','REFUNDED','UNPAID') COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_code` (`order_code`),
  KEY `idx_orders_user` (`user_id`),
  KEY `idx_orders_status` (`order_status`),
  KEY `idx_orders_created` (`created_at`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `orders_chk_1` CHECK ((`total_amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `transaction_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_method` enum('COD','SIMULATED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('FAILED','PAID','REFUNDED','UNPAID') COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `transaction_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_id` (`order_id`),
  UNIQUE KEY `transaction_code` (`transaction_code`),
  KEY `idx_payments_order` (`order_id`),
  KEY `idx_payments_transaction_code` (`transaction_code`),
  CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `payments_chk_1` CHECK ((`amount` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `price` decimal(15,2) NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `stock_quantity` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_products_category` (`category_id`),
  KEY `idx_products_name` (`name`),
  KEY `idx_products_active` (`active`),
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  CONSTRAINT `products_chk_1` CHECK ((`price` >= 0)),
  CONSTRAINT `products_chk_2` CHECK ((`stock_quantity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'Áo thun nam basic Cotton','Áo thun cotton 100%, thoáng mát, dễ phối đồ. Có nhiều màu sắc đa dạng.',199000.00,'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80',1,50,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(2,'Quần jean nam slim fit','Quần jean co giãn nhẹ, dáng slim fit hiện đại, phù hợp đi làm và đi chơi.',450000.00,'https://images.unsplash.com/photo-1542272604-780c96856592?w=600&auto=format&fit=crop&q=80',1,30,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(3,'Áo khoác hoodie nữ','Hoodie lót bông ấm áp, có mũ trùm và túi chuột, phù hợp mùa thu đông.',350000.00,'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=600&auto=format&fit=crop&q=80',1,25,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(4,'Giày thể thao nam','Giày thể thao đế cao su chống trơn trượt, đệm EVA siêu êm chân, phù hợp chạy bộ và tập gym.',850000.00,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80',1,20,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(5,'Tai nghe Bluetooth không dây','Tai nghe over-ear, âm thanh Hi-Fi sống động, chống ồn chủ động ANC, pin trâu 30 giờ.',1200000.00,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80',2,15,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(6,'Bàn phím cơ gaming TKL','Switch cơ quang học nhạy bén, đèn nền LED RGB tùy biến, layout 87 phím nhỏ gọn, kết nối Type-C.',1500000.00,'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600&auto=format&fit=crop&q=80',2,8,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(7,'Chuột gaming wireless','Cảm biến quang học 16000 DPI, kết nối không dây siêu tốc 2.4GHz, trọng lượng siêu nhẹ 65g.',750000.00,'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80',2,12,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(8,'Webcam 1080p Full HD','Webcam độ phân giải Full HD 1080p 60fps, micro kép lọc tạp âm, ống kính tự động lấy nét.',550000.00,'https://images.unsplash.com/photo-1588508065123-287b28e013da?w=600&auto=format&fit=crop&q=80',2,20,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(9,'Bình nước giữ nhiệt 500ml','Inox 304 cao cấp an toàn sức khỏe, cách nhiệt chân không 2 lớp, giữ nóng 12h, giữ lạnh 24h.',350000.00,'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80',3,40,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(10,'Nồi cơm điện 1.8L','Công suất 700W nấu nhanh chín đều, lòng nồi phủ men chống dính siêu bền, nhiều chế độ nấu tự động.',780000.00,'https://images.unsplash.com/photo-1544233726-9f1d2b27be8b?w=600&auto=format&fit=crop&q=80',3,18,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(11,'Clean Code - Robert C. Martin','Sách kinh điển về nghệ thuật viết mã sạch, dễ đọc và dễ bảo trì cho lập trình viên chuyên nghiệp.',180000.00,'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80',4,35,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(12,'Lập trình Spring Boot 3 từ A đến Z','Giáo trình toàn diện về xây dựng kiến trúc Microservices và RESTful API với Spring Boot 3, Java 21.',250000.00,'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80',4,22,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(13,'Bộ tạ tay 2kg x 2 cái','Tạ tay cao cấp bọc cao su chống trầy xước sàn nhà, tay cầm vân kim cương chống trơn hiệu quả.',290000.00,'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80',5,0,1,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(14,'Thảm yoga TPE 6mm','Chất liệu TPE nguyên sinh thân thiện môi trường, chống trơn trượt 2 mặt, êm ái bảo vệ khớp gối.',380000.00,'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600&auto=format&fit=crop&q=80',5,15,1,'2026-11-02 21:45:18','2026-11-02 21:45:18');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(15) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` text COLLATE utf8mb4_unicode_ci,
  `role` enum('ADMIN','CUSTOMER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `lock_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `locked_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_email` (`email`),
  KEY `idx_users_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin@eshop.com','$2a$12$DeJXKsMNyJICotlNotxyRu/9P8LFnQlt5E96Z6V2Vx.eNtRMjdfUC','Quản trị viên','0900000001','Hà Nội, Việt Nam','ADMIN',1,NULL,NULL,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(2,'customer1@example.com','$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK','Nguyễn Văn An','0912345678','123 Đường Lê Lợi, Quận 1, TP.HCM','CUSTOMER',1,NULL,NULL,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(3,'customer2@example.com','$2a$12$.SsIZ3p2ZhYDIEYzPsOKdOVD2UGgW8zC83zXrXD1LBhAY/0bCUJdK','Trần Thị Bình','0987654321','456 Đường Nguyễn Huệ, Quận 3, TP.HCM','CUSTOMER',1,NULL,NULL,'2026-11-02 21:45:18','2026-11-02 21:45:18'),(4,'binhbadboy2810@gmail.com','$2a$12$3zdzGdk4C.Ae8XX4eACo4Ohd5MtlLZXppo8nupC2ZoJNz9Jb8/NSy','Đặng Thanh Bình','0372987662','Ngọc Trục','CUSTOMER',1,NULL,NULL,'2026-11-02 22:48:10','2026-11-02 22:48:10');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-02 23:11:49
