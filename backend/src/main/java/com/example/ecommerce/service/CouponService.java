package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ValidateCouponRequest;
import com.example.ecommerce.dto.response.CouponValidationResponse;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Service quản lý mã giảm giá (Coupon / Voucher Module).
 * Người phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Người phối hợp: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Tiêu chí nghiệm thu (DoD):
 * Logic validate và tính giảm giá voucher chính xác tuyệt đối; bảo vệ server-side chống gian lận.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(Locale.of("vi", "VN"));

    /**
     * Kiểm tra tính hợp lệ và điều kiện áp mã giảm giá khi checkout.
     * Toàn bộ giá trị chiết khấu và tổng tiền sau giảm được tính toán an toàn tại server.
     *
     * @param request Chứa mã code và giá trị đơn hàng (orderValue)
     * @return CouponValidationResponse thông tin chiết khấu chi tiết
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(ValidateCouponRequest request) {
        String code = request.getCode() != null ? request.getCode().trim().toUpperCase() : "";
        BigDecimal orderValue = request.getOrderValue();

        log.info("Bắt đầu validate mã giảm giá '{}' cho đơn hàng trị giá: {}", code, orderValue);

        if (code.isEmpty()) {
            throw new BusinessException("Mã giảm giá không được để trống");
        }

        if (orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá trị đơn hàng phải lớn hơn 0");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá '" + code + "' không tồn tại trong hệ thống"));

        // 1. Kiểm tra trạng thái hoạt động
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            log.warn("Mã giảm giá '{}' đang bị vô hiệu hóa", code);
            throw new BusinessException(String.format("Mã giảm giá '%s' hiện đang tạm ngưng áp dụng", coupon.getCode()));
        }

        // 2. Kiểm tra hạn sử dụng
        if (coupon.isExpired()) {
            String expireStr = coupon.getExpireAt().format(DATE_FORMATTER);
            log.warn("Mã giảm giá '{}' đã hết hạn vào {}", code, expireStr);
            throw new BusinessException(String.format("Mã giảm giá '%s' đã hết hạn sử dụng vào ngày %s",
                    coupon.getCode(), expireStr));
        }

        // 3. Kiểm tra số lần sử dụng tối đa
        if (coupon.isUsageLimitReached()) {
            log.warn("Mã giảm giá '{}' đã hết lượt sử dụng ({}/{})", code, coupon.getUsedCount(), coupon.getUsageLimit());
            throw new BusinessException(String.format("Mã giảm giá '%s' đã hết số lượt sử dụng", coupon.getCode()));
        }

        // 4. Kiểm tra giá trị đơn hàng tối thiểu
        if (coupon.getMinOrderValue() != null && orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            String minOrderFormatted = CURRENCY_FORMAT.format(coupon.getMinOrderValue()) + " đ";
            String currentOrderFormatted = CURRENCY_FORMAT.format(orderValue) + " đ";
            log.warn("Đơn hàng '{}' chưa đạt giá trị tối thiểu {} (hiện tại: {})", code, minOrderFormatted, currentOrderFormatted);
            throw new BusinessException(String.format("Đơn hàng chưa đạt giá trị tối thiểu %s để áp dụng mã '%s' (Đơn hiện tại: %s)",
                    minOrderFormatted, coupon.getCode(), currentOrderFormatted));
        }

        // 5. Tính toán số tiền giảm giá chính xác tuyệt đối ở server
        BigDecimal discountAmount = calculateDiscount(coupon, orderValue);
        BigDecimal finalAmount = orderValue.subtract(discountAmount).max(BigDecimal.ZERO);

        String message = buildSuccessMessage(coupon, discountAmount);

        log.info("Validate thành công mã '{}'! Giảm: {} đ, Giá cuối: {} đ", code, discountAmount, finalAmount);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountTypeDescription(coupon.getDiscountType().getDescription())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .orderValue(orderValue)
                .finalAmount(finalAmount)
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscount(coupon.getMaxDiscount())
                .expireAt(coupon.getExpireAt())
                .message(message)
                .valid(true)
                .build();
    }

    /**
     * Hàm tính toán số tiền giảm giá chính xác bảo vệ ở phía server (Server-side calculation).
     * Tuyệt đối không cho phép client tự gửi giá trị giảm lên hệ thống.
     *
     * Quy tắc:
     * 1. PERCENT:
     *    - discount = orderValue * (discountValue / 100)
     *    - Nếu có maxDiscount > 0: discount = min(discount, maxDiscount)
     * 2. FIXED:
     *    - discount = discountValue
     * 3. Ràng buộc an toàn:
     *    - discount không bao giờ vượt quá tổng giá trị đơn hàng (discount <= orderValue)
     *    - discount >= 0
     *
     * @param coupon Đối tượng Coupon từ Database
     * @param orderValue Giá trị đơn hàng tính từ DB
     * @return BigDecimal số tiền giảm chính xác (đã làm tròn 2 chữ số thập phân)
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderValue) {
        if (coupon == null || orderValue == null || orderValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            // Tính số tiền theo %: orderValue * (discountValue / 100)
            BigDecimal percentRate = coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = orderValue.multiply(percentRate).setScale(2, RoundingMode.HALF_UP);

            // Kiểm tra mức giảm tối đa (nếu có cấu hình max_discount)
            if (coupon.getMaxDiscount() != null && coupon.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                if (discount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discount = coupon.getMaxDiscount();
                }
            }
        } else if (coupon.getDiscountType() == DiscountType.FIXED) {
            // Giảm cố định một khoản tiền
            discount = coupon.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
        } else {
            discount = BigDecimal.ZERO;
        }

        // Ràng buộc bảo vệ tuyệt đối: Số tiền giảm không được vượt quá giá trị đơn hàng
        if (discount.compareTo(orderValue) > 0) {
            discount = orderValue;
        }

        // Ràng buộc số tiền giảm không được âm
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        return discount;
    }

    /**
     * Áp dụng mã giảm giá khi tạo đơn hàng (có khóa bi quan PESSIMISTIC_WRITE để chống race condition).
     * Tăng số lần sử dụng used_count lên 1.
     *
     * @param code Mã giảm giá
     * @param orderValue Tổng tiền hàng
     * @return Số tiền được giảm
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal applyCouponForOrder(String code, BigDecimal orderValue) {
        if (code == null || code.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String cleanCode = code.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeForUpdate(cleanCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá '" + cleanCode + "' không tồn tại"));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BusinessException(String.format("Mã giảm giá '%s' hiện đang tạm ngưng áp dụng", coupon.getCode()));
        }

        if (coupon.isExpired()) {
            throw new BusinessException(String.format("Mã giảm giá '%s' đã hết hạn sử dụng", coupon.getCode()));
        }

        if (coupon.isUsageLimitReached()) {
            throw new BusinessException(String.format("Mã giảm giá '%s' đã hết số lượt sử dụng", coupon.getCode()));
        }

        if (coupon.getMinOrderValue() != null && orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            String minOrderFormatted = CURRENCY_FORMAT.format(coupon.getMinOrderValue()) + " đ";
            throw new BusinessException(String.format("Đơn hàng chưa đạt giá trị tối thiểu %s để áp dụng mã '%s'",
                    minOrderFormatted, coupon.getCode()));
        }

        BigDecimal discount = calculateDiscount(coupon, orderValue);

        // Tăng số lượt sử dụng một cách an toàn
        int currentCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        coupon.setUsedCount(currentCount + 1);
        couponRepository.save(coupon);

        log.info("Áp dụng mã giảm giá '{}' thành công cho đơn hàng: giảm {} đ (lượt dùng: {}/{})",
                coupon.getCode(), discount, coupon.getUsedCount(), coupon.getUsageLimit());

        return discount;
    }

    /**
     * Lấy danh sách các mã giảm giá đang hoạt động (dành cho người dùng tham khảo).
     */
    @Transactional(readOnly = true)
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Tra cứu thông tin chi tiết một coupon theo mã.
     */
    @Transactional(readOnly = true)
    public Coupon getCouponByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("Mã giảm giá không được để trống");
        }
        return couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã giảm giá: " + code));
    }

    private String buildSuccessMessage(Coupon coupon, BigDecimal discountAmount) {
        String discountFormatted = CURRENCY_FORMAT.format(discountAmount) + " đ";
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) == 0) {
                return String.format("Áp dụng thành công mã %s: Giảm %s%% (tối đa %s)",
                        coupon.getCode(),
                        coupon.getDiscountValue().stripTrailingZeros().toPlainString(),
                        discountFormatted);
            }
            return String.format("Áp dụng thành công mã %s: Giảm %s%% (%s)",
                    coupon.getCode(),
                    coupon.getDiscountValue().stripTrailingZeros().toPlainString(),
                    discountFormatted);
        } else {
            return String.format("Áp dụng thành công mã %s: Giảm %s", coupon.getCode(), discountFormatted);
        }
    }
}

