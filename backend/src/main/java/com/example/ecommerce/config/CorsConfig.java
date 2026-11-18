package com.example.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình CORS tập trung và bảo mật cho toàn bộ ứng dụng (Spring Security CorsFilter).
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D16-CCT] Rà Soát CORS Môi Trường Mạng & Security Audit
 *
 * Cho phép các origin:
 *   - http://localhost:3000 (React / Nginx local)
 *   - http://localhost:5173 (Vite dev server)
 *   - http://rikkei-shop.me (Demo domain)
 *
 * Tuyệt đối không sử dụng wildcard '*' khi allowCredentials = true.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://rikkei-shop.me,https://rikkei-shop.me,https://www.rikkei-shop.me}")
    private String allowedOrigins;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Bean CorsConfigurationSource duy nhất được Spring Security sử dụng (.cors(Customizer.withDefaults())).
     * Loại bỏ cấu hình addCorsMappings trùng lặp để tránh xung đột hoặc ghi đè cấu hình.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(origin -> !"*".equals(origin)) // Ngăn chặn kết hợp nguy hiểm '*' với credentials=true
                .toList();

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Guest-Session-Id",
                "Cache-Control"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/api/v1/uploads/**", "/uploads/**")
                .addResourceLocations(location);
    }
}
