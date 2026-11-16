package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.dto.request.Auth0ExchangeRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.UserOAuthAccount;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.repository.UserOAuthAccountRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test cho quy trình trao đổi Auth0 Token và đồng bộ tài khoản người dùng [AUTH0-GOOGLE].
 *
 * Kiểm tra các kịch bản:
 * 1. User mới: tạo User (ROLE_CUSTOMER) + UserOAuthAccount, trả về native JWT và HttpOnly cookie.
 * 2. User đã liên kết: đăng nhập thành công, đồng bộ avatar nếu có thay đổi.
 * 3. Email đã tồn tại trong local users nhưng chưa liên kết: từ chối tự động liên kết (Account Takeover Prevention).
 * 4. User bị khóa: từ chối đăng nhập (LockedAccountException -> 403 Forbidden).
 * 5. Phân tách đặc quyền: từ chối tài khoản ROLE_ADMIN đăng nhập tại storefront qua Auth0.
 * 6. Controller HTTP endpoint POST /api/v1/auth/auth0/exchange trả về chuẩn ApiResponse.
 *
 * Phụ trách: Cao Chí Thiện
 * Module: Auth & Security
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth0testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-test",
    "app.otp.hmac-secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-test",
    "app.auth0.domain=dev-rikkei-ecommerce.us.auth0.com",
    "app.auth0.client-id=test-auth0-client-id"
})
class Auth0ExchangeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOAuthAccountRepository userOAuthAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private Auth0TokenValidator auth0TokenValidator;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void cleanUpBefore() {
        userOAuthAccountRepository.deleteAll();
        userRepository.deleteAll();
        when(refreshTokenService.createRefreshToken(any())).thenReturn("mock-refresh-token-123456");
    }

    @AfterEach
    void cleanUpAfter() {
        userOAuthAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-EXCH-01: User mới -> Tự động tạo User (ROLE_CUSTOMER) và UserOAuthAccount, cấp token hợp lệ")
    void exchangeAuth0Token_NewUser_CreatesCustomerAndOAuthAccount() {
        String token = "valid.auth0.token.newuser";
        String sub = "google-oauth2|100000000001";
        String email = "new.google.user@example.com";

        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("Nguyen Van Moi")
                .picture("https://lh3.googleusercontent.com/avatar1.jpg")
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        AuthResponse response = authService.exchangeAuth0IdToken(token);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(email, response.getUser().getEmail());
        assertEquals(Role.CUSTOMER, response.getUser().getRole());

        // Kiểm tra database MySQL/H2
        Optional<User> savedUserOpt = userRepository.findByEmail(email);
        assertTrue(savedUserOpt.isPresent());
        User savedUser = savedUserOpt.get();
        assertEquals(Role.CUSTOMER, savedUser.getRole(), "Tài khoản tạo mới bắt buộc phải là ROLE_CUSTOMER");
        assertTrue(savedUser.getActive());
        assertTrue(savedUser.getEmailVerified());
        assertEquals("Nguyen Van Moi", savedUser.getFullName());
        assertEquals("https://lh3.googleusercontent.com/avatar1.jpg", savedUser.getAvatarUrl());

        // Kiểm tra liên kết UserOAuthAccount
        Optional<UserOAuthAccount> savedOAuthOpt =
                userOAuthAccountRepository.findByProviderAndProviderId("AUTH0", sub);
        assertTrue(savedOAuthOpt.isPresent());
        assertEquals(savedUser.getId(), savedOAuthOpt.get().getUser().getId());
        assertEquals(email, savedOAuthOpt.get().getEmail());
    }

    @Test
    @DisplayName("TC-EXCH-02: User đã liên kết -> Đăng nhập thành công và cập nhật avatar mới nhất")
    void exchangeAuth0Token_ExistingLinkedUser_LogsInAndUpdateAvatar() {
        String sub = "google-oauth2|200000000002";
        String email = "existing.linked@example.com";

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("SecureP@ss123"))
                .fullName("Nguyen Van Da Co")
                .avatarUrl("https://lh3.googleusercontent.com/old_avatar.jpg")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .user(user)
                .provider("AUTH0")
                .providerId(sub)
                .email(email)
                .build();
        userOAuthAccountRepository.save(oauthAccount);

        String token = "valid.auth0.token.existing";
        String newPicture = "https://lh3.googleusercontent.com/new_avatar.jpg";
        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("Nguyen Van Da Co")
                .picture(newPicture)
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        AuthResponse response = authService.exchangeAuth0IdToken(token);

        assertNotNull(response);
        assertEquals(user.getId(), response.getUser().getId());

        // Kiểm tra avatar đã được cập nhật
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(newPicture, updatedUser.getAvatarUrl());
    }

    @Test
    @DisplayName("TC-EXCH-03: Email đã tồn tại trong local users nhưng chưa liên kết -> Từ chối tự động liên kết (Account Takeover Prevention)")
    void exchangeAuth0Token_ExistingLocalEmailWithoutLink_ThrowsBusinessException() {
        String email = "local.user@example.com";

        User localUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode("LocalPassword123"))
                .fullName("Local Account User")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        userRepository.save(localUser);

        String token = "auth0.token.unlinked";
        String sub = "google-oauth2|300000000003";

        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("Attempted Hijacker")
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.exchangeAuth0IdToken(token));
        assertTrue(ex.getMessage().contains("đã được đăng ký"));
        assertTrue(ex.getMessage().contains("mật khẩu"));

        // Xác nhận chưa hề có liên kết OAuth nào được tạo trong DB
        assertFalse(userOAuthAccountRepository.existsByProviderAndProviderId("AUTH0", sub));
    }

    @Test
    @DisplayName("TC-EXCH-04: User bị Admin khóa -> Ném LockedAccountException kèm lý do khóa")
    void exchangeAuth0Token_LockedUser_ThrowsLockedAccountException() {
        String sub = "google-oauth2|400000000004";
        String email = "locked.user@example.com";

        User lockedUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123"))
                .fullName("Locked User")
                .role(Role.CUSTOMER)
                .active(false)
                .lockReason("Vi phạm quy chế bán hàng")
                .emailVerified(true)
                .build();
        lockedUser = userRepository.save(lockedUser);

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .user(lockedUser)
                .provider("AUTH0")
                .providerId(sub)
                .email(email)
                .build();
        userOAuthAccountRepository.save(oauthAccount);

        String token = "auth0.token.locked";
        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("Locked User")
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        LockedAccountException ex = assertThrows(LockedAccountException.class, () -> authService.exchangeAuth0IdToken(token));
        assertEquals("Vi phạm quy chế bán hàng", ex.getReason());
    }

    @Test
    @DisplayName("TC-EXCH-05: Tài khoản ROLE_ADMIN cố gắng đăng nhập tại storefront qua Auth0 -> Bị từ chối")
    void exchangeAuth0Token_AdminUser_ThrowsBusinessException() {
        String sub = "google-oauth2|500000000005";
        String email = "admin@rikkei.com";

        User adminUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode("AdminPass123"))
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .active(true)
                .emailVerified(true)
                .build();
        adminUser = userRepository.save(adminUser);

        UserOAuthAccount oauthAccount = UserOAuthAccount.builder()
                .user(adminUser)
                .provider("AUTH0")
                .providerId(sub)
                .email(email)
                .build();
        userOAuthAccountRepository.save(oauthAccount);

        String token = "auth0.token.admin";
        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("System Administrator")
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.exchangeAuth0IdToken(token));
        assertTrue(ex.getMessage().contains("ADMIN"));
    }

    @Test
    @DisplayName("TC-CTRL-01: POST /api/v1/auth/auth0/exchange thành công -> 200 OK + JWT body + HttpOnly cookie")
    void controllerExchange_Success_Returns200AndHttpOnlyCookie() throws Exception {
        String token = "mock.valid.auth0.token";
        String sub = "google-oauth2|600000000006";
        String email = "ctrl.success@example.com";

        Auth0UserInfo mockInfo = Auth0UserInfo.builder()
                .sub(sub)
                .email(email)
                .emailVerified(true)
                .name("Test User Controller")
                .build();

        when(auth0TokenValidator.validateToken(token)).thenReturn(mockInfo);

        Auth0ExchangeRequest request = Auth0ExchangeRequest.builder()
                .idToken(token)
                .build();

        mockMvc.perform(post("/api/v1/auth/auth0/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    @DisplayName("TC-CTRL-02: POST /api/v1/auth/auth0/exchange với token rỗng -> 400 Bad Request")
    void controllerExchange_BlankToken_Returns400BadRequest() throws Exception {
        Auth0ExchangeRequest request = Auth0ExchangeRequest.builder()
                .idToken("   ")
                .build();

        mockMvc.perform(post("/api/v1/auth/auth0/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

