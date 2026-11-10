package com.example.ecommerce.chat.controller;

import com.example.ecommerce.exception.ChatAuthenticationException;
import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.ChatService;
import com.example.ecommerce.chat.service.RateLimiterService;
import com.example.ecommerce.dto.request.ChatRequest;
import com.example.ecommerce.dto.request.ClaimSessionRequest;
import com.example.ecommerce.dto.response.ChatResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.exception.*;
import com.example.ecommerce.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller API Test cho ChatController sử dụng MockMvc standalone và GlobalExceptionHandler.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_GUEST_TOKEN = "test-guest-secret-token-32bytes";
    private static final Long TEST_USER_ID = 1001L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("1. POST /api/v1/chat/sessions - Khởi tạo phiên khách")
    class CreateGuestSessionTests {

        @Test
        @DisplayName("CTRL-01: Tạo phiên khách thành công trả HTTP 201 cùng sessionId và rawGuestToken")
        void testCreateGuestSession_Success() throws Exception {
            ChatSessionMetadata metadata = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            GuestSessionCreationResult result = GuestSessionCreationResult.builder()
                    .sessionId(TEST_SESSION_ID)
                    .rawGuestToken(TEST_GUEST_TOKEN)
                    .metadata(metadata)
                    .build();

            when(chatService.createGuestSession()).thenReturn(result);

            mockMvc.perform(post("/api/v1/chat/sessions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(TEST_SESSION_ID))
                    .andExpect(jsonPath("$.data.guestToken").value(TEST_GUEST_TOKEN))
                    .andExpect(jsonPath("$.data.expiresAt").exists());

            verify(rateLimiterService).checkRateLimit(anyString(), isNull());
        }

        @Test
        @DisplayName("CTRL-02: Vượt quá rate limit khi tạo phiên trả HTTP 429 kèm header Retry-After")
        void testCreateGuestSession_RateLimitExceeded() throws Exception {
            doThrow(new ChatRateLimitExceededException("Quá nhiều yêu cầu", 45L))
                    .when(rateLimiterService).checkRateLimit(anyString(), isNull());

            mockMvc.perform(post("/api/v1/chat/sessions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "45"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Quá nhiều yêu cầu"));
        }
    }

    @Nested
    @DisplayName("2. POST /api/v1/chat/messages - Gửi tin nhắn chat")
    class SendMessageTests {

        @Test
        @DisplayName("CTRL-03: Khách vãng lai gửi tin nhắn thành công với header X-Guest-Token")
        void testSendMessage_GuestSuccess() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Sản phẩm này có size XL không?")
                    .build();

            ChatResponse response = ChatResponse.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Dạ sản phẩm còn size XL ạ!")
                    .version(2L)
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Sản phẩm này có size XL không?"), eq(TEST_GUEST_TOKEN), isNull(), any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(TEST_SESSION_ID))
                    .andExpect(jsonPath("$.data.message").value("Dạ sản phẩm còn size XL ạ!"))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("CTRL-04: Khách vãng lai thiếu header X-Guest-Token bị từ chối với HTTP 401")
        void testSendMessage_GuestMissingToken() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)"));
        }

        @Test
        @DisplayName("CTRL-04b: Khách vãng lai gửi tin nhắn qua endpoint tương thích POST /api/v1/chat thành công")
        void testSendMessage_ViaBaseChatEndpoint_GuestSuccess() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Sản phẩm này có size M không?")
                    .build();

            ChatResponse response = ChatResponse.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Dạ sản phẩm còn size M ạ!")
                    .version(2L)
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Sản phẩm này có size M không?"), eq(TEST_GUEST_TOKEN), isNull(), any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/chat")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(TEST_SESSION_ID))
                    .andExpect(jsonPath("$.data.message").value("Dạ sản phẩm còn size M ạ!"))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("CTRL-04c: Gửi tin nhắn qua POST /api/v1/chat thiếu header X-Guest-Token bị từ chối HTTP 401")
        void testSendMessage_ViaBaseChatEndpoint_GuestMissingToken() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)"));
        }

        @Test
        @DisplayName("CTRL-04d: Guest token sai hoặc không hợp lệ bị từ chối HTTP 401 với thông điệp rõ ràng")
        void testSendMessage_InvalidGuestToken_Returns401() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Hỏi giá"), eq("invalid-token"), isNull(), any()))
                    .thenThrow(new ChatAuthenticationException("Guest token không hợp lệ hoặc phiên đã được claim"));

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", "invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Guest token không hợp lệ hoặc phiên đã được claim"));
        }

        @Test
        @DisplayName("CTRL-05: Người dùng đã đăng nhập gửi tin nhắn thành công qua SecurityContext, không cần X-Guest-Token")
        void testSendMessage_AuthenticatedUserSuccess() throws Exception {
            CustomUserDetails principal = new CustomUserDetails(TEST_USER_ID, "user@example.com", "pass", Role.CUSTOMER, true);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Kiểm tra đơn hàng của tôi")
                    .build();

            ChatResponse response = ChatResponse.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Đơn hàng của bạn đang được giao")
                    .version(4L)
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Kiểm tra đơn hàng của tôi"), isNull(), eq(String.valueOf(TEST_USER_ID)), any()))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.version").value(4));
        }

        @Test
        @DisplayName("CTRL-06: Truyền Authorization Bearer nhưng không xác thực thành công trong SecurityContext bị từ chối 401")
        void testSendMessage_InvalidBearerToken_Rejects401() throws Exception {
            // Không set SecurityContext nhưng gửi header Authorization (JWT sai / hết hạn)
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-expired-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
        }

        @Test
        @DisplayName("CTRL-07: Validation lỗi khi message trống hoặc quá dài trả HTTP 400")
        void testSendMessage_MessageValidationFailure() throws Exception {
            ChatRequest emptyMsg = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("   ")
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyMsg)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            ChatRequest tooLongMsg = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("a".repeat(1001))
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tooLongMsg)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("CTRL-08: Validation lỗi khi sessionId sai định dạng UUID trả HTTP 400")
        void testSendMessage_InvalidUuidFailure() throws Exception {
            ChatRequest invalidUuid = ChatRequest.builder()
                    .sessionId("not-a-valid-uuid")
                    .message("Xin chào")
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUuid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.sessionId").value("Session ID không đúng định dạng UUID"));
        }

        @Test
        @DisplayName("CTRL-09: Khi Lock Contention xảy ra trả HTTP 409 Conflict")
        void testSendMessage_LockContention_Returns409() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            when(chatService.sendMessage(any(), any(), any(), any(), any()))
                    .thenThrow(new ChatLockContentionException("Phiên đang xử lý yêu cầu trước"));

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Phiên đang xử lý yêu cầu trước"));
        }

        @Test
        @DisplayName("CTRL-10: Khi OCC version conflict xảy ra trả HTTP 409 Conflict")
        void testSendMessage_OccConflict_Returns409() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            when(chatService.sendMessage(any(), any(), any(), any(), any()))
                    .thenThrow(new ChatVersionConflictException("Dữ liệu đã bị thay đổi bởi request khác"));

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Dữ liệu đã bị thay đổi bởi request khác"));
        }

        @Test
        @DisplayName("CTRL-11: Khi Gemini gặp lỗi upstream trả HTTP 503 Service Unavailable")
        void testSendMessage_GeminiServiceError_Returns503() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            when(chatService.sendMessage(any(), any(), any(), any(), any()))
                    .thenThrow(new GeminiServiceException("Upstream 500 error"));

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("CTRL-11b: Khi Gemini bị timeout trả HTTP 504 Gateway Timeout")
        void testSendMessage_GeminiTimeout_Returns504() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            when(chatService.sendMessage(any(), any(), any(), any(), any()))
                    .thenThrow(new GeminiTimeoutException("Timeout"));

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGatewayTimeout());
        }
    }

    @Nested
    @DisplayName("3. POST /api/v1/chat/claim - Chuyển quyền sở hữu phiên chat")
    class ClaimSessionTests {

        @Test
        @DisplayName("CTRL-12: Người dùng đã đăng nhập claim phiên khách thành công")
        void testClaimSession_Success() throws Exception {
            CustomUserDetails principal = new CustomUserDetails(TEST_USER_ID, "user@example.com", "pass", Role.CUSTOMER, true);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .guestToken(TEST_GUEST_TOKEN)
                    .build();

            ChatSessionMetadata updatedMeta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId(String.valueOf(TEST_USER_ID))
                    .version(2L)
                    .build();

            when(chatService.claimSession(TEST_SESSION_ID, TEST_GUEST_TOKEN, String.valueOf(TEST_USER_ID)))
                    .thenReturn(updatedMeta);

            mockMvc.perform(post("/api/v1/chat/claim")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.ownerId").value(String.valueOf(TEST_USER_ID)))
                    .andExpect(jsonPath("$.data.version").value(2));
        }

        @Test
        @DisplayName("CTRL-13: Khách vãng lai chưa đăng nhập gọi claim bị từ chối với HTTP 403 Forbidden")
        void testClaimSession_UnauthenticatedUser_Returns403() throws Exception {
            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .guestToken(TEST_GUEST_TOKEN)
                    .build();

            mockMvc.perform(post("/api/v1/chat/claim")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Yêu cầu đăng nhập để nhận quyền sở hữu phiên trò chuyện"));
        }

        @Test
        @DisplayName("CTRL-14: ClaimSessionRequest thiếu sessionId hoặc guestToken bị từ chối với HTTP 400")
        void testClaimSession_ValidationFailure() throws Exception {
            CustomUserDetails principal = new CustomUserDetails(TEST_USER_ID, "user@example.com", "pass", Role.CUSTOMER, true);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            ClaimSessionRequest blankRequest = ClaimSessionRequest.builder()
                    .sessionId("")
                    .guestToken("")
                    .build();

            mockMvc.perform(post("/api/v1/chat/claim")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
