package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.WishlistItemResponse;
import com.example.ecommerce.dto.response.WishlistToggleResponse;
import com.example.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller tiếp nhận các yêu cầu API liên quan đến Danh sách sản phẩm yêu thích (Wishlist).
 * Base URL: /api/v1/wishlist
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 1. GET /api/v1/wishlist
     * Lấy danh sách sản phẩm đã yêu thích của người dùng hiện tại kèm thông tin chi tiết sản phẩm.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist() {
        List<WishlistItemResponse> wishlist = wishlistService.getWishlist();
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    /**
     * 2. GET /api/v1/wishlist/ids
     * Lấy danh sách ID các sản phẩm đã thả tim của người dùng hiện tại.
     */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Long>>> getWishlistProductIds() {
        List<Long> productIds = wishlistService.getWishlistProductIds();
        return ResponseEntity.ok(ApiResponse.success(productIds));
    }

    /**
     * 3. POST /api/v1/wishlist/toggle/{productId}
     * Thêm hoặc xóa sản phẩm khỏi danh sách yêu thích.
     */
    @PostMapping("/toggle/{productId}")
    public ResponseEntity<ApiResponse<WishlistToggleResponse>> toggleWishlist(@PathVariable Long productId) {
        WishlistToggleResponse response = wishlistService.toggleWishlist(productId);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}

