package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ValidateCouponRequest;
import com.example.ecommerce.dto.response.CouponValidationResponse;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test kiểm thử logic validate và tính toán giảm giá của CouponService.
 * Đảm bảo tính toán chính xác tuyệt đối ở server và bảo vệ chống gian lận.
 *
 * Người phụ trách: Vũ Đức Huy Hoàng
 * Người phối hợp: Đặng Thanh Bình
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        percentCoupon = Coupon.builder()
                .id(1L)
                .code("SALE10")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10)) // 10%
                .minOrderValue(BigDecimal.valueOf(200000)) // Đơn tối thiểu 200.000 đ
                .maxDiscount(BigDecimal.valueOf(50000)) // Giảm tối đa 50.000 đ
                .usageLimit(100)
                .usedCount(10)
                .expireAt(LocalDateTime.now().plusDays(7))
                .active(true)
                .build();

        fixedCoupon = Coupon.builder()
                .id(2L)
                .code("GIAM50K")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(50000)) // Giảm 50.000 đ
                .minOrderValue(BigDecimal.valueOf(150000)) // Đơn tối thiểu 150.000 đ
                .usageLimit(50)
                .usedCount(5)
                .expireAt(LocalDateTime.now().plusDays(3))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Validate thành công mã giảm theo PERCENT: Đơn 300.000đ giảm 10% = 30.000đ")
    void validateCoupon_PercentType_CalculatesDiscountCorrectly() {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("sale10") // Test không phân biệt hoa thường
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SALE10");
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(response.getOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(270000));
        assertThat(response.isValid()).isTrue();
    }

    @Test
    @DisplayName("Validate thành công mã PERCENT có giới hạn trần: Đơn 1.000.000đ giảm 10% bị giới hạn ở mức trần maxDiscount 50.000đ")
    void validateCoupon_PercentType_CapsAtMaxDiscount() {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(1000000)) // 10% của 1.000.000 = 100.000đ -> vượt trần 50.000đ
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertThat(response).isNotNull();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(950000));
        assertThat(response.isValid()).isTrue();
    }

    @Test
    @DisplayName("Validate thành công mã giảm FIXED: Đơn 300.000đ giảm cố định 50.000đ = 250.000đ")
    void validateCoupon_FixedType_CalculatesDiscountCorrectly() {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("GIAM50K")
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("GIAM50K")).thenReturn(Optional.of(fixedCoupon));

        CouponValidationResponse response = couponService.validateCoupon(request);

        assertThat(response).isNotNull();
        assertThat(response.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(response.isValid()).isTrue();
    }

    @Test
    @DisplayName("Bảo vệ chống số tiền giảm vượt quá giá trị đơn hàng: Giảm tối đa bằng đúng giá trị đơn, finalAmount không bao giờ âm")
    void calculateDiscount_CapsAtOrderValue() {
        Coupon bigFixedCoupon = Coupon.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(100000)) // Giảm 100k
                .build();

        BigDecimal orderValue = BigDecimal.valueOf(40000); // Đơn chỉ có 40k

        BigDecimal discount = couponService.calculateDiscount(bigFixedCoupon, orderValue);

        // Số tiền giảm tối đa chỉ là 40k
        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(40000));
    }

    @Test
    @DisplayName("Thất bại khi mã giảm giá không tồn tại trong DB")
    void validateCoupon_ThrowsException_WhenCodeNotFound() {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("NOTEXIST")
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("Thất bại khi mã giảm giá bị vô hiệu hóa (active = false)")
    void validateCoupon_ThrowsException_WhenInactive() {
        percentCoupon.setActive(false);

        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tạm ngưng áp dụng");
    }

    @Test
    @DisplayName("Thất bại khi mã giảm giá đã hết hạn sử dụng (expireAt trong quá khứ)")
    void validateCoupon_ThrowsException_WhenExpired() {
        percentCoupon.setExpireAt(LocalDateTime.now().minusDays(1)); // Đã hết hạn hôm qua

        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã hết hạn sử dụng");
    }

    @Test
    @DisplayName("Thất bại khi mã giảm giá đã đạt số lần sử dụng tối đa (usedCount >= usageLimit)")
    void validateCoupon_ThrowsException_WhenUsageLimitReached() {
        percentCoupon.setUsageLimit(50);
        percentCoupon.setUsedCount(50); // Đã dùng hết 50/50

        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(300000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã hết số lượt sử dụng");
    }

    @Test
    @DisplayName("Thất bại khi đơn hàng chưa đạt giá trị tối thiểu minOrderValue")
    void validateCoupon_ThrowsException_WhenOrderValueBelowMinimum() {
        // minOrderValue = 200.000đ, nhưng đơn hàng chỉ có 150.000đ
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(150000))
                .build();

        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.validateCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa đạt giá trị tối thiểu");
    }

    @Test
    @DisplayName("Áp dụng mã giảm giá khi tạo đơn: Khóa bi quan và tăng usedCount lên 1")
    void applyCouponForOrder_IncrementsUsedCount() {
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal discount = couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(300000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(percentCoupon.getUsedCount()).isEqualTo(11); // Ban đầu 10 -> lên 11
        verify(couponRepository).save(percentCoupon);
    }
}

