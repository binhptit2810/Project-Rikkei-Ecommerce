package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.security.ClientIpResolver;
import com.example.ecommerce.chat.service.impl.RedisRateLimiterServiceImpl;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test thực tế cho RedisRateLimiterServiceImpl trên Redis thật sử dụng Bucket4j.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 *
 * Yêu cầu: Redis thật đang chạy tại localhost:6379.
 * Kích hoạt: Đặt biến môi trường RUN_REDIS_TESTS=true khi chạy gradle test.
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TESTS", matches = "true")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RedisRateLimiterIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, byte[]> redisConnection;
    private static RedisRateLimiterServiceImpl rateLimiterService;

    private String testIp;
    private String testUserId;

    @BeforeAll
    static void initRedis() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        redisClient = RedisClient.create("redis://localhost:6379");
        redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        ProxyManager<String> proxyManager = Bucket4jLettuce.casBasedBuilder(redisConnection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();

        ClientIpResolver clientIpResolver = new ClientIpResolver("127.0.0.1,::1");
        rateLimiterService = new RedisRateLimiterServiceImpl(proxyManager, clientIpResolver);
    }

    @AfterAll
    static void tearDownRedis() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        testIp = "it-ip-" + UUID.randomUUID();
        testUserId = "it-user-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanUp() {
        if (testIp != null) {
            redisTemplate.delete("chat:rate-limit:ip:{" + testIp + "}");
        }
    }

    @Test
    @DisplayName("IT-RL-01: Khách vãng lai gửi 10 request thành công, request thứ 11 bị chặn với 429 và Retry-After")
    void testGuestRateLimitingLive() {
        // 10 request đầu tiên thành công
        for (int i = 1; i <= 10; i++) {
            rateLimiterService.checkRateLimit(testIp, null);
        }

        // Request thứ 11 bị chặn
        assertThatThrownBy(() -> rateLimiterService.checkRateLimit(testIp, null))
                .isInstanceOf(ChatRateLimitExceededException.class)
                .satisfies(ex -> {
                    ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                    assertThat(rle.getRetryAfterSeconds()).isBetween(1L, 60L);
                });
    }

    @Test
    @DisplayName("IT-RL-02: Người dùng đăng nhập cùng IP chia sẻ hạn mức 10 req/phút: request 11 bị chặn (bỏ hạn mức 30 cũ)")
    void testUserRateLimitingLive_SharesTenLimit() {
        // Gửi 5 request dưới danh nghĩa Guest
        for (int i = 1; i <= 5; i++) {
            rateLimiterService.checkRateLimit(testIp, null);
        }

        // Gửi 5 request tiếp theo dưới danh nghĩa Authenticated User trên cùng IP
        for (int i = 6; i <= 10; i++) {
            rateLimiterService.checkRateLimit(testIp, testUserId);
        }

        // Request thứ 11 của User bị chặn (vì cùng IP đã chạm mốc 10)
        assertThatThrownBy(() -> rateLimiterService.checkRateLimit(testIp, testUserId))
                .isInstanceOf(ChatRateLimitExceededException.class)
                .satisfies(ex -> {
                    ChatRateLimitExceededException rle = (ChatRateLimitExceededException) ex;
                    assertThat(rle.getRetryAfterSeconds()).isBetween(1L, 60L);
                });
    }

    @Test
    @DisplayName("IT-RL-03: Hai IP khác nhau có bộ đếm Token Bucket độc lập")
    void testDifferentIpsIndependentCounters() {
        String ip1 = "it-ip1-" + UUID.randomUUID();
        String ip2 = "it-ip2-" + UUID.randomUUID();

        try {
            // IP1 dùng hết 10 lượt
            for (int i = 1; i <= 10; i++) {
                rateLimiterService.checkRateLimit(ip1, null);
            }

            // IP1 bị chặn ở lượt 11
            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(ip1, null))
                    .isInstanceOf(ChatRateLimitExceededException.class);

            // IP2 vẫn còn nguyên 10 lượt
            for (int i = 1; i <= 10; i++) {
                rateLimiterService.checkRateLimit(ip2, null);
            }
        } finally {
            redisTemplate.delete("chat:rate-limit:ip:{" + ip1 + "}");
            redisTemplate.delete("chat:rate-limit:ip:{" + ip2 + "}");
        }
    }

    @Test
    @DisplayName("IT-RL-04: Key rate limit trong Redis được gán TTL tự động")
    void testRealTtlInRedis() {
        rateLimiterService.checkRateLimit(testIp, null);

        Long ttlSeconds = redisTemplate.getExpire("chat:rate-limit:ip:{" + testIp + "}", TimeUnit.SECONDS);
        assertThat(ttlSeconds).isNotNull().isGreaterThan(0L);
    }

    @Test
    @DisplayName("IT-RL-05: Kiểm tra tính nguyên tử (Atomicity) khi 20 threads đồng thời gửi request: đúng 10 thành công, 10 bị từ chối")
    void testConcurrencyAndAtomicity() throws Exception {
        int totalThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    rateLimiterService.checkRateLimit(testIp, null);
                    successCount.incrementAndGet();
                } catch (ChatRateLimitExceededException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Với giới hạn 10 tokens: đúng 10 thành công và đúng 10 bị từ chối
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(rejectedCount.get()).isEqualTo(10);
    }
}
