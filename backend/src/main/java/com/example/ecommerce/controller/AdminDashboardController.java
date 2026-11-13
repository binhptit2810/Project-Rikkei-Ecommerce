package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản trị Bảng điều khiển (Admin Dashboard Controller).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Tiêu chí nghiệm thu (DoD):
 * - API GET /admin/dashboard/stats: Đếm tổng đơn hàng, doanh thu thực tế, đơn chờ duyệt.
 * - Truy vấn danh sách các sản phẩm có nguy cơ hết hàng (stock_quantity <= 5).
 */
@RestController
@RequestMapping({"/api/v1/admin/dashboard", "/admin/dashboard"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * Lấy toàn bộ dữ liệu thống kê tổng quan cho Bảng điều khiển Quản trị viên.
     * GET /api/v1/admin/dashboard/stats
     * GET /admin/dashboard/stats
     *
     * @param lowStockThreshold Ngưỡng số lượng tồn kho để cảnh báo (mặc định: 5)
     * @return ApiResponse chứa DashboardStatsResponse (tổng đơn, doanh thu thực tế, đơn chờ duyệt, sản phẩm sắp hết hàng)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @RequestParam(name = "lowStockThreshold", required = false, defaultValue = "5") Integer lowStockThreshold
    ) {
        log.info("Admin truy vấn thống kê Dashboard với ngưỡng cảnh báo tồn kho <= {}", lowStockThreshold);
        DashboardStatsResponse stats = dashboardService.getDashboardStats(lowStockThreshold);
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê Dashboard thành công", stats));
    }

    /**
     * Truy vấn riêng danh sách các sản phẩm có nguy cơ hết hàng (stock_quantity <= 5).
     * GET /api/v1/admin/dashboard/low-stock
     * GET /admin/dashboard/low-stock
     *
     * @param threshold Ngưỡng số lượng tồn kho để lọc (mặc định: 5)
     * @return ApiResponse chứa danh sách ProductResponse
     */
    @GetMapping({"/low-stock", "/low-stock-products"})
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam(name = "threshold", required = false, defaultValue = "5") Integer threshold
    ) {
        log.info("Admin truy vấn danh sách sản phẩm sắp hết hàng với ngưỡng <= {}", threshold);
        List<ProductResponse> lowStockProducts = dashboardService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm có nguy cơ hết hàng thành công", lowStockProducts));
    }

    /**
     * Truy vấn riêng danh sách Top sản phẩm bán chạy nhất tháng.
     * GET /api/v1/admin/dashboard/top-products
     * GET /admin/dashboard/best-sellers
     *
     * @param limit Số lượng sản phẩm muốn lấy (mặc định: 5)
     * @return ApiResponse chứa danh sách ProductResponse
     */
    @GetMapping({"/top-products", "/best-sellers"})
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopSellingProducts(
            @RequestParam(name = "limit", required = false, defaultValue = "5") Integer limit
    ) {
        log.info("Admin truy vấn danh sách top sản phẩm bán chạy với limit = {}", limit);
        List<ProductResponse> bestSellers = dashboardService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách top sản phẩm bán chạy thành công", bestSellers));
    }
}

