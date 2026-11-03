package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý đơn hàng (Order API).
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
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
     * Lấy danh sách toàn bộ lịch sử đơn hàng của người dùng đang đăng nhập.
     * GET /api/v1/orders/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<OrderResponse> orders = orderService.getMyOrders(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Xem thông tin chi tiết một đơn hàng theo ID.
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse order = orderService.getOrderById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * Hủy đơn hàng (chỉ áp dụng khi đơn hàng còn ở trạng thái PENDING hoặc CONFIRMED).
     * Sau khi hủy thành công, số lượng tồn kho sẽ được tự động hoàn lại đầy đủ.
     * PATCH /api/v1/orders/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse order = orderService.cancelOrder(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", order));
    }
}

