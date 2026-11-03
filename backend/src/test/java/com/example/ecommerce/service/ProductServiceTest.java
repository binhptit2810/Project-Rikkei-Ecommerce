package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
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
import org.mockito.ArgumentCaptor;
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

/**
 * Unit Test cho ProductService.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
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

    // =========================================================================
    // Public Read-Only Tests
    // =========================================================================

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

    // =========================================================================
    // Admin CRUD Tests
    // =========================================================================

    @Test
    @DisplayName("getAdminProducts: Lấy danh sách sản phẩm quản trị (kể cả active và inactive)")
    void getAdminProducts_shouldReturnPagedProducts() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(eq("áo"), eq(1L), eq(null), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getAdminProducts("áo", 1L, null, 0, 10, "createdAt", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchProducts(eq("áo"), eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("getAdminProductById: Admin xem chi tiết sản phẩm kể cả khi active = false")
    void getAdminProductById_inactiveProduct_shouldReturnProduct() {
        testProduct.setActive(false);
        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getAdminProductById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getActive()).isFalse();
    }

    @Test
    @DisplayName("createProduct: Thêm mới sản phẩm thành công khi danh mục tồn tại và active")
    void createProduct_validRequest_shouldCreateAndReturnProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Quần Jean Nam Slimfit")
                .description("Vải denim co giãn")
                .price(new BigDecimal("350000"))
                .imageUrl("https://example.com/jean.jpg")
                .categoryId(1L)
                .stockQuantity(100)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(20L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getName()).isEqualTo("Quần Jean Nam Slimfit");
        assertThat(response.getPrice()).isEqualByComparingTo("350000");
        assertThat(response.getStockQuantity()).isEqualTo(100);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getCategoryId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: Báo lỗi ResourceNotFoundException khi danh mục không tồn tại")
    void createProduct_categoryNotFound_shouldThrowResourceNotFoundException() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Quần Jean Nam")
                .price(new BigDecimal("350000"))
                .categoryId(999L)
                .stockQuantity(100)
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Danh mục không tìm thấy với id: 999");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: Báo lỗi BusinessException khi danh mục đã bị vô hiệu hóa (active = false)")
    void createProduct_categoryInactive_shouldThrowBusinessException() {
        testCategory.setActive(false);
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Áo Polo")
                .price(new BigDecimal("250000"))
                .categoryId(1L)
                .stockQuantity(20)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct: Cập nhật thông tin sản phẩm thành công")
    void updateProduct_validRequest_shouldUpdateSuccessfully() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Áo thun nam basic v2")
                .description("Mô tả mới")
                .price(new BigDecimal("219000"))
                .imageUrl("https://example.com/new.jpg")
                .categoryId(1L)
                .stockQuantity(80)
                .active(true)
                .build();

        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.updateProduct(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Áo thun nam basic v2");
        assertThat(response.getPrice()).isEqualByComparingTo("219000");
        assertThat(response.getStockQuantity()).isEqualTo(80);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("updateProduct: Báo lỗi ResourceNotFoundException khi sản phẩm không tồn tại")
    void updateProduct_productNotFound_shouldThrowResourceNotFoundException() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Áo thun")
                .price(new BigDecimal("200000"))
                .categoryId(1L)
                .stockQuantity(10)
                .build();

        when(productRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 999");
    }

    @Test
    @DisplayName("updateProduct: Báo lỗi BusinessException khi danh mục cập nhật bị vô hiệu hóa")
    void updateProduct_categoryInactive_shouldThrowBusinessException() {
        Category inactiveCat = Category.builder().id(2L).name("Cũ").active(false).build();
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Áo thun")
                .price(new BigDecimal("200000"))
                .categoryId(2L)
                .stockQuantity(10)
                .build();

        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(inactiveCat));

        assertThatThrownBy(() -> productService.updateProduct(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("deleteProduct: Xóa mềm thành công - đổi active = false, không xóa vật lý khỏi CSDL")
    void deleteProduct_success_shouldSoftDelete() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(10L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();

        // Khẳng định KHÔNG GỌI lệnh delete() vật lý
        verify(productRepository, never()).delete(any(Product.class));
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteProduct: Báo lỗi ResourceNotFoundException khi sản phẩm không tồn tại")
    void deleteProduct_notFound_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 999");

        verify(productRepository, never()).save(any());
        verify(productRepository, never()).delete(any());
    }
}
