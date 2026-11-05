package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.LockUserRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test kiểm tra cấu hình Logging SLF4J cho các Thao tác Nghiệp vụ Nhạy cảm [TSK-D09-CCT].
 * Xác minh:
 * - Tạo đơn hàng: log.info với userId và totalAmount (format chuẩn).
 * - Hủy đơn hàng: log.warn với userId, orderId, actorRole (format chuẩn).
 * - Điều chỉnh tồn kho: log.info với Admin userId, quantityBefore, quantityAfter.
 * - Khóa/mở tài khoản: log.info với actorUserId, targetUserId, reason.
 * - Thất bại thanh toán: log.warn với orderId, transactionCode.
 * - Không rò rỉ mật khẩu, token hoặc thông tin nhạy cảm.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SensitiveBusinessLoggingTest {

    // --- Mocks for OrderService ---
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PaymentService paymentService;

    // --- Mocks for InventoryService ---
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    // --- Mocks for UserService ---
    @Mock
    private RefreshTokenService refreshTokenService;

    // --- Mocks for PaymentService ---
    @Mock
    private PaymentRepository paymentRepository;

    private OrderService orderService;
    private InventoryService inventoryServiceImpl;
    private UserService userService;
    private PaymentService paymentServiceImpl;

    private User testCustomer;
    private CustomUserDetails adminUserDetails;
    private CustomUserDetails customerUserDetails;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, userRepository, inventoryService, paymentService);
        inventoryServiceImpl = new InventoryService(productRepository, inventoryTransactionRepository);
        userService = new UserService(userRepository, null, refreshTokenService);
        paymentServiceImpl = new PaymentService(paymentRepository, orderRepository, userRepository);

        testCustomer = User.builder()
                .id(15L)
                .email("customer15@example.com")
                .password("$2a$12$eB.hashedPasswordNotReal")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        adminUserDetails = new CustomUserDetails(1L, "admin@eshop.com", "secret", Role.ADMIN, true);
        customerUserDetails = new CustomUserDetails(15L, "customer15@example.com", "secret", Role.CUSTOMER, true);
    }

    // =========================================================================
    // 1. TẠO ĐƠN HÀNG LOGGING TESTS
    // =========================================================================

    @Test
    @DisplayName("Logging: Tạo đơn hàng thành công ghi log.info với userId, orderId và totalAmount chuẩn format")
    void createOrder_Logging_Success(CapturedOutput output) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Duong ABC, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(10L, 2)))
                .build();

        Product product = Product.builder()
                .id(10L)
                .name("Ao thun")
                .price(BigDecimal.valueOf(250000))
                .stockQuantity(100)
                .active(true)
                .build();

        when(userRepository.findByEmail("customer15@example.com")).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1001L);
            return o;
        });

        orderService.createOrder(request, "customer15@example.com");

        assertThat(output.getOut())
                .contains("INFO")
                .contains("userId=15 | action=CREATE_ORDER | result=SUCCESS | orderId=1001 | totalAmount=500000");

        // Đảm bảo không lộ thông tin nhạy cảm
        assertThat(output.getOut())
                .doesNotContain("hashedPasswordNotReal")
                .doesNotContain("secret");
    }

    @Test
    @DisplayName("Logging: Tạo đơn hàng thất bại khi hết hàng ghi log.warn với result=FAILURE")
    void createOrder_Logging_Failure_OutOfStock(CapturedOutput output) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Duong ABC, Ha Noi")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(10L, 5)))
                .build();

        Product product = Product.builder()
                .id(10L)
                .name("Ao thun")
                .price(BigDecimal.valueOf(200000))
                .stockQuantity(1)
                .active(true)
                .build();

        when(userRepository.findByEmail("customer15@example.com")).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1002L);
            return o;
        });
        doThrow(new BusinessException("Không đủ tồn kho")).when(inventoryService).deductStockForOrder(any());

        assertThatThrownBy(() -> orderService.createOrder(request, "customer15@example.com"))
                .isInstanceOf(BusinessException.class);

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=15 | action=CREATE_ORDER | result=FAILURE | orderId=1002 | reason=Không đủ tồn kho");
    }

    // =========================================================================
    // 2. HỦY ĐƠN HÀNG LOGGING TESTS
    // =========================================================================

    @Test
    @DisplayName("Logging: Hủy đơn hàng thành công ghi log.warn với userId, actorRole, orderId và reason")
    void cancelOrder_Logging_Success(CapturedOutput output) {
        Order order = Order.builder()
                .id(1001L)
                .orderCode("ORD-20261105-ABCXYZ")
                .user(testCustomer)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        when(orderRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer15@example.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(1001L, "customer15@example.com", "Đổi ý không mua nữa\r\nTest newline");

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=15 | action=CANCEL_ORDER | actorRole=CUSTOMER | result=SUCCESS | orderId=1001 | reason=Đổi ý không mua nữa Test newline");
    }

    @Test
    @DisplayName("Logging: Hủy đơn hàng thất bại do sai trạng thái ghi log.warn với result=FAILURE")
    void cancelOrder_Logging_Failure_InvalidStatus(CapturedOutput output) {
        Order order = Order.builder()
                .id(1001L)
                .orderCode("ORD-20261105-ABCXYZ")
                .user(testCustomer)
                .orderStatus(OrderStatus.SHIPPING)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        when(orderRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer15@example.com")).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> orderService.cancelOrder(1001L, "customer15@example.com", "Huy don"))
                .isInstanceOf(ConflictException.class);

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=15 | action=CANCEL_ORDER | actorRole=CUSTOMER | result=FAILURE | orderId=1001");
    }

    // =========================================================================
    // 3. ĐIỀU CHỈNH TỒN KHO LOGGING TESTS
    // =========================================================================

    @Test
    @DisplayName("Logging: Điều chỉnh tồn kho thành công ghi log.info với Admin userId, quantityBefore, quantityAfter")
    void adjustStock_Logging_Success(CapturedOutput output) {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(15)
                .type(InventoryTransactionType.MANUAL_IMPORT)
                .reason("Nhập thêm hàng đầu tháng")
                .build();

        Product product = Product.builder()
                .id(10L)
                .name("San pham test")
                .stockQuantity(20)
                .build();

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryServiceImpl.adjustStock(10L, request, 1L);

        assertThat(output.getOut())
                .contains("INFO")
                .contains("userId=1 | action=ADJUST_INVENTORY | result=SUCCESS | productId=10 | quantityBefore=20 | quantityAfter=35");
    }

    @Test
    @DisplayName("Logging: Điều chỉnh tồn kho thất bại khi xuất vượt tồn ghi log.warn với result=FAILURE")
    void adjustStock_Logging_Failure_ExceedStock(CapturedOutput output) {
        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(50)
                .type(InventoryTransactionType.MANUAL_EXPORT)
                .reason("Xuat kho qua muc")
                .build();

        Product product = Product.builder()
                .id(10L)
                .name("San pham test")
                .stockQuantity(10)
                .build();

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryServiceImpl.adjustStock(10L, request, 1L))
                .isInstanceOf(BusinessException.class);

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=1 | action=ADJUST_INVENTORY | result=FAILURE | productId=10 | quantityBefore=10");
    }

    // =========================================================================
    // 4. KHÓA / MỞ KHÓA TÀI KHOẢN LOGGING TESTS
    // =========================================================================

    @Test
    @DisplayName("Logging: Khóa tài khoản thành công ghi log.info với actorUserId, targetUserId và reason")
    void lockUser_Logging_Success(CapturedOutput output) {
        User target = User.builder()
                .id(2L)
                .email("user2@example.com")
                .active(true)
                .build();

        LockUserRequest request = new LockUserRequest("Vi pham dieu khoan\r\nSpam");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.lockUser(2L, request, adminUserDetails);

        assertThat(output.getOut())
                .contains("INFO")
                .contains("userId=1 | action=LOCK_USER | result=SUCCESS | targetUserId=2 | reason=Vi pham dieu khoan Spam");
    }

    @Test
    @DisplayName("Logging: Khóa tài khoản thất bại do Admin tự khóa chính mình ghi log.warn với result=FAILURE")
    void lockUser_Logging_Failure_SelfLock(CapturedOutput output) {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@eshop.com")
                .active(true)
                .build();

        LockUserRequest request = new LockUserRequest("Tu khoa");
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.lockUser(1L, request, adminUserDetails))
                .isInstanceOf(BusinessException.class);

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=1 | action=LOCK_USER | result=FAILURE | targetUserId=1 | reason=Bạn không thể tự khóa tài khoản của chính mình");
    }

    @Test
    @DisplayName("Logging: Mở khóa tài khoản thành công ghi log.info với actorUserId và targetUserId")
    void unlockUser_Logging_Success(CapturedOutput output) {
        User target = User.builder()
                .id(2L)
                .email("user2@example.com")
                .active(false)
                .lockReason("Ly do cu")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.unlockUser(2L, adminUserDetails);

        assertThat(output.getOut())
                .contains("INFO")
                .contains("userId=1 | action=UNLOCK_USER | result=SUCCESS | targetUserId=2");
    }

    // =========================================================================
    // 5. THẤT BẠI THANH TOÁN LOGGING TESTS
    // =========================================================================

    @Test
    @DisplayName("Logging: Thất bại thanh toán ghi log.warn với result=FAILURE, orderId và transactionCode")
    void processPayment_Logging_Failure(CapturedOutput output) {
        Order order = Order.builder()
                .id(10L)
                .orderCode("ORD-20261105-PAY01")
                .user(testCustomer)
                .totalAmount(BigDecimal.valueOf(1000000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Payment payment = Payment.builder()
                .id(999L)
                .order(order)
                .transactionCode("PAY-20261105-FAIL0001")
                .status(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(false)
                .transactionNote("Giao dịch từ chối bởi ngân hàng")
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(Optional.of(testCustomer));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentServiceImpl.processPayment(10L, request, testCustomer.getEmail());

        assertThat(output.getOut())
                .contains("WARN")
                .contains("userId=15 | action=PROCESS_PAYMENT | result=FAILURE | orderId=10 | transactionCode=PAY-20261105-FAIL0001");
    }

    @Test
    @DisplayName("Logging: Thanh toán thành công ghi log.info với result=SUCCESS, orderId và transactionCode")
    void processPayment_Logging_Success(CapturedOutput output) {
        Order order = Order.builder()
                .id(10L)
                .orderCode("ORD-20261105-PAY02")
                .user(testCustomer)
                .totalAmount(BigDecimal.valueOf(1000000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Payment payment = Payment.builder()
                .id(999L)
                .order(order)
                .transactionCode("PAY-20261105-SUCC0001")
                .status(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .transactionNote("Giao dịch thành công")
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(Optional.of(testCustomer));
        when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentServiceImpl.processPayment(10L, request, testCustomer.getEmail());

        assertThat(output.getOut())
                .contains("INFO")
                .contains("userId=15 | action=PROCESS_PAYMENT | result=SUCCESS | orderId=10 | transactionCode=PAY-20261105-SUCC0001");
    }
}

