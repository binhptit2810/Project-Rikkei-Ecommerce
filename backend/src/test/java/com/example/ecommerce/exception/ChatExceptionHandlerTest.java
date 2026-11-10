package com.example.ecommerce.exception;

import com.example.ecommerce.exception.ChatAuthenticationException;
import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import com.example.ecommerce.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test kiểm tra ánh xạ mã HTTP và ApiResponse của các ngoại lệ Chatbot trong GlobalExceptionHandler.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
class ChatExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("EX-01: ChatLockContentionException được ánh xạ thành HTTP 409 Conflict")
    void testHandleChatLockContention() {
        ChatLockContentionException ex = new ChatLockContentionException("Phiên đang xử lý yêu cầu trước");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleChatLockContention(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Phiên đang xử lý yêu cầu trước");
    }

    @Test
    @DisplayName("EX-02: ChatRateLimitExceededException được ánh xạ thành HTTP 429 kèm header Retry-After")
    void testHandleChatRateLimitExceeded() {
        ChatRateLimitExceededException ex = new ChatRateLimitExceededException("Vượt quá 10 câu/phút", 45L);
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleChatRateLimitExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("45");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Vượt quá 10 câu/phút");
    }

    @Test
    @DisplayName("EX-03: ChatVersionConflictException được ánh xạ thành HTTP 409 Conflict")
    void testHandleChatVersionConflict() {
        ChatVersionConflictException ex = new ChatVersionConflictException("OCC conflict");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleChatVersionConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("OCC conflict");
    }

    @Test
    @DisplayName("EX-04: GeminiServiceException được ánh xạ thành HTTP 503 Service Unavailable")
    void testHandleGeminiServiceException() {
        GeminiServiceException ex = new GeminiServiceException("Upstream 500 error");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeminiService(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Dịch vụ tư vấn AI tạm thời không khả dụng, vui lòng thử lại sau");
    }

    @Test
    @DisplayName("EX-05: GeminiTimeoutException được ánh xạ thành HTTP 504 Gateway Timeout")
    void testHandleGeminiTimeoutException() {
        GeminiTimeoutException ex = new GeminiTimeoutException("Timeout after 12s");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeminiTimeout(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Dịch vụ tư vấn AI phản hồi quá thời gian, vui lòng thử lại sau");
    }

    @Test
    @DisplayName("EX-06: BadCredentialsException trả về HTTP 401 Unauthorized cùng thông điệp bảo mật chuẩn")
    void testHandleBadCredentials_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Sai mật khẩu");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Email hoặc mật khẩu không đúng");
    }

    @Test
    @DisplayName("EX-07: ChatAuthenticationException được ánh xạ thành HTTP 401 Unauthorized kèm thông điệp nghiệp vụ")
    void testHandleChatAuthenticationException() {
        ChatAuthenticationException ex = new ChatAuthenticationException("Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)");
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleChatAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)");
    }
}
