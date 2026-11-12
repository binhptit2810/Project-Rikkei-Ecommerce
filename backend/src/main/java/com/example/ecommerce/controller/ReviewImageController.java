package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.ReviewImageResponse;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller xử lý tải lên hình ảnh đính kèm cho đánh giá sản phẩm.
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 *
 * Endpoint: POST /api/v1/reviews/{reviewId}/images (multipart/form-data)
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewImageController {

    private final ReviewService reviewService;

    /**
     * Tải lên từ 1 đến 3 ảnh đính kèm cho đánh giá sản phẩm.
     * Yêu cầu khách hàng đã đăng nhập và là chính chủ của đánh giá.
     *
     * @param reviewId ID của đánh giá
     * @param files    Danh sách tệp tin ảnh tải lên (hoặc tệp đơn 'file')
     * @param file     Tệp đơn tùy chọn để linh hoạt tương thích với nhiều client
     * @return ApiResponse chứa danh sách ReviewImageResponse
     */
    @PostMapping(value = "/{reviewId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReviewImageResponse>>> uploadReviewImages(
            @PathVariable Long reviewId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        List<MultipartFile> uploadList = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    uploadList.add(f);
                }
            }
        }
        if (file != null && !file.isEmpty()) {
            uploadList.add(file);
        }

        if (uploadList.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một tệp tin hình ảnh để tải lên");
        }

        List<ReviewImageResponse> response = reviewService.uploadReviewImages(reviewId, uploadList);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải lên hình ảnh đánh giá thành công", response));
    }
}

