package com.example.ecommerce.service;

import com.example.ecommerce.entity.Order;

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

    /**
     * Gửi email xác nhận người dùng vừa đặt hàng thành công.
     *
     * @param toEmail       địa chỉ email người nhận
     * @param recipientName tên người nhận hàng
     * @param order         thông tin chi tiết đơn hàng
     */
    void sendOrderConfirmationEmail(String toEmail, String recipientName, Order order);

    /**
     * Gửi email thông báo cập nhật hoặc hủy trạng thái đơn hàng.
     *
     * @param toEmail       địa chỉ email người nhận
     * @param recipientName tên người nhận
     * @param orderCode     mã đơn hàng
     * @param oldStatus     trạng thái cũ
     * @param newStatus     trạng thái mới
     * @param note          ghi chú hoặc lý do
     */
    void sendOrderStatusChangedEmail(String toEmail, String recipientName, String orderCode, String oldStatus, String newStatus, String note);

    /**
     * Gửi email thông báo bảo mật khi có thay đổi quan trọng trên tài khoản (đổi mật khẩu, cập nhật hồ sơ).
     *
     * @param toEmail       địa chỉ email người nhận
     * @param fullName      họ tên người nhận
     * @param actionTitle   tiêu đề hành động (Đổi mật khẩu, Cập nhật thông tin)
     * @param detailMessage nội dung chi tiết
     */
    void sendSecurityAlertEmail(String toEmail, String fullName, String actionTitle, String detailMessage);

    /**
     * Gửi email thông báo trạng thái tài khoản từ Quản trị viên (Khóa, Mở khóa, Xóa tài khoản).
     *
     * @param toEmail       địa chỉ email người nhận
     * @param fullName      họ tên người nhận
     * @param statusTitle   tiêu đề trạng thái
     * @param detailMessage nội dung thông báo
     * @param reason        lý do (nếu có)
     */
    void sendAccountStatusEmail(String toEmail, String fullName, String statusTitle, String detailMessage, String reason);
}

