package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO dữ liệu thống kê tổng quan Bảng điều khiển Quản trị viên (Admin Dashboard Stats).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Chứa các chỉ số nghiệp vụ:
 * 1. Tổng đơn hàng (totalOrders)
 * 2. Doanh thu thực tế từ các đơn đã thanh toán PAID (actualRevenue / totalRevenue)
 * 3. Đơn chờ duyệt (pendingOrders)
 * 4. Danh sách các sản phẩm có nguy cơ hết hàng (stock_quantity <= 5) (lowStockProducts)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    /**
     * Tổng số đơn hàng trên toàn hệ thống.
     */
    private long totalOrders;

    /**
     * Doanh thu thực tế (tổng tiền các đơn hàng đã thanh toán PAID và không bị hủy).
     */
    private BigDecimal actualRevenue;

    /**
     * Doanh thu thực tế (tên trường tương đương/alias cho frontend hoặc các client khác).
     */
    private BigDecimal totalRevenue;

    /**
     * Số đơn hàng đang chờ duyệt (trạng thái PENDING).
     */
    private long pendingOrders;

    /**
     * Số đơn hàng đã xác nhận (trạng thái CONFIRMED).
     */
    private long confirmedOrders;

    /**
     * Số đơn hàng đang xử lý (trạng thái PROCESSING).
     */
    private long processingOrders;

    /**
     * Số đơn hàng đang giao (trạng thái SHIPPING).
     */
    private long shippingOrders;

    /**
     * Số đơn hàng đã hoàn thành (trạng thái COMPLETED).
     */
    private long completedOrders;

    /**
     * Số đơn hàng đã hủy (trạng thái CANCELLED).
     */
    private long cancelledOrders;

    /**
     * Tổng số sản phẩm trong hệ thống.
     */
    private long totalProducts;

    /**
     * Số lượng sản phẩm có nguy cơ hết hàng (stock_quantity <= threshold).
     */
    private long lowStockCount;

    /**
     * Danh sách sản phẩm có nguy cơ hết hàng (mặc định stock_quantity <= 5).
     */
    private List<ProductResponse> lowStockProducts;

    /**
     * Tổng số người dùng hệ thống.
     */
    private long totalUsers;

    /**
     * Tổng số khách hàng (Role = CUSTOMER).
     */
    private long totalCustomers;
}

