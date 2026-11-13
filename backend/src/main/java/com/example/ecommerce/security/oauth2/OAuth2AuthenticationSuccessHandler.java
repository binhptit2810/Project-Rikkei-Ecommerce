package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Xử lý sau khi người dùng xác thực thành công danh tính với Google.
 * Sinh One-Time Exchange Ticket kết hợp Browser Binding Cookie (chống đánh cắp ticket qua Referrer/History)
 * và chuyển hướng về Frontend.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.oauth2.frontend-success-url:http://localhost:3000/oauth2/redirect}")
    private String frontendSuccessUrl;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    public static final String BINDING_COOKIE_NAME = "oauth2_binding";
    public static final String BINDING_COOKIE_PATH = "/api/v1/auth/oauth2";
    public static final String TICKET_REDIS_PREFIX = "auth:oauth2_ticket:";
    private static final long TICKET_TTL_SECONDS = 60L; // 60 giây

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof CustomOidcUser customOidcUser)) {
            log.error("Principal không phải kiểu CustomOidcUser: {}", authentication.getPrincipal().getClass());
            response.sendRedirect(frontendSuccessUrl + "?error=" + "invalid_principal");
            return;
        }

        User user = customOidcUser.getUserEntity();

        // 1. Sinh One-Time Ticket có độ entropy cao (32 bytes ngẫu nhiên)
        String rawTicket = generateRandomToken();
        String ticketHash = hashString(rawTicket);

        // 2. Sinh Browser Binding Secret (gắn vé với phiên trình duyệt hiện tại)
        String bindingSecret = generateRandomToken();
        String bindingHash = hashString(bindingSecret);

        // 3. Đóng gói dữ liệu ticket và lưu vào Redis với TTL ngắn (60s)
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();

        try {
            String json = objectMapper.writeValueAsString(ticketData);
            String redisKey = TICKET_REDIS_PREFIX + ticketHash;
            redisTemplate.opsForValue().set(redisKey, json, TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Lỗi khi serialize hoặc lưu OAuth2TicketData vào Redis: {}", e.getMessage());
            response.sendRedirect(frontendSuccessUrl + "?error=" + "ticket_generation_failed");
            return;
        }

        // 4. Thiết lập HttpOnly Cookie ràng buộc trình duyệt (Path giới hạn tại endpoint exchange)
        ResponseCookie bindingCookie = ResponseCookie.from(BINDING_COOKIE_NAME, bindingSecret)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(BINDING_COOKIE_PATH)
                .maxAge(120)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader("Set-Cookie", bindingCookie.toString());

        // 5. Dọn dẹp cookie lưu trữ OAuth2 authorization request
        authorizationRequestRepository.removeAuthorizationRequest(request, response);

        // 6. Xóa cờ xác thực session tạm thời (nếu có)
        clearAuthenticationAttributes(request);

        // 7. Chuyển hướng an toàn về Frontend kèm query param ticket duy nhất
        String targetUrl = UriComponentsBuilder.fromUriString(frontendSuccessUrl)
                .queryParam("ticket", rawTicket)
                .build().toUriString();

        log.info("Chuyển hướng người dùng về Frontend sau khi xác thực Google thành công: userId={}", user.getId());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm không khả dụng", e);
        }
    }
}

