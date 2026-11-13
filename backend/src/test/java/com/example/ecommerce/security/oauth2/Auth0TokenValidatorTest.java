package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho Auth0TokenValidator.
 * Kiểm tra các trường hợp bắt buộc:
 * 1. Token hợp lệ với đầy đủ claims.
 * 2. Token sai chữ ký / lỗi giải mã.
 * 3. Token hết hạn.
 * 4. Email chưa xác minh (email_verified == false).
 * 5. Thiếu claim sub hoặc email.
 * 6. Chuẩn hóa domain và xử lý cấu hình Auth0 bị thiếu.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
class Auth0TokenValidatorTest {

    private NimbusJwtDecoder mockDecoder;
    private Auth0TokenValidator validator;

    private static final String DOMAIN = "dev-rikkei-ecommerce.us.auth0.com";
    private static final String CLIENT_ID = "test-auth0-client-id-123456";

    @BeforeEach
    void setUp() {
        mockDecoder = mock(NimbusJwtDecoder.class);
        validator = new Auth0TokenValidator(DOMAIN, CLIENT_ID, mockDecoder);
    }

    @Test
    @DisplayName("TC-VAL-01: Token hợp lệ với đầy đủ claims -> Trả về Auth0UserInfo chính xác")
    void validateToken_ValidToken_ReturnsAuth0UserInfo() {
        String token = "valid.auth0.idtoken";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("https://" + DOMAIN + "/")
                .audience(List.of(CLIENT_ID))
                .subject("google-oauth2|109283019283")
                .claim("email", "Customer.Test@Example.Com")
                .claim("email_verified", true)
                .claim("name", "Nguyen Van An")
                .claim("picture", "https://lh3.googleusercontent.com/photo.jpg")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(mockDecoder.decode(token)).thenReturn(jwt);

        Auth0UserInfo info = validator.validateToken(token);

        assertNotNull(info);
        assertEquals("google-oauth2|109283019283", info.getSub());
        assertEquals("customer.test@example.com", info.getEmail(), "Email phải được lowercase và trim");
        assertTrue(info.isEmailVerified());
        assertEquals("Nguyen Van An", info.getName());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", info.getPicture());
    }

    @Test
    @DisplayName("TC-VAL-02: Token sai chữ ký hoặc giải mã lỗi -> Ném BusinessException")
    void validateToken_InvalidSignature_ThrowsBusinessException() {
        String token = "tampered.idtoken.signature";
        when(mockDecoder.decode(token)).thenThrow(new BadJwtException("Invalid JWT signature"));

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("TC-VAL-03: Token hết hạn -> Ném BusinessException")
    void validateToken_ExpiredToken_ThrowsBusinessException() {
        String token = "expired.idtoken";
        when(mockDecoder.decode(token)).thenThrow(new BadJwtException("Jwt expired at 2026-11-13T00:00:00Z"));

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("TC-VAL-04: Email chưa xác thực (email_verified=false) -> Ném BusinessException từ chối")
    void validateToken_EmailNotVerified_ThrowsBusinessException() {
        String token = "unverified.email.token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("https://" + DOMAIN + "/")
                .audience(List.of(CLIENT_ID))
                .subject("google-oauth2|999999")
                .claim("email", "unverified@example.com")
                .claim("email_verified", false)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(mockDecoder.decode(token)).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("chưa được xác thực"));
    }

    @Test
    @DisplayName("TC-VAL-05: Thiếu claim Subject (sub) -> Ném BusinessException")
    void validateToken_MissingSubject_ThrowsBusinessException() {
        String token = "missing.sub.token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("https://" + DOMAIN + "/")
                .audience(List.of(CLIENT_ID))
                .claim("email", "valid@example.com")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(mockDecoder.decode(token)).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("sub"));
    }

    @Test
    @DisplayName("TC-VAL-06: Thiếu claim Email -> Ném BusinessException")
    void validateToken_MissingEmail_ThrowsBusinessException() {
        String token = "missing.email.token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("https://" + DOMAIN + "/")
                .audience(List.of(CLIENT_ID))
                .subject("google-oauth2|123456")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(mockDecoder.decode(token)).thenReturn(jwt);

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validateToken(token));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("TC-VAL-07: Chuỗi ID Token rỗng hoặc null -> Ném BusinessException")
    void validateToken_BlankToken_ThrowsBusinessException() {
        assertThrows(BusinessException.class, () -> validator.validateToken("   "));
        assertThrows(BusinessException.class, () -> validator.validateToken(null));
    }

    @Test
    @DisplayName("TC-VAL-08: Thiếu cấu hình Auth0 domain hoặc clientId -> Ném BusinessException báo lỗi cấu hình")
    void validateToken_UnconfiguredAuth0_ThrowsBusinessException() {
        Auth0TokenValidator unconfiguredValidator = new Auth0TokenValidator("", "");
        BusinessException ex = assertThrows(BusinessException.class, () -> unconfiguredValidator.validateToken("any.token"));
        assertTrue(ex.getMessage().contains("chưa được cấu hình"));
    }

    @Test
    @DisplayName("TC-VAL-09: normalizeDomain loại bỏ chính xác https://, http:// và trailing slash")
    void normalizeDomain_FormatsProperly() {
        assertEquals("dev-test.us.auth0.com", Auth0TokenValidator.normalizeDomain("https://dev-test.us.auth0.com/"));
        assertEquals("dev-test.us.auth0.com", Auth0TokenValidator.normalizeDomain("http://dev-test.us.auth0.com"));
        assertEquals("dev-test.us.auth0.com", Auth0TokenValidator.normalizeDomain("  dev-test.us.auth0.com/  "));
        assertEquals("", Auth0TokenValidator.normalizeDomain(null));
        assertEquals("", Auth0TokenValidator.normalizeDomain("   "));
    }
}

