package com.example.ecommerce.chat.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tham số đầu vào cho tool xem chi tiết đơn hàng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailRequest {

    @JsonPropertyDescription("Mã đơn hàng chữ cần tra cứu (ví dụ: 'ORD-20241108-ABCD')")
    @JsonProperty(value = "orderCode", required = false)
    private String orderCode;

    @JsonPropertyDescription("Mã ID số của đơn hàng (ví dụ: 1, 15)")
    @JsonProperty(value = "orderId", required = false)
    private Long orderId;
}

