package com.example.ecommerce.service;

import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.dto.request.CreateVNPayPaymentRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.dto.response.VNPayIpnResponse;
import com.example.ecommerce.dto.response.VNPayPaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.PaymentTransactionRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho PaymentService (Module Thanh toán).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Kiểm tra các nghiệp vụ thanh toán mô phỏng COD / SIMULATED và sinh mã giao dịch định dạng PAY-.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private EmailService emailService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User customer;
    private User otherCustomer;
    private User admin;
    private Order testOrder;
    private Payment existingPayment;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        otherCustomer = User.builder()
                .id(2L)
                .email("other@example.com")
                .role(Role.CUSTOMER)
                .build();

        admin = User.builder()
                .id(99L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        testOrder = Order.builder()
                .id(10L)
                .orderCode("ORD-20261103-TEST01")
                .user(customer)
                .totalAmount(BigDecimal.valueOf(1500000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        existingPayment = Payment.builder()
                .id(100L)
                .order(testOrder)
                .transactionCode("PAY-20261103-A1B2C3D4")
                .amount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.UNPAID)
                .build();
    }

    @Nested
    @DisplayName("Khởi tạo thanh toán ban đầu (createInitialPayment)")
    class CreateInitialPaymentTests {

        @Test
        @DisplayName("Khởi tạo thanh toán COD ban đầu thành công với mã định danh tiền tố PAY-")
        void createInitialPayment_Success() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Payment payment = paymentService.createInitialPayment(testOrder, PaymentMethod.COD);

            assertThat(payment).isNotNull();
            assertThat(payment.getOrder()).isEqualTo(testOrder);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
            assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
            assertThat(payment.getTransactionCode()).isNotNull().startsWith("PAY-");
            assertThat(payment.getTransactionCode()).matches("^PAY-\\d{8}-[A-F0-9]{8}$");

            verify(paymentRepository, times(1)).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Xử lý thanh toán đơn hàng (processPayment)")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Thanh toán COD thành công: trạng thái UNPAID, mã giao dịch có tiền tố PAY-")
        void processPayment_COD_Success() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.COD)
                    .transactionNote("Giao giờ hành chính")
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getTransactionCode()).startsWith("PAY-");
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
            assertThat(response.getTransactionNote()).isEqualTo("Giao giờ hành chính");
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }

        @Test
        @DisplayName("Thanh toán SIMULATED thành công: trạng thái PAID, paidAt được cập nhật, đơn chuyển CONFIRMED")
        void processPayment_Simulated_Success() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .simulateSuccess(true)
                    .transactionNote("Mô phỏng thanh toán thẻ tín dụng thành công")
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getTransactionCode()).startsWith("PAY-");
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.SIMULATED);
            assertThat(response.getPaidAt()).isNotNull();
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Thanh toán SIMULATED giả lập thất bại: trạng thái FAILED, đơn ghi nhận FAILED")
        void processPayment_Simulated_Failure() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .simulateSuccess(false)
                    .transactionNote("Số dư tài khoản không đủ")
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(response.getPaidAt()).isNull();
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Thanh toán SIMULATED với status = FAILED: trạng thái FAILED, đơn ghi nhận FAILED")
        void processPayment_Simulated_StatusFailed() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .status(PaymentStatus.FAILED)
                    .transactionNote("Giao dịch bị từ chối")
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Thanh toán SIMULATED với status = PAID: trạng thái PAID, đơn chuyển CONFIRMED")
        void processPayment_Simulated_StatusPaid() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .status(PaymentStatus.PAID)
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Thanh toán với request null: mặc định mô phỏng SIMULATED thành công (PAID)")
        void processPayment_NullRequest_Success() {
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, null, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("Ném BusinessException khi đơn hàng đã bị hủy (CANCELLED)")
        void processPayment_ThrowsException_WhenOrderCancelled() {
            testOrder.setOrderStatus(OrderStatus.CANCELLED);
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> paymentService.processPayment(10L, request, customer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        @Test
        @DisplayName("Ném BusinessException khi đơn hàng đã được thanh toán trước đó (PAID)")
        void processPayment_ThrowsException_WhenOrderAlreadyPaid() {
            testOrder.setPaymentStatus(PaymentStatus.PAID);
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> paymentService.processPayment(10L, request, customer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Đơn hàng này đã được thanh toán thành công trước đó");
        }

        @Test
        @DisplayName("Ném BusinessException khi người dùng khác cố gắng thanh toán đơn hàng")
        void processPayment_ThrowsException_WhenUnauthorizedUser() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(otherCustomer.getEmail())).thenReturn(Optional.of(otherCustomer));

            assertThatThrownBy(() -> paymentService.processPayment(10L, request, otherCustomer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Bạn không có quyền thanh toán đơn hàng này");
        }

        @Test
        @DisplayName("Admin có quyền thanh toán thay cho khách hàng")
        void processPayment_Success_WhenAdminProcessesPayment() {
            ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .simulateSuccess(true)
                    .build();

            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.processPayment(10L, request, admin.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        }
    }

    @Nested
    @DisplayName("Tra cứu thông tin thanh toán (getPaymentByOrderId / getPaymentByTransactionCode)")
    class LookupPaymentTests {

        @Test
        @DisplayName("Tra cứu thông tin thanh toán theo mã giao dịch PAY- thành công")
        void getPaymentByTransactionCode_Success() {
            String code = "PAY-20261103-A1B2C3D4";
            when(paymentRepository.findByTransactionCode(code)).thenReturn(Optional.of(existingPayment));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            PaymentResponse response = paymentService.getPaymentByTransactionCode(code, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getTransactionCode()).isEqualTo(code);
            assertThat(response.getOrderId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Tra cứu bằng mã giao dịch rỗng ném BusinessException")
        void getPaymentByTransactionCode_ThrowsException_WhenCodeBlank() {
            assertThatThrownBy(() -> paymentService.getPaymentByTransactionCode("   ", customer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mã giao dịch không được để trống");
        }

        @Test
        @DisplayName("Tra cứu bằng mã giao dịch không tồn tại ném ResourceNotFoundException")
        void getPaymentByTransactionCode_ThrowsException_WhenNotFound() {
            when(paymentRepository.findByTransactionCode("PAY-NOTFOUND")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByTransactionCode("PAY-NOTFOUND", customer.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy giao dịch thanh toán");
        }

        @Test
        @DisplayName("Tra cứu thông tin thanh toán theo ID đơn hàng thành công")
        void getPaymentByOrderId_Success() {
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));

            PaymentResponse response = paymentService.getPaymentByOrderId(10L, customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(10L);
            assertThat(response.getTransactionCode()).startsWith("PAY-");
        }
    }

    @Nested
    @DisplayName("Thanh toán VNPay Sandbox (createVNPayPayment & processVNPayCallback)")
    class VNPayPaymentTests {

        private final String secretKey = "RAIQUIBGCARJHNLKZCPGUCUMRHQKGZDF";

        @Test
        @DisplayName("createVNPayPayment: Khởi tạo URL VNPay Sandbox thành công với đầy đủ chữ ký bảo mật")
        void createVNPayPayment_Success() {
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(vnPayConfig.getVersion()).thenReturn("2.1.0");
            when(vnPayConfig.getCommand()).thenReturn("pay");
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");
            when(vnPayConfig.getCurrCode()).thenReturn("VND");
            when(vnPayConfig.getOrderType()).thenReturn("other");
            when(vnPayConfig.getLocale()).thenReturn("vn");
            when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:3000/orders/vnpay-return");
            when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                    .orderId(10L)
                    .bankCode("NCB")
                    .orderInfo("Thanh toan don hang test")
                    .build();

            VNPayPaymentResponse response = paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail());

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(10L);
            assertThat(response.getPaymentUrl()).contains("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
            assertThat(response.getPaymentUrl()).contains("vnp_TmnCode=2QXUI4J4");
            assertThat(response.getPaymentUrl()).contains("vnp_BankCode=NCB");
            assertThat(response.getPaymentUrl()).contains("vnp_SecureHash=");
            assertThat(response.getVnpTxnRef()).isNotNull();
        }

        @Test
        @DisplayName("createVNPayPayment: Đơn hàng đã hủy ném BusinessException")
        void createVNPayPayment_CancelledOrder_ThrowsException() {
            testOrder.setOrderStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                    .orderId(10L)
                    .build();

            assertThatThrownBy(() -> paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        @Test
        @DisplayName("createVNPayPayment: Đơn hàng đã thanh toán PAID ném BusinessException")
        void createVNPayPayment_AlreadyPaid_ThrowsException() {
            testOrder.setPaymentStatus(PaymentStatus.PAID);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

            CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                    .orderId(10L)
                    .build();

            assertThatThrownBy(() -> paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Đơn hàng này đã được thanh toán thành công");
        }

        @Test
        @DisplayName("processVNPayCallback: Xác thực chữ ký hợp lệ nhưng không tự xác nhận thanh toán (chờ IPN xác nhận)")
        void processVNPayCallback_Success() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_BankCode", "NCB");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_TransactionNo", "14567890");

            String secureHash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", secureHash);

            when(paymentRepository.findByTransactionCode(existingPayment.getTransactionCode()))
                    .thenReturn(Optional.of(existingPayment));

            PaymentResponse response = paymentService.processVNPayCallback(queryParams);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(testOrder.getId());
            // Return URL không tự ý chuyển OrderStatus sang CONFIRMED hay gửi email, chờ IPN xác nhận
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            verify(emailService, never()).sendOrderConfirmationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("processVNPayCallback: Chữ ký không hợp lệ ném BusinessException")
        void processVNPayCallback_InvalidSignature_ThrowsException() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", "PAY-INVALID");
            queryParams.put("vnp_SecureHash", "invalid-hash-value");

            assertThatThrownBy(() -> paymentService.processVNPayCallback(queryParams))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Chữ ký bảo mật VNPay không hợp lệ");
        }

        @Test
        @DisplayName("createVNPayPayment: Mỗi lần gọi retry sinh ra vnp_TxnRef mới và lưu PaymentTransaction")
        void createVNPayPayment_GeneratesNewTxnRefOnRetry() {
            when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                    .orderId(10L)
                    .build();

            // Lần gọi 1
            VNPayPaymentResponse res1 = paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail());
            String txnRef1 = res1.getVnpTxnRef();

            // Lần gọi 2 (Retry)
            VNPayPaymentResponse res2 = paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail());
            String txnRef2 = res2.getVnpTxnRef();

            assertThat(txnRef1).isNotNull().startsWith("PAY-");
            assertThat(txnRef2).isNotNull().startsWith("PAY-");
            assertThat(txnRef1).isNotEqualTo(txnRef2);
            verify(paymentTransactionRepository, times(2)).save(any(PaymentTransaction.class));
        }

        @Test
        @DisplayName("processVNPayIpn: Giao dịch thành công (00 & 00) cập nhật Payment và Order sang PAID và CONFIRMED")
        void processVNPayIpn_Success() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-20261110-SUCCESS01";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(1L)
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_BankCode", "NCB");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_TxnRef", txnRef);
            queryParams.put("vnp_TransactionNo", "99887766");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("00");
            assertThat(ipnResponse.getMessage()).isEqualTo("Confirm Success");
            assertThat(tx.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

            verify(orderRepository).save(testOrder);
            verify(paymentRepository).save(existingPayment);
            verify(paymentTransactionRepository).save(tx);
        }

        @Test
        @DisplayName("processVNPayIpn: Giao dịch thất bại (ResponseCode=24) cập nhật FAILED và vẫn trả RspCode 00")
        void processVNPayIpn_FailedPayment() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-20261110-FAIL01";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .id(2L)
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "24");
            queryParams.put("vnp_TransactionStatus", "02");
            queryParams.put("vnp_TxnRef", txnRef);
            queryParams.put("vnp_TransactionNo", "99887767");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("00");
            assertThat(ipnResponse.getMessage()).isEqualTo("Confirm Success");
            assertThat(tx.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("processVNPayIpn: Sai chữ ký bảo mật trả về RspCode 97 (Invalid Checksum)")
        void processVNPayIpn_InvalidChecksum() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", "PAY-TEST");
            queryParams.put("vnp_SecureHash", "invalid-hash-string-that-fails-verification-1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678");

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("97");
            assertThat(ipnResponse.getMessage()).isEqualTo("Invalid Checksum");
            verifyNoInteractions(paymentTransactionRepository);
        }

        @Test
        @DisplayName("processVNPayIpn: Sai mã merchant vnp_TmnCode trả về RspCode 97")
        void processVNPayIpn_InvalidTmnCode() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "WRONG_TMN");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", "PAY-TEST");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("97");
            assertThat(ipnResponse.getMessage()).isEqualTo("Invalid Checksum");
        }

        @Test
        @DisplayName("processVNPayIpn: Không tìm thấy giao dịch trả về RspCode 01 (Order not Found)")
        void processVNPayIpn_OrderNotFound() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-NOT-FOUND";
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", txnRef);

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.empty());
            when(paymentRepository.findByTransactionCodeForUpdate(txnRef)).thenReturn(Optional.empty());

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("01");
            assertThat(ipnResponse.getMessage()).isEqualTo("Order not Found");
        }

        @Test
        @DisplayName("processVNPayIpn: Sai lệch số tiền trả về RspCode 04 (Invalid Amount)")
        void processVNPayIpn_InvalidAmount() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-AMOUNT-MISMATCH";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "999999999"); // Sai số tiền
            queryParams.put("vnp_TxnRef", txnRef);

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("04");
            assertThat(ipnResponse.getMessage()).isEqualTo("Invalid Amount");
        }

        @Test
        @DisplayName("processVNPayIpn: Giao dịch đã được xác nhận PAID trước đó trả RspCode 02 (Order already confirmed)")
        void processVNPayIpn_AlreadyConfirmed_Idempotency() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-ALREADY-PAID";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.PAID) // Đã PAID
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", txnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("02");
            assertThat(ipnResponse.getMessage()).isEqualTo("Order already confirmed");
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("processVNPayIpn: Đơn hàng đã CANCELLED nhận IPN thành công -> cập nhật REFUNDED, giữ nguyên CANCELLED, trả RspCode 00")
        void processVNPayIpn_OrderCancelled_HandledGracefully() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            testOrder.setOrderStatus(OrderStatus.CANCELLED);

            String txnRef = "PAY-CANCELLED-ORDER";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", txnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_TransactionNo", "11223344");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("00");
            assertThat(ipnResponse.getMessage()).isEqualTo("Confirm Success");
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED); // Không bị đổi sang CONFIRMED
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(tx.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(tx.getReconciliationStatus()).isEqualTo(ReconciliationStatus.REFUND_PENDING);
        }

        @Test
        @DisplayName("processVNPayIpn: ResponseCode=00 nhưng TransactionStatus!=00 không được coi là thành công")
        void processVNPayIpn_TransactionStatusNotOk() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String txnRef = "PAY-STATUS-NOT-OK";
            PaymentTransaction tx = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(txnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", txnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "01"); // Chưa hoàn tất (Pending)

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(txnRef)).thenReturn(Optional.of(tx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("00");
            assertThat(tx.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("processVNPayIpn: IPN thất bại của attempt cũ không ghi đè Payment đã PAID")
        void processVNPayIpn_FailedOldAttemptDoesNotOverwritePaidPayment() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            // Payment đã PAID bởi attempt khác
            existingPayment.setStatus(PaymentStatus.PAID);
            existingPayment.setTransactionCode("PAY-NEW-ATTEMPT-PAID");

            String oldTxnRef = "PAY-OLD-ATTEMPT";
            PaymentTransaction oldTx = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(oldTxnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", oldTxnRef);
            queryParams.put("vnp_ResponseCode", "24"); // Khách hủy
            queryParams.put("vnp_TransactionStatus", "02");

            String hash = VNPayUtil.hashAllFields(queryParams, secretKey);
            queryParams.put("vnp_SecureHash", hash);

            when(paymentTransactionRepository.findByTxnRefForUpdate(oldTxnRef)).thenReturn(Optional.of(oldTx));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, null);

            assertThat(ipnResponse.getRspCode()).isEqualTo("00");
            assertThat(oldTx.getStatus()).isEqualTo(PaymentStatus.FAILED);
            // Parent payment vẫn giữ nguyên trạng thái PAID!
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("processVNPayIpn: Phát hiện Parameter Pollution (HPP) trả về RspCode 97")
        void processVNPayIpn_ParameterPollution() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Map<String, String[]> paramMap = new HashMap<>();
            paramMap.put("vnp_Amount", new String[]{"10000000", "20000000"});
            when(request.getParameterMap()).thenReturn(paramMap);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "10000000");

            VNPayIpnResponse ipnResponse = paymentService.processVNPayIpn(queryParams, request);

            assertThat(ipnResponse.getRspCode()).isEqualTo("97");
            assertThat(ipnResponse.getMessage()).isEqualTo("Invalid Checksum");
        }

        @Test
        @DisplayName("Late IPN: T1 FAILED không làm T2 UNPAID thành FAILED và không đổi trạng thái Order PENDING")
        void lateIpn_T1Failed_DoesNotFailActiveT2OrPendingOrder() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            // Order và Payment đang chờ T2
            existingPayment.setStatus(PaymentStatus.UNPAID);
            existingPayment.setTransactionCode("PAY-ATTEMPT-T2");
            testOrder.setOrderStatus(OrderStatus.PENDING);
            testOrder.setPaymentStatus(PaymentStatus.UNPAID);

            String t1TxnRef = "PAY-ATTEMPT-T1";
            PaymentTransaction tx1 = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(t1TxnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", t1TxnRef);
            queryParams.put("vnp_ResponseCode", "24"); // Khách hủy giao dịch T1
            queryParams.put("vnp_TransactionStatus", "02");
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            PaymentTransaction tx2 = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef("PAY-ATTEMPT-T2")
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            when(paymentTransactionRepository.findByTxnRefForUpdate(t1TxnRef)).thenReturn(Optional.of(tx1));
            when(paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(testOrder.getId())).thenReturn(java.util.List.of(tx2, tx1));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse response = paymentService.processVNPayIpn(queryParams, null);

            assertThat(response.getRspCode()).isEqualTo("00");
            assertThat(tx1.getStatus()).isEqualTo(PaymentStatus.FAILED);
            // Quan trọng: Attempt T1 thất bại không được làm payment/order của T2 thành FAILED!
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(existingPayment.getTransactionCode()).isEqualTo("PAY-ATTEMPT-T2");
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Late IPN: T1 PAID đến muộn vẫn được ghi nhận đúng khi T2 đã FAILED")
        void lateIpn_T1Paid_WhenT2Failed_RecordsPaymentSuccessfully() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            // T2 đã bị FAILED trước đó
            existingPayment.setStatus(PaymentStatus.FAILED);
            existingPayment.setTransactionCode("PAY-ATTEMPT-T2");
            testOrder.setOrderStatus(OrderStatus.PENDING);
            testOrder.setPaymentStatus(PaymentStatus.FAILED);

            String t1TxnRef = "PAY-ATTEMPT-T1";
            PaymentTransaction tx1 = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(t1TxnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", t1TxnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_TransactionNo", "99887766");
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentTransactionRepository.findByTxnRefForUpdate(t1TxnRef)).thenReturn(Optional.of(tx1));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse response = paymentService.processVNPayIpn(queryParams, null);

            assertThat(response.getRspCode()).isEqualTo("00");
            assertThat(tx1.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(existingPayment.getTransactionCode()).isEqualTo(t1TxnRef);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Double Payment: T1 PAID, T2 PAID -> Giao dịch T1 được giữ chính, T2 đánh dấu OVERPAYMENT, không gửi email lần 2")
        void doublePayment_T1AndT2Paid_MarksOverpaymentAndNoDuplicateEmail() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            // T1 đã thanh toán thành công trước
            existingPayment.setStatus(PaymentStatus.PAID);
            existingPayment.setTransactionCode("PAY-ATTEMPT-T1");
            testOrder.setOrderStatus(OrderStatus.CONFIRMED);
            testOrder.setPaymentStatus(PaymentStatus.PAID);

            String t2TxnRef = "PAY-ATTEMPT-T2";
            PaymentTransaction tx2 = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(t2TxnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.UNPAID)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", t2TxnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_TransactionNo", "22222222");
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentTransactionRepository.findByTxnRefForUpdate(t2TxnRef)).thenReturn(Optional.of(tx2));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse response = paymentService.processVNPayIpn(queryParams, null);

            assertThat(response.getRspCode()).isEqualTo("00");
            assertThat(tx2.getStatus()).isEqualTo(PaymentStatus.PAID);
            // T2 phải được đánh dấu OVERPAYMENT
            assertThat(tx2.getReconciliationStatus()).isEqualTo(ReconciliationStatus.OVERPAYMENT);
            // Mã giao dịch chính trên Payment không bị ghi đè
            assertThat(existingPayment.getTransactionCode()).isEqualTo("PAY-ATTEMPT-T1");
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

            // Xác minh emailService KHÔNG bị gọi thêm lần nữa
            verify(emailService, times(0)).sendOrderConfirmationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("Double Payment: Duplicate IPN của cùng transaction T1 không bị nhận nhầm thành OVERPAYMENT")
        void doublePayment_DuplicateIpnOfSameTransaction_ReturnsAlreadyConfirmed() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            String t1TxnRef = "PAY-ATTEMPT-T1";
            existingPayment.setStatus(PaymentStatus.PAID);
            existingPayment.setTransactionCode(t1TxnRef);

            PaymentTransaction tx1 = PaymentTransaction.builder()
                    .payment(existingPayment)
                    .order(testOrder)
                    .txnRef(t1TxnRef)
                    .amount(BigDecimal.valueOf(1500000))
                    .status(PaymentStatus.PAID) // Đã PAID trước
                    .reconciliationStatus(ReconciliationStatus.NONE)
                    .build();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "2QXUI4J4");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_TxnRef", t1TxnRef);
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentTransactionRepository.findByTxnRefForUpdate(t1TxnRef)).thenReturn(Optional.of(tx1));
            when(paymentRepository.findByOrderIdForUpdate(testOrder.getId())).thenReturn(Optional.of(existingPayment));

            VNPayIpnResponse response = paymentService.processVNPayIpn(queryParams, null);

            assertThat(response.getRspCode()).isEqualTo("02"); // Already confirmed
            assertThat(tx1.getReconciliationStatus()).isEqualTo(ReconciliationStatus.NONE); // Không phải OVERPAYMENT
        }

        @Test
        @DisplayName("Return URL: Sai số tiền vnp_Amount ném BusinessException và bị từ chối")
        void returnUrl_InvalidAmount_ThrowsBusinessException() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "999999"); // Sai số tiền
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentRepository.findByTransactionCode(existingPayment.getTransactionCode()))
                    .thenReturn(Optional.of(existingPayment));

            assertThatThrownBy(() -> paymentService.processVNPayCallback(queryParams))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Số tiền thanh toán không khớp");
        }

        @Test
        @DisplayName("Return URL: Sai mã merchant vnp_TmnCode ném BusinessException và bị từ chối")
        void returnUrl_InvalidTmnCode_ThrowsBusinessException() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TmnCode", "WRONG_MERCHANT");
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            assertThatThrownBy(() -> paymentService.processVNPayCallback(queryParams))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Mã định danh Merchant không khớp");
        }

        @Test
        @DisplayName("Return URL: TransactionStatus khác 00 không được xác nhận thành công")
        void returnUrl_TransactionStatusFailed_NotConfirmed() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "02"); // Giao dịch lỗi tại VNPay
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentRepository.findByTransactionCode(existingPayment.getTransactionCode()))
                    .thenReturn(Optional.of(existingPayment));

            PaymentResponse response = paymentService.processVNPayCallback(queryParams);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING); // Không bị đổi sang CONFIRMED
        }

        @Test
        @DisplayName("Return URL: Phát hiện Parameter Pollution (HPP) bị từ chối")
        void returnUrl_ParameterPollution_ThrowsBusinessException() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Map<String, String[]> paramMap = new HashMap<>();
            paramMap.put("vnp_TxnRef", new String[]{"PAY-1", "PAY-2"});
            when(request.getParameterMap()).thenReturn(paramMap);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_TxnRef", "PAY-1");

            assertThatThrownBy(() -> paymentService.processVNPayCallback(queryParams, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Parameter Pollution");
        }

        @Test
        @DisplayName("Return URL: Đến trước IPN không thể tự xác nhận nếu không có chữ ký hợp lệ")
        void returnUrl_BeforeIpn_WithoutValidSignature_CannotConfirm() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_SecureHash", "invalid_forged_hash"); // Chữ ký giả mạo từ trình duyệt

            assertThatThrownBy(() -> paymentService.processVNPayCallback(queryParams))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Chữ ký bảo mật VNPay không hợp lệ");

            // Order và Payment vẫn UNPAID / PENDING
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Return URL và IPN đồng thời: Khi IPN đã xác nhận PAID, Return URL không thực hiện cập nhật trùng")
        void returnUrlAndIpn_Concurrent_NoDuplicateUpdate() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            // IPN đã xử lý xong và set PAID trước
            existingPayment.setStatus(PaymentStatus.PAID);
            existingPayment.setPaidAt(LocalDateTime.now());
            testOrder.setOrderStatus(OrderStatus.CONFIRMED);
            testOrder.setPaymentStatus(PaymentStatus.PAID);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentRepository.findByTransactionCode(existingPayment.getTransactionCode()))
                    .thenReturn(Optional.of(existingPayment));

            PaymentResponse response = paymentService.processVNPayCallback(queryParams);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
            // Không gọi save lại payment hoặc order
            verify(emailService, times(0)).sendOrderConfirmationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("createVNPayPayment: Khóa bi quan Order bằng findByIdForUpdate để tuần tự hóa việc tạo attempt")
        void createVNPayPayment_LocksOrderPessimistically() {
            when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testOrder));
            when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
            when(paymentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(existingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(vnPayConfig.getVersion()).thenReturn("2.1.0");
            when(vnPayConfig.getCommand()).thenReturn("pay");
            when(vnPayConfig.getTmnCode()).thenReturn("2QXUI4J4");
            when(vnPayConfig.getCurrCode()).thenReturn("VND");
            when(vnPayConfig.getOrderType()).thenReturn("other");
            when(vnPayConfig.getLocale()).thenReturn("vn");
            when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:3000/orders/vnpay-return");
            when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                    .orderId(10L)
                    .build();

            VNPayPaymentResponse response = paymentService.createVNPayPayment(request, "127.0.0.1", customer.getEmail());

            assertThat(response).isNotNull();
            verify(orderRepository).findByIdForUpdate(10L);
            verify(paymentRepository).findByOrderIdForUpdate(10L);
        }

        @Test
        @DisplayName("processVNPayCallback: Không tự ý cập nhật trạng thái PAID khi IPN chưa tới")
        void processVNPayCallback_DoesNotSelfConfirm_AwaitsIpn() {
            when(vnPayConfig.getHashSecret()).thenReturn(secretKey);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("vnp_Amount", "150000000");
            queryParams.put("vnp_ResponseCode", "00");
            queryParams.put("vnp_TransactionStatus", "00");
            queryParams.put("vnp_TxnRef", existingPayment.getTransactionCode());
            queryParams.put("vnp_TransactionNo", "99887766");
            queryParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(queryParams, secretKey));

            when(paymentRepository.findByTransactionCode(existingPayment.getTransactionCode()))
                    .thenReturn(Optional.of(existingPayment));

            PaymentResponse response = paymentService.processVNPayCallback(queryParams);

            assertThat(response).isNotNull();
            // Trạng thái tài chính vẫn UNPAID, order vẫn PENDING, email chưa được gửi
            assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.UNPAID);
            assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            verify(emailService, never()).sendOrderConfirmationEmail(any(), any(), any());
        }
    }
}

