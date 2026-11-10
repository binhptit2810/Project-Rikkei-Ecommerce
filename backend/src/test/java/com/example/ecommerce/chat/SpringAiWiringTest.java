package com.example.ecommerce.chat;

import com.example.ecommerce.chat.config.SpringAiConfig;
import com.example.ecommerce.chat.tool.ChatToolsConfig;
import com.example.ecommerce.chat.tool.InventoryTools;
import com.example.ecommerce.chat.tool.OrderTools;
import com.example.ecommerce.chat.tool.ProductTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test kiểm tra việc nạp và khởi tạo thành công Spring ApplicationContext với Spring AI.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Wiring Test
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_wiring;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SpringAiWiringTest {

    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired(required = false)
    private com.example.ecommerce.chat.config.GeminiThoughtSignatureInterceptor thoughtSignatureInterceptor;

    @Autowired
    private ProductTools productTools;

    @Autowired
    private InventoryTools inventoryTools;

    @Autowired
    private OrderTools orderTools;

    @Autowired
    private ChatToolsConfig chatToolsConfig;

    @Test
    @DisplayName("WIRING-01: Spring AI OpenAiChatModel, ChatClient và ThoughtSignatureInterceptor được khởi tạo thành công trong ApplicationContext")
    void testSpringAiBeansWired() {
        assertThat(openAiChatModel).isNotNull();
        assertThat(chatClient).isNotNull();
        assertThat(thoughtSignatureInterceptor).isNotNull();
    }

    @Test
    @DisplayName("WIRING-02: Toàn bộ Tool Components và Callback Beans được khởi tạo đầy đủ")
    void testToolsWired() {
        assertThat(productTools).isNotNull();
        assertThat(inventoryTools).isNotNull();
        assertThat(orderTools).isNotNull();
        assertThat(chatToolsConfig).isNotNull();

        FunctionCallback[] guestTools = chatToolsConfig.getGuestTools();
        assertThat(guestTools).hasSize(3);

        FunctionCallback[] allTools = chatToolsConfig.getAllTools();
        assertThat(allTools).hasSize(5);
    }

    @Test
    @DisplayName("WIRING-03: Kiểm tra cấu hình và metadata của các FunctionCallback")
    void testFunctionCallbackMetadata() {
        FunctionCallback searchTool = chatToolsConfig.searchProductsCallback();
        assertThat(searchTool.getName()).isEqualTo("searchProducts");
        assertThat(searchTool.getDescription()).contains("Tìm kiếm");

        FunctionCallback stockTool = chatToolsConfig.checkProductStockCallback();
        assertThat(stockTool.getName()).isEqualTo("checkProductStock");
        assertThat(stockTool.getDescription()).contains("tồn kho");

        FunctionCallback myOrdersTool = chatToolsConfig.getMyOrdersCallback();
        assertThat(myOrdersTool.getName()).isEqualTo("getMyOrders");
        assertThat(myOrdersTool.getDescription()).contains("đơn hàng");
    }
}

