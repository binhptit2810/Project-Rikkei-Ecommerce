package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ForgotPasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.SendRegistrationOtpRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.dto.request.ResendVerificationRequest;
import com.example.ecommerce.dto.request.VerifyEmailRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.RegisterResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.exception.UnverifiedAccountException;
import com.example.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho AuthController.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 * Kiểm tra xác thực (Authentication), Bean Validation,
 * Xử lý lỗi tập trung khi sai thông tin (401) hoặc tài khoản bị khóa (403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.example.ecommerce.service.RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("FR-AUTH-03: Đăng ký tài khoản với dữ liệu hợp lệ trả 201 CREATED kèm thông tin yêu cầu xác thực email")
    void register_ValidRequest_Returns201AndRegisterResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("levanc@example.com");
        request.setPassword("Password123@");
        request.setPhone("0912345678");

        RegisterResponse registerResponse = RegisterResponse.builder()
                .email("levanc@example.com")
                .fullName("Lê Văn C")
                .emailVerified(false)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(registerResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("levanc@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Lê Văn C"))
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("FR-AUTH-05: Xác thực email với mã OTP hợp lệ trả 200 OK và cấp JWT token")
    void verifyEmail_ValidOtp_Returns200AndTokens() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("user@example.com");
        request.setOtp("123456");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-verified-jwt")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.builder()
                        .id(1L)
                        .email("user@example.com")
                        .fullName("Nguyễn Văn A")
                        .role(Role.CUSTOMER)
                        .active(true)
                        .emailVerified(true)
                        .build())
                .build();

        when(authService.verifyEmail(any(VerifyEmailRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-verified-jwt"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));
    }

    @Test
    @DisplayName("NFR-DATA-15: Xác thực email với mã OTP không đủ 6 chữ số trả 400 BAD REQUEST")
    void verifyEmail_InvalidOtpFormat_Returns400() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("user@example.com");
        request.setOtp("12345"); // chỉ 5 chữ số

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.otp").value("Mã OTP phải gồm đúng 6 chữ số"));
    }

    @Test
    @DisplayName("FR-AUTH-06: Gửi lại mã OTP xác thực email trả 200 OK")
    void resendVerification_ValidEmail_Returns200() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("user@example.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("NFR-DATA-07: Đăng ký với email sai định dạng bị Bean Validation trả 400 BAD REQUEST")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("not-an-email");
        request.setPassword("Password123@");
        request.setPhone("0912345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").value("Email không đúng định dạng"));
    }

    @Test
    @DisplayName("NFR-DATA-08: Đăng ký với mật khẩu ngắn hơn 6 ký tự bị Bean Validation trả 400 BAD REQUEST")
    void register_ShortPassword_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("12345"); // ít hơn 6 ký tự
        request.setPhone("0912345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu phải từ 6 đến 100 ký tự"));
    }

    @Test
    @DisplayName("NFR-DATA-09: Đăng ký không có số điện thoại (null) bị Bean Validation trả 400 BAD REQUEST")
    void register_NullPhone_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");
        request.setPhone(null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").value("Số điện thoại không được để trống"));
    }

    @Test
    @DisplayName("NFR-DATA-10: Đăng ký với số điện thoại rỗng bị Bean Validation trả 400 BAD REQUEST")
    void register_EmptyPhone_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");
        request.setPhone("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone", containsString("Số điện thoại không được để trống")));
    }

    @Test
    @DisplayName("NFR-DATA-11: Đăng ký với số điện thoại chỉ chứa khoảng trắng bị Bean Validation trả 400 BAD REQUEST")
    void register_WhitespacePhone_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");
        request.setPhone("   ");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone", containsString("Số điện thoại không được để trống")));
    }

    @Test
    @DisplayName("NFR-DATA-12: Đăng ký với số điện thoại sai định dạng (không bắt đầu bằng 0) bị Bean Validation trả 400 BAD REQUEST")
    void register_InvalidFormatPhone_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");
        request.setPhone("1234567890"); // Không bắt đầu bằng 0

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").value("Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)"));
    }

    @Test
    @DisplayName("NFR-DATA-13: Đăng ký với số điện thoại không đủ 10 chữ số bị Bean Validation trả 400 BAD REQUEST")
    void register_ShortPhone_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");
        request.setPhone("091234"); // Không đủ 10 số

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phone").value("Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)"));
    }

    @Test
    @DisplayName("FR-AUTH-04: Đăng nhập thành công trả 200 OK kèm theo Bearer JWT")
    void login_ValidCredentials_Returns200AndToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-valid-login-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.builder()
                        .id(1L)
                        .email("valid@example.com")
                        .role(Role.CUSTOMER)
                        .active(true)
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-valid-login-token"));
    }

    @Test
    @DisplayName("NFR-SEC-29: Đăng nhập với tài khoản bị khóa trả 403 FORBIDDEN kèm lý do khóa")
    void login_LockedAccount_Returns403WithLockReason() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("Password123@");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new LockedAccountException("Tài khoản của bạn đã bị khóa.", "Vi phạm quy chế bán hàng gian lận"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa."))
                .andExpect(jsonPath("$.data.reason").value("Vi phạm quy chế bán hàng gian lận"))
                .andExpect(jsonPath("$.data.lockReason").value("Vi phạm quy chế bán hàng gian lận"));
    }

    @Test
    @DisplayName("NFR-SEC-31: Đăng nhập với tài khoản chưa xác thực email trả 403 FORBIDDEN kèm mã lỗi ACCOUNT_NOT_VERIFIED")
    void login_UnverifiedAccount_Returns403WithErrorCode() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@example.com");
        request.setPassword("Password123@");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnverifiedAccountException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email để tiếp tục.", "unverified@example.com"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tài khoản chưa được kích hoạt. Vui lòng xác thực email để tiếp tục."))
                .andExpect(jsonPath("$.data.errorCode").value("ACCOUNT_NOT_VERIFIED"))
                .andExpect(jsonPath("$.data.email").value("unverified@example.com"));
    }

    @Test
    @DisplayName("NFR-SEC-30: Đăng nhập sai email hoặc mật khẩu trả 401 UNAUTHORIZED")
    void login_WrongCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    // =========================================================================
    // Forgot Password & Reset Password Controller Tests (TSK-D10-CCT)
    // =========================================================================

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/forgot-password thành công trả 200 OK")
    void forgotPassword_ValidEmail_Returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("valid@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("Nếu email tồn tại trong hệ thống")));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/forgot-password email sai định dạng trả 400 BAD REQUEST")
    void forgotPassword_InvalidEmail_Returns400() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email-format");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").value("Email không đúng định dạng"));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/forgot-password email để trống trả 400 BAD REQUEST")
    void forgotPassword_BlankEmail_Returns400() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("   ");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email", containsString("Email không được để trống")));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/reset-password dữ liệu hợp lệ trả 200 OK")
    void resetPassword_ValidRequest_Returns200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "valid@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("Đặt lại mật khẩu thành công")));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/reset-password OTP không đúng 6 số trả 400 BAD REQUEST")
    void resetPassword_InvalidOtpFormat_Returns400() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "valid@example.com",
                "12345", // chỉ có 5 số
                "NewPassword123@",
                "NewPassword123@"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.otp").value("Mã OTP phải gồm đúng 6 chữ số"));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/reset-password mật khẩu mới ngắn hơn 6 ký tự trả 400 BAD REQUEST")
    void resetPassword_ShortPassword_Returns400() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "valid@example.com",
                "123456",
                "12345", // < 6 ký tự
                "12345"
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.newPassword").value("Mật khẩu mới phải từ 6 đến 100 ký tự"));
    }

    @Test
    @DisplayName("TSK-D10: POST /api/v1/auth/reset-password xác nhận mật khẩu để trống trả 400 BAD REQUEST")
    void resetPassword_BlankConfirmPassword_Returns400() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "valid@example.com",
                "123456",
                "NewPassword123@",
                "   "
        );

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.confirmPassword").value("Xác nhận mật khẩu mới không được để trống"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/send-registration-otp thành công trả 200 OK")
    void sendRegistrationOtp_Success() throws Exception {
        SendRegistrationOtpRequest request = SendRegistrationOtpRequest.builder()
                .email("testnew@example.com")
                .fullName("Nguyen Van Test")
                .phone("0987654321")
                .build();

        doNothing().when(authService).sendRegistrationOtp(any(SendRegistrationOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh thành công với cookie refresh_token hợp lệ")
    void refresh_Success() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token-xyz")
                .tokenType("Bearer")
                .expiresIn(900)
                .refreshToken("new-refresh-token-xyz")
                .build();

        when(authService.refreshToken("valid-raw-refresh-token")).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-raw-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token-xyz"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh trả lỗi 401 khi không có cookie refresh_token")
    void refresh_MissingCookie_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout thành công thu hồi token và xóa cookie")
    void logout_Success() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "some-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout thành công khi không có cookie")
    void logout_WithoutCookie_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
