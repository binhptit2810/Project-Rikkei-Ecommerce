package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response thông tin người dùng — dùng cho mọi API trả user data.
 *
 * Bảo mật:
 *   - Không chứa trường password, passwordHash hoặc dữ liệu nội bộ.
 *   - Sử dụng fromEntity() để convert an toàn từ User entity.
 *
 * Được sử dụng bởi:
 *   - AuthResponse  → trường "user" trong response đăng ký/đăng nhập
 *   - UserController → GET /api/v1/users/me, PUT /api/v1/users/me
 *
 * Các module có thể tham chiếu:
 *   - OrderService cần userId → dùng user.getId()
 *   - Module khác cần role    → dùng user.getRole()
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private Boolean active;
    private String lockReason;
    private LocalDateTime lockedAt;
    private LocalDateTime createdAt;

    /**
     * Factory method an toàn: chuyển User entity thành UserResponse DTO.
     * Password KHÔNG được copy sang DTO.
     *
     * @param user User entity từ database
     * @return UserResponse không chứa thông tin nhạy cảm
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .active(user.getActive())
                .lockReason(user.getLockReason())
                .lockedAt(user.getLockedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
