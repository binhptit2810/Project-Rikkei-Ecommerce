package com.example.ecommerce.exception;

import lombok.Getter;

/**
 * Ngoại lệ khi yêu cầu gửi tin nhắn chatbot vượt quá giới hạn tần suất (Rate Limiting).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Sẽ được GlobalExceptionHandler bắt và chuyển đổi thành HTTP 429 Too Many Requests
 * kèm header Retry-After tương ứng với số giây còn lại.
 */
@Getter
public class ChatRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public ChatRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}

