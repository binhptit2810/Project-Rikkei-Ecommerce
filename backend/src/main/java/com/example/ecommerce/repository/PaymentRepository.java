package com.example.ecommerce.repository;

import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    long countByPaymentMethod(PaymentMethod paymentMethod);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p JOIN p.order o WHERE p.paymentMethod = :paymentMethod " +
           "AND (p.status = :paidStatus OR o.orderStatus = :completedStatus) " +
           "AND o.orderStatus <> :cancelledStatus")
    BigDecimal sumRevenueByPaymentMethod(
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("paidStatus") PaymentStatus paidStatus,
            @Param("completedStatus") OrderStatus completedStatus,
            @Param("cancelledStatus") OrderStatus cancelledStatus
    );
}

