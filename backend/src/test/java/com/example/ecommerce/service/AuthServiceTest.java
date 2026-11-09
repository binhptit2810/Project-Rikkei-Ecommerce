package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ForgotPasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.SendRegistrationOtpRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.dto.request.ResendVerificationRequest;
import com.example.ecommerce.dto.request.VerifyEmailRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.RegisterResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.exception.UnverifiedAccountException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthService.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 * Kiểm tra các yêu cầu chức năng (Đăng ký, Đăng nhập)
 * và phi chức năng (Bảo mật băm BCrypt, JWT, Bắt lỗi khóa tài khoản).
 */
// [TSK-D02-CCT]
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpHmacSecret", "test-otp-hmac-secret-key-at-least-32-chars");
        ReflectionTestUtils.setField(authService, "otpExpirationSeconds", 300L);
        ReflectionTestUtils.setField(authService, "otpRateLimitSeconds", 60L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setPassword("Password123@");
        registerRequest.setFullName("Nguyễn Văn A");
        registerRequest.setPhone("0987654321");
        registerRequest.setAddress("123 Hà Nội");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("testuser@example.com");
        loginRequest.setPassword("Password123@");

        mockUser = User.builder()
                .id(1L)
                .email("testuser@example.com")
                .password("$2a$12$hashedPasswordExample")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("NFR-SEC-01 & FR-AUTH-01: Đăng ký tài khoản thành công - Mật khẩu được băm BCrypt, emailVerified=false, gửi OTP qua email và không cấp token")
    void register_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPasswordExample");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RegisterResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getEmailVerified()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("$2a$12$hashedPasswordExample");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(savedUser.getActive()).isTrue();
        assertThat(savedUser.getEmailVerified()).isFalse();
        assertThat(savedUser.getEmailVerifiedAt()).isNull();

        verify(emailService).sendVerificationOtpEmail(eq("testuser@example.com"), anyString(), eq("Nguyễn Văn A"));
        verify(valueOperations).set(eq("auth:verify_otp:testuser@example.com"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("auth:verify_ratelimit:testuser@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS));
        verify(jwtUtils, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("NFR-DATA-01: Đăng ký thất bại khi email đã tồn tại và đã xác thực")
    void register_EmailAlreadyExists_Verified_ThrowsBusinessException() {
        mockUser.setEmailVerified(true);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email đã được sử dụng");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("NFR-SEC-04: Đăng ký thất bại khi email đã tồn tại nhưng chưa xác thực (chống ghi đè tài khoản)")
    void register_EmailAlreadyExists_Unverified_ThrowsBusinessException() {
        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa hoàn tất xác thực");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("NFR-DATA-02: Email được chuẩn hóa tự động (lowercase và trim khoảng trắng)")
    void register_EmailNormalization() {
        registerRequest.setEmail("  TestUser@Example.COM  ");
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    @DisplayName("FR-AUTH-02 & NFR-SEC-02: Đăng nhập thành công với tài khoản hợp lệ, nhận Access Token Bearer")
    void login_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock-valid-jwt");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-valid-jwt");
        assertThat(response.getUser().getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    @DisplayName("NFR-SEC-03: Đăng nhập thất bại khi tài khoản bị khóa - Bắn LockedAccountException kèm lý do")
    void login_AccountLocked_ThrowsLockedAccountException() {
        mockUser.setActive(false);
        mockUser.setLockReason("Vi phạm điều khoản chính sách thanh toán");

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
                    assertThat(lockedEx.getReason()).isEqualTo("Vi phạm điều khoản chính sách thanh toán");
                });

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("NFR-SEC-04: Đăng nhập tài khoản bị DisabledException từ Spring Security chuyển thành LockedAccountException")
    void login_DisabledException_ThrowsLockedAccountException() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedAccountException.class)
                .hasMessage("Tài khoản của bạn đã bị khóa.");
    }

    @Test
    @DisplayName("NFR-SEC-05: Đăng nhập sai mật khẩu ném BadCredentialsException")
    void login_WrongPassword_ThrowsBadCredentialsException() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Sai mật khẩu"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Branch: Đăng nhập tài khoản bị khóa nhưng không có lockReason (lockReason = null)")
    void login_AccountLocked_WithoutReason_ThrowsLockedAccountExceptionWithNullReason() {
        mockUser.setActive(false);
        mockUser.setLockReason(null);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
                    assertThat(lockedEx.getReason()).isNull();
                });
    }

    @Test
    @DisplayName("Branch: User không tồn tại trong DB trước khi authenticate -> ném BadCredentialsException")
    void login_UserNotFoundBeforeAuth_ThrowsBadCredentialsException() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Sai thông tin"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Branch: User không tồn tại trong DB trước khi authenticate nhưng ném DisabledException")
    void login_UserNotFoundBeforeAuth_ThrowsDisabledException() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getReason()).isNull();
                });
    }

    @Test
    @DisplayName("Branch: User có lockReason khi ném DisabledException -> đưa lý do vào LockedAccountException")
    void login_DisabledException_WithLockReasonInDatabase() {
        mockUser.setLockReason("Tài khoản có dấu hiệu gian lận thẻ");
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getReason()).isEqualTo("Tài khoản có dấu hiệu gian lận thẻ");
                });
    }

    @Test
    @DisplayName("Branch: User null trước auth nhưng sau auth tìm thấy user trong DB -> Đăng nhập thành công")
    void login_UserNullBeforeAuth_FindsUserAfterAuth_Success() {
        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.empty()) // Trước auth = null
                .thenReturn(Optional.of(mockUser)); // Sau auth = tìm thấy

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock-token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-token");
    }

    @Test
    @DisplayName("Branch: User null trước auth và sau auth vẫn không tìm thấy trong DB -> ném BusinessException")
    void login_UserNullBeforeAuth_NotFoundAfterAuth_ThrowsBusinessException() {
        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.empty()); // Cả 2 lần đều empty

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy thông tin người dùng");
    }

    // =========================================================================
    // Refresh Token & Logout Tests (BUG-02 Fix & Sequence Verification)
    // =========================================================================

    @Test
    @DisplayName("BUG-02 & FR-AUTH-03: Làm mới token thành công khi token metadata hợp lệ, user active và password hash khớp")
    void refreshToken_Success() {
        RefreshTokenService.RefreshTokenData tokenData = RefreshTokenService.RefreshTokenData.builder()
                .userId(1L)
                .email("testuser@example.com")
                .role("CUSTOMER")
                .familyId("fam-123")
                .status("ACTIVE")
                .passwordHash(mockUser.getPassword())
                .build();

        RefreshTokenService.RotationResult rotationResult = new RefreshTokenService.RotationResult(
                "new-raw-refresh-token",
                tokenData
        );

        when(refreshTokenService.getTokenData("valid-raw-refresh-token")).thenReturn(tokenData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(refreshTokenService.rotateRefreshToken("valid-raw-refresh-token")).thenReturn(rotationResult);
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("new-access-token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);

        AuthResponse response = authService.refreshToken("valid-raw-refresh-token");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(response.getRefreshToken()).isEqualTo("new-raw-refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("testuser@example.com");

        // Xác nhận thứ tự thực thi chính xác: đọc token -> kiểm tra DB -> xoay vòng Redis
        org.mockito.InOrder inOrder = inOrder(refreshTokenService, userRepository);
        inOrder.verify(refreshTokenService).getTokenData("valid-raw-refresh-token");
        inOrder.verify(userRepository).findById(1L);
        inOrder.verify(refreshTokenService).rotateRefreshToken("valid-raw-refresh-token");
    }

    @Test
    @DisplayName("BUG-02: Làm mới token thất bại khi token rỗng -> ném BadCredentialsException không gọi DB/Redis")
    void refreshToken_BlankToken_ThrowsBadCredentialsException() {
        assertThatThrownBy(() -> authService.refreshToken("   "))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token không hợp lệ hoặc không tồn tại");

        verifyNoInteractions(userRepository);
        verify(refreshTokenService, never()).rotateRefreshToken(anyString());
    }

    @Test
    @DisplayName("BUG-02: User không tồn tại trong DB -> ném BusinessException và KHÔNG xoay vòng token trong Redis")
    void refreshToken_UserNotFoundInDb_ThrowsBusinessException_DoesNotRotateToken() {
        RefreshTokenService.RefreshTokenData tokenData = RefreshTokenService.RefreshTokenData.builder()
                .userId(999L)
                .build();

        when(refreshTokenService.getTokenData("valid-raw-token")).thenReturn(tokenData);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("valid-raw-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy thông tin người dùng");

        // Đảm bảo không xoay vòng token khi kiểm tra DB thất bại
        verify(refreshTokenService, never()).rotateRefreshToken(anyString());
    }

    @Test
    @DisplayName("BUG-02: User bị khóa tài khoản -> ném LockedAccountException và KHÔNG xoay vòng token trong Redis")
    void refreshToken_UserLocked_ThrowsLockedAccountException_DoesNotRotateToken() {
        mockUser.setActive(false);
        mockUser.setLockReason("Tài khoản gian lận thẻ");

        RefreshTokenService.RefreshTokenData tokenData = RefreshTokenService.RefreshTokenData.builder()
                .userId(1L)
                .passwordHash(mockUser.getPassword())
                .build();

        when(refreshTokenService.getTokenData("valid-raw-token")).thenReturn(tokenData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.refreshToken("valid-raw-token"))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
                    assertThat(lockedEx.getReason()).isEqualTo("Tài khoản gian lận thẻ");
                });

        // Đảm bảo không xoay vòng token khi tài khoản bị khóa
        verify(refreshTokenService, never()).rotateRefreshToken(anyString());
    }

    @Test
    @DisplayName("BUG-02: Mật khẩu đã thay đổi sau khi cấp token -> ném BadCredentialsException và KHÔNG xoay vòng token")
    void refreshToken_PasswordChangedInDb_ThrowsBadCredentialsException_DoesNotRotateToken() {
        RefreshTokenService.RefreshTokenData tokenData = RefreshTokenService.RefreshTokenData.builder()
                .userId(1L)
                .passwordHash("$2a$12$oldHashedPasswordSnapshot")
                .build();

        // Password trong DB đã được cập nhật khác với snapshot trong token
        mockUser.setPassword("$2a$12$newlyChangedPasswordHash");

        when(refreshTokenService.getTokenData("valid-raw-token")).thenReturn(tokenData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.refreshToken("valid-raw-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mật khẩu tài khoản đã thay đổi. Vui lòng đăng nhập lại.");

        // Đảm bảo không xoay vòng token khi password hash không khớp
        verify(refreshTokenService, never()).rotateRefreshToken(anyString());
    }

    @Test
    @DisplayName("BUG-02: Lỗi truy vấn CSDL (Database timeout/failure) -> ném ngoại lệ và KHÔNG làm mất/xoay token của client")
    void refreshToken_DatabaseError_ThrowsException_DoesNotRotateToken() {
        RefreshTokenService.RefreshTokenData tokenData = RefreshTokenService.RefreshTokenData.builder()
                .userId(1L)
                .passwordHash(mockUser.getPassword())
                .build();

        when(refreshTokenService.getTokenData("valid-raw-token")).thenReturn(tokenData);
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database connection timeout"));

        assertThatThrownBy(() -> authService.refreshToken("valid-raw-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection timeout");

        // Đảm bảo token trong Redis không bị xoay mất khi DB gặp sự cố
        verify(refreshTokenService, never()).rotateRefreshToken(anyString());
    }

    @Test
    @DisplayName("NFR-SEC-RT-05: Đăng xuất tài khoản gọi thu hồi Refresh Token trong Redis")
    void logout_Success() {
        authService.logout("raw-token-to-logout");

        verify(refreshTokenService).revokeToken("raw-token-to-logout");
    }

    @Test
    @DisplayName("Portal Separation: ADMIN đăng nhập tại cổng CUSTOMER bị từ chối bằng thông báo chung không lộ vai trò")
    void login_AdminAccount_OnCustomerPortal_ThrowsBadCredentialsException() {
        loginRequest.setPortal("CUSTOMER");
        mockUser.setRole(Role.ADMIN);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_ADMIN")
                .build();
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email hoặc mật khẩu không chính xác.");

        // Tuyệt đối không tạo access token hay refresh token
        verify(jwtUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Portal Separation: CUSTOMER đăng nhập tại cổng ADMIN bị từ chối bằng thông báo chung")
    void login_CustomerAccount_OnAdminPortal_ThrowsBadCredentialsException() {
        loginRequest.setPortal("ADMIN");
        mockUser.setRole(Role.CUSTOMER);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email hoặc mật khẩu không chính xác.");

        verify(jwtUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Portal Separation: CUSTOMER đăng nhập đúng cổng CUSTOMER thành công")
    void login_CustomerAccount_OnCustomerPortal_Success() {
        loginRequest.setPortal("CUSTOMER");
        mockUser.setRole(Role.CUSTOMER);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock.jwt.token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(mockUser)).thenReturn("mock-refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUser().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Portal Separation: ADMIN đăng nhập đúng cổng ADMIN thành công")
    void login_AdminAccount_OnAdminPortal_Success() {
        loginRequest.setPortal("ADMIN");
        mockUser.setRole(Role.ADMIN);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_ADMIN")
                .build();
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock.admin.jwt.token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(mockUser)).thenReturn("mock-admin-refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.admin.jwt.token");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
    }

    // =========================================================================
    // Forgot Password & Reset Password Tests (TSK-D10-CCT)
    // =========================================================================

    @Test
    @DisplayName("TSK-D10: forgotPassword thành công khi email tồn tại, active=true, ngoài 60s rate limit")
    void forgotPassword_Success() {
        when(redisTemplate.hasKey("auth:otp_ratelimit:testuser@example.com")).thenReturn(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser@example.com");
        authService.forgotPassword(request);

        // Xác minh lưu OTP vào Redis với TTL 300s
        verify(valueOperations).set(eq("auth:otp:testuser@example.com"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        // Xác minh lưu rate limit với TTL 60s
        verify(valueOperations).set(eq("auth:otp_ratelimit:testuser@example.com"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        // Xác minh xóa attempts cũ
        verify(redisTemplate).delete("auth:otp_attempts:testuser@example.com");
        // Xác minh gọi EmailService gửi OTP 6 số
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("testuser@example.com"), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).matches("^\\d{6}$");
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword chống dò tìm tài khoản (User Enumeration) khi email không tồn tại")
    void forgotPassword_EmailNotExists_ReturnsSuccessWithoutSendingEmail() {
        when(redisTemplate.hasKey("auth:otp_ratelimit:notfound@example.com")).thenReturn(false);
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest("notfound@example.com");
        authService.forgotPassword(request);

        // Không sinh OTP, không lưu Redis, không gửi email
        verifyNoInteractions(emailService);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword không gửi email OTP khi tài khoản bị khóa (active = false)")
    void forgotPassword_AccountLocked_ReturnsSuccessWithoutSendingEmail() {
        mockUser.setActive(false);
        when(redisTemplate.hasKey("auth:otp_ratelimit:testuser@example.com")).thenReturn(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser@example.com");
        authService.forgotPassword(request);

        verifyNoInteractions(emailService);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword bị từ chối khi trong khoảng thời gian Rate Limit 60 giây")
    void forgotPassword_WithinRateLimit60s_ThrowsBusinessException() {
        when(redisTemplate.hasKey("auth:otp_ratelimit:testuser@example.com")).thenReturn(true);

        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser@example.com");

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới");

        verify(userRepository, never()).findByEmail(anyString());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword lỗi gửi email -> xóa OTP khỏi Redis và ném BusinessException, giữ rate limit")
    void forgotPassword_SendMailFailed_RollbacksRedisAndThrowsException() {
        when(redisTemplate.hasKey("auth:otp_ratelimit:testuser@example.com")).thenReturn(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendOtpEmail(anyString(), anyString());

        ForgotPasswordRequest request = new ForgotPasswordRequest("testuser@example.com");

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể gửi email xác thực lúc này");

        // Đảm bảo OTP đã được dọn khỏi Redis để tránh tồn tại mã không gửi được
        verify(redisTemplate).delete("auth:otp:testuser@example.com");
        // Đảm bảo không xóa rate limit key để ngăn spam
        verify(redisTemplate, never()).delete("auth:otp_ratelimit:testuser@example.com");
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword chuẩn hóa email (lowercase + trim)")
    void forgotPassword_EmailNormalized_LowerCaseAndTrim() {
        when(redisTemplate.hasKey("auth:otp_ratelimit:testuser@example.com")).thenReturn(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ForgotPasswordRequest request = new ForgotPasswordRequest("   TestUser@EXAMPLE.com   ");
        authService.forgotPassword(request);

        verify(redisTemplate).hasKey("auth:otp_ratelimit:testuser@example.com");
        verify(userRepository).findByEmail("testuser@example.com");
        verify(emailService).sendOtpEmail(eq("testuser@example.com"), anyString());
    }

    @Test
    @DisplayName("TSK-D10: forgotPassword với email rỗng ném BusinessException")
    void forgotPassword_NullOrBlankEmail_ThrowsBusinessException() {
        assertThatThrownBy(() -> authService.forgotPassword(new ForgotPasswordRequest("   ")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email không được để trống");
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thành công - Băm BCrypt mật khẩu mới, thu hồi phiên và xóa OTP")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewSecurePassword123@",
                "NewSecurePassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewSecurePassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("SUCCESS:280");
        when(passwordEncoder.encode("NewSecurePassword123@")).thenReturn("$2a$12$newBcryptHashedPassword");

        authService.resetPassword(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$12$newBcryptHashedPassword");

        // Thu hồi toàn bộ Refresh Token của user trong Redis
        verify(refreshTokenService).revokeAllUserSessions(mockUser.getId());
        // Xóa hoàn toàn OTP và Attempts key khỏi Redis sau khi DB thành công
        verify(redisTemplate).delete(Arrays.asList("auth:otp:testuser@example.com", "auth:otp_attempts:testuser@example.com"));
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi xác nhận mật khẩu không khớp")
    void resetPassword_ConfirmPasswordMismatch_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewSecurePassword123@",
                "DifferentPassword123@"
        );

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới và xác nhận mật khẩu không khớp");

        verifyNoInteractions(redisTemplate);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi mật khẩu mới trùng với mật khẩu cũ")
    void resetPassword_NewPasswordSameAsOld_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "OldPassword123@",
                "OldPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("OldPassword123@", mockUser.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới không được trùng với mật khẩu hiện tại");

        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi tài khoản bị khóa")
    void resetPassword_AccountLocked_ThrowsLockedAccountException() {
        mockUser.setActive(false);
        mockUser.setLockReason("Tài khoản bị tạm khóa do vi phạm");

        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(LockedAccountException.class)
                .satisfies(ex -> {
                    LockedAccountException lockedEx = (LockedAccountException) ex;
                    assertThat(lockedEx.getMessage()).isEqualTo("Tài khoản của bạn đã bị khóa.");
                    assertThat(lockedEx.getReason()).isEqualTo("Tài khoản bị tạm khóa do vi phạm");
                });

        verifyNoInteractions(redisTemplate);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi mã OTP không tồn tại hoặc đã hết hạn")
    void resetPassword_OtpExpiredOrNotFound_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewPassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("NOT_FOUND");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mã OTP không hợp lệ hoặc đã hết hạn");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi mã OTP sai (hiển thị số lần thử còn lại)")
    void resetPassword_OtpMismatch_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewPassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("MISMATCH:1");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mã OTP không chính xác. Bạn còn 4 lần thử.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi nhập sai OTP quá 5 lần (OTP bị hủy)")
    void resetPassword_OtpMaxAttemptsExceeded_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewPassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("MAX_ATTEMPTS_EXCEEDED");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn đã nhập sai mã OTP quá 5 lần. Mã OTP đã bị hủy, vui lòng yêu cầu mã mới.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword thất bại khi mã OTP đã được sử dụng")
    void resetPassword_OtpAlreadyUsed_ThrowsBusinessException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewPassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("ALREADY_USED");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mã OTP đã được sử dụng");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TSK-D10: resetPassword lỗi CSDL -> phục hồi lại OTP trên Redis và ném ngoại lệ")
    void resetPassword_DatabaseError_RestoresOtpInRedisAndThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "testuser@example.com",
                "123456",
                "NewPassword123@",
                "NewPassword123@"
        );

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("NewPassword123@", mockUser.getPassword())).thenReturn(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn("SUCCESS:200");
        when(passwordEncoder.encode("NewPassword123@")).thenReturn("$2a$12$newBcryptHashedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection dead"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection dead");

        // Xác minh cơ chế rollback phục hồi lại mã OTP trên Redis
        verify(valueOperations).set(eq("auth:otp:testuser@example.com"), anyString(), eq(200L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("NFR-SEC-05: Đăng nhập thất bại khi tài khoản CUSTOMER chưa xác thực email - Ném UnverifiedAccountException")
    void login_UnverifiedCustomer_ThrowsUnverifiedAccountException() {
        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_CUSTOMER")
                .build();
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnverifiedAccountException.class)
                .hasMessageContaining("chưa được xác thực email");
    }

    @Test
    @DisplayName("NFR-SEC-06: Đăng nhập thành công với tài khoản ADMIN ngay cả khi emailVerified=false")
    void login_AdminUser_UnaffectedByEmailVerified() {
        mockUser.setRole(Role.ADMIN);
        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser@example.com")
                .password("hashed")
                .authorities("ROLE_ADMIN")
                .build();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock-admin-jwt");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-admin-jwt");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("FR-AUTH-07: Xác thực email thành công với OTP hợp lệ - Cập nhật emailVerified=true và cấp JWT tokens")
    void verifyEmail_Success() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("testuser@example.com");
        request.setOtp("123456");

        mockUser.setEmailVerified(false);
        mockUser.setEmailVerifiedAt(null);

        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString())).thenReturn("SUCCESS:200");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("verified-access-token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(86400L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("verified-refresh-token");

        AuthResponse response = authService.verifyEmail(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("verified-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("verified-refresh-token");
        assertThat(response.getUser().getEmailVerified()).isTrue();

        assertThat(mockUser.getEmailVerified()).isTrue();
        assertThat(mockUser.getEmailVerifiedAt()).isNotNull();

        verify(redisTemplate).delete(anyList());
    }

    @Test
    @DisplayName("NFR-SEC-07: Xác thực email thất bại khi tài khoản đã được xác thực trước đó")
    void verifyEmail_AlreadyVerified_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("testuser@example.com");
        request.setOtp("123456");

        mockUser.setEmailVerified(true);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã được xác thực");

        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("NFR-SEC-08: Xác thực email thất bại khi mã OTP không chính xác")
    void verifyEmail_InvalidOtp_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("testuser@example.com");
        request.setOtp("654321");

        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString())).thenReturn("MISMATCH:1");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mã OTP không chính xác");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-SEC-09: Xác thực email thất bại khi vượt quá 5 lần nhập sai")
    void verifyEmail_ExceededMaxAttempts_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("testuser@example.com");
        request.setOtp("654321");

        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString())).thenReturn("MAX_ATTEMPTS_EXCEEDED");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("quá 5 lần");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-SEC-10: Xác thực email thất bại khi OTP đã hết hạn hoặc không tồn tại")
    void verifyEmail_ExpiredOtp_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("testuser@example.com");
        request.setOtp("123456");

        mockUser.setEmailVerified(false);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString())).thenReturn("NOT_FOUND");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết hạn");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-AUTH-08: Gửi lại mã OTP xác thực email thành công khi đã hết thời gian rate-limit")
    void resendVerification_Success() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("testuser@example.com");

        mockUser.setEmailVerified(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:testuser@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        authService.resendVerification(request);

        verify(emailService).sendVerificationOtpEmail(eq("testuser@example.com"), anyString(), eq("Nguyễn Văn A"));
        verify(valueOperations).set(eq("auth:verify_otp:testuser@example.com"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete("auth:verify_attempts:testuser@example.com");
    }

    @Test
    @DisplayName("NFR-SEC-11: Gửi lại mã OTP thất bại khi còn trong thời gian rate limit 60 giây")
    void resendVerification_RateLimited_ThrowsBusinessException() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("testuser@example.com");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:testuser@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(false);

        assertThatThrownBy(() -> authService.resendVerification(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng đợi 60 giây");

        verify(emailService, never()).sendVerificationOtpEmail(any(), any(), any());
    }

    @Test
    @DisplayName("NFR-SEC-12: Chống User Enumeration khi gửi lại mã OTP cho email không tồn tại hoặc đã kích hoạt")
    void resendVerification_AntiEnumeration_ReturnsSilently() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("unknown@example.com");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:unknown@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.resendVerification(request);

        verify(emailService, never()).sendVerificationOtpEmail(any(), any(), any());
        verify(valueOperations, never()).set(eq("auth:verify_otp:unknown@example.com"), anyString(), anyLong(), any(TimeUnit.class));
        verify(redisTemplate, never()).delete(eq("auth:verify_ratelimit:unknown@example.com"));
    }

    @Test
    @DisplayName("HIGH-01 & TASK-B: Đăng ký khi SMTP thất bại - Tài khoản vẫn được lưu, OTP bị xóa và rate-limit được giải phóng nguyên tử qua Lua script")
    void register_SmtpFailure_UserPersistedAndRateLimitReleased() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPasswordExample");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AtomicReference<String> savedLock = new AtomicReference<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("auth:verify_ratelimit:testuser@example.com".equals(key)) {
                savedLock.set(invocation.getArgument(1));
            }
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:verify_ratelimit:testuser@example.com")), anyString()))
                .thenReturn(1L);

        doThrow(new RuntimeException("Resend SMTP connection failed"))
                .when(emailService).sendVerificationOtpEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tài khoản đã được tạo nhưng không thể gửi email xác thực");

        verify(userRepository).save(any(User.class));
        verify(redisTemplate).delete("auth:verify_otp:testuser@example.com");
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("auth:verify_ratelimit:testuser@example.com")), eq(savedLock.get()));
    }

    @Test
    @DisplayName("HIGH-02 & TASK-B: Gửi lại mã OTP khi SMTP thất bại - Giải phóng rate-limit lock nguyên tử bằng Lua script với đúng owner lockValue")
    void resendVerification_SmtpFailure_ReleasesOwnedLock() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("testuser@example.com");

        mockUser.setEmailVerified(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:testuser@example.com"), lockCaptor.capture(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:verify_ratelimit:testuser@example.com")), anyString()))
                .thenReturn(1L);

        doThrow(new RuntimeException("Resend SMTP connection timed out"))
                .when(emailService).sendVerificationOtpEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> authService.resendVerification(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể gửi email xác thực lúc này");

        verify(redisTemplate).delete("auth:verify_otp:testuser@example.com");
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("auth:verify_ratelimit:testuser@example.com")), eq(lockCaptor.getValue()));
    }

    // =========================================================================
    // TASK-B: Dedicated Lua Compare-and-Delete Unit Tests
    // =========================================================================

    @Test
    @DisplayName("TASK-B Test 1: releaseRateLimitLock thành công khi đúng owner (Lua trả về 1)")
    void releaseRateLimitLock_CorrectOwner_ReturnsTrue() {
        String key = "auth:verify_ratelimit:testuser@example.com";
        String lockA = "lock-uuid-owner-a";

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(lockA)))
                .thenReturn(1L);

        boolean result = authService.releaseRateLimitLock(key, lockA);

        assertThat(result).isTrue();
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(lockA));
    }

    @Test
    @DisplayName("TASK-B Test 2: releaseRateLimitLock thất bại khi sai owner (Lua trả về 0)")
    void releaseRateLimitLock_WrongOwner_ReturnsFalse() {
        String key = "auth:verify_ratelimit:testuser@example.com";
        String wrongLock = "wrong-uuid";

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(wrongLock)))
                .thenReturn(0L);

        boolean result = authService.releaseRateLimitLock(key, wrongLock);

        assertThat(result).isFalse();
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(wrongLock));
    }

    @Test
    @DisplayName("TASK-B Test 3: releaseRateLimitLock an toàn khi lock đã hết hạn hoặc key không tồn tại")
    void releaseRateLimitLock_ExpiredOrMissingKey_ReturnsFalseWithoutError() {
        String key = "auth:verify_ratelimit:testuser@example.com";
        String lockValue = "any-lock-value";

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(lockValue)))
                .thenReturn(0L);

        boolean result = authService.releaseRateLimitLock(key, lockValue);

        assertThat(result).isFalse();

        // Edge case: input null/empty
        assertThat(authService.releaseRateLimitLock("", lockValue)).isFalse();
        assertThat(authService.releaseRateLimitLock(key, null)).isFalse();
    }

    @Test
    @DisplayName("TASK-B Test 4: Request cũ sở hữu lock cũ không thể xóa nhầm lock mới của request khác")
    void releaseRateLimitLock_StaleRequestCannotDeleteNewLock() {
        String key = "auth:verify_ratelimit:testuser@example.com";
        String oldLockA = "lock-request-a-expired";

        // Giả lập Redis hiện đang chứa lockB của request B -> Lua script so sánh thấy lệch -> trả về 0
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(oldLockA)))
                .thenReturn(0L);

        boolean released = authService.releaseRateLimitLock(key, oldLockA);

        assertThat(released).isFalse();
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of(key)), eq(oldLockA));
    }

    @Test
    @DisplayName("TASK-B Test 5: SMTP thất bại - chỉ giải phóng lock của chính request đó, không xóa nhầm key khác")
    void releaseRateLimitLock_SmtpFailure_OnlyOwnLockReleased() {
        String keyA = "auth:verify_ratelimit:userA@example.com";
        String lockA = "uuid-request-A";

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of(keyA)), eq(lockA)))
                .thenReturn(1L);

        boolean released = authService.releaseRateLimitLock(keyA, lockA);

        assertThat(released).isTrue();
        // Xác nhận không bao giờ gọi nhầm key của userB
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), eq(List.of("auth:verify_ratelimit:userB@example.com")), any());
        verify(redisTemplate, never()).delete("auth:verify_ratelimit:userB@example.com");
    }

    @Test
    @DisplayName("LOW-02: Khởi động trên môi trường Docker/Prod thất bại nếu thiếu hoặc dùng default OTP_HMAC_SECRET")
    void validateOtpHmacSecret_ProductionEnvironment_WeakSecret_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(authService, "activeProfile", "docker");
        ReflectionTestUtils.setField(authService, "otpHmacSecret", "ecommerce-default-otp-hmac-secret-change-in-production-2024");

        assertThatThrownBy(() -> authService.validateOtpHmacSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL SECURITY ERROR");

        ReflectionTestUtils.setField(authService, "otpHmacSecret", "short-secret");
        assertThatThrownBy(() -> authService.validateOtpHmacSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL SECURITY ERROR");
    }

    @Test
    @DisplayName("LOW-02: Khởi động trên môi trường Docker/Prod thành công với OTP_HMAC_SECRET an toàn >= 32 ký tự")
    void validateOtpHmacSecret_ProductionEnvironment_ValidSecret_Success() {
        ReflectionTestUtils.setField(authService, "activeProfile", "docker");
        ReflectionTestUtils.setField(authService, "otpHmacSecret", "my-super-secret-production-hmac-key-2026-safe");

        // Should execute successfully without throwing exception
        authService.validateOtpHmacSecret();
    }

    @Test
    @DisplayName("sendRegistrationOtp thành công")
    void sendRegistrationOtp_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0987654321")).thenReturn(false);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:newuser@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        SendRegistrationOtpRequest req = SendRegistrationOtpRequest.builder()
                .email("newuser@example.com")
                .fullName("Nguyen Van B")
                .phone("0987654321")
                .build();

        authService.sendRegistrationOtp(req);

        verify(emailService, times(1)).sendVerificationOtpEmail(eq("newuser@example.com"), anyString(), eq("Nguyen Van B"));
    }

    @Test
    @DisplayName("sendRegistrationOtp ném lỗi khi email đã được xác thực")
    void sendRegistrationOtp_Throws_WhenEmailAlreadyVerified() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(mockUser));

        SendRegistrationOtpRequest req = SendRegistrationOtpRequest.builder()
                .email("testuser@example.com")
                .build();

        assertThatThrownBy(() -> authService.sendRegistrationOtp(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email đã được sử dụng");
    }

    @Test
    @DisplayName("sendRegistrationOtp ném lỗi khi số điện thoại đã tồn tại")
    void sendRegistrationOtp_Throws_WhenPhoneExists() {
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0987654321")).thenReturn(true);

        SendRegistrationOtpRequest req = SendRegistrationOtpRequest.builder()
                .email("newuser@example.com")
                .phone("0987654321")
                .build();

        assertThatThrownBy(() -> authService.sendRegistrationOtp(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số điện thoại này đã được sử dụng");
    }

    @Test
    @DisplayName("sendRegistrationOtp ném lỗi khi bị rate limit 60s")
    void sendRegistrationOtp_Throws_WhenRateLimited() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0987654321")).thenReturn(false);
        when(valueOperations.setIfAbsent(eq("auth:verify_ratelimit:newuser@example.com"), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        SendRegistrationOtpRequest req = SendRegistrationOtpRequest.builder()
                .email("newuser@example.com")
                .phone("0987654321")
                .build();

        assertThatThrownBy(() -> authService.sendRegistrationOtp(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng đợi 60 giây");
    }

    @Test
    @DisplayName("register với OTP hợp lệ thành công")
    void register_WithOtp_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByPhone("0987654321")).thenReturn(false);

        String otp = "123456";
        String secret = "test-otp-hmac-secret-key-at-least-32-chars";
        String expectedHmac = ReflectionTestUtils.invokeMethod(authService, "calculateHmac", otp, secret);
        when(valueOperations.get("auth:verify_otp:newuser@example.com")).thenReturn(expectedHmac);

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$encodedPassword");
        User savedUser = User.builder()
                .id(99L)
                .email("newuser@example.com")
                .fullName("Nguyen Van C")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser@example.com");
        req.setPassword("Password123@");
        req.setFullName("Nguyen Van C");
        req.setPhone("0987654321");
        req.setOtp("123456");

        RegisterResponse resp = authService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("newuser@example.com");
        assertThat(resp.getEmailVerified()).isTrue();
    }
}
