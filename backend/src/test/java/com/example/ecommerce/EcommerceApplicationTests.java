package com.example.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Kiểm tra Spring context khởi động thành công.
 * Chạy với profile test sử dụng H2 in-memory DB để không cần kết nối MySQL thật khi test.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EcommerceApplicationTests {

    @Test
    void contextLoads() {
        // Kiểm tra Spring context khởi động thành công
    }
}
