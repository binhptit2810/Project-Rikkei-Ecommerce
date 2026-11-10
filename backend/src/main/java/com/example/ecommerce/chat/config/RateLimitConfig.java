package com.example.ecommerce.chat.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;

/**
 * Cấu hình Distributed Rate Limiting sử dụng Bucket4j và Redis Lettuce CAS-based ProxyManager.
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT)
 */
@Configuration
@Slf4j
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration redisTimeout;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "rateLimitRedisClient")
    public RedisClient rateLimitRedisClient() {
        RedisURI redisUri = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withTimeout(redisTimeout != null ? redisTimeout : Duration.ofMillis(2000))
                .build();
        return RedisClient.create(redisUri);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProxyManager<String> bucketProxyManager(RedisClient rateLimitRedisClient) {
        return new LazyLettuceProxyManager(rateLimitRedisClient);
    }

    /**
     * Lazy ProxyManager wrapper: Chỉ kết nối Redis khi có request thực tế cần check rate limit.
     * Tránh làm chết Spring Context khi khởi động trong test environment (như @SpringBootTest)
     * đồng thời bảo đảm cơ chế Fail-Closed: ném RedisConnectionException khi Redis down để trả về HTTP 503.
     */
    private static class LazyLettuceProxyManager implements ProxyManager<String> {
        private final RedisClient redisClient;
        private volatile ProxyManager<String> delegate;
        private volatile StatefulRedisConnection<String, byte[]> connection;

        public LazyLettuceProxyManager(RedisClient redisClient) {
            this.redisClient = redisClient;
        }

        private ProxyManager<String> getDelegate() {
            if (delegate == null) {
                synchronized (this) {
                    if (delegate == null) {
                        log.info("Khởi tạo kết nối Lettuce CAS ProxyManager tới Redis...");
                        this.connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
                        this.delegate = Bucket4jLettuce.casBasedBuilder(connection)
                                .expirationAfterWrite(
                                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
                                )
                                .build();
                    }
                }
            }
            return delegate;
        }

        @Override
        public RemoteBucketBuilder<String> builder() {
            return getDelegate().builder();
        }

        @Override
        public Optional<BucketConfiguration> getProxyConfiguration(String key) {
            return getDelegate().getProxyConfiguration(key);
        }

        @Override
        public void removeProxy(String key) {
            getDelegate().removeProxy(key);
        }

        @Override
        public boolean isAsyncModeSupported() {
            return getDelegate().isAsyncModeSupported();
        }

        @Override
        public AsyncProxyManager<String> asAsync() {
            return getDelegate().asAsync();
        }
    }
}
