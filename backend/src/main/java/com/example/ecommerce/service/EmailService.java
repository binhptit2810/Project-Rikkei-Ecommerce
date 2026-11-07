package com.example.ecommerce.service;

/**
 * Interface dịch vụ gửi email trong hệ thống.
 * Thiết kế trừu tượng độc lập với nhà cung cấp SMTP (Gmail, Mailtrap, AWS SES,...).
 *
 * Task ID: TSK-D10-CCT - Quên mật khẩu & Xác thực OTP qua Redis/Email
 */
public interface EmailService {

    /**
     * Gửi email chứa mã OTP xác thực đặt lại mật khẩu.
     *
     * @param toEmail địa chỉ email người nhận
     * @param otp     mã OTP 6 chữ số
     */
    void sendOtpEmail(String toEmail, String otp);

    /**
     * Gửi email chứa mã OTP xác thực địa chỉ email khi đăng ký tài khoản mới.
     *
     * @param toEmail  địa chỉ email người nhận
     * @param otp      mã OTP 6 chữ số
     * @param fullName họ tên người nhận
     */
    void sendVerificationOtpEmail(String toEmail, String otp, String fullName);
}

