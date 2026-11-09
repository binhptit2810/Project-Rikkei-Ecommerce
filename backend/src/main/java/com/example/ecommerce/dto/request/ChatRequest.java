package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload gửi tin nhắn trò chuyện đến Chatbot tư vấn bán hàng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật:
 * 1. Không nhận ownerId hoặc userId từ client để chống giả mạo danh tính (IDOR).
 * 2. sessionId có thể để trống (null) ở lượt chat đầu tiên của khách vãng lai (Guest).
 *    Nếu có truyền, sessionId bắt buộc phải đúng định dạng chuẩn UUID v4.
 * 3. Nội dung tin nhắn bị giới hạn tối đa 1000 ký tự để chống DoS và lạm dụng tài nguyên LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * Mã định danh phiên chat (UUID v4).
     * Để null nếu bắt đầu phiên trò chuyện mới.
     */
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "Session ID không đúng định dạng UUID"
    )
    private String sessionId;

    /**
     * Nội dung câu hỏi của khách hàng.
     */
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 1000, message = "Nội dung tin nhắn không được vượt quá 1000 ký tự")
    private String message;
}

