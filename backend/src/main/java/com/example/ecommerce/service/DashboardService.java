package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service tổng hợp và tính toán các chỉ số kinh doanh phục vụ Bảng điều khiển Quản trị viên (Admin Dashboard Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Tiêu chí nghiệm thu (DoD):
 * 1. Tổng doanh thu hôm nay, tuần này, tháng này, năm nay.
 * 2. Tổng số đơn hàng (Chờ xử lý, Đã thanh toán, Đã giao, Đã hủy).
 * 3. Doanh thu theo từng ngày trong tháng phục vụ vẽ biểu đồ (Chart Data).
 * 4. Tỷ lệ thanh toán: COD vs VNPay Online.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Thu thập toàn bộ chỉ số thống kê kinh doanh và tổng quan hệ thống cho Admin Dashboard.
     *
     * @param lowStockThreshold Ngưỡng cảnh báo tồn kho thấp (mặc định <= 5)
     * @return DashboardStatsResponse chứa toàn bộ số liệu thống kê chuẩn xác
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Integer lowStockThreshold) {
        int threshold = (lowStockThreshold != null && lowStockThreshold >= 0)
                ? lowStockThreshold
                : DEFAULT_LOW_STOCK_THRESHOLD;

        log.info("Thu thập số liệu thống kê Admin Dashboard (ngưỡng tồn kho <= {})", threshold);

        // =========================================================================
        // 1. THỐNG KÊ ĐƠN HÀNG (CHỜ XỬ LÝ, ĐÃ THANH TOÁN, ĐÃ GIAO, ĐÃ HỦY)
        // =========================================================================
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long processingOrders = orderRepository.countByOrderStatus(OrderStatus.PROCESSING);
        long shippingOrders = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long completedOrders = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long deliveredOrders = completedOrders; // Đã giao thành công
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);
        long paidOrders = orderRepository.countByPaymentStatusAndOrderStatusNot(PaymentStatus.PAID, OrderStatus.CANCELLED);

        // =========================================================================
        // 2. THỐNG KÊ DOANH THU THEO KỲ (HÔM NAY, TUẦN NÀY, THÁNG NÀY, NĂM NAY)
        // =========================================================================
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = endOfToday;

        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = endOfToday;

        LocalDateTime startOfYear = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfYear = endOfToday;

        BigDecimal actualRevenue = orderRepository.sumActualRevenue(
                PaymentStatus.PAID,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED
        );
        if (actualRevenue == null) {
            actualRevenue = BigDecimal.ZERO;
        }

        BigDecimal revenueToday = orderRepository.sumRevenueBetween(
                startOfToday, endOfToday, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (revenueToday == null) {
            revenueToday = BigDecimal.ZERO;
        }

        BigDecimal revenueThisWeek = orderRepository.sumRevenueBetween(
                startOfWeek, endOfWeek, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (revenueThisWeek == null) {
            revenueThisWeek = BigDecimal.ZERO;
        }

        BigDecimal revenueThisMonth = orderRepository.sumRevenueBetween(
                startOfMonth, endOfMonth, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (revenueThisMonth == null) {
            revenueThisMonth = BigDecimal.ZERO;
        }

        BigDecimal revenueThisYear = orderRepository.sumRevenueBetween(
                startOfYear, endOfYear, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (revenueThisYear == null) {
            revenueThisYear = BigDecimal.ZERO;
        }

        // =========================================================================
        // 3. DOANH THU THEO TỪNG NGÀY TRONG THÁNG PHỤC VỤ VẼ BIỂU ĐỒ (CHART DATA)
        // =========================================================================
        YearMonth currentYearMonth = YearMonth.from(today);
        int daysInMonth = currentYearMonth.lengthOfMonth();

        Map<Integer, DashboardStatsResponse.DailyRevenuePoint> dailyMap = new LinkedHashMap<>();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = currentYearMonth.atDay(d);
            dailyMap.put(d, DashboardStatsResponse.DailyRevenuePoint.builder()
                    .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .day(d)
                    .revenue(BigDecimal.ZERO)
                    .orderCount(0L)
                    .build());
        }

        LocalDateTime monthStart = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<Order> monthOrders = orderRepository.findOrdersBetween(monthStart, monthEnd);

        if (monthOrders != null) {
            for (Order order : monthOrders) {
                if (order.getCreatedAt() != null) {
                    int day = order.getCreatedAt().getDayOfMonth();
                    DashboardStatsResponse.DailyRevenuePoint point = dailyMap.get(day);
                    if (point != null) {
                        point.setOrderCount(point.getOrderCount() + 1);
                        boolean isValidRevenue = (order.getPaymentStatus() == PaymentStatus.PAID
                                || order.getOrderStatus() == OrderStatus.COMPLETED)
                                && order.getOrderStatus() != OrderStatus.CANCELLED;
                        if (isValidRevenue && order.getTotalAmount() != null) {
                            point.setRevenue(point.getRevenue().add(order.getTotalAmount()));
                        }
                    }
                }
            }
        }
        List<DashboardStatsResponse.DailyRevenuePoint> dailyRevenue = new ArrayList<>(dailyMap.values());

        // =========================================================================
        // 4. TỶ LỆ THANH TOÁN: COD VS VNPAY ONLINE
        // =========================================================================
        long codCount = paymentRepository.countByPaymentMethod(PaymentMethod.COD);
        BigDecimal codRevenue = paymentRepository.sumRevenueByPaymentMethod(
                PaymentMethod.COD, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (codRevenue == null) {
            codRevenue = BigDecimal.ZERO;
        }

        long vnpayCount = paymentRepository.countByPaymentMethod(PaymentMethod.VNPAY);
        BigDecimal vnpayRevenue = paymentRepository.sumRevenueByPaymentMethod(
                PaymentMethod.VNPAY, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (vnpayRevenue == null) {
            vnpayRevenue = BigDecimal.ZERO;
        }

        long simulatedCount = paymentRepository.countByPaymentMethod(PaymentMethod.SIMULATED);
        BigDecimal simulatedRevenue = paymentRepository.sumRevenueByPaymentMethod(
                PaymentMethod.SIMULATED, PaymentStatus.PAID, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        if (simulatedRevenue == null) {
            simulatedRevenue = BigDecimal.ZERO;
        }

        long onlineCount = vnpayCount + simulatedCount;
        BigDecimal onlineRevenue = vnpayRevenue.add(simulatedRevenue);

        long totalPaymentCount = codCount + onlineCount;
        BigDecimal totalPaymentRevenue = codRevenue.add(onlineRevenue);

        double codPercentage = 0.0;
        double vnpayPercentage = 0.0;
        if (totalPaymentCount > 0) {
            codPercentage = Math.round(((double) codCount / totalPaymentCount) * 1000.0) / 10.0;
            vnpayPercentage = Math.round(((double) onlineCount / totalPaymentCount) * 1000.0) / 10.0;
        }

        DashboardStatsResponse.PaymentDistribution paymentDistribution = DashboardStatsResponse.PaymentDistribution.builder()
                .codCount(codCount)
                .codRevenue(codRevenue)
                .codPercentage(codPercentage)
                .vnpayCount(onlineCount)
                .vnpayRevenue(onlineRevenue)
                .vnpayPercentage(vnpayPercentage)
                .totalCount(totalPaymentCount)
                .totalRevenue(totalPaymentRevenue)
                .build();

        // =========================================================================
        // 5. CÁC CHỈ SỐ BỔ TRỢ (TỒN KHO, ĐỐI SOÁT NHẬT KÝ KHO, NGƯỜI DÙNG)
        // =========================================================================
        long totalProducts = productRepository.count();
        long lowStockCount = productRepository.countByStockQuantityLessThanEqual(threshold);
        List<Product> lowStockEntities = productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold);
        List<ProductResponse> lowStockProducts = lowStockEntities.stream()
                .map(ProductResponse::fromEntity)
                .toList();

        long orderPlacedCount = inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.ORDER_PLACED);
        long orderCancelledCount = inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.ORDER_CANCELLED);
        long manualImportCount = inventoryTransactionRepository.countByTransactionType(InventoryTransactionType.MANUAL_IMPORT);

        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .actualRevenue(actualRevenue)
                .totalRevenue(actualRevenue)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .revenueThisYear(revenueThisYear)
                .pendingOrders(pendingOrders)
                .paidOrders(paidOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .confirmedOrders(confirmedOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .dailyRevenue(dailyRevenue)
                .paymentDistribution(paymentDistribution)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .lowStockProducts(lowStockProducts)
                .orderPlacedCount(orderPlacedCount)
                .orderCancelledCount(orderCancelledCount)
                .manualImportCount(manualImportCount)
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .build();
    }

    /**
     * Truy vấn riêng danh sách các sản phẩm có nguy cơ hết hàng.
     *
     * @param lowStockThreshold Ngưỡng tồn kho (mặc định <= 5)
     * @return Danh sách ProductResponse của các sản phẩm có tồn kho <= ngưỡng
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Integer lowStockThreshold) {
        int threshold = (lowStockThreshold != null && lowStockThreshold >= 0)
                ? lowStockThreshold
                : DEFAULT_LOW_STOCK_THRESHOLD;

        return productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold)
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }
}

