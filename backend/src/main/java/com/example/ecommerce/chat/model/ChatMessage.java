package com.example.ecommerce.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Đại diện cho một tin nhắn trong hội thoại tư vấn bán hàng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * Vai trò của người gửi: "system", "user", hoặc "assistant".
     */
    private String role;

    /**
     * Nội dung tin nhắn.
     */
    private String content;

    /**
     * Thời điểm tạo tin nhắn.
     */
    private LocalDateTime timestamp;

    public static ChatMessage of(String role, String content) {
        return ChatMessage.builder()
                .role(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ChatMessage ofUser(String content) {
        return of("user", content);
    }

    public static ChatMessage ofAssistant(String content) {
        return of("assistant", content);
    }

    public static ChatMessage ofSystem(String content) {
        return of("system", content);
    }
}

