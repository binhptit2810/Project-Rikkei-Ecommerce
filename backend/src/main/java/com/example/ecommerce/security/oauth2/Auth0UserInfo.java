package com.example.ecommerce.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Dữ liệu người dùng đã được trích xuất và xác thực an toàn từ Auth0 ID Token.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Auth0UserInfo {

    /** Định danh duy nhất Subject (sub) từ Auth0, ví dụ: "google-oauth2|109283019283". */
    private String sub;

    /** Địa chỉ email chuẩn hóa đã được xác thực từ Google qua Auth0. */
    private String email;

    /** Cờ xác nhận email đã được verify bởi nhà cung cấp danh tính. */
    private boolean emailVerified;

    /** Họ tên người dùng từ profile Google. */
    private String name;

    /** Đường dẫn ảnh đại diện avatar từ Google. */
    private String picture;
}

