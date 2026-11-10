package com.example.ecommerce.exception;

/**
 * Ngoại lệ xảy ra khi dịch vụ Gemini upstream gặp lỗi (401, 429, 500, lỗi kết nối hoặc định dạng sai).
 * Được GlobalExceptionHandler ánh xạ thành HTTP 503 Service Unavailable.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Trích xuất exception độc lập thay thế GeminiRestClient.
 */
public class GeminiServiceException extends RuntimeException {

    public GeminiServiceException(String message) {
        super(message);
    }

    public GeminiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

