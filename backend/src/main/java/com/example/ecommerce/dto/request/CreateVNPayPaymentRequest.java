package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload yêu cầu khởi tạo giao dịch thanh toán qua Cổng VNPay Sandbox.
 * Endpoint: POST /api/v1/payments/create-vnpay
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Cao Chí Thiện (CCT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVNPayPaymentRequest {

    @NotNull(message = "ID đơn hàng không được để trống")
    private Long orderId;

    /**
     * @deprecated CẢNH BÁO BẢO MẬT (TSK-D13-CCT): Trường amount gửi từ client bị BỎ QUA hoàn toàn
     * ở tầng PaymentService để chống gian lận sửa giá (Money Tampering).
     * Số tiền thanh toán VNPay LUÔN được truy vấn trực tiếp từ order.getTotalAmount() trong database.
     * Giữ lại trường này để tương thích ngược API payload cũ nếu có.
     */
    @Deprecated
    private BigDecimal amount;

    private String bankCode;

    private String orderInfo;
}
