package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu gửi đánh giá sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Rating tối thiểu là 1 sao")
    @Max(value = 5, message = "Rating tối đa là 5 sao")
    private Integer rating;

    @NotBlank(message = "Nội dung đánh giá không được để trống")
    @Size(max = 1000, message = "Nội dung đánh giá không được vượt quá 1000 ký tự")
    private String comment;
}

