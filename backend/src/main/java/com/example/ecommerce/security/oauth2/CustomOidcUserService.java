package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.UserOAuthAccount;
import com.example.ecommerce.repository.UserOAuthAccountRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service xử lý xác thực OpenID Connect (OIDC) với Google.
 * Kế thừa OidcUserService của Spring Security để tự động xác minh chữ ký, issuer,
 * audience, và thời hạn của ID Token trước khi xử lý dữ liệu người dùng.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Service
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOidcUserService(UserRepository userRepository,
                                 UserOAuthAccountRepository userOAuthAccountRepository,
                                 @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userOAuthAccountRepository = userOAuthAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Tải và xác minh ID Token từ nhà cung cấp OIDC.
     * Tách riêng thành phương thức protected để hỗ trợ unit test mà không cần gọi mạng thật.
     */
    protected OidcUser fetchOidcUser(OidcUserRequest userRequest) {
        return super.loadUser(userRequest);
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Để Spring Security OIDC kiểm tra chữ ký, issuer, audience và thời hạn ID Token
        OidcUser oidcUser = fetchOidcUser(userRequest);

        // 2. Trích xuất thuộc tính OpenID Connect từ Google
        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();
        String name = oidcUser.getFullName();
        String picture = oidcUser.getPicture();

        // 3. Kiểm tra tính hợp lệ của định danh Google
        if (!StringUtils.hasText(sub)) {
            log.error("Google OIDC không trả về định danh Subject (sub)");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_sub", "Định danh tài khoản Google (sub) không hợp lệ.", null)
            );
        }

        if (!StringUtils.hasText(email)) {
            log.error("Google OIDC không trả về địa chỉ email cho sub={}", sub);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email", "Tài khoản Google không cung cấp địa chỉ email.", null)
            );
        }

        String normalizedEmail = email.toLowerCase().trim();

        if (!Boolean.TRUE.equals(emailVerified)) {
            log.warn("Từ chối tài khoản Google có email chưa xác thực: email={}", normalizedEmail);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email", "Địa chỉ email Google chưa được xác thực. Vui lòng xác thực với Google trước khi đăng nhập.", null)
            );
        }

        // 4. Tìm kiếm liên kết tài khoản dựa trên provider ('GOOGLE') và providerId (sub)
        Optional<UserOAuthAccount> oauthAccountOpt =
                userOAuthAccountRepository.findByProviderAndProviderId(PROVIDER_GOOGLE, sub);

        if (oauthAccountOpt.isPresent()) {
            User user = oauthAccountOpt.get().getUser();

            // Kiểm tra email của User đã liên kết có khớp với email trong Google ID Token không
            if (!user.getEmail().equalsIgnoreCase(normalizedEmail)) {
                log.warn("Google sub={} đã liên kết với user ID={}, email={}, nhưng token trả về email={}",
                        sub, user.getId(), user.getEmail(), normalizedEmail);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("oauth_conflict", "Tài khoản Google này đã được liên kết với một tài khoản khác trong hệ thống.", null)
                );
            }

            // Kiểm tra trạng thái tài khoản bị Admin khóa
            if (Boolean.FALSE.equals(user.getActive())) {
                String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : "Tài khoản đã bị vô hiệu hóa";
                log.warn("Từ chối đăng nhập Google do tài khoản nội bộ bị khóa: userId={}, reason={}", user.getId(), lockReason);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_locked", "Tài khoản của bạn đã bị khóa: " + lockReason, null)
                );
            }

            // Phân tách vai trò: Google Login tại storefront chỉ dành cho CUSTOMER
            if (user.getRole() == Role.ADMIN) {
                log.warn("Từ chối đăng nhập Google tại cổng Khách hàng cho tài khoản ADMIN: email={}", normalizedEmail);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("admin_access_denied", "Tài khoản Quản trị viên (ADMIN) không được phép đăng nhập tại cổng Khách hàng.", null)
                );
            }

            // Đồng bộ avatar mới nhất từ Google nếu có thay đổi
            if (StringUtils.hasText(picture) && !picture.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(picture);
                userRepository.save(user);
                log.info("Đã cập nhật avatar mới từ Google cho user ID={}", user.getId());
            }

            log.info("Đăng nhập Google thành công cho tài khoản đã liên kết: userId={}, email={}", user.getId(), normalizedEmail);
            return new CustomOidcUser(oidcUser, user);
        }

        // 5. Nếu chưa có liên kết: Kiểm tra xem email đã tồn tại trong hệ thống chưa
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Kiểm tra trạng thái tài khoản bị Admin khóa
            if (Boolean.FALSE.equals(existingUser.getActive())) {
                String lockReason = StringUtils.hasText(existingUser.getLockReason()) ? existingUser.getLockReason().trim() : "Tài khoản đã bị vô hiệu hóa";
                log.warn("Từ chối liên kết/đăng nhập Google do tài khoản nội bộ bị khóa: userId={}, reason={}", existingUser.getId(), lockReason);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_locked", "Tài khoản của bạn đã bị khóa: " + lockReason, null)
                );
            }

            // Phân tách vai trò: Google Login tại storefront chỉ dành cho CUSTOMER
            if (existingUser.getRole() == Role.ADMIN) {
                log.warn("Từ chối liên kết/đăng nhập Google tại cổng Khách hàng cho tài khoản ADMIN: email={}", normalizedEmail);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("admin_access_denied", "Tài khoản Quản trị viên (ADMIN) không được phép đăng nhập tại cổng Khách hàng.", null)
                );
            }

            // Kiểm tra xem providerId (sub) này có đang thuộc về tài khoản khác không (chống liên kết nhầm / cướp sub)
            Optional<UserOAuthAccount> existingProviderOpt = userOAuthAccountRepository.findByProviderAndProviderId(PROVIDER_GOOGLE, sub);
            if (existingProviderOpt.isPresent() && !existingProviderOpt.get().getUser().getId().equals(existingUser.getId())) {
                log.error("Cảnh báo bảo mật: Định danh Google sub={} đã được liên kết với user ID={} khác, không thể gán cho user ID={}",
                        sub, existingProviderOpt.get().getUser().getId(), existingUser.getId());
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("oauth_conflict", "Tài khoản Google này đã được liên kết với một tài khoản khác trong hệ thống.", null)
                );
            }

            // Thực hiện liên kết an toàn (Auto-link) do Google đã xác nhận email_verified == true
            // Đảm bảo không tạo duplicate liên kết: nếu chưa có liên kết với GOOGLE thì mới tạo
            Optional<UserOAuthAccount> userGoogleAccountOpt = userOAuthAccountRepository.findByUserIdAndProvider(existingUser.getId(), PROVIDER_GOOGLE);
            if (userGoogleAccountOpt.isEmpty()) {
                UserOAuthAccount newOAuthAccount = UserOAuthAccount.builder()
                        .user(existingUser)
                        .provider(PROVIDER_GOOGLE)
                        .providerId(sub)
                        .email(normalizedEmail)
                        .build();
                userOAuthAccountRepository.save(newOAuthAccount);
                log.info("Đã tự động liên kết thành công Google OAuth vào tài khoản nội bộ: userId={}, email={}", existingUser.getId(), normalizedEmail);
            }

            // Cập nhật email_verified = true cho tài khoản nội bộ nếu trước đó chưa xác thực
            boolean userUpdated = false;
            if (Boolean.FALSE.equals(existingUser.getEmailVerified())) {
                existingUser.setEmailVerified(true);
                existingUser.setEmailVerifiedAt(LocalDateTime.now());
                userUpdated = true;
                log.info("Tự động kích hoạt trạng thái xác thực email cho user ID={} qua Google OIDC", existingUser.getId());
            }

            // Đồng bộ avatar từ Google nếu có thay đổi
            if (StringUtils.hasText(picture) && !picture.equals(existingUser.getAvatarUrl())) {
                existingUser.setAvatarUrl(picture);
                userUpdated = true;
                log.info("Cập nhật avatar từ Google cho user ID={}", existingUser.getId());
            }

            if (userUpdated) {
                existingUser = userRepository.save(existingUser);
            }

            log.info("Đăng nhập Google thành công cho tài khoản đã liên kết: userId={}, email={}", existingUser.getId(), normalizedEmail);
            return new CustomOidcUser(oidcUser, existingUser);
        }

        // 6. Nếu email chưa tồn tại: Tạo tài khoản CUSTOMER mới và tạo liên kết OAuth
        String secureRandomPassword = generateCryptographicPassword();
        String passwordHash = passwordEncoder.encode(secureRandomPassword);

        User newUser = User.builder()
                .email(normalizedEmail)
                .password(passwordHash)
                .fullName(StringUtils.hasText(name) ? name.trim() : "Khách hàng Google")
                .avatarUrl(StringUtils.hasText(picture) ? picture.trim() : null)
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);

        UserOAuthAccount newOAuthAccount = UserOAuthAccount.builder()
                .user(newUser)
                .provider(PROVIDER_GOOGLE)
                .providerId(sub)
                .email(normalizedEmail)
                .build();

        userOAuthAccountRepository.save(newOAuthAccount);

        log.info("Tạo tài khoản CUSTOMER mới thành công qua Google OIDC: userId={}, email={}", newUser.getId(), normalizedEmail);
        return new CustomOidcUser(oidcUser, newUser);
    }

    /**
     * Sinh mật khẩu ngẫu nhiên bảo mật cao (Cryptographically Secure) bằng SecureRandom (32 bytes = 256 bits).
     * Tuyệt đối không dùng UUID. Không ghi mật khẩu ra log.
     */
    private String generateCryptographicPassword() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

