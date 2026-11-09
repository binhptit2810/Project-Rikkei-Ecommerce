package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceAdminAndLookupTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private User otherUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User A")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("User B")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(999L)
                .email("admin@example.com")
                .fullName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getMyOrders không phân trang trả về danh sách OrderResponse")
    void getMyOrders_List_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        Order order = Order.builder()
                .id(10L)
                .orderCode("ORD-10")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(150000))
                .orderStatus(OrderStatus.CONFIRMED)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getMyOrders("user@example.com");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderCode()).isEqualTo("ORD-10");
    }

    @Test
    @DisplayName("getOrderByOrderCode thành công khi user là chủ sở hữu")
    void getOrderByOrderCode_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        Order order = Order.builder()
                .id(11L)
                .orderCode("ORD-11")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(250000))
                .orderStatus(OrderStatus.PENDING)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findByOrderCode("ORD-11")).thenReturn(Optional.of(order));

        OrderResponse res = orderService.getOrderByOrderCode("ORD-11", "user@example.com");
        assertThat(res.getOrderCode()).isEqualTo("ORD-11");
    }

    @Test
    @DisplayName("getOrderByOrderCode ném AccessDeniedException khi user khác cố truy cập")
    void getOrderByOrderCode_AccessDenied() {
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        Order order = Order.builder()
                .id(12L)
                .orderCode("ORD-12")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(250000))
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findByOrderCode("ORD-12")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderByOrderCode("ORD-12", "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập thông tin đơn hàng này");
    }

    @Test
    @DisplayName("getOrderByOrderCode thành công khi user là Admin dù không sở hữu đơn")
    void getOrderByOrderCode_AdminAccess_Success() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        Order order = Order.builder()
                .id(13L)
                .orderCode("ORD-13")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(250000))
                .orderStatus(OrderStatus.PENDING)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findByOrderCode("ORD-13")).thenReturn(Optional.of(order));

        OrderResponse res = orderService.getOrderByOrderCode("ORD-13", "admin@example.com");
        assertThat(res.getOrderCode()).isEqualTo("ORD-13");
    }

    @Test
    @DisplayName("getOrderByOrderCode ném ResourceNotFoundException khi không tìm thấy đơn")
    void getOrderByOrderCode_NotFound() {
        when(orderRepository.findByOrderCode("ORD-NOTFOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByOrderCode("ORD-NOTFOUND", "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy đơn hàng với mã: ORD-NOTFOUND");
    }

    @Test
    @DisplayName("getAdminOrders phân trang đơn hàng cho admin thành công")
    void getAdminOrders_Success() {
        Order order = Order.builder()
                .id(14L)
                .orderCode("ORD-14")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(350000))
                .orderStatus(OrderStatus.PROCESSING)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.searchOrdersAdmin(eq(OrderStatus.PROCESSING), eq("ORD-14"), any(Pageable.class))).thenReturn(page);

        PageResponse<OrderResponse> res = orderService.getAdminOrders(0, 10, OrderStatus.PROCESSING, "ORD-14", "createdAt", "desc");
        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getAllOrders lấy toàn bộ danh sách đơn hàng cho admin thành công")
    void getAllOrders_Success() {
        Order order = Order.builder()
                .id(15L)
                .orderCode("ORD-15")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(450000))
                .orderStatus(OrderStatus.COMPLETED)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<OrderResponse> list = orderService.getAllOrders();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getOrderCode()).isEqualTo("ORD-15");
    }

    @Test
    @DisplayName("getOrderById theo id cho admin thành công")
    void getOrderById_Admin_Success() {
        Order order = Order.builder()
                .id(16L)
                .orderCode("ORD-16")
                .user(testUser)
                .totalAmount(BigDecimal.valueOf(550000))
                .orderStatus(OrderStatus.CONFIRMED)
                .recipientName("User A")
                .recipientPhone("0987654321")
                .shippingAddress("Hanoi")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.findByIdWithItems(16L)).thenReturn(Optional.of(order));

        OrderResponse res = orderService.getOrderById(16L);
        assertThat(res.getId()).isEqualTo(16L);
    }
}
