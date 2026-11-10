package com.example.ecommerce.chat.tool.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Tham số đầu vào cho tool tìm kiếm sản phẩm.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    @JsonPropertyDescription("Từ khóa tìm kiếm theo tên hoặc mô tả sản phẩm (ví dụ: 'áo thun', 'tai nghe', 'giày')")
    @JsonProperty(value = "keyword", required = false)
    private String keyword;

    @JsonPropertyDescription("Mã ID danh mục sản phẩm cần lọc nếu có (ví dụ: 1, 2)")
    @JsonProperty(value = "categoryId", required = false)
    private Long categoryId;

    @JsonPropertyDescription("Mức giá tối thiểu (VND)")
    @JsonProperty(value = "minPrice", required = false)
    private BigDecimal minPrice;

    @JsonPropertyDescription("Mức giá tối đa (VND)")
    @JsonProperty(value = "maxPrice", required = false)
    private BigDecimal maxPrice;
}

