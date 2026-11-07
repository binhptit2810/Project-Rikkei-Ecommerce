package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi sau khi đăng ký tài khoản thành công.
 * Yêu cầu người dùng thực hiện xác thực email bằng mã OTP trước khi đăng nhập.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String email;
    private String fullName;
    private Boolean emailVerified;
}

