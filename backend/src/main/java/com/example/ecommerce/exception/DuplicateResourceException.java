package com.example.ecommerce.exception;

/**
 * Ngoại lệ trùng lặp tài nguyên (HTTP 409 CONFLICT hoặc HTTP 400).
 * Ném ra khi người dùng cố gắng tạo lại tài nguyên đã tồn tại (ví dụ: đã đánh giá sản phẩm này rồi).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}

