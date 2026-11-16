package com.example.ecommerce.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho OAuth2ClientConfig.
 * Xác minh tính tùy chọn (Optional) của Google OAuth2 Client:
 * 1. Khi GOOGLE_CLIENT_ID để trống: Khởi tạo repository rỗng an toàn, không crash, không đăng ký google.
 * 2. Khi GOOGLE_CLIENT_ID có giá trị: Khởi tạo ClientRegistration của Google với đúng scope và redirect-uri.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
class OAuth2ClientConfigTest {

    @Test
    @DisplayName("TC-OAUTH-01: GOOGLE_CLIENT_ID rỗng -> Khởi tạo repository rỗng, findByRegistrationId('google') trả về null")
    void clientRegistrationRepository_EmptyClientId_ReturnsEmptyRepository() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();
        ReflectionTestUtils.setField(config, "googleClientId", "");
        ReflectionTestUtils.setField(config, "googleClientSecret", "");
        ReflectionTestUtils.setField(config, "redirectUri", "{baseUrl}/login/oauth2/code/{registrationId}");

        ClientRegistrationRepository repository = config.clientRegistrationRepository();

        assertNotNull(repository, "ClientRegistrationRepository không được null");
        assertNull(repository.findByRegistrationId("google"), "Khi client-id rỗng, registration 'google' không được đăng ký");
    }

    @Test
    @DisplayName("TC-OAUTH-02: GOOGLE_CLIENT_ID là null hoặc placeholder -> Không đăng ký Google Client")
    void clientRegistrationRepository_NullOrPlaceholder_ReturnsEmptyRepository() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();
        ReflectionTestUtils.setField(config, "googleClientId", "google-client-id-placeholder");
        ReflectionTestUtils.setField(config, "googleClientSecret", "google-client-secret-placeholder");
        ReflectionTestUtils.setField(config, "redirectUri", "{baseUrl}/login/oauth2/code/{registrationId}");

        ClientRegistrationRepository repository = config.clientRegistrationRepository();

        assertNotNull(repository);
        assertNull(repository.findByRegistrationId("google"));
    }

    @Test
    @DisplayName("TC-OAUTH-03: GOOGLE_CLIENT_ID hợp lệ -> Đăng ký Google ClientRegistration thành công")
    void clientRegistrationRepository_ValidClientId_RegistersGoogleClient() {
        OAuth2ClientConfig config = new OAuth2ClientConfig();
        ReflectionTestUtils.setField(config, "googleClientId", "test-google-client-id.apps.googleusercontent.com");
        ReflectionTestUtils.setField(config, "googleClientSecret", "test-google-client-secret");
        ReflectionTestUtils.setField(config, "redirectUri", "{baseUrl}/login/oauth2/code/{registrationId}");

        ClientRegistrationRepository repository = config.clientRegistrationRepository();

        assertNotNull(repository);
        ClientRegistration googleReg = repository.findByRegistrationId("google");
        assertNotNull(googleReg, "Registration 'google' phải tồn tại khi client-id hợp lệ");
        assertEquals("test-google-client-id.apps.googleusercontent.com", googleReg.getClientId());
        assertEquals("test-google-client-secret", googleReg.getClientSecret());
        assertTrue(googleReg.getScopes().contains("openid"));
        assertTrue(googleReg.getScopes().contains("profile"));
        assertTrue(googleReg.getScopes().contains("email"));
    }
}

