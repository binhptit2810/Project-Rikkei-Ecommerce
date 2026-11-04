package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.service.OrderService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderResponse sampleOrderResponse() {
        return OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-20261104-ABC123")
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("123 Duong ABC, Quan 1, TP.HCM")
                .totalAmount(new BigDecimal("500000"))
                .orderStatus(OrderStatus.PENDING)
                .orderStatusDescription("Chờ xác nhận")
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentStatusDescription("Chưa thanh toán")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders: Thành công khi là ADMIN")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getOrders_Admin_Returns200() throws Exception {
        OrderResponse order = sampleOrderResponse();
        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(order))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(orderService.getAdminOrders(anyInt(), anyInt(), any(), any(), anyString(), anyString()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/orders?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderCode").value("ORD-20261104-ABC123"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders: Từ chối 403 khi là CUSTOMER")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getOrders_Customer_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders: Từ chối 401 khi chưa đăng nhập")
    void getOrders_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/orders/{id}: Thành công khi là ADMIN")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getOrderDetail_Admin_Returns200() throws Exception {
        OrderResponse order = sampleOrderResponse();
        when(orderService.getOrderById(1L)).thenReturn(order);

        mockMvc.perform(get("/api/v1/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-20261104-ABC123"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/orders/{id}/status: Cập nhật thành công khi là ADMIN")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void updateOrderStatus_Admin_Returns200() throws Exception {
        OrderResponse updated = sampleOrderResponse();
        updated.setOrderStatus(OrderStatus.CONFIRMED);
        updated.setOrderStatusDescription("Đã xác nhận");

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/orders/{id}/status: Lỗi 400 khi luồng chuyển đổi trạng thái không hợp lệ")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void updateOrderStatus_InvalidTransition_Returns400() throws Exception {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.COMPLETED)
                .build();

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.COMPLETED)))
                .thenThrow(new BusinessException("Không thể chuyển đổi trạng thái không hợp lệ"));

        mockMvc.perform(patch("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /admin/orders/{id}/status: Cập nhật thành công qua endpoint trực tiếp /admin/orders/{id}/status")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void updateOrderStatus_DirectPath_Admin_Returns200() throws Exception {
        OrderResponse updated = sampleOrderResponse();
        updated.setOrderStatus(OrderStatus.CONFIRMED);
        updated.setOrderStatusDescription("Đã xác nhận");

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(updated);

        mockMvc.perform(patch("/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /admin/orders/{id}/status: Từ chối 403 khi người dùng có role CUSTOMER")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void updateOrderStatus_Customer_Returns403() throws Exception {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /admin/orders/{id}/status: Từ chối 401 khi chưa xác thực")
    void updateOrderStatus_Unauthenticated_Returns401() throws Exception {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        mockMvc.perform(patch("/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
