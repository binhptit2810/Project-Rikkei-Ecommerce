package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload yêu cầu xử lý thanh toán đơn hàng (COD hoặc SIMULATED).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Builder.Default
    private Boolean simulateSuccess = true;

    private String transactionNote;
}

