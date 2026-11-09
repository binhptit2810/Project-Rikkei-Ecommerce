package com.example.ecommerce.exception;

/**
 * Ngoại lệ xung đột Distributed Lock khi có request khác đang xử lý cùng session.
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 * Sẽ được GlobalExceptionHandler ánh xạ thành HTTP 429 Too Many Requests trong Phase sau.
 */
public class ChatLockContentionException extends RuntimeException {

    public ChatLockContentionException(String message) {
        super(message);
    }

    public ChatLockContentionException(String message, Throwable cause) {
        super(message, cause);
    }
}

