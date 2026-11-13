package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho API POST /api/v1/auth/oauth2/exchange.
 * Trao đổi One-Time Ticket lấy JWT Access Token và Refresh Token.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2TicketExchangeRequest {

    @NotBlank(message = "Mã xác thực ticket không được để trống")
    private String ticket;
}

