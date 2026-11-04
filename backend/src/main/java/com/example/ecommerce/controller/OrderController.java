package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CancelOrderRequest;
import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý đơn hàng (Order API).
 * Người phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Người phối hợp: Đặng Thanh Bình [DTB] (Inventory & Payment)
 *
 * Tất cả các endpoints yêu cầu người dùng phải đăng nhập (vai trò CUSTOMER hoặc ADMIN).
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng mới từ giỏ hàng.
     * POST /api/v1/orders
     *
     * @param request Thông tin giao hàng, phương thức thanh toán và danh sách sản phẩm (chỉ gồm productId và quantity).
     * @param userDetails Thông tin tài khoản người dùng đang đăng nhập lấy từ token JWT.
     * @return 201 Created cùng dữ liệu chi tiết đơn hàng đã được tính toán an toàn tại Backend.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse orderResponse = orderService.createOrder(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", orderResponse));
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng đang đăng nhập có phân trang.
     * GET /api/v1/orders/my?page=0&size=10&sortBy=createdAt&sortDir=desc
     *
     * @param page Số thứ tự trang (mặc định 0)
     * @param size Số phần tử mỗi trang (mặc định 10)
     * @param sortBy Trường sắp xếp (mặc định createdAt)
     * @param sortDir Hướng sắp xếp (mặc định desc)
     * @param userDetails Thông tin tài khoản người dùng đang đăng nhập
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetails userDetails) {
        PageResponse<OrderResponse> orders = orderService.getMyOrders(
                userDetails.getUsername(), page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Xem thông tin chi tiết một đơn hàng theo ID kèm danh sách món hàng (OrderItem).
     * GET /api/v1/orders/{id}
     *
     * @param id ID của đơn hàng
     * @param userDetails Thông tin tài khoản người dùng đang đăng nhập
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse order = orderService.getOrderDetail(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * Hủy đơn hàng (chỉ áp dụng khi đơn hàng còn ở trạng thái PENDING hoặc CONFIRMED).
     * Sau khi hủy thành công, số lượng tồn kho sẽ được tự động hoàn lại đầy đủ qua restoreStockForOrder() [DTB].
     * PATCH /api/v1/orders/{id}/cancel
     *
     * @param id ID của đơn hàng cần hủy
     * @param request Lý do hủy đơn hàng (tùy chọn)
     * @param userDetails Thông tin tài khoản người dùng đang đăng nhập
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String reason = (request != null) ? request.getReason() : null;
        OrderResponse order = (reason != null && !reason.trim().isEmpty())
                ? orderService.cancelOrder(id, userDetails.getUsername(), reason.trim())
                : orderService.cancelOrder(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", order));
    }
}
