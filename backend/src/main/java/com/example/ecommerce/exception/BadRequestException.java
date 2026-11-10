package com.example.ecommerce.exception;

/**
 * Ngoại lệ yêu cầu không hợp lệ (HTTP 400 BAD REQUEST).
 * Ném ra khi client gửi dữ liệu vi phạm nghiệp vụ (ví dụ: đánh giá sản phẩm khi chưa mua).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

