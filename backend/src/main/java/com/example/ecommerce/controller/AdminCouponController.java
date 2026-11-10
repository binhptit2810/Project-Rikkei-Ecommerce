package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateCouponRequest;
import com.example.ecommerce.dto.request.UpdateCouponRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.CouponResponse;
import com.example.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản trị Mã giảm giá (Admin Coupon & Voucher Management API).
 * Người phụ trách: Đặng Thanh Bình (DTB)
 * Người phối hợp: Đỗ Đức Mạnh (DDM)
 *
 * Tiêu chí nghiệm thu (DoD):
 * Trang quản trị voucher cho phép Admin thêm/sửa/xóa mã nhanh chóng.
 * Tất cả endpoint được bảo vệ nghiêm ngặt bằng vai trò ADMIN:
 * - Bảo vệ ở cấp Filter: /api/v1/admin/** (hasRole('ADMIN'))
 * - Bảo vệ ở cấp Method: @PreAuthorize("hasRole('ADMIN')")
 */
@RestController
@RequestMapping({"/api/v1/admin/coupons", "/api/v1/admin/vouchers"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * Lấy toàn bộ danh sách mã giảm giá trong hệ thống (bao gồm đang dùng, tạm dừng, hết hạn).
     * GET /api/v1/admin/coupons hoặc GET /api/v1/admin/vouchers
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        log.info("Admin truy vấn toàn bộ danh sách mã giảm giá");
        List<CouponResponse> coupons = couponService.getAllCouponsForAdmin();
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Tra cứu thông tin chi tiết một mã giảm giá theo ID.
     * GET /api/v1/admin/coupons/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        log.info("Admin xem chi tiết mã giảm giá ID: {}", id);
        CouponResponse coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }

    /**
     * Tạo mới mã giảm giá (Voucher).
     * POST /api/v1/admin/coupons
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        log.info("Admin yêu cầu tạo mới mã giảm giá: {}", request.getCode());
        CouponResponse created = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công", created));
    }

    /**
     * Cập nhật thông tin mã giảm giá hiện có.
     * PUT /api/v1/admin/coupons/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request) {
        log.info("Admin cập nhật mã giảm giá ID: {}", id);
        CouponResponse updated = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã giảm giá thành công", updated));
    }

    /**
     * Xóa vĩnh viễn mã giảm giá khỏi hệ thống.
     * DELETE /api/v1/admin/coupons/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        log.info("Admin xóa mã giảm giá ID: {}", id);
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mã giảm giá thành công", null));
    }

    /**
     * Bật / tắt nhanh trạng thái kích hoạt của mã giảm giá (Active/Inactive).
     * PATCH /api/v1/admin/coupons/{id}/toggle-status
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCouponStatus(@PathVariable Long id) {
        log.info("Admin chuyển đổi trạng thái kích hoạt mã giảm giá ID: {}", id);
        CouponResponse toggled = couponService.toggleCouponStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", toggled));
    }
}
