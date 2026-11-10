package com.example.ecommerce.chat.service;

import com.example.ecommerce.exception.ChatAuthenticationException;
import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatRequestContext;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.model.IdempotencyCheckResult;
import com.example.ecommerce.chat.service.impl.ChatServiceImpl;
import com.example.ecommerce.chat.tool.ChatToolsConfig;
import com.example.ecommerce.dto.response.ChatResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ChatLockContentionException;
import com.example.ecommerce.exception.ChatRateLimitExceededException;
import com.example.ecommerce.exception.ChatVersionConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho ChatServiceImpl với Spring AI ChatClient.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Spring AI, Tool Calling và RAG cho Chatbot tư vấn bán hàng.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatToolsConfig chatToolsConfig;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUpChatClient() {
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        lenient().when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        lenient().when(requestSpec.functions(any(FunctionCallback[].class))).thenReturn(requestSpec);
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_LOCK_TOKEN = "lock-token-12345";
    private static final String TEST_GUEST_TOKEN = "raw-guest-token-secret";
    private static final String TEST_USER_ID = "1001";

    @Nested
    @DisplayName("1. Guest Session Creation Tests")
    class GuestSessionCreationTests {

        @Test
        @DisplayName("CS-01: Khởi tạo phiên khách vãng lai gọi ChatMemoryService đúng 1 lần và trả kết quả chính xác")
        void testCreateGuestSession_Success() {
            ChatSessionMetadata metadata = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            GuestSessionCreationResult expectedResult = GuestSessionCreationResult.builder()
                    .sessionId(TEST_SESSION_ID)
                    .rawGuestToken(TEST_GUEST_TOKEN)
                    .metadata(metadata)
                    .build();

            when(chatMemoryService.createGuestSession()).thenReturn(expectedResult);

            GuestSessionCreationResult actual = chatService.createGuestSession();

            assertThat(actual).isNotNull();
            assertThat(actual.getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(actual.getRawGuestToken()).isEqualTo(TEST_GUEST_TOKEN);
            assertThat(actual.getMetadata().getVersion()).isEqualTo(1L);

            verify(chatMemoryService, times(1)).createGuestSession();
        }
    }

    @Nested
    @DisplayName("2. Send Message Tests - Success Flows")
    class SendMessageSuccessTests {

        @Test
        @DisplayName("CS-02: Khách vãng lai gửi tin nhắn thành công, Spring AI được truyền đúng System Prompt, hội thoại và ToolContext")
        void testSendMessage_GuestSuccess() {
            // Mock lock
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            // Mock memory chứa 2 tin nhắn cũ
            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            ChatMessage oldUserMsg = ChatMessage.ofUser("Xin chào shop");
            ChatMessage oldAssistantMsg = ChatMessage.ofAssistant("Dạ chào bạn!");
            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(List.of(oldUserMsg, oldAssistantMsg))
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null)).thenReturn(memoryData);

            // Mock tools cho Guest
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            // Mock Spring AI response
            when(callResponseSpec.content())
                    .thenReturn("Rikkei Store hiện có nhiều mẫu áo thun nam nữ cao cấp ạ!");

            // Mock save memory
            ChatSessionMetadata newMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(2L)
                    .build();

            when(chatMemoryService.saveChatMemory(
                    eq(TEST_SESSION_ID),
                    eq(TEST_LOCK_TOKEN),
                    eq(1L),
                    any(ChatMessage.class),
                    any(ChatMessage.class),
                    eq(TEST_GUEST_TOKEN),
                    isNull()
            )).thenReturn(newMeta);

            // Thực thi
            ChatResponse response = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Shop có bán áo thun không?",
                    TEST_GUEST_TOKEN,
                    null
            );

            // Xác minh kết quả trả về
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(response.getMessage()).isEqualTo("Rikkei Store hiện có nhiều mẫu áo thun nam nữ cao cấp ạ!");
            assertThat(response.getVersion()).isEqualTo(2L);

            // Xác minh danh sách messages truyền cho Spring AI
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(requestSpec).messages(messagesCaptor.capture());
            List<Message> springAiMessages = messagesCaptor.getValue();

            // Phải có đúng 4 tin nhắn: 1 System prompt + 2 cũ + 1 mới
            assertThat(springAiMessages).hasSize(4);
            assertThat(springAiMessages.get(0).getContent()).contains("Rikkei Ecommerce");
            assertThat(springAiMessages.get(1).getContent()).isEqualTo("Xin chào shop");
            assertThat(springAiMessages.get(2).getContent()).isEqualTo("Dạ chào bạn!");
            assertThat(springAiMessages.get(3).getContent()).isEqualTo("Shop có bán áo thun không?");

            // Xác minh toolContext chứa dữ liệu đúng
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
            verify(requestSpec).toolContext(contextCaptor.capture());
            Map<String, Object> capturedContext = contextCaptor.getValue();
            assertThat(capturedContext.get("isGuest")).isEqualTo(true);
            assertThat(capturedContext.get("sessionId")).isEqualTo(TEST_SESSION_ID);
            assertThat(capturedContext.get("guestToken")).isEqualTo(TEST_GUEST_TOKEN);

            // Xác minh giải phóng lock trong finally
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-03: Người dùng đã xác thực (User) gửi tin nhắn thành công, dùng all tools và không cần guest token")
        void testSendMessage_AuthenticatedUserSuccess() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId(TEST_USER_ID)
                    .version(5L)
                    .build();

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(Collections.emptyList())
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, null, TEST_USER_ID)).thenReturn(memoryData);
            when(chatToolsConfig.getAllTools()).thenReturn(new FunctionCallback[0]);

            when(callResponseSpec.content())
                    .thenReturn("Dạ đơn hàng đang được xử lý ạ!");

            ChatSessionMetadata newMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId(TEST_USER_ID)
                    .version(6L)
                    .build();

            when(chatMemoryService.saveChatMemory(
                    eq(TEST_SESSION_ID),
                    eq(TEST_LOCK_TOKEN),
                    eq(5L),
                    any(ChatMessage.class),
                    any(ChatMessage.class),
                    isNull(),
                    eq(TEST_USER_ID)
            )).thenReturn(newMeta);

            ChatResponse response = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Kiểm tra đơn hàng",
                    null,
                    TEST_USER_ID
            );

            assertThat(response).isNotNull();
            assertThat(response.getVersion()).isEqualTo(6L);
            verify(chatToolsConfig).getAllTools();
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }
    }

    @Nested
    @DisplayName("3. Send Message Tests - Validation & Error Handling")
    class SendMessageErrorTests {

        @Test
        @DisplayName("CS-04: Ném IllegalArgumentException khi sessionId hoặc message bị null/blank hoặc quá dài")
        void testSendMessage_InputValidation() {
            // Session ID null hoặc blank
            assertThatThrownBy(() -> chatService.sendMessage(null, "Alo", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");

            assertThatThrownBy(() -> chatService.sendMessage("   ", "Alo", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session ID không được để trống");

            // Message null hoặc blank
            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, null, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nội dung tin nhắn không được để trống");

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "   ", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nội dung tin nhắn không được để trống");

            // Message vượt quá 1000 ký tự
            String tooLongMessage = "x".repeat(1001);
            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, tooLongMessage, TEST_GUEST_TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("không được vượt quá 1000 ký tự");
        }

        @Test
        @DisplayName("CS-05: Khách vãng lai thiếu guest token ném ChatAuthenticationException ngay lập tức")
        void testSendMessage_GuestMissingToken() {
            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Alo", null, null))
                    .isInstanceOf(ChatAuthenticationException.class)
                    .hasMessageContaining("Guest token bắt buộc");

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Alo", "   ", null))
                    .isInstanceOf(ChatAuthenticationException.class)
                    .hasMessageContaining("Guest token bắt buộc");
        }

        @Test
        @DisplayName("CS-06: Lock Contention ném ChatLockContentionException, không gọi Spring AI và không release lock thừa")
        void testSendMessage_LockContention() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID))
                    .thenThrow(new ChatLockContentionException("Đang xử lý yêu cầu trước"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Hỏi giá", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ChatLockContentionException.class);

            verify(chatClient, never()).prompt();
            verify(chatMemoryService, never()).releaseLock(any(), any());
        }

        @Test
        @DisplayName("CS-07: Khi đọc memory bị lỗi (404/403), giải phóng lock trong finally và không gọi Spring AI")
        void testSendMessage_MemoryReadFailure_ReleasesLock() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenThrow(new ResourceNotFoundException("Session không tồn tại"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Hỏi giá", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(chatClient, never()).prompt();
            verify(chatMemoryService, never()).saveChatMemory(any(), any(), any(), any(), any(), any(), any());
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-08: Khi Spring AI upstream bị lỗi, giải phóng lock, KHÔNG lưu memory và ném GeminiServiceException")
        void testSendMessage_SpringAiFailure_PreservesHistoryAndReleasesLock() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(List.of(ChatMessage.ofUser("Câu cũ")))
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null)).thenReturn(memoryData);
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            // Spring AI upstream lỗi NonTransientAiException
            when(callResponseSpec.content())
                    .thenThrow(new NonTransientAiException("Lỗi upstream 503 từ Gemini"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu mới", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(GeminiServiceException.class);

            // Tuyệt đối không gọi saveChatMemory
            verify(chatMemoryService, never()).saveChatMemory(any(), any(), any(), any(), any(), any(), any());
            // Lock luôn được giải phóng
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-08B: Khi Spring AI timeout, ném GeminiTimeoutException và giải phóng lock an toàn")
        void testSendMessage_SpringAiTimeout_ThrowsGeminiTimeoutException() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(Collections.emptyList())
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null)).thenReturn(memoryData);
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            // Giả lập ResourceAccessException chứa SocketTimeoutException
            ResourceAccessException timeoutException = new ResourceAccessException(
                    "Request timed out", new SocketTimeoutException("Read timed out")
            );

            when(callResponseSpec.content())
                    .thenThrow(timeoutException);

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu hỏi", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(GeminiTimeoutException.class);

            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-09: Khi Spring AI trả về phản hồi null hoặc rỗng, ném GeminiServiceException và giải phóng lock")
        void testSendMessage_SpringAiBlankReply_ThrowsException() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(Collections.emptyList())
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null)).thenReturn(memoryData);
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            when(callResponseSpec.content())
                    .thenReturn("   ");

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu hỏi", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(GeminiServiceException.class)
                    .hasMessageContaining("không hợp lệ hoặc bị rỗng");

            verify(chatMemoryService, never()).saveChatMemory(any(), any(), any(), any(), any(), any(), any());
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-10: Khi ghi memory bị OCC conflict (409), ném ChatVersionConflictException và giải phóng lock an toàn")
        void testSendMessage_OccConflict_ReleasesLock() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();

            ChatMemoryData memoryData = ChatMemoryData.builder()
                    .metadata(oldMeta)
                    .messages(Collections.emptyList())
                    .build();

            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null)).thenReturn(memoryData);
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            when(callResponseSpec.content())
                    .thenReturn("Phản hồi AI");

            when(chatMemoryService.saveChatMemory(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new ChatVersionConflictException("OCC conflict"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu hỏi", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ChatVersionConflictException.class);

            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }
    }

    @Nested
    @DisplayName("4. Claim Session Tests")
    class ClaimSessionTests {

        @Test
        @DisplayName("CS-11: Chuyển giao phiên khách sang User gọi đúng ChatMemoryService")
        void testClaimSession_DelegatesToMemoryService() {
            ChatSessionMetadata updatedMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId(TEST_USER_ID)
                    .version(3L)
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(chatMemoryService.claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, TEST_USER_ID))
                    .thenReturn(updatedMeta);

            ChatSessionMetadata actual = chatService.claimSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, TEST_USER_ID);

            assertThat(actual).isNotNull();
            assertThat(actual.getOwnerId()).isEqualTo(TEST_USER_ID);
            assertThat(actual.getVersion()).isEqualTo(3L);

            verify(chatMemoryService).claimGuestSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, TEST_USER_ID);
        }
    }

    // =========================================================================
    // 5. IDEMPOTENCY & RATE LIMIT TESTS
    // =========================================================================

    @Nested
    @DisplayName("5. Idempotency & Rate Limit Tests")
    class IdempotencyAndRateLimitTests {

        @Test
        @DisplayName("CS-12: Idempotency HIT trả lại kết quả cached ChatResponse ngay lập tức (0ms, 0 Gemini call)")
        void testSendMessage_IdempotencyHit_ReturnsCachedImmediately() {
            ChatResponse cached = ChatResponse.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Kết quả đã cache")
                    .version(5L)
                    .build();

            IdempotencyCheckResult checkResult = IdempotencyCheckResult.completed(cached);
            when(chatMemoryService.checkAndClaimIdempotency(TEST_SESSION_ID, "client-msg-123", "Hỏi size áo"))
                    .thenReturn(checkResult);

            ChatResponse actual = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi size áo",
                    TEST_GUEST_TOKEN,
                    null,
                    "client-msg-123"
            );

            assertThat(actual).isSameAs(cached);
            verify(chatMemoryService, never()).acquireLock(anyString());
            verifyNoInteractions(chatClient);
        }

        @Test
        @DisplayName("CS-13: Idempotency IN_PROGRESS ném ChatLockContentionException")
        void testSendMessage_IdempotencyInProgress_ThrowsLockContention() {
            IdempotencyCheckResult checkResult = IdempotencyCheckResult.inProgress();
            when(chatMemoryService.checkAndClaimIdempotency(TEST_SESSION_ID, "client-msg-123", "Hỏi size áo"))
                    .thenReturn(checkResult);

            assertThatThrownBy(() -> chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi size áo",
                    TEST_GUEST_TOKEN,
                    null,
                    "client-msg-123"
            )).isInstanceOf(ChatLockContentionException.class)
              .hasMessageContaining("đang được xử lý");

            verify(chatMemoryService, never()).acquireLock(anyString());
        }

        @Test
        @DisplayName("CS-14: Idempotency MISMATCH ném BadRequestException")
        void testSendMessage_IdempotencyMismatch_ThrowsBadRequest() {
            IdempotencyCheckResult checkResult = IdempotencyCheckResult.mismatch();
            when(chatMemoryService.checkAndClaimIdempotency(TEST_SESSION_ID, "client-msg-123", "Hỏi giá áo"))
                    .thenReturn(checkResult);

            assertThatThrownBy(() -> chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi giá áo",
                    TEST_GUEST_TOKEN,
                    null,
                    "client-msg-123"
            )).isInstanceOf(BadRequestException.class)
              .hasMessageContaining("clientMessageId đã được sử dụng");
        }

        @Test
        @DisplayName("CS-15: Gửi tin nhắn thành công lưu IdempotencyResult vào Redis với TTL 300s")
        void testSendMessage_Success_SavesIdempotency() {
            when(chatMemoryService.checkAndClaimIdempotency(TEST_SESSION_ID, "client-msg-456", "Hỏi áo"))
                    .thenReturn(IdempotencyCheckResult.newRequest());
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);
            when(callResponseSpec.content()).thenReturn("Dạ áo còn hàng ạ!");

            ChatSessionMetadata newMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(2L)
                    .build();
            when(chatMemoryService.saveChatMemory(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(newMeta);

            ChatResponse response = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi áo",
                    TEST_GUEST_TOKEN,
                    null,
                    "client-msg-456"
            );

            assertThat(response).isNotNull();
            verify(chatMemoryService).saveIdempotencyResult(eq(TEST_SESSION_ID), eq("client-msg-456"), any(ChatResponse.class));
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-16: Khi có lỗi xảy ra, giải phóng lock và clearIdempotency để cho phép retry")
        void testSendMessage_Failure_ClearsIdempotency() {
            when(chatMemoryService.checkAndClaimIdempotency(TEST_SESSION_ID, "client-msg-789", "Hỏi áo"))
                    .thenReturn(IdempotencyCheckResult.newRequest());
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            when(callResponseSpec.content()).thenThrow(new RuntimeException("Gemini network error"));

            assertThatThrownBy(() -> chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi áo",
                    TEST_GUEST_TOKEN,
                    null,
                    "client-msg-789"
            )).isInstanceOf(RuntimeException.class);

            verify(chatMemoryService).clearIdempotency(TEST_SESSION_ID, "client-msg-789");
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-17: Gemini 429 Too Many Requests chuyển đổi thành ChatRateLimitExceededException kèm Retry-After")
        void testSendMessage_Gemini429_ThrowsChatRateLimitExceededException() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, "45");
            HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                    HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                    headers, "Quota exceeded".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
            );

            when(callResponseSpec.content()).thenThrow(tooManyRequests);

            assertThatThrownBy(() -> chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Hỏi giá",
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(ChatRateLimitExceededException.class)
              .hasMessageContaining("lượng yêu cầu lớn");

            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-18: Gemini thành công trả về cả message và danh sách products từ ChatRequestContext")
        void testSendMessage_Success_ReturnsProducts() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            ProductResponse p = ProductResponse.builder()
                    .id(1L)
                    .name("Áo thun")
                    .price(new BigDecimal("150000"))
                    .active(true)
                    .build();

            // Giả lập trong quá trình executeChatClientCall, tool đã ghi product vào ChatRequestContext
            when(callResponseSpec.content()).thenAnswer(invocation -> {
                ChatRequestContext ctx = ChatRequestContext.get();
                if (ctx != null) {
                    ctx.addReferencedProduct(p);
                }
                return "Dạ shop có áo thun này nhé!";
            });

            ChatSessionMetadata newMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(2L)
                    .build();
            when(chatMemoryService.saveChatMemory(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(newMeta);

            ChatResponse response = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Tìm áo thun",
                    TEST_GUEST_TOKEN,
                    null
            );

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Dạ shop có áo thun này nhé!");
            assertThat(response.getProducts()).hasSize(1);
            assertThat(response.getProducts().get(0).getId()).isEqualTo(1L);
            assertThat(response.getProducts().get(0).getName()).isEqualTo("Áo thun");
        }

        @Test
        @DisplayName("CS-19: Gemini 429 sau khi đã tìm được sản phẩm trả về fallback message và đính kèm products")
        void testSendMessage_Gemini429_WithProducts_Fallback() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            ProductResponse p = ProductResponse.builder()
                    .id(55L)
                    .name("Giày Sneaker")
                    .price(new BigDecimal("950000"))
                    .active(true)
                    .build();

            // Giả lập tool đã chạy và thêm product vào context, sau đó Gemini trả 429 ở turn cuối
            when(callResponseSpec.content()).thenAnswer(invocation -> {
                ChatRequestContext ctx = ChatRequestContext.get();
                if (ctx != null) {
                    ctx.addReferencedProduct(p);
                }
                throw HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        HttpHeaders.EMPTY, "Quota exceeded".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
                );
            });

            ChatSessionMetadata newMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(2L)
                    .build();
            when(chatMemoryService.saveChatMemory(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(newMeta);

            ChatResponse response = chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Tìm giày",
                    TEST_GUEST_TOKEN,
                    null
            );

            assertThat(response).isNotNull();
            assertThat(response.getMessage()).contains("quá tải");
            assertThat(response.getMessage()).contains("tìm thấy một số sản phẩm phù hợp");
            assertThat(response.getProducts()).hasSize(1);
            assertThat(response.getProducts().get(0).getId()).isEqualTo(55L);
            assertThat(response.getProducts().get(0).getName()).isEqualTo("Giày Sneaker");

            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-20: Lỗi không phải 429 (như 500 upstream) không bị biến thành fallback rate limit")
        void testSendMessage_Non429Error_NotFallback() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);

            ChatSessionMetadata oldMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .build();
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenReturn(ChatMemoryData.builder().metadata(oldMeta).messages(Collections.emptyList()).build());
            when(chatToolsConfig.getGuestTools()).thenReturn(new FunctionCallback[0]);

            when(callResponseSpec.content()).thenThrow(new RuntimeException("Upstream internal error"));

            assertThatThrownBy(() -> chatService.sendMessage(
                    TEST_SESSION_ID,
                    "Tìm áo",
                    TEST_GUEST_TOKEN,
                    null
            )).isInstanceOf(RuntimeException.class)
              .hasMessageNotContaining("quá tải");

            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }
    }
}
