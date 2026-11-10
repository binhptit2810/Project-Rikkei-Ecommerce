package com.example.ecommerce.chat.tool;

import com.example.ecommerce.chat.tool.dto.MyOrdersRequest;
import com.example.ecommerce.chat.tool.dto.OrderDetailRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho OrderTools (Spring AI Tool Calling).
 * Phụ trách: Cao Chí Thiện & Đỗ Hoàng Anh [TSK-D12-DHA]
 * Task ID: [TSK-D11-CCT] [TSK-D12-DHA]
 */
@ExtendWith(MockitoExtension.class)
class OrderToolsTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderTools orderTools;

    private User testUser;
    private ToolContext authenticatedToolContext;
    private ToolContext guestToolContext;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(100L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .build();

        authenticatedToolContext = new ToolContext(Map.of(
                "authenticatedUserId", "100",
                "isGuest", false,
                "sessionId", "session-auth-123"
        ));

        guestToolContext = new ToolContext(Map.of(
                "isGuest", true,
                "sessionId", "session-guest-456"
        ));
    }

    @Test
    @DisplayName("OT-01: getMyOrders khi khách vãng lai (Guest) gọi sẽ bị từ chối và yêu cầu đăng nhập")
    void testGetMyOrders_GuestBlocked() {
        String result = orderTools.getMyOrders(new MyOrdersRequest(), guestToolContext);

        assertThat(result).contains("khách vãng lai");
        assertThat(result).contains("Vui lòng đăng nhập");
    }

    @Test
    @DisplayName("OT-02: getMyOrders khi ToolContext rỗng hoặc null sẽ từ chối truy cập")
    void testGetMyOrders_NullContextBlocked() {
        String result = orderTools.getMyOrders(new MyOrdersRequest(), null);
        assertThat(result).contains("Vui lòng đăng nhập");
    }

    @Test
    @DisplayName("OT-03: getMyOrders thành công trả về danh sách đơn hàng của người dùng đã xác thực")
    void testGetMyOrders_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        OrderResponse order1 = OrderResponse.builder()
                .id(10L)
                .orderCode("ORD-20241108-001")
                .orderStatus(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("500000"))
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0901234567")
                .createdAt(LocalDateTime.of(2024, 11, 8, 10, 0))
                .build();

        PageResponse<OrderResponse> pageResponse = PageResponse.from(
                new PageImpl<OrderResponse>(List.of(order1), PageRequest.of(0, 5), 1)
        );

        when(orderService.getMyOrders(eq("customer@example.com"), eq(0), eq(5), anyString(), anyString()))
                .thenReturn(pageResponse);

        String result = orderTools.getMyOrders(new MyOrdersRequest(), authenticatedToolContext);

        assertThat(result).contains("ORD-20241108-001");
        assertThat(result).contains("customer@example.com");
        assertThat(result).contains("Giao hàng thành công");
        assertThat(result).contains("500,000 đ");
    }

    @Test
    @DisplayName("OT-04: getMyOrders khi người dùng chưa có đơn hàng nào thông báo rõ ràng")
    void testGetMyOrders_EmptyOrders() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        PageResponse<OrderResponse> emptyPage = PageResponse.from(
                new PageImpl<OrderResponse>(Collections.emptyList(), PageRequest.of(0, 5), 0)
        );

        when(orderService.getMyOrders(eq("customer@example.com"), eq(0), eq(5), anyString(), anyString()))
                .thenReturn(emptyPage);

        String result = orderTools.getMyOrders(new MyOrdersRequest(), authenticatedToolContext);

        assertThat(result).contains("chưa có đơn hàng nào");
    }

    @Test
    @DisplayName("OT-05: getOrderDetail khi khách vãng lai gọi sẽ bị từ chối")
    void testGetOrderDetail_GuestBlocked() {
        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-20241108-001")
                .build();

        String result = orderTools.getOrderDetail(request, guestToolContext);

        assertThat(result).contains("Vui lòng đăng nhập");
    }

    @Test
    @DisplayName("OT-06: getOrderDetail thành công trả về thông tin chi tiết và danh sách sản phẩm")
    void testGetOrderDetail_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        OrderItemResponse item = OrderItemResponse.builder()
                .id(1L)
                .productId(5L)
                .productName("Áo Khoác Gió")
                .unitPrice(new BigDecimal("350000"))
                .quantity(2)
                .subtotal(new BigDecimal("700000"))
                .build();

        OrderResponse order = OrderResponse.builder()
                .id(10L)
                .orderCode("ORD-20241108-001")
                .orderStatus(OrderStatus.PROCESSING)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("700000"))
                .createdAt(LocalDateTime.of(2024, 11, 8, 10, 0))
                .items(List.of(item))
                .build();

        when(orderService.getOrderByOrderCode("ORD-20241108-001", "customer@example.com"))
                .thenReturn(order);

        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-20241108-001")
                .build();

        String result = orderTools.getOrderDetail(request, authenticatedToolContext);

        assertThat(result).contains("ORD-20241108-001");
        assertThat(result).contains("Đang đóng gói xử lý");
        assertThat(result).contains("Áo Khoác Gió");
        assertThat(result).contains("Số lượng: 2");
        assertThat(result).contains("700,000 đ");
    }

    @Test
    @DisplayName("OT-07: getOrderDetail khi người dùng cố truy cập đơn hàng của người khác (AccessDeniedException)")
    void testGetOrderDetail_AccessDenied() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        when(orderService.getOrderByOrderCode("ORD-OTHER-999", "customer@example.com"))
                .thenThrow(new AccessDeniedException("Không có quyền"));

        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-OTHER-999")
                .build();

        String result = orderTools.getOrderDetail(request, authenticatedToolContext);

        assertThat(result).contains("Bạn không có quyền truy cập");
    }

    @Test
    @DisplayName("OT-08: getOrderDetail khi không tìm thấy đơn hàng (ResourceNotFoundException)")
    void testGetOrderDetail_NotFound() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        when(orderService.getOrderByOrderCode("ORD-NOTFOUND", "customer@example.com"))
                .thenThrow(new ResourceNotFoundException("Đơn hàng không tìm thấy với mã: ORD-NOTFOUND"));

        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-NOTFOUND")
                .build();

        String result = orderTools.getOrderDetail(request, authenticatedToolContext);

        assertThat(result).contains("Không tìm thấy đơn hàng");
    }

    @Test
    @DisplayName("OT-09: getOrderDetail khi không truyền orderCode lẫn orderId trả về yêu cầu tham số")
    void testGetOrderDetail_MissingParams() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        String result = orderTools.getOrderDetail(new OrderDetailRequest(), authenticatedToolContext);

        assertThat(result).contains("Vui lòng cung cấp mã đơn hàng");
    }

    @Test
    @DisplayName("OT-10: getOrderDetail khi orderCode dạng ORD-001 hoặc ORD-123 fallback sang tìm theo numeric ID thành công")
    void testGetOrderDetail_NumericFallback_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        when(orderService.getOrderByOrderCode("ORD-001", "customer@example.com"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        OrderResponse order = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20241108-001")
                .orderStatus(OrderStatus.PROCESSING)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalAmount(new BigDecimal("700000"))
                .createdAt(LocalDateTime.of(2024, 11, 8, 10, 0))
                .build();

        when(orderService.getOrderDetail(1L, "customer@example.com"))
                .thenReturn(order);

        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-001")
                .build();

        String result = orderTools.getOrderDetail(request, authenticatedToolContext);

        assertThat(result).contains("ORD-20241108-001");
        assertThat(result).contains("Đang đóng gói xử lý");
    }

    @Test
    @DisplayName("OT-11: getOrderDetail khi orderCode dạng ORD-001 fallback theo numeric ID nhưng ID cũng không tồn tại")
    void testGetOrderDetail_NumericFallback_NotFound() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        when(orderService.getOrderByOrderCode("ORD-999", "customer@example.com"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        when(orderService.getOrderDetail(999L, "customer@example.com"))
                .thenThrow(new ResourceNotFoundException("Not found by ID"));

        OrderDetailRequest request = OrderDetailRequest.builder()
                .orderCode("ORD-999")
                .build();

        String result = orderTools.getOrderDetail(request, authenticatedToolContext);

        assertThat(result).contains("Không tìm thấy đơn hàng");
    }
}
