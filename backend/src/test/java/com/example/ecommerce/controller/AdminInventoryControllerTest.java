package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.InventoryTransactionType;
import com.example.ecommerce.service.InventoryService;
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

/**
 * Controller Test cho AdminInventoryController.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_inventory_ctrl;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("POST /api/v1/admin/inventory/{id}/adjust: Quyền ADMIN điều chỉnh kho thành công trả 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void adjustStock_Success_WhenAdmin() throws Exception {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(50)
                .type(InventoryTransactionType.MANUAL_IMPORT)
                .reason("Nhập hàng đợt 1 tháng 11")
                .build();

        InventoryTransactionResponse response = InventoryTransactionResponse.builder()
                .id(1L)
                .productId(10L)
                .productName("Sản phẩm mẫu")
                .changeQuantity(50)
                .stockAfter(60)
                .transactionType(InventoryTransactionType.MANUAL_IMPORT)
                .transactionTypeDescription("Admin nhập kho thủ công")
                .note("Nhập hàng đợt 1 tháng 11")
                .createdAt(LocalDateTime.now())
                .build();

        when(inventoryService.adjustStock(eq(10L), any(AdjustInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/inventory/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockAfter").value(60))
                .andExpect(jsonPath("$.data.transactionType").value("MANUAL_IMPORT"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventory/{id}/adjust: Vai trò CUSTOMER bị từ chối trả 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void adjustStock_Forbidden_WhenCustomer() throws Exception {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(10)
                .build();

        mockMvc.perform(post("/api/v1/admin/inventory/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory/transactions: Quyền ADMIN tra cứu lịch sử kho thành công trả 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void getTransactions_Success_WhenAdmin() throws Exception {
        InventoryTransactionResponse tx = InventoryTransactionResponse.builder()
                .id(1L)
                .productId(10L)
                .productName("Sản phẩm mẫu")
                .changeQuantity(10)
                .stockAfter(15)
                .transactionType(InventoryTransactionType.MANUAL_IMPORT)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<InventoryTransactionResponse> pageResponse = PageResponse.<InventoryTransactionResponse>builder()
                .content(List.of(tx))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(inventoryService.getTransactions(any(), any(), any(), any(), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/inventory/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory/{id}/transactions: ADMIN tra cứu lịch sử theo sản phẩm thành công")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void getTransactionsByProduct_Success_WhenAdmin() throws Exception {
        InventoryTransactionResponse tx = InventoryTransactionResponse.builder()
                .id(2L)
                .productId(15L)
                .changeQuantity(5)
                .stockAfter(20)
                .transactionType(InventoryTransactionType.MANUAL_IMPORT)
                .createdAt(LocalDateTime.now())
                .build();

        when(inventoryService.getTransactionsByProduct(15L)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/admin/inventory/15/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2));
    }
}
