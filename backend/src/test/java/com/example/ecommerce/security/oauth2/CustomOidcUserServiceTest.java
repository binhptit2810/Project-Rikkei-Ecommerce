package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.UserOAuthAccount;
import com.example.ecommerce.repository.UserOAuthAccountRepository;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho CustomOidcUserService [TSK-D15-CCT].
 * Kiểm tra các kịch bản:
 * 1. Đăng ký tài khoản CUSTOMER mới qua Google OIDC.
 * 2. Đăng nhập tài khoản Google đã liên kết.
 * 3. Chặn đăng nhập nếu tài khoản bị khóa (active = false).
 * 4. Chặn đăng nhập nếu tài khoản là ADMIN tại storefront.
 * 5. Từ chối nếu Google trả về email chưa xác thực (email_verified = false).
 * 6. Từ chối nếu thiếu email hoặc thiếu Subject (sub).
 * 7. BẢO MẬT: Không tự động liên kết nếu trùng email với tài khoản LOCAL có sẵn.
 */
@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserOAuthAccountRepository userOAuthAccountRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Mock
    private OidcUserRequest oidcUserRequest;

    private CustomOidcUserService oidcUserService;

    @BeforeEach
    void setUp() {
        oidcUserService = spy(new CustomOidcUserService(userRepository, userOAuthAccountRepository, passwordEncoder));
    }

    private OidcUser createMockOidcUser(String sub, String email, Boolean emailVerified, String name, String picture) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("email_verified", emailVerified);
        claims.put("name", name);
        claims.put("picture", picture);

        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        return new DefaultOidcUser(Collections.emptyList(), idToken, userInfo, "sub");
    }

    @Test
    @DisplayName("TC-OIDC-01: Tạo mới tài khoản CUSTOMER khi email và Google sub chưa tồn tại")
    void loadUser_NewGoogleUser_CreatesCustomerAccount() {
        String sub = "google-sub-123456";
        String email = "newuser@gmail.com";
        String name = "Nguyễn Văn Mới";
        String picture = "https://lh3.googleusercontent.com/avatar123.jpg";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, true, name, picture);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        when(userOAuthAccountRepository.findByProviderAndProviderId("GOOGLE", sub)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        User savedUser = User.builder()
                .id(100L)
                .email(email)
                .fullName(name)
                .avatarUrl(picture)
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .password("$2a$12$someRandomBcryptHashValue")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        OidcUser result = oidcUserService.loadUser(oidcUserRequest);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(CustomOidcUser.class);
        CustomOidcUser customOidcUser = (CustomOidcUser) result;
        assertThat(customOidcUser.getEmail()).isEqualTo(email);
        assertThat(customOidcUser.getUserEntity().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(customOidcUser.getUserEntity().getAvatarUrl()).isEqualTo(picture);

        // Xác minh lưu user và oauth account
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo(email);
        assertThat(capturedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(capturedUser.getEmailVerified()).isTrue();
        assertThat(capturedUser.getAvatarUrl()).isEqualTo(picture);
        // Mật khẩu ngẫu nhiên được mã hóa BCrypt
        assertThat(capturedUser.getPassword()).startsWith("$2a$12$");

        verify(userOAuthAccountRepository).save(any(UserOAuthAccount.class));
    }

    @Test
    @DisplayName("TC-OIDC-02: Đăng nhập thành công khi Google Sub đã được liên kết trước đó")
    void loadUser_ExistingLinkedUser_ReturnsCustomOidcUser() {
        String sub = "google-sub-789";
        String email = "existing@gmail.com";
        String name = "Trần Đã Có";
        String picture = "https://lh3.googleusercontent.com/updated-avatar.jpg";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, true, name, picture);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        User existingUser = User.builder()
                .id(50L)
                .email(email)
                .fullName(name)
                .avatarUrl("https://lh3.googleusercontent.com/old-avatar.jpg")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .id(1L)
                .user(existingUser)
                .provider("GOOGLE")
                .providerId(sub)
                .email(email)
                .build();

        when(userOAuthAccountRepository.findByProviderAndProviderId("GOOGLE", sub)).thenReturn(Optional.of(oauthAccount));

        OidcUser result = oidcUserService.loadUser(oidcUserRequest);

        assertThat(result).isNotNull();
        assertThat(((CustomOidcUser) result).getUserEntity().getId()).isEqualTo(50L);
        // Đảm bảo avatar mới được đồng bộ
        verify(userRepository).save(existingUser);
        assertThat(existingUser.getAvatarUrl()).isEqualTo(picture);
    }

    @Test
    @DisplayName("TC-OIDC-03: Chặn đăng nhập nếu tài khoản đã liên kết bị khóa (active = false)")
    void loadUser_LockedAccount_ThrowsOAuth2AuthenticationException() {
        String sub = "google-sub-locked";
        String email = "locked@gmail.com";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, true, "Locked User", null);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        User lockedUser = User.builder()
                .id(99L)
                .email(email)
                .role(Role.CUSTOMER)
                .active(false)
                .lockReason("Vi phạm chính sách thanh toán")
                .build();

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .user(lockedUser)
                .provider("GOOGLE")
                .providerId(sub)
                .build();

        when(userOAuthAccountRepository.findByProviderAndProviderId("GOOGLE", sub)).thenReturn(Optional.of(oauthAccount));

        assertThatThrownBy(() -> oidcUserService.loadUser(oidcUserRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Tài khoản của bạn đã bị khóa: Vi phạm chính sách thanh toán");
    }

    @Test
    @DisplayName("TC-OIDC-04: Chặn tài khoản ADMIN đăng nhập qua luồng Google Customer")
    void loadUser_AdminAccount_ThrowsOAuth2AuthenticationException() {
        String sub = "google-sub-admin";
        String email = "admin@eshop.com";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, true, "Admin User", null);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        User adminUser = User.builder()
                .id(1L)
                .email(email)
                .role(Role.ADMIN)
                .active(true)
                .build();

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .user(adminUser)
                .provider("GOOGLE")
                .providerId(sub)
                .build();

        when(userOAuthAccountRepository.findByProviderAndProviderId("GOOGLE", sub)).thenReturn(Optional.of(oauthAccount));

        assertThatThrownBy(() -> oidcUserService.loadUser(oidcUserRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Tài khoản Quản trị viên (ADMIN) không được phép đăng nhập tại cổng Khách hàng");
    }

    @Test
    @DisplayName("TC-OIDC-05: Từ chối đăng nhập nếu email Google chưa xác thực (email_verified = false)")
    void loadUser_UnverifiedEmail_ThrowsOAuth2AuthenticationException() {
        String sub = "google-sub-unverified";
        String email = "unverified@gmail.com";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, false, "Unverified User", null);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        assertThatThrownBy(() -> oidcUserService.loadUser(oidcUserRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Địa chỉ email Google chưa được xác thực");
    }

    @Test
    @DisplayName("TC-OIDC-06: AN NINH - Từ chối tự động liên kết nếu trùng email với tài khoản LOCAL có sẵn")
    void loadUser_EmailMatchesLocalAccount_ThrowsOAuth2AuthenticationException() {
        String sub = "google-sub-attacker-or-new";
        String email = "localcustomer@example.com";

        OidcUser mockOidcUser = createMockOidcUser(sub, email, true, "Local Match", null);
        doReturn(mockOidcUser).when(oidcUserService).fetchOidcUser(any(OidcUserRequest.class));

        when(userOAuthAccountRepository.findByProviderAndProviderId("GOOGLE", sub)).thenReturn(Optional.empty());

        User localUser = User.builder()
                .id(10L)
                .email(email)
                .password("$2a$12$LocalUserExistingBcryptPassword")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> oidcUserService.loadUser(oidcUserRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email này đã được đăng ký bằng tài khoản thông thường. Vui lòng đăng nhập bằng mật khẩu để liên kết tài khoản.");

        // Khẳng định không ghi đè mật khẩu và không tạo liên kết tùy tiện
        verify(userRepository, never()).save(any());
        verify(userOAuthAccountRepository, never()).save(any());
    }
}

