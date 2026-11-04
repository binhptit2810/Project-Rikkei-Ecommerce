package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.UpdateOrderStatusRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý đơn hàng dành cho Quản trị viên (Admin Order Management).
 * Phụ trách: Đỗ Hoàng Anh [DHA] - TSK-D08-DHA
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Lấy danh sách đơn hàng toàn hệ thống có phân trang, lọc theo trạng thái và tìm kiếm.
     * GET /api/v1/admin/orders?page=0&size=10&status=PENDING&search=ORD-
     *
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số phần tử trên mỗi trang (mặc định 10)
     * @param status Trạng thái đơn hàng (tùy chọn)
     * @param search Từ khóa tìm kiếm theo mã đơn, tên hoặc số điện thoại người nhận
     * @param sortBy Trường sắp xếp (mặc định createdAt)
     * @param sortDir Hướng sắp xếp (mặc định desc)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<OrderResponse> orders = orderService.getAdminOrders(
                page, size, status, search, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Lấy toàn bộ danh sách đơn hàng không phân trang.
     * GET /api/v1/admin/orders/all
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Lấy chi tiết đơn hàng theo ID dành cho Admin.
     * GET /api/v1/admin/orders/{id}
     *
     * @param id ID của đơn hàng
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Cập nhật trạng thái đơn hàng (tuân thủ quy trình chuyển đổi trạng thái State Machine).
     * PATCH /api/v1/admin/orders/{id}/status
     *
     * @param id ID của đơn hàng cần cập nhật
     * @param request DTO chứa trạng thái mới
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse response = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", response));
    }
}
