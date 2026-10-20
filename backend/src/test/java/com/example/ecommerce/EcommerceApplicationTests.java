package com.example.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Kiểm tra Spring context khởi động thành công.
 *
 * Chạy với profile test sử dụng H2 in-memory DB:
 *   - Không cần kết nối MySQL thật khi test.
 *   - Hibernate 6 tự nhận diện H2 dialect (không cần khai báo thủ công).
 *   - Spring Security tự configure với các bean đã định nghĩa.
 *   - jwt.secret cần >= 32 ký tự cho HMAC-SHA256.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class EcommerceApplicationTests {

    @Test
    void contextLoads() {
        // Kiểm tra Spring context khởi động thành công với Security, JPA, JWT
    }
}
