package com.example.ecommerce.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO nội bộ biểu diễn dữ liệu hội thoại và metadata đọc từ Redis.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryData {

    /**
     * Metadata của phiên hội thoại (version, owner, timestamps).
     */
    private ChatSessionMetadata metadata;

    /**
     * Danh sách các tin nhắn trong bộ nhớ hội thoại (tối đa 20 tin nhắn, 10 cặp).
     */
    private List<ChatMessage> messages;
}

