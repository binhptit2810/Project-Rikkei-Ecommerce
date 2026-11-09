package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.client.GeminiRestClient;
import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.impl.ChatServiceImpl;
import com.example.ecommerce.dto.response.ChatResponse;
import com.example.ecommerce.exception.ChatLockContentionException;
import com.example.ecommerce.exception.ChatVersionConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho ChatServiceImpl.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMemoryService chatMemoryService;

    @Mock
    private GeminiRestClient geminiRestClient;

    @InjectMocks
    private ChatServiceImpl chatService;

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
        @DisplayName("CS-02: Khách vãng lai gửi tin nhắn thành công, Gemini được truyền đúng System Prompt và hội thoại")
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

            // Mock Gemini response
            when(geminiRestClient.generateChatCompletion(anyList()))
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

            // Xác minh danh sách messages truyền cho Gemini
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(geminiRestClient).generateChatCompletion(messagesCaptor.capture());
            List<ChatMessage> geminiMessages = messagesCaptor.getValue();

            // Phải có đúng 4 tin nhắn: 1 System prompt + 2 cũ + 1 mới
            assertThat(geminiMessages).hasSize(4);
            assertThat(geminiMessages.get(0).getRole()).isEqualTo("system");
            assertThat(geminiMessages.get(0).getContent()).contains("Rikkei Ecommerce");
            assertThat(geminiMessages.get(1).getContent()).isEqualTo("Xin chào shop");
            assertThat(geminiMessages.get(2).getContent()).isEqualTo("Dạ chào bạn!");
            assertThat(geminiMessages.get(3).getRole()).isEqualTo("user");
            assertThat(geminiMessages.get(3).getContent()).isEqualTo("Shop có bán áo thun không?");

            // Xác minh giải phóng lock trong finally
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-03: Người dùng đã xác thực (User) gửi tin nhắn thành công, không cần guest token")
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
            when(geminiRestClient.generateChatCompletion(anyList())).thenReturn("Dạ đơn hàng đang được xử lý ạ!");

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
        @DisplayName("CS-05: Khách vãng lai thiếu guest token ném BadCredentialsException ngay lập tức")
        void testSendMessage_GuestMissingToken() {
            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Alo", null, null))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Guest token bắt buộc");

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Alo", "   ", null))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Guest token bắt buộc");
        }

        @Test
        @DisplayName("CS-06: Lock Contention ném ChatLockContentionException, không gọi Gemini và không release lock thừa")
        void testSendMessage_LockContention() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID))
                    .thenThrow(new ChatLockContentionException("Đang xử lý yêu cầu trước"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Hỏi giá", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ChatLockContentionException.class);

            verify(geminiRestClient, never()).generateChatCompletion(any());
            verify(chatMemoryService, never()).releaseLock(any(), any());
        }

        @Test
        @DisplayName("CS-07: Khi đọc memory bị lỗi (404/403), giải phóng lock trong finally và không gọi Gemini")
        void testSendMessage_MemoryReadFailure_ReleasesLock() {
            when(chatMemoryService.acquireLock(TEST_SESSION_ID)).thenReturn(TEST_LOCK_TOKEN);
            when(chatMemoryService.getChatMemory(TEST_SESSION_ID, TEST_GUEST_TOKEN, null))
                    .thenThrow(new ResourceNotFoundException("Session không tồn tại"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Hỏi giá", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(geminiRestClient, never()).generateChatCompletion(any());
            verify(chatMemoryService, never()).saveChatMemory(any(), any(), any(), any(), any(), any(), any());
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-08: Khi Gemini upstream bị lỗi hoặc timeout, giải phóng lock, KHÔNG lưu memory và KHÔNG làm mất lịch sử cũ")
        void testSendMessage_GeminiFailure_PreservesHistoryAndReleasesLock() {
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

            // Gemini upstream lỗi 503
            when(geminiRestClient.generateChatCompletion(anyList()))
                    .thenThrow(new GeminiRestClient.GeminiServiceException("Lỗi upstream 503"));

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu mới", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(GeminiRestClient.GeminiServiceException.class);

            // Tuyệt đối không gọi saveChatMemory
            verify(chatMemoryService, never()).saveChatMemory(any(), any(), any(), any(), any(), any(), any());
            // Lock luôn được giải phóng
            verify(chatMemoryService).releaseLock(TEST_SESSION_ID, TEST_LOCK_TOKEN);
        }

        @Test
        @DisplayName("CS-09: Khi Gemini trả về phản hồi null hoặc rỗng, ném GeminiServiceException và giải phóng lock")
        void testSendMessage_GeminiBlankReply_ThrowsException() {
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
            when(geminiRestClient.generateChatCompletion(anyList())).thenReturn("   ");

            assertThatThrownBy(() -> chatService.sendMessage(TEST_SESSION_ID, "Câu hỏi", TEST_GUEST_TOKEN, null))
                    .isInstanceOf(GeminiRestClient.GeminiServiceException.class)
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
            when(geminiRestClient.generateChatCompletion(anyList())).thenReturn("Phản hồi AI");

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
}

