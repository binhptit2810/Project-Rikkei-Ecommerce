package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.BestSellerProjection;
import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho DashboardService.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Đỗ Hoàng Anh [DHA]
 *
 * Tiêu chí nghiệm thu (DoD):
 * 1. Tổng doanh thu hôm nay, tuần này, tháng này, năm nay.
 * 2. Tổng số đơn hàng (Chờ xử lý, Đã thanh toán, Đã giao, Đã hủy).
 * 3. Doanh thu theo từng ngày trong tháng phục vụ vẽ biểu đồ (Chart Data).
 * 4. Tỷ lệ thanh toán: COD vs VNPay Online.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

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
    @DisplayName("getDashboardStats: Trả về đầy đủ tổng đơn, doanh thu theo kỳ, biểu đồ ngày, tỷ lệ COD vs VNPay và tồn kho")
    void getDashboardStats_Success() {
        // Mock dữ liệu đơn hàng và doanh thu thực tế
        when(orderRepository.count()).thenReturn(25L);
        when(orderRepository.sumActualRevenue(PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED))
                .thenReturn(BigDecimal.valueOf(18500000));
        when(orderRepository.countByOrderStatus(OrderStatus.PENDING)).thenReturn(4L);
        when(orderRepository.countByOrderStatus(OrderStatus.CONFIRMED)).thenReturn(6L);
        when(orderRepository.countByOrderStatus(OrderStatus.PROCESSING)).thenReturn(5L);
        when(orderRepository.countByOrderStatus(OrderStatus.SHIPPING)).thenReturn(3L);
        when(orderRepository.countByOrderStatus(OrderStatus.COMPLETED)).thenReturn(6L);
        when(orderRepository.countByOrderStatus(OrderStatus.CANCELLED)).thenReturn(1L);
        when(orderRepository.countByPaymentStatusAndOrderStatusNot(PaymentStatus.PAID, OrderStatus.CANCELLED))
                .thenReturn(20L);

        // Mock doanh thu theo kỳ (hôm nay, tuần này, tháng này, năm nay)
        when(orderRepository.sumRevenueBetween(any(), any(), eq(PaymentStatus.PAID), eq(OrderStatus.COMPLETED), eq(OrderStatus.CANCELLED)))
                .thenReturn(BigDecimal.valueOf(2500000));

        // Mock đơn hàng trong tháng cho biểu đồ
        Order orderToday = Order.builder()
                .id(1L)
                .orderCode("ORD-TODAY")
                .totalAmount(BigDecimal.valueOf(500000))
                .paymentStatus(PaymentStatus.PAID)
                .orderStatus(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findOrdersBetween(any(), any())).thenReturn(List.of(orderToday));

        // Mock phương thức thanh toán COD vs VNPay Online
        when(paymentRepository.countByPaymentMethod(PaymentMethod.COD)).thenReturn(15L);
        when(paymentRepository.sumRevenueByPaymentMethod(eq(PaymentMethod.COD), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000000));
        when(paymentRepository.countByPaymentMethod(PaymentMethod.VNPAY)).thenReturn(10L);
        when(paymentRepository.sumRevenueByPaymentMethod(eq(PaymentMethod.VNPAY), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(8500000));
        when(paymentRepository.countByPaymentMethod(PaymentMethod.SIMULATED)).thenReturn(0L);
        when(paymentRepository.sumRevenueByPaymentMethod(eq(PaymentMethod.SIMULATED), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        // Mock dữ liệu sản phẩm và cảnh báo kho
        when(productRepository.count()).thenReturn(50L);
        when(productRepository.countByStockQuantityLessThanEqual(5)).thenReturn(2L);
        when(productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of(lowStock1, lowStock2));

        // Mock đối soát nhật ký kho (ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT)
        when(inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.ORDER_PLACED)).thenReturn(15L);
        when(inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.ORDER_CANCELLED)).thenReturn(2L);
        when(inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.MANUAL_IMPORT)).thenReturn(5L);

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

        // 1. Kiểm tra doanh thu theo kỳ
        assertThat(stats.getRevenueToday()).isEqualByComparingTo(BigDecimal.valueOf(2500000));
        assertThat(stats.getRevenueThisWeek()).isEqualByComparingTo(BigDecimal.valueOf(2500000));
        assertThat(stats.getRevenueThisMonth()).isEqualByComparingTo(BigDecimal.valueOf(2500000));
        assertThat(stats.getRevenueThisYear()).isEqualByComparingTo(BigDecimal.valueOf(2500000));

        // 2. Kiểm tra thống kê đơn hàng
        assertThat(stats.getPendingOrders()).isEqualTo(4L);
        assertThat(stats.getPaidOrders()).isEqualTo(20L);
        assertThat(stats.getDeliveredOrders()).isEqualTo(6L);
        assertThat(stats.getCompletedOrders()).isEqualTo(6L);
        assertThat(stats.getCancelledOrders()).isEqualTo(1L);

        // 3. Kiểm tra biểu đồ doanh thu theo từng ngày trong tháng
        int daysInMonth = YearMonth.now().lengthOfMonth();
        assertThat(stats.getDailyRevenue()).isNotNull().hasSize(daysInMonth);
        int todayDay = LocalDateTime.now().getDayOfMonth();
        DashboardStatsResponse.DailyRevenuePoint todayPoint = stats.getDailyRevenue().get(todayDay - 1);
        assertThat(todayPoint.getDay()).isEqualTo(todayDay);
        assertThat(todayPoint.getOrderCount()).isEqualTo(1L);
        assertThat(todayPoint.getRevenue()).isEqualByComparingTo(BigDecimal.valueOf(500000));

        // 4. Kiểm tra tỷ lệ thanh toán COD vs VNPay
        assertThat(stats.getPaymentDistribution()).isNotNull();
        assertThat(stats.getPaymentDistribution().getCodCount()).isEqualTo(15L);
        assertThat(stats.getPaymentDistribution().getVnpayCount()).isEqualTo(10L);
        assertThat(stats.getPaymentDistribution().getTotalCount()).isEqualTo(25L);
        assertThat(stats.getPaymentDistribution().getCodPercentage()).isEqualTo(60.0);
        assertThat(stats.getPaymentDistribution().getVnpayPercentage()).isEqualTo(40.0);

        // Kiểm tra danh sách sản phẩm nguy cơ hết hàng
        assertThat(stats.getLowStockCount()).isEqualTo(2L);
        assertThat(stats.getLowStockProducts()).hasSize(2);

        // Kiểm tra đối soát nhật ký kho
        assertThat(stats.getOrderPlacedCount()).isEqualTo(15L);
        assertThat(stats.getOrderCancelledCount()).isEqualTo(2L);
        assertThat(stats.getManualImportCount()).isEqualTo(5L);

        // Kiểm tra người dùng
        assertThat(stats.getTotalUsers()).isEqualTo(100L);
        assertThat(stats.getTotalCustomers()).isEqualTo(95L);

        // Kiểm tra các chỉ số tăng trưởng MoM và Top Selling [TSK-D15-DHA]
        assertThat(stats.getRevenueGrowth()).isNotNull();
        assertThat(stats.getOrdersGrowth()).isNotNull();
        assertThat(stats.getNewCustomers()).isNotNull();
        assertThat(stats.getCustomersGrowth()).isNotNull();
        assertThat(stats.getPendingOrdersGrowth()).isNotNull();
        assertThat(stats.getTopSellingProducts()).isNotNull();
        assertThat(stats.getBestSellers()).isNotNull();
        assertThat(stats.getRevenueByDate()).isNotNull();
        assertThat(stats.getCodVsVnpay()).isNotNull();

        verify(orderRepository).count();
        verify(orderRepository).sumActualRevenue(PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        verify(orderRepository).countByOrderStatus(OrderStatus.PENDING);
        verify(paymentRepository).countByPaymentMethod(PaymentMethod.COD);
        verify(paymentRepository).countByPaymentMethod(PaymentMethod.VNPAY);
    }

    @Test
    @DisplayName("getTopSellingProducts: Trả về danh sách top sản phẩm bán chạy có rank và doanh thu")
    void getTopSellingProducts_Success() {
        BestSellerProjection proj = new BestSellerProjection() {
            @Override public Long getProductId() { return 101L; }
            @Override public String getProductName() { return "Áo Polo Nam"; }
            @Override public String getDescription() { return "Áo chất lượng cao"; }
            @Override public BigDecimal getPrice() { return BigDecimal.valueOf(350000); }
            @Override public String getImageUrl() { return "https://example.com/polo.jpg"; }
            @Override public Long getCategoryId() { return 1L; }
            @Override public String getCategoryName() { return "Thời trang Nam"; }
            @Override public Integer getStockQuantity() { return 45; }
            @Override public Boolean getActive() { return true; }
            @Override public Double getRatingAverage() { return 4.9; }
            @Override public Integer getTotalReviews() { return 12; }
            @Override public Long getTotalSold() { return 30L; }
            @Override public BigDecimal getTotalRevenue() { return BigDecimal.valueOf(10500000); }
            @Override public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
            @Override public LocalDateTime getUpdatedAt() { return LocalDateTime.now(); }
        };

        when(orderItemRepository.findBestSellers(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(proj)));

        List<ProductResponse> result = dashboardService.getTopSellingProducts(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(101L);
        assertThat(result.get(0).getName()).isEqualTo("Áo Polo Nam");
        assertThat(result.get(0).getTotalSold()).isEqualTo(30L);
        assertThat(result.get(0).getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10500000));
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("getDashboardStats: Doanh thu null thì mặc định trả về BigDecimal.ZERO")
    void getDashboardStats_NullRevenue_DefaultsToZero() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumActualRevenue(PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED))
                .thenReturn(null);
        when(orderRepository.sumRevenueBetween(any(), any(), any(), any(), any()))
                .thenReturn(null);
        when(orderRepository.findOrdersBetween(any(), any()))
                .thenReturn(List.of());
        when(paymentRepository.countByPaymentMethod(any()))
                .thenReturn(0L);
        when(paymentRepository.sumRevenueByPaymentMethod(any(), any(), any(), any()))
                .thenReturn(null);
        when(productRepository.count()).thenReturn(0L);
        when(productRepository.countByStockQuantityLessThanEqual(5)).thenReturn(0L);
        when(productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(5))
                .thenReturn(List.of());
        when(inventoryTransactionRepository.countByTransactionType(any())).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(0L);

        DashboardStatsResponse stats = dashboardService.getDashboardStats(null);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalOrders()).isEqualTo(0L);
        assertThat(stats.getActualRevenue()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getRevenueToday()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getRevenueThisWeek()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getRevenueThisMonth()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getRevenueThisYear()).isEqualTo(BigDecimal.ZERO);
        assertThat(stats.getPaymentDistribution().getCodPercentage()).isEqualTo(0.0);
        assertThat(stats.getPaymentDistribution().getVnpayPercentage()).isEqualTo(0.0);
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
