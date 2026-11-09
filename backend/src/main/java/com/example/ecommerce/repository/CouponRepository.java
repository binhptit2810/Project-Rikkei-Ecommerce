package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    Optional<Coupon> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Coupon> findByActiveTrueOrderByCreatedAtDesc();

    /**
     * Khóa bi quan (PESSIMISTIC_WRITE) khi cập nhật lượt sử dụng mã giảm giá.
     * Chống xung đột đồng thời (Race condition) và ngăn chặn vượt số lần sử dụng tối đa (usage_limit).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}

