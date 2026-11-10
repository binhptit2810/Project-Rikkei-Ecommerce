package com.example.ecommerce.chat.config;

import com.example.ecommerce.chat.model.ChatRequestContext;
import com.example.ecommerce.exception.GeminiServiceException;
import com.example.ecommerce.exception.GeminiTimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientHttpRequestInterceptor tự động bảo toàn và tái gắn Thought Signature (extra_content.google.thought_signature)
 * cho Google Gemini OpenAI-compatible API trong các vòng lặp Tool Calling / Function Calling.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Khắc phục lỗi thiếu thought_signature khi gọi Function Calling với Google Gemini.
 *
 * Nguyên tắc kỹ thuật & an toàn:
 * 1. Khi Gemini trả về response có tool_calls chứa thought_signature (trong extra_content.google hoặc trực tiếp),
 *    interceptor lưu lại signature ánh xạ theo tool_call_id.
 * 2. Khi Spring AI tạo request tiếp theo (chứa assistant message kèm tool_calls và kết quả thực thi tool),
 *    interceptor kiểm tra và tái gắn đúng thought_signature vào từng tool_call tương ứng.
 * 3. Nếu tool_call chưa có signature trong bộ nhớ tạm (ví dụ kịch bản replay), tự động gắn sentinel
 *    "skip_thought_signature_validator" để vượt qua lớp kiểm tra nghiêm ngặt của Gemini.
 * 4. Bọc request với HttpHeaders cập nhật Content-Length chính xác khi body JSON bị thay đổi kích thước.
 * 5. Bọc response bằng BufferingClientHttpResponse để downstream (Spring AI / Jackson) luôn đọc lại được body stream.
 * 6. Giới hạn dung lượng bộ nhớ đệm (LRU cache eviction) tránh rò rỉ bộ nhớ.
 * 7. Tuyệt đối không ghi API key, authorization header hoặc dữ liệu nhạy cảm vào log.
 */
@Slf4j
public class GeminiThoughtSignatureInterceptor implements ClientHttpRequestInterceptor {

    public static final String SKIP_VALIDATOR_SENTINEL = "skip_thought_signature_validator";
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L; // 10 phút

    private final ObjectMapper objectMapper;
    private final Map<String, SignatureEntry> signatureCache = new ConcurrentHashMap<>();

    public record SignatureEntry(
            JsonNode extraContentNode,
            String thoughtSignature,
            long timestamp
    ) {}

    public GeminiThoughtSignatureInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        // Bước 0: Kiểm tra ngân sách thời gian (Deadline) và giới hạn vòng lặp Tool Calling
        ChatRequestContext context = ChatRequestContext.get();
        if (context != null) {
            int currentTurn = context.incrementAndGetTurn();
            if (currentTurn > context.getMaxTurns()) {
                log.error("Vượt quá số lượt gọi Tool Calling tối đa: {}/{}", currentTurn, context.getMaxTurns());
                throw new GeminiServiceException("Vượt quá số lượt gọi Tool Calling tối đa (" + context.getMaxTurns() + " lượt)");
            }
            long remainingMs = context.getRemainingMs();
            if (remainingMs < 4000L) {
                log.error("Ngân sách thời gian xử lý còn lại quá ngắn để gửi request tới Gemini: {}ms", remainingMs);
                throw new GeminiTimeoutException("Tổng thời gian xử lý yêu cầu AI vượt quá giới hạn ngân sách (Request Deadline Exceeded)");
            }
        }

        byte[] requestBodyToSend = body;
        HttpRequest requestToSend = request;

        try {
            // Bước 1: Tiền xử lý Outgoing Request — kiểm tra và bổ sung thought_signature vào tool_calls nếu bị thiếu
            if (body != null && body.length > 0) {
                byte[] augmentedBody = augmentRequestBodyIfNeeded(body);
                if (augmentedBody != null && augmentedBody != body) {
                    requestBodyToSend = augmentedBody;
                    requestToSend = new ContentLengthAdjustingRequest(request, augmentedBody.length);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tiền xử lý Gemini request body cho thought_signature (tiếp tục gửi body gốc): {}", e.getMessage());
        }

        // Bước 2: Thực thi request tới Gemini OpenAI endpoint
        ClientHttpResponse response = execution.execute(requestToSend, requestBodyToSend);

        // Bước 3: Đọc và đệm lại response body để bảo đảm downstream có thể đọc tiếp InputStream
        byte[] responseBytes = new byte[0];
        try {
            InputStream is = response.getBody();
            if (is != null) {
                responseBytes = is.readAllBytes();
            }
        } catch (IOException e) {
            log.warn("Không thể đọc response body stream từ upstream: status={}, message={}",
                    response.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("Lỗi không mong muốn khi đọc response body stream: {}", e.getMessage());
        }

        // Nếu response là lỗi (4xx, 5xx), ghi log chẩn đoán chi tiết headers phục vụ truy vết
        try {
            if (response.getStatusCode().isError()) {
                HttpHeaders headers = response.getHeaders();
                String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
                log.warn("Gemini API error response: status={}, statusText={}, retryAfter={}, contentType={}, bodyBytes={}",
                        response.getStatusCode(), response.getStatusText(), retryAfter, headers.getContentType(), responseBytes.length);
                if (responseBytes.length > 0) {
                    log.warn("Gemini API error response body: {}", new String(responseBytes, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }

        try {
            // Bước 4: Hậu xử lý Incoming Response — chỉ trích xuất signature khi HTTP 2xx thành công
            if (response.getStatusCode().is2xxSuccessful() && responseBytes.length > 0) {
                captureSignaturesFromResponse(responseBytes);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi trích xuất thought_signature từ Gemini response body: {}", e.getMessage());
        }

        // Luôn trả về BufferingClientHttpResponse để Jackson / OpenAiApi đọc lại toàn bộ stream
        return new BufferingClientHttpResponse(response, responseBytes);
    }

    /**
     * Kiểm tra và bổ sung thought_signature vào các tool_calls trong assistant message của request body.
     */
    public byte[] augmentRequestBodyIfNeeded(byte[] body) {
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            if (!rootNode.isObject()) {
                return body;
            }

            JsonNode messagesNode = rootNode.path("messages");
            if (!messagesNode.isArray() || messagesNode.isEmpty()) {
                return body;
            }

            boolean modified = false;

            for (JsonNode messageNode : messagesNode) {
                String role = messageNode.path("role").asText("");
                if ("assistant".equalsIgnoreCase(role) && messageNode.has("tool_calls")) {
                    JsonNode toolCallsNode = messageNode.path("tool_calls");
                    if (toolCallsNode.isArray()) {
                        for (JsonNode toolCallNode : toolCallsNode) {
                            if (toolCallNode instanceof ObjectNode toolCallObj) {
                                boolean hasSignature = false;

                                // Kiểm tra đã có signature chưa
                                if (toolCallObj.has("extra_content") &&
                                        toolCallObj.path("extra_content").path("google").hasNonNull("thought_signature")) {
                                    hasSignature = true;
                                } else if (toolCallObj.hasNonNull("thought_signature")) {
                                    hasSignature = true;
                                }

                                if (!hasSignature) {
                                    String toolCallId = toolCallObj.path("id").asText(null);
                                    SignatureEntry entry = (toolCallId != null) ? signatureCache.get(toolCallId) : null;

                                    if (entry != null && entry.extraContentNode() != null) {
                                        toolCallObj.set("extra_content", entry.extraContentNode().deepCopy());
                                        if (entry.thoughtSignature() != null) {
                                            toolCallObj.put("thought_signature", entry.thoughtSignature());
                                        }
                                        log.debug("Đã tái gắn thought_signature từ cache cho tool_call_id={}", toolCallId);
                                    } else {
                                        // Fallback sentinel bypass validation của Gemini
                                        ObjectNode extraContentObj = objectMapper.createObjectNode();
                                        ObjectNode googleObj = objectMapper.createObjectNode();
                                        googleObj.put("thought_signature", SKIP_VALIDATOR_SENTINEL);
                                        extraContentObj.set("google", googleObj);

                                        toolCallObj.set("extra_content", extraContentObj);
                                        toolCallObj.put("thought_signature", SKIP_VALIDATOR_SENTINEL);
                                        log.debug("Đã gắn sentinel thought_signature cho tool_call_id={}", toolCallId);
                                    }
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }

            if (modified) {
                return objectMapper.writeValueAsBytes(rootNode);
            }
        } catch (Exception e) {
            log.debug("Không thể phân tích request JSON để bổ sung thought_signature: {}", e.getMessage());
        }

        return body;
    }

    /**
     * Trích xuất và lưu trữ thought_signature từ các tool_calls trong response body của Gemini.
     */
    public void captureSignaturesFromResponse(byte[] responseBytes) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBytes);
            JsonNode choicesNode = rootNode.path("choices");
            if (!choicesNode.isArray()) {
                return;
            }

            long now = System.currentTimeMillis();
            evictExpiredSignatures(now);

            for (JsonNode choiceNode : choicesNode) {
                JsonNode messageNode = choiceNode.path("message");
                JsonNode toolCallsNode = messageNode.path("tool_calls");
                if (toolCallsNode.isArray()) {
                    for (JsonNode toolCallNode : toolCallsNode) {
                        String id = toolCallNode.path("id").asText(null);
                        if (id == null || id.isBlank()) {
                            continue;
                        }

                        JsonNode extraContent = toolCallNode.path("extra_content");
                        String thoughtSig = null;

                        if (extraContent.isObject() && extraContent.path("google").hasNonNull("thought_signature")) {
                            thoughtSig = extraContent.path("google").path("thought_signature").asText();
                        } else if (toolCallNode.hasNonNull("thought_signature")) {
                            thoughtSig = toolCallNode.path("thought_signature").asText();
                        }

                        // Nếu tìm thấy extra_content hoặc thought_signature, lưu vào bộ nhớ đệm
                        if ((extraContent.isObject() && !extraContent.isEmpty()) || (thoughtSig != null && !thoughtSig.isBlank())) {
                            JsonNode extraToSave = extraContent.isObject() && !extraContent.isEmpty()
                                    ? extraContent.deepCopy()
                                    : createExtraContentNode(thoughtSig);

                            signatureCache.put(id, new SignatureEntry(extraToSave, thoughtSig, now));
                            log.debug("Đã ghi nhận thought_signature cho tool_call_id={}", id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Không thể trích xuất thought_signature từ response JSON: {}", e.getMessage());
        }
    }

    private JsonNode createExtraContentNode(String thoughtSig) {
        ObjectNode extraContentObj = objectMapper.createObjectNode();
        ObjectNode googleObj = objectMapper.createObjectNode();
        googleObj.put("thought_signature", thoughtSig != null ? thoughtSig : SKIP_VALIDATOR_SENTINEL);
        extraContentObj.set("google", googleObj);
        return extraContentObj;
    }

    private void evictExpiredSignatures(long now) {
        if (signatureCache.size() > MAX_CACHE_SIZE) {
            signatureCache.entrySet().removeIf(entry -> (now - entry.getValue().timestamp()) > CACHE_TTL_MS);
            if (signatureCache.size() > MAX_CACHE_SIZE) {
                signatureCache.clear();
            }
        }
    }

    public int getCacheSize() {
        return signatureCache.size();
    }

    public boolean hasSignature(String toolCallId) {
        return signatureCache.containsKey(toolCallId);
    }

    public void clearCache() {
        signatureCache.clear();
    }

    /**
     * Request wrapper hỗ trợ cập nhật lại Content-Length sau khi sửa đổi JSON payload.
     */
    private static class ContentLengthAdjustingRequest extends HttpRequestWrapper {
        private final HttpHeaders headers;

        public ContentLengthAdjustingRequest(HttpRequest request, int contentLength) {
            super(request);
            this.headers = new HttpHeaders();
            this.headers.putAll(request.getHeaders());
            this.headers.setContentLength(contentLength);
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }
    }

    /**
     * Response wrapper đệm sẵn toàn bộ InputStream dưới dạng byte array trong bộ nhớ,
     * cho phép getBody() được gọi nhiều lần bởi các converter downstream mà không làm cạn stream.
     */
    private static class BufferingClientHttpResponse implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private final byte[] body;

        public BufferingClientHttpResponse(ClientHttpResponse response, byte[] body) {
            this.response = response;
            this.body = (body != null) ? body : new byte[0];
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }
    }
}
