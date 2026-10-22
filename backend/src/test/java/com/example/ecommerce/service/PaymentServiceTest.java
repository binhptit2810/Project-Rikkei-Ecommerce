package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer1@example.com")
                .fullName("Khách hàng 1")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        testOrder = Order.builder()
                .id(100L)
                .orderCode("ORD-20261022-1111")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(500000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
    }

    @Test
    @DisplayName("Thanh toán COD thành công - Payment và Order có trạng thái UNPAID")
    void processPayment_Success_COD() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(100L, "customer1@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Thanh toán SIMULATED thành công - Payment và Order chuyển sang PAID")
    void processPayment_Success_Simulated() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .transactionNote("Mô phỏng thanh toán ví điện tử")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(100L, "customer1@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.SIMULATED);
        assertThat(response.getPaidAt()).isNotNull();
        assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Thanh toán SIMULATED thất bại - Payment và Order chuyển sang FAILED")
    void processPayment_Failure_Simulated() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(false)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(100L, "customer1@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(testOrder.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Thanh toán thất bại khi đơn hàng đã bị hủy - Ném BusinessException")
    void processPayment_ThrowsException_WhenOrderCancelled() {
        testOrder.setOrderStatus(OrderStatus.CANCELLED);
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> paymentService.processPayment(100L, "customer1@example.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã bị hủy");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thanh toán thất bại khi đơn hàng đã thanh toán thành công trước đó - Ném BusinessException")
    void processPayment_ThrowsException_WhenOrderAlreadyPaid() {
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> paymentService.processPayment(100L, "customer1@example.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được thanh toán thành công");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thanh toán thất bại khi người thực hiện không phải chủ đơn hàng và không phải ADMIN")
    void processPayment_ThrowsException_WhenNotOwnerAndNotAdmin() {
        User anotherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .role(Role.CUSTOMER)
                .build();

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> paymentService.processPayment(100L, "other@example.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn không có quyền thực hiện");
    }
}
