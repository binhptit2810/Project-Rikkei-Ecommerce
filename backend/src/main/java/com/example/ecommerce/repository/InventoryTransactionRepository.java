package com.example.ecommerce.repository;

import com.example.ecommerce.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    org.springframework.data.domain.Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId, org.springframework.data.domain.Pageable pageable);

    List<InventoryTransaction> findByOrderId(Long orderId);

    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();

    org.springframework.data.domain.Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
