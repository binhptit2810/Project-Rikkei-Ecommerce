package com.example.ecommerce.chat.controller;

import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.ChatService;
import com.example.ecommerce.chat.service.RateLimiterService;
import com.example.ecommerce.dto.request.ChatRequest;
import com.example.ecommerce.dto.request.ClaimSessionRequest;
import com.example.ecommerce.dto.response.ChatResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security Integration Test xác minh toàn bộ luồng Spring Security Filter Chain cho Chatbot API.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Kiểm tra:
 * 1. Không có Authorization header -> Cho phép Guest truy cập endpoint permitAll (/sessions, /messages).
 * 2. Có Bearer token nhưng sai/hết hạn -> Trả HTTP 401 ngay lập tức, KHÔNG âm thầm chuyển thành Guest.
 * 3. Có Bearer token hợp lệ -> Trích xuất danh tính User thành công.
 * 4. Quyền truy cập các endpoint khác của hệ thống (/orders, /admin) vẫn được bảo vệ nguyên vẹn.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_chat_sec;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class ChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private ChatService chatService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private UserRepository userRepository;

    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_GUEST_TOKEN = "valid-guest-secret-token";
    private static final String TEST_USER_EMAIL = "testuser@rikkei.com";
    private static final Long TEST_USER_ID = 9999L;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        // Mock User trong Database cho CustomUserDetailsService
        User mockUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_USER_EMAIL)
                .password("$2a$12$e8F27vJgQxQ...")
                .fullName("Test Security User")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(TEST_USER_EMAIL)).thenReturn(Optional.of(mockUser));

        // Sinh JWT Access Token hợp lệ
        validJwtToken = jwtUtils.generateToken(TEST_USER_ID, TEST_USER_EMAIL, Role.CUSTOMER.name());
    }

    @Nested
    @DisplayName("1. Guest Access (No Authorization Header)")
    class GuestAccessTests {

        @Test
        @DisplayName("SEC-01: Không có Authorization header, POST /sessions được permitAll và trả 201")
        void testGuestCreateSession_PermitAll() throws Exception {
            ChatSessionMetadata meta = ChatSessionMetadata.builder()
                    .sessionId(TEST_SESSION_ID)
                    .ownerId("GUEST")
                    .version(1L)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            when(chatService.createGuestSession()).thenReturn(
                    GuestSessionCreationResult.builder()
                            .sessionId(TEST_SESSION_ID)
                            .rawGuestToken(TEST_GUEST_TOKEN)
                            .metadata(meta)
                            .build()
            );

            mockMvc.perform(post("/api/v1/chat/sessions")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.sessionId").value(TEST_SESSION_ID));
        }

        @Test
        @DisplayName("SEC-02: Không có Authorization header nhưng có X-Guest-Token, POST /messages được permitAll")
        void testGuestSendMessage_PermitAll() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi thông tin sản phẩm")
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Hỏi thông tin sản phẩm"), eq(TEST_GUEST_TOKEN), isNull()))
                    .thenReturn(ChatResponse.builder()
                            .sessionId(TEST_SESSION_ID)
                            .message("Dạ chào bạn!")
                            .version(2L)
                            .build());

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("SEC-02b: Không có Authorization header nhưng có X-Guest-Token, POST /api/v1/chat được permitAll")
        void testGuestSendMessage_BaseEndpoint_PermitAll() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi thông tin sản phẩm")
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Hỏi thông tin sản phẩm"), eq(TEST_GUEST_TOKEN), isNull()))
                    .thenReturn(ChatResponse.builder()
                            .sessionId(TEST_SESSION_ID)
                            .message("Dạ chào bạn!")
                            .version(2L)
                            .build());

            mockMvc.perform(post("/api/v1/chat")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("SEC-03: Không có Authorization header, POST /claim bị chặn 403 Forbidden")
        void testGuestClaimSession_Forbidden() throws Exception {
            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .guestToken(TEST_GUEST_TOKEN)
                    .build();

            mockMvc.perform(post("/api/v1/chat/claim")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("2. Invalid / Expired Bearer Token Security Tests")
    class InvalidBearerTokenTests {

        @Test
        @DisplayName("SEC-04: Gửi Bearer token không hợp lệ tới POST /messages bị từ chối HTTP 401, KHÔNG giáng cấp thành Guest")
        void testInvalidBearerToken_MessagesReturns401() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Hỏi giá")
                    .build();

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-signature-jwt-token")
                            .header("X-Guest-Token", TEST_GUEST_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
        }

        @Test
        @DisplayName("SEC-05: Gửi Bearer token sai định dạng tới POST /claim bị từ chối HTTP 401")
        void testInvalidBearerToken_ClaimReturns401() throws Exception {
            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .guestToken(TEST_GUEST_TOKEN)
                    .build();

            mockMvc.perform(post("/api/v1/chat/claim")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer malformed.jwt")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
        }
    }

    @Nested
    @DisplayName("3. Authenticated Access (Valid Bearer JWT)")
    class AuthenticatedAccessTests {

        @Test
        @DisplayName("SEC-06: Gửi Bearer JWT hợp lệ tới POST /messages được xác thực đúng userId từ SecurityContext")
        void testValidBearerToken_MessagesSuccess() throws Exception {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(TEST_SESSION_ID)
                    .message("Tôi đã đăng nhập")
                    .build();

            when(chatService.sendMessage(eq(TEST_SESSION_ID), eq("Tôi đã đăng nhập"), isNull(), eq(String.valueOf(TEST_USER_ID)), any()))
                    .thenReturn(ChatResponse.builder()
                            .sessionId(TEST_SESSION_ID)
                            .message("Chào mừng bạn quay lại!")
                            .version(3L)
                            .build());

            mockMvc.perform(post("/api/v1/chat/messages")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.version").value(3));
        }

        @Test
        @DisplayName("SEC-07: Gửi Bearer JWT hợp lệ tới POST /claim thực hiện chuyển quyền sở hữu thành công")
        void testValidBearerToken_ClaimSuccess() throws Exception {
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
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.ownerId").value(String.valueOf(TEST_USER_ID)));
        }
    }

    @Nested
    @DisplayName("4. Other Endpoints Security Protection")
    class OtherEndpointsSecurityTests {

        @Test
        @DisplayName("SEC-08: PermitAll trên /api/v1/chat/** không làm ảnh hưởng đến bảo vệ của các API khác (/orders vẫn 401 nếu thiếu JWT)")
        void testOtherProtectedEndpointsRemainSecured() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Chưa đăng nhập hoặc token hết hạn"));
        }
    }
}
