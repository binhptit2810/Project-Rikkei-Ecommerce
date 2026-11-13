package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Review;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu đánh giá sản phẩm (bảng reviews).
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Lấy danh sách đánh giá của sản phẩm có phân trang (kèm tất cả trạng thái).
     */
    Page<Review> findByProductId(Long productId, Pageable pageable);

    /**
     * Lấy danh sách đánh giá đang hiển thị công khai của sản phẩm có phân trang [TSK-D14-CCT].
     */
    Page<Review> findByProductIdAndIsVisibleTrue(Long productId, Pageable pageable);

    /**
     * Khóa bi quan (Pessimistic Write Lock) một bản ghi Review để đồng bộ tải ảnh an toàn,
     * tránh race condition vượt quá giới hạn 3 ảnh khi có nhiều request đồng thời [TSK-D14-CCT].
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") Long id);

    /**
     * Lọc và tìm kiếm danh sách đánh giá dành cho Quản trị viên (Admin).
     */
    @Query(value = "SELECT r FROM Review r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user WHERE " +
           "(:productId IS NULL OR r.product.id = :productId) AND " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:isHidden IS NULL OR r.isHidden = :isHidden)",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE " +
           "(:productId IS NULL OR r.product.id = :productId) AND " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:isHidden IS NULL OR r.isHidden = :isHidden)")
    Page<Review> findAllForAdmin(@Param("productId") Long productId,
                                 @Param("rating") Integer rating,
                                 @Param("isHidden") Boolean isHidden,
                                 Pageable pageable);

    /**
     * Lấy danh sách đánh giá phục vụ Quản trị viên (Admin) kèm tìm nạp Product và User,
     * hỗ trợ lọc theo trạng thái isVisible (null = lấy tất cả).
     */
    @Query(value = "SELECT r FROM Review r LEFT JOIN FETCH r.product LEFT JOIN FETCH r.user WHERE (:isVisible IS NULL OR r.isVisible = :isVisible)",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE (:isVisible IS NULL OR r.isVisible = :isVisible)")
    Page<Review> findAllForAdmin(@Param("isVisible") Boolean isVisible, Pageable pageable);

    /**
     * Kiểm tra người dùng đã đánh giá sản phẩm này chưa (chặn đánh giá trùng lặp).
     */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /**
     * Tìm bản ghi đánh giá của người dùng cụ thể đối với một sản phẩm.
     */
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    /**
     * Tính điểm đánh giá trung bình của sản phẩm (chỉ tính các review đang hiển thị công khai).
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND (r.isVisible = true OR r.isVisible IS NULL) AND (r.isHidden = false OR r.isHidden IS NULL)")
    Double calculateAverageRating(@Param("productId") Long productId);

    /**
     * Đếm tổng số lượt đánh giá của sản phẩm (chỉ tính các review đang hiển thị công khai).
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND (r.isVisible = true OR r.isVisible IS NULL) AND (r.isHidden = false OR r.isHidden IS NULL)")
    Integer countByProductId(@Param("productId") Long productId);

    /**
     * Thống kê số lượng đánh giá gom nhóm theo từng mức sao (1, 2, 3, 4, 5) của các review hiển thị công khai.
     * Kết quả mỗi phần tử: Object[]{ Integer rating, Long count }
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND (r.isVisible = true OR r.isVisible IS NULL) AND (r.isHidden = false OR r.isHidden IS NULL) GROUP BY r.rating")
    List<Object[]> countRatingsGrouped(@Param("productId") Long productId);
}
