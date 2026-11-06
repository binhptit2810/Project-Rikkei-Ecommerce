package com.example.ecommerce.service.impl;

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

import java.nio.charset.StandardCharsets;

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

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("Bắt đầu chuẩn bị gửi email OTP đặt lại mật khẩu tới: {}", toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu đặt lại mật khẩu Rikkei Ecommerce");

            String htmlContent = buildOtpEmailTemplate(otp);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Gửi email OTP đặt lại mật khẩu thành công tới: {}", toEmail);
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Lỗi khi gửi email OTP tới {}: {}", toEmail, e.getMessage());
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi email tới {}: {}", toEmail, e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi gửi email xác thực, vui lòng thử lại sau");
        }
    }

    /**
     * Tạo nội dung HTML rõ ràng, hiện đại và chuẩn responsive cho email OTP.
     */
    private String buildOtpEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html lang=\"vi\">" +
                "<head><meta charset=\"UTF-8\"><title>Đặt lại mật khẩu</title></head>" +
                "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f7f9fa;\">" +
                "  <div style=\"max-width: 550px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">" +
                "    <h2 style=\"color: #1e88e5; text-align: center; margin-top: 0;\">Rikkei Ecommerce</h2>" +
                "    <hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;\">" +
                "    <p>Xin chào,</p>" +
                "    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Rikkei Ecommerce của bạn. Dưới đây là mã xác thực OTP của bạn:</p>" +
                "    <div style=\"text-align: center; margin: 30px 0;\">" +
                "      <span style=\"display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #d32f2f; background: #fff3e0; padding: 12px 24px; border-radius: 6px; border: 1px dashed #ffb74d;\">" +
                otp +
                "      </span>" +
                "    </div>" +
                "    <p style=\"margin-bottom: 10px;\">⏱️ <strong>Thời hạn hiệu lực:</strong> Mã xác thực có hiệu lực trong vòng <strong>5 phút</strong>.</p>" +
                "    <p style=\"color: #d32f2f; font-weight: bold;\">⚠️ Lưu ý bảo mật: Tuyệt đối không chia sẻ mã này với bất kỳ ai, bao gồm cả nhân viên hỗ trợ.</p>" +
                "    <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc thông báo cho bộ phận an ninh của chúng tôi.</p>" +
                "    <hr style=\"border: none; border-top: 1px solid #e0e0e0; margin: 25px 0 15px;\">" +
                "    <p style=\"font-size: 12px; color: #888; text-align: center; margin: 0;\">Email tự động từ hệ thống Rikkei Ecommerce. Vui lòng không trả lời thư này.</p>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}

