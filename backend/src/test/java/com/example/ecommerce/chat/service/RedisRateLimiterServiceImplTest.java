package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.security.ClientIpResolver;
import com.example.ecommerce.chat.service.impl.RedisRateLimiterServiceImpl;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RedisRateLimiterServiceImpl sử dụng Bucket4j + Redis.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 */
@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceImplTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> bucketBuilder;

    @Mock
    private BucketProxy bucket;

    private ClientIpResolver clientIpResolver;
    private RedisRateLimiterServiceImpl rateLimiterService;

    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_USER_ID = "1001";

    @BeforeEach
    void setUp() {
        clientIpResolver = new ClientIpResolver("127.0.0.1,::1");
        rateLimiterService = new RedisRateLimiterServiceImpl(proxyManager, clientIpResolver);
    }

    @Nested
    @DisplayName("1. Token Bucket Rate Limiting (10 requests / 60s / IP)")
    class TokenBucketPolicyTests {

        @Test
        @DisplayName("RL-01: Yêu cầu của khách vãng lai nằm trong hạn mức (consumed=true) được chấp thuận")
        void testCheckRateLimit_GuestUnderLimit_Success() {
            ConsumptionProbe probe = ConsumptionProbe.consumed(9, 10);
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(eq("chat:rate-limit:ip:{" + TEST_IP + "}"), any(Supplier.class)))
                    .thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            // Không ném ngoại lệ
            rateLimiterService.checkRateLimit(TEST_IP, null);

            verify(bucket).tryConsumeAndReturnRemaining(1);
        }

        @Test
        @DisplayName("RL-02: Yêu cầu của khách vãng lai vượt quá hạn mức (consumed=false) bị chặn với 429 và Retry-After")
        void testCheckRateLimit_GuestOverLimit_RejectsWith429() {
            // Giả lập còn 5.2 giây để hồi phục 1 token
            ConsumptionProbe probe = ConsumptionProbe.rejected(0, 5_200_000_000L, 0);
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(eq("chat:rate-limit:ip:{" + TEST_IP + "}"), any(Supplier.class)))
                    .thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(ChatRateLimitExceededException.class)
                    .hasMessageContaining("tối đa 10 câu hỏi/phút")
                    .satisfies(ex -> {
                        ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                        assertThat(rle.getRetryAfterSeconds()).isEqualTo(6L); // ceil(5.2) = 6s
                    });
        }

        @Test
        @DisplayName("RL-03: Người dùng đã đăng nhập dùng chung bucket IP với Guest (10 req/phút/IP), không còn hạn mức 30")
        void testCheckRateLimit_UserSharesSameIpBucket() {
            ConsumptionProbe probe = ConsumptionProbe.consumed(5, 10);
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(eq("chat:rate-limit:ip:{" + TEST_IP + "}"), any(Supplier.class)))
                    .thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            // Truyền userId nhưng key vẫn là IP
            rateLimiterService.checkRateLimit(TEST_IP, TEST_USER_ID);

            verify(bucketBuilder).build(eq("chat:rate-limit:ip:{" + TEST_IP + "}"), any(Supplier.class));
        }

        @Test
        @DisplayName("RL-04: Hai IP khác nhau sử dụng hai Bucket Key độc lập")
        void testCheckRateLimit_DifferentIpsIndependentBuckets() {
            ConsumptionProbe probe = ConsumptionProbe.consumed(8, 10);
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            rateLimiterService.checkRateLimit("10.0.0.1", null);
            rateLimiterService.checkRateLimit("10.0.0.2", null);

            verify(bucketBuilder).build(eq("chat:rate-limit:ip:{10.0.0.1}"), any(Supplier.class));
            verify(bucketBuilder).build(eq("chat:rate-limit:ip:{10.0.0.2}"), any(Supplier.class));
        }

        @Test
        @DisplayName("RL-05: Client IP null hoặc rỗng được sanitize an toàn thành 'unknown'")
        void testCheckRateLimit_NullOrBlankIpSanitized() {
            ConsumptionProbe probe = ConsumptionProbe.consumed(9, 10);
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(eq("chat:rate-limit:ip:{unknown}"), any(Supplier.class)))
                    .thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            rateLimiterService.checkRateLimit("   ", null);

            verify(bucketBuilder).build(eq("chat:rate-limit:ip:{unknown}"), any(Supplier.class));
        }
    }

    @Nested
    @DisplayName("2. Fail-Closed & Error Handling (HTTP 503)")
    class FailClosedTests {

        @Test
        @DisplayName("RL-06: Khi Redis gặp sự cố kết nối, ném RedisSystemException (Fail-Closed)")
        void testCheckRateLimit_RedisConnectionFailure_Throws503() {
            when(proxyManager.builder()).thenThrow(new RuntimeException("Unable to connect to Redis: connection refused"));

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(RedisSystemException.class)
                    .hasMessageContaining("Lỗi kết nối Redis khi kiểm tra hạn mức");
        }

        @Test
        @DisplayName("RL-07: Khi Redis trả về null bucket, ném RedisSystemException, không cho qua")
        void testCheckRateLimit_NullBucket_Throws503() {
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(null);

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(RedisSystemException.class);
        }

        @Test
        @DisplayName("RL-08: Khi Redis trả về null probe, ném RedisSystemException, không cho qua")
        void testCheckRateLimit_NullProbe_Throws503() {
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(null);

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(RedisSystemException.class);
        }

        @Test
        @DisplayName("RL-09: Retry-After tối thiểu là 1 giây ngay cả khi thời gian chờ nanosecond rất nhỏ")
        void testCheckRateLimit_MinimumOneSecondRetryAfter() {
            ConsumptionProbe probe = ConsumptionProbe.rejected(0, 10_000_000L, 0); // 10ms
            when(proxyManager.builder()).thenReturn(bucketBuilder);
            when(bucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucket);
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(TEST_IP, null))
                    .isInstanceOf(ChatRateLimitExceededException.class)
                    .satisfies(ex -> {
                        ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                        assertThat(rle.getRetryAfterSeconds()).isEqualTo(1L);
                    });
        }

        @Test
        @DisplayName("RL-10: Kiểm tra hằng số cấu hình chuẩn Token Bucket")
        void testConfigurationConstants() {
            assertThat(RedisRateLimiterServiceImpl.LIMIT_CAPACITY).isEqualTo(10);
            assertThat(RedisRateLimiterServiceImpl.WINDOW_SECONDS).isEqualTo(60);
            assertThat(RedisRateLimiterServiceImpl.GUEST_LIMIT).isEqualTo(10);
            assertThat(RedisRateLimiterServiceImpl.USER_LIMIT).isEqualTo(10);
        }
    }
}
