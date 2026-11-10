package com.example.ecommerce.chat.config;

import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import com.example.ecommerce.chat.model.ChatRequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho GeminiThoughtSignatureInterceptor.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Khắc phục lỗi thiếu thought_signature khi gọi Function Calling với Google Gemini.
 */
class GeminiThoughtSignatureInterceptorTest {

    private ObjectMapper objectMapper;
    private GeminiThoughtSignatureInterceptor interceptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interceptor = new GeminiThoughtSignatureInterceptor(objectMapper);
        ChatRequestContext.clear();
    }

    @AfterEach
    void tearDown() {
        ChatRequestContext.clear();
    }

    @Test
    @DisplayName("TSI-01: Trích xuất thành công thought_signature từ extra_content.google trong response của Gemini")
    void testCaptureSignatureFromExtraContent() {
        String responseJson = """
            {
              "id": "chatcmpl-test-01",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": null,
                    "tool_calls": [
                      {
                        "id": "call_search_123",
                        "type": "function",
                        "function": {
                          "name": "searchProducts",
                          "arguments": "{\\"query\\":\\"ao thun\\"}"
                        },
                        "extra_content": {
                          "google": {
                            "thought_signature": "cryptographic_signature_abc123"
                          }
                        }
                      }
                    ]
                  },
                  "finish_reason": "tool_calls"
                }
              ]
            }
            """;

        interceptor.captureSignaturesFromResponse(responseJson.getBytes(StandardCharsets.UTF_8));

        assertThat(interceptor.hasSignature("call_search_123")).isTrue();
        assertThat(interceptor.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("TSI-02: Trích xuất thành công thought_signature khi xuất hiện trực tiếp tại root của tool_call")
    void testCaptureSignatureFromDirectField() {
        String responseJson = """
            {
              "id": "chatcmpl-test-02",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "tool_calls": [
                      {
                        "id": "call_stock_456",
                        "type": "function",
                        "function": {
                          "name": "checkProductStock",
                          "arguments": "{\\"productId\\":1}"
                        },
                        "thought_signature": "direct_signature_xyz789"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        interceptor.captureSignaturesFromResponse(responseJson.getBytes(StandardCharsets.UTF_8));

        assertThat(interceptor.hasSignature("call_stock_456")).isTrue();
    }

    @Test
    @DisplayName("TSI-03: Tái gắn chính xác thought_signature từ bộ nhớ đệm vào request tiếp theo của Spring AI")
    void testAugmentRequestBody_WithCachedSignature() throws Exception {
        // Giả lập turn 1: Lưu signature vào cache
        String responseJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "tool_calls": [
                      {
                        "id": "call_search_123",
                        "type": "function",
                        "function": { "name": "searchProducts", "arguments": "{}" },
                        "extra_content": {
                          "google": {
                            "thought_signature": "sig_saved_turn_1"
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;
        interceptor.captureSignaturesFromResponse(responseJson.getBytes(StandardCharsets.UTF_8));

        // Giả lập turn 2: Request từ Spring AI không có extra_content / thought_signature
        String turn2RequestJson = """
            {
              "model": "gemini-3.6-flash",
              "messages": [
                { "role": "user", "content": "Tìm kiếm áo" },
                {
                  "role": "assistant",
                  "content": null,
                  "tool_calls": [
                    {
                      "id": "call_search_123",
                      "type": "function",
                      "function": { "name": "searchProducts", "arguments": "{}" }
                    }
                  ]
                },
                {
                  "role": "tool",
                  "tool_call_id": "call_search_123",
                  "content": "{\\"products\\":[]}"
                }
              ]
            }
            """;

        byte[] augmentedBytes = interceptor.augmentRequestBodyIfNeeded(turn2RequestJson.getBytes(StandardCharsets.UTF_8));
        String augmentedJson = new String(augmentedBytes, StandardCharsets.UTF_8);

        JsonNode root = objectMapper.readTree(augmentedJson);
        JsonNode assistantToolCall = root.path("messages").get(1).path("tool_calls").get(0);

        assertThat(assistantToolCall.path("extra_content").path("google").path("thought_signature").asText())
                .isEqualTo("sig_saved_turn_1");
        assertThat(assistantToolCall.path("thought_signature").asText())
                .isEqualTo("sig_saved_turn_1");
    }

    @Test
    @DisplayName("TSI-04: Tự động gắn sentinel skip_thought_signature_validator khi không có signature trong cache (fallback)")
    void testAugmentRequestBody_FallbackSentinel() throws Exception {
        // Request có tool_call không hề xuất hiện trong cache (ví dụ kịch bản replay / migrate)
        String requestJson = """
            {
              "messages": [
                {
                  "role": "assistant",
                  "tool_calls": [
                    {
                      "id": "call_unknown_999",
                      "type": "function",
                      "function": { "name": "searchProducts", "arguments": "{}" }
                    }
                  ]
                }
              ]
            }
            """;

        byte[] augmentedBytes = interceptor.augmentRequestBodyIfNeeded(requestJson.getBytes(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(augmentedBytes);
        JsonNode toolCall = root.path("messages").get(0).path("tool_calls").get(0);

        assertThat(toolCall.path("extra_content").path("google").path("thought_signature").asText())
                .isEqualTo(GeminiThoughtSignatureInterceptor.SKIP_VALIDATOR_SENTINEL);
        assertThat(toolCall.path("thought_signature").asText())
                .isEqualTo(GeminiThoughtSignatureInterceptor.SKIP_VALIDATOR_SENTINEL);
    }

    @Test
    @DisplayName("TSI-05: Không sửa đổi nếu tool_call đã có sẵn thought_signature hợp lệ")
    void testAugmentRequestBody_AlreadyHasSignature() {
        String requestJson = """
            {
              "messages": [
                {
                  "role": "assistant",
                  "tool_calls": [
                    {
                      "id": "call_already_ok",
                      "type": "function",
                      "function": { "name": "searchProducts", "arguments": "{}" },
                      "extra_content": {
                        "google": {
                          "thought_signature": "existing_valid_sig"
                        }
                      }
                    }
                  ]
                }
              ]
            }
            """;

        byte[] originalBytes = requestJson.getBytes(StandardCharsets.UTF_8);
        byte[] resultBytes = interceptor.augmentRequestBodyIfNeeded(originalBytes);

        // Body không bị thay đổi tham chiếu hoặc nội dung
        assertThat(resultBytes).isEqualTo(originalBytes);
    }

    @Test
    @DisplayName("TSI-06: Request thông thường không có tool_calls được giữ nguyên hoàn toàn")
    void testAugmentRequestBody_NormalMessage() {
        String normalRequestJson = """
            {
              "model": "gemini-3.6-flash",
              "messages": [
                { "role": "system", "content": "You are an assistant" },
                { "role": "user", "content": "Xin chào sàn Rikkei" }
              ]
            }
            """;

        byte[] originalBytes = normalRequestJson.getBytes(StandardCharsets.UTF_8);
        byte[] resultBytes = interceptor.augmentRequestBodyIfNeeded(originalBytes);

        assertThat(resultBytes).isEqualTo(originalBytes);
    }

    @Test
    @DisplayName("TSI-07: Hỗ trợ nhiều tool_calls song song (parallel function calling)")
    void testMultipleToolCalls() throws Exception {
        String responseJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "tool_calls": [
                      {
                        "id": "call_multi_1",
                        "type": "function",
                        "function": { "name": "searchProducts", "arguments": "{}" },
                        "extra_content": { "google": { "thought_signature": "sig_multi_1" } }
                      },
                      {
                        "id": "call_multi_2",
                        "type": "function",
                        "function": { "name": "checkProductStock", "arguments": "{}" },
                        "extra_content": { "google": { "thought_signature": "sig_multi_2" } }
                      }
                    ]
                  }
                }
              ]
            }
            """;
        interceptor.captureSignaturesFromResponse(responseJson.getBytes(StandardCharsets.UTF_8));

        assertThat(interceptor.hasSignature("call_multi_1")).isTrue();
        assertThat(interceptor.hasSignature("call_multi_2")).isTrue();

        String turn2Request = """
            {
              "messages": [
                {
                  "role": "assistant",
                  "tool_calls": [
                    { "id": "call_multi_1", "type": "function", "function": { "name": "searchProducts", "arguments": "{}" } },
                    { "id": "call_multi_2", "type": "function", "function": { "name": "checkProductStock", "arguments": "{}" } }
                  ]
                }
              ]
            }
            """;

        byte[] augmentedBytes = interceptor.augmentRequestBodyIfNeeded(turn2Request.getBytes(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(augmentedBytes);
        JsonNode toolCalls = root.path("messages").get(0).path("tool_calls");

        assertThat(toolCalls.get(0).path("extra_content").path("google").path("thought_signature").asText())
                .isEqualTo("sig_multi_1");
        assertThat(toolCalls.get(1).path("extra_content").path("google").path("thought_signature").asText())
                .isEqualTo("sig_multi_2");
    }

    @Test
    @DisplayName("TSI-08: Full intercept loop thực thi đúng với ClientHttpRequestExecution và cập nhật Content-Length")
    void testFullInterceptExecution() throws IOException {
        // Bước 1: Giả lập response chứa tool call & thought signature
        String turn1ResponseJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "tool_calls": [
                      {
                        "id": "call_exec_01",
                        "type": "function",
                        "function": { "name": "searchProducts", "arguments": "{}" },
                        "extra_content": { "google": { "thought_signature": "exec_signature_token" } }
                      }
                    ]
                  }
                }
              ]
            }
            """;

        MockClientHttpRequest request1 = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        MockClientHttpResponse response1 = new MockClientHttpResponse(turn1ResponseJson.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        ClientHttpRequestExecution execution1 = mock(ClientHttpRequestExecution.class);
        when(execution1.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response1);

        interceptor.intercept(request1, "{}".getBytes(StandardCharsets.UTF_8), execution1);
        assertThat(interceptor.hasSignature("call_exec_01")).isTrue();

        // Bước 2: Giả lập Turn 2 gọi intercept
        String turn2RequestJson = """
            {
              "messages": [
                {
                  "role": "assistant",
                  "tool_calls": [
                    { "id": "call_exec_01", "type": "function", "function": { "name": "searchProducts", "arguments": "{}" } }
                  ]
                }
              ]
            }
            """;

        MockClientHttpRequest request2 = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        MockClientHttpResponse response2 = new MockClientHttpResponse("{\"choices\":[]}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);

        ClientHttpRequestExecution execution2 = mock(ClientHttpRequestExecution.class);
        when(execution2.execute(any(HttpRequest.class), any(byte[].class))).thenAnswer(invocation -> {
            HttpRequest req = invocation.getArgument(0);
            byte[] body = invocation.getArgument(1);

            // Xác minh header Content-Length đã được cập nhật đúng với body đã gắn signature
            assertThat(req.getHeaders().getContentLength()).isEqualTo(body.length);

            // Xác minh body đã có thought_signature
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            assertThat(bodyStr).contains("exec_signature_token");

            return response2;
        });

        ClientHttpResponse actualResponse = interceptor.intercept(request2, turn2RequestJson.getBytes(StandardCharsets.UTF_8), execution2);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TSI-09: Chặn đệ quy vô hạn khi turnCount > 3 ném GeminiServiceException")
    void testRecursionGuard_MaxTurnsExceeded() {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);
        ChatRequestContext.get().incrementAndGetTurn(); // turn 1
        ChatRequestContext.get().incrementAndGetTurn(); // turn 2
        ChatRequestContext.get().incrementAndGetTurn(); // turn 3
        ChatRequestContext.get().incrementAndGetTurn(); // turn 4 (> 3)

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        assertThatThrownBy(() -> interceptor.intercept(request, "{}".getBytes(StandardCharsets.UTF_8), execution))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("Tool Calling");

        verifyNoInteractions(execution);
    }

    @Test
    @DisplayName("TSI-10: Ngắt sớm khi remainingBudget < 4000ms ném GeminiTimeoutException")
    void testBudgetGuard_InsufficientBudget() {
        // Thiết lập deadline chỉ còn 2 giây (< 4000ms)
        ChatRequestContext.init(System.currentTimeMillis() + 2000L, 3);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        assertThatThrownBy(() -> interceptor.intercept(request, "{}".getBytes(StandardCharsets.UTF_8), execution))
                .isInstanceOf(GeminiTimeoutException.class)
                .hasMessageContaining("Request Deadline Exceeded");

        verifyNoInteractions(execution);
    }

    @Test
    @DisplayName("TSI-11: Response lỗi 429 Too Many Requests không gây exception, bảo toàn Status và Header Retry-After")
    void testErrorResponse_PreservesHeadersAndStatus() throws IOException {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        MockClientHttpResponse errorResponse = new MockClientHttpResponse("{\"error\":\"rate limit\"}".getBytes(StandardCharsets.UTF_8), HttpStatus.TOO_MANY_REQUESTS);
        errorResponse.getHeaders().set(HttpHeaders.RETRY_AFTER, "5");

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(errorResponse);

        ClientHttpResponse actualResponse = interceptor.intercept(request, "{}".getBytes(StandardCharsets.UTF_8), execution);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(actualResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
    }
}

