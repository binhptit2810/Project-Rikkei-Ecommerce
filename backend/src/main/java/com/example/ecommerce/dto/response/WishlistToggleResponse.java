package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi kết quả toggle (thêm/xóa) sản phẩm trong Wishlist.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistToggleResponse {

    private boolean liked;
    private String message;
}

