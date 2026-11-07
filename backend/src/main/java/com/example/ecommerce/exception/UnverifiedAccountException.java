package com.example.ecommerce.exception;

import lombok.Getter;

/**
 * Ngoại lệ ném ra khi tài khoản CUSTOMER chưa hoàn tất xác thực email.
 */
@Getter
public class UnverifiedAccountException extends RuntimeException {

    private final String email;
    private final String errorCode = "ACCOUNT_NOT_VERIFIED";

    public UnverifiedAccountException(String message, String email) {
        super(message);
        this.email = email;
    }
}

