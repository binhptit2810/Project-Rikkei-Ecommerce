package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.InventoryService;
import com.example.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý tồn kho dành riêng cho Admin.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final ProductService productService;

    /**
     * Lấy danh sách sản phẩm phục vụ quản lý tồn kho.
     * GET /api/v1/admin/inventory
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getInventoryList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "stockQuantity") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<ProductResponse> products = productService.getAdminProducts(
                keyword, categoryId, active, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Admin điều chỉnh tăng hoặc giảm số lượng tồn kho của một sản phẩm.
     * POST /api/v1/admin/inventory/{id}/adjust
     */
    @PostMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        InventoryTransactionResponse response = inventoryService.adjustStock(id, request);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", response));
    }

    /**
     * Xem toàn bộ lịch sử biến động kho trong hệ thống.
     * GET /api/v1/admin/inventory/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getAllTransactions() {
        List<InventoryTransactionResponse> list = inventoryService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * Xem lịch sử biến động kho của một sản phẩm cụ thể.
     * GET /api/v1/admin/inventory/{id}/transactions
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getProductTransactions(
            @PathVariable Long id
    ) {
        List<InventoryTransactionResponse> list = inventoryService.getTransactionsByProduct(id);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
