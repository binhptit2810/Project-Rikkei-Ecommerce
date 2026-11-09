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
 * Controller tiếp nhận và xử lý tải lên tệp tin / hình ảnh dành cho Admin.
 * Endpoint: POST /api/v1/admin/uploads
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Tải lên hình ảnh sản phẩm hoặc danh mục từ máy tính.
     * POST /api/v1/admin/uploads (multipart/form-data)
     *
     * @param file Tệp tin hình ảnh cần upload
     * @return FileUploadResponse chứa URL truy cập hình ảnh
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        FileUploadResponse response = fileStorageService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.success("Tải hình ảnh lên máy chủ thành công", response));
    }
}
