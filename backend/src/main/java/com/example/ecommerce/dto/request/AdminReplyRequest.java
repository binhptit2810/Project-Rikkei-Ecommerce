package com.example.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu Quản trị viên phản hồi đánh giá khách hàng.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D14-DDM] Xây Dựng API Chỉnh Sửa, Xóa Đánh Giá & Admin Phản Hồi Đánh Giá
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReplyRequest {

    /**
     * Nội dung câu trả lời / phản hồi từ Quản trị viên (hỗ trợ cả key "reply" hoặc "adminReply").
     */
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(max = 1000, message = "Nội dung phản hồi không được vượt quá 1000 ký tự")
    @JsonAlias({"adminReply"})
    private String reply;
}

