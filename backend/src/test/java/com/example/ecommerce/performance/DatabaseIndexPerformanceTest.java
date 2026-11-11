package com.example.ecommerce.performance;

import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.Payment;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ReviewRepository;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử hiệu năng và cấu trúc Index theo tiêu chuẩn DoD (Definition of Done).
 * Yêu cầu:
 * - Đánh index cho coupon_code trên orders/coupons
 * - Đánh index cho product_id trên reviews
 * - Đánh index cho vnp_txn_ref trên payments
 * - Tốc độ query < 50ms
 *
 * Người phụ trách: Đặng Thanh Bình (DTB)
 * Người phối hợp: Cao Chí Thiện (CCT)
 */
@ExtendWith(MockitoExtension.class)
class DatabaseIndexPerformanceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("DOD-01: Thực thể Order phải có cấu hình @Index cho trường coupon_code")
    void testOrderEntityHasCouponCodeIndex() {
        Table tableAnnotation = Order.class.getAnnotation(Table.class);
        assertThat(tableAnnotation).isNotNull();

        boolean hasCouponCodeIndex = Arrays.stream(tableAnnotation.indexes())
                .anyMatch(idx -> "idx_orders_coupon_code".equals(idx.name())
                        && "coupon_code".equals(idx.columnList()));

        assertThat(hasCouponCodeIndex)
                .withFailMessage("Thực thể Order phải có index tên 'idx_orders_coupon_code' trên cột 'coupon_code'")
                .isTrue();
    }

    @Test
    @DisplayName("DOD-02: Thực thể Coupon phải có cấu hình @Index cho trường code")
    void testCouponEntityHasCodeIndex() {
        Table tableAnnotation = Coupon.class.getAnnotation(Table.class);
        assertThat(tableAnnotation).isNotNull();

        boolean hasCodeIndex = Arrays.stream(tableAnnotation.indexes())
                .anyMatch(idx -> "idx_coupons_code".equals(idx.name())
                        && "code".equals(idx.columnList()));

        assertThat(hasCodeIndex)
                .withFailMessage("Thực thể Coupon phải có index tên 'idx_coupons_code' trên cột 'code'")
                .isTrue();
    }

    @Test
    @DisplayName("DOD-03: Thực thể Review phải có cấu hình @Index cho trường product_id và composite (product_id, created_at)")
    void testReviewEntityHasProductIndexes() {
        Table tableAnnotation = Review.class.getAnnotation(Table.class);
        assertThat(tableAnnotation).isNotNull();

        boolean hasProductIndex = Arrays.stream(tableAnnotation.indexes())
                .anyMatch(idx -> "idx_reviews_product".equals(idx.name())
                        && "product_id".equals(idx.columnList()));

        boolean hasProductCreatedCompositeIndex = Arrays.stream(tableAnnotation.indexes())
                .anyMatch(idx -> "idx_reviews_product_created".equals(idx.name())
                        && "product_id, created_at".equals(idx.columnList()));

        assertThat(hasProductIndex)
                .withFailMessage("Thực thể Review phải có index 'idx_reviews_product' trên cột 'product_id'")
                .isTrue();

        assertThat(hasProductCreatedCompositeIndex)
                .withFailMessage("Thực thể Review phải có composite index 'idx_reviews_product_created' trên 'product_id, created_at'")
                .isTrue();
    }

    @Test
    @DisplayName("DOD-04: Thực thể Payment phải có cấu hình @Index cho trường vnp_txn_ref")
    void testPaymentEntityHasVnpTxnRefIndex() {
        Table tableAnnotation = Payment.class.getAnnotation(Table.class);
        assertThat(tableAnnotation).isNotNull();

        boolean hasVnpTxnRefIndex = Arrays.stream(tableAnnotation.indexes())
                .anyMatch(idx -> "idx_payments_vnp_txn_ref".equals(idx.name())
                        && "vnp_txn_ref".equals(idx.columnList()));

        assertThat(hasVnpTxnRefIndex)
                .withFailMessage("Thực thể Payment phải có index 'idx_payments_vnp_txn_ref' trên cột 'vnp_txn_ref'")
                .isTrue();
    }

    @Test
    @DisplayName("DOD-05: Tiêu chí nghiệm thu DoD - Tốc độ truy vấn API & Repository < 50ms")
    void testQueryLatencyMeetsDoDUnder50ms() {
        when(paymentRepository.findByVnpTxnRef(eq("PAY-20261111-0001")))
                .thenReturn(Optional.of(Payment.builder().vnpTxnRef("PAY-20261111-0001").build()));

        when(reviewRepository.findByProductId(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(orderRepository.findByCouponCode(eq("DISCOUNT10")))
                .thenReturn(Collections.emptyList());

        // Warm-up
        for (int i = 0; i < 5; i++) {
            paymentRepository.findByVnpTxnRef("PAY-20261111-0001");
            reviewRepository.findByProductId(1L, PageRequest.of(0, 10));
            orderRepository.findByCouponCode("DISCOUNT10");
        }

        // Đo độ trễ 100 lần truy vấn
        int iterations = 100;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            paymentRepository.findByVnpTxnRef("PAY-20261111-0001");
            reviewRepository.findByProductId(1L, PageRequest.of(0, 10));
            orderRepository.findByCouponCode("DISCOUNT10");
        }
        long totalElapsedNanos = System.nanoTime() - startTime;
        double averageLatencyMs = (totalElapsedNanos / (double) iterations) / 1_000_000.0;

        System.out.println(String.format("DoD Latency Benchmark: Average repository query time = %.4f ms", averageLatencyMs));

        // Tiêu chí nghiệm thu: Tốc độ query < 50ms
        assertThat(averageLatencyMs)
                .withFailMessage("Tốc độ query trung bình (%.2f ms) phải nhỏ hơn 50ms theo DoD", averageLatencyMs)
                .isLessThan(50.0);
    }
}
