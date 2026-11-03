package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateCategoryRequest;
import com.example.ecommerce.dto.request.UpdateCategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.service.CategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho AdminCategoryController.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_cat;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/categories: Admin lấy toàn bộ danh mục thành công trả 200 OK")
    void getAllCategories_asAdmin_returns200() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .id(1L)
                .name("Thời trang")
                .active(true)
                .build();

        when(categoryService.getAllCategoriesForAdmin()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Thời trang"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/categories/{id}: Admin xem chi tiết danh mục thành công trả 200 OK")
    void getCategoryById_asAdmin_returns200() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .id(1L)
                .name("Thời trang")
                .active(true)
                .build();

        when(categoryService.getCategoryById(1L)).thenReturn(cat);

        mockMvc.perform(get("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Thời trang"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/categories: Admin thêm mới danh mục thành công trả 201 CREATED")
    void createCategory_asAdmin_validRequest_returns201() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Giày dép")
                .description("Phụ kiện giày dép")
                .imageUrl("https://example.com/shoes.jpg")
                .build();

        CategoryResponse created = CategoryResponse.builder()
                .id(2L)
                .name("Giày dép")
                .description("Phụ kiện giày dép")
                .imageUrl("https://example.com/shoes.jpg")
                .active(true)
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thêm mới danh mục thành công"))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("Giày dép"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/categories: Tên danh mục rỗng trả về 400 BAD REQUEST do Bean Validation")
    void createCategory_blankName_returns400() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("   ")
                .build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/admin/categories/{id}: Admin cập nhật danh mục thành công trả 200 OK")
    void updateCategory_asAdmin_validRequest_returns200() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Thời trang nam")
                .description("Mô tả mới")
                .active(true)
                .build();

        CategoryResponse updated = CategoryResponse.builder()
                .id(1L)
                .name("Thời trang nam")
                .description("Mô tả mới")
                .active(true)
                .build();

        when(categoryService.updateCategory(eq(1L), any(UpdateCategoryRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật danh mục thành công"))
                .andExpect(jsonPath("$.data.name").value("Thời trang nam"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/admin/categories/{id}: Admin xóa mềm danh mục thành công trả 200 OK")
    void deleteCategory_asAdmin_returns200() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa mềm danh mục thành công"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/admin/categories: Khách hàng (CUSTOMER) bị từ chối 403 FORBIDDEN")
    void createCategory_asCustomer_returns403() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Điện tử").build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void createCategory_unauthenticated_returns401() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Điện tử").build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

