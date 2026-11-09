package com.example.ecommerce.dto;

import com.example.ecommerce.dto.request.ChatRequest;
import com.example.ecommerce.dto.request.ClaimSessionRequest;
import com.example.ecommerce.dto.response.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test kiểm tra validation và serialization của các DTO cho Chatbot.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 */
class ChatDtoTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpAll() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("1. ChatRequest Validation Tests")
    class ChatRequestTests {

        @Test
        @DisplayName("DTO-01: ChatRequest hợp lệ với sessionId UUID chuẩn và message")
        void testChatRequest_ValidWithSessionId() {
            ChatRequest request = ChatRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .message("Cho tôi hỏi về chính sách bảo hành?")
                    .build();

            Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("DTO-02: ChatRequest hợp lệ với sessionId null (lượt chat đầu tiên của khách)")
        void testChatRequest_ValidWithNullSessionId() {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(null)
                    .message("Sản phẩm này còn hàng không?")
                    .build();

            Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("DTO-03: ChatRequest vi phạm khi message là null hoặc blank")
        void testChatRequest_InvalidBlankMessage() {
            ChatRequest nullMsg = ChatRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .message(null)
                    .build();

            Set<ConstraintViolation<ChatRequest>> violationsNull = validator.validate(nullMsg);
            assertThat(violationsNull).hasSize(1);
            assertThat(violationsNull.iterator().next().getMessage()).isEqualTo("Nội dung tin nhắn không được để trống");

            ChatRequest blankMsg = ChatRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .message("   ")
                    .build();

            Set<ConstraintViolation<ChatRequest>> violationsBlank = validator.validate(blankMsg);
            assertThat(violationsBlank).hasSize(1);
            assertThat(violationsBlank.iterator().next().getMessage()).isEqualTo("Nội dung tin nhắn không được để trống");
        }

        @Test
        @DisplayName("DTO-04: ChatRequest vi phạm khi message vượt quá 1000 ký tự")
        void testChatRequest_InvalidMessageTooLong() {
            String longMessage = "A".repeat(1001);
            ChatRequest request = ChatRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .message(longMessage)
                    .build();

            Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Nội dung tin nhắn không được vượt quá 1000 ký tự");
        }

        @Test
        @DisplayName("DTO-05: ChatRequest vi phạm khi sessionId không đúng định dạng UUID")
        void testChatRequest_InvalidSessionIdFormat() {
            ChatRequest request = ChatRequest.builder()
                    .sessionId("not-a-valid-uuid-12345")
                    .message("Hỏi về giá áo")
                    .build();

            Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Session ID không đúng định dạng UUID");
        }

        @Test
        @DisplayName("DTO-06: ChatRequest JSON serialization và deserialization thành công")
        void testChatRequest_JsonSerDe() throws Exception {
            String json = """
                {
                    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                    "message": "Kiểm tra đơn hàng số 10"
                }
                """;

            ChatRequest request = objectMapper.readValue(json, ChatRequest.class);
            assertThat(request.getSessionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
            assertThat(request.getMessage()).isEqualTo("Kiểm tra đơn hàng số 10");

            String serialized = objectMapper.writeValueAsString(request);
            assertThat(serialized).contains("550e8400-e29b-41d4-a716-446655440000");
        }
    }

    @Nested
    @DisplayName("2. ClaimSessionRequest Validation Tests")
    class ClaimSessionRequestTests {

        @Test
        @DisplayName("DTO-07: ClaimSessionRequest hợp lệ với sessionId UUID và guestToken")
        void testClaimSessionRequest_Valid() {
            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .guestToken("c7b5a1f2e8d9c0b1a2f3e4d5c6b7a8f9")
                    .build();

            Set<ConstraintViolation<ClaimSessionRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("DTO-08: ClaimSessionRequest vi phạm khi thiếu sessionId hoặc guestToken")
        void testClaimSessionRequest_MissingFields() {
            ClaimSessionRequest empty = new ClaimSessionRequest();
            Set<ConstraintViolation<ClaimSessionRequest>> violations = validator.validate(empty);
            assertThat(violations).hasSize(2);

            ClaimSessionRequest blankToken = ClaimSessionRequest.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .guestToken("   ")
                    .build();
            Set<ConstraintViolation<ClaimSessionRequest>> violationsToken = validator.validate(blankToken);
            assertThat(violationsToken).hasSize(1);
            assertThat(violationsToken.iterator().next().getMessage()).isEqualTo("Guest token không được để trống");
        }

        @Test
        @DisplayName("DTO-09: ClaimSessionRequest vi phạm khi sessionId sai định dạng UUID")
        void testClaimSessionRequest_InvalidSessionId() {
            ClaimSessionRequest request = ClaimSessionRequest.builder()
                    .sessionId("abc-xyz")
                    .guestToken("valid-token")
                    .build();

            Set<ConstraintViolation<ClaimSessionRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).isEqualTo("Session ID không đúng định dạng UUID");
        }
    }

    @Nested
    @DisplayName("3. ChatResponse Tests")
    class ChatResponseTests {

        @Test
        @DisplayName("DTO-10: ChatResponse khởi tạo, getters, setters, và không chứa thông tin nhạy cảm")
        void testChatResponse_StructureAndSafety() throws Exception {
            ChatResponse response = ChatResponse.builder()
                    .sessionId("550e8400-e29b-41d4-a716-446655440000")
                    .message("Dạ sản phẩm hiện đang còn hàng với size M, L ạ!")
                    .version(2L)
                    .build();

            assertThat(response.getSessionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
            assertThat(response.getMessage()).contains("còn hàng");
            assertThat(response.getVersion()).isEqualTo(2L);

            String json = objectMapper.writeValueAsString(response);
            assertThat(json).doesNotContain("guestToken");
            assertThat(json).doesNotContain("ownerId");
            assertThat(json).doesNotContain("apiKey");
            assertThat(json).doesNotContain("authorization");
        }
    }

    @Nested
    @DisplayName("4. GuestSessionResponse Tests")
    class GuestSessionResponseTests {

        @Test
        @DisplayName("DTO-11: GuestSessionResponse khởi tạo đầy đủ builder, getter, setter và serialization")
        void testGuestSessionResponse() throws Exception {
            java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusHours(24);
            com.example.ecommerce.dto.response.GuestSessionResponse response =
                    com.example.ecommerce.dto.response.GuestSessionResponse.builder()
                            .sessionId("550e8400-e29b-41d4-a716-446655440000")
                            .guestToken("test-token-123")
                            .expiresAt(expiresAt)
                            .build();

            assertThat(response.getSessionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
            assertThat(response.getGuestToken()).isEqualTo("test-token-123");
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt);

            String json = objectMapper.writeValueAsString(response);
            assertThat(json).contains("550e8400-e29b-41d4-a716-446655440000");
            assertThat(json).contains("test-token-123");
        }
    }
}

