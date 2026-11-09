package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO phản hồi kết quả tạo URL thanh toán chuyển hướng sang VNPay Sandbox.
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Cao Chí Thiện (CCT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentResponse {

    private String paymentUrl;
    private String vnpTxnRef;
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String message;

    // Thông tin thanh toán mã QR tượng trưng (VietQR / VNPAY-QR)
    private String qrCodeUrl;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
}

