package com.example.ecommerce.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cấu trúc dữ liệu lưu trữ One-Time Ticket trong Redis.
 * Tuyệt đối không lưu Access Token hoặc Refresh Token tại đây.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TicketData {
    private Long userId;
    private String email;
    /** Hash SHA-256 của chuỗi bí mật lưu trong cookie oauth2_binding (Browser Binding). */
    private String bindingHash;
    private long createdAt;
}

