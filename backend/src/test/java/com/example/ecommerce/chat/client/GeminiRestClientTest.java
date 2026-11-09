package com.example.ecommerce.chat.client;

import com.example.ecommerce.chat.client.GeminiRestClient.GeminiServiceException;
import com.example.ecommerce.chat.client.GeminiRestClient.GeminiTimeoutException;
import com.example.ecommerce.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit Test cho GeminiRestClient sử dụng MockRestServiceServer của Spring.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Kiểm tra:
 * 1. Request gửi đúng endpoint và method POST.
 * 2. Request body đúng format OpenAI-compatible.
 * 3. Header Authorization chứa Bearer token đúng.
 * 4. Parse thành công câu trả lời của Assistant.
 * 5. Phân loại chính xác lỗi upstream: 401, 429, 500 thành GeminiServiceException.
 * 6. Xử lý timeout thành GeminiTimeoutException.
 * 7. Xử lý phản hồi sai định dạng hoặc rỗng.
 * 8. Đảm bảo không tự ý retry request.
 */
class GeminiRestClientTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String API_KEY = "test-gemini-secret-api-key-xyz";
    private static final String MODEL = "gemini-2.5-flash";

    private MockRestServiceServer mockServer;
    private GeminiRestClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        objectMapper = new ObjectMapper();

        client = new GeminiRestClient(
                API_KEY,
                BASE_URL,
                MODEL,
                builder.build(),
                objectMapper
        );
    }

    @Test
    @DisplayName("GEMINI-01: Gửi request đúng format OpenAI-compatible và parse thành công assistant content")
    void testGenerateChatCompletion_Success() {
        String mockResponse = """
            {
              "id": "chatcmpl-test-12345",
              "object": "chat.completion",
              "created": 1731160000,
              "model": "gemini-2.5-flash",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Dạ chào bạn, Rikkei Store có bán áo thun nam nữ đủ kích cỡ ạ!"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
            """;

        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Cửa hàng có áo thun không?"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Cửa hàng có áo thun không?"));
        String reply = client.generateChatCompletion(messages);

        assertThat(reply).isEqualTo("Dạ chào bạn, Rikkei Store có bán áo thun nam nữ đủ kích cỡ ạ!");
        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-02: Xử lý upstream 401 Unauthorized thành GeminiServiceException")
    void testGenerateChatCompletion_Upstream401() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"API key not valid\"}"));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("lỗi xác thực upstream");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-03: Xử lý upstream 429 Too Many Requests thành GeminiServiceException")
    void testGenerateChatCompletion_Upstream429() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Rate limit exceeded\"}"));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("quá tải hạn mức");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-04: Xử lý upstream 500 Internal Server Error thành GeminiServiceException")
    void testGenerateChatCompletion_Upstream500() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Internal server error\"}"));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("sự cố nội bộ");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-05: Xử lý SocketTimeoutException thành GeminiTimeoutException (HTTP 504 mapping)")
    void testGenerateChatCompletion_Timeout() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiTimeoutException.class)
                .hasMessageContaining("vượt quá thời gian xử lý (timeout)");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-06: Xử lý lỗi kết nối mạng I/O thành GeminiServiceException")
    void testGenerateChatCompletion_ConnectionFailure() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("Không thể kết nối đến dịch vụ tư vấn AI");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-07: Xử lý phản hồi rỗng hoặc sai định dạng JSON thành GeminiServiceException")
    void testGenerateChatCompletion_MalformedJson() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("invalid-json-content", MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("Không thể phân tích định dạng phản hồi");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-08: Xử lý phản hồi thiếu mảng choices thành GeminiServiceException")
    void testGenerateChatCompletion_MissingChoices() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\": []}", MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("thiếu danh sách choices");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-09: Xử lý phản hồi có nội dung content rỗng/blank thành GeminiServiceException")
    void testGenerateChatCompletion_BlankContent() {
        String mockResponse = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "    "
                  }
                }
              ]
            }
            """;

        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("không có ký tự hợp lệ");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-10: Ném ngoại lệ khi API key chưa được cấu hình")
    void testGenerateChatCompletion_UnconfiguredApiKey() {
        RestClient.Builder builder = RestClient.builder();
        GeminiRestClient unconfiguredClient = new GeminiRestClient(
                "",
                BASE_URL,
                MODEL,
                builder.build(),
                objectMapper
        );

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> unconfiguredClient.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("chưa được cấu hình khóa truy cập");
    }

    @Test
    @DisplayName("GEMINI-11: Ném IllegalArgumentException khi danh sách messages là null hoặc rỗng")
    void testGenerateChatCompletion_EmptyOrNullMessages() {
        assertThatThrownBy(() -> client.generateChatCompletion(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Danh sách tin nhắn không được để trống");

        assertThatThrownBy(() -> client.generateChatCompletion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Danh sách tin nhắn không được để trống");
    }

    @Test
    @DisplayName("GEMINI-12: Constructor với RestClient.Builder khởi tạo client với cấu hình timeout mặc định 5s connect và 12s read")
    void testSpringConstructor_WithBuilder() {
        RestClient.Builder builder = RestClient.builder();
        GeminiRestClient diClient = new GeminiRestClient(
                API_KEY,
                BASE_URL,
                MODEL,
                builder,
                objectMapper
        );

        assertThat(diClient).isNotNull();
        assertThat(diClient.getBaseUrl()).isEqualTo(BASE_URL);
        assertThat(diClient.getModel()).isEqualTo(MODEL);

        RestClient restClient = (RestClient) ReflectionTestUtils.getField(diClient, "restClient");
        assertThat(restClient).isNotNull();

        ClientHttpRequestFactory requestFactory = null;
        Class<?> current = restClient.getClass();
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                if (ClientHttpRequestFactory.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        requestFactory = (ClientHttpRequestFactory) field.get(restClient);
                        if (requestFactory != null) break;
                    } catch (Exception ignored) {
                    }
                }
            }
            if (requestFactory != null) break;
            current = current.getSuperclass();
        }

        assertThat(requestFactory).isNotNull();
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);

        Object connectTimeout = ReflectionTestUtils.getField(requestFactory, "connectTimeout");
        if (connectTimeout instanceof Duration duration) {
            assertThat(duration.toMillis()).isEqualTo(5000L);
        } else if (connectTimeout instanceof Number num) {
            assertThat(num.intValue()).isEqualTo(5000);
        }

        Object readTimeout = ReflectionTestUtils.getField(requestFactory, "readTimeout");
        if (readTimeout instanceof Duration duration) {
            assertThat(duration.toMillis()).isEqualTo(12000L);
        } else if (readTimeout instanceof Number num) {
            assertThat(num.intValue()).isEqualTo(12000);
        }
    }

    @Test
    @DisplayName("GEMINI-13: Gọi generateChatCompletion với temperature null, gửi request với temperature mặc định 0.7")
    void testGenerateChatCompletion_WithNullTemperature() {
        String mockResponse = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Phản hồi với temperature mặc định 0.7"
                  }
                }
              ]
            }
            """;

        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Hỏi với temp null"));
        String reply = client.generateChatCompletion(messages, null);

        assertThat(reply).isEqualTo("Phản hồi với temperature mặc định 0.7");
        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-14: Xử lý upstream 400 Bad Request thành GeminiServiceException")
    void testGenerateChatCompletion_Upstream400() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Bad request\"}"));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("Lỗi kết nối dịch vụ tư vấn AI upstream với mã trạng thái: 400");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-15: Xử lý upstream 503 Service Unavailable thành GeminiServiceException")
    void testGenerateChatCompletion_Upstream503() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": \"Service unavailable\"}"));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("sự cố nội bộ từ nhà cung cấp");

        mockServer.verify();
    }

    @Test
    @DisplayName("GEMINI-16: Xử lý phản hồi có choices nhưng thiếu trường message thành GeminiServiceException")
    void testGenerateChatCompletion_MissingMessageInChoice() {
        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\": [{\"finish_reason\": \"stop\"}]}", MediaType.APPLICATION_JSON));

        List<ChatMessage> messages = List.of(ChatMessage.ofUser("Xin chào"));

        assertThatThrownBy(() -> client.generateChatCompletion(messages))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("thiếu trường message");

        mockServer.verify();
    }
}

