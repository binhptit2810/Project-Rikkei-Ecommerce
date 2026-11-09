package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ValidateCouponRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.CouponValidationResponse;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý mã giảm giá (Coupon API).
 * Người phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Người phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * API kiểm tra điều kiện và tính toán số tiền giảm giá khi áp mã tại Checkout / Giỏ hàng.
     * POST /api/v1/coupons/validate
     *
     * @param request Dữ liệu gồm mã code và giá trị đơn hàng (orderValue)
     * @return 200 OK cùng chi tiết số tiền được giảm và tổng thanh toán mới sau giảm
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request) {
        CouponValidationResponse response = couponService.validateCoupon(request);
        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã giảm giá thành công", response));
    }

    /**
     * Lấy danh sách các mã giảm giá đang hoạt động dành cho khách mua hàng.
     * GET /api/v1/coupons/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Coupon>>> getActiveCoupons() {
        List<Coupon> coupons = couponService.getActiveCoupons();
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Tra cứu chi tiết một mã giảm giá theo mã code.
     * GET /api/v1/coupons/{code}
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Coupon>> getCouponByCode(@PathVariable String code) {
        Coupon coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(ApiResponse.success(coupon));
    }
}

