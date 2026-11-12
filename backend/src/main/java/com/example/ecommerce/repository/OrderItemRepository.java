package com.example.ecommerce.repository;

import com.example.ecommerce.dto.response.BestSellerProjection;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Lấy danh sách sản phẩm Best Seller (bán chạy nhất) theo tháng và năm có phân trang.
     * Tính tổng số lượng bán ra theo từng sản phẩm: GROUP BY product_id, SUM(quantity).
     * Loại trừ các đơn hàng bị hủy (CANCELLED).
     * Sắp xếp theo tổng số lượng bán giảm dần (DESC).
     *
     * Phụ trách: Đặng Thanh Bình (DTB)
     * DoD: Tốc độ truy vấn < 30ms
     */
    @Query(value = "SELECT " +
                   "  p.id AS productId, " +
                   "  p.name AS productName, " +
                   "  p.description AS description, " +
                   "  p.price AS price, " +
                   "  p.image_url AS imageUrl, " +
                   "  p.category_id AS categoryId, " +
                   "  c.name AS categoryName, " +
                   "  p.stock_quantity AS stockQuantity, " +
                   "  p.active AS active, " +
                   "  p.rating_average AS ratingAverage, " +
                   "  p.total_reviews AS totalReviews, " +
                   "  p.created_at AS createdAt, " +
                   "  p.updated_at AS updatedAt, " +
                   "  CAST(SUM(oi.quantity) AS SIGNED) AS totalSold " +
                   "FROM order_items oi " +
                   "INNER JOIN orders o ON oi.order_id = o.id " +
                   "INNER JOIN products p ON oi.product_id = p.id " +
                   "LEFT JOIN categories c ON p.category_id = c.id " +
                   "WHERE o.order_status != 'CANCELLED' " +
                   "  AND o.created_at >= :startDate " +
                   "  AND o.created_at < :endDate " +
                   "GROUP BY p.id, c.name " +
                   "ORDER BY totalSold DESC, p.id ASC",
           countQuery = "SELECT COUNT(DISTINCT oi.product_id) " +
                        "FROM order_items oi " +
                        "INNER JOIN orders o ON oi.order_id = o.id " +
                        "WHERE o.order_status != 'CANCELLED' " +
                        "  AND o.created_at >= :startDate " +
                        "  AND o.created_at < :endDate",
           nativeQuery = true)
    Page<BestSellerProjection> findBestSellers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Kiểm tra người dùng đã từng mua sản phẩm và đơn hàng ở trạng thái chỉ định hay chưa.
     */
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND oi.order.orderStatus = :orderStatus")
    boolean hasUserPurchasedProductWithStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("orderStatus") OrderStatus orderStatus);
}
