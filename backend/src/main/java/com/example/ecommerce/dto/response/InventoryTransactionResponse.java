package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.InventoryTransaction;
import com.example.ecommerce.entity.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin biến động tồn kho (Inventory Transaction Response).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public static InventoryTransactionResponse fromEntity(InventoryTransaction tx) {
        if (tx == null) {
            return null;
        }

        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .productId(tx.getProduct() != null ? tx.getProduct().getId() : null)
                .productName(tx.getProduct() != null ? tx.getProduct().getName() : null)
                .changeQuantity(tx.getChangeQuantity())
                .stockAfter(tx.getStockAfter())
                .transactionType(tx.getTransactionType())
                .transactionTypeDescription(tx.getTransactionType() != null ? tx.getTransactionType().getDescription() : null)
                .orderId(tx.getOrderId())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

