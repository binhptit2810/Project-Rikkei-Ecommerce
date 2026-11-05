package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.InventoryTransactionType;
import com.example.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.ecommerce.security.CustomUserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller quản trị tồn kho (Admin Inventory Controller).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Đỗ Đức Mạnh [DDM]
 * Task ID: [TSK-D07-DTB] Xây dựng API Điều chỉnh Kho Thủ công & Tra cứu Nhật ký Biến động Kho
 *
 * Yêu cầu phân quyền:
 * - Bảo vệ ở cấp Filter: /api/v1/admin/** (hasRole('ADMIN'))
 * - Bảo vệ ở cấp Method: @PreAuthorize("hasRole('ADMIN')")
 */
@RestController
@RequestMapping({"/api/v1/admin/inventory", "/admin/inventory"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminInventoryController {

    private final InventoryService inventoryService;

    /**
     * API điều chỉnh kho thủ công cho một sản phẩm (nhập hoặc xuất kho).
     * POST /api/v1/admin/inventory/{id}/adjust
     * POST /admin/inventory/{id}/adjust
     *
     * @param id ID của sản phẩm cần điều chỉnh
     * @param request DTO chứa changeQuantity, type, reason/note
     * @return ApiResponse chứa InventoryTransactionResponse
     */
    @PostMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> adjustStock(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdjustInventoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long adminUserId = (userDetails instanceof CustomUserDetails cud) ? cud.getUserId() : null;
        InventoryTransactionResponse response = (adminUserId != null)
                ? inventoryService.adjustStock(id, request, adminUserId)
                : inventoryService.adjustStock(id, request);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", response));
    }

    /**
     * API tra cứu lịch sử biến động kho trên toàn hệ thống có bộ lọc và phân trang.
     * GET /api/v1/admin/inventory/transactions
     * GET /admin/inventory/transactions
     *
     * @param productId Lọc theo ID sản phẩm (tùy chọn)
     * @param type Lọc theo loại giao dịch (ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT, MANUAL_EXPORT, INITIAL_STOCK)
     * @param startDate Lọc từ thời điểm (ISO-8601, ví dụ: 2026-11-01T00:00:00)
     * @param endDate Lọc đến thời điểm
     * @param page Số trang (mặc định 0)
     * @param size Số phần tử mỗi trang (mặc định 10)
     * @param sortBy Trường sắp xếp (mặc định createdAt)
     * @param sortDir Hướng sắp xếp (mặc định desc)
     * @return ApiResponse chứa PageResponse<InventoryTransactionResponse>
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<InventoryTransactionResponse> response = inventoryService.getTransactions(
                productId,
                type,
                startDate,
                endDate,
                page,
                size,
                sortBy,
                sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * API tra cứu nhanh lịch sử biến động kho của một sản phẩm cụ thể.
     * GET /api/v1/admin/inventory/{id}/transactions
     * GET /admin/inventory/{id}/transactions
     *
     * @param id ID của sản phẩm
     * @return ApiResponse chứa danh sách lịch sử biến động kho của sản phẩm
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getTransactionsByProduct(
            @PathVariable("id") Long id
    ) {
        List<InventoryTransactionResponse> response = inventoryService.getTransactionsByProduct(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

