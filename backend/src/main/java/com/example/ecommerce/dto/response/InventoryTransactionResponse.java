package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin lịch sử biến động kho hàng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Integer changeQuantity;
    private Integer stockAfter;
    private InventoryTransactionType transactionType;
    private String transactionTypeDescription;
    private Long orderId;
    private String note;
    private LocalDateTime createdAt;
}
