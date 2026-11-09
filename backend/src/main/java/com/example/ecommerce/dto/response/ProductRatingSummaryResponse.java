package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO phản hồi thống kê điểm đánh giá và tỷ lệ các mức sao của sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummaryResponse {

    /** Điểm đánh giá trung bình làm tròn 1 chữ số thập phân (ví dụ: 4.8). */
    private Double ratingAverage;

    /** Tổng số lượt đánh giá của sản phẩm. */
    private Integer totalReviews;

    /** Thống kê số lượng đánh giá theo từng mức sao: 1, 2, 3, 4, 5. */
    private Map<Integer, Long> ratingCounts;
}

