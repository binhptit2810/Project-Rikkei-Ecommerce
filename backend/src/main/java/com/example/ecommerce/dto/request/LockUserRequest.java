package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho API Admin khóa tài khoản:
 * PATCH /api/v1/admin/users/{id}/lock
 *
 * Yêu cầu:
 * - Lý do khóa là bắt buộc (không được rỗng hoặc chỉ chứa khoảng trắng).
 * - Giới hạn tối đa 500 ký tự phù hợp với cột `lock_reason` trong cơ sở dữ liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockUserRequest {

    @NotBlank(message = "Lý do khóa tài khoản không được để trống")
    @Size(max = 500, message = "Lý do khóa tài khoản không được vượt quá 500 ký tự")
    private String reason;
}
