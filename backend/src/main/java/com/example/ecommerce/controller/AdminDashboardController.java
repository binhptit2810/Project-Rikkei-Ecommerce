package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller thống kê số liệu Dashboard cho Admin.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderService orderService;

    /**
     * Lấy toàn bộ số liệu thống kê đơn hàng, doanh thu và tồn kho.
     * GET /api/v1/admin/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = orderService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
