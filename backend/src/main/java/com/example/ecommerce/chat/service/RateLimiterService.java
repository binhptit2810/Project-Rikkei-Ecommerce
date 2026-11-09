package com.example.ecommerce.chat.service;

/**
 * Interface kiểm soát tần suất gửi yêu cầu (Rate Limiter) phân tán cho Chatbot.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc & Hạn mức:
 * 1. Khách vãng lai (Guest): 10 requests / phút / IP.
 * 2. Người dùng đã đăng nhập (User): 30 requests / phút / userId.
 * 3. Kiểm tra và đếm nguyên tử qua Redis Lua Script, chống race condition.
 */
public interface RateLimiterService {

    /**
     * Kiểm tra và tăng số lượng request của client trong cửa sổ trượt 60 giây.
     * Ném ChatRateLimitExceededException nếu vượt quá giới hạn cho phép.
     *
     * @param clientIp            Địa chỉ IP thực của client (áp dụng khi khách là Guest)
     * @param authenticatedUserId ID của người dùng đã xác thực (null nếu là Guest)
     */
    void checkRateLimit(String clientIp, String authenticatedUserId);
}

