package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CategoryRequest;
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
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_category;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
    @DisplayName("GET /api/v1/admin/categories: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void getAllCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/categories: Admin thêm danh mục thành công trả 201 CREATED")
    void createCategory_asAdmin_returns201() throws Exception {
        CategoryRequest request = CategoryRequest.builder()
                .name("Gia dụng")
                .description("Đồ dùng nhà bếp")
                .active(true)
                .build();

        CategoryResponse response = CategoryResponse.builder()
                .id(3L)
                .name("Gia dụng")
                .description("Đồ dùng nhà bếp")
                .active(true)
                .build();

        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("Gia dụng"));
    }

    @Test
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/admin/categories/3: Admin xóa danh mục thành công trả 200 OK")
    void deleteCategory_asAdmin_returns200() throws Exception {
        doNothing().when(categoryService).deleteCategory(3L);

        mockMvc.perform(delete("/api/v1/admin/categories/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
