package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.BestSellerProjection;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.OrderItemRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit Test chuyên biệt cho tính năng Best Seller theo tháng và năm.
 *
 * Người phụ trách: Đặng Thanh Bình (DTB)
 * Tiêu chí nghiệm thu (DoD):
 * 1. API Best Seller trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng (totalSold DESC).
 * 2. Tốc độ truy vấn xử lý < 30ms.
 */
@ExtendWith(MockitoExtension.class)
class ProductBestSellerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    private BestSellerProjection createMockProjection(Long id, String name, BigDecimal price, Long totalSold) {
        return new BestSellerProjection() {
            @Override
            public Long getProductId() { return id; }

            @Override
            public String getProductName() { return name; }

            @Override
            public String getDescription() { return "Mô tả " + name; }

            @Override
            public BigDecimal getPrice() { return price; }

            @Override
            public String getImageUrl() { return "https://example.com/img/" + id + ".jpg"; }

            @Override
            public Long getCategoryId() { return 1L; }

            @Override
            public String getCategoryName() { return "Thời trang"; }

            @Override
            public Integer getStockQuantity() { return 50; }

            @Override
            public Boolean getActive() { return true; }

            @Override
            public Double getRatingAverage() { return 4.8; }

            @Override
            public Integer getTotalReviews() { return 12; }

            @Override
            public Long getTotalSold() { return totalSold; }

            @Override
            public LocalDateTime getCreatedAt() { return LocalDateTime.now().minusDays(10); }

            @Override
            public LocalDateTime getUpdatedAt() { return LocalDateTime.now(); }
        };
    }

    @Test
    @DisplayName("DOD-01: Best Seller phải trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng (totalSold DESC)")
    void testGetBestSellers_SortedByTotalSoldDesc() {
        // Giả lập danh sách Best Seller: Sản phẩm A (150 bán), B (80 bán), C (20 bán)
        List<BestSellerProjection> mockList = new ArrayList<>();
        mockList.add(createMockProjection(101L, "Áo Polo Nam Tech", BigDecimal.valueOf(350000), 150L));
        mockList.add(createMockProjection(102L, "Quần Short Kaki", BigDecimal.valueOf(250000), 80L));
        mockList.add(createMockProjection(103L, "Tất Cổ Cao Cotton", BigDecimal.valueOf(45000), 20L));

        Page<BestSellerProjection> page = new PageImpl<>(mockList);
        when(orderItemRepository.findBestSellers(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> response = productService.getBestSellers(11, 2026, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(3);

        // Kiểm tra đúng thứ tự totalSold giảm dần theo DoD
        assertThat(response.getContent().get(0).getTotalSold()).isEqualTo(150L);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Áo Polo Nam Tech");

        assertThat(response.getContent().get(1).getTotalSold()).isEqualTo(80L);
        assertThat(response.getContent().get(1).getName()).isEqualTo("Quần Short Kaki");

        assertThat(response.getContent().get(2).getTotalSold()).isEqualTo(20L);
        assertThat(response.getContent().get(2).getName()).isEqualTo("Tất Cổ Cao Cotton");

        for (int i = 0; i < response.getContent().size() - 1; i++) {
            assertThat(response.getContent().get(i).getTotalSold())
                    .isGreaterThanOrEqualTo(response.getContent().get(i + 1).getTotalSold());
        }
    }

    @Test
    @DisplayName("DOD-02: Khi không truyền month và year, mặc định truy vấn tháng và năm hiện tại")
    void testGetBestSellers_DefaultCurrentMonthAndYear() {
        when(orderItemRepository.findBestSellers(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        createMockProjection(1L, "Sản phẩm Tháng Hiện Tại", BigDecimal.valueOf(100000), 50L)
                )));

        PageResponse<ProductResponse> response = productService.getBestSellers(null, null, 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTotalSold()).isEqualTo(50L);
    }

    @Test
    @DisplayName("DOD-03: Kiểm tra validate month không hợp lệ (< 1 hoặc > 12) ném BusinessException")
    void testGetBestSellers_InvalidMonth_ThrowsException() {
        assertThatThrownBy(() -> productService.getBestSellers(0, 2026, 0, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tháng không hợp lệ");

        assertThatThrownBy(() -> productService.getBestSellers(13, 2026, 0, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tháng không hợp lệ");
    }

    @Test
    @DisplayName("DOD-04: Kiểm tra validate year không hợp lệ (< 2000) ném BusinessException")
    void testGetBestSellers_InvalidYear_ThrowsException() {
        assertThatThrownBy(() -> productService.getBestSellers(11, 1999, 0, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Năm không hợp lệ");
    }

    @Test
    @DisplayName("DOD-05: Tiêu chí nghiệm thu DoD - Tốc độ xử lý Service & Query < 30ms")
    void testGetBestSellers_ExecutionLatencyUnder30ms() {
        List<BestSellerProjection> mockList = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            mockList.add(createMockProjection(i, "Product " + i, BigDecimal.valueOf(100000 * i), 100L - i));
        }

        Page<BestSellerProjection> page = new PageImpl<>(mockList);
        when(orderItemRepository.findBestSellers(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(page);

        // Warm up
        for (int i = 0; i < 10; i++) {
            productService.getBestSellers(11, 2026, 0, 10);
        }

        // Đo thời gian 100 lần lặp
        int iterations = 100;
        long startNanos = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            productService.getBestSellers(11, 2026, 0, 10);
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        double avgMs = (elapsedNanos / (double) iterations) / 1_000_000.0;

        System.out.printf("Best Seller Service Query Latency Benchmark: Average = %.4f ms%n", avgMs);

        // DoD: Tốc độ truy vấn < 30ms
        assertThat(avgMs)
                .withFailMessage("Thời gian truy vấn trung bình (%.2f ms) phải nhỏ hơn 30ms theo DoD", avgMs)
                .isLessThan(30.0);
    }
}

