package com.example.ecommerce.service.impl;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Triển khai dịch vụ gửi email sử dụng JavaMailSender và SMTP.
 * Độc lập với nhà cung cấp SMTP cụ thể (hỗ trợ Gmail, Mailtrap,... qua cấu hình).
 *
 * Task ID: TSK-D10-CCT - Quên mật khẩu & Xác thực OTP qua Redis/Email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:no-reply@rikkeiecommerce.com}}")
    private String fromEmail;

    @Value("${app.mail.from-name:}")
    private String fromName;

    @Value("${app.mail.dev-fallback-enabled:true}")
    private boolean devFallbackEnabled = false;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private void configureSender(MimeMessageHelper helper) throws MessagingException, UnsupportedEncodingException {
        if (StringUtils.hasText(fromName)) {
            helper.setFrom(fromEmail, fromName);
        } else {
            helper.setFrom(fromEmail);
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        sendHtmlEmail(toEmail, subject, htmlContent, null);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent, String otpForFallback) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            configureSender(helper);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Gửi email thành công tới: {} | Tiêu đề: {}", toEmail, subject);
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Lỗi khi gửi email tới {}: {}", toEmail, e.getMessage());
            if (devFallbackEnabled) {
                if (StringUtils.hasText(otpForFallback)) {
                    log.warn("""
                            
                            ====================================================================================================
                            [DEV/STAGING EMAIL OTP FALLBACK] GỬI EMAIL QUA SMTP GẶP SỰ CỐ / GIỚI HẠN GMAIL (550 / NETWORK):
                            >> Chi tiết lỗi SMTP: {}
                            >> Địa chỉ nhận: {}
                            >> Tiêu đề: {}
                            >> MÃ XÁC THỰC OTP: [ {} ] (Hiệu lực: 5 phút)
                            Hệ thống đã giữ nguyên mã xác thực OTP trong Redis để bạn và các thành viên kiểm thử bình thường!
                            ====================================================================================================""",
                            e.getMessage(), toEmail, subject, otpForFallback);
                } else {
                    log.warn("""
                            
                            ====================================================================================================
                            [DEV/STAGING EMAIL NOTIFICATION FALLBACK] KHÔNG THỂ GỬI EMAIL THÔNG BÁO TỚI {}:
                            >> Chi tiết lỗi SMTP: {}
                            >> Tiêu đề: {}
                            Bỏ qua lỗi gửi email thông báo để không làm gián đoạn nghiệp vụ chính của hệ thống.
                            ====================================================================================================""",
                            toEmail, e.getMessage(), subject);
                }
                return;
            }
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi email tới {}: {}", toEmail, e.getMessage());
            if (devFallbackEnabled) {
                if (StringUtils.hasText(otpForFallback)) {
                    log.warn("""
                            
                            ====================================================================================================
                            [DEV/STAGING EMAIL OTP FALLBACK] LỖI HỆ THỐNG GỬI EMAIL:
                            >> Chi tiết lỗi: {}
                            >> Địa chỉ nhận: {}
                            >> Tiêu đề: {}
                            >> MÃ XÁC THỰC OTP: [ {} ]
                            Hệ thống đã giữ nguyên mã xác thực OTP trong Redis để bạn và các thành viên kiểm thử bình thường!
                            ====================================================================================================""",
                            e.getMessage(), toEmail, subject, otpForFallback);
                } else {
                    log.warn("""
                            
                            ====================================================================================================
                            [DEV/STAGING EMAIL NOTIFICATION FALLBACK] LỖI HỆ THỐNG GỬI EMAIL TỚI {}:
                            >> Chi tiết lỗi: {}
                            >> Tiêu đề: {}
                            Bỏ qua lỗi để không làm gián đoạn nghiệp vụ.
                            ====================================================================================================""",
                            toEmail, e.getMessage(), subject);
                }
                return;
            }
            throw new BusinessException("Lỗi hệ thống khi gửi email xác thực, vui lòng thử lại sau");
        }
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("Bắt đầu chuẩn bị gửi email OTP đặt lại mật khẩu tới: {}", toEmail);
        String subject = "Yêu cầu đặt lại mật khẩu Rikkei Ecommerce";
        String htmlContent = buildOtpEmailTemplate(otp);
        sendHtmlEmail(toEmail, subject, htmlContent, otp);
        if (devFallbackEnabled) {
            log.info("[DEV INFO] Mã OTP đặt lại mật khẩu gửi tới {}: [{}]", toEmail, otp);
        }
    }

    @Override
    public void sendVerificationOtpEmail(String toEmail, String otp, String fullName) {
        log.info("Bắt đầu chuẩn bị gửi email OTP xác thực tài khoản tới: {}", toEmail);
        String subject = "Xác thực địa chỉ email - Rikkei Store";
        String htmlContent = buildVerificationEmailTemplate(otp, fullName);
        sendHtmlEmail(toEmail, subject, htmlContent, otp);
        if (devFallbackEnabled) {
            log.info("[DEV INFO] Mã OTP xác thực email gửi tới {}: [{}]", toEmail, otp);
        }
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, String recipientName, Order order) {
        log.info("Bắt đầu chuẩn bị gửi email xác nhận đặt hàng đơn #{} tới: {}", order.getOrderCode(), toEmail);
        String subject = "[Rikkei Store] Xác nhận đặt hàng thành công #" + order.getOrderCode();
        String htmlContent = buildOrderConfirmationEmailTemplate(recipientName, order);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendOrderStatusChangedEmail(String toEmail, String recipientName, String orderCode, String oldStatus, String newStatus, String note) {
        log.info("Bắt đầu chuẩn bị gửi email cập nhật trạng thái đơn #{} tới: {}", orderCode, toEmail);
        String subject = "[Rikkei Store] Cập nhật trạng thái đơn hàng #" + orderCode;
        String htmlContent = buildOrderStatusChangedEmailTemplate(recipientName, orderCode, oldStatus, newStatus, note);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendSecurityAlertEmail(String toEmail, String fullName, String actionTitle, String detailMessage) {
        log.info("Bắt đầu chuẩn bị gửi email cảnh báo bảo mật ({}) tới: {}", actionTitle, toEmail);
        String subject = "[Rikkei Store] Cảnh báo bảo mật: " + actionTitle;
        String htmlContent = buildSecurityAlertEmailTemplate(fullName, actionTitle, detailMessage);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendAccountStatusEmail(String toEmail, String fullName, String statusTitle, String detailMessage, String reason) {
        log.info("Bắt đầu chuẩn bị gửi email thông báo trạng thái tài khoản ({}) tới: {}", statusTitle, toEmail);
        String subject = "[Rikkei Store] Thông báo tài khoản: " + statusTitle;
        String htmlContent = buildAccountStatusEmailTemplate(fullName, statusTitle, detailMessage, reason);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Tạo nội dung HTML email xác thực tài khoản mới chuẩn giao diện chuyên nghiệp, responsive.
     */
    private String buildVerificationEmailTemplate(String otp, String fullName) {
        String greetingName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "Quý khách";
        return "<!DOCTYPE html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Xác thực tài khoản Rikkei Store</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr>" +
                "      <td align=\"center\">" +
                "        <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01); border: 1px solid #e2e8f0;\">" +
                "          <!-- Header Banner -->" +
                "          <tr>" +
                "            <td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 32px 30px; text-align: center;\">" +
                "              <div style=\"display: inline-block; background: #2563eb; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);\">⚡</div>" +
                "              <h1 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;\">RIKKEI STORE</h1>" +
                "              <p style=\"margin: 6px 0 0 0; color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing: 2px; font-weight: 600;\">Official Technology E-Commerce Store</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body Content -->" +
                "          <tr>" +
                "            <td style=\"padding: 36px 32px 28px 32px;\">" +
                "              <h2 style=\"margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 700;\">Xin chào " + greetingName + ",</h2>" +
                "              <p style=\"margin: 0 0 16px 0; color: #475569; font-size: 14px; line-height: 1.7;\">" +
                "                Chào mừng bạn đến với <strong>Rikkei Store</strong> — Nền tảng mua sắm thiết bị công nghệ chính hãng hàng đầu. Cảm ơn bạn đã tin tưởng và đăng ký tài khoản thành viên cùng chúng tôi." +
                "              </p>" +
                "              <p style=\"margin: 0 0 24px 0; color: #475569; font-size: 14px; line-height: 1.7;\">" +
                "                Để hoàn tất việc kích hoạt tài khoản và bảo vệ quyền riêng tư mua sắm của bạn, vui lòng nhập mã xác thực OTP (One-Time Password) sau vào màn hình xác thực:" +
                "              </p>" +
                "              <!-- OTP Box -->" +
                "              <div style=\"background: #eff6ff; border: 1.5px dashed #93c5fd; border-radius: 14px; padding: 24px 20px; text-align: center; margin: 28px 0;\">" +
                "                <span style=\"display: block; font-size: 11px; font-weight: 700; color: #2563eb; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;\">MÃ XÁC THỰC CỦA BẠN (OTP)</span>" +
                "                <div style=\"font-family: 'JetBrains Mono', Consolas, 'SF Mono', Monaco, monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #1d4ed8; margin: 6px 0; text-shadow: 0 1px 2px rgba(29, 78, 216, 0.1);\">" +
                otp +
                "                </div>" +
                "                <span style=\"display: inline-block; font-size: 12px; font-weight: 600; color: #d97706; background: #fef3c7; padding: 4px 12px; border-radius: 20px; margin-top: 8px;\">⏱️ Hiệu lực trong vòng 5 phút</span>" +
                "              </div>" +
                "              <!-- Notice Box -->" +
                "              <div style=\"background: #f8fafc; border-left: 4px solid #3b82f6; padding: 14px 16px; border-radius: 0 8px 8px 0; margin-bottom: 24px;\">" +
                "                <p style=\"margin: 0 0 8px 0; font-size: 13px; font-weight: 700; color: #0f172a;\">Lưu ý quan trọng về an toàn:</p>" +
                "                <ul style=\"margin: 0; padding-left: 18px; color: #475569; font-size: 12px; line-height: 1.6;\">" +
                "                  <li><strong>Tuyệt đối bảo mật:</strong> Không chia sẻ mã OTP này cho bất kỳ ai, kể cả nhân viên tự xưng là tổng đài hỗ trợ Rikkei Store.</li>" +
                "                  <li>Mã xác thực chỉ có thể sử dụng <strong>01 lần duy nhất</strong>.</li>" +
                "                  <li>Nếu bạn không thực hiện đăng ký tài khoản tại Rikkei Store, hãy an tâm bỏ qua email này. Tài khoản sẽ không được kích hoạt nếu không có mã OTP.</li>" +
                "                </ul>" +
                "              </div>" +
                "              <p style=\"margin: 0; color: #475569; font-size: 13px; line-height: 1.6;\">" +
                "                Nếu bạn cần hỗ trợ thêm, đội ngũ chăm sóc khách hàng của chúng tôi luôn sẵn sàng phục vụ bạn qua hotline <strong>1900 6868</strong> hoặc email <strong>support@rikkeistore.vn</strong>." +
                "              </p>" +
                "              <p style=\"margin: 18px 0 0 0; color: #0f172a; font-size: 13px; font-weight: 600;\">" +
                "                Trân trọng,<br>" +
                "                <span style=\"color: #2563eb;\">Đội ngũ Rikkei Store</span>" +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 30px; text-align: center; color: #94a3b8; font-size: 11px; line-height: 1.6;\">" +
                "              <p style=\"margin: 0 0 6px 0; font-weight: 600; color: #64748b;\">© 2026 Rikkei Store. Bản quyền thuộc về Rikkei E-Commerce.</p>" +
                "              <p style=\"margin: 0 0 6px 0;\">Địa chỉ: Tòa nhà Sông Đà, Đường Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội</p>" +
                "              <p style=\"margin: 0; font-style: italic;\">Đây là email tự động từ hệ thống Rikkei Store. Vui lòng không trả lời trực tiếp email này.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tạo nội dung HTML email đặt lại mật khẩu chuyên nghiệp, cảnh báo bảo mật.
     */
    private String buildOtpEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Đặt lại mật khẩu Rikkei Store</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr>" +
                "      <td align=\"center\">" +
                "        <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01); border: 1px solid #e2e8f0;\">" +
                "          <!-- Header Banner -->" +
                "          <tr>" +
                "            <td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 32px 30px; text-align: center;\">" +
                "              <div style=\"display: inline-block; background: #2563eb; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);\">⚡</div>" +
                "              <h1 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;\">RIKKEI STORE</h1>" +
                "              <p style=\"margin: 6px 0 0 0; color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing: 2px; font-weight: 600;\">Trung tâm bảo mật tài khoản</p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Body Content -->" +
                "          <tr>" +
                "            <td style=\"padding: 36px 32px 28px 32px;\">" +
                "              <h2 style=\"margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 700;\">Xin chào Quý khách,</h2>" +
                "              <p style=\"margin: 0 0 16px 0; color: #475569; font-size: 14px; line-height: 1.7;\">" +
                "                Hệ thống <strong>Rikkei Store (Rikkei Ecommerce)</strong> vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với địa chỉ email này." +
                "              </p>" +
                "              <p style=\"margin: 0 0 24px 0; color: #475569; font-size: 14px; line-height: 1.7;\">" +
                "                Để tạo mật khẩu mới và bảo vệ an toàn cho tài khoản, vui lòng sử dụng mã xác thực OTP sau đây:" +
                "              </p>" +
                "              <!-- OTP Box -->" +
                "              <div style=\"background: #fff7ed; border: 1.5px dashed #fdba74; border-radius: 14px; padding: 24px 20px; text-align: center; margin: 28px 0;\">" +
                "                <span style=\"display: block; font-size: 11px; font-weight: 700; color: #c2410c; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;\">MÃ XÁC THỰC ĐẶT LẠI MẬT KHẨU</span>" +
                "                <div style=\"font-family: 'JetBrains Mono', Consolas, 'SF Mono', Monaco, monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ea580c; margin: 6px 0; text-shadow: 0 1px 2px rgba(234, 88, 12, 0.1);\">" +
                otp +
                "                </div>" +
                "                <span style=\"display: inline-block; font-size: 12px; font-weight: 600; color: #dc2626; background: #fee2e2; padding: 4px 12px; border-radius: 20px; margin-top: 8px;\">⏱️ Hiệu lực trong vòng 5 phút</span>" +
                "              </div>" +
                "              <!-- Warning Box -->" +
                "              <div style=\"background: #fef2f2; border-left: 4px solid #ef4444; padding: 14px 16px; border-radius: 0 8px 8px 0; margin-bottom: 24px;\">" +
                "                <p style=\"margin: 0 0 8px 0; font-size: 13px; font-weight: 700; color: #991b1b;\">Cảnh báo bảo mật:</p>" +
                "                <ul style=\"margin: 0; padding-left: 18px; color: #7f1d1d; font-size: 12px; line-height: 1.6;\">" +
                "                  <li><strong>Tuyệt đối không chia sẻ mã này:</strong> Rikkei Store không bao giờ yêu cầu bạn cung cấp mã OTP dưới bất kỳ hình thức nào.</li>" +
                "                  <li>Nếu bạn <strong>không yêu cầu</strong> đặt lại mật khẩu, có thể ai đó đang cố truy cập tài khoản của bạn. Vui lòng bỏ qua email này hoặc đăng nhập đổi mật khẩu ngay lập tức.</li>" +
                "                </ul>" +
                "              </div>" +
                "              <p style=\"margin: 0; color: #475569; font-size: 13px; line-height: 1.6;\">" +
                "                Mọi thắc mắc cần trợ giúp, xin liên hệ hotline: <strong>1900 6868</strong> hoặc email: <strong>support@rikkeistore.vn</strong>." +
                "              </p>" +
                "              <p style=\"margin: 18px 0 0 0; color: #0f172a; font-size: 13px; font-weight: 600;\">" +
                "                Trân trọng,<br>" +
                "                <span style=\"color: #2563eb;\">Đội ngũ Rikkei Store</span>" +
                "              </p>" +
                "            </td>" +
                "          </tr>" +
                "          <!-- Footer -->" +
                "          <tr>" +
                "            <td style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 30px; text-align: center; color: #94a3b8; font-size: 11px; line-height: 1.6;\">" +
                "              <p style=\"margin: 0 0 6px 0; font-weight: 600; color: #64748b;\">© 2026 Rikkei Store. Bản quyền thuộc về Rikkei E-Commerce.</p>" +
                "              <p style=\"margin: 0 0 6px 0;\">Địa chỉ: Tòa nhà Sông Đà, Đường Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội</p>" +
                "              <p style=\"margin: 0; font-style: italic;\">Đây là email tự động từ hệ thống Rikkei Store. Vui lòng không trả lời trực tiếp email này.</p>" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    /**
     * Tạo nội dung HTML thông báo xác nhận đặt hàng thành công.
     */
    private String buildOrderConfirmationEmailTemplate(String recipientName, Order order) {
        String greetingName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Quý khách";
        String createdAtStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMATTER) : "Vừa xong";
        String totalAmountStr = String.format("%,.0f đ", order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0);
        String paymentMethodStr = (order.getPayment() != null && order.getPayment().getPaymentMethod() != null)
                ? order.getPayment().getPaymentMethod().name() : "Thanh toán khi nhận hàng (COD)";

        StringBuilder itemsRows = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String itemPrice = String.format("%,.0f đ", item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                String itemSubtotal = String.format("%,.0f đ", item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0);
                itemsRows.append("<tr>")
                        .append("<td style=\"padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #1e293b;\"><strong>").append(item.getProductName()).append("</strong></td>")
                        .append("<td align=\"center\" style=\"padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #475569;\">").append(item.getQuantity()).append("</td>")
                        .append("<td align=\"right\" style=\"padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; color: #475569;\">").append(itemPrice).append("</td>")
                        .append("<td align=\"right\" style=\"padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 13px; font-weight: 700; color: #2563eb;\">").append(itemSubtotal).append("</td>")
                        .append("</tr>");
            }
        }

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><title>Xác nhận đơn hàng</title></head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr><td align=\"center\">" +
                "      <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;\">" +
                "        <tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 32px 30px; text-align: center;\">" +
                "          <div style=\"display: inline-block; background: #10b981; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px;\">✓</div>" +
                "          <h1 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 800;\">ĐẶT HÀNG THÀNH CÔNG</h1>" +
                "          <p style=\"margin: 6px 0 0 0; color: #94a3b8; font-size: 11px; text-transform: uppercase; letter-spacing: 2px;\">Rikkei Store Official Notification</p>" +
                "        </td></tr>" +
                "        <tr><td style=\"padding: 32px;\">" +
                "          <h2 style=\"margin: 0 0 12px 0; color: #0f172a; font-size: 18px;\">Xin chào " + greetingName + ",</h2>" +
                "          <p style=\"margin: 0 0 20px 0; color: #475569; font-size: 14px; line-height: 1.6;\">Cảm ơn bạn đã đặt hàng tại <strong>Rikkei Store</strong>! Đơn hàng của bạn đã được tiếp nhận và nhân viên của chúng tôi đang tiến hành đóng gói, xử lý.</p>" +
                "          <div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin-bottom: 24px;\">" +
                "            <table width=\"100%\" cellspacing=\"0\" cellpadding=\"4\" border=\"0\" style=\"font-size: 13px; color: #334155;\">" +
                "              <tr><td width=\"40%\"><strong>Mã đơn hàng:</strong></td><td style=\"font-weight: 700; color: #2563eb;\">#" + order.getOrderCode() + "</td></tr>" +
                "              <tr><td><strong>Thời gian đặt:</strong></td><td>" + createdAtStr + "</td></tr>" +
                "              <tr><td><strong>Người nhận:</strong></td><td>" + greetingName + " (" + order.getRecipientPhone() + ")</td></tr>" +
                "              <tr><td><strong>Địa chỉ giao hàng:</strong></td><td>" + order.getShippingAddress() + "</td></tr>" +
                "              <tr><td><strong>Phương thức:</strong></td><td>" + paymentMethodStr + "</td></tr>" +
                "            </table>" +
                "          </div>" +
                "          <h3 style=\"margin: 0 0 12px 0; color: #0f172a; font-size: 15px;\">Chi tiết sản phẩm</h3>" +
                "          <table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"margin-bottom: 20px; border-collapse: collapse;\">" +
                "            <thead><tr style=\"background: #f1f5f9; text-align: left;\">" +
                "              <th style=\"padding: 8px 12px; font-size: 12px; color: #475569;\">Sản phẩm</th>" +
                "              <th align=\"center\" style=\"padding: 8px 12px; font-size: 12px; color: #475569;\">SL</th>" +
                "              <th align=\"right\" style=\"padding: 8px 12px; font-size: 12px; color: #475569;\">Đơn giá</th>" +
                "              <th align=\"right\" style=\"padding: 8px 12px; font-size: 12px; color: #475569;\">Thành tiền</th>" +
                "            </tr></thead>" +
                "            <tbody>" + itemsRows + "</tbody>" +
                "          </table>" +
                "          <div style=\"background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 16px; text-align: right; margin-bottom: 24px;\">" +
                "            <span style=\"font-size: 13px; color: #1e3a8a; margin-right: 12px;\">Tổng giá trị thanh toán:</span>" +
                "            <span style=\"font-size: 20px; font-weight: 800; color: #1d4ed8;\">" + totalAmountStr + "</span>" +
                "          </div>" +
                "          <p style=\"margin: 0 0 6px 0; color: #64748b; font-size: 12px;\">Hotline hỗ trợ 24/7: <strong>1900 6868</strong> | Email: <strong>support@rikkeistore.vn</strong></p>" +
                "          <p style=\"margin: 0; color: #0f172a; font-size: 13px; font-weight: 600;\">Trân trọng,<br><span style=\"color: #2563eb;\">Rikkei Store Team</span></p>" +
                "        </td></tr>" +
                "        <tr><td style=\"background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 18px; text-align: center; color: #94a3b8; font-size: 11px;\">© 2026 Rikkei Store. Hệ thống thông báo mua hàng tự động.</td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body></html>";
    }

    /**
     * Tạo nội dung HTML thông báo cập nhật hoặc hủy trạng thái đơn hàng.
     */
    private String buildOrderStatusChangedEmailTemplate(String recipientName, String orderCode, String oldStatus, String newStatus, String note) {
        String greetingName = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Quý khách";
        String noteRow = StringUtils.hasText(note) ? "<tr><td><strong>Ghi chú / Lý do:</strong></td><td style=\"color: #b91c1c;\">" + note + "</td></tr>" : "";

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><title>Cập nhật trạng thái đơn hàng</title></head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr><td align=\"center\">" +
                "      <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;\">" +
                "        <tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 30px; text-align: center;\">" +
                "          <div style=\"display: inline-block; background: #3b82f6; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px;\">📦</div>" +
                "          <h1 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 800;\">CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG</h1>" +
                "          <p style=\"margin: 4px 0 0 0; color: #94a3b8; font-size: 11px;\">Mã đơn: #" + orderCode + "</p>" +
                "        </td></tr>" +
                "        <tr><td style=\"padding: 32px;\">" +
                "          <h2 style=\"margin: 0 0 12px 0; color: #0f172a; font-size: 18px;\">Xin chào " + greetingName + ",</h2>" +
                "          <p style=\"margin: 0 0 20px 0; color: #475569; font-size: 14px; line-height: 1.6;\">Đơn hàng <strong>#" + orderCode + "</strong> của bạn vừa được cập nhật tiến trình xử lý:</p>" +
                "          <div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin-bottom: 24px;\">" +
                "            <table width=\"100%\" cellspacing=\"0\" cellpadding=\"6\" border=\"0\" style=\"font-size: 13px; color: #334155;\">" +
                "              <tr><td width=\"35%\"><strong>Trạng thái trước:</strong></td><td><span style=\"color: #64748b;\">" + oldStatus + "</span></td></tr>" +
                "              <tr><td><strong>Trạng thái mới:</strong></td><td><span style=\"font-weight: 800; color: #2563eb;\">" + newStatus + "</span></td></tr>" +
                noteRow +
                "            </table>" +
                "          </div>" +
                "          <p style=\"margin: 0 0 6px 0; color: #64748b; font-size: 12px;\">Bạn có thể kiểm tra chi tiết đơn hàng bất kỳ lúc nào tại mục <strong>Lịch sử đơn hàng</strong> trên website.</p>" +
                "          <p style=\"margin: 16px 0 0 0; color: #0f172a; font-size: 13px; font-weight: 600;\">Trân trọng,<br><span style=\"color: #2563eb;\">Rikkei Store Team</span></p>" +
                "        </td></tr>" +
                "        <tr><td style=\"background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; color: #94a3b8; font-size: 11px;\">© 2026 Rikkei Store. Hệ thống thông báo tự động.</td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body></html>";
    }

    /**
     * Tạo nội dung HTML cảnh báo bảo mật khi người dùng thay đổi mật khẩu hoặc hồ sơ.
     */
    private String buildSecurityAlertEmailTemplate(String fullName, String actionTitle, String detailMessage) {
        String greetingName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "Quý khách";

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><title>Cảnh báo bảo mật</title></head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr><td align=\"center\">" +
                "      <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;\">" +
                "        <tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 30px; text-align: center;\">" +
                "          <div style=\"display: inline-block; background: #eab308; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px;\">🛡️</div>" +
                "          <h1 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 800;\">THÔNG BÁO BẢO MẬT TÀI KHOẢN</h1>" +
                "          <p style=\"margin: 4px 0 0 0; color: #94a3b8; font-size: 11px;\">" + actionTitle + "</p>" +
                "        </td></tr>" +
                "        <tr><td style=\"padding: 32px;\">" +
                "          <h2 style=\"margin: 0 0 12px 0; color: #0f172a; font-size: 18px;\">Xin chào " + greetingName + ",</h2>" +
                "          <p style=\"margin: 0 0 16px 0; color: #475569; font-size: 14px; line-height: 1.6;\">" + detailMessage + "</p>" +
                "          <div style=\"background: #fef2f2; border-left: 4px solid #ef4444; border-radius: 0 10px 10px 0; padding: 14px 16px; margin: 20px 0;\">" +
                "            <p style=\"margin: 0 0 6px 0; font-size: 13px; font-weight: 700; color: #991b1b;\">Bạn có thực hiện thao tác này không?</p>" +
                "            <p style=\"margin: 0; font-size: 12px; color: #7f1d1d; line-height: 1.5;\">Nếu bạn <strong>không thực hiện</strong> thay đổi này, hãy liên hệ ngay với Trung tâm CSKH Rikkei Store qua hotline <strong>1900 6868</strong> để được hỗ trợ khóa tài khoản và kiểm tra bảo mật khẩn cấp.</p>" +
                "          </div>" +
                "          <p style=\"margin: 20px 0 0 0; color: #0f172a; font-size: 13px; font-weight: 600;\">Trân trọng,<br><span style=\"color: #2563eb;\">Rikkei Store Security Center</span></p>" +
                "        </td></tr>" +
                "        <tr><td style=\"background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; color: #94a3b8; font-size: 11px;\">© 2026 Rikkei Store. Hệ thống thông báo bảo mật tự động.</td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body></html>";
    }

    /**
     * Tạo nội dung HTML thông báo trạng thái tài khoản từ quản trị viên (khóa, mở khóa, xóa tài khoản).
     */
    private String buildAccountStatusEmailTemplate(String fullName, String statusTitle, String detailMessage, String reason) {
        String greetingName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "Quý khách";
        String reasonBlock = StringUtils.hasText(reason)
                ? "<div style=\"background: #fff1f2; border: 1px solid #fecdd3; border-radius: 10px; padding: 12px 16px; margin: 16px 0; font-size: 13px; color: #9f1239;\"><strong>Lý do từ Quản trị viên:</strong> " + reason + "</div>"
                : "";

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><title>" + statusTitle + "</title></head>" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 30px 15px;\">" +
                "    <tr><td align=\"center\">" +
                "      <table role=\"presentation\" width=\"100%\" max-width=\"600\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;\">" +
                "        <tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 30px; text-align: center;\">" +
                "          <div style=\"display: inline-block; background: #dc2626; color: #ffffff; width: 44px; height: 44px; line-height: 44px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 12px;\">⚠️</div>" +
                "          <h1 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 800;\">THÔNG BÁO TỪ QUẢN TRỊ VIÊN</h1>" +
                "          <p style=\"margin: 4px 0 0 0; color: #94a3b8; font-size: 11px;\">" + statusTitle + "</p>" +
                "        </td></tr>" +
                "        <tr><td style=\"padding: 32px;\">" +
                "          <h2 style=\"margin: 0 0 12px 0; color: #0f172a; font-size: 18px;\">Xin chào " + greetingName + ",</h2>" +
                "          <p style=\"margin: 0 0 16px 0; color: #475569; font-size: 14px; line-height: 1.6;\">" + detailMessage + "</p>" +
                reasonBlock +
                "          <p style=\"margin: 16px 0 0 0; color: #475569; font-size: 13px; line-height: 1.6;\">Mọi thắc mắc cần khiếu nại hoặc hỗ trợ mở lại tài khoản, vui lòng liên hệ Ban quản trị qua hotline <strong>1900 6868</strong> hoặc gửi thư đến <strong>support@rikkeistore.vn</strong>.</p>" +
                "          <p style=\"margin: 20px 0 0 0; color: #0f172a; font-size: 13px; font-weight: 600;\">Trân trọng,<br><span style=\"color: #2563eb;\">Ban Quản Trị Rikkei Store</span></p>" +
                "        </td></tr>" +
                "        <tr><td style=\"background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; color: #94a3b8; font-size: 11px;\">© 2026 Rikkei Store. Thông báo quản trị hệ thống.</td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body></html>";
    }
}
