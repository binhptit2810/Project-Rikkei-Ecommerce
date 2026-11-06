package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử đối soát và xác thực tính chính xác số liệu Dashboard so với CSDL thực tế.
 * Người phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Người phối hợp: Đỗ Hoàng Anh [DHA]
 *
 * Tiêu chí nghiệm thu (DoD):
 * 1. Kiểm tra tính chính xác của số liệu Dashboard thống kê so với truy vấn CSDL thực tế (100% khớp).
 * 2. Xác nhận thống kê doanh thu CHỈ tính trên các đơn hàng đã thanh toán (PAID) hoặc hoàn thành (COMPLETED).
 * 3. Đối soát các bản ghi nhật ký kho: ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_dashboard_reconciliation;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
@Transactional
class DashboardDatabaseReconciliationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private User customer;
    private User adminUser;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Xóa sạch dữ liệu test để đối soát độc lập
        inventoryTransactionRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .email("admin.reconcile@ecommerce.com")
                .password("$2a$12$dummyhashforreconciliationtest")
                .fullName("Admin Đối Soát")
                .role(Role.ADMIN)
                .active(true)
                .build());

        customer = userRepository.save(User.builder()
                .email("customer.reconcile@ecommerce.com")
                .password("$2a$12$dummyhashforreconciliationtest")
                .fullName("Khách Hàng Đối Soát")
                .role(Role.CUSTOMER)
                .active(true)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Thời Trang Nam Test")
                .active(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Áo Polo Đối Soát")
                .price(BigDecimal.valueOf(200000))
                .stockQuantity(50)
                .category(testCategory)
                .active(true)
                .build());
    }

    /**
     * Yêu cầu 1: Xác nhận thống kê doanh thu CHỈ tính trên các đơn hàng đã thanh toán (PAID) HOẶC hoàn thành (COMPLETED).
     * Tuyệt đối không tính các đơn hàng:
     * - UNPAID & PENDING / CONFIRMED / PROCESSING / SHIPPING (chưa thu tiền)
     * - FAILED (thanh toán thất bại)
     * - CANCELLED / REFUNDED (đã hủy / đã hoàn tiền)
     */
    @Test
    @DisplayName("Doanh thu thực tế: CHỈ tính trên các đơn hàng đã thanh toán (PAID) HOẶC hoàn thành (COMPLETED)")
    void testRevenueAccuracy_OnlyPaidOrCompleted() {
        // 1. Các đơn HỢP LỆ tính vào doanh thu thực tế:
        // Đơn 1: Đã thanh toán trực tuyến PAID, đang CONFIRMED (1,000,000)
        createTestOrder("ORD-PAID-CONF", BigDecimal.valueOf(1000000), OrderStatus.CONFIRMED, PaymentStatus.PAID);

        // Đơn 2: Đã thanh toán trực tuyến PAID, đang PROCESSING (2,500,000)
        createTestOrder("ORD-PAID-PROC", BigDecimal.valueOf(2500000), OrderStatus.PROCESSING, PaymentStatus.PAID);

        // Đơn 3: Đã thanh toán trực tuyến PAID, đang SHIPPING (3,000,000)
        createTestOrder("ORD-PAID-SHIP", BigDecimal.valueOf(3000000), OrderStatus.SHIPPING, PaymentStatus.PAID);

        // Đơn 4: Đã thanh toán trực tuyến PAID, đã COMPLETED (4,000,000)
        createTestOrder("ORD-PAID-COMP", BigDecimal.valueOf(4000000), OrderStatus.COMPLETED, PaymentStatus.PAID);

        // Đơn 5: Đơn COD giao thành công COMPLETED, thu tiền tại chỗ (500,000) -> Được tính vì đã hoàn thành!
        createTestOrder("ORD-COD-COMP", BigDecimal.valueOf(500000), OrderStatus.COMPLETED, PaymentStatus.UNPAID);

        // Tổng doanh thu thực tế kỳ vọng = 1,000,000 + 2,500,000 + 3,000,000 + 4,000,000 + 500,000 = 11,000,000
        BigDecimal expectedRevenue = BigDecimal.valueOf(11000000).setScale(2, RoundingMode.HALF_UP);

        // 2. Các đơn KHÔNG ĐƯỢC TÍNH vào doanh thu:
        // Đơn 6: COD mới tạo PENDING, chưa thanh toán (1,200,000) -> KHÔNG TÍNH
        createTestOrder("ORD-UNP-PEND", BigDecimal.valueOf(1200000), OrderStatus.PENDING, PaymentStatus.UNPAID);

        // Đơn 7: COD đang CONFIRMED, chưa thanh toán (800,000) -> KHÔNG TÍNH
        createTestOrder("ORD-UNP-CONF", BigDecimal.valueOf(800000), OrderStatus.CONFIRMED, PaymentStatus.UNPAID);

        // Đơn 8: Thanh toán trực tuyến thất bại FAILED (700,000) -> KHÔNG TÍNH
        createTestOrder("ORD-PAY-FAIL", BigDecimal.valueOf(700000), OrderStatus.PENDING, PaymentStatus.FAILED);

        // Đơn 9: Đã hủy và hoàn tiền CANCELLED / REFUNDED (1,500,000) -> KHÔNG TÍNH
        createTestOrder("ORD-CANC-REF", BigDecimal.valueOf(1500000), OrderStatus.CANCELLED, PaymentStatus.REFUNDED);

        // Đơn 10: Đã hủy khi chưa thanh toán CANCELLED / UNPAID (600,000) -> KHÔNG TÍNH
        createTestOrder("ORD-CANC-UNP", BigDecimal.valueOf(600000), OrderStatus.CANCELLED, PaymentStatus.UNPAID);

        // 3. Thực thi lấy thống kê Dashboard
        DashboardStatsResponse stats = dashboardService.getDashboardStats(5);

        // 4. Đối soát trực tiếp với truy vấn cơ sở dữ liệu MySQL
        BigDecimal dbQueryRevenue = (BigDecimal) entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
                "WHERE (payment_status = 'PAID' OR order_status = 'COMPLETED') " +
                "AND order_status <> 'CANCELLED'"
        ).getSingleResult();

        assertThat(stats.getActualRevenue())
                .as("Doanh thu thực tế trả về từ Service phải chính xác tuyệt đối với truy vấn CSDL")
                .isEqualByComparingTo(expectedRevenue)
                .isEqualByComparingTo(dbQueryRevenue);

        assertThat(stats.getTotalRevenue())
                .as("Trường bí danh totalRevenue phải đồng nhất với actualRevenue")
                .isEqualByComparingTo(expectedRevenue);

        // Đảm bảo tổng số đơn là 10
        assertThat(stats.getTotalOrders()).isEqualTo(10L);
    }

    /**
     * Yêu cầu 2: Đối soát các bản ghi nhật ký kho ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT.
     * Kiểm tra:
     * - Khi đặt hàng: trừ kho và ghi nhận ORDER_PLACED (changeQuantity < 0).
     * - Khi hủy đơn: hoàn kho và ghi nhận ORDER_CANCELLED (changeQuantity > 0).
     * - Khi admin nhập kho: tăng kho và ghi nhận MANUAL_IMPORT (changeQuantity > 0).
     * - Công thức đối soát tồn kho: Tồn hiện tại = Tồn ban đầu + SUM(changeQuantity).
     */
    @Test
    @DisplayName("Đối soát nhật ký kho: ORDER_PLACED (trừ kho), ORDER_CANCELLED (hoàn kho), MANUAL_IMPORT (nhập kho)")
    void testInventoryAuditReconciliation_OrderPlaced_OrderCancelled_ManualImport() {
        int initialStock = testProduct.getStockQuantity(); // 50
        assertThat(initialStock).isEqualTo(50);

        // Bước 1: Khách đặt hàng mua 8 sản phẩm (ORDER_PLACED)
        Order order = createOrderWithItems("ORD-AUDIT-01", testProduct, 8);
        inventoryService.deductStockForOrder(order);

        // Kiểm tra tồn kho sau đặt hàng: 50 - 8 = 42
        Product productAfterOrder = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productAfterOrder.getStockQuantity()).isEqualTo(42);

        // Đối soát bản ghi ORDER_PLACED
        List<InventoryTransaction> orderPlacedLogs = inventoryTransactionRepository.findByOrderId(order.getId());
        assertThat(orderPlacedLogs).hasSize(1);
        InventoryTransaction placedTx = orderPlacedLogs.get(0);
        assertThat(placedTx.getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_PLACED);
        assertThat(placedTx.getChangeQuantity()).isEqualTo(-8);
        assertThat(placedTx.getStockAfter()).isEqualTo(42);

        // Bước 2: Hủy đơn hàng và hoàn lại tồn kho (ORDER_CANCELLED)
        inventoryService.restoreStockForOrder(order);

        // Kiểm tra tồn kho sau hoàn: 42 + 8 = 50
        Product productAfterCancel = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productAfterCancel.getStockQuantity()).isEqualTo(50);

        // Đối soát bản ghi ORDER_CANCELLED
        List<InventoryTransaction> allOrderLogs = inventoryTransactionRepository.findByOrderId(order.getId());
        assertThat(allOrderLogs).hasSize(2);
        InventoryTransaction cancelTx = allOrderLogs.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.ORDER_CANCELLED)
                .findFirst()
                .orElseThrow();
        assertThat(cancelTx.getChangeQuantity()).isEqualTo(8);
        assertThat(cancelTx.getStockAfter()).isEqualTo(50);

        // Bước 3: Admin nhập kho thủ công thêm 25 sản phẩm (MANUAL_IMPORT)
        AdjustInventoryRequest importReq = AdjustInventoryRequest.builder()
                .changeQuantity(25)
                .type(InventoryTransactionType.MANUAL_IMPORT)
                .reason("Nhập kho bổ sung đầu tháng 11")
                .build();
        inventoryService.adjustStock(testProduct.getId(), importReq, adminUser.getId());

        // Kiểm tra tồn kho sau nhập: 50 + 25 = 75
        Product productAfterImport = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productAfterImport.getStockQuantity()).isEqualTo(75);

        // Đối soát bản ghi MANUAL_IMPORT
        List<InventoryTransaction> productLogs = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(testProduct.getId());
        InventoryTransaction importTx = productLogs.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.MANUAL_IMPORT)
                .findFirst()
                .orElseThrow();
        assertThat(importTx.getChangeQuantity()).isEqualTo(25);
        assertThat(importTx.getStockAfter()).isEqualTo(75);
        assertThat(importTx.getNote()).contains("Nhập kho bổ sung đầu tháng 11");

        // Bước 4: Kiểm tra công thức đối soát toàn vẹn tồn kho:
        // Tồn kho thực tế = Tồn kho ban đầu + SUM(changeQuantity)
        Integer totalChange = inventoryTransactionRepository.sumChangeQuantityByProductId(testProduct.getId());
        // totalChange = (-8) + (+8) + (+25) = 25
        assertThat(totalChange).isEqualTo(25);
        assertThat(initialStock + totalChange).isEqualTo(productAfterImport.getStockQuantity());

        // Bước 5: Kiểm tra số liệu đối soát phản ánh chính xác trên DashboardStatsResponse
        DashboardStatsResponse stats = dashboardService.getDashboardStats(5);
        assertThat(stats.getOrderPlacedCount()).isGreaterThanOrEqualTo(1L);
        assertThat(stats.getOrderCancelledCount()).isGreaterThanOrEqualTo(1L);
        assertThat(stats.getManualImportCount()).isGreaterThanOrEqualTo(1L);
    }

    /**
     * Yêu cầu 3: Kiểm tra tính chính xác của số liệu Dashboard thống kê so với truy vấn CSDL MySQL thực tế.
     * Đối soát các chỉ số:
     * - Tổng đơn hàng (totalOrders)
     * - Đơn chờ duyệt (pendingOrders)
     * - Danh sách sản phẩm có nguy cơ hết hàng (stock_quantity <= 5)
     */
    @Test
    @DisplayName("Kiểm tra tính chính xác của toàn bộ chỉ số Dashboard so với truy vấn CSDL MySQL")
    void testDashboardStatsAccuracy_AgainstDatabase() {
        // Tạo các đơn với nhiều trạng thái
        createTestOrder("ORD-STAT-01", BigDecimal.valueOf(200000), OrderStatus.PENDING, PaymentStatus.UNPAID);
        createTestOrder("ORD-STAT-02", BigDecimal.valueOf(300000), OrderStatus.PENDING, PaymentStatus.UNPAID);
        createTestOrder("ORD-STAT-03", BigDecimal.valueOf(400000), OrderStatus.CONFIRMED, PaymentStatus.PAID);
        createTestOrder("ORD-STAT-04", BigDecimal.valueOf(500000), OrderStatus.PROCESSING, PaymentStatus.PAID);
        createTestOrder("ORD-STAT-05", BigDecimal.valueOf(600000), OrderStatus.SHIPPING, PaymentStatus.PAID);
        createTestOrder("ORD-STAT-06", BigDecimal.valueOf(700000), OrderStatus.COMPLETED, PaymentStatus.PAID);
        createTestOrder("ORD-STAT-07", BigDecimal.valueOf(800000), OrderStatus.CANCELLED, PaymentStatus.REFUNDED);

        // Tạo các sản phẩm với số lượng tồn kho khác nhau
        Product pOutOfStock = productRepository.save(Product.builder()
                .name("Sản phẩm hết hàng")
                .price(BigDecimal.valueOf(100000))
                .stockQuantity(0) // <= 5
                .category(testCategory)
                .active(true)
                .build());

        Product pLowStock1 = productRepository.save(Product.builder()
                .name("Sản phẩm còn 2 cái")
                .price(BigDecimal.valueOf(150000))
                .stockQuantity(2) // <= 5
                .category(testCategory)
                .active(true)
                .build());

        Product pLowStock2 = productRepository.save(Product.builder()
                .name("Sản phẩm còn 5 cái")
                .price(BigDecimal.valueOf(180000))
                .stockQuantity(5) // <= 5
                .category(testCategory)
                .active(true)
                .build());

        Product pSafeStock = productRepository.save(Product.builder()
                .name("Sản phẩm còn 20 cái")
                .price(BigDecimal.valueOf(250000))
                .stockQuantity(20) // > 5
                .category(testCategory)
                .active(true)
                .build());

        // Lấy thống kê qua Service
        DashboardStatsResponse stats = dashboardService.getDashboardStats(5);

        // 1. Đối soát số lượng đơn hàng
        long dbTotalOrders = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM orders").getSingleResult()).longValue();
        long dbPendingOrders = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM orders WHERE order_status = 'PENDING'").getSingleResult()).longValue();
        long dbConfirmedOrders = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM orders WHERE order_status = 'CONFIRMED'").getSingleResult()).longValue();
        long dbCompletedOrders = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM orders WHERE order_status = 'COMPLETED'").getSingleResult()).longValue();
        long dbCancelledOrders = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM orders WHERE order_status = 'CANCELLED'").getSingleResult()).longValue();

        assertThat(stats.getTotalOrders()).isEqualTo(dbTotalOrders).isEqualTo(7L);
        assertThat(stats.getPendingOrders()).isEqualTo(dbPendingOrders).isEqualTo(2L);
        assertThat(stats.getConfirmedOrders()).isEqualTo(dbConfirmedOrders).isEqualTo(1L);
        assertThat(stats.getCompletedOrders()).isEqualTo(dbCompletedOrders).isEqualTo(1L);
        assertThat(stats.getCancelledOrders()).isEqualTo(dbCancelledOrders).isEqualTo(1L);

        // 2. Đối soát sản phẩm có nguy cơ hết hàng (stock_quantity <= 5)
        long dbLowStockCount = ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM products WHERE stock_quantity <= 5"
        ).getSingleResult()).longValue();

        assertThat(stats.getLowStockCount()).isEqualTo(dbLowStockCount).isEqualTo(3L);
        assertThat(stats.getLowStockProducts()).hasSize(3);

        // Đảm bảo danh sách được sắp xếp theo tồn kho tăng dần: 0 -> 2 -> 5
        assertThat(stats.getLowStockProducts().get(0).getId()).isEqualTo(pOutOfStock.getId());
        assertThat(stats.getLowStockProducts().get(0).getStockQuantity()).isEqualTo(0);
        assertThat(stats.getLowStockProducts().get(1).getId()).isEqualTo(pLowStock1.getId());
        assertThat(stats.getLowStockProducts().get(1).getStockQuantity()).isEqualTo(2);
        assertThat(stats.getLowStockProducts().get(2).getId()).isEqualTo(pLowStock2.getId());
        assertThat(stats.getLowStockProducts().get(2).getStockQuantity()).isEqualTo(5);

        // 3. Đối soát người dùng
        long dbTotalUsers = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM users").getSingleResult()).longValue();
        long dbCustomers = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'").getSingleResult()).longValue();

        assertThat(stats.getTotalUsers()).isEqualTo(dbTotalUsers);
        assertThat(stats.getTotalCustomers()).isEqualTo(dbCustomers);
    }

    /**
     * Kiểm thử API GET /admin/dashboard/stats hoạt động đúng và trả số liệu chính xác 100%.
     */
    @Test
    @DisplayName("API GET /admin/dashboard/stats: Trả về HTTP 200 và dữ liệu khớp hoàn toàn với CSDL")
    @WithMockUser(username = "admin.reconcile@ecommerce.com", roles = {"ADMIN"})
    void testGetDashboardStatsApi_Success() throws Exception {
        createTestOrder("ORD-API-01", BigDecimal.valueOf(1500000), OrderStatus.CONFIRMED, PaymentStatus.PAID);
        createTestOrder("ORD-API-02", BigDecimal.valueOf(500000), OrderStatus.PENDING, PaymentStatus.UNPAID);

        mockMvc.perform(get("/admin/dashboard/stats?lowStockThreshold=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.pendingOrders").value(1))
                .andExpect(jsonPath("$.data.actualRevenue").value(1500000.0));
    }

    // Helper tạo đơn hàng test
    private Order createTestOrder(String code, BigDecimal amount, OrderStatus orderStatus, PaymentStatus paymentStatus) {
        Order order = Order.builder()
                .orderCode(code)
                .user(customer)
                .recipientName("Người Nhận " + code)
                .recipientPhone("0901234567")
                .shippingAddress("123 Phố Test, Hà Nội")
                .totalAmount(amount)
                .orderStatus(orderStatus)
                .paymentStatus(paymentStatus)
                .items(new ArrayList<>())
                .build();

        return orderRepository.save(order);
    }

    // Helper tạo đơn kèm OrderItem
    private Order createOrderWithItems(String code, Product product, int quantity) {
        Order order = Order.builder()
                .orderCode(code)
                .user(customer)
                .recipientName("Khách Hàng Mua Sắm")
                .recipientPhone("0988888888")
                .shippingAddress("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội")
                .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .build();

        order.addItem(item);
        return orderRepository.save(order);
    }
}
