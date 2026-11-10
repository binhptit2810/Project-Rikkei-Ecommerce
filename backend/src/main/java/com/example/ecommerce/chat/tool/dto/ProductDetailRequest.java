package com.example.ecommerce.chat.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tham số đầu vào cho tool xem chi tiết sản phẩm.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailRequest {

    @JsonPropertyDescription("Mã ID định danh của sản phẩm cần xem chi tiết (bắt buộc, ví dụ: 1, 2, 10)")
    @JsonProperty(value = "productId", required = true)
    private Long productId;
}

