package com.example.ecommerce.chat.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tham số đầu vào cho tool tra cứu đơn hàng cá nhân.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyOrdersRequest {

    @JsonPropertyDescription("Số lượng đơn hàng tối đa cần lấy (mặc định: 5, tối đa: 5)")
    @JsonProperty(value = "limit", required = false)
    private Integer limit;
}

