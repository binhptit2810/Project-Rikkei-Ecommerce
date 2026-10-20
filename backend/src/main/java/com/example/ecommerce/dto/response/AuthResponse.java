package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body cho POST /api/v1/auth/register và POST /api/v1/auth/login.
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
}
