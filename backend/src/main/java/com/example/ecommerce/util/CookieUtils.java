package com.example.ecommerce.util;

import org.springframework.http.ResponseCookie;

/**
 * Tiện ích tạo và quản lý ResponseCookie an toàn cho Refresh Token.
 *
 * Nguyên tắc bảo mật:
 * - HttpOnly: true (ngăn JavaScript độc hại đọc token qua XSS).
 * - Secure: true trong production (HTTPS), false trong dev local (HTTP).
 * - SameSite: Lax (chống tấn công CSRF qua cross-site request).
 * - Path: /api/v1/auth (chỉ gửi cookie đến các endpoint xác thực liên quan).
 */
public final class CookieUtils {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    private CookieUtils() {
        // Private constructor for utility class
    }

    /**
     * Tạo cookie Refresh Token hợp lệ với thời hạn chỉ định.
     *
     * @param token         chuỗi refresh token thô
     * @param maxAgeSeconds thời hạn cookie tính bằng giây (ví dụ: 604800 = 7 ngày)
     * @param secure        bật cờ Secure (chỉ HTTPS) hay không
     * @param sameSite      giá trị SameSite (ví dụ: "Lax")
     * @return ResponseCookie hoàn chỉnh
     */
    public static ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds, boolean secure, String sameSite) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite != null ? sameSite : "Lax")
                .build();
    }

    /**
     * Tạo cookie xóa Refresh Token trên trình duyệt (Max-Age = 0).
     *
     * @param secure   bật cờ Secure hay không
     * @param sameSite giá trị SameSite
     * @return ResponseCookie với maxAge = 0
     */
    public static ResponseCookie createDeleteRefreshTokenCookie(boolean secure, String sameSite) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(0)
                .sameSite(sameSite != null ? sameSite : "Lax")
                .build();
    }
}
