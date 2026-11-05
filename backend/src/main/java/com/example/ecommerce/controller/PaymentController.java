package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý thanh toán và tra cứu giao dịch PAY- (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@RestController
@RequestMapping({"/api/v1/payments", "/payments"})
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Xử lý thanh toán đơn hàng (COD hoặc SIMULATED) và sinh/cập nhật mã giao dịch duy nhất PAY-.
     * Hỗ trợ mô phỏng thanh toán thành công (PAID) hoặc thất bại (FAILED).
     * POST /api/v1/payments/order/{orderId}
     * POST /api/v1/payments/{orderId}/pay
     */
    @PostMapping({"/order/{orderId}", "/{orderId}/pay"})
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) ProcessPaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        if (request == null) {
            request = ProcessPaymentRequest.builder()
                    .paymentMethod(com.example.ecommerce.entity.PaymentMethod.SIMULATED)
                    .simulateSuccess(true)
                    .build();
        }
        PaymentResponse response = paymentService.processPayment(orderId, request, email);
        return ResponseEntity.ok(ApiResponse.success("Xử lý thanh toán thành công", response));
    }

    /**
     * Tra cứu thông tin thanh toán theo ID đơn hàng.
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Tra cứu thông tin thanh toán theo mã giao dịch duy nhất có tiền tố PAY- (định dạng PAY-YYYYMMDD-XXXXXXXX).
     * GET /api/v1/payments/transaction/{transactionCode}
     */
    @GetMapping("/transaction/{transactionCode}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByTransactionCode(
            @PathVariable String transactionCode,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        PaymentResponse response = paymentService.getPaymentByTransactionCode(transactionCode, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

