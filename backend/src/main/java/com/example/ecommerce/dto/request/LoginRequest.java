package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body cho POST /api/v1/auth/login.
 *
 * Lưu ý bảo mật:
 *   - Không ghi password vào log.
 *   - Response không tiết lộ email có tồn tại hay không khi thông tin sai.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
