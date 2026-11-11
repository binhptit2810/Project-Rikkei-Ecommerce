package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateCouponRequest;
import com.example.ecommerce.dto.request.UpdateCouponRequest;
import com.example.ecommerce.dto.request.ValidateCouponRequest;
import com.example.ecommerce.dto.response.CouponResponse;
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

    // ==========================================
    // ADMIN CRUD UNIT TESTS
    // ==========================================

    @Test
    @DisplayName("Admin: Lấy danh sách toàn bộ voucher thành công")
    void getAllCouponsForAdmin_Success() {
        when(couponRepository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(percentCoupon, fixedCoupon));

        java.util.List<CouponResponse> results = couponService.getAllCouponsForAdmin();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCode()).isEqualTo("SALE10");
        assertThat(results.get(1).getCode()).isEqualTo("GIAM50K");
    }

    @Test
    @DisplayName("Admin: Lấy chi tiết voucher theo ID thành công")
    void getCouponById_Success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));

        CouponResponse response = couponService.getCouponById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("SALE10");
    }

    @Test
    @DisplayName("Admin: Lấy chi tiết voucher theo ID ném ResourceNotFoundException khi không tồn tại")
    void getCouponById_NotFound_ThrowsException() {
        when(couponRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    @Test
    @DisplayName("Admin: Tạo mới voucher thành công")
    void createCoupon_Success() {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("TET2026")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(25))
                .minOrderValue(BigDecimal.valueOf(500000))
                .maxDiscount(BigDecimal.valueOf(100000))
                .usageLimit(50)
                .expireAt(LocalDateTime.now().plusDays(20))
                .active(true)
                .build();

        when(couponRepository.existsByCodeIgnoreCase("TET2026")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> {
            Coupon c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CouponResponse response = couponService.createCoupon(request);

        assertThat(response.getCode()).isEqualTo("TET2026");
        assertThat(response.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(25));
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("Admin: Tạo mới voucher thất bại khi trùng mã code")
    void createCoupon_DuplicateCode_ThrowsException() {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("SALE10")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(15))
                .build();

        when(couponRepository.existsByCodeIgnoreCase("SALE10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Admin: Tạo mới voucher thất bại khi giảm giá % vượt quá 100")
    void createCoupon_PercentOver100_ThrowsException() {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("OVER100")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(150))
                .build();

        when(couponRepository.existsByCodeIgnoreCase("OVER100")).thenReturn(false);

        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được vượt quá 100%");
    }

    @Test
    @DisplayName("Admin: Cập nhật voucher thành công")
    void updateCoupon_Success() {
        UpdateCouponRequest request = UpdateCouponRequest.builder()
                .code("SALE20")
                .discountValue(BigDecimal.valueOf(20))
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.existsByCodeIgnoreCaseAndIdNot("SALE20", 1L)).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponResponse response = couponService.updateCoupon(1L, request);

        assertThat(response.getCode()).isEqualTo("SALE20");
        assertThat(response.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(20));
        verify(couponRepository).save(percentCoupon);
    }

    @Test
    @DisplayName("Admin: Xóa voucher thành công")
    void deleteCoupon_Success() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));
        doNothing().when(couponRepository).delete(percentCoupon);

        couponService.deleteCoupon(1L);

        verify(couponRepository).delete(percentCoupon);
    }

    @Test
    @DisplayName("Admin: Bật/tắt trạng thái kích hoạt voucher thành công")
    void toggleCouponStatus_Success() {
        percentCoupon.setActive(true);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponResponse response = couponService.toggleCouponStatus(1L);

        assertThat(response.getActive()).isFalse();
        verify(couponRepository).save(percentCoupon);
    }

    @Test
    @DisplayName("Admin: Toggle status ném ResourceNotFoundException khi ID không tồn tại")
    void toggleCouponStatus_NotFound_ThrowsException() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.toggleCouponStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Admin: Xóa voucher ném ResourceNotFoundException khi ID không tồn tại")
    void deleteCoupon_NotFound_ThrowsException() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.deleteCoupon(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Admin: Cập nhật voucher ném ResourceNotFoundException khi ID không tồn tại")
    void updateCoupon_NotFound_ThrowsException() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.updateCoupon(99L, new UpdateCouponRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Admin: Cập nhật voucher thất bại khi mã trùng với coupon khác")
    void updateCoupon_DuplicateCode_ThrowsException() {
        UpdateCouponRequest request = UpdateCouponRequest.builder().code("EXISTING").build();
        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.existsByCodeIgnoreCaseAndIdNot("EXISTING", 1L)).thenReturn(true);

        assertThatThrownBy(() -> couponService.updateCoupon(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được sử dụng");
    }

    @Test
    @DisplayName("Admin: Cập nhật voucher thất bại khi discountValue > 100% đối với PERCENT")
    void updateCoupon_PercentOver100_ThrowsException() {
        UpdateCouponRequest request = UpdateCouponRequest.builder()
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(120))
                .build();
        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.updateCoupon(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được vượt quá 100%");
    }

    @Test
    @DisplayName("Admin: Cập nhật voucher với đầy đủ các trường tùy chọn")
    void updateCoupon_AllOptionalFields() {
        UpdateCouponRequest request = UpdateCouponRequest.builder()
                .code("NEWALL")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(30000))
                .minOrderValue(BigDecimal.valueOf(100000))
                .maxDiscount(BigDecimal.valueOf(30000))
                .usageLimit(200)
                .expireAt(LocalDateTime.now().plusDays(10))
                .active(true)
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.existsByCodeIgnoreCaseAndIdNot("NEWALL", 1L)).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponResponse resp = couponService.updateCoupon(1L, request);

        assertThat(resp.getCode()).isEqualTo("NEWALL");
        assertThat(resp.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(resp.getDiscountValue()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(resp.getMinOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(resp.getMaxDiscount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(resp.getUsageLimit()).isEqualTo(200);
        assertThat(resp.getActive()).isTrue();
    }

    @Test
    @DisplayName("Lấy danh sách mã giảm giá đang hoạt động thành công")
    void getActiveCoupons_Success() {
        when(couponRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(java.util.List.of(percentCoupon, fixedCoupon));

        java.util.List<Coupon> active = couponService.getActiveCoupons();

        assertThat(active).hasSize(2);
        verify(couponRepository).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Lấy thông tin coupon theo mã thành công")
    void getCouponByCode_Success() {
        when(couponRepository.findByCodeIgnoreCase("SALE10")).thenReturn(Optional.of(percentCoupon));

        Coupon c = couponService.getCouponByCode("SALE10");

        assertThat(c.getCode()).isEqualTo("SALE10");
    }

    @Test
    @DisplayName("Lấy coupon theo mã rỗng ném BusinessException")
    void getCouponByCode_Blank_ThrowsException() {
        assertThatThrownBy(() -> couponService.getCouponByCode("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("Lấy coupon theo mã không tồn tại ném ResourceNotFoundException")
    void getCouponByCode_NotFound_ThrowsException() {
        when(couponRepository.findByCodeIgnoreCase("NOTFOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponByCode("NOTFOUND"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("applyCouponForOrder: Mã rỗng hoặc null trả về 0")
    void applyCouponForOrder_NullOrEmptyCode_ReturnsZero() {
        assertThat(couponService.applyCouponForOrder(null, BigDecimal.valueOf(500000)))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(couponService.applyCouponForOrder("   ", BigDecimal.valueOf(500000)))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("applyCouponForOrder: Áp dụng thành công tăng usedCount và trả về discount")
    void applyCouponForOrder_Success() {
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal discount = couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(300000));

        assertThat(discount).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(percentCoupon.getUsedCount()).isEqualTo(11);
        verify(couponRepository).save(percentCoupon);
    }

    @Test
    @DisplayName("applyCouponForOrder: Ném ResourceNotFoundException khi mã không tồn tại")
    void applyCouponForOrder_NotFound_ThrowsException() {
        when(couponRepository.findByCodeForUpdate("NOTFOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.applyCouponForOrder("NOTFOUND", BigDecimal.valueOf(300000)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("applyCouponForOrder: Ném BusinessException khi coupon không active")
    void applyCouponForOrder_Inactive_ThrowsException() {
        percentCoupon.setActive(false);
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(300000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tạm ngưng áp dụng");
    }

    @Test
    @DisplayName("applyCouponForOrder: Ném BusinessException khi coupon hết hạn")
    void applyCouponForOrder_Expired_ThrowsException() {
        percentCoupon.setExpireAt(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(300000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết hạn sử dụng");
    }

    @Test
    @DisplayName("applyCouponForOrder: Ném BusinessException khi coupon hết lượt sử dụng")
    void applyCouponForOrder_LimitReached_ThrowsException() {
        percentCoupon.setUsedCount(100);
        percentCoupon.setUsageLimit(100);
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(300000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết số lượt sử dụng");
    }

    @Test
    @DisplayName("applyCouponForOrder: Ném BusinessException khi đơn hàng chưa đạt giá trị tối thiểu")
    void applyCouponForOrder_BelowMinOrder_ThrowsException() {
        when(couponRepository.findByCodeForUpdate("SALE10")).thenReturn(Optional.of(percentCoupon));

        assertThatThrownBy(() -> couponService.applyCouponForOrder("SALE10", BigDecimal.valueOf(100000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa đạt giá trị tối thiểu");
    }

    @Test
    @DisplayName("calculateDiscount: Các trường hợp đặc biệt và giới hạn giá trị")
    void calculateDiscount_SpecialCases() {
        assertThat(couponService.calculateDiscount(null, BigDecimal.valueOf(100000)))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(couponService.calculateDiscount(percentCoupon, null))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(couponService.calculateDiscount(percentCoupon, BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(couponService.calculateDiscount(percentCoupon, BigDecimal.valueOf(-50000)))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Fixed coupon vượt quá giá trị đơn hàng thì chỉ giảm tối đa bằng giá trị đơn hàng
        fixedCoupon.setDiscountValue(BigDecimal.valueOf(200000));
        BigDecimal disc = couponService.calculateDiscount(fixedCoupon, BigDecimal.valueOf(150000));
        assertThat(disc).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }
}

