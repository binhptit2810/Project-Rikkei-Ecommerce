package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.impl.RedisChatMemoryServiceImpl;
import com.example.ecommerce.exception.ChatLockContentionException;
import com.example.ecommerce.exception.ChatVersionConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import com.example.ecommerce.exception.ChatAuthenticationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test chạy trên Redis thực tế (Redis 7 Standalone).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Kiểm soát kích hoạt: Chỉ chạy khi biến môi trường RUN_REDIS_TESTS=true
 * để đảm bảo an toàn cho môi trường CI/CD không có container Redis sẵn sàng.
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TESTS", matches = "true")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RedisChatMemoryIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisChatMemoryServiceImpl service;
    private static ObjectMapper objectMapper;

    private final List<String> sessionIdsToClean = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new RedisChatMemoryServiceImpl(redisTemplate, objectMapper);
    }

    @AfterAll
    static void afterAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Redis server không khả dụng trên localhost:6379: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        for (String sessionId : sessionIdsToClean) {
            redisTemplate.delete(List.of(
                    RedisChatMemoryServiceImpl.buildSessionKey(sessionId),
                    RedisChatMemoryServiceImpl.buildMemoryKey(sessionId),
                    RedisChatMemoryServiceImpl.buildLockKey(sessionId),
                    RedisChatMemoryServiceImpl.buildGuestTokenKey(sessionId)
            ));
        }
        sessionIdsToClean.clear();
    }

    @Test
    @DisplayName("IT-01: Tạo guest session và kiểm tra TTL thực tế trong Redis (~86400s)")
    void testCreateGuestSession_Ttl() {
        GuestSessionCreationResult result = service.createGuestSession();
        String sessionId = result.getSessionId();
        sessionIdsToClean.add(sessionId);

        String sessionKey = RedisChatMemoryServiceImpl.buildSessionKey(sessionId);
        String tokenKey = RedisChatMemoryServiceImpl.buildGuestTokenKey(sessionId);

        Long sessionTtl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
        Long tokenTtl = redisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);

        assertThat(sessionTtl).isNotNull().isGreaterThan(86000L).isLessThanOrEqualTo(86400L);
        assertThat(tokenTtl).isNotNull().isGreaterThan(86000L).isLessThanOrEqualTo(86400L);
    }

    @Test
    @DisplayName("IT-02: Lưu và đọc memory thực tế từ Redis")
    void testSaveAndGetMemory() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        String lockToken = service.acquireLock(sessionId);
        try {
            ChatMessage userMsg = ChatMessage.ofUser("Shop có áo sơ mi trắng không?");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("Dạ Rikkei Store có sẵn áo sơ mi trắng size S, M, L ạ.");

            ChatSessionMetadata saved = service.saveChatMemory(
                    sessionId, lockToken, 1L, userMsg, assistantMsg, session.getRawGuestToken(), null
            );
            assertThat(saved.getVersion()).isEqualTo(2L);
        } finally {
            service.releaseLock(sessionId, lockToken);
        }

        ChatMemoryData memoryData = service.getChatMemory(sessionId, session.getRawGuestToken(), null);
        assertThat(memoryData.getMetadata().getVersion()).isEqualTo(2L);
        assertThat(memoryData.getMessages()).hasSize(2);
        assertThat(memoryData.getMessages().get(0).getContent()).contains("áo sơ mi trắng");
        assertThat(memoryData.getMessages().get(1).getContent()).contains("Rikkei Store");
    }

    @Test
    @DisplayName("IT-03: Trim 12 cặp tin nhắn (24 messages) xuống đúng 10 cặp (20 messages) bảo toàn cặp")
    void testTrim12PairsTo10Pairs() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        long currentVersion = 1L;
        for (int i = 1; i <= 12; i++) {
            String lockToken = service.acquireLock(sessionId);
            try {
                ChatMessage userMsg = ChatMessage.ofUser("Câu hỏi " + i);
                ChatMessage assistantMsg = ChatMessage.ofAssistant("Câu trả lời " + i);

                ChatSessionMetadata saved = service.saveChatMemory(
                        sessionId, lockToken, currentVersion, userMsg, assistantMsg, session.getRawGuestToken(), null
                );
                currentVersion = saved.getVersion();
            } finally {
                service.releaseLock(sessionId, lockToken);
            }
        }

        assertThat(currentVersion).isEqualTo(13L);

        ChatMemoryData memoryData = service.getChatMemory(sessionId, session.getRawGuestToken(), null);
        List<ChatMessage> messages = memoryData.getMessages();

        assertThat(messages).hasSize(20); // Giữ đúng 20 messages (10 cặp)
        // Cặp 1 và cặp 2 đã bị loại bỏ, tin nhắn đầu tiên phải là "Câu hỏi 3"
        assertThat(messages.get(0).getContent()).isEqualTo("Câu hỏi 3");
        assertThat(messages.get(1).getContent()).isEqualTo("Câu trả lời 3");
        // Tin nhắn cuối cùng phải là "Câu trả lời 12"
        assertThat(messages.get(19).getContent()).isEqualTo("Câu trả lời 12");
    }

    @Test
    @DisplayName("IT-04: Từ chối truy cập khi guest token sai")
    void testInvalidTokenRejected() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        assertThatThrownBy(() -> service.getChatMemory(sessionId, "invalid-token", null))
                .isInstanceOf(ChatAuthenticationException.class)
                .hasMessageContaining("Guest token không hợp lệ");
    }

    @Test
    @DisplayName("IT-05: Kiểm tra Distributed Lock contention giữa hai tiến trình")
    void testLockContentionBetweenTwoRequests() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        String lock1 = service.acquireLock(sessionId);
        try {
            assertThatThrownBy(() -> service.acquireLock(sessionId))
                    .isInstanceOf(ChatLockContentionException.class)
                    .hasMessageContaining("đang xử lý yêu cầu trước");
        } finally {
            service.releaseLock(sessionId, lock1);
        }

        // Sau khi release, tiến trình khác chiếm lock thành công
        String lock2 = service.acquireLock(sessionId);
        assertThat(lock2).isNotBlank();
        service.releaseLock(sessionId, lock2);
    }

    @Test
    @DisplayName("IT-06: Kiểm tra OCC conflict khi sử dụng version cũ")
    void testOccConflictWithStaleVersion() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        String lockToken1 = service.acquireLock(sessionId);
        try {
            service.saveChatMemory(
                    sessionId, lockToken1, 1L,
                    ChatMessage.ofUser("Q1"), ChatMessage.ofAssistant("A1"),
                    session.getRawGuestToken(), null
            );
        } finally {
            service.releaseLock(sessionId, lockToken1);
        }

        // Thao tác 2 cố tình gửi expectedVersion = 1L (trong khi version hiện tại đã là 2L)
        String lockToken2 = service.acquireLock(sessionId);
        try {
            assertThatThrownBy(() -> service.saveChatMemory(
                    sessionId, lockToken2, 1L,
                    ChatMessage.ofUser("Q2"), ChatMessage.ofAssistant("A2"),
                    session.getRawGuestToken(), null
            )).isInstanceOf(ChatVersionConflictException.class)
                    .hasMessageContaining("OCC conflict");
        } finally {
            service.releaseLock(sessionId, lockToken2);
        }
    }

    @Test
    @DisplayName("IT-07: Hai user đồng thời claim cùng session: đúng 1 user thành công")
    void testTwoUsersClaimingSameSessionConcurrently() throws Exception {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<Boolean> successList = new CopyOnWriteArrayList<>();
        List<Exception> exceptionList = new CopyOnWriteArrayList<>();

        Consumer<String> claimTask = (userId) -> {
            try {
                startLatch.await();
                service.claimGuestSession(sessionId, session.getRawGuestToken(), userId);
                successList.add(true);
            } catch (Exception e) {
                exceptionList.add(e);
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(() -> claimTask.accept("user-101"));
        executor.submit(() -> claimTask.accept("user-102"));

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Đúng 1 tiến trình thành công
        assertThat(successList).hasSize(1);
        // Tiến trình kia gặp lỗi (AccessDeniedException hoặc ChatLockContentionException)
        assertThat(exceptionList).hasSize(1);
    }

    @Test
    @DisplayName("IT-08: Claim session khi chưa có memory vẫn thành công")
    void testClaimSessionWithoutMemory() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        ChatSessionMetadata claimed = service.claimGuestSession(sessionId, session.getRawGuestToken(), "user-555");

        assertThat(claimed.getOwnerId()).isEqualTo("user-555");
        assertThat(claimed.getVersion()).isEqualTo(2L);

        // Guest token bị xóa vĩnh viễn
        String tokenKey = RedisChatMemoryServiceImpl.buildGuestTokenKey(sessionId);
        assertThat(redisTemplate.hasKey(tokenKey)).isFalse();
    }

    @Test
    @DisplayName("IT-09: Kiểm tra TTL sau khi claim chuyển sang User (~604800s - 7 ngày)")
    void testTtlAfterClaim() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        service.claimGuestSession(sessionId, session.getRawGuestToken(), "user-888");

        String sessionKey = RedisChatMemoryServiceImpl.buildSessionKey(sessionId);
        Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);

        assertThat(ttl).isNotNull().isGreaterThan(600000L).isLessThanOrEqualTo(604800L);
    }

    @Test
    @DisplayName("IT-10: Lua script từ chối và bảo toàn dữ liệu khi key sai Redis type")
    void testLuaScriptRejectsWrongKeyType() {
        String sessionId = UUID.randomUUID().toString();
        sessionIdsToClean.add(sessionId);

        String sessionKey = RedisChatMemoryServiceImpl.buildSessionKey(sessionId);
        // Cố tình tạo sessionKey dạng Set thay vì String
        redisTemplate.opsForSet().add(sessionKey, "corrupted-type");

        assertThatThrownBy(() -> service.getChatMemory(sessionId, "dummy", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sai kiểu");
    }

    @Test
    @DisplayName("IT-11: Dữ liệu không bị thay đổi khi validation thất bại (chống partial write)")
    void testDataIntegrityPreservedOnValidationFailure() {
        GuestSessionCreationResult session = service.createGuestSession();
        String sessionId = session.getSessionId();
        sessionIdsToClean.add(sessionId);

        String lockToken = service.acquireLock(sessionId);
        try {
            // Thử ghi với expectedVersion = 99L (sai OCC)
            assertThatThrownBy(() -> service.saveChatMemory(
                    sessionId, lockToken, 99L,
                    ChatMessage.ofUser("Q"), ChatMessage.ofAssistant("A"),
                    session.getRawGuestToken(), null
            )).isInstanceOf(ChatVersionConflictException.class);
        } finally {
            service.releaseLock(sessionId, lockToken);
        }

        // Xác nhận memory vẫn rỗng và version vẫn là 1
        ChatMemoryData memoryData = service.getChatMemory(sessionId, session.getRawGuestToken(), null);
        assertThat(memoryData.getMetadata().getVersion()).isEqualTo(1L);
        assertThat(memoryData.getMessages()).isEmpty();
    }

    @FunctionalInterface
    interface RunnableWithArg<T> {
        void run(T arg);
    }
}
