package com.example.ecommerce.repository;

import com.example.ecommerce.entity.InventoryTransaction;
import com.example.ecommerce.entity.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<InventoryTransaction> findByOrderId(Long orderId);

    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();

    Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByTransactionType(InventoryTransactionType transactionType);

    @Query("SELECT COALESCE(SUM(it.changeQuantity), 0) FROM InventoryTransaction it WHERE it.product.id = :productId")
    Integer sumChangeQuantityByProductId(@Param("productId") Long productId);

    @Query("SELECT it FROM InventoryTransaction it " +
           "LEFT JOIN FETCH it.product p " +
           "WHERE (:productId IS NULL OR p.id = :productId) " +
           "AND (:transactionType IS NULL OR it.transactionType = :transactionType) " +
           "AND (:startDate IS NULL OR it.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR it.createdAt <= :endDate)")
    Page<InventoryTransaction> findWithFilters(
            @Param("productId") Long productId,
            @Param("transactionType") InventoryTransactionType transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
