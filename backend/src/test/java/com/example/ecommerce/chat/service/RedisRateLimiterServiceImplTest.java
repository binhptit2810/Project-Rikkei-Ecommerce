package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.service.impl.RedisRateLimiterServiceImpl;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RedisRateLimiterServiceImpl.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisRateLimiterServiceImpl rateLimiterService;

    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_USER_ID = "1001";

    @Nested
    @DisplayName("1. Guest Rate Limiter Tests (Hạn mức 10 req/phút)")
    class GuestRateLimiterTests {

        @Test
        @DisplayName("RL-01: Yêu cầu của khách vãng lai nằm trong hạn mức (counter <= 10) được cho phép")
        void testCheckRateLimit_GuestUnderLimit() {
            // Redis script trả về {allowed=1, ttlMillis=50000, currentCount=5}
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("chat:rate-limit:guest:{" + TEST_IP + "}")), any(), any()))
                    .thenReturn(List.of(1L, 50000L, 5L));

            // Không ném ngoại lệ
            rateLimiterService.checkRateLimit(TEST_IP, null);

            verify(redisTemplate).execute(
                    any(DefaultRedisScript.class),
                    eq(Collections.singletonList("chat:rate-limit:guest:{" + TEST_IP + "}")),
                    eq("60"),
                    eq("10")
            );
        }

        @Test
        @DisplayName("RL-02: Yêu cầu của khách vãng lai vượt quá hạn mức (> 10) bị từ chối với ChatRateLimitExceededException")
        void testCheckRateLimit_GuestOverLimit() {
            // Redis script trả về {allowed=0, ttlMillis=45200, currentCount=11}
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("chat:rate-limit:guest:{" + TEST_IP + "}")), any(), any()))
                    .thenReturn(List.of(0L, 45200L, 11L));

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(ChatRateLimitExceededException.class)
                    .hasMessageContaining("tối đa 10 câu hỏi/phút")
                    .satisfies(ex -> {
                        ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                        assertThat(rle.getRetryAfterSeconds()).isEqualTo(46L); // (45200 + 999) / 1000 = 46
                    });
        }

        @Test
        @DisplayName("RL-03: Client IP null hoặc blank được chuẩn hóa thành 'unknown' an toàn")
        void testCheckRateLimit_GuestBlankIpSanitized() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("chat:rate-limit:guest:{unknown}")), any(), any()))
                    .thenReturn(List.of(1L, 60000L, 1L));

            rateLimiterService.checkRateLimit("   ", null);

            verify(redisTemplate).execute(
                    any(DefaultRedisScript.class),
                    eq(Collections.singletonList("chat:rate-limit:guest:{unknown}")),
                    eq("60"),
                    eq("10")
            );
        }
    }

    @Nested
    @DisplayName("2. Authenticated User Rate Limiter Tests (Hạn mức 30 req/phút)")
    class UserRateLimiterTests {

        @Test
        @DisplayName("RL-04: Yêu cầu của người dùng đã xác thực nằm trong hạn mức (counter <= 30) được cho phép")
        void testCheckRateLimit_UserUnderLimit() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("chat:rate-limit:user:{" + TEST_USER_ID + "}")), any(), any()))
                    .thenReturn(List.of(1L, 40000L, 25L));

            rateLimiterService.checkRateLimit(TEST_IP, TEST_USER_ID);

            verify(redisTemplate).execute(
                    any(DefaultRedisScript.class),
                    eq(Collections.singletonList("chat:rate-limit:user:{" + TEST_USER_ID + "}")),
                    eq("60"),
                    eq("30")
            );
        }

        @Test
        @DisplayName("RL-05: Yêu cầu của người dùng đã xác thực vượt quá hạn mức (> 30) bị từ chối")
        void testCheckRateLimit_UserOverLimit() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("chat:rate-limit:user:{" + TEST_USER_ID + "}")), any(), any()))
                    .thenReturn(List.of(0L, 30100L, 31L));

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, TEST_USER_ID))
                    .isInstanceOf(ChatRateLimitExceededException.class)
                    .hasMessageContaining("tối đa 30 câu hỏi/phút")
                    .satisfies(ex -> {
                        ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                        assertThat(rle.getRetryAfterSeconds()).isEqualTo(31L); // (30100 + 999) / 1000 = 31
                    });
        }
    }

    @Nested
    @DisplayName("3. Error & Fallback Tests")
    class ErrorAndFallbackTests {

        @Test
        @DisplayName("RL-06: Redis script trả về null hoặc rỗng, cho phép request đi tiếp dưới dạng fallback")
        void testCheckRateLimit_RedisNullResponseFallback() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                    .thenReturn(null);

            // Fallback: không ném ngoại lệ làm gián đoạn người dùng
            rateLimiterService.checkRateLimit(TEST_IP, null);

            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                    .thenReturn(Collections.emptyList());

            rateLimiterService.checkRateLimit(TEST_IP, null);
        }

        @Test
        @DisplayName("RL-07: Lỗi kết nối Redis hạ tầng được bọc thành RedisSystemException")
        void testCheckRateLimit_RedisInfrastructureException() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                    .thenThrow(new RuntimeException("Redis connection timeout"));

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(RedisSystemException.class)
                    .hasMessageContaining("Lỗi kết nối Redis khi kiểm tra hạn mức");
        }

        @Test
        @DisplayName("RL-08: Retry-After tối thiểu là 1 giây ngay cả khi TTL millisecond rất nhỏ")
        void testCheckRateLimit_MinimumOneSecondRetryAfter() {
            when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                    .thenReturn(List.of(0L, 50L, 11L)); // 50ms remaining

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(ChatRateLimitExceededException.class)
                    .satisfies(ex -> {
                        ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                        assertThat(rle.getRetryAfterSeconds()).isEqualTo(1L);
                    });
        }

        @Test
        @DisplayName("RL-09: Kiểm tra hằng số cấu hình chuẩn")
        void testConfigurationConstants() {
            assertThat(RedisRateLimiterServiceImpl.GUEST_LIMIT).isEqualTo(10);
            assertThat(RedisRateLimiterServiceImpl.USER_LIMIT).isEqualTo(30);
            assertThat(RedisRateLimiterServiceImpl.WINDOW_SECONDS).isEqualTo(60);
        }
    }
}
