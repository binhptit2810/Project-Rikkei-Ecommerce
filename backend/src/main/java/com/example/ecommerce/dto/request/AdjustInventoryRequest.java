package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu điều chỉnh tồn kho thủ công từ Admin.
 * Hỗ trợ cả hai trường hợp:
 * 1. Truyền changeQuantity có dấu (dương = nhập, âm = xuất).
 * 2. Hoặc truyền type (MANUAL_IMPORT / MANUAL_EXPORT) và changeQuantity.
 * 3. Điền lý do điều chỉnh qua trường reason hoặc note.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustInventoryRequest {

    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer changeQuantity;

    private InventoryTransactionType type;

    @Size(max = 255, message = "Lý do điều chỉnh không được vượt quá 255 ký tự")
    private String reason;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    /**
     * Lấy lý do điều chỉnh ưu tiên: reason -> note -> null
     */
    public String getEffectiveReason() {
        if (reason != null && !reason.trim().isEmpty()) {
            return reason.trim();
        }
        if (note != null && !note.trim().isEmpty()) {
            return note.trim();
        }
        return null;
    }
}

