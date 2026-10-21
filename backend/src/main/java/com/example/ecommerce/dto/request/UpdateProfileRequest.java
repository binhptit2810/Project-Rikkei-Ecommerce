package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body cho PUT /api/v1/users/me.
 *
 * Hỗ trợ cập nhật từng phần (partial update):
 *   - null nghĩa là không thay đổi trường đó.
 *   - Chuỗi rỗng nghĩa là xóa giá trị (cho phone, address).
 *
 * Những trường KHÔNG được phép cập nhật qua API này:
 *   - email    (thay đổi email cần flow xác thực riêng)
 *   - password (dùng PUT /api/v1/users/me/password)
 *   - role     (chỉ Admin mới được thay đổi)
 *   - active   (chỉ Admin mới được thay đổi)
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Họ tên không vượt quá 100 ký tự")
    private String fullName;

    /** Cho phép truyền null (giữ nguyên) hoặc chuỗi rỗng (xóa phone). */
    @Pattern(regexp = "^(|0\\d{9})$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    private String address;
}
