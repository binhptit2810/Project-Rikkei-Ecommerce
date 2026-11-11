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
import org.springframework.data.domain.*;

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
 * Unit Test chuyên sâu cho ProductService.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D08-DDM] Tối ưu hóa Bộ lọc Tìm kiếm Sản phẩm & Viết Unit Tests
 * ProductService
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private com.example.ecommerce.repository.CouponRepository couponRepository;

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
    // [TSK-D08-DDM] Bộ lọc tìm kiếm đa tiêu chí & Chi tiết sản phẩm
    // =========================================================================

    @Test
    @DisplayName("getProducts_WithKeywordAndCategory_ReturnsFilteredPage: Tìm kiếm kết hợp keyword + categoryId")
    void getProducts_WithKeywordAndCategory_ReturnsFilteredPage() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(eq("áo"), eq(1L), isNull(), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts("áo", 1L, null, null, 0, 10, "name", "asc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Áo thun nam basic");
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Thời trang");
        assertThat(result.getContent().get(0).getStockQuantity()).isEqualTo(50);
        verify(productRepository).searchProducts(eq("áo"), eq(1L), isNull(), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("getProducts_SortByPriceAsc_ReturnsSortedPage: Sắp xếp theo giá tăng dần")
    void getProducts_SortByPriceAsc_ReturnsSortedPage() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.searchProducts(any(), any(), any(), any(), eq(true), pageableCaptor.capture()))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts(null, null, null, null, 0, 10, "price",
                "asc");

        assertThat(result).isNotNull();
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("price")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("getProducts_SortByPriceDesc_ReturnsSortedPage: Sắp xếp theo giá giảm dần")
    void getProducts_SortByPriceDesc_ReturnsSortedPage() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(productRepository.searchProducts(any(), any(), any(), any(), eq(true), pageableCaptor.capture()))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts(null, null, null, null, 0, 10, "price",
                "desc");

        assertThat(result).isNotNull();
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("price")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("getProducts_FilterByPriceRange_ReturnsMatchingProducts: Lọc theo khoảng giá min/max")
    void getProducts_FilterByPriceRange_ReturnsMatchingProducts() {
        BigDecimal minPrice = new BigDecimal("100000");
        BigDecimal maxPrice = new BigDecimal("300000");
        Page<Product> page = new PageImpl<>(List.of(testProduct));

        when(productRepository.searchProducts(isNull(), isNull(), eq(minPrice), eq(maxPrice), eq(true),
                any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getProducts(null, null, minPrice, maxPrice, 0, 10,
                "price", "asc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPrice()).isGreaterThanOrEqualTo(minPrice);
        assertThat(result.getContent().get(0).getPrice()).isLessThanOrEqualTo(maxPrice);
        verify(productRepository).searchProducts(isNull(), isNull(), eq(minPrice), eq(maxPrice), eq(true),
                any(Pageable.class));
    }

    @Test
    @DisplayName("getProducts_EmptyResult_ReturnsEmptyPage: Tìm kiếm không có kết quả trả về Page rỗng hợp lệ")
    void getProducts_EmptyResult_ReturnsEmptyPage() {
        // Sửa tại đây: truyền Pageable kèm total = 0
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productRepository.searchProducts(any(), any(), any(), any(), eq(true), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<ProductResponse> result = productService.getProducts("khong_tim_thay_xyz", 999L, null, null, 0, 10,
                "createdAt", "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("getProductById_Success: Tìm chi tiết sản phẩm theo ID thành công")
    void getProductById_Success() {
        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getProductById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Áo thun nam basic");
        assertThat(response.getPrice()).isEqualByComparingTo("199000");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("getProductById_NotFound_ThrowsResourceNotFoundException: Ném ngoại lệ 404 chuẩn khi ID không tồn tại")
    void getProductById_NotFound_ThrowsResourceNotFoundException() {
        when(productRepository.findByIdWithCategory(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 999");
    }

    @Test
    @DisplayName("getProductById: Báo lỗi khi sản phẩm bị ẩn (active = false) đối với khách hàng")
    void getProductById_inactive_shouldThrowException() {
        testProduct.setActive(false);
        when(productRepository.findByIdWithCategory(10L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.getProductById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tìm thấy với id: 10");
    }

    // =========================================================================
    // [TSK-D06-DDM] Admin CRUD Tests
    // =========================================================================

    @Test
    @DisplayName("getAdminProducts: Lấy danh sách sản phẩm quản trị (kể cả active và inactive)")
    void getAdminProducts_shouldReturnPagedProducts() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(eq("áo"), eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> result = productService.getAdminProducts("áo", 1L, null, 0, 10, "createdAt",
                "desc");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchProducts(eq("áo"), eq(1L), isNull(), isNull(), isNull(), any(Pageable.class));
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

    @Test
    @DisplayName("getDealOfTheDay: Trả về deal sản phẩm kèm tính giá gốc và % giảm chính xác")
    void getDealOfTheDay_success() {
        Category electronics = Category.builder().id(2L).name("Điện tử").build();
        Product dealItem = Product.builder()
                .id(10L)
                .name("Tai nghe Hi-Fi Không Dây")
                .description("Chống ồn ANC")
                .price(new BigDecimal("1200000"))
                .category(electronics)
                .stockQuantity(15)
                .ratingAverage(4.8)
                .totalReviews(120)
                .active(true)
                .build();

        when(productRepository.findDealCandidates(any(Pageable.class))).thenReturn(List.of(dealItem));
        when(couponRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());

        var deal = productService.getDealOfTheDay();

        assertThat(deal).isNotNull();
        assertThat(deal.getId()).isEqualTo(10L);
        assertThat(deal.getName()).isEqualTo("Tai nghe Hi-Fi Không Dây");
        assertThat(deal.getSalePrice()).isEqualByComparingTo("1200000");
        assertThat(deal.getOriginalPrice()).isGreaterThan(deal.getSalePrice());
        assertThat(deal.getDiscountPercentage()).isEqualTo(25);
    }

    @Test
    @DisplayName("getDealOfTheDay: Trả về null khi không có sản phẩm nào khả dụng")
    void getDealOfTheDay_empty_returnsNull() {
        when(productRepository.findDealCandidates(any(Pageable.class))).thenReturn(List.of());

        var deal = productService.getDealOfTheDay();

        assertThat(deal).isNull();
    }

    @Test
    @DisplayName("getDealOfTheDay: Tìm thấy deal kèm mã giảm giá hợp lệ từ CouponRepository")
    void getDealOfTheDay_withPromoCoupon() {
        Category other = Category.builder().id(1L).name("Thời trang").build();
        Product dealItem = Product.builder()
                .id(20L)
                .name("Áo thun")
                .price(new BigDecimal("200000"))
                .category(other)
                .stockQuantity(10)
                .active(true)
                .build();
        com.example.ecommerce.entity.Coupon promo = com.example.ecommerce.entity.Coupon.builder()
                .code("SALE30")
                .discountType(com.example.ecommerce.entity.DiscountType.PERCENT)
                .discountValue(new BigDecimal("30"))
                .expireAt(LocalDateTime.now().plusDays(2))
                .usedCount(5)
                .usageLimit(100)
                .active(true)
                .build();

        when(productRepository.findDealCandidates(any(Pageable.class))).thenReturn(List.of(dealItem));
        when(couponRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(promo));

        var deal = productService.getDealOfTheDay();

        assertThat(deal).isNotNull();
        assertThat(deal.getCouponCode()).isEqualTo("SALE30");
        assertThat(deal.getDiscountPercentage()).isEqualTo(30);
    }
}
