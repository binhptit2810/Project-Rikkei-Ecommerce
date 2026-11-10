package com.example.ecommerce.chat;

import com.example.ecommerce.chat.config.GeminiThoughtSignatureInterceptor;
import com.example.ecommerce.chat.tool.dto.ProductSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * End-to-End Integration Test kiểm chứng toàn bộ chu trình Tool Calling giữa Spring AI và Gemini:
 * Đảm bảo thought_signature từ Turn 1 được bảo toàn và truyền đầy đủ sang Turn 2,
 * ngăn chặn dứt điểm lỗi HTTP 400 "Function call is missing a thought_signature".
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT]
 */
class SpringAiToolCallingThoughtSignatureTest {

    @Test
    @DisplayName("E2E-TOOL-01: Chu trình Tool Calling bảo toàn thought_signature giữa Turn 1 và Turn 2")
    void testToolCallingPreservesThoughtSignatureAcrossTurns() {
        ObjectMapper objectMapper = new ObjectMapper();
        GeminiThoughtSignatureInterceptor interceptor = new GeminiThoughtSignatureInterceptor(objectMapper);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(requestFactory);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(bufferingFactory)
                .requestInterceptor(interceptor);

        // Khởi tạo MockRestServiceServer gắn vào restClientBuilder
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        OpenAiApi openAiApi = new OpenAiApi(
                "https://generativelanguage.googleapis.com/v1beta/openai",
                "test-dummy-api-key",
                "/chat/completions",
                "/v1/embeddings",
                restClientBuilder,
                WebClient.builder(),
                new DefaultResponseErrorHandler()
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("gemini-3.6-flash")
                .withTemperature(0.7)
                .build();

        // Định nghĩa 1 tool callback mẫu: searchProducts
        FunctionCallback searchProductsTool = FunctionCallback.builder()
                .description("Tìm kiếm sản phẩm")
                .function("searchProducts", (ProductSearchRequest req) -> Map.of("count", 1, "items", List.of("Áo thun Rikkei Premium")))
                .inputType(ProductSearchRequest.class)
                .build();

        FunctionCallbackContext functionCallbackContext = new FunctionCallbackContext();
        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                options,
                functionCallbackContext,
                List.of(searchProductsTool),
                new RetryTemplate()
        );

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // -------------------------------------------------------------
        // Turn 1 Mock: Gemini trả về Tool Call kèm thought_signature bí mật
        // -------------------------------------------------------------
        String turn1ResponseJson = """
            {
              "id": "chatcmpl-turn-1",
              "object": "chat.completion",
              "created": 1731215000,
              "model": "gemini-3.6-flash",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": null,
                    "tool_calls": [
                      {
                        "id": "call_search_98765",
                        "type": "function",
                        "function": {
                          "name": "searchProducts",
                          "arguments": "{\\"query\\":\\"ao thun\\"}"
                        },
                        "extra_content": {
                          "google": {
                            "thought_signature": "cryptographic_proof_of_thinking_98765"
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

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andRespond(withSuccess(turn1ResponseJson, MediaType.APPLICATION_JSON));

        // -------------------------------------------------------------
        // Turn 2 Mock: Spring AI thực thi Tool searchProducts và gửi request thứ hai.
        // MockServer xác minh payload gửi lên Gemini BẮT BUỘC chứa cryptographic_proof_of_thinking_98765!
        // -------------------------------------------------------------
        String turn2FinalResponseJson = """
            {
              "id": "chatcmpl-turn-2",
              "object": "chat.completion",
              "created": 1731215005,
              "model": "gemini-3.6-flash",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Tôi tìm thấy 1 sản phẩm: Áo thun Rikkei Premium phù hợp với yêu cầu của bạn!"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
            """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(content().string(containsString("cryptographic_proof_of_thinking_98765")))
                .andExpect(content().string(containsString("extra_content")))
                .andRespond(withSuccess(turn2FinalResponseJson, MediaType.APPLICATION_JSON));

        // -------------------------------------------------------------
        // Kích hoạt cuộc gọi Chatbot: Người dùng gửi câu hỏi
        // -------------------------------------------------------------
        String reply = chatClient.prompt()
                .user("Tìm giúp tôi áo thun")
                .functions(searchProductsTool)
                .call()
                .content();

        // -------------------------------------------------------------
        // Kiểm chứng kết quả
        // -------------------------------------------------------------
        mockServer.verify();

        assertThat(reply).isNotBlank();
        assertThat(reply).contains("Áo thun Rikkei Premium");
        assertThat(interceptor.hasSignature("call_search_98765")).isTrue();
    }
}

