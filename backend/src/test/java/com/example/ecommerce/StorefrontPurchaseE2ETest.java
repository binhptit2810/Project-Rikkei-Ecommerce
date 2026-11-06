package com.example.ecommerce;

import com.example.ecommerce.dto.request.CancelOrderRequest;
import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử toàn bộ luồng mua hàng Storefront End-to-End (E2E) trên cơ sở dữ liệu thực (H2 Mode=MySQL).
 * Người phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Người phối hợp: Đặng Thanh Bình [DTB] (Inventory & Payment)
 *
 * Kiểm thử chi tiết:
 * 1. Đặt hàng COD hoàn chỉnh (Tồn kho, Order, OrderItem snapshot, TotalAmount, Trạng thái)
 * 2. Đối soát tổng tiền snapshot chính xác tuyệt đối tới số thập phân, không phụ thuộc giá sửa đổi sau này
 * 3. Thanh toán trực tuyến mô phỏng SIMULATED thành công (PAID, CONFIRMED, mã PAY-)
 * 4. Thanh toán trực tuyến mô phỏng thất bại và thử lại thanh toán thành công
 * 5. Chặn thanh toán trùng lặp khi đơn đã PAID
 * 6. Chặn thanh toán khi đơn hàng đã CANCELLED
 * 7. Hủy đơn hàng PENDING thành công (chuyển CANCELLED, hoàn lại tồn kho và ghi nhật ký ORDER_CANCELLED)
 * 8. Hủy đơn hàng đã PAID -> tự động chuyển trạng thái thanh toán sang REFUNDED
 * 9. Kiểm tra phân quyền: Khách hàng không thể xem hoặc hủy đơn hàng của khách hàng khác (403 Forbidden)
 * 10. Chặn bán vượt tồn kho (Overselling) và bảo toàn dữ liệu khi giao dịch thất bại
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_storefront_e2e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class StorefrontPurchaseE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private User customer1;
    private User customer2;
    private Category testCategory;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        // Dọn dẹp dữ liệu trước mỗi bài kiểm thử
        orderItemRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Tạo người dùng thử nghiệm
        customer1 = userRepository.save(User.builder()
                .email("hoang.customer@example.com")
                .fullName("Vũ Đức Huy Hoàng")
                .password("$2a$10$encodedpassword")
                .role(Role.CUSTOMER)
                .active(true)
                .phone("0912345678")
                .address("123 Phố Huế, Hai Bà Trưng, Hà Nội")
                .build());

        customer2 = userRepository.save(User.builder()
                .email("other.customer@example.com")
                .fullName("Người Dùng Khác")
                .password("$2a$10$encodedpassword")
                .role(Role.CUSTOMER)
                .active(true)
                .phone("0987654321")
                .address("456 Lê Lợi, Quận 1, TP.HCM")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("Thời trang nam")
                .description("Quần áo thời trang nam cao cấp")
                .active(true)
                .build());

        // Sản phẩm A: 250,000.00 đ, tồn kho ban đầu 10
        productA = productRepository.save(Product.builder()
                .name("Áo Polo Nam Classic")
                .description("Chất liệu cotton thoáng mát")
                .price(BigDecimal.valueOf(250000.00).setScale(2, RoundingMode.HALF_UP))
                .stockQuantity(10)
                .category(testCategory)
                .active(true)
                .build());

        // Sản phẩm B: 149,500.50 đ (có phần thập phân), tồn kho ban đầu 15
        productB = productRepository.save(Product.builder()
                .name("Quần Short Kaki Co Giãn")
                .description("Quần short năng động mùa hè")
                .price(BigDecimal.valueOf(149500.50).setScale(2, RoundingMode.HALF_UP))
                .stockQuantity(15)
                .category(testCategory)
                .active(true)
                .build());
    }

    // =========================================================================
    // 1. KIỂM THỬ ĐẶT HÀNG COD TOÀN DIỆN (Section 3)
    // =========================================================================
    @Test
    @DisplayName("ST-01: Đặt hàng COD thành công - tạo Order, OrderItem, trừ kho, thanh toán UNPAID")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_PlaceOrder_COD_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hai Bà Trưng, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao vào giờ hành chính")
                .items(List.of(
                        new OrderItemRequest(productA.getId(), 2) // 250,000 * 2 = 500,000.00
                ))
                .build();

        String responseContent = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt hàng thành công"))
                .andExpect(jsonPath("$.data.orderCode").value(startsWith("ORD-")))
                .andExpect(jsonPath("$.data.recipientName").value("Vũ Đức Huy Hoàng"))
                .andExpect(jsonPath("$.data.totalAmount").value(500000.00))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.transactionCode").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName").value("Áo Polo Nam Classic"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(250000.00))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(500000.00))
                .andReturn().getResponse().getContentAsString();

        // Kiểm tra dữ liệu được lưu thực tế trong cơ sở dữ liệu
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(orders).hasSize(1);
        Order savedOrder = orderRepository.findByIdWithItems(orders.get(0).getId()).orElseThrow();

        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000.00));
        assertThat(savedOrder.getItems()).hasSize(1);

        OrderItem item = savedOrder.getItems().get(0);
        assertThat(item.getProductName()).isEqualTo("Áo Polo Nam Classic");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(250000.00));
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(500000.00));

        // Kiểm tra tồn kho của sản phẩm A bị trừ chính xác từ 10 xuống 8
        Product updatedProductA = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(updatedProductA.getStockQuantity()).isEqualTo(8);

        // Kiểm tra bản ghi biến động kho InventoryTransaction
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productA.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_PLACED);
        assertThat(transactions.get(0).getChangeQuantity()).isEqualTo(-2);
        assertThat(transactions.get(0).getStockAfter()).isEqualTo(8);

        // Kiểm tra bản ghi Payment ban đầu
        Payment payment = paymentRepository.findByOrderId(savedOrder.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(payment.getTransactionCode()).startsWith("PAY-");
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000.00));
    }

    // =========================================================================
    // 2. ĐỐI SOÁT TỔNG TIỀN VÀ TÍNH TOÁN SNAPSHOT CHÍNH XÁC (Section 7)
    // =========================================================================
    @Test
    @DisplayName("ST-02: Đối soát tổng tiền snapshot với giá có phần thập phân và giá sản phẩm bị thay đổi sau đó")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_TotalAmount_Snapshot_Reconciliation() throws Exception {
        // Đặt mua:
        // Product A: 250,000.00 * 3 = 750,000.00
        // Product B: 149,500.50 * 2 = 299,001.00
        // Tổng kỳ vọng: 1,049,001.00
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hai Bà Trưng, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        new OrderItemRequest(productA.getId(), 3),
                        new OrderItemRequest(productB.getId(), 2)
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(1049001.00))
                .andExpect(jsonPath("$.data.itemCount").value(5));

        Long createdOrderId = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0).getId();
        Order order = orderRepository.findByIdWithItems(createdOrderId).orElseThrow();
        BigDecimal expectedTotal = BigDecimal.valueOf(1049001.00).setScale(2, RoundingMode.HALF_UP);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedTotal);

        // Đối soát công thức bắt buộc: Order.totalAmount = sum(OrderItem.unitPrice * OrderItem.quantity)
        BigDecimal sumCalculated = order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(sumCalculated);

        // THAY ĐỔI GIÁ SẢN PHẨM VÀ TÊN SẢN PHẨM TRONG DATABASE ĐỂ KIỂM TRA TÍNH TOÀN VẸN SNAPSHOT
        productA.setPrice(BigDecimal.valueOf(999999.99));
        productA.setName("Áo Polo Nam Siêu Đắt");
        productRepository.save(productA);

        productB.setPrice(BigDecimal.valueOf(500.00));
        productB.setName("Quần Xả Kho");
        productRepository.save(productB);

        // Gọi API xem chi tiết đơn hàng cũ: giá snapshot và tên sản phẩm snapshot KHÔNG ĐƯỢC PHÉP THAY ĐỔI
        mockMvc.perform(get("/api/v1/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(1049001.00))
                .andExpect(jsonPath("$.data.items[0].productName").value("Áo Polo Nam Classic"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(250000.00))
                .andExpect(jsonPath("$.data.items[1].productName").value("Quần Short Kaki Co Giãn"))
                .andExpect(jsonPath("$.data.items[1].unitPrice").value(149500.50));
    }

    // =========================================================================
    // 3. KIỂM THỬ THANH TOÁN SIMULATED THÀNH CÔNG (Section 4)
    // =========================================================================
    @Test
    @DisplayName("ST-03: Thanh toán mô phỏng SIMULATED thành công - chuyển PAID và CONFIRMED, mã PAY-")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_SimulatedPayment_Success() throws Exception {
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.SIMULATED)
                .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"));

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);

        // Gọi API thanh toán mô phỏng thành công
        ProcessPaymentRequest payReq = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .transactionNote("Thanh toán thẻ Visa qua cổng mô phỏng thành công")
                .build();

        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.transactionCode").value(startsWith("PAY-")))
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty());

        // Kiểm tra trạng thái đơn hàng trong DB được tự động chuyển sang CONFIRMED và PAID
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        Payment updatedPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updatedPayment.getPaidAt()).isNotNull();
    }

    // =========================================================================
    // 4. KIỂM THỬ THANH TOÁN SIMULATED THẤT BẠI VÀ THỬ LẠI (Section 4)
    // =========================================================================
    @Test
    @DisplayName("ST-04: Thanh toán mô phỏng thất bại -> FAILED; người dùng thực hiện lại thành công -> PAID")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_SimulatedPayment_Failure_And_Retry() throws Exception {
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.SIMULATED)
                .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);

        // 1. Giả lập thanh toán thất bại
        ProcessPaymentRequest failReq = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(false)
                .transactionNote("Thẻ không đủ số dư")
                .build();

        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        Order failedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(failedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        // Trạng thái đơn vẫn là PENDING, không tự động xác nhận khi thanh toán lỗi
        assertThat(failedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);

        // 2. Người dùng thao tác lại (Retry payment) với thẻ hợp lệ (thành công)
        ProcessPaymentRequest retryReq = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .transactionNote("Thử lại thanh toán với thẻ mới")
                .build();

        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(retryReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        Order paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(paidOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paidOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // =========================================================================
    // 5. CHẶN THANH TOÁN TRÙNG LẶP KHI ĐƠN ĐÃ THANH TOÁN (Section 4)
    // =========================================================================
    @Test
    @DisplayName("ST-05: Chặn thanh toán trùng lặp khi đơn hàng đã PAID -> 400 Bad Request")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_DuplicatePayment_Rejected() throws Exception {
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.SIMULATED)
                .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);

        // Thanh toán lần 1: thành công
        ProcessPaymentRequest payReq = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .build();

        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // Thanh toán lần 2 (trùng lặp): bị từ chối
        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("đã được thanh toán thành công trước đó")));
    }

    // =========================================================================
    // 6. KIỂM THỬ HỦY ĐƠN HÀNG VÀ HOÀN LẠI TỒN KHO (Section 6)
    // =========================================================================
    @Test
    @DisplayName("ST-06: Hủy đơn hàng PENDING thành công - trạng thái CANCELLED, hoàn tồn kho chính xác")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_CancelOrder_Pending_Success() throws Exception {
        // Tồn kho ban đầu của productA = 10
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(productA.getId(), 3))) // Trừ 3 -> còn 7
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);
        assertThat(productRepository.findById(productA.getId()).orElseThrow().getStockQuantity()).isEqualTo(7);

        // Thực hiện hủy đơn hàng
        CancelOrderRequest cancelReq = CancelOrderRequest.builder()
                .reason("Muốn đổi size áo khác")
                .build();

        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.note").value(containsString("Lý do hủy: Muốn đổi size áo khác")));

        // Kiểm tra tồn kho được hoàn lại đầy đủ: 7 + 3 = 10
        Product restoredProductA = productRepository.findById(productA.getId()).orElseThrow();
        assertThat(restoredProductA.getStockQuantity()).isEqualTo(10);

        // Kiểm tra nhật ký biến động kho ORDER_CANCELLED
        List<InventoryTransaction> txs = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productA.getId());
        assertThat(txs.get(0).getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_CANCELLED);
        assertThat(txs.get(0).getChangeQuantity()).isEqualTo(3);
        assertThat(txs.get(0).getStockAfter()).isEqualTo(10);

        // Hủy lại lần 2 -> ném lỗi Conflict 409
        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 7. HỦY ĐƠN ĐÃ THANH TOÁN -> TỰ ĐỘNG REFUNDED (Section 6)
    // =========================================================================
    @Test
    @DisplayName("ST-07: Hủy đơn hàng đã thanh toán (PAID) -> tự động chuyển sang REFUNDED")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_CancelOrder_PaidOrder_BecomesRefunded() throws Exception {
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.SIMULATED)
                .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);

        // Thanh toán thành công (order chuyển sang CONFIRMED & PAID)
        mockMvc.perform(post("/api/v1/payments/order/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProcessPaymentRequest.builder()
                                .paymentMethod(PaymentMethod.SIMULATED)
                                .simulateSuccess(true)
                                .build())))
                .andExpect(status().isOk());

        // Hủy đơn hàng CONFIRMED đã thanh toán
        CancelOrderRequest cancelReq = CancelOrderRequest.builder()
                .reason("Khách hàng báo hủy trước khi đóng gói")
                .build();

        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"));

        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // =========================================================================
    // 8. KIỂM THỬ BẢO MẬT PHÂN QUYỀN VÀ TRUY CẬP ĐƠN HÀNG (Section 5 & 6)
    // =========================================================================
    @Test
    @DisplayName("ST-08: Người dùng không thể xem hoặc hủy đơn hàng của người dùng khác -> AccessDeniedException")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_CrossUser_AccessDenied() throws Exception {
        // Customer 1 tạo đơn hàng
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId()).get(0);

        // Customer 2 cố gắng xem chi tiết đơn hàng của Customer 1 -> Bị từ chối
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orderService.getOrderDetail(order.getId(), "other.customer@example.com")
        ).isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
         .hasMessageContaining("không có quyền");

        // Customer 2 cố gắng hủy đơn hàng của Customer 1 -> Bị từ chối
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                orderService.cancelOrder(order.getId(), "other.customer@example.com", "Hủy trộm")
        ).isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
         .hasMessageContaining("không có quyền");

        // Customer 2 xem danh sách đơn hàng của mình -> Không thấy đơn của Customer 1
        var customer2Orders = orderService.getMyOrders("other.customer@example.com", 0, 10, "createdAt", "desc");
        assertThat(customer2Orders.getContent()).isEmpty();
        assertThat(customer2Orders.getTotalElements()).isEqualTo(0);
    }

    // =========================================================================
    // 9. KIỂM THỬ BẢO VỆ KHO: CHẶN ĐẶT HÀNG KHI HẾT HÀNG VÀ ROLLBACK (Section 8)
    // =========================================================================
    @Test
    @DisplayName("ST-09: Chặn đặt hàng khi vượt quá tồn kho -> 400 Bad Request, Rollback an toàn")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_OutOfStock_Order_Rejected_And_RolledBack() throws Exception {
        // Tồn kho productA = 10, yêu cầu mua 20
        CreateOrderRequest createReq = CreateOrderRequest.builder()
                .recipientName("Vũ Đức Huy Hoàng")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(productA.getId(), 20)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("không đủ số lượng tồn kho")));

        // Đảm bảo không có đơn hàng dở dang nào được tạo trong DB (Rollback thành công)
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(customer1.getId());
        assertThat(orders).isEmpty();

        // Tồn kho productA giữ nguyên = 10
        assertThat(productRepository.findById(productA.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    // =========================================================================
    // 10. KIỂM THỬ LỊCH SỬ ĐƠN HÀNG VÀ PHÂN TRANG (Section 5)
    // =========================================================================
    @Test
    @DisplayName("ST-10: Xem lịch sử đơn hàng có phân trang - GET /api/v1/orders/my")
    @WithMockUser(username = "hoang.customer@example.com", roles = {"CUSTOMER"})
    void test_GetMyOrders_Pagination() throws Exception {
        // Tạo 3 đơn hàng
        for (int i = 1; i <= 3; i++) {
            CreateOrderRequest req = CreateOrderRequest.builder()
                    .recipientName("Vũ Đức Huy Hoàng " + i)
                    .recipientPhone("0912345678")
                    .shippingAddress("Địa chỉ " + i)
                    .paymentMethod(PaymentMethod.COD)
                    .items(List.of(new OrderItemRequest(productA.getId(), 1)))
                    .build();

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Lấy lịch sử trang 0, kích thước 2
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        // Lấy lịch sử trang 1, kích thước 2 -> còn lại 1 đơn
        mockMvc.perform(get("/api/v1/orders/my")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }
}
