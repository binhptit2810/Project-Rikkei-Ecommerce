package com.example.ecommerce.service;

import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.service.impl.EmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho EmailService (TSK-D10-CCT).
 * Kiểm tra việc tạo MimeMessage, gửi email OTP HTML và xử lý ngoại lệ SMTP an toàn.
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
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@rikkeiecommerce.com");
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    @DisplayName("EmailService: Gửi email OTP thành công với nội dung HTML")
    void sendOtpEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtpEmail("test@example.com", "123456");

        verify(mailSender).createMimeMessage();
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

