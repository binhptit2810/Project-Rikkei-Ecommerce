package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateCategoryRequest;
import com.example.ecommerce.dto.request.UpdateCategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CategoryService (Admin CRUD và Public methods).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Thời trang")
                .description("Mô tả ngành hàng thời trang")
                .imageUrl("https://example.com/cat.jpg")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getActiveCategories: Lấy danh sách danh mục active thành công")
    void getActiveCategories_shouldReturnActiveList() {
        when(categoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(testCategory));

        List<CategoryResponse> result = categoryService.getActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Thời trang");
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    @DisplayName("getAllCategoriesForAdmin: Lấy toàn bộ danh mục cho Admin thành công")
    void getAllCategoriesForAdmin_shouldReturnAllCategories() {
        Category inactiveCategory = Category.builder()
                .id(2L)
                .name("Đồ cũ")
                .active(false)
                .build();

        when(categoryRepository.findAllByOrderByIdAsc()).thenReturn(List.of(testCategory, inactiveCategory));

        List<CategoryResponse> result = categoryService.getAllCategoriesForAdmin();

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getActive()).isFalse();
    }

    @Test
    @DisplayName("createCategory: Thêm mới danh mục thành công khi tên hợp lệ và chưa tồn tại")
    void createCategory_valid_shouldCreateAndReturnCategory() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Điện thoại & Tablet")
                .description("Thiết bị di động")
                .imageUrl("https://example.com/phone.jpg")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Điện thoại & Tablet")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(10L);
            c.setCreatedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Điện thoại & Tablet");
        assertThat(response.getActive()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: Báo lỗi BusinessException khi tên danh mục đã tồn tại")
    void createCategory_duplicateName_shouldThrowBusinessException() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Thời trang")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Thời trang")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tên danh mục đã tồn tại: Thời trang");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory: Cập nhật danh mục thành công")
    void updateCategory_valid_shouldUpdateSuccessfully() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Thời trang nam nữ")
                .description("Thời trang cập nhật")
                .imageUrl("https://example.com/new.jpg")
                .active(true)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Thời trang nam nữ", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Thời trang nam nữ");
        assertThat(response.getDescription()).isEqualTo("Thời trang cập nhật");
        verify(categoryRepository).save(testCategory);
    }

    @Test
    @DisplayName("updateCategory: Báo lỗi BusinessException khi đổi tên danh mục trùng với danh mục khác")
    void updateCategory_duplicateName_shouldThrowBusinessException() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Điện tử")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Điện tử", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tên danh mục đã tồn tại: Điện tử");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory: Báo lỗi ResourceNotFoundException khi ID danh mục không tồn tại")
    void updateCategory_notFound_shouldThrowResourceNotFoundException() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Mới")
                .build();

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Danh mục không tìm thấy với id: 99");
    }

    @Test
    @DisplayName("deleteCategory: Xóa mềm thành công - đổi active = false, không xóa vật lý khỏi CSDL")
    void deleteCategory_success_shouldSoftDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.deleteCategory(1L);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();

        // Khẳng định KHÔNG GỌI lệnh delete() vật lý
        verify(categoryRepository, never()).delete(any(Category.class));
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteCategory: Báo lỗi ResourceNotFoundException khi danh mục không tồn tại")
    void deleteCategory_notFound_shouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Danh mục không tìm thấy với id: 99");

        verify(categoryRepository, never()).save(any());
        verify(categoryRepository, never()).delete(any());
    }
}

