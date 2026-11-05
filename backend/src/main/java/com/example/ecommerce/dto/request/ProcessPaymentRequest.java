package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload yêu cầu xử lý thanh toán đơn hàng (COD hoặc SIMULATED).
 * Hỗ trợ mô phỏng thanh toán thành công (PAID) hoặc thất bại (FAILED).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.SIMULATED;

    @Builder.Default
    private Boolean simulateSuccess = true;

    private PaymentStatus status;

    private String transactionNote;

    /**
     * Xác định kết quả mô phỏng là thành công hay thất bại.
     * Ưu tiên trường status (PAID/FAILED), sau đó đến simulateSuccess (true/false).
     */
    public boolean isSuccessSimulation() {
        if (status != null) {
            return status == PaymentStatus.PAID;
        }
        return simulateSuccess == null || simulateSuccess;
    }
}
