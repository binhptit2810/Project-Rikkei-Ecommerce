package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho PUT /api/v1/users/me/password.
 *
 * Nguyên tắc bảo mật:
 *   - Bắt buộc xác minh mật khẩu hiện tại trước khi đổi.
 *   - Mật khẩu mới phải từ 6 đến 100 ký tự.
 *   - Xác nhận mật khẩu mới phải khớp với mật khẩu mới.
 *   - Không log mật khẩu ra ngoài log server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
    private String confirmPassword;
}
