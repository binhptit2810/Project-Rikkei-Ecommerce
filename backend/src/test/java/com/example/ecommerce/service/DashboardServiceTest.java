package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho DashboardService.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Tiêu chí nghiệm thu (DoD):
 * - API Dashboard trả đủ chỉ số: tổng đơn hàng, doanh thu thực tế, đơn chờ duyệt.
 * - Truy vấn danh sách các sản phẩm có nguy cơ hết hàng (stock_quantity <= 5).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Product lowStock1;
    private Product lowStock2;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Thời trang Nam")
                .build();

        lowStock1 = Product.builder()
                .id(101L)
                .name("Áo Thun Oversize")
                .price(BigDecimal.valueOf(250000))
                .stockQuantity(2)
                .category(category)
                .active(true)
                .build();

        lowStock2 = Product.builder()
                .id(102L)
                .name("Quần Jean Slimfit")
                .price(BigDecimal.valueOf(450000))
                .stockQuantity(5)
                .category(category)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getDashboardStats: Trả về đầy đủ tổng đơn, doanh thu thực tế, đơn chờ duyệt và sản phẩm nguy cơ hết hàng")
    void getDashboardStats_Success() {
        // Mock dữ liệu đơn hàng
        when(orderRepository.count()).thenReturn(25L);
        when(orderRepository.sumActualRevenue(PaymentStatus.PAID, OrderStatus.CANCELLED))
                .thenReturn(BigDecimal.valueOf(18500000));
        when(orderRepository.countByOrderStatus(OrderStatus.PENDING)).thenReturn(4L);
        when(orderRepository.countByOrderStatus(OrderStatus.CONFIRMED)).thenReturn(6L);
        when(orderRepository.countByOrderStatus(OrderStatus.PROCESSING)).thenReturn(5L);
        when(orderRepository.countByOrderStatus(OrderStatus.SHIPPING)).thenReturn(3L);
        when(orderRepository.countByOrderStatus(OrderStatus.COMPLETED)).thenReturn(6L);
        when(orderRepository.countByOrderStatus(OrderStatus.CANCELLED)).thenReturn(1L);

        // Mock dữ liệu sản phẩm và cảnh báo kho
        when(productRepository.count()).thenReturn(50L);
        when(productRepository.countByStockQuantityLessThanEqual(5)).thenReturn(2L);
        when(productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of(lowStock1, lowStock2));

        // Mock dữ liệu người dùng
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(95L);

        // Thực thi
        DashboardStatsResponse stats = dashboardService.getDashboardStats(5);

        // Kiểm tra kết quả
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalOrders()).isEqualTo(25L);
        assertThat(stats.getActualRevenue()).isEqualByComparingTo(BigDecimal.valueOf(18500000));
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(18500000));
        assertThat(stats.getPendingOrders()).isEqualTo(4L);
        assertThat(stats.getConfirmedOrders()).isEqualTo(6L);
        assertThat(stats.getCompletedOrders()).isEqualTo(6L);
        assertThat(stats.getCancelledOrders()).isEqualTo(1L);

        // Kiểm tra danh sách sản phẩm nguy cơ hết hàng
        assertThat(stats.getLowStockCount()).isEqualTo(2L);
        assertThat(stats.getLowStockProducts()).hasSize(2);
        assertThat(stats.getLowStockProducts().get(0).getId()).isEqualTo(101L);
        assertThat(stats.getLowStockProducts().get(0).getStockQuantity()).isEqualTo(2);
        assertThat(stats.getLowStockProducts().get(1).getId()).isEqualTo(102L);
        assertThat(stats.getLowStockProducts().get(1).getStockQuantity()).isEqualTo(5);

        // Kiểm tra người dùng
        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getTotalCustomers()).isEqualTo(95L);

        verify(orderRepository).count();
        verify(orderRepository).sumActualRevenue(PaymentStatus.PAID, OrderStatus.CANCELLED);
        verify(orderRepository).countByOrderStatus(OrderStatus.PENDING);
        verify(productRepository).findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5);
    }

    @Test
    @DisplayName("getDashboardStats: Doanh thu null thì mặc định trả về BigDecimal.ZERO")
    void getDashboardStats_NullRevenue_DefaultsToZero() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumActualRevenue(PaymentStatus.PAID, OrderStatus.CANCELLED))
                .thenReturn(null);
        when(productRepository.count()).thenReturn(0L);
        when(productRepository.countByStockQuantityLessThanEqual(5)).thenReturn(0L);
        when(productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of());
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(0L);

        DashboardStatsResponse stats = dashboardService.getDashboardStats(null);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalOrders()).isEqualTo(0L);
        assertThat(stats.getActualRevenue()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getLowStockProducts()).isEmpty();
    }

    @Test
    @DisplayName("getLowStockProducts: Lấy danh sách sản phẩm tồn kho <= ngưỡng")
    void getLowStockProducts_Success() {
        when(productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of(lowStock1, lowStock2));

        List<ProductResponse> result = dashboardService.getLowStockProducts(5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Áo Thun Oversize");
        assertThat(result.get(1).getName()).isEqualTo("Quần Jean Slimfit");
    }
}

