package com.example.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Response body cho POST /api/v1/auth/register và POST /api/v1/auth/login.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Frontend lưu accessToken vào localStorage hoặc memory store, sau đó gửi trong
 * header: Authorization: Bearer <accessToken>
 *
 * Ví dụ JSON response:
 * {
 *   "success": true,
 *   "message": "Đăng nhập thành công",
 *   "data": {
 *     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *     "tokenType": "Bearer",
 *     "expiresIn": 86400,
 *     "user": {
 *       "id": 1,
 *       "email": "customer1@example.com",
 *       "fullName": "Nguyễn Văn An",
 *       "role": "CUSTOMER"
 *     }
 *   }
 * }
 */
// [TSK-D02-CCT]
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT access token. */
    private String accessToken;

    /** Luôn là "Bearer". */
    private String tokenType;

    /** Thời hạn token tính bằng giây (expiresIn = jwt.expiration / 1000). */
    private long expiresIn;

    /** Thông tin người dùng đã lọc trường nhạy cảm. */
    private UserResponse user;

    /**
     * Refresh Token thô (chỉ dùng nội bộ để Controller đặt cookie HttpOnly).
     * @JsonIgnore đảm bảo giá trị này TUYỆT ĐỐI không bị serialize ra JSON response.
     * @ToString.Exclude đảm bảo token không bị lộ qua AuthResponse.toString() hoặc logging.
     */
    @JsonIgnore
    @ToString.Exclude
    private String refreshToken;
}
