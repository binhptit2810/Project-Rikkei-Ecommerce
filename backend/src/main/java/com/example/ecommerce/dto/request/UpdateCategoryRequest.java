package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho API cập nhật thông tin danh mục (Admin):
 * PUT /api/v1/admin/categories/{id}
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String name;

    private String description;

    @Size(max = 500, message = "Đường dẫn hình ảnh không được vượt quá 500 ký tự")
    private String imageUrl;

    private Boolean active;
}

