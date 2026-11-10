package com.example.ecommerce.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Cấu hình khởi tạo Spring AI ChatClient và OpenAiChatModel kết nối Google Gemini OpenAI-compatible API.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Integration
 *
 * Nguyên tắc kỹ thuật & an toàn:
 * 1. Đọc API Key, Base URL, Model từ biến môi trường.
 * 2. Tuyệt đối không log API key hoặc hardcode bí mật.
 * 3. Chuẩn hóa Base URL: tự động loại bỏ hậu tố '/chat/completions' nếu biến môi trường bị cấu hình thừa,
 *    ngăn ngừa lỗi 404 Not Found khi ghép với completions-path.
 * 4. Thiết lập Connect Timeout (5s) và Read Timeout (15s) tránh treo luồng ứng dụng.
 */
@Configuration
@Slf4j
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String configuredBaseUrl;

    @Value("${spring.ai.openai.chat.completions-path:/chat/completions}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.options.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Bean
    public GeminiThoughtSignatureInterceptor geminiThoughtSignatureInterceptor(
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        ObjectMapper mapperToUse = (objectMapper != null) ? objectMapper : new ObjectMapper();
        return new GeminiThoughtSignatureInterceptor(mapperToUse);
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            FunctionCallbackContext functionCallbackContext,
            List<FunctionCallback> toolCallbacks,
            GeminiThoughtSignatureInterceptor thoughtSignatureInterceptor
    ) {
        String cleanBaseUrl = sanitizeBaseUrl(configuredBaseUrl);
        String cleanCompletionsPath = sanitizeCompletionsPath(completionsPath);

        log.info("Initializing Spring AI OpenAiChatModel for Gemini: baseUrl={}, path={}, model={}",
                cleanBaseUrl, cleanCompletionsPath, modelName);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);  // 5s connect timeout
        requestFactory.setReadTimeout(12000);   // 12s read timeout (giảm từ 15s để nằm trọn trong deadline 32s)

        BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(requestFactory);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(bufferingFactory)
                .requestInterceptor(thoughtSignatureInterceptor);

        OpenAiApi openAiApi = new OpenAiApi(
                cleanBaseUrl,
                apiKey != null ? apiKey.trim() : "",
                cleanCompletionsPath,
                "/v1/embeddings",
                restClientBuilder,
                WebClient.builder(),
                new DefaultResponseErrorHandler()
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(modelName != null && !modelName.isBlank() ? modelName.trim() : "gemini-2.5-flash")
                .withTemperature(temperature != null ? temperature : 0.7)
                .build();

        // Khởi tạo RetryTemplate phân tầng: maxAttempts=2, Exponential Backoff, loại trừ 4xx/ReadTimeout/Quota
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
        backOffPolicy.setInitialInterval(1500L);
        backOffPolicy.setMultiplier(1.5);
        backOffPolicy.setMaxInterval(3500L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(new GeminiRetryPolicy(2));

        return new OpenAiChatModel(
                openAiApi,
                options,
                functionCallbackContext,
                toolCallbacks,
                retryTemplate
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        log.info("Building Spring AI ChatClient instance from OpenAiChatModel");
        return ChatClient.builder(openAiChatModel).build();
    }

    /**
     * Chuẩn hóa baseUrl: loại bỏ khoảng trắng, dấu gạch chéo cuối và hậu tố '/chat/completions' nếu có.
     */
    public static String sanitizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://generativelanguage.googleapis.com/v1beta/openai";
        }
        String cleaned = url.trim();
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.endsWith("/chat/completions")) {
            cleaned = cleaned.substring(0, cleaned.length() - "/chat/completions".length());
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    /**
     * Chuẩn hóa completionsPath: đảm bảo bắt đầu bằng '/' và không bị thừa gạch chéo.
     */
    public static String sanitizeCompletionsPath(String path) {
        if (path == null || path.isBlank()) {
            return "/chat/completions";
        }
        String cleaned = path.trim();
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }
        return cleaned;
    }
}

