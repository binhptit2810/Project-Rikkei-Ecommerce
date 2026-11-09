package com.example.ecommerce.exception;

/**
 * Ngoại lệ xung đột phiên bản dữ liệu phiên chat (Optimistic Concurrency Control).
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 * Sẽ được GlobalExceptionHandler ánh xạ thành HTTP 409 Conflict trong Phase sau.
 */
public class ChatVersionConflictException extends RuntimeException {

    public ChatVersionConflictException(String message) {
        super(message);
    }

    public ChatVersionConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

