package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO trả về thông tin quét mã VietQR qua Cổng SePay.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SePayPaymentResponse {

    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String qrCodeUrl;
    private String transferContent;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String message;
}
