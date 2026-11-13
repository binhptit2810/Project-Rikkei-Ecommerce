package com.example.ecommerce.repository;

import com.example.ecommerce.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy vấn dữ liệu hình ảnh đánh giá (bảng review_images).
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 */
@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
     * Lấy danh sách ảnh thuộc về một review theo reviewId sắp xếp theo thời gian tạo tăng dần.
     */
    List<ReviewImage> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

    /**
     * Đếm số lượng ảnh hiện có của một review.
     */
    int countByReviewId(Long reviewId);

    /**
     * Xóa toàn bộ ảnh thuộc về một review.
     */
    void deleteByReviewId(Long reviewId);
}
