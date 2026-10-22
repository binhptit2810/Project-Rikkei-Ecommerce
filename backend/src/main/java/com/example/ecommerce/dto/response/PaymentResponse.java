package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả về thông tin chi tiết thanh toán của đơn hàng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderCode;
    private PaymentMethod paymentMethod;
    private String paymentMethodDescription;
    private PaymentStatus status;
    private String statusDescription;
    private BigDecimal amount;
    private String transactionNote;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
