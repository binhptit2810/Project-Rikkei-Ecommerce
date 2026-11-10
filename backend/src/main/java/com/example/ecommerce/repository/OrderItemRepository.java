package com.example.ecommerce.repository;

import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

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
