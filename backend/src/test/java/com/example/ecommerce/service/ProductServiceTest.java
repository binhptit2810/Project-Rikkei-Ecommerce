package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category testCategory;
    private Product testProduct;

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

        testProduct = Product.builder()
                .id(10L)
                .name("Áo thun nam basic")
                .description("Chất liệu cotton 100%")
                .price(new BigDecimal("199000"))
                .imageUrl("https://example.com/aothun.jpg")
                .category(testCategory)
                .stockQuantity(50)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getProducts: Tìm kiếm và lọc sản phẩm thành công kèm phân trang")
    void getProducts_validParams_shouldReturnPagedProducts() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(eq("áo"), eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts("áo", 1L, 0, 10, "price", "asc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Áo thun nam basic");
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Thời trang");
        assertThat(result.getContent().get(0).getStockQuantity()).isEqualTo(50);
        verify(productRepository).searchProducts(eq("áo"), eq(1L), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("getProductById: Lấy chi tiết sản phẩm thành công khi sản phẩm tồn tại và đang active")
    void getProductById_existingAndActive_shouldReturnProduct() {
        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getProductById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Áo thun nam basic");
        assertThat(response.getPrice()).isEqualByComparingTo("199000");
    }

    @Test
    @DisplayName("getProductById: Báo lỗi khi sản phẩm không tồn tại")
    void getProductById_notFound_shouldThrowException() {
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 99");
    }

    @Test
    @DisplayName("getProductById: Báo lỗi khi sản phẩm bị ẩn (active = false)")
    void getProductById_inactive_shouldThrowException() {
        testProduct.setActive(false);
        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.getProductById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 10");
    }
}
