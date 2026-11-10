package com.example.ecommerce;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test xác minh cơ chế tự động nạp biến môi trường từ file .env của EcommerceApplication.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT]
 */
class EcommerceApplicationDotEnvTest {

    @Test
    @DisplayName("DOTENV-01: EcommerceApplication.loadDotEnvIfPresent nạp thành công biến từ file .env")
    void testLoadDotEnvIfPresent_Success() {
        // Thực thi hàm load từ file .env thực tế của dự án
        EcommerceApplication.loadDotEnvIfPresent();

        // Kiểm tra xem GEMINI_API_KEY hoặc GEMINI_BASE_URL đã được đưa vào System Property chưa
        String baseUrl = System.getProperty("GEMINI_BASE_URL");
        assertThat(baseUrl).isNotNull();
        assertThat(baseUrl).contains("generativelanguage.googleapis.com");

        String apiKey = System.getProperty("GEMINI_API_KEY");
        assertThat(apiKey).isNotNull();
        assertThat(apiKey).isNotBlank();
    }
}

