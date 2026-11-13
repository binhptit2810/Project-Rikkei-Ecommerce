package com.example.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Tiêu chí nghiệm thu (DoD):
 * 1. Tổng doanh thu hôm nay, tuần này, tháng này, năm nay.
 * 2. Tổng số đơn hàng (Chờ xử lý, Đã thanh toán, Đã giao, Đã hủy).
 * 3. Doanh thu theo từng ngày trong tháng phục vụ vẽ biểu đồ (Chart Data).
 * 4. Tỷ lệ thanh toán: COD vs VNPay Online.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // =========================================================================
    // 1. CHỈ SỐ DOANH THU THEO KỲ (TODAY, THIS WEEK, THIS MONTH, THIS YEAR)
    // =========================================================================

    /**
     * Doanh thu hôm nay (từ 00:00:00 hôm nay đến hiện tại).
     */
    private BigDecimal revenueToday;

    /**
     * Doanh thu tuần này (từ Thứ Hai tuần này đến hiện tại).
     */
    private BigDecimal revenueThisWeek;

    /**
     * Doanh thu tháng này (từ ngày 01 tháng này đến hiện tại).
     */
    private BigDecimal revenueThisMonth;

    /**
     * Doanh thu năm nay (từ 01/01 năm nay đến hiện tại).
     */
    private BigDecimal revenueThisYear;

    /**
     * Doanh thu thực tế lũy kế toàn thời gian (tổng tiền các đơn hàng đã thanh toán PAID hoặc hoàn tất COMPLETED).
     */
    private BigDecimal actualRevenue;

    /**
     * Tên trường tương đương/alias cho frontend hoặc các client khác.
     */
    private BigDecimal totalRevenue;

    // =========================================================================
    // 2. THỐNG KÊ ĐƠN HÀNG THEO TRẠNG THÁI (CHỜ XỬ LÝ, ĐÃ THANH TOÁN, ĐÃ GIAO, ĐÃ HỦY)
    // =========================================================================

    /**
     * Tổng số đơn hàng trên toàn hệ thống.
     */
    private long totalOrders;

    /**
     * Số đơn hàng đang chờ duyệt / chờ xử lý (trạng thái PENDING).
     */
    private long pendingOrders;

    /**
     * Số đơn hàng đã thanh toán (paymentStatus = PAID).
     */
    private long paidOrders;

    /**
     * Số đơn hàng đã giao thành công (trạng thái COMPLETED).
     */
    private long deliveredOrders;

    /**
     * Số đơn hàng đã hoàn thành (tương đương deliveredOrders).
     */
    private long completedOrders;

    /**
     * Số đơn hàng đã hủy (trạng thái CANCELLED).
     */
    private long cancelledOrders;

    /**
     * Số đơn hàng đã xác nhận (trạng thái CONFIRMED).
     */
    private long confirmedOrders;

    /**
     * Số đơn hàng đang chuẩn bị / đóng gói (trạng thái PROCESSING).
     */
    private long processingOrders;

    /**
     * Số đơn hàng đang giao vận (trạng thái SHIPPING).
     */
    private long shippingOrders;

    // =========================================================================
    // 3. DOANH THU THEO TỪNG NGÀY TRONG THÁNG PHỤC VỤ VẼ BIỂU ĐỒ (CHART DATA)
    // =========================================================================

    /**
     * Dữ liệu thống kê doanh thu và số đơn từng ngày trong tháng hiện tại.
     */
    private List<DailyRevenuePoint> dailyRevenue;

    // =========================================================================
    // 4. TỶ LỆ THANH TOÁN: COD VS VNPAY ONLINE
    // =========================================================================

    /**
     * Thống kê tỷ lệ và số lượng phương thức thanh toán COD vs VNPay Online.
     */
    @JsonProperty("paymentDistribution")
    private PaymentDistribution paymentDistribution;

    /**
     * Alias để frontend có thể đọc qua paymentMethodStats nếu cần.
     */
    public PaymentDistribution getPaymentMethodStats() {
        return paymentDistribution;
    }

    // =========================================================================
    // 5. CÁC CHỈ SỐ BỔ TRỢ HỆ THỐNG (TỒN KHO, NGƯỜI DÙNG, NHẬT KÝ KHO)
    // =========================================================================

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

    /**
     * Thống kê nhật ký kho: Số giao dịch trừ kho do đặt hàng (ORDER_PLACED).
     */
    private Long orderPlacedCount;

    /**
     * Thống kê nhật ký kho: Số giao dịch hoàn kho do hủy đơn (ORDER_CANCELLED).
     */
    private Long orderCancelledCount;

    /**
     * Thống kê nhật ký kho: Số giao dịch admin nhập kho thủ công (MANUAL_IMPORT).
     */
    private Long manualImportCount;

    // =========================================================================
    // SUB-DTOs
    // =========================================================================

    /**
     * Điểm dữ liệu biểu đồ doanh thu theo từng ngày trong tháng.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenuePoint {
        /**
         * Ngày theo định dạng ISO (YYYY-MM-DD), ví dụ: "2026-11-01".
         */
        private String date;

        /**
         * Ngày trong tháng (1..31).
         */
        private int day;

        /**
         * Doanh thu thực tế trong ngày.
         */
        private BigDecimal revenue;

        /**
         * Số lượng đơn hàng được tạo trong ngày.
         */
        private long orderCount;
    }

    /**
     * Thống kê phân bổ phương thức thanh toán COD vs VNPay Online.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDistribution {
        /**
         * Số lượng đơn hàng thanh toán COD.
         */
        private long codCount;

        /**
         * Doanh thu từ thanh toán COD (đã thu/hoàn tất).
         */
        private BigDecimal codRevenue;

        /**
         * Tỷ lệ phần trăm đơn hàng COD (0..100%).
         */
        private double codPercentage;

        /**
         * Số lượng đơn hàng thanh toán VNPay Online.
         */
        private long vnpayCount;

        /**
         * Doanh thu từ thanh toán VNPay Online.
         */
        private BigDecimal vnpayRevenue;

        /**
         * Tỷ lệ phần trăm đơn hàng VNPay Online (0..100%).
         */
        private double vnpayPercentage;

        /**
         * Tổng số giao dịch thanh toán được thống kê.
         */
        private long totalCount;

        /**
         * Tổng doanh thu các phương thức thanh toán.
         */
        private BigDecimal totalRevenue;
    }
}

