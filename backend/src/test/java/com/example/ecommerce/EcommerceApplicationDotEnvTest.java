package com.example.ecommerce;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit Test độc lập xác minh cơ chế nạp biến môi trường từ file .env của EcommerceApplication.
 * Không phụ thuộc file .env thật của repository, đảm bảo chạy độc lập trên mọi môi trường CI/CD.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT]
 */
class EcommerceApplicationDotEnvTest {

    private static final List<String> TRACKED_KEYS = List.of(
            "DOTENV_TEST_VAR_1",
            "DOTENV_TEST_VAR_2",
            "DOTENV_TEST_URL",
            "DOTENV_TEST_COMPLEX_URL",
            "DOTENV_TEST_QUOTED_DOUBLE",
            "DOTENV_TEST_QUOTED_SINGLE",
            "DOTENV_TEST_PRESET",
            "DOTENV_TEST_UNSET"
    );

    private final Map<String, String> originalProperties = new HashMap<>();

    @BeforeEach
    void setUp() {
        originalProperties.clear();
        for (String key : TRACKED_KEYS) {
            originalProperties.put(key, System.getProperty(key));
        }
    }

    @AfterEach
    void tearDown() {
        for (String key : TRACKED_KEYS) {
            String originalValue = originalProperties.get(key);
            if (originalValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, originalValue);
            }
        }
    }

    @Test
    @DisplayName("DOTENV-01: File .env tạm tồn tại và có biến hợp lệ -> Nạp thành công vào System Properties")
    void testDotEnv01_ValidEnvFile_LoadsSuccessfully(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env.test");
        Files.writeString(envFile, "DOTENV_TEST_VAR_1=rikkei_ecommerce_test\n", StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_VAR_1")).isEqualTo("rikkei_ecommerce_test");
    }

    @Test
    @DisplayName("DOTENV-02: File .env không tồn tại -> Không ném exception, ứng dụng chạy bình thường")
    void testDotEnv02_NonExistentFile_GracefulHandling(@TempDir Path tempDir) {
        File nonExistentFile = tempDir.resolve("non_existent.env").toFile();

        assertThatCode(() -> EcommerceApplication.loadDotEnvIfPresent(nonExistentFile))
                .doesNotThrowAnyException();

        assertThat(System.getProperty("DOTENV_TEST_UNSET")).isNull();
    }

    @Test
    @DisplayName("DOTENV-03: System Property đã tồn tại trước đó -> Không bị ghi đè bởi file .env")
    void testDotEnv03_ExistingProperty_NotOverwritten(@TempDir Path tempDir) throws IOException {
        System.setProperty("DOTENV_TEST_PRESET", "initial_preset_value");

        Path envFile = tempDir.resolve(".env.test");
        Files.writeString(envFile, "DOTENV_TEST_PRESET=overwritten_value\n", StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_PRESET")).isEqualTo("initial_preset_value");
    }

    @Test
    @DisplayName("DOTENV-04: File .env chứa nhiều biến -> Xử lý đúng và nạp đầy đủ")
    void testDotEnv04_MultipleVariables_LoadedCorrectly(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env.test");
        String content = """
                DOTENV_TEST_VAR_1=value_one
                DOTENV_TEST_VAR_2=value_two
                DOTENV_TEST_URL=https://generativelanguage.googleapis.com/v1beta/openai
                """;
        Files.writeString(envFile, content, StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_VAR_1")).isEqualTo("value_one");
        assertThat(System.getProperty("DOTENV_TEST_VAR_2")).isEqualTo("value_two");
        assertThat(System.getProperty("DOTENV_TEST_URL")).isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
    }

    @Test
    @DisplayName("DOTENV-05: File .env có dòng comment và dòng trống -> Bỏ qua hợp lệ, không tạo property rác")
    void testDotEnv05_CommentsAndEmptyLines_Ignored(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env.test");
        String content = """
                # This is a comment at the beginning
                   
                # Another indented comment
                DOTENV_TEST_VAR_1=active_value
                
                # Comment in between
                DOTENV_TEST_VAR_2=second_value
                
                # Trailing comment
                """;
        Files.writeString(envFile, content, StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_VAR_1")).isEqualTo("active_value");
        assertThat(System.getProperty("DOTENV_TEST_VAR_2")).isEqualTo("second_value");
    }

    @Test
    @DisplayName("DOTENV-06: Giá trị có dấu '=' hoặc bọc trong dấu ngoặc kép/đơn -> Parse chính xác và gỡ ngoặc")
    void testDotEnv06_ComplexValuesWithEqualsAndQuotes(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env.test");
        String content = """
                DOTENV_TEST_COMPLEX_URL=https://example.com/api?param1=val1&param2=val2
                DOTENV_TEST_QUOTED_DOUBLE="quoted double string with spaces"
                DOTENV_TEST_QUOTED_SINGLE='quoted single string with spaces'
                """;
        Files.writeString(envFile, content, StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_COMPLEX_URL"))
                .isEqualTo("https://example.com/api?param1=val1&param2=val2");
        assertThat(System.getProperty("DOTENV_TEST_QUOTED_DOUBLE"))
                .isEqualTo("quoted double string with spaces");
        assertThat(System.getProperty("DOTENV_TEST_QUOTED_SINGLE"))
                .isEqualTo("quoted single string with spaces");
    }

    @Test
    @DisplayName("DOTENV-07: Biến cấu hình không có trong file .env -> Giữ nguyên null, không tự sinh giá trị giả")
    void testDotEnv07_UnsetVariable_RemainsNull(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env.test");
        Files.writeString(envFile, "DOTENV_TEST_VAR_1=only_configured_var\n", StandardCharsets.UTF_8);

        EcommerceApplication.loadDotEnvIfPresent(envFile.toFile());

        assertThat(System.getProperty("DOTENV_TEST_VAR_1")).isEqualTo("only_configured_var");
        assertThat(System.getProperty("DOTENV_TEST_UNSET")).isNull();
    }

    @Test
    @DisplayName("DOTENV-08: Kiểm tra tính cô lập trạng thái giữa các bài test -> Không rò rỉ System Properties")
    void testDotEnv08_NoStateLeakageBetweenTests() {
        // Xác minh tại thời điểm bắt đầu test, các biến test đều null do @AfterEach đã dọn dẹp từ test trước
        assertThat(System.getProperty("DOTENV_TEST_VAR_1")).isNull();
        assertThat(System.getProperty("DOTENV_TEST_VAR_2")).isNull();
        assertThat(System.getProperty("DOTENV_TEST_PRESET")).isNull();
    }
}
