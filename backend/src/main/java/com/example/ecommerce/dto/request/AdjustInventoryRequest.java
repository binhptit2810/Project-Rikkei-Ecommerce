package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận yêu cầu điều chỉnh tồn kho thủ công từ Admin.
 * changeQuantity:
 *  - Dương (> 0): Nhập thêm vào kho
 *  - Âm (< 0): Xuất bớt khỏi kho
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustInventoryRequest {

    @NotNull(message = "Số lượng điều chỉnh không được để trống")
    private Integer changeQuantity;

    private String note;
}
