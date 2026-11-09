package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload chuyển quyền sở hữu phiên chat từ Guest sang User đã đăng nhập.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật:
 * 1. guestToken là token bảo mật được cấp khi tạo phiên Guest, dùng để chứng minh quyền sở hữu phiên.
 * 2. Không nhận userId từ client; userId bắt buộc được trích xuất an toàn từ SecurityContext (JWT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSessionRequest {

    /**
     * Mã định danh phiên chat của khách (UUID v4).
     */
    @NotBlank(message = "Session ID không được để trống")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "Session ID không đúng định dạng UUID"
    )
    private String sessionId;

    /**
     * Mã token chứng minh quyền sở hữu phiên của khách vãng lai.
     */
    @NotBlank(message = "Guest token không được để trống")
    private String guestToken;
}

