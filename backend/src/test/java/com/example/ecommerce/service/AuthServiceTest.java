package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.LockedAccountException;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthService.
 * Kiểm tra các yêu cầu chức năng (Đăng ký, Đăng nhập)
 * và phi chức năng (Bảo mật băm BCrypt, JWT, Bắt lỗi khóa tài khoản).
 */
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

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
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
                .build();
    }

    @Test
    @DisplayName("NFR-SEC-01 & FR-AUTH-01: Đăng ký tài khoản thành công - Mật khẩu được băm BCrypt và role mặc định CUSTOMER")
    void register_Success() {
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPasswordExample");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("mock-jwt-token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("testuser@example.com");
        assertThat(response.getUser().getRole()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("$2a$12$hashedPasswordExample");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(savedUser.getActive()).isTrue();
    }

    @Test
    @DisplayName("NFR-DATA-01: Đăng ký thất bại khi email đã tồn tại trong hệ thống")
    void register_EmailAlreadyExists_ThrowsBusinessException() {
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email đã được sử dụng");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("NFR-DATA-02: Email được chuẩn hóa tự động (lowercase và trim khoảng trắng)")
    void register_EmailNormalization() {
        registerRequest.setEmail("  TestUser@Example.COM  ");
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("token");

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
        when(jwtUtils.generateToken(userDetails)).thenReturn("mock-token");

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy thông tin người dùng");
    }
}

