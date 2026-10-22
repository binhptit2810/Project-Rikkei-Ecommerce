package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_payment_controller;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("POST /api/v1/payments/{orderId}/pay: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void processPayment_Unauthenticated_Returns401() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .build();

        mockMvc.perform(post("/api/v1/payments/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{orderId}/pay: Đã đăng nhập thanh toán thành công trả về 200 OK")
    @WithMockUser(username = "customer1@example.com", roles = "CUSTOMER")
    void processPayment_Authenticated_Returns200() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(10L)
                .orderCode("ORD-20261022-0001")
                .paymentMethod(PaymentMethod.SIMULATED)
                .status(PaymentStatus.PAID)
                .amount(BigDecimal.valueOf(250000))
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(eq(10L), eq("customer1@example.com"), any(ProcessPaymentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/10/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId}: Tra cứu thông tin thanh toán thành công")
    @WithMockUser(username = "customer1@example.com", roles = "CUSTOMER")
    void getPaymentByOrderId_Authenticated_Returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(10L)
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.UNPAID)
                .amount(BigDecimal.valueOf(300000))
                .build();

        when(paymentService.getPaymentByOrderId(10L, "customer1@example.com"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"));
    }
}
