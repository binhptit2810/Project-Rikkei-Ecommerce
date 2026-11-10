package com.example.ecommerce.repository;

import com.example.ecommerce.entity.PaymentTransaction;
import com.example.ecommerce.entity.ReconciliationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất lịch sử các lần thử thanh toán (Payment Attempts).
 * Phụ trách: Cao Chí Thiện (TSK-D12-CCT - Phase 1)
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.txnRef = :txnRef")
    Optional<PaymentTransaction> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    List<PaymentTransaction> findByReconciliationStatus(ReconciliationStatus reconciliationStatus);

    boolean existsByTxnRef(String txnRef);
}

