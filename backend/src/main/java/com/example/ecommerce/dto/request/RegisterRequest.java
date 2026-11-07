package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body cho POST /api/v1/auth/register.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Validation rules:
 *   fullName — bắt buộc, tối đa 100 ký tự
 *   email    — bắt buộc, đúng định dạng email, tối đa 100 ký tự
 *   password — bắt buộc, 6–100 ký tự
 *   phone    — bắt buộc, 10 chữ số bắt đầu bằng 0 (VD: 0912345678)
 *   address  — tuỳ chọn, không giới hạn độ dài
 */
// [TSK-D02-CCT]
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

    /** Số điện thoại Việt Nam: bắt buộc, bắt đầu bằng 0, tổng cộng 10 chữ số. */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)")
    private String phone;

    private String address;

    /** Mã OTP xác thực email (6 chữ số). */
    private String otp;
}
