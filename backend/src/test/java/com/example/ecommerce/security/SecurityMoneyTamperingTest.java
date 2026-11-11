package com.example.ecommerce.security;

import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.CreateVNPayPaymentRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.VNPayIpnResponse;
import com.example.ecommerce.dto.response.VNPayPaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.service.CouponService;
import com.example.ecommerce.service.EmailService;
import com.example.ecommerce.service.InventoryService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử bảo mật chống gian lận tiền tệ (Money Integrity & Payment Anti-Fraud).
 * Phụ trách: Cao Chí Thiện (CCT) & Đặng Thanh Bình (DTB)
 * Task ID: [TSK-D13-CCT]
 * 
 * Kiểm tra các kịch bản tấn công: SEC-13-01 đến SEC-13-08.
 */
@ExtendWith(MockitoExtension.class)
class SecurityMoneyTamperingTest {

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
    @Mock
    private EmailService emailService;
    @Mock
    private CouponService couponService;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private VNPayConfig vnPayConfig;
    @Mock
    private HttpServletRequest httpServletRequest;

    private OrderService orderService;
    private PaymentService paymentServiceImpl;
    private Validator validator;

    private User testUser;
    private Product product1;
    private Product product2;
    private static final String VNP_HASH_SECRET = "TEST_SECRET_KEY_FOR_SECURITY_TEST_1234567890";
    private static final String VNP_TMN_CODE = "2QXUI4J4";

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        orderService = new OrderService(
                orderRepository,
                productRepository,
                userRepository,
                inventoryService,
                paymentService,
                emailService,
                couponService,
                couponRepository
        );

        paymentServiceImpl = new PaymentService(
                paymentRepository,
                orderRepository,
                userRepository,
                vnPayConfig,
                emailService,
                paymentTransactionRepository
        );

        testUser = User.builder()
                .id(1L)
                .email("buyer@example.com")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        product1 = Product.builder()
                .id(101L)
                .name("Bàn phím cơ cao cấp")
                .price(new BigDecimal("1500000"))
                .stockQuantity(50)
                .active(true)
                .build();

        product2 = Product.builder()
                .id(102L)
                .name("Chuột không dây")
                .price(new BigDecimal("500000"))
                .stockQuantity(30)
                .active(true)
                .build();
    }

    // =========================================================================
    // SEC-13-01: Client thao túng giá sản phẩm khi tạo đơn hàng
    // =========================================================================
    @Test
    @DisplayName("SEC-13-01: Client gửi đơn hàng, backend tính giá từ DB (1.500.000đ), không tin giá client")
    void sec13_01_clientTamperedPrice_backendEnforcesDatabasePrice() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        // Client chỉ gửi productId và quantity trong OrderItemRequest (DTO hoàn toàn không có trường price)
        OrderItemRequest itemReq = OrderItemRequest.builder()
                .productId(101L)
                .quantity(2)
                .build();

        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(itemReq))
                .build();

        var response = orderService.createOrder(orderReq, "buyer@example.com");

        // Backend lấy giá niêm yết 1.500.000đ * 2 = 3.000.000đ
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3000000"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("3000000"));

        verify(inventoryService).deductStockForOrder(any(Order.class));
    }

    // =========================================================================
    // SEC-13-02: Client thao túng tổng tiền đơn hàng
    // =========================================================================
    @Test
    @DisplayName("SEC-13-02: Backend tự động tính toán tổng tiền từ danh sách sản phẩm và giảm giá, không nhận totalAmount từ client")
    void sec13_02_clientTamperedTotalAmount_backendCalculatesRealTotal() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));
        when(couponService.applyCouponForOrder(eq("GIAM100K"), any(BigDecimal.class))).thenReturn(new BigDecimal("100000.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(11L);
            return o;
        });

        // 1 sản phẩm 1 (1.500.000) + 2 sản phẩm 2 (500.000 * 2 = 1.000.000) = Subtotal 2.500.000
        // Giảm 100.000 -> Total = 2.400.000
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.VNPAY)
                .couponCode("GIAM100K")
                .items(List.of(
                        OrderItemRequest.builder().productId(101L).quantity(1).build(),
                        OrderItemRequest.builder().productId(102L).quantity(2).build()
                ))
                .build();

        var response = orderService.createOrder(orderReq, "buyer@example.com");

        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2400000"));
        verify(couponService).applyCouponForOrder(eq("GIAM100K"), any(BigDecimal.class));
    }

    // =========================================================================
    // SEC-13-03: Client gửi số lượng âm hoặc bằng 0
    // =========================================================================
    @Test
    @DisplayName("SEC-13-03: Số lượng đặt mua bằng 0 hoặc âm bị chặn bởi Bean Validation @Min(1) và Service")
    void sec13_03_clientSubmitsZeroOrNegativeQuantity_rejected() {
        // 1. Kiểm tra Bean Validation với quantity = 0
        OrderItemRequest zeroQty = OrderItemRequest.builder().productId(101L).quantity(0).build();
        Set<ConstraintViolation<OrderItemRequest>> violationsZero = validator.validate(zeroQty);
        assertThat(violationsZero).isNotEmpty();
        assertThat(violationsZero.iterator().next().getMessage()).contains("Số lượng đặt phải lớn hơn hoặc bằng 1");

        // 2. Kiểm tra Bean Validation với quantity = -5
        OrderItemRequest negQty = OrderItemRequest.builder().productId(101L).quantity(-5).build();
        Set<ConstraintViolation<OrderItemRequest>> violationsNeg = validator.validate(negQty);
        assertThat(violationsNeg).isNotEmpty();

        // 3. Kiểm tra OrderService phòng thủ tầng 2 ném BusinessException
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .recipientName("Hacker")
                .recipientPhone("0987654321")
                .shippingAddress("123 Hacker St")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(negQty))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(orderReq, "buyer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng sản phẩm đặt mua phải lớn hơn hoặc bằng 1");
    }

    // =========================================================================
    // SEC-13-04: Áp dụng voucher hết hạn, chưa kích hoạt hoặc không đủ điều kiện
    // =========================================================================
    @Test
    @DisplayName("SEC-13-04: Áp dụng voucher hết hạn hoặc chưa đạt minOrderValue bị từ chối")
    void sec13_04_clientAppliesInvalidCoupon_rejected() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2)); // 500.000đ

        // Giả lập couponService từ chối vì đơn hàng chưa đạt giá trị tối thiểu
        doThrow(new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu 1.000.000 đ để áp dụng mã giảm giá này"))
                .when(couponService).applyCouponForOrder(eq("MIN1M"), any(BigDecimal.class));

        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .recipientName("Người mua")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy")
                .paymentMethod(PaymentMethod.COD)
                .couponCode("MIN1M")
                .items(List.of(OrderItemRequest.builder().productId(102L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(orderReq, "buyer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đơn hàng chưa đạt giá trị tối thiểu");
    }

    // =========================================================================
    // SEC-13-05: Voucher vượt số lượt sử dụng tối đa (usageLimit)
    // =========================================================================
    @Test
    @DisplayName("SEC-13-05: Áp dụng voucher đã hết lượt sử dụng bị từ chối")
    void sec13_05_clientAppliesExhaustedCoupon_rejected() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        doThrow(new BusinessException("Mã giảm giá đã hết lượt sử dụng"))
                .when(couponService).applyCouponForOrder(eq("EXHAUSTED"), any(BigDecimal.class));

        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .recipientName("Người mua")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy")
                .paymentMethod(PaymentMethod.COD)
                .couponCode("EXHAUSTED")
                .items(List.of(OrderItemRequest.builder().productId(101L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(orderReq, "buyer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã giảm giá đã hết lượt sử dụng");
    }

    // =========================================================================
    // SEC-13-06: Client thao túng trường amount trong CreateVNPayPaymentRequest
    // =========================================================================
    @Test
    @DisplayName("SEC-13-06: Client gửi amount=1.000đ trong CreateVNPayPaymentRequest bị bỏ qua, URL dùng 1.500.000đ từ DB")
    void sec13_06_clientTamperedAmountInVNPayRequest_backendUsesOrderTotal() {
        Order dbOrder = Order.builder()
                .id(50L)
                .orderCode("ORD-20261111-SEC01")
                .user(testUser)
                .totalAmount(new BigDecimal("1500000"))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(dbOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(vnPayConfig.getVersion()).thenReturn("2.1.0");
        when(vnPayConfig.getCommand()).thenReturn("pay");
        when(vnPayConfig.getTmnCode()).thenReturn(VNP_TMN_CODE);
        when(vnPayConfig.getCurrCode()).thenReturn("VND");
        when(vnPayConfig.getOrderType()).thenReturn("other");
        when(vnPayConfig.getLocale()).thenReturn("vn");
        when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:3000/orders/vnpay-return");
        when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getHashSecret()).thenReturn(VNP_HASH_SECRET);

        // Client cố ý gửi amount = 1.000đ để gian lận tiền tệ
        CreateVNPayPaymentRequest tamperedReq = CreateVNPayPaymentRequest.builder()
                .orderId(50L)
                .amount(new BigDecimal("1000"))
                .orderInfo("Thanh toan gian lan")
                .build();

        VNPayPaymentResponse response = paymentServiceImpl.createVNPayPayment(tamperedReq, "127.0.0.1", "buyer@example.com");

        // Số tiền đưa vào URL VNPay phải là 1.500.000 * 100 = 150.000.000 xu, KHÔNG PHẢI 100.000 xu
        assertThat(response.getPaymentUrl()).contains("vnp_Amount=150000000");
        assertThat(response.getPaymentUrl()).doesNotContain("vnp_Amount=100000");
    }

    // =========================================================================
    // SEC-13-07: Giả mạo chữ ký VNPay hoặc chữ ký bị sai lệch
    // =========================================================================
    @Test
    @DisplayName("SEC-13-07: IPN callback có chữ ký giả mạo hoặc vnp_Amount bị sửa đổi bị từ chối với RspCode 97")
    void sec13_07_tamperedVNPaySecureHash_rejectedWithChecksumError() {
        when(vnPayConfig.getHashSecret()).thenReturn(VNP_HASH_SECRET);

        // Giả lập callback VNPay với chữ ký sai lệch
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_TmnCode", VNP_TMN_CODE);
        ipnParams.put("vnp_Amount", "150000000");
        ipnParams.put("vnp_TxnRef", "ORD-20261111-SEC01");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TransactionNo", "14000123");
        ipnParams.put("vnp_SecureHash", "invalid_fake_tampered_hash_0123456789abcdef");

        VNPayIpnResponse ipnResponse = paymentServiceImpl.processVNPayIpn(ipnParams, null);

        assertThat(ipnResponse.getRspCode()).isEqualTo("97");
        assertThat(ipnResponse.getMessage()).isEqualTo("Invalid Checksum");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // =========================================================================
    // SEC-13-08: Replay request / Giao dịch VNPay IPN trùng lặp
    // =========================================================================
    @Test
    @DisplayName("SEC-13-08: Replay callback cho đơn hàng đã PAID trả về RspCode 02 (Order already confirmed), không ghi đè thanh toán")
    void sec13_08_duplicateVNPayIpnReplayAttack_handledIdempotently() {
        Order paidOrder = Order.builder()
                .id(60L)
                .orderCode("ORD-PAID-ALREADY")
                .user(testUser)
                .totalAmount(new BigDecimal("1500000"))
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        Payment existingPayment = Payment.builder()
                .id(70L)
                .order(paidOrder)
                .status(PaymentStatus.PAID)
                .transactionCode("PAY-ALREADY-PAID")
                .amount(new BigDecimal("1500000"))
                .build();

        PaymentTransaction tx = PaymentTransaction.builder()
                .id(80L)
                .payment(existingPayment)
                .order(paidOrder)
                .txnRef("PAY-ALREADY-PAID")
                .amount(new BigDecimal("1500000"))
                .status(PaymentStatus.PAID)
                .build();

        when(vnPayConfig.getHashSecret()).thenReturn(VNP_HASH_SECRET);
        when(vnPayConfig.getTmnCode()).thenReturn(VNP_TMN_CODE);
        when(paymentTransactionRepository.findByTxnRefForUpdate("PAY-ALREADY-PAID")).thenReturn(Optional.of(tx));

        Map<String, String> validParams = new HashMap<>();
        validParams.put("vnp_TmnCode", VNP_TMN_CODE);
        validParams.put("vnp_Amount", "150000000");
        validParams.put("vnp_TxnRef", "PAY-ALREADY-PAID");
        validParams.put("vnp_ResponseCode", "00");
        validParams.put("vnp_TransactionStatus", "00");
        validParams.put("vnp_TransactionNo", "14999999");
        validParams.put("vnp_PayDate", "20261111120000");

        // Tạo chữ ký hợp lệ
        String secureHash = VNPayUtil.hashAllFields(validParams, VNP_HASH_SECRET);
        validParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse ipnResponse = paymentServiceImpl.processVNPayIpn(validParams, null);

        // RspCode "02": Order already confirmed (chuẩn VNPay IPN Idempotency)
        assertThat(ipnResponse.getRspCode()).isEqualTo("02");
        assertThat(ipnResponse.getMessage()).isEqualTo("Order already confirmed");

        // Không cập nhật lại đơn hàng
        verify(orderRepository, never()).save(any());
    }
}
