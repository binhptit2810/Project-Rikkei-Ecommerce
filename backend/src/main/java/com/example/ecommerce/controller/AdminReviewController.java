package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị Đánh giá & Nhận xét sản phẩm dành riêng cho Quản trị viên (Admin).
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 *
 * Tất cả endpoint yêu cầu quyền ADMIN:
 * - Bảo vệ Filter Chain: /api/v1/admin/** (hasRole('ADMIN'))
 * - Bảo vệ Method Security: @PreAuthorize("hasRole('ADMIN')")
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Lấy danh sách tất cả đánh giá phục vụ Quản trị viên (Admin) kèm phân trang và lọc theo trạng thái hiển thị.
     * GET /api/v1/admin/reviews?page=0&size=10&isVisible=true
     *
     * @param page      Số trang (mặc định: 0)
     * @param size      Số phần tử mỗi trang (mặc định: 10)
     * @param isVisible Lọc theo trạng thái hiển thị (null: tất cả, true: hiển thị, false: bị ẩn)
     * @return ApiResponse chứa PageResponse<ReviewResponse>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isVisible) {
        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ReviewResponse> response = reviewService.getAllReviewsForAdmin(isVisible, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Chuyển đổi trạng thái hiển thị công khai (ẩn / hiện) của đánh giá.
     * PUT /api/v1/admin/reviews/{id}/toggle-status
     *
     * @param id ID đánh giá
     * @return ApiResponse chứa ReviewResponse với trạng thái isVisible mới
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<ReviewResponse>> toggleReviewVisibility(@PathVariable Long id) {
        ReviewResponse response = reviewService.toggleReviewVisibility(id);
        String message = Boolean.TRUE.equals(response.getIsVisible())
                ? "Hiển thị đánh giá thành công"
                : "Ẩn đánh giá thành công";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Xóa vĩnh viễn đánh giá sản phẩm và dọn dẹp các tệp ảnh khỏi Local Storage.
     * DELETE /api/v1/admin/reviews/{id}
     *
     * @param id ID đánh giá cần xóa
     * @return ApiResponse thông báo thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReviewByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá vĩnh viễn thành công", null));
    }
}
