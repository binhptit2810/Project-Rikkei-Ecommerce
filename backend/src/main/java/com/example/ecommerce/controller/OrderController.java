package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CancelOrderRequest;
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
 * Controller quản lý đơn hàng dành cho khách hàng.
 * Phụ trách: Vũ Đức Huy Hoàng (Cart & Order Module).
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
public class OrderController {

    private final OrderService orderService;

    /**
     * Tạo đơn hàng mới từ giỏ hàng.
     * POST /api/v1/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", response));
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng hiện tại.
     * GET /api/v1/orders/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<OrderResponse> orders = orderService.getMyOrders(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Lấy chi tiết đơn hàng theo ID.
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        OrderResponse response = orderService.getOrderDetail(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Hủy đơn hàng (khi còn ở trạng thái PENDING hoặc CONFIRMED).
     * PATCH /api/v1/orders/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        String reason = (request != null) ? request.getReason() : null;
        OrderResponse response = orderService.cancelOrder(userDetails.getUsername(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", response));
    }
}
