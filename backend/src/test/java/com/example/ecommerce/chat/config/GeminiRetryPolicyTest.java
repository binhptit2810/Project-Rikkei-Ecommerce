package com.example.ecommerce.chat.config;

import com.example.ecommerce.chat.model.ChatRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test toàn diện cho GeminiRetryPolicy.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Khắc phục Chatbot Gemini chập chờn, timeout & 429 Too Many Requests.
 */
class GeminiRetryPolicyTest {

    private GeminiRetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        retryPolicy = new GeminiRetryPolicy(2);
        // Thiết lập budget dồi dào mặc định: 30s
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);
    }

    @AfterEach
    void tearDown() {
        ChatRequestContext.clear();
    }

    @Test
    @DisplayName("GRP-01: Connect timeout cho phép retry 1 lần")
    void testShouldRetry_ConnectTimeout() {
        SocketTimeoutException ste = new SocketTimeoutException("connect timed out");
        ResourceAccessException rae = new ResourceAccessException("Connection timed out", ste);

        assertThat(retryPolicy.shouldRetry(rae)).isTrue();
    }

    @Test
    @DisplayName("GRP-02: ConnectException cho phép retry 1 lần")
    void testShouldRetry_ConnectException() {
        ConnectException ce = new ConnectException("Connection refused: connect");
        ResourceAccessException rae = new ResourceAccessException("Connection error", ce);

        assertThat(retryPolicy.shouldRetry(rae)).isTrue();
    }

    @Test
    @DisplayName("GRP-03: Read timeout TUYỆT ĐỐI KHÔNG RETRY để tránh nhân đôi trễ")
    void testShouldRetry_ReadTimeout_NeverRetries() {
        SocketTimeoutException ste = new SocketTimeoutException("Read timed out");
        ResourceAccessException rae = new ResourceAccessException("I/O error on POST request: Read timed out", ste);

        assertThat(retryPolicy.shouldRetry(rae)).isFalse();
    }

    @Test
    @DisplayName("GRP-04: SocketException cho phép retry")
    void testShouldRetry_SocketException() {
        SocketException se = new SocketException("Connection reset");
        ResourceAccessException rae = new ResourceAccessException("Socket error", se);

        assertThat(retryPolicy.shouldRetry(rae)).isTrue();
    }

    @Test
    @DisplayName("GRP-05: 429 [no body] không có header Retry-After FAIL-FAST ngay lập tức, không retry")
    void testShouldRetry_429NoBody_FailFast() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        assertThat(retryPolicy.shouldRetry(ex)).isFalse();
    }

    @Test
    @DisplayName("GRP-06: 429 Quota cạn kiệt (RESOURCE_EXHAUSTED) TUYỆT ĐỐI KHÔNG RETRY")
    void testShouldRetry_429QuotaExhausted_NeverRetries() {
        String errorBody = """
            {
              "error": {
                "code": 429,
                "message": "Resource has been exhausted (e.g. check quota).",
                "status": "RESOURCE_EXHAUSTED"
              }
            }
            """;
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                HttpHeaders.EMPTY, errorBody.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );

        assertThat(retryPolicy.shouldRetry(ex)).isFalse();
    }

    @Test
    @DisplayName("GRP-07: 429 có Retry-After <= 5s và ngân sách dồi dào cho phép retry 1 lần")
    void testShouldRetry_429WithShortRetryAfter_Retries() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "3");

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                headers, "Rate limit reached".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );

        assertThat(retryPolicy.shouldRetry(ex)).isTrue();
    }

    @Test
    @DisplayName("GRP-08: 429 có Retry-After > 5s từ chối retry (fail-fast)")
    void testShouldRetry_429WithLongRetryAfter_FailFast() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "60");

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                headers, "Rate limit reached".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );

        assertThat(retryPolicy.shouldRetry(ex)).isFalse();
    }

    @Test
    @DisplayName("GRP-09: 502, 503, 504 cho phép retry 1 lần")
    void testShouldRetry_Transient5xx() {
        HttpServerErrorException ex502 = new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
        HttpServerErrorException ex503 = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        HttpServerErrorException ex504 = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT);

        assertThat(retryPolicy.shouldRetry(ex502)).isTrue();
        assertThat(retryPolicy.shouldRetry(ex503)).isTrue();
        assertThat(retryPolicy.shouldRetry(ex504)).isTrue();
    }

    @Test
    @DisplayName("GRP-10: 400, 401, 403, 404, 500 thông thường không retry")
    void testShouldRetry_NonRetryableStatusCodes() {
        HttpClientErrorException ex400 = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        HttpClientErrorException ex401 = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        HttpClientErrorException ex403 = new HttpClientErrorException(HttpStatus.FORBIDDEN);
        HttpClientErrorException ex404 = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        HttpServerErrorException ex500 = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(retryPolicy.shouldRetry(ex400)).isFalse();
        assertThat(retryPolicy.shouldRetry(ex401)).isFalse();
        assertThat(retryPolicy.shouldRetry(ex403)).isFalse();
        assertThat(retryPolicy.shouldRetry(ex404)).isFalse();
        assertThat(retryPolicy.shouldRetry(ex500)).isFalse();
    }

    @Test
    @DisplayName("GRP-11: Khi ngân sách thời gian còn lại < 8000ms, từ chối mọi retry kể cả lỗi tạm thời")
    void testShouldRetry_InsufficientBudget_DeniesRetry() {
        // Chỉ còn 5s ngân sách
        ChatRequestContext.init(System.currentTimeMillis() + 5000L, 3);

        HttpServerErrorException ex503 = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(retryPolicy.shouldRetry(ex503)).isFalse();
    }

    @Test
    @DisplayName("GRP-12: canRetry tôn trọng maxAttempts = 2 của SimpleRetryPolicy")
    void testCanRetry_MaxAttempts() {
        RetryContext context = retryPolicy.open(null);
        HttpServerErrorException ex503 = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);

        // Attempt 1: canRetry = true
        retryPolicy.registerThrowable(context, ex503);
        assertThat(retryPolicy.canRetry(context)).isTrue();

        // Attempt 2: canRetry = false (đã đạt giới hạn maxAttempts=2)
        retryPolicy.registerThrowable(context, ex503);
        assertThat(retryPolicy.canRetry(context)).isFalse();
    }

    @Test
    @DisplayName("GRP-13: parseRetryAfter phân tích chính xác dạng seconds và dạng RFC 1123 HTTP-date")
    void testParseRetryAfter() {
        // Dạng số giây
        Duration d1 = GeminiRetryPolicy.parseRetryAfter("5");
        assertThat(d1).isNotNull();
        assertThat(d1.toSeconds()).isEqualTo(5L);

        // Dạng HTTP-date tương lai 10s
        ZonedDateTime future = ZonedDateTime.now().plusSeconds(10);
        String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(future);
        Duration d2 = GeminiRetryPolicy.parseRetryAfter(httpDate);
        assertThat(d2).isNotNull();
        assertThat(d2.toSeconds()).isBetween(8L, 12L);

        // Dạng null hoặc không hợp lệ
        assertThat(GeminiRetryPolicy.parseRetryAfter(null)).isNull();
        assertThat(GeminiRetryPolicy.parseRetryAfter("")).isNull();
        assertThat(GeminiRetryPolicy.parseRetryAfter("invalid-format")).isNull();
    }
}

