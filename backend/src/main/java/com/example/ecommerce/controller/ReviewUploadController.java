package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller tiếp nhận và xử lý tải lên tệp tin / hình ảnh đánh giá của khách hàng.
 * Phụ trách: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 */
@RestController
@RequestMapping("/api/v1/reviews/upload")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReviewUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Khách hàng tải ảnh đính kèm cho bài đánh giá sản phẩm (tối đa 3 ảnh).
     * POST /api/v1/reviews/upload (multipart/form-data)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadReviewImage(
            @RequestParam("file") MultipartFile file) {

        FileUploadResponse response = fileStorageService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.success("Tải hình ảnh đánh giá thành công", response));
    }
}
