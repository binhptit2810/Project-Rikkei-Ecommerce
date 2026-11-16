package com.example.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Cấu hình đăng ký ClientRegistrationRepository cho Spring Security OAuth2.
 * Cho phép Google OAuth2 Client là tùy chọn (Optional):
 * - Nếu GOOGLE_CLIENT_ID được cấu hình và hợp lệ: khởi tạo Google ClientRegistration.
 * - Nếu GOOGLE_CLIENT_ID để trống: khởi tạo repository rỗng an toàn, không làm crash Backend,
 *   nhường quyền xác thực Google cho Auth0 Identity Provider.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
@Configuration
@Slf4j
public class OAuth2ClientConfig {

    @Value("${GOOGLE_CLIENT_ID:${app.oauth2.google.client-id:${spring.security.oauth2.client.registration.google.client-id:}}}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:${app.oauth2.google.client-secret:${spring.security.oauth2.client.registration.google.client-secret:}}}")
    private String googleClientSecret;

    @Value("${app.oauth2.google.redirect-uri:${spring.security.oauth2.client.registration.google.redirect-uri:{baseUrl}/login/oauth2/code/{registrationId}}}")
    private String redirectUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        if (StringUtils.hasText(googleClientId) && !googleClientId.contains("placeholder")) {
            log.info("Khởi tạo Google OAuth2 Client Registration với Client ID đã cấu hình");
            ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId.trim())
                    .clientSecret(StringUtils.hasText(googleClientSecret) ? googleClientSecret.trim() : "none")
                    .scope("openid", "profile", "email")
                    .redirectUri(redirectUri)
                    .build();
            return new InMemoryClientRegistrationRepository(Map.of("google", googleRegistration));
        }

        log.info("GOOGLE_CLIENT_ID để trống hoặc chưa cấu hình: Google OAuth2 trực tiếp cũ ở trạng thái vô hiệu hóa. Hệ thống sử dụng Auth0 Google Login.");
        return new InMemoryClientRegistrationRepository(Collections.emptyMap());
    }
}

