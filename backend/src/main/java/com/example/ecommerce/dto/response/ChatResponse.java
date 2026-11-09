package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload trả về câu trả lời của Chatbot tư vấn bán hàng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật:
 * 1. Không trả về guestToken trong response thông thường.
 * 2. Không để lộ thông tin xác thực hoặc chi tiết kết nối hạ tầng nội bộ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Mã định danh phiên chat hiện tại (UUID v4).
     */
    private String sessionId;

    /**
     * Nội dung câu trả lời tư vấn từ Chatbot AI.
     */
    private String message;

    /**
     * Phiên bản dữ liệu phiên sau khi ghi nhận tin nhắn (Optimistic Concurrency Control).
     */
    private Long version;
}

