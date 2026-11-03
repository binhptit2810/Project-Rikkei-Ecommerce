package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
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
}

