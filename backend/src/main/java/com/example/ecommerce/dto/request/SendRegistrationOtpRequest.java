package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu gửi mã OTP xác thực email trước khi tạo tài khoản.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendRegistrationOtpRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không vượt quá 100 ký tự")
    private String email;

    private String fullName;
}
