package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu thanh toán mô phỏng hoặc COD.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    /**
     * Dùng cho phương thức SIMULATED: true = mô phỏng thanh toán thành công, false = mô phỏng thất bại
     */
    @Builder.Default
    private Boolean simulateSuccess = true;

    private String transactionNote;
}
