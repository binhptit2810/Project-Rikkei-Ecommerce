package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service xác minh Auth0 ID Token bằng NimbusJwtDecoder.
 * Sử dụng JWKS endpoint từ Auth0 để tự động xác minh chữ ký RS256,
 * hỗ trợ JWKS Key Rotation, caching public keys và kiểm tra chặt chẽ
 * các claims: iss, aud, exp, nbf, sub, email, email_verified.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
@Service
@Slf4j
public class Auth0TokenValidator {

    private final String domain;
    private final String clientId;
    private volatile NimbusJwtDecoder jwtDecoder;

    @Autowired
    public Auth0TokenValidator(
            @Value("${app.auth0.domain:}") String domain,
            @Value("${app.auth0.client-id:}") String clientId) {
        this.domain = normalizeDomain(domain);
        this.clientId = StringUtils.hasText(clientId) ? clientId.trim() : "";
    }

    /**
     * Constructor phục vụ Unit Testing hoặc inject custom JwtDecoder.
     */
    public Auth0TokenValidator(String domain, String clientId, NimbusJwtDecoder customDecoder) {
        this.domain = normalizeDomain(domain);
        this.clientId = StringUtils.hasText(clientId) ? clientId.trim() : "";
        this.jwtDecoder = customDecoder;
    }

    /**
     * Chuẩn hóa Auth0 Domain loại bỏ protocol thừa và trailing slash.
     */
    public static String normalizeDomain(String rawDomain) {
        if (!StringUtils.hasText(rawDomain)) {
            return "";
        }
        String clean = rawDomain.trim();
        if (clean.startsWith("https://")) {
            clean = clean.substring(8);
        } else if (clean.startsWith("http://")) {
            clean = clean.substring(7);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /**
     * Khởi tạo hoặc tái sử dụng NimbusJwtDecoder với cấu hình bảo mật chuẩn OIDC.
     */
    protected NimbusJwtDecoder getOrBuildDecoder() {
        if (jwtDecoder != null) {
            return jwtDecoder;
        }
        synchronized (this) {
            if (jwtDecoder != null) {
                return jwtDecoder;
            }

            if (!StringUtils.hasText(domain) || !StringUtils.hasText(clientId)) {
                log.warn("Auth0 domain hoặc client-id chưa được cấu hình trong application.yml");
                throw new BusinessException("Dịch vụ xác thực Auth0 chưa được cấu hình trên hệ thống.");
            }

            String issuer = "https://" + domain + "/";
            String jwksUri = "https://" + domain + "/.well-known/jwks.json";

            log.info("Khởi tạo NimbusJwtDecoder cho Auth0: issuer={}, jwksUri={}", issuer, jwksUri);

            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();

            // 1. Validator mặc định: kiểm tra timestamp (exp, nbf) với 60s clock skew và issuer
            OAuth2TokenValidator<Jwt> defaultIssuerValidator = JwtValidators.createDefaultWithIssuer(issuer);

            // 2. Audience Validator: aud claim bắt buộc phải chứa Auth0 Client ID của ứng dụng
            OAuth2TokenValidator<Jwt> audienceValidator = token -> {
                List<String> audiences = token.getAudience();
                if (audiences != null && audiences.contains(clientId)) {
                    return OAuth2TokenValidatorResult.success();
                }
                log.warn("Auth0 ID Token bị từ chối do Audience không khớp: expected={}, actual={}", clientId, audiences);
                OAuth2Error error = new OAuth2Error("invalid_audience", "Audience không khớp với Client ID ứng dụng", null);
                return OAuth2TokenValidatorResult.failure(error);
            };

            OAuth2TokenValidator<Jwt> combinedValidator =
                    new DelegatingOAuth2TokenValidator<>(defaultIssuerValidator, audienceValidator);
            decoder.setJwtValidator(combinedValidator);

            this.jwtDecoder = decoder;
            return this.jwtDecoder;
        }
    }

    /**
     * Xác thực Auth0 ID Token và trích xuất dữ liệu người dùng.
     *
     * @param idToken chuỗi JWT ID Token nhận từ frontend
     * @return Auth0UserInfo thông tin người dùng đã được thẩm định
     */
    public Auth0UserInfo validateToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BusinessException("Mã xác thực Auth0 ID Token không được để trống.");
        }

        NimbusJwtDecoder decoder = getOrBuildDecoder();
        Jwt jwt;

        try {
            jwt = decoder.decode(idToken.trim());
        } catch (JwtException e) {
            log.warn("Xác thực chữ ký/thời hạn Auth0 ID Token thất bại: {}", e.getMessage());
            throw new BusinessException("Mã xác thực Auth0 ID Token không hợp lệ hoặc đã hết hạn.");
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi giải mã Auth0 ID Token: {}", e.getMessage());
            throw new BusinessException("Không thể xác thực danh tính từ Auth0. Vui lòng thử lại.");
        }

        // Kiểm tra claim Subject (sub)
        String sub = jwt.getSubject();
        if (!StringUtils.hasText(sub)) {
            log.error("Auth0 ID Token không chứa Subject (sub)");
            throw new BusinessException("Auth0 ID Token không chứa định danh người dùng (sub).");
        }

        // Kiểm tra claim Email
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email)) {
            log.error("Auth0 ID Token không cung cấp địa chỉ email cho sub={}", sub);
            throw new BusinessException("Tài khoản Google/Auth0 không cung cấp địa chỉ email.");
        }

        // Bắt buộc kiểm tra email_verified == true
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            log.warn("Từ chối Auth0 token có email chưa xác thực: email={}, sub={}", email, sub);
            throw new BusinessException("Địa chỉ email từ Google/Auth0 chưa được xác thực. Vui lòng xác thực email của bạn trước khi đăng nhập.");
        }

        String name = jwt.getClaimAsString("name");
        String picture = jwt.getClaimAsString("picture");

        return Auth0UserInfo.builder()
                .sub(sub.trim())
                .email(email.toLowerCase().trim())
                .emailVerified(true)
                .name(StringUtils.hasText(name) ? name.trim() : null)
                .picture(StringUtils.hasText(picture) ? picture.trim() : null)
                .build();
    }

    /**
     * Setter phục vụ testing.
     */
    public void setJwtDecoder(NimbusJwtDecoder decoder) {
        this.jwtDecoder = decoder;
    }
}

