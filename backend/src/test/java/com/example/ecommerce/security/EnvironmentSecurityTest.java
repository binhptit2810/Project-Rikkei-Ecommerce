package com.example.ecommerce.security;

import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.EmailService;
import com.example.ecommerce.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bộ kiểm thử an toàn môi trường, cấu hình bảo mật và kiểm soát rò rỉ bí mật.
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D13-CCT]
 * 
 * Kiểm tra các kịch bản: SEC-13-14 đến SEC-13-17.
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentSecurityTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private EmailService emailService;

    // =========================================================================
    // SEC-13-14: Kiểm tra .env bắt buộc nằm trong .gitignore
    // =========================================================================
    @Test
    @DisplayName("SEC-13-14: Xác nhận file .env và *.env đều được liệt kê trong .gitignore để chống commit secret")
    void sec13_14_verifyDotEnvIsIgnoredInGit() throws IOException {
        Path rootGitignore = Paths.get("..", ".gitignore");
        if (!Files.exists(rootGitignore)) {
            rootGitignore = Paths.get(".gitignore");
        }
        assertThat(Files.exists(rootGitignore)).isTrue();

        List<String> lines = Files.readAllLines(rootGitignore);
        boolean containsDotEnv = lines.stream().anyMatch(line -> line.trim().equals(".env") || line.trim().equals("*.env"));
        assertThat(containsDotEnv)
                .as("Root .gitignore bắt buộc phải chứa .env để ngăn chặn commit thông tin bí mật lên kho mã nguồn")
                .isTrue();
    }

    // =========================================================================
    // SEC-13-15: Xác nhận .env.example và docker-compose không chứa secret thật
    // =========================================================================
    @Test
    @DisplayName("SEC-13-15: File mẫu .env.example và docker-compose không chứa secret thật hoặc email cá nhân")
    void sec13_15_verifyDotEnvExampleContainsOnlyPlaceholders() throws IOException {
        Path envExamplePath = Paths.get("..", ".env.example");
        if (!Files.exists(envExamplePath)) {
            envExamplePath = Paths.get(".env.example");
        }
        assertThat(Files.exists(envExamplePath)).isTrue();

        String envExampleContent = Files.readString(envExamplePath);
        // Không chứa tài khoản Brevo thực tế đã được phát hiện trước đó
        assertThat(envExampleContent).doesNotContain("af39f5001@smtp-brevo.com");
        // OTP HMAC Secret phải là placeholder
        assertThat(envExampleContent).doesNotContain("OTP_HMAC_SECRET=k9X2mP5vL8qR3wY6zF1nH4bC7jD0sT2x");

        // Kiểm tra docker-compose.yml không cung cấp fallback bí mật nguy hiểm
        Path dockerComposePath = Paths.get("..", "docker-compose.yml");
        if (!Files.exists(dockerComposePath)) {
            dockerComposePath = Paths.get("docker-compose.yml");
        }
        if (Files.exists(dockerComposePath)) {
            String dockerComposeContent = Files.readString(dockerComposePath);
            assertThat(dockerComposeContent).doesNotContain("k9X2mP5vL8qR3wY6zF1nH4bC7jD0sT2x");
            assertThat(dockerComposeContent).doesNotContain("af39f5001@smtp-brevo.com");
        }
    }

    // =========================================================================
    // SEC-13-16: Kiểm tra bảo vệ Logging thông tin nhạy cảm
    // =========================================================================
    @Test
    @DisplayName("SEC-13-16: Xác nhận mã nguồn các Service nhạy cảm không in raw password, full secret hay credit token ra log")
    void sec13_16_verifySensitiveBusinessLoggingPatterns() throws IOException {
        Path authServicePath = Paths.get("src", "main", "java", "com", "example", "ecommerce", "service", "AuthService.java");
        if (!Files.exists(authServicePath)) {
            authServicePath = Paths.get("backend", "src", "main", "java", "com", "example", "ecommerce", "service", "AuthService.java");
        }
        assertThat(Files.exists(authServicePath)).isTrue();

        String authSource = Files.readString(authServicePath);
        // Không log mật khẩu người dùng
        assertThat(authSource).doesNotContain("log.info(\"User password: {}\"");
        assertThat(authSource).doesNotContain("log.debug(\"Password: {}\"");
        // Không in raw token hoặc secret ra INFO log
        assertThat(authSource).doesNotContain("log.info(\"JWT Secret: {}\"");
    }

    // =========================================================================
    // SEC-13-17: Xác nhận cấu hình Production fail-fast nếu thiếu biến môi trường
    // =========================================================================
    @Test
    @DisplayName("SEC-13-17: VNPayConfig và AuthService fail-fast trên môi trường Production khi thiếu secret bắt buộc")
    void sec13_17_verifyProductionConfigFailsFastOnMissingSecrets() {
        // 1. Kiểm tra VNPayConfig trên Production
        VNPayConfig vnPayConfig = new VNPayConfig();
        vnPayConfig.setActiveProfile("prod");
        vnPayConfig.setTmnCode(null);
        vnPayConfig.setHashSecret(null);

        assertThatThrownBy(vnPayConfig::validateVNPayConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL SECURITY ERROR: Biến môi trường VNPAY_TMN_CODE và VNPAY_HASH_SECRET bắt buộc");

        // 2. VNPayConfig trên Dev không ném lỗi
        vnPayConfig.setActiveProfile("dev");
        vnPayConfig.validateVNPayConfig(); // Không ném exception

        // 3. Kiểm tra AuthService trên Production khi thiếu OTP_HMAC_SECRET
        AuthService authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils,
                refreshTokenService,
                redisTemplate,
                emailService
        );

        ReflectionTestUtils.setField(authService, "activeProfile", "prod");
        ReflectionTestUtils.setField(authService, "otpHmacSecret", "");

        assertThatThrownBy(authService::validateOtpHmacSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL SECURITY ERROR: Biến môi trường OTP_HMAC_SECRET bắt buộc");

        // 4. Kiểm tra AuthService trên Production nếu dùng default dev secret
        ReflectionTestUtils.setField(authService, "otpHmacSecret", "ecommerce-default-otp-hmac-secret-change-in-production-2024");
        assertThatThrownBy(authService::validateOtpHmacSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không được phép sử dụng secret mặc định");
    }
}

