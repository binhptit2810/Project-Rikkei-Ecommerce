package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị đánh giá sản phẩm dành cho Admin.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D14-DDM] Xây Dựng API Chỉnh Sửa, Xóa Đánh Giá & Admin Phản Hồi Đánh Giá
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Admin phản hồi trực tiếp đánh giá của khách hàng.
     * POST /api/v1/admin/reviews/{reviewId}/reply
     */
    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReplyRequest request) {

        ReviewResponse response = reviewService.replyReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Phản hồi đánh giá thành công", response));
    }

    /**
     * Admin cập nhật lại câu trả lời / phản hồi đánh giá.
     * PUT /api/v1/admin/reviews/{reviewId}/reply
     */
    @PutMapping("/{reviewId}/reply")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReplyReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody AdminReplyRequest request) {

        ReviewResponse response = reviewService.replyReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phản hồi thành công", response));
    }
}

