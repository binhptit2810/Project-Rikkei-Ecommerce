package com.example.ecommerce.chat.client;

import com.example.ecommerce.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Client giao tiếp với Google Gemini thông qua OpenAI-compatible endpoint.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc kỹ thuật & bảo mật:
 * 1. Sử dụng Spring Framework 6.1 RestClient native, không dùng Spring AI hay OpenAI SDK.
 * 2. Connect timeout: 5s, Read timeout: 12s.
 * 3. Tuyệt đối không log API key, header Authorization hoặc payload chứa dữ liệu nhạy cảm.
 * 4. Không tự động retry request để tránh gửi câu hỏi trùng lặp hoặc phát sinh chi phí token.
 * 5. Bọc lỗi upstream (401, 429, 500) thành GeminiServiceException (ánh xạ HTTP 503 trong Phase sau).
 * 6. Bọc lỗi timeout thành GeminiTimeoutException (ánh xạ HTTP 504 trong Phase sau).
 */
@Component
@Slf4j
public class GeminiRestClient {

    private final String apiKey;

    @Getter
    private final String baseUrl;

    @Getter
    private final String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Ngoại lệ xảy ra khi dịch vụ Gemini upstream gặp lỗi (401, 429, 500, lỗi kết nối hoặc định dạng sai).
     * Sẽ được ánh xạ thành HTTP 503 Service Unavailable trong Phase sau.
     */
    public static class GeminiServiceException extends RuntimeException {
        public GeminiServiceException(String message) {
            super(message);
        }

        public GeminiServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Ngoại lệ xảy ra khi yêu cầu gọi tới Gemini bị quá thời gian xử lý (Connect/Read timeout).
     * Sẽ được ánh xạ thành HTTP 504 Gateway Timeout trong Phase sau.
     */
    public static class GeminiTimeoutException extends RuntimeException {
        public GeminiTimeoutException(String message) {
            super(message);
        }

        public GeminiTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Constructor phục vụ Spring Dependency Injection.
     */
    @Autowired
    public GeminiRestClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai/chat/completions}") String baseUrl,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this(
                apiKey,
                baseUrl,
                model,
                buildDefaultRestClient(baseUrl, restClientBuilder),
                objectMapper
        );
    }

    /**
     * Constructor linh hoạt phục vụ Unit Test hoặc tùy biến RestClient.
     */
    public GeminiRestClient(
            String apiKey,
            String baseUrl,
            String model,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.trim()
                : "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        this.model = (model != null && !model.isBlank())
                ? model.trim()
                : "gemini-2.5-flash";
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private static RestClient buildDefaultRestClient(String baseUrl, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5s connect timeout
        requestFactory.setReadTimeout(12000);  // 12s read timeout

        String url = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.trim()
                : "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

        return builder
                .baseUrl(url)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Gửi danh sách tin nhắn tới Google Gemini qua OpenAI-compatible endpoint và trả về nội dung câu trả lời.
     *
     * @param messages Danh sách tin nhắn hội thoại theo định dạng OpenAI-compatible.
     * @return Nội dung phản hồi từ Assistant.
     */
    public String generateChatCompletion(List<ChatMessage> messages) {
        return generateChatCompletion(messages, 0.7);
    }

    /**
     * Gửi danh sách tin nhắn tới Google Gemini với cấu hình temperature tùy chọn.
     *
     * @param messages    Danh sách tin nhắn hội thoại.
     * @param temperature Mức độ sáng tạo (0.0 - 1.0).
     * @return Nội dung phản hồi từ Assistant.
     */
    public String generateChatCompletion(List<ChatMessage> messages, Double temperature) {
        if (this.apiKey.isBlank()) {
            log.error("Gemini API key is not configured");
            throw new GeminiServiceException("Dịch vụ tư vấn AI tạm thời chưa được cấu hình khóa truy cập");
        }

        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Danh sách tin nhắn không được để trống");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", this.model);
            requestBody.put("temperature", temperature != null ? temperature : 0.7);

            List<Map<String, String>> formattedMessages = messages.stream()
                    .map(msg -> Map.of(
                            "role", msg.getRole() != null ? msg.getRole() : "user",
                            "content", msg.getContent() != null ? msg.getContent() : ""
                    ))
                    .toList();
            requestBody.put("messages", formattedMessages);

            String responseBody = restClient.post()
                    .uri(this.baseUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, resp) -> {
                        log.error("Gemini API authentication failed (HTTP {})", resp.getStatusCode().value());
                        throw new GeminiServiceException("Dịch vụ tư vấn AI tạm thời không khả dụng do lỗi xác thực upstream");
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        log.error("Gemini API rate limit exceeded (HTTP 429)");
                        throw new GeminiServiceException("Dịch vụ tư vấn AI đang quá tải hạn mức (quota exceeded), vui lòng thử lại sau");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("Gemini API server error (HTTP {})", resp.getStatusCode().value());
                        throw new GeminiServiceException("Dịch vụ tư vấn AI gặp sự cố nội bộ từ nhà cung cấp");
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Gemini API unhandled HTTP error: {}", resp.getStatusCode().value());
                        throw new GeminiServiceException("Lỗi kết nối dịch vụ tư vấn AI upstream với mã trạng thái: " + resp.getStatusCode().value());
                    })
                    .body(String.class);

            return parseAssistantResponse(responseBody);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                log.error("Gemini API request timed out: {}", e.getMessage());
                throw new GeminiTimeoutException("Yêu cầu tư vấn AI vượt quá thời gian xử lý (timeout)", e);
            }
            log.error("Gemini API connection error: {}", e.getMessage());
            throw new GeminiServiceException("Không thể kết nối đến dịch vụ tư vấn AI", e);
        } catch (GeminiTimeoutException | GeminiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Gemini chat completion: {}", e.getMessage());
            throw new GeminiServiceException("Lỗi xử lý phản hồi từ dịch vụ tư vấn AI", e);
        }
    }

    private boolean isTimeout(ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && (msg.toLowerCase().contains("timed out") || msg.toLowerCase().contains("timeout"));
    }

    private String parseAssistantResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new GeminiServiceException("Phản hồi từ Gemini API rỗng");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new GeminiServiceException("Định dạng phản hồi Gemini API không hợp lệ: thiếu danh sách choices");
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode messageNode = firstChoice.path("message");
            if (messageNode.isMissingNode() || messageNode.isNull()) {
                throw new GeminiServiceException("Định dạng phản hồi Gemini API không hợp lệ: thiếu trường message");
            }
            JsonNode contentNode = messageNode.path("content");

            if (contentNode.isMissingNode() || contentNode.isNull()) {
                throw new GeminiServiceException("Nội dung phản hồi từ Gemini API rỗng");
            }

            String content = contentNode.asText();
            if (content.isBlank()) {
                throw new GeminiServiceException("Nội dung phản hồi từ Gemini API không có ký tự hợp lệ");
            }

            return content.trim();
        } catch (GeminiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response JSON: {}", e.getMessage());
            throw new GeminiServiceException("Không thể phân tích định dạng phản hồi từ dịch vụ tư vấn AI", e);
        }
    }
}

