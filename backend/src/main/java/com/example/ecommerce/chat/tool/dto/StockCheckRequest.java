package com.example.ecommerce.chat.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tham số đầu vào cho tool kiểm tra tồn kho sản phẩm.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckRequest {

    @JsonPropertyDescription("Mã ID định danh của sản phẩm cần kiểm tra tồn kho (bắt buộc, ví dụ: 1, 2, 10)")
    @JsonProperty(value = "productId", required = true)
    private Long productId;
}

