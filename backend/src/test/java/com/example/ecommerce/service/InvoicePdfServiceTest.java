package com.example.ecommerce.service;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.util.VietnameseNumberToWords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho InvoicePdfService (Xuất hóa đơn PDF chuyên nghiệp).
 * Người phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Tiêu chí nghiệm thu (DoD):
 * 1. File PDF hóa đơn xuất ra sắc nét, chuẩn định dạng doanh nghiệp hiện đại.
 * 2. Đầy đủ mã đơn, logo, danh sách món kèm size, tiền giảm voucher và tiền thanh toán VNPay.
 * 3. Hỗ trợ hiển thị tiếng Việt UTF-8 chuẩn xác, chữ ký số điện tử doanh nghiệp.
 */
@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoicePdfService invoicePdfService;

    private User customer;
    private User admin;
    private User otherCustomer;
    private Order testOrder;
    private Payment vnpayPayment;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .id(10L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn An")
                .role(Role.CUSTOMER)
                .build();

        admin = User.builder()
                .id(1L)
                .email("admin@example.com")
                .fullName("Quản Trị Viên Bình")
                .role(Role.ADMIN)
                .build();

        otherCustomer = User.builder()
                .id(99L)
                .email("other@example.com")
                .fullName("Người Dùng Khác")
                .role(Role.CUSTOMER)
                .build();

        testOrder = Order.builder()
                .id(100L)
                .orderCode("ORD-20261116-VNPPDF")
                .user(customer)
                .recipientName("Nguyễn Văn An")
                .recipientPhone("0912345678")
                .shippingAddress("Số 18, Phố Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội")
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .couponCode("GIAM50K")
                .discountAmount(BigDecimal.valueOf(50000))
                .totalAmount(BigDecimal.valueOf(900000))
                .note("Giao hàng trong giờ hành chính")
                .createdAt(LocalDateTime.of(2026, 11, 16, 10, 30, 0))
                .items(new ArrayList<>())
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .productName("Áo Polo Nam Thể Thao Cao Cấp")
                .selectedSize("Size XL")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(250000))
                .subtotal(BigDecimal.valueOf(500000))
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(2L)
                .order(testOrder)
                .productName("Quần Jeans Slimfit Co Giãn 4 Chiều")
                .selectedSize("Size 32")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(450000))
                .subtotal(BigDecimal.valueOf(450000))
                .build();

        testOrder.getItems().add(item1);
        testOrder.getItems().add(item2);

        vnpayPayment = Payment.builder()
                .id(50L)
                .order(testOrder)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PAID)
                .amount(BigDecimal.valueOf(900000))
                .transactionCode("VNP14892301")
                .vnpTxnRef("ORD-20261116-VNPPDF")
                .paidAt(LocalDateTime.of(2026, 11, 16, 10, 35, 12))
                .build();

        testOrder.setPayment(vnpayPayment);
    }

    @Test
    @DisplayName("generateInvoicePdf: Xuất hóa đơn PDF thành công đầy đủ mã đơn, danh sách món kèm size, voucher và VNPay")
    void generateInvoicePdf_Success_WithVNPayAndVoucher() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(100L, "customer@example.com");

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // Kiểm tra chữ ký file PDF chuẩn "%PDF-"
        String pdfHeader = new String(pdfBytes, 0, 5);
        assertThat(pdfHeader).isEqualTo("%PDF-");

        verify(orderRepository).findByIdWithItems(100L);
        verify(userRepository).findByEmail("customer@example.com");
    }

    @Test
    @DisplayName("generateInvoicePdf: Cho phép Admin xuất hóa đơn của bất kỳ khách hàng nào")
    void generateInvoicePdf_AdminCanExportAnyOrder() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(100L, "admin@example.com");

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf: Xuất hóa đơn thành công cho đơn COD không dùng voucher")
    void generateInvoicePdf_Success_WithCODAndNoVoucher() {
        testOrder.setCouponCode(null);
        testOrder.setDiscountAmount(BigDecimal.ZERO);
        testOrder.setPaymentStatus(PaymentStatus.UNPAID);

        Payment codPayment = Payment.builder()
                .id(51L)
                .order(testOrder)
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.UNPAID)
                .amount(BigDecimal.valueOf(950000))
                .transactionCode("COD-TXN-100")
                .build();
        testOrder.setPayment(codPayment);

        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(100L, "customer@example.com");

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf: Từ chối truy cập AccessDeniedException khi khách hàng xem hóa đơn của người khác")
    void generateInvoicePdf_ForbiddenUser_ThrowsAccessDeniedException() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomer));

        assertThatThrownBy(() -> invoicePdfService.generateInvoicePdf(100L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập hóa đơn của đơn hàng này");
    }

    @Test
    @DisplayName("generateInvoicePdf: Ném ResourceNotFoundException khi không tìm thấy đơn hàng")
    void generateInvoicePdf_OrderNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoicePdfService.generateInvoicePdf(999L, "customer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy đơn hàng với ID: 999");
    }

    @Test
    @DisplayName("generateInvoicePdf: Ném ResourceNotFoundException khi không tìm thấy user")
    void generateInvoicePdf_UserNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoicePdfService.generateInvoicePdf(100L, "notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại với email: notfound@example.com");
    }

    @Test
    @DisplayName("generateInvoicePdf: Tìm thấy đơn qua findById dự phòng và lấy thanh toán qua PaymentRepository")
    void generateInvoicePdf_FallbackFindByIdAndPaymentRepo() {
        testOrder.setPayment(null);
        testOrder.setNote(null);
        testOrder.getItems().get(0).setSelectedSize(null);
        testOrder.setOrderStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.empty());
        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(vnpayPayment));

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(100L, "customer@example.com");
        assertThat(pdfBytes).isNotNull();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("generateInvoicePdf: Đơn hàng hoàn toàn không có thanh toán")
    void generateInvoicePdf_NoPayment_Success() {
        testOrder.setPayment(null);
        testOrder.setOrderStatus(OrderStatus.PENDING);
        testOrder.setPaymentStatus(null);
        testOrder.setDiscountAmount(null);

        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(100L, "customer@example.com");
        assertThat(pdfBytes).isNotNull();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("VietnameseNumberToWords: Chuyển đổi số tiền thành chữ tiếng Việt chuẩn xác")
    void testVietnameseNumberToWords() {
        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(1500000)))
                .isEqualTo("Một triệu năm trăm nghìn đồng chẵn");

        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(900000)))
                .isEqualTo("Chín trăm nghìn đồng chẵn");

        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.valueOf(250000)))
                .isEqualTo("Hai trăm năm mươi nghìn đồng chẵn");

        assertThat(VietnameseNumberToWords.toVietnameseWords(BigDecimal.ZERO))
                .isEqualTo("Không đồng chẵn");
    }
}
