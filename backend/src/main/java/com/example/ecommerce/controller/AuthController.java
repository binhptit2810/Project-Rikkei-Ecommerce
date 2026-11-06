package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ForgotPasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.util.CookieUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xác thực người dùng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Tất cả endpoint trong controller này là PUBLIC (không cần Bearer token).
 * Cấu hình mở trong SecurityConfig:
 *   .requestMatchers(POST, "/api/v1/auth/register").permitAll()
 *   .requestMatchers(POST, "/api/v1/auth/login").permitAll()
 *   .requestMatchers(POST, "/api/v1/auth/refresh").permitAll()
 *   .requestMatchers(POST, "/api/v1/auth/logout").permitAll()
 *
 * Cơ chế bảo mật:
 *   - Access Token: trả trong response body JSON (Zustand memory store).
 *   - Refresh Token: truyền qua HttpOnly cookie (không trả trong JSON).
 */
// [TSK-D02-CCT]
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Đăng ký tài khoản mới.
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        HttpHeaders headers = buildCookieHeaders(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(headers)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    /**
     * Đăng nhập bằng email và mật khẩu.
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        HttpHeaders headers = buildCookieHeaders(response.getRefreshToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("Đăng nhập thành công", response));
    }

    /**
     * Làm mới Access Token thông qua Refresh Token cookie.
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = CookieUtils.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không tồn tại trong cookie");
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        HttpHeaders headers = buildCookieHeaders(response.getRefreshToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("Làm mới token thành công", response));
    }

    /**
     * Đăng xuất: thu hồi phiên trong Redis và xóa cookie trên trình duyệt.
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = CookieUtils.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            authService.logout(refreshToken);
        }

        HttpHeaders headers = buildDeleteCookieHeaders();

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("Đăng xuất thành công", null));
    }

    /**
     * Yêu cầu đặt lại mật khẩu và gửi OTP qua Email (TSK-D10-CCT).
     * POST /api/v1/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Nếu email tồn tại trong hệ thống và hợp lệ, bạn sẽ nhận được mã OTP qua email.", null)
        );
    }

    /**
     * Xác thực OTP và đặt lại mật khẩu mới (TSK-D10-CCT).
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại với mật khẩu mới.", null)
        );
    }

    // ---------- Helper methods ----------

    private HttpHeaders buildCookieHeaders(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            ResponseCookie cookie = CookieUtils.createRefreshTokenCookie(
                    refreshToken,
                    refreshExpirationMs / 1000,
                    cookieSecure,
                    cookieSameSite
            );
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return headers;
    }

    private HttpHeaders buildDeleteCookieHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ResponseCookie cookie = CookieUtils.createDeleteRefreshTokenCookie(cookieSecure, cookieSameSite);
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
}
