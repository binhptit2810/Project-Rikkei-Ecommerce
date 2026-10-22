package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Thời trang")
                .description("Quần áo thời trang")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getActiveCategories: Trả về danh sách các danh mục đang hoạt động")
    void getActiveCategories_shouldReturnList() {
        when(categoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(testCategory));

        List<CategoryResponse> list = categoryService.getActiveCategories();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Thời trang");
        verify(categoryRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    @DisplayName("getCategoryById: Lấy chi tiết danh mục thành công")
    void getCategoryById_existing_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Thời trang");
    }

    @Test
    @DisplayName("createCategory: Tạo danh mục mới thành công")
    void createCategory_validData_shouldSaveAndReturn() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Điện tử")
                .description("Thiết bị điện tử")
                .active(true)
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Điện tử")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("Điện tử");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: Báo lỗi khi tên danh mục đã tồn tại")
    void createCategory_duplicateName_shouldThrowBusinessException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Thời trang")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Thời trang")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tên danh mục đã tồn tại: Thời trang");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCategory: Soft-delete khi danh mục đang có sản phẩm")
    void deleteCategory_withProducts_shouldSoftDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        categoryService.deleteCategory(1L);

        assertThat(testCategory.getActive()).isFalse();
        verify(categoryRepository).save(testCategory);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCategory: Xóa hẳn khi danh mục chưa có sản phẩm nào")
    void deleteCategory_noProducts_shouldDeletePermanently() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(testCategory);
    }
}
