package com.example.ecommerce.exception;

/**
 * Ngoại lệ xảy ra khi yêu cầu gọi tới Gemini bị quá thời gian xử lý (Connect/Read timeout hoặc Request Deadline exceeded).
 * Được GlobalExceptionHandler ánh xạ thành HTTP 504 Gateway Timeout.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Trích xuất exception độc lập thay thế GeminiRestClient.
 */
public class GeminiTimeoutException extends RuntimeException {

    public GeminiTimeoutException(String message) {
        super(message);
    }

    public GeminiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

