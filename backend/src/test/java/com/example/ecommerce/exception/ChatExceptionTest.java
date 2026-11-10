package com.example.ecommerce.exception;

import com.example.ecommerce.exception.ChatAuthenticationException;
import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test kiểm tra khởi tạo và message của các ngoại lệ chuyên biệt cho Chatbot.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
class ChatExceptionTest {

    @Test
    @DisplayName("EX-01: ChatLockContentionException khởi tạo thành công với message")
    void testChatLockContentionException_WithMessage() {
        String message = "Hệ thống đang xử lý yêu cầu trước, vui lòng thử lại sau giây lát";
        ChatLockContentionException ex = new ChatLockContentionException(message);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("EX-02: ChatLockContentionException khởi tạo thành công với message và cause")
    void testChatLockContentionException_WithMessageAndCause() {
        String message = "Lock contention occurred";
        RuntimeException cause = new RuntimeException("Redis lock acquire timeout");
        ChatLockContentionException ex = new ChatLockContentionException(message, cause);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("EX-03: ChatVersionConflictException khởi tạo thành công với message")
    void testChatVersionConflictException_WithMessage() {
        String message = "Phiên bản dữ liệu không khớp, vui lòng thử lại";
        ChatVersionConflictException ex = new ChatVersionConflictException(message);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("EX-04: ChatVersionConflictException khởi tạo thành công với message và cause")
    void testChatVersionConflictException_WithMessageAndCause() {
        String message = "OCC version mismatch";
        RuntimeException cause = new RuntimeException("Version 2 expected but was 3");
        ChatVersionConflictException ex = new ChatVersionConflictException(message, cause);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("EX-05: GeminiServiceException khởi tạo thành công với message")
    void testGeminiServiceException_WithMessage() {
        String message = "Dịch vụ AI tạm thời không khả dụng do lỗi xác thực";
        GeminiServiceException ex = new GeminiServiceException(message);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("EX-06: GeminiServiceException khởi tạo thành công với message và cause")
    void testGeminiServiceException_WithMessageAndCause() {
        String message = "Không thể kết nối đến dịch vụ AI";
        IOException cause = new IOException("Connection refused");
        GeminiServiceException ex = new GeminiServiceException(message, cause);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("EX-07: GeminiTimeoutException khởi tạo thành công với message")
    void testGeminiTimeoutException_WithMessage() {
        String message = "Yêu cầu AI vượt quá thời gian xử lý";
        GeminiTimeoutException ex = new GeminiTimeoutException(message);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("EX-08: GeminiTimeoutException khởi tạo thành công với message và cause")
    void testGeminiTimeoutException_WithMessageAndCause() {
        String message = "Timeout khi gọi Gemini upstream";
        SocketTimeoutException cause = new SocketTimeoutException("Read timed out");
        GeminiTimeoutException ex = new GeminiTimeoutException(message, cause);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("EX-09: ChatRateLimitExceededException khởi tạo thành công với message và retryAfterSeconds")
    void testChatRateLimitExceededException() {
        String message = "Quá nhiều câu hỏi";
        ChatRateLimitExceededException ex = new ChatRateLimitExceededException(message, 30L);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(30L);
    }

    @Test
    @DisplayName("EX-10: ChatRateLimitExceededException bảo đảm retryAfterSeconds tối thiểu là 1 giây")
    void testChatRateLimitExceededException_MinimumOneSecond() {
        ChatRateLimitExceededException ex = new ChatRateLimitExceededException("Limit reached", 0L);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(1L);

        ChatRateLimitExceededException exNeg = new ChatRateLimitExceededException("Limit reached", -5L);
        assertThat(exNeg.getRetryAfterSeconds()).isEqualTo(1L);
    }

    @Test
    @DisplayName("EX-11: ChatAuthenticationException khởi tạo thành công với message")
    void testChatAuthenticationException_WithMessage() {
        String message = "Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)";
        ChatAuthenticationException ex = new ChatAuthenticationException(message);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("EX-12: ChatAuthenticationException khởi tạo thành công với message và cause")
    void testChatAuthenticationException_WithMessageAndCause() {
        String message = "Guest token không hợp lệ";
        RuntimeException cause = new RuntimeException("Invalid token signature");
        ChatAuthenticationException ex = new ChatAuthenticationException(message, cause);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}

