package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.impl.RedisChatMemoryServiceImpl;
import com.example.ecommerce.exception.ChatLockContentionException;
import com.example.ecommerce.exception.ChatVersionConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho RedisChatMemoryServiceImpl (Phase 2).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@ExtendWith(MockitoExtension.class)
class RedisChatMemoryServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private RedisChatMemoryServiceImpl service;

    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_LOCK_TOKEN = "11111111-2222-3333-4444-555555555555";
    private static final String TEST_GUEST_TOKEN = "test-guest-token-32bytes-secret-123456789";

    @BeforeEach
    void setUp() {
        // Reset or prepare common fixtures if needed
    }

    // =========================================================================
    // 1. TOKEN & HASH & MODEL UNIT TESTS
    // =========================================================================

    @Nested
    @DisplayName("1. Token, Hash & Internal Model Tests")
    class TokenAndModelTests {

        @Test
        @DisplayName("UT-01: Sinh guest token đúng 32 bytes entropy, Base64 URL-safe, unpadded")
        void testGuestTokenFormat() {
            when(redisTemplate.execute(any(), anyList(), any(), any(), any())).thenReturn(1L);

            GuestSessionCreationResult result = service.createGuestSession();

            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isNotBlank();
            UUID.fromString(result.getSessionId()); // Không ném ngoại lệ -> đúng chuẩn UUID

            String rawToken = result.getRawGuestToken();
            assertThat(rawToken).isNotBlank();
            assertThat(rawToken).doesNotContain("=").doesNotContain("+").doesNotContain("/");

            byte[] decoded = Base64.getUrlDecoder().decode(rawToken);
            assertThat(decoded).hasSize(32); // Đúng 32 bytes entropy
        }

        @Test
        @DisplayName("UT-02: Băm SHA-256 chính xác và đồng nhất chuỗi hex 64 ký tự")
        void testSha256Hex() {
            String input = "sample-secret-token";
            String hash1 = RedisChatMemoryServiceImpl.sha256Hex(input);
            String hash2 = RedisChatMemoryServiceImpl.sha256Hex(input);

            assertThat(hash1).isNotNull().hasSize(64);
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).matches("^[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("UT-03: GuestSessionCreationResult & ChatMemoryData bao phủ getter, setter, builder")
        void testInternalModelsCoverage() {
            ChatSessionMetadata metadata = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            GuestSessionCreationResult result = GuestSessionCreationResult.builder()
                    .sessionId(TEST_SESSION_ID)
                    .rawGuestToken("raw-token")
                    .metadata(metadata)
                    .build();

            assertThat(result.getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(result.getRawGuestToken()).isEqualTo("raw-token");
            assertThat(result.getMetadata()).isEqualTo(metadata);

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(metadata)
                    .messages(List.of(ChatMessage.ofUser("Hi")))
                    .build();

            assertThat(memoryData.getMetadata()).isEqualTo(metadata);
            assertThat(memoryData.getMessages()).hasSize(1);
        }

        @Test
        @DisplayName("UT-03b: Kiểm tra build các Redis Key chuẩn hash tag {sessionId}")
        void testBuildRedisKeys() {
            assertThat(RedisChatMemoryServiceImpl.buildSessionKey(TEST_SESSION_ID))
                    .isEqualTo("chat:session:{" + TEST_SESSION_ID + "}");
            assertThat(RedisChatMemoryServiceImpl.buildMemoryKey(TEST_SESSION_ID))
                    .isEqualTo("chat:memory:{" + TEST_SESSION_ID + "}");
            assertThat(RedisChatMemoryServiceImpl.buildLockKey(TEST_SESSION_ID))
                    .isEqualTo("chat:lock:{" + TEST_SESSION_ID + "}");
            assertThat(RedisChatMemoryServiceImpl.buildGuestTokenKey(TEST_SESSION_ID))
                    .isEqualTo("chat:guest-token:{" + TEST_SESSION_ID + "}");
        }
    }

    // =========================================================================
    // 2. GUEST SESSION CREATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("2. Guest Session Creation Tests")
    class GuestSessionCreationTests {

        @Test
        @DisplayName("UT-04: Tạo Guest session thành công khi Lua script trả về 1")
        void testCreateGuestSession_Success() {
            when(redisTemplate.execute(any(), anyList(), any(), any(), any()))
                    .thenReturn(1L);

            GuestSessionCreationResult result = service.createGuestSession();

            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isNotBlank();
            assertThat(result.getRawGuestToken()).isNotBlank();
            assertThat(result.getMetadata().getOwnerId()).isEqualTo(RedisChatMemoryServiceImpl.OWNER_GUEST);
            assertThat(result.getMetadata().getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("UT-05: Key collision khi tạo session, retry thành công ở lượt tiếp theo")
        void testCreateGuestSession_RetryOnCollision() {
            when(redisTemplate.execute(any(), anyList(), any(), any(), any()))
                    .thenReturn(-999L) // Lượt 1: va chạm
                    .thenReturn(1L);   // Lượt 2: thành công

            GuestSessionCreationResult result = service.createGuestSession();

            assertThat(result).isNotNull();
            verify(redisTemplate, times(2)).execute(any(), anyList(), any(), any(), any());
        }

        @Test
        @DisplayName("UT-06: Ném IllegalStateException khi va chạm khóa vượt quá số lần retry")
        void testCreateGuestSession_ExhaustRetries() {
            when(redisTemplate.execute(any(), anyList(), any(), any(), any()))
                    .thenReturn(-999L);

            assertThatThrownBy(() -> service.createGuestSession())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("va chạm khóa liên tục");
        }

        @Test
        @DisplayName("UT-06b: Ném IllegalStateException khi serialize metadata thất bại")
        void testCreateGuestSession_SerializationException() throws Exception {
            doThrow(mock(JsonProcessingException.class))
                    .when(objectMapper).writeValueAsString(any(ChatSessionMetadata.class));

            assertThatThrownBy(() -> service.createGuestSession())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Lỗi tuần tự hóa metadata phiên trò chuyện");
        }
    }

    // =========================================================================
    // 3. READ CHAT MEMORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("3. Read Chat Memory Tests")
    class ReadChatMemoryTests {

        @Test
        @DisplayName("UT-07: Đọc session Guest hợp lệ khi token hash khớp")
        void testGetChatMemory_GuestSuccess() throws Exception {
            ChatSessionMetadata meta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(2L)
                    .build();
            String metaJson = objectMapper.writeValueAsString(meta);

            ChatMessage msg1 = ChatMessage.ofUser("Xin chào");
            ChatMessage msg2 = ChatMessage.ofAssistant("Dạ chào bạn");
            List<String> rawMessages = List.of(
                    objectMapper.writeValueAsString(msg1),
                    objectMapper.writeValueAsString(msg2)
            );

            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(1L, metaJson, rawMessages));

            ChatMemoryData data = service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null);

            assertThat(data).isNotNull();
            assertThat(data.getMetadata().getVersion()).isEqualTo(2L);
            assertThat(data.getMessages()).hasSize(2);
            assertThat(data.getMessages().get(0).getContent()).isEqualTo("Xin chào");
        }

        @Test
        @DisplayName("UT-08: Đọc memory ném BadCredentialsException khi thiếu hoặc sai guest token")
        void testGetChatMemory_InvalidGuestToken() {
            // Thiếu token
            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, "", null))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Guest token bắt buộc");

            // Sai token trong Redis
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-11L, ""));

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, "wrong-token", null))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Guest token không hợp lệ");
        }

        @Test
        @DisplayName("UT-09: Đọc memory ném ResourceNotFoundException khi session không tồn tại (-1)")
        void testGetChatMemory_NotFound() {
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-1L, ""));

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("không tồn tại hoặc đã hết hạn");
        }

        @Test
        @DisplayName("UT-10: Đọc memory ném AccessDeniedException khi sai owner (-4)")
        void testGetChatMemory_ForbiddenOwner() {
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-4L, ""));

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, null, "user-999"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("không có quyền truy cập");
        }

        @Test
        @DisplayName("UT-10b: Đọc memory với session ID null hoặc rỗng ném IllegalArgumentException")
        void testGetChatMemory_BlankSessionId() {
            assertThatThrownBy(() -> service.getChatMemory("", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");

            assertThatThrownBy(() -> service.getChatMemory(null, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");
        }

        @Test
        @DisplayName("UT-10c: Đọc session của Authenticated User thành công")
        void testGetChatMemory_AuthenticatedUser_Success() throws Exception {
            ChatSessionMetadata meta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("user-101")
                    .version(3L)
                    .build();
            String metaJson = objectMapper.writeValueAsString(meta);

            when(redisTemplate.execute(any(), anyList(), eq("user-101"), eq("")))
                    .thenReturn(List.of(1L, metaJson, List.of()));

            ChatMemoryData data = service.getChatMemory(TEST_SESSION_ID, null, "user-101");

            assertThat(data).isNotNull();
            assertThat(data.getMetadata().getOwnerId()).isEqualTo("user-101");
            assertThat(data.getMessages()).isEmpty();
        }

        @Test
        @DisplayName("UT-10d: Đọc memory ném ResourceNotFoundException khi Redis trả về null hoặc rỗng")
        void testGetChatMemory_EmptyResult() {
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(null);

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("không tồn tại hoặc đã hết hạn");

            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("không tồn tại hoặc đã hết hạn");
        }

        @Test
        @DisplayName("UT-10e: Đọc memory ném IllegalStateException khi Redis trả về mã lỗi -2, -6 hoặc mã lạ")
        void testGetChatMemory_ErrorStatuses() {
            // status -2
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-2L, ""));
            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cấu trúc dữ liệu Redis không hợp lệ");

            // status -6
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-6L, ""));
            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Metadata phiên trò chuyện trong Redis bị hỏng");

            // status lạ -999
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(-999L, ""));
            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Mã lỗi không xác định từ Redis Lua Script: -999");
        }

        @Test
        @DisplayName("UT-10f: Đọc memory ném IllegalStateException khi JSON metadata hoặc message bị hỏng")
        void testGetChatMemory_CorruptedJson() throws Exception {
            // Metadata bị hỏng JSON
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(1L, "not-a-valid-json", List.of()));

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Metadata phiên trò chuyện trong Redis bị hỏng hoặc sai định dạng JSON");

            // Message bị hỏng JSON
            ChatSessionMetadata meta = ChatSessionMetadata.builder().sessionId(TEST_SESSION_ID).ownerId("GUEST").version(1L).build();
            String metaJson = objectMapper.writeValueAsString(meta);
            when(redisTemplate.execute(any(), anyList(), any(), any()))
                    .thenReturn(List.of(1L, metaJson, List.of("invalid-message-json")));

            assertThatThrownBy(() -> service.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Dữ liệu tin nhắn trong Redis bị hỏng");
        }
    }

    // =========================================================================
    // 4. DISTRIBUTED LOCK TESTS
    // =========================================================================

    @Nested
    @DisplayName("4. Distributed Lock Tests")
    class DistributedLockTests {

        @Test
        @DisplayName("UT-11: Chiếm lock thành công khi SET NX trả về true")
        void testAcquireLock_Success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), eq(45000L), eq(TimeUnit.MILLISECONDS)))
                    .thenReturn(Boolean.TRUE);

            String lockToken = service.acquireLock(TEST_SESSION_ID);

            assertThat(lockToken).isNotBlank();
            UUID.fromString(lockToken); // Validate UUID
        }

        @Test
        @DisplayName("UT-12: Ném ChatLockContentionException (HTTP 429) khi SET NX trả về false")
        void testAcquireLock_Contention() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), eq(45000L), eq(TimeUnit.MILLISECONDS)))
                    .thenReturn(Boolean.FALSE);

            assertThatThrownBy(() -> service.acquireLock(TEST_SESSION_ID))
                    .isInstanceOf(ChatLockContentionException.class)
                    .hasMessageContaining("đang xử lý yêu cầu trước");
        }

        @Test
        @DisplayName("UT-13: Giải phóng lock an toàn qua Compare-and-Delete")
        void testReleaseLock_Safe() {
            when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

            service.releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);

            verify(redisTemplate).execute(any(), eq(List.of("chat:lock:{" + TEST_SESSION_ID + "}")), eq(TEST_LOCK_TOKEN));
        }

        @Test
        @DisplayName("UT-13b: Acquire lock với sessionId null hoặc rỗng ném IllegalArgumentException")
        void testAcquireLock_BlankSessionId() {
            assertThatThrownBy(() -> service.acquireLock(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");

            assertThatThrownBy(() -> service.acquireLock(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");
        }

        @Test
        @DisplayName("UT-13c: Acquire lock ném RedisSystemException khi Redis gặp lỗi hạ tầng")
        void testAcquireLock_RedisException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenThrow(new RuntimeException("Redis connection refused"));

            assertThatThrownBy(() -> service.acquireLock(TEST_SESSION_ID))
                    .isInstanceOf(RedisSystemException.class)
                    .hasMessageContaining("Lỗi kết nối Redis khi acquire lock");
        }

        @Test
        @DisplayName("UT-13d: Release lock an toàn khi tham số null/rỗng hoặc Redis ném exception không làm crash hệ thống")
        void testReleaseLock_EdgeCases() {
            // Tham số rỗng/null
            service.releaseLock("", TEST_LOCK_TOKEN);
            service.releaseLock(TEST_SESSION_ID, "");
            service.releaseLock(null, TEST_LOCK_TOKEN);
            service.releaseLock(TEST_SESSION_ID, null);

            verify(redisTemplate, never()).execute(any(), anyList(), any());

            // Redis execute ném lỗi
            when(redisTemplate.execute(any(), anyList(), any()))
                    .thenThrow(new RuntimeException("Redis timeout during release"));

            // Không ném exception ra ngoài
            service.releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }
    }

    // =========================================================================
    // 5. SAVE CHAT MEMORY TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. Save Chat Memory & OCC Tests")
    class SaveChatMemoryTests {

        @Test
        @DisplayName("UT-14: Ghi memory thành công, tăng version và cập nhật metadata")
        void testSaveChatMemory_Success() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi giá");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("150k");

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(2L); // newVersion = 2

            ChatSessionMetadata updated = service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    1L,
                    userMsg,
                    assistantMsg,
                    TEST_GUEST_TOKEN,
                    null
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getVersion()).isEqualTo(2L);
            assertThat(updated.getOwnerId()).isEqualTo("GUEST");
        }

        @Test
        @DisplayName("UT-15: Ghi memory ném ChatVersionConflictException (409) khi OCC version sai (-3)")
        void testSaveChatMemory_OccConflict() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi giá");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("150k");

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-3L);

            assertThatThrownBy(() -> service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    1L,
                    userMsg,
                    assistantMsg,
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(ChatVersionConflictException.class)
                    .hasMessageContaining("OCC conflict");
        }

        @Test
        @DisplayName("UT-16: Ghi memory ném ChatLockContentionException (429) khi lock token hết hạn hoặc sai (-101, -102)")
        void testSaveChatMemory_LockExpiredOrMismatch() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi giá");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("150k");

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-101L);

            assertThatThrownBy(() -> service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    1L,
                    userMsg,
                    assistantMsg,
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(ChatLockContentionException.class)
                    .hasMessageContaining("Distributed Lock đã hết hạn");
        }

        @Test
        @DisplayName("UT-17: Từ chối ghi khi danh sách tin nhắn bị lẻ hoặc sai cấu trúc (-5)")
        void testSaveChatMemory_CorruptedMemory() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi giá");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("150k");

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-5L);

            assertThatThrownBy(() -> service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    1L,
                    userMsg,
                    assistantMsg,
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("lỗi cấu trúc lẻ tin nhắn");
        }

        @Test
        @DisplayName("UT-18: Kiểm tra validation tin nhắn trước khi gọi Redis: sai role hoặc nội dung trống")
        void testSaveChatMemory_MessageValidation() {
            ChatMessage invalidRoleMsg = ChatMessage.builder().role("invalid").content("Alo").build();
            ChatMessage validAssistant = ChatMessage.ofAssistant("Dạ");

            assertThatThrownBy(() -> service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    1L,
                    invalidRoleMsg,
                    validAssistant,
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("yêu cầu role='user'");
        }

        @Test
        @DisplayName("UT-18b: Kiểm tra toàn diện validation các tham số đầu vào của saveChatMemory")
        void testSaveChatMemory_ValidationAllBranches() {
            ChatMessage validUser = ChatMessage.ofUser("Chào bạn");
            ChatMessage validAssistant = ChatMessage.ofAssistant("Chào quý khách");

            // SessionId rỗng
            assertThatThrownBy(() -> service.saveChatMemory("", TEST_LOCK_TOKEN, 1L, validUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Session ID không được để trống");

            // LockToken rỗng
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, "", 1L, validUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Lock token không được để trống");

            // ExpectedVersion null hoặc < 1
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, null, validUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected version không hợp lệ");
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 0L, validUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Expected version không hợp lệ");

            // User message null
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, null, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tin nhắn người dùng không hợp lệ");

            // User message content rỗng
            ChatMessage emptyContentUser = ChatMessage.builder().role("user").content("   ").build();
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, emptyContentUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Nội dung tin nhắn người dùng không được để trống");

            // User message length > 1000
            ChatMessage longContentUser = ChatMessage.builder().role("user").content("a".repeat(1001)).build();
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, longContentUser, validAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vượt quá 1000 ký tự");

            // Assistant message null
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, validUser, null, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tin nhắn trợ lý không hợp lệ");

            // Assistant message role sai
            ChatMessage wrongRoleAssistant = ChatMessage.builder().role("bot").content("Trả lời").build();
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, validUser, wrongRoleAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("yêu cầu role='assistant'");

            // Assistant message content rỗng
            ChatMessage emptyContentAssistant = ChatMessage.builder().role("assistant").content("").build();
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, validUser, emptyContentAssistant, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Nội dung tin nhắn trợ lý ảo không được để trống");

            // Guest thiếu token
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, validUser, validAssistant, "", null))
                    .isInstanceOf(BadCredentialsException.class).hasMessageContaining("Guest token bắt buộc");
        }

        @Test
        @DisplayName("UT-18c: Ghi memory cho Authenticated User thành công")
        void testSaveChatMemory_AuthenticatedUser_Success() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi giá");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("150k");

            when(redisTemplate.execute(any(), anyList(), any(), any(), eq("user-101"), eq(""), any(), any(), any(), any(), eq(String.valueOf(RedisChatMemoryServiceImpl.USER_TTL_SECONDS))))
                    .thenReturn(5L);

            ChatSessionMetadata updated = service.saveChatMemory(
                    TEST_SESSION_ID,
                    TEST_LOCK_TOKEN,
                    4L,
                    userMsg,
                    assistantMsg,
                    null,
                    "user-101"
            );

            assertThat(updated).isNotNull();
            assertThat(updated.getVersion()).isEqualTo(5L);
            assertThat(updated.getOwnerId()).isEqualTo("user-101");
        }

        @Test
        @DisplayName("UT-18d: Xử lý các mã lỗi Redis khi saveChatMemory (-1, -2, -4, -6, -7, -11, -102, null, lạ)")
        void testSaveChatMemory_AllErrorStatuses() {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("Đáp");

            // null result
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(null);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Không nhận được phản hồi");

            // -102 lock mismatch
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-102L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ChatLockContentionException.class).hasMessageContaining("bị chiếm bởi một tiến trình khác");

            // -1 not found
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-1L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("không tồn tại hoặc đã hết hạn");

            // -2 invalid structure
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-2L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cấu trúc dữ liệu Redis không hợp lệ");

            // -4 access denied
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-4L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(AccessDeniedException.class).hasMessageContaining("không có quyền cập nhật");

            // -6 corrupted metadata
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-6L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Metadata phiên trò chuyện trong Redis bị hỏng");

            // -7 version overflow
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-7L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("giới hạn an toàn tối đa");

            // -11 bad token
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-11L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(BadCredentialsException.class).hasMessageContaining("Guest token không hợp lệ");

            // -999 unknown
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-999L);
            assertThatThrownBy(() -> service.saveChatMemory(TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Mã lỗi không xác định từ Redis Lua Script: -999");
        }

        @Test
        @DisplayName("UT-18e: Ném IllegalStateException khi serialize ChatMessage thất bại trong saveChatMemory")
        void testSaveChatMemory_SerializationException() throws Exception {
            ChatMessage userMsg = ChatMessage.ofUser("Hỏi");
            ChatMessage assistantMsg = ChatMessage.ofAssistant("Đáp");

            doThrow(mock(JsonProcessingException.class))
                    .when(objectMapper).writeValueAsString(any(ChatMessage.class));

            assertThatThrownBy(() -> service.saveChatMemory(
                    TEST_SESSION_ID, TEST_LOCK_TOKEN, 1L, userMsg, assistantMsg, TEST_GUEST_TOKEN, null
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Lỗi tuần tự hóa tin nhắn trò chuyện");
        }
    }

    // =========================================================================
    // 6. CLAIM GUEST SESSION TESTS
    // =========================================================================

    @Nested
    @DisplayName("6. Claim Guest Session Tests")
    class ClaimGuestSessionTests {

        @Test
        @DisplayName("UT-19: Claim guest session thành công, chuyển owner sang userId và tăng version")
        void testClaimGuestSession_Success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(Boolean.TRUE);

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(2L); // newVersion

            ChatSessionMetadata claimed = service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "1001");

            assertThat(claimed).isNotNull();
            assertThat(claimed.getOwnerId()).isEqualTo("1001");
            assertThat(claimed.getVersion()).isEqualTo(2L);

            // Xác minh lock được giải phóng trong finally
            verify(redisTemplate).execute(any(), eq(List.of("chat:lock:{" + TEST_SESSION_ID + "}")), anyString());
        }

        @Test
        @DisplayName("UT-20: Claim ném AccessDeniedException khi session đã thuộc User khác (-10)")
        void testClaimGuestSession_AlreadyClaimed() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(Boolean.TRUE);

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-10L);

            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "1001"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("không phải phiên khách");
        }

        @Test
        @DisplayName("UT-21: Claim ném BadCredentialsException khi guest token không khớp (-11)")
        void testClaimGuestSession_BadToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(Boolean.TRUE);

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-11L);

            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, "wrong-token", "1001"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Guest token không hợp lệ");
        }

        @Test
        @DisplayName("UT-22: Xử lý version overflow (-7) khi chạm ngưỡng số học an toàn")
        void testClaimGuestSession_VersionOverflow() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(Boolean.TRUE);

            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(-7L);

            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "1001"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("giới hạn an toàn tối đa");
        }

        @Test
        @DisplayName("UT-22b: Kiểm tra validation tham số đầu vào của claimGuestSession")
        void testClaimGuestSession_BlankParameters() {
            assertThatThrownBy(() -> service.claimGuestSession("", TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Session ID không được để trống");

            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, "", "user-101"))
                    .isInstanceOf(BadCredentialsException.class).hasMessageContaining("Guest token không được để trống");

            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, ""))
                    .isInstanceOf(AccessDeniedException.class).hasMessageContaining("Yêu cầu thông tin người dùng đã xác thực");
        }

        @Test
        @DisplayName("UT-22c: Xử lý các mã lỗi Redis khi claimGuestSession (-1, -2, -6, -101, -102, null, lạ)")
        void testClaimGuestSession_ErrorStatuses() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(Boolean.TRUE);

            // null result
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any())).thenReturn(null);
            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Không nhận được phản hồi");

            // -101 / -102 lock contention
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any())).thenReturn(-101L);
            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(ChatLockContentionException.class).hasMessageContaining("Distributed Lock không hợp lệ");

            // -1 not found
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any())).thenReturn(-1L);
            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("không tồn tại hoặc đã hết hạn");

            // -2 / -6 invalid structure
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any())).thenReturn(-2L);
            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cấu trúc dữ liệu Redis không hợp lệ");

            // -999 unknown
            when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any(), any(), any())).thenReturn(-999L);
            assertThatThrownBy(() -> service.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, "user-101"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Mã lỗi không xác định từ Redis Lua Script: -999");
        }
    }
}
