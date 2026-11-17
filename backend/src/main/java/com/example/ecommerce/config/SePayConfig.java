package com.example.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cấu hình Cổng thanh toán VietQR SePay.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 */
@Configuration
@ConfigurationProperties(prefix = "sepay")
@Getter
@Setter
public class SePayConfig {

    private String bankCode = "MBBank";
    private String accountNumber = "0372987662";
    private String accountName = "DANG THANH BINH";
    private String apiKey = "rikkei_sepay_secret_2026";
    private String qrTemplate = "compact2";

    /**
     * Tạo URL ảnh VietQR SePay động theo số tiền và mã đơn hàng.
     * Cú pháp SePay: https://qr.sepay.vn/img?acc={STK}&bank={BANK}&amount={TIEN}&des={NOI_DUNG}&template={TEMPLATE}
     */
    public String generateQrUrl(BigDecimal amount, String orderCode) {
        long amountLong = amount != null ? amount.longValue() : 0L;
        String memo = (orderCode != null && !orderCode.isBlank()) ? orderCode.trim() : "";
        String encodedMemo = URLEncoder.encode(memo, StandardCharsets.UTF_8);
        return String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=%s",
                this.accountNumber,
                this.bankCode,
                amountLong,
                encodedMemo,
                this.qrTemplate
        );
    }
}
