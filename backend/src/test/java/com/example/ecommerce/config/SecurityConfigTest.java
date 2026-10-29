package com.example.ecommerce.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test xác minh cấu hình SecurityConfig và PasswordEncoder BCrypt cost factor 12.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null, null, null);

    @Test
    @DisplayName("NFR-SEC-BCRYPT: Xác minh PasswordEncoder sử dụng BCrypt với cost factor bằng 12 ($2a$12$)")
    void passwordEncoder_UsesBcryptWithCost12() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

        String rawPassword = "SecurePassword@2026";
        String encoded = encoder.encode(rawPassword);

        // Khẳng định hash BCrypt có tiền tố cost factor 12
        assertThat(encoded).startsWith("$2a$12$");

        // Khẳng định so sánh mật khẩu chính xác
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("WrongPassword@2026", encoded)).isFalse();
    }
}
