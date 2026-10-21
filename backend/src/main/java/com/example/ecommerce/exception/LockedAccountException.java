package com.example.ecommerce.exception;

import lombok.Getter;
import org.springframework.security.authentication.DisabledException;

/**
 * Ngoại lệ ném ra khi tài khoản đã bị Admin khóa và người dùng cố gắng đăng nhập.
 * Kế thừa DisabledException để tương thích với kiến trúc xác thực của Spring Security,
 * đồng thời mang theo lý do khóa (reason) lấy từ database để trả về cho client.
 */
@Getter
public class LockedAccountException extends DisabledException {

    private final String reason;

    public LockedAccountException(String message, String reason) {
        super(message);
        this.reason = reason;
    }
}
