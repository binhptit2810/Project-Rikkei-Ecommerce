package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy vấn dữ liệu đánh giá sản phẩm (bảng reviews).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Lấy danh sách đánh giá của sản phẩm có phân trang.
     */
    Page<Review> findByProductId(Long productId, Pageable pageable);

    /**
     * Lọc và tìm kiếm danh sách đánh giá dành cho Quản trị viên (Admin).
     */
    @Query("SELECT r FROM Review r WHERE " +
           "(:productId IS NULL OR r.product.id = :productId) AND " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:isHidden IS NULL OR r.isHidden = :isHidden)")
    Page<Review> findAllForAdmin(@Param("productId") Long productId,
                                 @Param("rating") Integer rating,
                                 @Param("isHidden") Boolean isHidden,
                                 Pageable pageable);

    /**
     * Kiểm tra người dùng đã đánh giá sản phẩm này chưa (chặn đánh giá trùng lặp).
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /**
     * Tính điểm đánh giá trung bình của sản phẩm.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRating(@Param("productId") Long productId);

    /**
     * Đếm tổng số lượt đánh giá của sản phẩm.
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Integer countByProductId(@Param("productId") Long productId);

    /**
     * Thống kê số lượng đánh giá gom nhóm theo từng mức sao (1, 2, 3, 4, 5).
     * Kết quả mỗi phần tử: Object[]{ Integer rating, Long count }
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating")
    List<Object[]> countRatingsGrouped(@Param("productId") Long productId);
}

