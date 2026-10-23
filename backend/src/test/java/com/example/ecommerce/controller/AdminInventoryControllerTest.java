package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.InventoryTransactionType;
import com.example.ecommerce.service.InventoryService;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_inventory;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private ProductService productService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("GET /api/v1/admin/inventory: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void getInventory_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory: Tài khoản CUSTOMER bị từ chối 403 FORBIDDEN")
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void getInventory_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventory/{id}/adjust: Admin điều chỉnh tồn kho thành công trả về 200 OK")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adjustStock_AdminRole_Success() throws Exception {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(10)
                .note("Nhập kho bổ sung")
                .build();

        InventoryTransactionResponse response = InventoryTransactionResponse.builder()
                .id(1L)
                .productId(10L)
                .productName("Sản phẩm mẫu")
                .changeQuantity(10)
                .stockAfter(50)
                .transactionType(InventoryTransactionType.MANUAL_ADJUST)
                .note("Nhập kho bổ sung")
                .createdAt(LocalDateTime.now())
                .build();

        when(inventoryService.adjustStock(eq(10L), any(AdjustInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/inventory/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockAfter").value(50))
                .andExpect(jsonPath("$.data.changeQuantity").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventory/{id}/adjust: Thiếu trường changeQuantity trả về 400 BAD_REQUEST")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adjustStock_MissingQuantity_Returns400() throws Exception {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(null)
                .build();

        mockMvc.perform(post("/api/v1/admin/inventory/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory/transactions: Admin xem lịch sử giao dịch thành công")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAllTransactions_AdminRole_Success() throws Exception {
        when(inventoryService.getAllTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/inventory/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
