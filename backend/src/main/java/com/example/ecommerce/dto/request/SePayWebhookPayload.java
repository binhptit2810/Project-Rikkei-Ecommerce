package com.example.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO tiếp nhận dữ liệu Webhook từ Cổng SePay khi có biến động số dư tiền vào tài khoản ngân hàng.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayWebhookPayload {

    private Long id;

    private String gateway;

    private String transactionDate;

    private String accountNumber;

    private String subAccount;

    private String code;

    /**
     * Nội dung chuyển khoản khách hàng nhập (chứa mã đơn hàng ORD-...)
     */
    private String content;

    /**
     * Loại giao dịch: "in" (tiền vào) hoặc "out" (tiền ra)
     */
    private String transferType;

    private String description;

    /**
     * Số tiền chuyển vào tài khoản
     */
    private BigDecimal transferAmount;

    /**
     * Mã tham chiếu / mã giao dịch ngân hàng
     */
    private String referenceCode;

    private BigDecimal accumulated;
}
