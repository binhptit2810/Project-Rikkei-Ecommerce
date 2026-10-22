package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
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
    private PaymentRepository paymentRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer1@example.com")
                .fullName("Khách hàng 1")
                .phone("0912345678")
                .address("123 Đường ABC")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Áo thun nam basic Cotton")
                .price(BigDecimal.valueOf(199000))
                .stockQuantity(50)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công (COD) - Trạng thái PENDING, trừ tồn kho")
    void createOrder_Success_COD() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Khách hàng 1")
                .recipientPhone("0912345678")
                .shippingAddress("123 Đường ABC")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao giờ hành chính")
                .items(List.of(new OrderItemRequest(1L, 2)))
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        OrderResponse response = orderService.createOrder("customer1@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(398000));
        assertThat(response.getItems()).hasSize(1);

        verify(inventoryService, times(1)).deductStockForOrder(any(Order.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công (SIMULATED) - Trạng thái thanh toán PAID")
    void createOrder_Success_Simulated() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Khách hàng 1")
                .recipientPhone("0912345678")
                .shippingAddress("123 Đường ABC")
                .paymentMethod(PaymentMethod.SIMULATED)
                .items(List.of(new OrderItemRequest(1L, 1)))
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(11L);
            return o;
        });

        OrderResponse response = orderService.createOrder("customer1@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.SIMULATED);
        verify(inventoryService).deductStockForOrder(any(Order.class));
    }

    @Test
    @DisplayName("Tạo đơn hàng thất bại khi sản phẩm ngừng kinh doanh")
    void createOrder_ThrowsException_WhenProductInactive() {
        testProduct.setActive(false);
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Khách hàng 1")
                .recipientPhone("0912345678")
                .shippingAddress("123 Đường ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(1L, 1)))
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> orderService.createOrder("customer1@example.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hiện đang ngừng kinh doanh");

        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).deductStockForOrder(any());
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng của người dùng hiện tại")
    void getMyOrders_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .user(testUser)
                .recipientName("Khách hàng 1")
                .recipientPhone("0912345678")
                .shippingAddress("123 Đường ABC")
                .totalAmount(BigDecimal.valueOf(199000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getMyOrders("customer1@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderCode()).isEqualTo("ORD-20261022-1001");
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công khi trạng thái là PENDING - Hoàn tồn kho")
    void cancelOrder_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("customer1@example.com", 1L, "Thay đổi ý định");

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService, times(1)).restoreStockForOrder(order);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn đang SHIPPING")
    void cancelOrder_ThrowsException_WhenShipping() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .user(testUser)
                .orderStatus(OrderStatus.SHIPPING)
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("customer1@example.com", 1L, "Hủy đơn"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái 'Đang giao hàng'");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi đơn đã bị hủy trước đó")
    void cancelOrder_ThrowsException_WhenAlreadyCancelled() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .user(testUser)
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(userRepository.findByEmail("customer1@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("customer1@example.com", 1L, "Hủy tiếp"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đơn hàng này đã được hủy trước đó");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái tuần tự PENDING sang CONFIRMED - Thành công")
    void updateOrderStatus_PendingToConfirmed_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Cập nhật trạng thái nhảy bước PENDING sang SHIPPING - Ném BusinessException")
    void updateOrderStatus_ThrowsException_WhenSkippingState() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chuyển trạng thái đơn hàng");
    }

    @Test
    @DisplayName("Cập nhật trạng thái ngược SHIPPING sang PROCESSING - Ném BusinessException")
    void updateOrderStatus_ThrowsException_WhenReverseState() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .orderStatus(OrderStatus.SHIPPING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PROCESSING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chuyển trạng thái đơn hàng");
    }

    @Test
    @DisplayName("Cập nhật trạng thái đơn COMPLETED - Ném BusinessException")
    void updateOrderStatus_ThrowsException_WhenAlreadyCompleted() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .orderStatus(OrderStatus.COMPLETED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đơn hàng đã hoàn thành");
    }

    @Test
    @DisplayName("Cập nhật trạng thái đơn COD từ SHIPPING sang COMPLETED - Tự động cập nhật Payment sang PAID")
    void updateOrderStatus_ShippingToCompleted_UpdatesPaymentToPaid() {
        Payment payment = Payment.builder()
                .id(5L)
                .status(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .build();

        Order order = Order.builder()
                .id(1L)
                .orderCode("ORD-20261022-1001")
                .orderStatus(OrderStatus.SHIPPING)
                .paymentStatus(PaymentStatus.UNPAID)
                .payment(payment)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.COMPLETED);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository).save(payment);
    }
}
