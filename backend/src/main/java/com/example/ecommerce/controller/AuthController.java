package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xác thực người dùng.
 *
 * Tất cả endpoint trong controller này là PUBLIC (không cần token).
 * Cấu hình mở trong SecurityConfig:
 *   .requestMatchers(POST, "/api/v1/auth/register").permitAll()
 *   .requestMatchers(POST, "/api/v1/auth/login").permitAll()
 *
 * Được sử dụng bởi:
 *   - Frontend: trang đăng ký (/register) và đăng nhập (/login)
 *   - Đỗ Hoàng Anh: tích hợp Auth vào frontend, lưu token vào localStorage/Zustand
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới.
     *
     * POST /api/v1/auth/register
     *
     * Request body:
     *   {
     *     "fullName": "Nguyễn Văn A",
     *     "email": "user@example.com",
     *     "password": "Password123",
     *     "phone": "0912345678",    // tùy chọn
     *     "address": "123 Đường ABC" // tùy chọn
     *   }
     *
     * Response 201 Created:
     *   {
     *     "success": true,
     *     "message": "Đăng ký tài khoản thành công",
     *     "data": { "accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400, "user": {...} }
     *   }
     *
     * Response 400: validation lỗi hoặc email đã tồn tại
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    /**
     * Đăng nhập bằng email và mật khẩu.
     *
     * POST /api/v1/auth/login
     *
     * Request body:
     *   {
     *     "email": "user@example.com",
     *     "password": "Password123"
     *   }
     *
     * Response 200 OK:
     *   {
     *     "success": true,
     *     "message": "Đăng nhập thành công",
     *     "data": { "accessToken": "...", "tokenType": "Bearer", "expiresIn": 86400, "user": {...} }
     *   }
     *
     * Response 400: validation lỗi (email rỗng, format sai)
     * Response 401: sai email hoặc mật khẩu
     * Response 403: tài khoản bị vô hiệu hóa
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }
}
