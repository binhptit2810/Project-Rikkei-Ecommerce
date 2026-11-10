package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi dữ liệu "Deal of the Day" (Sản phẩm ưu đãi trong ngày).
 * Toàn bộ thông tin lấy trực tiếp từ database MySQL và mã khuyến mãi thực tế.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealOfTheDayResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;

    /** Giá gốc trước khuyến mãi */
    private BigDecimal originalPrice;

    /** Giá bán khuyến mãi (khớp chính xác với giá trong bảng products) */
    private BigDecimal salePrice;

    /** Tỷ lệ phần trăm giảm giá tính chính xác */
    private Integer discountPercentage;

    /** Số lượng tồn kho thực tế */
    private Integer stockQuantity;

    /** Điểm đánh giá trung bình */
    private Double ratingAverage;

    /** Tổng số lượt đánh giá */
    private Integer totalReviews;

    /** Mã giảm giá liên quan (nếu có) */
    private String couponCode;

    /** Thời điểm kết thúc khuyến mãi thực tế dùng cho đồng hồ đếm ngược */
    private LocalDateTime endTime;

    /** Trạng thái kích hoạt */
    private Boolean active;
}

