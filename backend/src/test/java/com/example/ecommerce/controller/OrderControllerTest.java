package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_order;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("POST /api/v1/orders: Tạo đơn hàng thành công trả 201 Created")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void createOrder_Authenticated_Returns201() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao buổi sáng")
                .items(List.of(new OrderItemRequest(1L, 2)))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261102-123456")
                .recipientName("Nguyễn Văn A")
                .totalAmount(BigDecimal.valueOf(500000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), eq("customer@example.com"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt hàng thành công"))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-20261102-123456"))
                .andExpect(jsonPath("$.data.totalAmount").value(500000));
    }

    @Test
    @DisplayName("POST /api/v1/orders: Từ chối truy cập 401 khi chưa đăng nhập")
    void createOrder_Unauthenticated_Returns401() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Phố Huế, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(1L, 2)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/orders/my: Lấy danh sách đơn hàng của người dùng đang đăng nhập")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getMyOrders_Returns200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261102-123456")
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        when(orderService.getMyOrders("customer@example.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderCode").value("ORD-20261102-123456"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/cancel: Hủy đơn hàng thành công")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void cancelOrder_Returns200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261102-123456")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(eq(1L), eq("customer@example.com"))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id}: Xem chi tiết đơn hàng thành công trả 200 OK")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getOrderById_Success_Returns200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261102-123456")
                .totalAmount(BigDecimal.valueOf(500000))
                .orderStatus(OrderStatus.PENDING)
                .build();

        when(orderService.getOrderById(eq(1L), eq("customer@example.com"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-20261102-123456"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id}: Từ chối 403 FORBIDDEN khi truy cập đơn của người khác (IDOR Protection)")
    @WithMockUser(username = "other@example.com", roles = {"CUSTOMER"})
    void getOrderById_AccessDenied_Returns403() throws Exception {
        when(orderService.getOrderById(eq(1L), eq("other@example.com")))
                .thenThrow(new AccessDeniedException("Bạn không có quyền truy cập thông tin đơn hàng này"));

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập thông tin đơn hàng này"));
    }

    @Test
    @DisplayName("PATCH /api/v1/orders/{id}/cancel: Từ chối 403 FORBIDDEN khi hủy đơn của người khác (IDOR Protection)")
    @WithMockUser(username = "other@example.com", roles = {"CUSTOMER"})
    void cancelOrder_AccessDenied_Returns403() throws Exception {
        when(orderService.cancelOrder(eq(1L), eq("other@example.com")))
                .thenThrow(new AccessDeniedException("Bạn không có quyền hủy đơn hàng này"));

        mockMvc.perform(patch("/api/v1/orders/1/cancel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền hủy đơn hàng này"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id}: Từ chối truy cập 401 khi chưa đăng nhập")
    void getOrderById_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isUnauthorized());
    }
}
