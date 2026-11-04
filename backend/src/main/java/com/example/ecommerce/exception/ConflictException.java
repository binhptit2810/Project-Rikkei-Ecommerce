package com.example.ecommerce.exception;

/**
 * Ngoại lệ xung đột trạng thái tài nguyên (HTTP 409 CONFLICT).
 * Ném ra khi trạng thái hiện tại của thực thể không cho phép thực hiện thao tác yêu cầu
 * (ví dụ: hủy đơn hàng khi đơn đã sang trạng thái PROCESSING, SHIPPING, COMPLETED hoặc đã bị CANCELLED trước đó).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

