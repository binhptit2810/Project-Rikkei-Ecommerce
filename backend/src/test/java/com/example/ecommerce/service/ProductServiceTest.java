package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
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

    @Test
    @DisplayName("createProduct: Thêm sản phẩm mới thành công với danh mục và tồn kho hợp lệ")
    void createProduct_validData_shouldSaveAndReturnProduct() {
        ProductRequest request = ProductRequest.builder()
                .name("Quần jean nam")
                .description("Co giãn 4 chiều")
                .price(new BigDecimal("450000"))
                .imageUrl("https://example.com/jeans.jpg")
                .categoryId(1L)
                .stockQuantity(30)
                .active(true)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(20L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getName()).isEqualTo("Quần jean nam");
        assertThat(response.getStockQuantity()).isEqualTo(30);
        assertThat(response.getCategoryId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: Báo lỗi khi giá sản phẩm âm")
    void createProduct_negativePrice_shouldThrowBusinessException() {
        ProductRequest request = ProductRequest.builder()
                .name("Sản phẩm giá âm")
                .price(new BigDecimal("-1000"))
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Giá sản phẩm phải lớn hơn hoặc bằng 0");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct: Báo lỗi khi danh mục không tồn tại")
    void createProduct_categoryNotFound_shouldThrowResourceNotFoundException() {
        ProductRequest request = ProductRequest.builder()
                .name("Sản phẩm mới")
                .price(new BigDecimal("100000"))
                .categoryId(999L)
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Danh mục không tìm thấy với id: 999");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProduct: Cập nhật thông tin sản phẩm và tồn kho thành công")
    void updateProduct_validData_shouldUpdateFields() {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Áo thun nam basic Premium")
                .description("Cải tiến vải co giãn")
                .price(new BigDecimal("220000"))
                .categoryId(1L)
                .stockQuantity(60)
                .active(true)
                .build();

        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(10L, updateRequest);

        assertThat(response.getName()).isEqualTo("Áo thun nam basic Premium");
        assertThat(response.getPrice()).isEqualByComparingTo("220000");
        assertThat(response.getStockQuantity()).isEqualTo(60);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("deleteProduct: Xóa mềm sản phẩm (chuyển active = false)")
    void deleteProduct_existingProduct_shouldSoftDelete() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        productService.deleteProduct(10L);

        assertThat(testProduct.getActive()).isFalse();
        verify(productRepository).save(testProduct);
    }
}
