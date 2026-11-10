package com.example.ecommerce.chat.service.impl;

import com.example.ecommerce.chat.security.ClientIpResolver;
import com.example.ecommerce.chat.service.RateLimiterService;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Hiện thực hóa kiểm soát tần suất phân tán (Rate Limiting) cho Chatbot sử dụng Bucket4j + Redis (Lettuce CAS).
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 *
 * Chính sách kỹ thuật:
 * 1. Khách vãng lai (Guest) và Người dùng đăng nhập (User) đều áp dụng chung: 10 requests / 60 giây / IP.
 *    Hạn mức 30 requests/user trước đây bị loại bỏ hoàn toàn.
 * 2. Cùng địa chỉ IP chia sẻ chung một Bucket Token duy nhất để chống lạm dụng.
 * 3. Thuật toán Token Bucket Greedy Refill: dung lượng 10 tokens, nạp tuyến tính 10 tokens / 60s (1 token mỗi 6 giây).
 *    Cho phép burst tối đa 10 requests, sau đó cứ 6 giây được thêm 1 token.
 * 4. Cơ chế Fail-Closed: Khi Redis gặp sự cố (down, timeout, connection refuse) hoặc kết quả null/không hợp lệ,
 *    hệ thống lập tức ném RedisSystemException (trả về HTTP 503), TUYỆT ĐỐI KHÔNG fallback in-memory hay bỏ qua.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiterServiceImpl implements RateLimiterService {

    private final ProxyManager<String> proxyManager;
    private final ClientIpResolver clientIpResolver;

    public static final int LIMIT_CAPACITY = 10;
    public static final int WINDOW_SECONDS = 60;
    public static final int GUEST_LIMIT = 10;
    public static final int USER_LIMIT = 10;

    private static final BucketConfiguration BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(LIMIT_CAPACITY).refillGreedy(LIMIT_CAPACITY, Duration.ofSeconds(WINDOW_SECONDS)))
            .build();

    @Override
    public void checkRateLimit(String clientIp, String authenticatedUserId) {
        String sanitizedIp = (clientIpResolver != null) ? clientIpResolver.sanitizeIp(clientIp) : ((clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown");
        String key = "chat:rate-limit:ip:{" + sanitizedIp + "}";

        try {
            Bucket bucket = proxyManager.builder().build(key, () -> BUCKET_CONFIGURATION);
            if (bucket == null) {
                log.error("ProxyManager returned null bucket for key={}", key);
                throw new RedisSystemException("Không thể khởi tạo Token Bucket từ Redis", null);
            }

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe == null) {
                log.error("ConsumptionProbe returned null from Redis for key={}", key);
                throw new RedisSystemException("Kết quả kiểm tra hạn mức không hợp lệ từ Redis", null);
            }

            if (!probe.isConsumed()) {
                long nanosToWait = probe.getNanosToWaitForRefill();
                long retryAfterSeconds = Math.max(1L, (nanosToWait + 999_999_999L) / 1_000_000_000L);
                log.warn("Rate limit exceeded for IP={}: retryAfterSeconds={}", sanitizedIp, retryAfterSeconds);
                throw new ChatRateLimitExceededException(
                        "Bạn đã gửi quá nhiều tin nhắn (tối đa " + LIMIT_CAPACITY + " câu hỏi/phút). Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                        retryAfterSeconds
                );
            }
        } catch (ChatRateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            // FAIL-CLOSED: Bắt mọi lỗi hạ tầng Redis, không fallback in-memory, bảo vệ API Gemini upstream
            log.error("Redis rate limiter infrastructure failure for IP [{}]: {}", sanitizedIp, e.getMessage());
            throw new RedisSystemException("Lỗi kết nối Redis khi kiểm tra hạn mức tần suất gửi tin nhắn", e);
        }
    }
}
