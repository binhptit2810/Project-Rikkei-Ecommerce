package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho API POST /api/v1/auth/auth0/exchange.
 * Tiếp nhận Auth0 ID Token gửi từ React SPA sau khi người dùng đăng nhập bằng Google.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auth0ExchangeRequest {

    @NotBlank(message = "Mã xác thực Auth0 ID Token không được để trống")
    private String idToken;
}

