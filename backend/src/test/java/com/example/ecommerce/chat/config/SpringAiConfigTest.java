package com.example.ecommerce.chat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test cho SpringAiConfig (Sanitization logic cho Google Gemini URL).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT]
 */
class SpringAiConfigTest {

    @Test
    @DisplayName("SAC-01: sanitizeBaseUrl loại bỏ hậu tố /chat/completions nếu bị cấu hình thừa trong .env")
    void testSanitizeBaseUrl_WithCompletionsPath() {
        String input = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        String clean = SpringAiConfig.sanitizeBaseUrl(input);

        assertThat(clean).isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
    }

    @Test
    @DisplayName("SAC-02: sanitizeBaseUrl loại bỏ dấu gạch chéo cuối cùng")
    void testSanitizeBaseUrl_WithTrailingSlash() {
        String input = "https://generativelanguage.googleapis.com/v1beta/openai/";
        String clean = SpringAiConfig.sanitizeBaseUrl(input);

        assertThat(clean).isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
    }

    @Test
    @DisplayName("SAC-03: sanitizeBaseUrl trả về URL mặc định an toàn khi null hoặc rỗng")
    void testSanitizeBaseUrl_NullOrBlank() {
        assertThat(SpringAiConfig.sanitizeBaseUrl(null))
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
        assertThat(SpringAiConfig.sanitizeBaseUrl("   "))
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
    }

    @Test
    @DisplayName("SAC-04: sanitizeCompletionsPath đảm bảo luôn có dấu gạch chéo đầu")
    void testSanitizeCompletionsPath() {
        assertThat(SpringAiConfig.sanitizeCompletionsPath("chat/completions"))
                .isEqualTo("/chat/completions");
        assertThat(SpringAiConfig.sanitizeCompletionsPath("/chat/completions"))
                .isEqualTo("/chat/completions");
        assertThat(SpringAiConfig.sanitizeCompletionsPath(null))
                .isEqualTo("/chat/completions");
    }
}

