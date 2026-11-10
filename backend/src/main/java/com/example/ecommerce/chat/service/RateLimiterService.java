package com.example.ecommerce.chat.service;

/**
 * Interface kiểm soát tần suất gửi yêu cầu (Rate Limiter) phân tán cho Chatbot.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 *
 * Nguyên tắc & Hạn mức:
 * 1. Cả Khách vãng lai (Guest) và Người dùng đăng nhập (User) đều áp dụng chung: 10 requests / 60 giây / IP.
 * 2. Thuật toán Token Bucket Greedy Refill thông qua Bucket4j và Redis Lettuce CAS.
 * 3. Khi Redis gặp sự cố hoặc timeout: Fail-Closed (HTTP 503).
 */
public interface RateLimiterService {

    /**
     * Kiểm tra hạn mức yêu cầu của client theo Token Bucket (10 requests / 60s / IP).
     * Ném ChatRateLimitExceededException (HTTP 429) nếu vượt quá giới hạn cho phép.
     * Ném RedisSystemException (HTTP 503) nếu Redis không khả dụng (Fail-Closed).
     *
     * @param clientIp            Địa chỉ IP của client (được xác định an toàn qua ClientIpResolver)
     * @param authenticatedUserId ID của người dùng đã xác thực (dùng chung bucket IP)
     */
    void checkRateLimit(String clientIp, String authenticatedUserId);
}

