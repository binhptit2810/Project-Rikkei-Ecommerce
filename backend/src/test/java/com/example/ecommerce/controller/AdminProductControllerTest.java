package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho AdminProductController.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_prod;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/products: Admin lấy danh sách sản phẩm quản trị thành công trả 200 OK")
    void getProducts_asAdmin_returns200() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .id(1L)
                .name("Áo thun nam basic")
                .price(new BigDecimal("199000"))
                .stockQuantity(50)
                .active(true)
                .build();

        PageResponse<ProductResponse> page = PageResponse.<ProductResponse>builder()
                .content(List.of(prod))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(productService.getAdminProducts(any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Áo thun nam basic"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/products/{id}: Admin xem chi tiết sản phẩm thành công trả 200 OK")
    void getProductById_asAdmin_returns200() throws Exception {
        ProductResponse prod = ProductResponse.builder()
                .id(1L)
                .name("Áo thun nam basic")
                .price(new BigDecimal("199000"))
                .active(false) // kể cả sản phẩm inactive admin vẫn xem được
                .build();

        when(productService.getAdminProductById(1L)).thenReturn(prod);

        mockMvc.perform(get("/api/v1/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/products: Admin thêm mới sản phẩm thành công trả 201 CREATED")
    void createProduct_asAdmin_validRequest_returns201() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo Polo Nam Cao Cấp")
                .description("Vải cá sấu thoáng mát")
                .price(new BigDecimal("299000"))
                .imageUrl("https://example.com/polo.jpg")
                .categoryId(1L)
                .stockQuantity(100)
                .build();

        ProductResponse created = ProductResponse.builder()
                .id(10L)
                .name("Áo Polo Nam Cao Cấp")
                .price(new BigDecimal("299000"))
                .categoryId(1L)
                .categoryName("Thời trang")
                .stockQuantity(100)
                .active(true)
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thêm mới sản phẩm thành công"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Áo Polo Nam Cao Cấp"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/products: Giá âm bị Bean Validation từ chối trả 400 BAD REQUEST")
    void createProduct_negativePrice_returns400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo Polo")
                .price(new BigDecimal("-1000"))
                .categoryId(1L)
                .stockQuantity(10)
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.data.price").exists());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/products: Tồn kho âm hoặc thiếu categoryId bị từ chối trả 400 BAD REQUEST")
    void createProduct_negativeStockOrMissingCategory_returns400() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo Polo")
                .price(new BigDecimal("200000"))
                .categoryId(null)
                .stockQuantity(-5)
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/admin/products/{id}: Admin cập nhật sản phẩm thành công trả 200 OK")
    void updateProduct_asAdmin_validRequest_returns200() throws Exception {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Áo Polo Nam v2")
                .description("Cập nhật vải")
                .price(new BigDecimal("320000"))
                .categoryId(1L)
                .stockQuantity(150)
                .active(true)
                .build();

        ProductResponse updated = ProductResponse.builder()
                .id(10L)
                .name("Áo Polo Nam v2")
                .price(new BigDecimal("320000"))
                .stockQuantity(150)
                .active(true)
                .build();

        when(productService.updateProduct(eq(10L), any(UpdateProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/products/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật sản phẩm thành công"))
                .andExpect(jsonPath("$.data.name").value("Áo Polo Nam v2"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/admin/products/{id}: Admin xóa mềm sản phẩm thành công trả 200 OK")
    void deleteProduct_asAdmin_returns200() throws Exception {
        doNothing().when(productService).deleteProduct(10L);

        mockMvc.perform(delete("/api/v1/admin/products/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa mềm sản phẩm thành công"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/admin/products: Khách hàng (CUSTOMER) bị từ chối 403 FORBIDDEN")
    void createProduct_asCustomer_returns403() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo")
                .price(new BigDecimal("1000"))
                .categoryId(1L)
                .stockQuantity(10)
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/products: Người dùng chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void createProduct_unauthenticated_returns401() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo")
                .price(new BigDecimal("1000"))
                .categoryId(1L)
                .stockQuantity(10)
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

