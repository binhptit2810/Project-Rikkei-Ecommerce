package com.example.ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Cấu hình tham số kết nối Cổng thanh toán VNPay Sandbox.
 * Phụ trách: Đặng Thanh Bình (DTB)
 * Phối hợp: Cao Chí Thiện (CCT)
 * Hardened: TSK-D13-CCT - Loại bỏ credential hardcoded trong code, bổ sung validate fail-fast trong môi trường Production.
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
@Slf4j
public class VNPayConfig {

    public static final String DEFAULT_SANDBOX_TMN_CODE = "2QXUI4J4";
    public static final String DEFAULT_SANDBOX_HASH_SECRET = "RAIQUIBGCARJHNLKZCPGUCUMRHQKGZDF";

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String tmnCode;
    private String hashSecret;
    private String returnUrl = "http://localhost:3000/orders/vnpay-return";
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String currCode = "VND";
    private String locale = "vn";

    @PostConstruct
    public void validateVNPayConfig() {
        boolean isProduction = activeProfile != null && (
                "docker".equalsIgnoreCase(activeProfile)
                        || "prod".equalsIgnoreCase(activeProfile)
                        || "production".equalsIgnoreCase(activeProfile)
        );

        if (!StringUtils.hasText(tmnCode) || !StringUtils.hasText(hashSecret)) {
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: Biến môi trường VNPAY_TMN_CODE và VNPAY_HASH_SECRET bắt buộc phải được cấu hình trên môi trường Production.");
            } else {
                log.warn("CẢNH BÁO BẢO MẬT: vnpay.tmn-code hoặc vnpay.hash-secret chưa được cấu hình qua environment variable. Sử dụng fallback từ file cấu hình dev/test.");
            }
        } else if (isProduction && (DEFAULT_SANDBOX_TMN_CODE.equals(tmnCode.trim()) || DEFAULT_SANDBOX_HASH_SECRET.equals(hashSecret.trim()))) {
            log.warn("CẢNH BÁO BẢO MẬT: Môi trường Production đang cấu hình TMN_CODE hoặc HASH_SECRET trùng với mã Sandbox mẫu của VNPay.");
        }
    }
}
