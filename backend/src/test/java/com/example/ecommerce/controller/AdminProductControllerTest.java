package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_product;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("GET /api/v1/admin/products: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void getAdminProducts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/admin/products: Khách hàng thường truy cập bị từ chối 403 FORBIDDEN")
    void getAdminProducts_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/products: Admin thêm sản phẩm thành công trả 201 CREATED")
    void createProduct_asAdmin_returns201() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Tai nghe Bluetooth")
                .description("Chống ồn chủ động")
                .price(new BigDecimal("1200000"))
                .stockQuantity(15)
                .active(true)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(5L)
                .name("Tai nghe Bluetooth")
                .price(new BigDecimal("1200000"))
                .stockQuantity(15)
                .active(true)
                .build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Tai nghe Bluetooth"))
                .andExpect(jsonPath("$.data.stockQuantity").value(15));
    }

    @Test
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/admin/products/5: Admin xóa sản phẩm thành công trả 200 OK")
    void deleteProduct_asAdmin_returns200() throws Exception {
        doNothing().when(productService).deleteProduct(5L);

        mockMvc.perform(delete("/api/v1/admin/products/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
