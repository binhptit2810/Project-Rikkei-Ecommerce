package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CancelOrderRequest;
import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("POST /api/v1/orders: Trả về 401 khi chưa đăng nhập")
    void createOrder_WithoutAuth_Returns401() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(1L, 1)))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer1@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/orders: Tạo đơn hàng thành công trả về 201 CREATED")
    void createOrder_Success_Returns201() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Duong ABC")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(1L, 2)))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261022-1234")
                .recipientName("Nguyen Van A")
                .totalAmount(BigDecimal.valueOf(398000))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.COD)
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        OrderItemResponse.builder()
                                .id(10L)
                                .productId(1L)
                                .productName("Áo thun")
                                .unitPrice(BigDecimal.valueOf(199000))
                                .quantity(2)
                                .subtotal(BigDecimal.valueOf(398000))
                                .build()
                ))
                .build();

        when(orderService.createOrder(eq("customer1@example.com"), any(CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-20261022-1234"))
                .andExpect(jsonPath("$.data.totalAmount").value(398000));
    }

    @Test
    @WithMockUser(username = "customer1@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/orders/my: Lấy danh sách đơn hàng của người dùng thành công")
    void getMyOrders_Success_Returns200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261022-1234")
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(199000))
                .build();

        when(orderService.getMyOrders("customer1@example.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderCode").value("ORD-20261022-1234"));
    }

    @Test
    @WithMockUser(username = "customer1@example.com", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/v1/orders/{id}/cancel: Hủy đơn hàng thành công")
    void cancelOrder_Success_Returns200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261022-1234")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(eq("customer1@example.com"), eq(1L), any()))
                .thenReturn(response);

        CancelOrderRequest cancelReq = new CancelOrderRequest("Khách đổi ý");

        mockMvc.perform(patch("/api/v1/orders/1/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"));
    }
}
