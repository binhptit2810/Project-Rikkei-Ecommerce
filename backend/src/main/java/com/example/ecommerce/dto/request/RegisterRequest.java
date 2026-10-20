package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body cho POST /api/v1/auth/register.
 *
 * Validation rules:
 *   fullName — bắt buộc, tối đa 100 ký tự
 *   email    — bắt buộc, đúng định dạng email, tối đa 100 ký tự
 *   password — bắt buộc, 6–100 ký tự
 *   phone    — tuỳ chọn, chuỗi rỗng hoặc số bắt đầu bằng 0 gồm 10 chữ số
 *   address  — tuỳ chọn, không giới hạn độ dài
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
    private String password;

    /** Số điện thoại Việt Nam: bắt đầu bằng 0, tổng cộng 10 chữ số. Cho phép để trống. */
    @Pattern(regexp = "^(|0\\d{9})$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    private String address;
}
