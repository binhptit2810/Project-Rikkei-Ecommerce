package com.example.ecommerce.exception;

/**
 * Ngoại lệ xác thực trong phiên hội thoại Chatbot (thiếu hoặc sai Guest Token, hoặc lỗi xác thực phiên chat).
 * Ánh xạ sang HTTP 401 Unauthorized với thông điệp rõ ràng, tách biệt với BadCredentialsException của Auth.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Sửa lỗi xác thực Guest Token cho Chatbot.
 */
public class ChatAuthenticationException extends RuntimeException {

    public ChatAuthenticationException(String message) {
        super(message);
    }

    public ChatAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

