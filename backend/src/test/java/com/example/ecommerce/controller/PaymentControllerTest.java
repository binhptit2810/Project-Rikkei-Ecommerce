package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateVNPayPaymentRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.dto.response.VNPayIpnResponse;
import com.example.ecommerce.dto.response.VNPayPaymentResponse;
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

/**
 * Controller Test cho PaymentController (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
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
    @DisplayName("POST /api/v1/payments/{orderId}/pay: Xử lý thanh toán thành công trả 200 OK cùng mã PAY-")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void processPayment_Success() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .transactionNote("Mô phỏng thanh toán trực tuyến")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionCode("PAY-20261103-12345678")
                .orderId(10L)
                .orderCode("ORD-20261103-ABCDEF")
                .paymentMethod(PaymentMethod.SIMULATED)
                .paymentMethodDescription("Cổng thanh toán mô phỏng")
                .status(PaymentStatus.PAID)
                .statusDescription("Đã thanh toán")
                .amount(BigDecimal.valueOf(1500000))
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(eq(10L), any(ProcessPaymentRequest.class), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/10/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionCode").value("PAY-20261103-12345678"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/order/{orderId}: Mô phỏng thanh toán thành công (PAID) qua đường dẫn order/{id}")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void processPayment_ViaOrderEndpoint_Success() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(true)
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionCode("PAY-20261105-SUCCESS01")
                .orderId(15L)
                .status(PaymentStatus.PAID)
                .build();

        when(paymentService.processPayment(eq(15L), any(ProcessPaymentRequest.class), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/order/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/order/{orderId}: Mô phỏng thanh toán thất bại (FAILED)")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void processPayment_ViaOrderEndpoint_Failed() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.SIMULATED)
                .simulateSuccess(false)
                .transactionNote("Tài khoản thẻ không đủ số dư")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(2L)
                .transactionCode("PAY-20261105-FAILED01")
                .orderId(16L)
                .status(PaymentStatus.FAILED)
                .build();

        when(paymentService.processPayment(eq(16L), any(ProcessPaymentRequest.class), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/order/16")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/{orderId}/pay: Chưa đăng nhập trả 401 Unauthorized")
    void processPayment_Unauthorized_Returns401() throws Exception {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .paymentMethod(PaymentMethod.COD)
                .build();

        mockMvc.perform(post("/api/v1/payments/10/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId}: Tra cứu thanh toán theo orderId thành công trả 200 OK")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getPaymentByOrderId_Success() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionCode("PAY-20261103-87654321")
                .orderId(20L)
                .status(PaymentStatus.UNPAID)
                .build();

        when(paymentService.getPaymentByOrderId(eq(20L), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionCode").value("PAY-20261103-87654321"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/transaction/{code}: Tra cứu thông tin theo mã giao dịch PAY- thành công")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getPaymentByTransactionCode_Success() throws Exception {
        String code = "PAY-20261103-99998888";
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionCode(code)
                .orderId(30L)
                .status(PaymentStatus.PAID)
                .build();

        when(paymentService.getPaymentByTransactionCode(eq(code), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/transaction/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionCode").value(code));
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-vnpay: Sinh URL thanh toán VNPay Sandbox thành công")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void createVNPayPayment_Success() throws Exception {
        CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                .orderId(15L)
                .bankCode("NCB")
                .orderInfo("Thanh toan don hang #15")
                .build();

        VNPayPaymentResponse response = VNPayPaymentResponse.builder()
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=150000000&vnp_TxnRef=PAY-20261109-12345678")
                .vnpTxnRef("PAY-20261109-12345678")
                .orderId(15L)
                .amount(BigDecimal.valueOf(1500000))
                .message("Tạo URL chuyển hướng thanh toán VNPay thành công")
                .build();

        when(paymentService.createVNPayPayment(any(CreateVNPayPaymentRequest.class), any(), eq("customer@example.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/create-vnpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentUrl").value(response.getPaymentUrl()))
                .andExpect(jsonPath("$.data.vnpTxnRef").value("PAY-20261109-12345678"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-vnpay: Chưa đăng nhập trả 401 Unauthorized")
    void createVNPayPayment_Unauthorized_Returns401() throws Exception {
        CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                .orderId(15L)
                .build();

        mockMvc.perform(post("/api/v1/payments/create-vnpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-callback: Callback công khai không cần token xử lý thành công")
    void handleVNPayCallback_Success() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionCode("PAY-20261109-12345678")
                .orderId(15L)
                .status(PaymentStatus.PAID)
                .build();

        when(paymentService.processVNPayCallback(any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/vnpay-callback")
                        .param("vnp_TxnRef", "PAY-20261109-12345678")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", "mockedhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionCode").value("PAY-20261109-12345678"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-ipn: Webhook công khai không cần JWT, trả JSON phẳng chuẩn đặc tả VNPay")
    void handleVNPayIpn_Success_ReturnsFlatJson() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.success());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-20261110-12345678")
                        .param("vnp_Amount", "150000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_SecureHash", "validmockhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    @DisplayName("GET /payments/vnpay-ipn: Route alias không có /api/v1 cũng hoạt động chuẩn xác")
    void handleVNPayIpn_AliasRoute_Success() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.success());

        mockMvc.perform(get("/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-20261110-12345678")
                        .param("vnp_SecureHash", "validmockhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-ipn: Sai chữ ký trả về HTTP 200 kèm RspCode 97")
    void handleVNPayIpn_InvalidChecksum_Returns97() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.invalidChecksum());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-TEST")
                        .param("vnp_SecureHash", "badhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-ipn: Không tìm thấy giao dịch trả về HTTP 200 kèm RspCode 01")
    void handleVNPayIpn_OrderNotFound_Returns01() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.orderNotFound());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-UNKNOWN")
                        .param("vnp_SecureHash", "validhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("01"))
                .andExpect(jsonPath("$.Message").value("Order not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-ipn: Sai số tiền trả về HTTP 200 kèm RspCode 04")
    void handleVNPayIpn_InvalidAmount_Returns04() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.invalidAmount());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-TEST")
                        .param("vnp_Amount", "999999")
                        .param("vnp_SecureHash", "validhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("04"))
                .andExpect(jsonPath("$.Message").value("Invalid Amount"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay-ipn: Giao dịch đã xác nhận trước đó trả về HTTP 200 kèm RspCode 02")
    void handleVNPayIpn_AlreadyConfirmed_Returns02() throws Exception {
        when(paymentService.processVNPayIpn(any(), any()))
                .thenReturn(VNPayIpnResponse.alreadyConfirmed());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "PAY-TEST")
                        .param("vnp_SecureHash", "validhash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("02"))
                .andExpect(jsonPath("$.Message").value("Order already confirmed"));
    }
}

