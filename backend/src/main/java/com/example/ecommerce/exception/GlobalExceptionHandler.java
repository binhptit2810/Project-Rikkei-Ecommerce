package com.example.ecommerce.exception;

import com.example.ecommerce.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý lỗi tập trung — các exception được bắt tại đây và trả về format ApiResponse chuẩn.
 *
 * Thứ tự ưu tiên handler: từ cụ thể nhất đến tổng quát nhất.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Lỗi đường dẫn / endpoint không tồn tại (Spring MVC 6.1+ / Spring Boot 3.3.x) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource or endpoint not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Đường dẫn không tồn tại"));
    }

    /** Lỗi nghiệp vụ khi không tìm thấy tài nguyên */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Lỗi nghiệp vụ chung (Bad request) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Lỗi xác thực sai thông tin đăng nhập (email/password không đúng).
     * Trả 401 thay vì 400 để phân biệt với lỗi validation dữ liệu.
     * Không tiết lộ chi tiết (email không tồn tại vs password sai).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng"));
    }

    /**
     * Lỗi tài khoản bị khóa kèm theo lý do cụ thể từ Admin — trả 403 FORBIDDEN.
     * data: { "reason": "...", "lockReason": "..." }
     */
    @ExceptionHandler(LockedAccountException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleLockedAccount(LockedAccountException ex) {
        log.warn("Locked account login attempt: reason={}", ex.getReason());
        Map<String, String> data = new HashMap<>();
        if (StringUtils.hasText(ex.getReason())) {
            data.put("reason", ex.getReason());
            data.put("lockReason", ex.getReason());
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Tài khoản của bạn đã bị khóa.")
                        .data(data.isEmpty() ? null : data)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Lỗi tài khoản bị vô hiệu hóa — trả 403 thay vì 401.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleDisabled(DisabledException ex) {
        log.warn("Disabled account login attempt: {}", ex.getMessage());
        Map<String, String> data = new HashMap<>();
        if (StringUtils.hasText(ex.getMessage()) && !ex.getMessage().equalsIgnoreCase("User is disabled")) {
            data.put("reason", ex.getMessage());
            data.put("lockReason", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Tài khoản của bạn đã bị khóa.")
                        .data(data.isEmpty() ? null : data)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /**
     * Lỗi không đủ quyền truy cập.
     * Lưu ý: SecurityConfig đã xử lý AccessDeniedException qua accessDeniedHandler.
     * Handler này là fallback cho @PreAuthorize trong Service/Controller.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
    }

    /** Lỗi validation Bean Validation */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dữ liệu không hợp lệ")
                        .data(errors)
                        .build());
    }

    /**
     * Lỗi Redis không khả dụng hoặc truy vấn quá thời gian — trả 503 SERVICE UNAVAILABLE.
     */
    @ExceptionHandler({
            org.springframework.data.redis.RedisConnectionFailureException.class,
            org.springframework.dao.QueryTimeoutException.class,
            org.springframework.data.redis.RedisSystemException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRedisUnavailable(Exception ex) {
        log.error("Redis service unavailable or query timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Dịch vụ xác thực tạm thời không khả dụng, vui lòng thử lại sau"));
    }

    /** Lỗi không xác định — ghi log và trả về 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
