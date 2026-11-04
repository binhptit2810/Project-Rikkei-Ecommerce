package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private User otherUser;
    private User adminUser;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Trần Thị B")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(999L)
                .email("admin@example.com")
                .fullName("Quản Trị Viên")
                .role(Role.ADMIN)
                .active(true)
                .build();

        product1 = Product.builder()
                .id(101L)
                .name("Áo Thun Nam Cotton")
                .price(BigDecimal.valueOf(250000))
                .stockQuantity(20)
                .active(true)
                .build();

        product2 = Product.builder()
                .id(102L)
                .name("Quần Jean Slimfit")
                .price(BigDecimal.valueOf(500000))
                .stockQuantity(15)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công: tính giá từ DB, lưu snapshot và gọi trừ kho")
    void createOrder_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Thị B")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao giờ hành chính")
                .items(List.of(
                        new OrderItemRequest(101L, 2), // 2 * 250,000 = 500,000
                        new OrderItemRequest(102L, 1)  // 1 * 500,000 = 500,000 -> Total = 1,000,000
                ))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(99L);
            o.setCreatedAt(LocalDateTime.now());
            o.setUpdatedAt(LocalDateTime.now());
            return o;
        });

        Payment dummyPayment = Payment.builder()
                .id(10L)
                .transactionCode("PAY-20261102-12345678")
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.UNPAID)
                .amount(BigDecimal.valueOf(1000000))
                .build();
        when(paymentService.createInitialPayment(any(Order.class), eq(PaymentMethod.COD))).thenReturn(dummyPayment);

        OrderResponse response = orderService.createOrder(request, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getOrderCode()).startsWith("ORD-");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(response.getTransactionCode()).isEqualTo("PAY-20261102-12345678");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItemCount()).isEqualTo(3);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Áo Thun Nam Cotton");
        assertThat(saved.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(saved.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(500000));

        verify(inventoryService).deductStockForOrder(saved);
        verify(paymentService).createInitialPayment(saved, PaymentMethod.COD);
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi sản phẩm không tồn tại trong DB")
    void createOrder_ThrowsException_WhenProductNotFound() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Thị B")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(999L, 1)))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request, "customer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tồn tại");

        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(any());
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi sản phẩm đã ngừng kinh doanh (inactive)")
    void createOrder_ThrowsException_WhenProductInactive() {
        product1.setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Thị B")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(101L, 1)))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        assertThatThrownBy(() -> orderService.createOrder(request, "customer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng kinh doanh");

        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(any());
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi kho không đủ số lượng (InventoryService ném lỗi)")
    void createOrder_Rollback_WhenInventoryFails() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Thị B")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(101L, 100)))
                .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new BusinessException("không đủ số lượng tồn kho"))
                .when(inventoryService).deductStockForOrder(any(Order.class));

        assertThatThrownBy(() -> orderService.createOrder(request, "customer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không đủ số lượng tồn kho");

        verify(paymentService, never()).createInitialPayment(any(), any());
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng có phân trang thành công")
    void getMyOrders_Paged_Success() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(250000))
                .items(new ArrayList<>())
                .build();

        Page<Order> page = new PageImpl<>(List.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<OrderResponse> response = orderService.getMyOrders("customer@example.com", 0, 10, "createdAt", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getOrderCode()).isEqualTo("ORD-20261102-111111");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Customer chưa có đơn hàng: trả về PageResponse rỗng hợp lệ")
    void getMyOrders_EmptyList_ReturnsValidEmptyPage() {
        Page<Order> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<OrderResponse> response = orderService.getMyOrders("customer@example.com", 0, 10, "createdAt", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Lấy chi tiết đơn hàng (getOrderDetail) kèm danh sách món hàng thành công")
    void getOrderDetail_Success_ReturnsDetailWithItems() {
        OrderItem item = OrderItem.builder()
                .id(10L)
                .product(product1)
                .productName("Áo Thun Nam Cotton")
                .unitPrice(BigDecimal.valueOf(250000))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(500000))
                .build();

        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(500000))
                .items(List.of(item))
                .build();

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        OrderResponse response = orderService.getOrderDetail(1L, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Áo Thun Nam Cotton");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(500000));
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công khi trạng thái là PENDING và gọi restoreStockForOrder()")
    void cancelOrder_Success_WhenPending() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L, "customer@example.com");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStockForOrder(existingOrder);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công khi trạng thái là CONFIRMED và gọi restoreStockForOrder()")
    void cancelOrder_Success_WhenConfirmed() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L, "customer@example.com");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStockForOrder(existingOrder);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Tự động chuyển trạng thái thanh toán sang REFUNDED khi đơn hàng đã thanh toán (PAID)")
    void cancelOrder_Success_AutoRefundsPayment_WhenPaid() {
        Payment payment = Payment.builder()
                .id(50L)
                .status(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.SIMULATED)
                .amount(BigDecimal.valueOf(500000))
                .build();

        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .payment(payment)
                .items(new ArrayList<>())
                .build();
        payment.setOrder(existingOrder);

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L, "customer@example.com");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(existingOrder.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(existingOrder.getPayment().getTransactionNote()).contains("Đã hoàn tiền do hủy đơn hàng");
        verify(inventoryService).restoreStockForOrder(existingOrder);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại với ConflictException (409) khi trạng thái là PROCESSING (không cho phép hủy)")
    void cancelOrder_ThrowsException_WhenProcessing() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PROCESSING)
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái 'Đang xử lý'");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại với ConflictException (409) khi trạng thái đã SHIPPING")
    void cancelOrder_ThrowsException_WhenShipping() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.SHIPPING)
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái 'Đang giao hàng'");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại với ConflictException (409) khi trạng thái đã COMPLETED")
    void cancelOrder_ThrowsException_WhenCompleted() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái 'Đã hoàn thành'");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }

    @Test
    @DisplayName("Hủy đơn hàng CANCELLED lần nữa: ném ConflictException (409), không hoàn kho lần 2")
    void cancelOrder_ThrowsConflict_WhenAlreadyCancelled_DoesNotRestoreStockAgain() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái 'Đã hủy'");

        verify(inventoryService, never()).restoreStockForOrder(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hoàn kho thất bại khi hủy: ném ngoại lệ và rollback transaction")
    void cancelOrder_Rollback_WhenInventoryRestoreFails() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new BusinessException("Lỗi dịch vụ tồn kho")).when(inventoryService).restoreStockForOrder(existingOrder);

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lỗi dịch vụ tồn kho");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR-01: CUSTOMER truy cập đơn hàng của chính mình thành công")
    void getOrderById_Success_WhenOwnerAccesses() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(250000))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        OrderResponse response = orderService.getOrderById(1L, "customer@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrderCode()).isEqualTo("ORD-20261102-111111");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("IDOR-02: CUSTOMER A truy cập đơn hàng của CUSTOMER B bị ném AccessDeniedException")
    void getOrderById_ThrowsAccessDenied_WhenOtherCustomerAccesses() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> orderService.getOrderById(1L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập thông tin đơn hàng này");
    }

    @Test
    @DisplayName("IDOR-03: ADMIN truy cập đơn hàng của CUSTOMER B thành công (bỏ qua kiểm tra ownership)")
    void getOrderById_Success_WhenAdminAccesses() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(250000))
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        OrderResponse response = orderService.getOrderById(1L, "admin@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("IDOR-04: CUSTOMER A hủy đơn hàng của CUSTOMER B bị ném AccessDeniedException và không thay đổi dữ liệu")
    void cancelOrder_ThrowsAccessDenied_WhenOtherCustomerCancels() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền hủy đơn hàng này");

        assertThat(existingOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(inventoryService, never()).restoreStockForOrder(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("IDOR-05: ADMIN hủy đơn hàng của CUSTOMER thành công (bỏ qua kiểm tra ownership)")
    void cancelOrder_Success_WhenAdminCancels() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(1L, "admin@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStockForOrder(existingOrder);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("IDOR-06: CUSTOMER truy cập đơn hàng không tồn tại nhận ResourceNotFoundException")
    void getOrderById_ThrowsNotFound_WhenOrderDoesNotExist() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L, "customer@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy đơn hàng với ID: 999");

        verify(userRepository, never()).findByEmail(anyString());
    }
}
