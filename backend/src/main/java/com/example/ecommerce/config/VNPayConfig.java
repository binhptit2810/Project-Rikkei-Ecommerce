package com.example.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tham số kết nối Cổng thanh toán VNPay Sandbox.
 * Phụ trách: Đặng Thanh Bình (DTB)
 * Phối hợp: Cao Chí Thiện (CCT)
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayConfig {

    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String tmnCode = "2QXUI4J4";
    private String hashSecret = "RAIQUIBGCARJHNLKZCPGUCUMRHQKGZDF";
    private String returnUrl = "http://localhost:3000/orders/vnpay-return";
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String currCode = "VND";
    private String locale = "vn";
}

