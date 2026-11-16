package com.example.ecommerce.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Kho lưu trữ OAuth2AuthorizationRequest phân tán và phi trạng thái (Stateless) dựa trên Redis.
 * Thay thế HttpSessionOAuth2AuthorizationRequestRepository để bảo đảm kiến trúc Stateless 100%,
 * hỗ trợ đa node / container Docker và chống tấn công CSRF thông qua Double-Submit Cookie & State Verification.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    public static final String OAUTH2_STATE_COOKIE_NAME = "oauth2_auth_state";
    private static final String REDIS_PREFIX = "auth:oauth2_req:";
    private static final long EXPIRATION_SECONDS = 180L; // 3 phút TTL

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = getStateFromRequest(request);
        if (!StringUtils.hasText(state)) {
            return null;
        }

        // Chống tấn công CSRF: Đối chiếu tham số state từ URL với cookie state trong trình duyệt
        String cookieState = getCookieValue(request, OAUTH2_STATE_COOKIE_NAME);
        if (StringUtils.hasText(cookieState) && !cookieState.equals(state)) {
            log.warn("CẢNH BÁO BẢO MẬT: Phát hiện sai lệch OAuth2 state parameter ({}) và cookie state ({})", state, cookieState);
            return null;
        }

        String redisKey = REDIS_PREFIX + state;
        String serialized = redisTemplate.opsForValue().get(redisKey);
        if (serialized == null) {
            log.debug("Không tìm thấy OAuth2AuthorizationRequest trong Redis hoặc đã hết hạn: state={}", state);
            return null;
        }

        return deserialize(serialized);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        if (!StringUtils.hasText(state)) {
            log.warn("OAuth2AuthorizationRequest không có state hợp lệ");
            return;
        }

        String serialized = serialize(authorizationRequest);
        String redisKey = REDIS_PREFIX + state;

        redisTemplate.opsForValue().set(redisKey, serialized, EXPIRATION_SECONDS, TimeUnit.SECONDS);

        // Đặt cookie HttpOnly chứa state để bảo vệ chống giả mạo request cross-browser
        ResponseCookie cookie = ResponseCookie.from(OAUTH2_STATE_COOKIE_NAME, state)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(EXPIRATION_SECONDS)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("Đã lưu OAuth2AuthorizationRequest vào Redis: state={}", state);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest originalRequest = loadAuthorizationRequest(request);
        String state = getStateFromRequest(request);

        if (StringUtils.hasText(state)) {
            redisTemplate.delete(REDIS_PREFIX + state);
        }

        // Xóa cookie state
        ResponseCookie clearCookie = ResponseCookie.from(OAUTH2_STATE_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader("Set-Cookie", clearCookie.toString());

        return originalRequest;
    }

    private String getStateFromRequest(HttpServletRequest request) {
        String stateParam = request.getParameter("state");
        if (StringUtils.hasText(stateParam)) {
            return stateParam.trim();
        }
        return getCookieValue(request, OAUTH2_STATE_COOKIE_NAME);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> name.equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(request);
            oos.flush();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("Lỗi khi tuần tự hóa OAuth2AuthorizationRequest: {}", e.getMessage());
            throw new IllegalArgumentException("Không thể tuần tự hóa OAuth2AuthorizationRequest", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String base64) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            log.error("Lỗi khi giải tuần tự hóa OAuth2AuthorizationRequest: {}", e.getMessage());
            return null;
        }
    }
}

