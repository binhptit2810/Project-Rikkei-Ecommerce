package com.example.ecommerce.chat.service.impl;

import com.example.ecommerce.chat.service.RateLimiterService;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Hiện thực hóa kiểm soát tần suất phân tán (Rate Limiting) cho Chatbot sử dụng Redis Lua Script.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Quy tắc kỹ thuật:
 * 1. Guest: Giới hạn 10 requests / 60 giây theo IP. Redis Key: chat:rate-limit:guest:{ip}
 * 2. Authenticated User: Giới hạn 30 requests / 60 giây theo userId. Redis Key: chat:rate-limit:user:{userId}
 * 3. Lua script nguyên tử thực hiện INCR và EXPIRE đồng thời, loại bỏ triệt để rủi ro counter tồn tại vĩnh viễn không có TTL.
 * 4. Không lưu raw guest token trong rate-limit key để đảm bảo an toàn bảo mật.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiterServiceImpl implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public static final int GUEST_LIMIT = 10;
    public static final int USER_LIMIT = 30;
    public static final int WINDOW_SECONDS = 60;

    /**
     * Lua script nguyên tử:
     * - KEYS[1]: rate limit key
     * - ARGV[1]: windowSeconds (60s)
     * - ARGV[2]: limit (10 hoặc 30)
     *
     * Luồng xử lý:
     * 1. INCR counter
     * 2. Nếu counter == 1: thiết lập EXPIRE ARGV[1]
     * 3. Lấy TTL còn lại bằng PTTL (đơn vị millisecond)
     * 4. So sánh counter với limit:
     *    - Nếu counter > limit: trả về {0, ttlMillis, counter} (từ chối)
     *    - Ngược lại: trả về {1, ttlMillis, counter} (chấp thuận)
     */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setResultType(List.class);
        RATE_LIMIT_SCRIPT.setScriptText("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('PTTL', KEYS[1])
            local limit = tonumber(ARGV[2])
            if current > limit then
                return {0, ttl, current}
            else
                return {1, ttl, current}
            end
            """);
    }

    @Override
    public void checkRateLimit(String clientIp, String authenticatedUserId) {
        boolean isUser = (authenticatedUserId != null && !authenticatedUserId.isBlank());
        String key;
        int limit;

        if (isUser) {
            key = "chat:rate-limit:user:{" + authenticatedUserId.trim() + "}";
            limit = USER_LIMIT;
        } else {
            String sanitizedIp = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";
            key = "chat:rate-limit:guest:{" + sanitizedIp + "}";
            limit = GUEST_LIMIT;
        }

        try {
            @SuppressWarnings("rawtypes")
            List result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(WINDOW_SECONDS),
                    String.valueOf(limit)
            );

            if (result == null || result.isEmpty()) {
                log.warn("Redis rate limit script returned null/empty for key={}, allowing request as fallback", key);
                return;
            }

            long allowed = ((Number) result.get(0)).longValue();
            long ttlMillis = ((Number) result.get(1)).longValue();
            long currentCount = ((Number) result.get(2)).longValue();

            if (allowed == 0) {
                long retryAfterSeconds = Math.max(1L, (ttlMillis + 999L) / 1000L);
                log.warn("Rate limit exceeded for key={}: currentCount={}, limit={}, retryAfterSeconds={}",
                        key, currentCount, limit, retryAfterSeconds);
                throw new ChatRateLimitExceededException(
                        "Bạn đã gửi quá nhiều tin nhắn (tối đa " + limit + " câu hỏi/phút). Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                        retryAfterSeconds
                );
            }
        } catch (ChatRateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis error while executing rate limit script for key={}: {}", key, e.getMessage());
            throw new RedisSystemException("Lỗi kết nối Redis khi kiểm tra hạn mức tần suất gửi tin nhắn", e);
        }
    }
}

