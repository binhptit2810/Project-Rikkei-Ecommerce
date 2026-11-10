package com.example.ecommerce.chat.config;

import com.example.ecommerce.chat.model.ChatRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;

/**
 * Chính sách Retry phân tầng chuyên biệt cho Google Gemini API kết nối qua Spring AI.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tối ưu hóa độ tin cậy và xử lý Quota Chatbot Gemini.
 *
 * Nguyên tắc phân loại:
 * 1. 4xx (400, 401, 403, 404) & Quota/Billing cạn kiệt: TUYỆT ĐỐI KHÔNG RETRY.
 * 2. 429 [no body] không metadata: FAIL-FAST ngay để chống Retry Storm.
 * 3. 429 có Retry-After <= 5s: Retry tối đa 1 lần nếu còn ngân sách deadline >= 8s.
 * 4. 502, 503, 504: Retry tối đa 1 lần với Exponential Backoff.
 * 5. Connect Timeout: Retry tối đa 1 lần.
 * 6. Read Timeout: TUYỆT ĐỐI KHÔNG RETRY.
 */
@Slf4j
public class GeminiRetryPolicy extends SimpleRetryPolicy {

    public GeminiRetryPolicy(int maxAttempts) {
        super(maxAttempts, Collections.emptyMap(), true, false);
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable t = context.getLastThrowable();
        if (t == null) {
            return context.getRetryCount() < getMaxAttempts();
        }
        if (!shouldRetry(t)) {
            return false;
        }
        return context.getRetryCount() < getMaxAttempts();
    }

    public boolean shouldRetry(Throwable throwable) {
        // Kiểm tra ngân sách thời gian còn lại
        ChatRequestContext requestContext = ChatRequestContext.get();
        if (requestContext != null && requestContext.getRemainingMs() < 8000L) {
            log.warn("Ngân sách thời gian còn lại quá ít ({}ms), từ chối retry Gemini API",
                    requestContext.getRemainingMs());
            return false;
        }

        // 1. Kiểm tra 4xx
        if (throwable instanceof HttpClientErrorException clientError) {
            HttpStatusCode status = clientError.getStatusCode();
            if (status == HttpStatus.TOO_MANY_REQUESTS) { // 429
                String body = clientError.getResponseBodyAsString();
                if (isQuotaExhausted(body)) {
                    log.warn("Gemini 429 Quota/Billing Exhausted: Tuyệt đối không retry");
                    return false;
                }

                // Kiểm tra Header Retry-After
                HttpHeaders headers = clientError.getResponseHeaders();
                String retryAfterHeader = (headers != null) ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;
                if (retryAfterHeader != null && !retryAfterHeader.isBlank()) {
                    Duration duration = parseRetryAfter(retryAfterHeader);
                    if (duration != null && duration.toSeconds() <= 5L) {
                        log.info("Gemini 429 có Retry-After={}s <= 5s: Cho phép retry 1 lần", duration.toSeconds());
                        return true;
                    }
                    log.warn("Gemini 429 có Retry-After={}s > 5s: Fail-fast, không retry",
                            duration != null ? duration.toSeconds() : "unknown");
                    return false;
                }

                // 429 [no body] hoặc không có Retry-After: Fail-fast chống Retry Storm!
                log.warn("Gemini 429 [no body] không có metadata Retry-After: Fail-fast chống retry storm");
                return false;
            }

            // Mọi 4xx khác (400, 401, 403, 404): Không retry
            return false;
        }

        // 2. Kiểm tra 5xx
        if (throwable instanceof HttpServerErrorException serverError) {
            HttpStatusCode status = serverError.getStatusCode();
            if (status == HttpStatus.SERVICE_UNAVAILABLE || status == HttpStatus.BAD_GATEWAY || status == HttpStatus.GATEWAY_TIMEOUT) {
                log.info("Gemini trả lỗi tạm thời {}: Cho phép retry 1 lần", status.value());
                return true;
            }
            return false;
        }

        // 3. Kiểm tra lỗi mạng ResourceAccessException
        if (throwable instanceof ResourceAccessException rae) {
            Throwable cause = rae.getCause();
            if (cause instanceof SocketTimeoutException ste) {
                String msg = (ste.getMessage() != null) ? ste.getMessage().toLowerCase() : "";
                if (msg.contains("connect timed out") || msg.contains("connect")) {
                    log.info("Gemini Connect Timeout: Cho phép retry kết nối 1 lần");
                    return true;
                }
                if (msg.contains("read timed out") || msg.contains("read")) {
                    log.warn("Gemini Read Timeout: TUYỆT ĐỐI KHÔNG RETRY để tránh nhân đôi thời gian và tải");
                    return false;
                }
            }
            if (cause instanceof ConnectException) {
                log.info("Gemini ConnectException: Cho phép retry kết nối 1 lần");
                return true;
            }
            if (cause instanceof SocketException) {
                log.info("Gemini SocketException: Cho phép retry 1 lần");
                return true;
            }
        }

        return false;
    }

    private boolean isQuotaExhausted(String body) {
        if (body == null || body.isBlank()) return false;
        String lower = body.toLowerCase();
        return lower.contains("resource_exhausted") ||
                lower.contains("insufficient_quota") ||
                lower.contains("quota exceeded") ||
                lower.contains("billing");
    }

    public static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return null;
        String trimmed = header.trim();
        try {
            long seconds = Long.parseLong(trimmed);
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
        }
        try {
            ZonedDateTime retryDate = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration d = Duration.between(ZonedDateTime.now(retryDate.getZone()), retryDate);
            return d.isNegative() ? Duration.ZERO : d;
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}

