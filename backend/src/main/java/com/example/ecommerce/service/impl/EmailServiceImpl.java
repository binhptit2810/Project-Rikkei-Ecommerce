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

    @Override
    public void sendVerificationOtpEmail(String toEmail, String otp, String fullName) {
        log.info("Bắt đầu chuẩn bị gửi email OTP xác thực tài khoản tới: {}", toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực địa chỉ email - Rikkei Store");

            String htmlContent = buildVerificationEmailTemplate(otp, fullName);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Gửi email OTP xác thực tài khoản thành công tới: {}", toEmail);
        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Lỗi khi gửi email xác thực tới {}: {}", toEmail, e.getMessage());
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi email xác thực tới {}: {}", toEmail, e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi gửi email xác thực, vui lòng thử lại sau");
        }
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
     * Tạo nội dung HTML rõ ràng, hiện đại và chuẩn responsive cho email OTP đặt lại mật khẩu.
     */
    private String buildOtpEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Yêu cầu đặt lại mật khẩu - Rikkei Store</title>" +
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
}

