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
 * Controller xử lý thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Thực hiện thanh toán (COD hoặc mô phỏng thanh toán trực tuyến SIMULATED).
     * POST /api/v1/payments/{orderId}/pay
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        PaymentResponse response = paymentService.processPayment(orderId, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý thanh toán thành công", response));
    }

    /**
     * Tra cứu thông tin thanh toán của một đơn hàng.
     * GET /api/v1/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
