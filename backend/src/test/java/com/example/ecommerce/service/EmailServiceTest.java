package com.example.ecommerce.service;

import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.service.impl.EmailServiceImpl;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho EmailService (TSK-D10-CCT).
 * Kiểm tra việc tạo MimeMessage, gửi email OTP HTML và xử lý ngoại lệ SMTP an toàn (Resend SMTP).
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "onboarding@resend.dev");
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("EmailService: Gửi email OTP thành công, xác minh From address, To address, Subject và nội dung HTML chứa OTP")
    void sendOtpEmail_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtpEmail("customer@example.com", "987654");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);

        // Kiểm tra địa chỉ From và To
        assertThat(mimeMessage.getFrom()).isNotNull();
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("onboarding@resend.dev");
        assertThat(mimeMessage.getAllRecipients()).isNotNull();
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("customer@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Yêu cầu đặt lại mật khẩu Rikkei Ecommerce");

        // Kiểm tra nội dung email chứa OTP
        String bodyText = extractTextFromMessage(mimeMessage.getContent());
        assertThat(bodyText).contains("987654");
        assertThat(bodyText).contains("Rikkei Ecommerce");
    }

    private String extractTextFromMessage(Object content) throws Exception {
        if (content instanceof String str) {
            return str;
        } else if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                sb.append(extractTextFromMessage(bodyPart.getContent()));
            }
            return sb.toString();
        }
        return "";
    }

    @Test
    @DisplayName("EmailService: Bắt lỗi MailAuthenticationException (sai Resend API Key) và ném BusinessException an toàn")
    void sendOtpEmail_SmtpAuthenticationFailure_ThrowsBusinessException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("Resend API Key authentication failed"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendOtpEmail("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("EmailService: Bắt lỗi SMTP Connection Timeout (5000ms) và ném BusinessException an toàn")
    void sendOtpEmail_SmtpConnectionTimeout_ThrowsBusinessException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection timed out after 5000ms"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendOtpEmail("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("EmailService: Bắt lỗi MailSendException khi SMTP lỗi và ném BusinessException an toàn")
    void sendOtpEmail_SmtpFailure_ThrowsBusinessException() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendOtpEmail("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể gửi email xác thực lúc này");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("EmailService: Bắt lỗi ngoại lệ chung và ném BusinessException an toàn không lộ thông tin")
    void sendOtpEmail_GenericException_ThrowsBusinessException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server unexpected crash"));

        assertThatThrownBy(() -> emailService.sendOtpEmail("test@example.com", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lỗi hệ thống khi gửi email xác thực");
    }
}

