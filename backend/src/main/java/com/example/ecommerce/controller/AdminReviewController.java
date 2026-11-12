package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị đánh giá sản phẩm dành cho Admin.
 * Phụ trách: Đỗ Đức Mạnh (DDM) & Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Lấy toàn bộ danh sách đánh giá của hệ thống có phân trang và bộ lọc.
     * GET /api/v1/admin/reviews?page=0&size=10&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean isHidden) {

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<ReviewResponse> response = reviewService.getAllReviewsForAdmin(pageable, productId, rating, isHidden);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

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

    /**
     * Admin bật/tắt trạng thái ẩn hoặc hiện của đánh giá.
     * PATCH /api/v1/admin/reviews/{reviewId}/visibility
     */
    @PatchMapping("/{reviewId}/visibility")
    public ResponseEntity<ApiResponse<ReviewResponse>> toggleReviewVisibility(
            @PathVariable Long reviewId) {

        ReviewResponse response = reviewService.toggleReviewVisibility(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hiển thị đánh giá thành công", response));
    }

    /**
     * Admin xóa đánh giá khỏi hệ thống.
     * DELETE /api/v1/admin/reviews/{reviewId}
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId) {

        reviewService.deleteReviewByAdmin(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}

