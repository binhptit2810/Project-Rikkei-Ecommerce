package com.example.ecommerce.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Metadata quản lý phiên trò chuyện của khách hoặc người dùng đã xác thực.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật & tính toàn vẹn:
 * 1. ownerId được lưu trữ và xử lý dưới dạng String ("GUEST" hoặc String.valueOf(userId)).
 * 2. version kiểu Long tăng dần phục vụ Optimistic Concurrency Control (OCC) trong Redis Lua.
 * 3. Tuyệt đối không chứa raw guest token, API key hoặc thông tin xác thực nhạy cảm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionMetadata {

    /**
     * Mã định danh phiên chat (UUID v4).
     */
    private String sessionId;

    /**
     * Mã định danh chủ sở hữu phiên ("GUEST" hoặc String userId).
     */
    private String ownerId;

    /**
     * Phiên bản dữ liệu phục vụ Optimistic Concurrency Control.
     */
    private Long version;

    /**
     * Thời điểm tạo phiên.
     */
    private LocalDateTime createdAt;

    /**
     * Thời điểm cập nhật tin nhắn cuối cùng.
     */
    private LocalDateTime updatedAt;

    /**
     * Thời điểm phiên hết hạn (TTL 24h).
     */
    private LocalDateTime expiresAt;
}

