package com.example.ecommerce.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Test cho JwtUtils.
 * Kiểm tra các yêu cầu phi chức năng về Bảo mật JWT (NFR-SEC):
 *   - Tạo token có chữ ký HMAC-SHA256 hợp lệ.
 *   - Xác thực tính toàn vẹn và thời hạn của token.
 *   - Phát hiện và từ chối token hết hạn hoặc bị sửa đổi.
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "this-is-a-very-secure-test-secret-key-with-at-least-256-bits!";
    private final long expiration = 3600000L; // 1 giờ

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", expiration);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(List.of())
                .build();
    }

    @Test
    @DisplayName("NFR-SEC-15: Sinh token JWT hợp lệ với email làm subject")
    void generateToken_ValidUserDetails_GeneratesValidJwt() {
        String token = jwtUtils.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtils.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtUtils.getExpirationInSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("NFR-SEC-16: Xác thực token hợp lệ cho đúng người dùng")
    void validateToken_ValidTokenAndUser_ReturnsTrue() {
        String token = jwtUtils.generateToken(userDetails);

        boolean isValid = jwtUtils.validateToken(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("NFR-SEC-17: Từ chối token nếu subject không khớp với UserDetails")
    void validateToken_SubjectMismatch_ReturnsFalse() {
        String token = jwtUtils.generateToken(userDetails);

        UserDetails anotherUser = User.builder()
                .username("another@example.com")
                .password("password")
                .authorities(List.of())
                .build();

        boolean isValid = jwtUtils.validateToken(token, anotherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-18: Từ chối token bị sửa đổi hoặc sai chữ ký (Invalid Signature)")
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = jwtUtils.generateToken(userDetails);
        String tamperedToken = token + "corrupted";

        boolean isValid = jwtUtils.validateToken(tamperedToken, userDetails);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-19: Từ chối token đã hết hạn (Expired JWT)")
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Tạo token với thời hạn quá khứ
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 200000))
                .expiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(key)
                .compact();

        boolean isValid = jwtUtils.validateToken(expiredToken, userDetails);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-19B: Từ chối token có định dạng malformed (MalformedJwtException)")
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean isValid = jwtUtils.validateToken("not.a.valid.jwt.token", userDetails);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-19C: Từ chối token rỗng hoặc null (IllegalArgumentException)")
    void validateToken_EmptyOrNullToken_ReturnsFalse() {
        assertThat(jwtUtils.validateToken("", userDetails)).isFalse();
        assertThat(jwtUtils.validateToken(null, userDetails)).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-19D: Kiểm tra token còn hạn bằng isTokenExpired trả về false")
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = jwtUtils.generateToken(userDetails);
        assertThat(jwtUtils.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("NFR-SEC-19E: Kiểm tra token hết hạn bằng isTokenExpired ném ExpiredJwtException do JJWT xác thực hạn")
    void isTokenExpired_ExpiredToken_ThrowsExpiredJwtException() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 200000))
                .expiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtUtils.isTokenExpired(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}

