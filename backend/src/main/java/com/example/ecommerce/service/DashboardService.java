package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service tổng hợp chỉ số thống kê cho Quản trị viên (Admin Dashboard Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Tiêu chí nghiệm thu (DoD):
 * - API Dashboard trả đủ chỉ số: tổng đơn hàng, doanh thu thực tế, đơn chờ duyệt.
 * - Truy vấn danh sách các sản phẩm có nguy cơ hết hàng (mặc định stock_quantity <= 5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Thu thập toàn bộ chỉ số thống kê tổng quan hệ thống cho Admin Dashboard.
     *
     * @param lowStockThreshold Ngưỡng cảnh báo tồn kho thấp (mặc định <= 5)
     * @return DashboardStatsResponse chứa toàn bộ số liệu thống kê
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Integer lowStockThreshold) {
        int threshold = (lowStockThreshold != null && lowStockThreshold >= 0)
                ? lowStockThreshold
                : DEFAULT_LOW_STOCK_THRESHOLD;

        log.info("Thu thập số liệu thống kê Admin Dashboard (ngưỡng tồn kho <= {})", threshold);

        // 1. Thống kê đơn hàng & Doanh thu thực tế
        long totalOrders = orderRepository.count();
        BigDecimal actualRevenue = orderRepository.sumActualRevenue(PaymentStatus.PAID, OrderStatus.CANCELLED);
        if (actualRevenue == null) {
            actualRevenue = BigDecimal.ZERO;
        }

        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long processingOrders = orderRepository.countByOrderStatus(OrderStatus.PROCESSING);
        long shippingOrders = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long completedOrders = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

        // 2. Thống kê tồn kho & sản phẩm có nguy cơ hết hàng
        long totalProducts = productRepository.count();
        long lowStockCount = productRepository.countByStockQuantityLessThanEqual(threshold);
        List<Product> lowStockEntities = productRepository.findByStockQuantityLessThanEqualOrderByStockQuantityAsc(threshold);
        List<ProductResponse> lowStockProducts = lowStockEntities.stream()
                .map(ProductResponse::fromEntity)
                .toList();

        // 3. Thống kê người dùng
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .actualRevenue(actualRevenue)
                .totalRevenue(actualRevenue)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalProducts(totalProducts)
                .lowStockCount(lowStockCount)
                .lowStockProducts(lowStockProducts)
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

