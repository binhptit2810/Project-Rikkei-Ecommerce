package com.example.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Entry point của ứng dụng E-commerce Backend.
 */
@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(EcommerceApplication.class, args);
    }

    /**
     * Tự động nạp file .env từ thư mục gốc hoặc thư mục cha nếu biến môi trường chưa được thiết lập.
     * Giúp ứng dụng nhận GEMINI_API_KEY và các cấu hình khi chạy qua IntelliJ IDEA, Eclipse, hoặc dòng lệnh.
     */
    public static void loadDotEnvIfPresent() {
        loadDotEnvIfPresent(
                new File(".env"),
                new File("../.env")
        );
    }

    /**
     * Nạp file .env từ danh sách file ứng viên (hỗ trợ truyền file tạm trong Unit Test).
     *
     * @param candidates Mảng các file .env ứng viên cần kiểm tra
     */
    public static void loadDotEnvIfPresent(File... candidates) {
        if (candidates == null || candidates.length == 0) {
            return;
        }

        for (File envFile : candidates) {
            if (envFile != null && envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                            continue;
                        }

                        int equalsIndex = trimmed.indexOf('=');
                        String key = trimmed.substring(0, equalsIndex).trim();
                        String value = trimmed.substring(equalsIndex + 1).trim();

                        if (key.isEmpty()) {
                            continue;
                        }

                        // Loại bỏ dấu nháy kép hoặc đơn bao quanh nếu có
                        if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) ||
                            (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                            value = value.substring(1, value.length() - 1);
                        }

                        // Chỉ gán nếu chưa được set trong System Environment hoặc System Properties
                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                    // Sau khi tìm thấy và nạp file .env đầu tiên thì dừng
                    break;
                } catch (IOException e) {
                    System.err.println("Warning: Không thể đọc file .env: " + e.getMessage());
                }
            }
        }
    }
}
