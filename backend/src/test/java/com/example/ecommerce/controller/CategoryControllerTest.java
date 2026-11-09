package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    @DisplayName("GET /api/v1/categories - Lấy danh sách danh mục hoạt động thành công")
    void testGetActiveCategories() throws Exception {
        CategoryResponse cat1 = CategoryResponse.builder()
                .id(1L)
                .name("Thời trang Nam")
                .description("Mô tả danh mục")
                .active(true)
                .build();

        when(categoryService.getActiveCategories()).thenReturn(List.of(cat1));

        mockMvc.perform(get("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Thời trang Nam"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Lấy chi tiết danh mục theo ID thành công")
    void testGetCategoryById() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .id(10L)
                .name("Giày Sneaker")
                .description("Mô tả giày sneaker")
                .active(true)
                .build();

        when(categoryService.getCategoryById(10L)).thenReturn(cat);

        mockMvc.perform(get("/api/v1/categories/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Giày Sneaker"));
    }
}
