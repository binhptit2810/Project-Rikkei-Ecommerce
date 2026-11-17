package com.example.ecommerce.service;

import com.example.ecommerce.config.SePayConfig;
import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.dto.request.SePayWebhookPayload;
import com.example.ecommerce.dto.response.SePayPaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.PaymentTransactionRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho tính năng Thanh toán Quét mã VietQR SePay (MBBank: 0372987662).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@ExtendWith(MockitoExtension.class)
class SePayPaymentTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private EmailService emailService;

    private SePayConfig sePayConfig;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        sePayConfig = new SePayConfig();
        sePayConfig.setBankCode("MBBank");
        sePayConfig.setAccountNumber("0372987662");
        sePayConfig.setAccountName("DANG THANH BINH");
        sePayConfig.setApiKey("rikkei_sepay_secret_2026");
        sePayConfig.setQrTemplate("compact2");

        paymentService = new PaymentService(
                paymentRepository,
                orderRepository,
                userRepository,
                new VNPayConfig(),
                emailService,
                paymentTransactionRepository,
                sePayConfig
        );
    }

    @Test
    @DisplayName("Lấy thông tin VietQR SePay: Sinh link ảnh VietQR MBBank chính xác theo mã đơn và số tiền")
    void testGetSePayPaymentInfo_Success() {
        Order order = Order.builder()
                .id(100L)
                .orderCode("ORD-20261117-ABC")
                .totalAmount(new BigDecimal("250000"))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        SePayPaymentResponse response = paymentService.getSePayPaymentInfo(100L, null);

        assertNotNull(response);
        assertEquals(100L, response.getOrderId());
        assertEquals("ORD-20261117-ABC", response.getOrderCode());
        assertEquals("MBBank", response.getBankCode());
        assertEquals("0372987662", response.getAccountNumber());
        assertEquals("DANG THANH BINH", response.getAccountName());
        assertTrue(response.getQrCodeUrl().contains("acc=0372987662"));
        assertTrue(response.getQrCodeUrl().contains("bank=MBBank"));
        assertTrue(response.getQrCodeUrl().contains("amount=250000"));
        assertTrue(response.getQrCodeUrl().contains("des=ORD-20261117-ABC"));
        assertTrue(response.getQrCodeUrl().contains("template=compact2"));
    }

    @Test
    @DisplayName("Webhook SePay: Xác nhận thanh toán thành công và chuyển đơn hàng sang CONFIRMED khi nhận được tiền")
    void testHandleSePayWebhook_Success() {
        Order order = Order.builder()
                .id(101L)
                .orderCode("ORD-20261117-999")
                .totalAmount(new BigDecimal("500000"))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        Payment payment = Payment.builder()
                .id(201L)
                .order(order)
                .amount(new BigDecimal("500000"))
                .status(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.SEPAY)
                .build();

        when(orderRepository.findByOrderCode("ORD-20261117-999")).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SePayWebhookPayload payload = SePayWebhookPayload.builder()
                .id(88888L)
                .gateway("MBBank")
                .accountNumber("0372987662")
                .transferAmount(new BigDecimal("500000"))
                .transferType("in")
                .content("MBVCB.123456 ORD-20261117-999 thanh toan giay")
                .referenceCode("MB-REF-123456")
                .build();

        Map<String, Object> result = paymentService.handleSePayWebhook(payload, "Apikey rikkei_sepay_secret_2026");

        assertTrue((Boolean) result.get("success"));
        assertEquals("ORD-20261117-999", result.get("orderCode"));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getOrderStatus());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertNotNull(payment.getPaidAt());

        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Webhook SePay: Từ chối request khi API Key trong header không hợp lệ")
    void testHandleSePayWebhook_InvalidApiKey_ThrowsException() {
        SePayWebhookPayload payload = SePayWebhookPayload.builder()
                .id(88888L)
                .transferType("in")
                .content("ORD-20261117-999")
                .build();

        assertThrows(BusinessException.class, () ->
                paymentService.handleSePayWebhook(payload, "Apikey wrong_api_key")
        );
    }

    @Test
    @DisplayName("Webhook SePay: Bỏ qua biến động số dư tiền ra (transferType=out)")
    void testHandleSePayWebhook_OutgoingTransfer_Ignored() {
        SePayWebhookPayload payload = SePayWebhookPayload.builder()
                .id(88889L)
                .transferType("out")
                .transferAmount(new BigDecimal("100000"))
                .content("Chuyen tien tra hang")
                .build();

        Map<String, Object> result = paymentService.handleSePayWebhook(payload, "Apikey rikkei_sepay_secret_2026");

        assertTrue((Boolean) result.get("success"));
        assertEquals("Bỏ qua giao dịch không phải tiền vào", result.get("message"));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Webhook SePay: Không ghi đè khi đơn hàng đã thanh toán trước đó")
    void testHandleSePayWebhook_AlreadyPaid_ReturnsSuccessWithoutReProcessing() {
        Order order = Order.builder()
                .id(102L)
                .orderCode("ORD-20261117-ALREADY")
                .totalAmount(new BigDecimal("300000"))
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .build();

        Payment payment = Payment.builder()
                .id(202L)
                .order(order)
                .status(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.SEPAY)
                .build();

        when(orderRepository.findByOrderCode("ORD-20261117-ALREADY")).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(102L)).thenReturn(Optional.of(payment));

        SePayWebhookPayload payload = SePayWebhookPayload.builder()
                .id(88890L)
                .transferType("in")
                .transferAmount(new BigDecimal("300000"))
                .content("ORD-20261117-ALREADY")
                .build();

        Map<String, Object> result = paymentService.handleSePayWebhook(payload, "Apikey rikkei_sepay_secret_2026");

        assertTrue((Boolean) result.get("success"));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
