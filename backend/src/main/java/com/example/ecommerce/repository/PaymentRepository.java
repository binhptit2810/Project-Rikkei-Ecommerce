package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);

    boolean existsByVnpTxnRef(String vnpTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transactionCode = :transactionCode")
    Optional<Payment> findByTransactionCodeForUpdate(@Param("transactionCode") String transactionCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    boolean existsByTransactionCode(String transactionCode);
}

