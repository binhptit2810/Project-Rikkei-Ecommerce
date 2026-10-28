package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body cho POST /api/v1/auth/login.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Lưu ý bảo mật:
 *   - Không ghi password vào log.
 *   - Response không tiết lộ email có tồn tại hay không khi thông tin sai.
 */
// [TSK-D02-CCT]
@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    /**
     * Cổng đăng nhập yêu cầu: "CUSTOMER" hoặc "ADMIN" (tùy chọn).
     * Dùng để Backend kiểm soát và từ chối tài khoản sai cổng ngay tại tầng xác thực.
     */
    private String portal;
}

