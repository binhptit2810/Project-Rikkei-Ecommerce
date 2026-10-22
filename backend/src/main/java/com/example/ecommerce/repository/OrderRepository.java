package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderCode(String orderCode);

    long countByOrderStatus(com.example.ecommerce.entity.OrderStatus orderStatus);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = :orderStatus")
    java.math.BigDecimal sumTotalAmountByOrderStatus(@org.springframework.data.repository.query.Param("orderStatus") com.example.ecommerce.entity.OrderStatus orderStatus);
}
