package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
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
        // Arrange
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

        // Act
        OrderResponse response = orderService.createOrder(request, "customer@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getOrderCode()).startsWith("ORD-");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(response.getTransactionCode()).isEqualTo("PAY-20261102-12345678");
        assertThat(response.getItems()).hasSize(2);

        // Verify Order items snapshot
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Áo Thun Nam Cotton");
        assertThat(saved.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(saved.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(500000));

        // Verify InventoryService is invoked to deduct stock
        verify(inventoryService).deductStockForOrder(saved);
        // Verify PaymentService is invoked
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
    @DisplayName("Hủy đơn hàng thành công khi trạng thái là PENDING và hoàn tồn kho")
    void cancelOrder_Success() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L, "customer@example.com");

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStockForOrder(existingOrder);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("Hủy đơn hàng thất bại khi trạng thái đã SHIPPING")
    void cancelOrder_ThrowsException_WhenShipping() {
        Order existingOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261102-111111")
                .user(testUser)
                .orderStatus(OrderStatus.SHIPPING)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "customer@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể hủy đơn hàng ở trạng thái");

        verify(inventoryService, never()).restoreStockForOrder(any());
    }
}

