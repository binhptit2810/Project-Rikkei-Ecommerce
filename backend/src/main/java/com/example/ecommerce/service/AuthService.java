package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Nghiệp vụ xác thực người dùng: đăng ký và đăng nhập.
 *
 * Nguyên tắc bảo mật:
 *   - Mật khẩu được hash BCrypt (cost=12) trước khi lưu DB.
 *   - Email được chuẩn hóa lowercase + trim để tránh đăng ký trùng.
 *   - Không ghi password hoặc JWT vào log.
 *   - Lỗi đăng nhập sai (BadCredentialsException) được xử lý tại GlobalExceptionHandler → 401.
 *   - Tài khoản mới mặc định role=CUSTOMER, active=true.
 *   - Không cho phép client tự gán role ADMIN qua API đăng ký.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    /**
     * Đăng ký tài khoản mới.
     *
     * Luồng:
     *   1. Kiểm tra email chưa tồn tại
     *   2. Hash password bằng BCrypt
     *   3. Lưu User với role=CUSTOMER, active=true
     *   4. Tạo JWT và trả AuthResponse
     *
     * @param request thông tin đăng ký đã được validate
     * @return AuthResponse chứa token và thông tin user
     * @throws BusinessException nếu email đã tồn tại
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email đã được sử dụng: " + normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(Role.CUSTOMER)  // Luôn là CUSTOMER — không tin client
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("Đăng ký tài khoản mới: {}", normalizedEmail);

        // Tạo UserDetails để generate token
        UserDetails userDetails = buildUserDetailsForToken(user);
        String token = jwtUtils.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Đăng nhập bằng email và mật khẩu.
     *
     * Luồng:
     *   1. Gọi authenticationManager.authenticate()
     *      → DaoAuthenticationProvider gọi CustomUserDetailsService.loadUserByUsername()
     *      → So sánh password với BCrypt hash trong DB
     *   2. Nếu sai → ném BadCredentialsException → GlobalExceptionHandler → 401
     *   3. Nếu đúng → tạo JWT Access Token, tạo Refresh Token trong Redis và trả AuthResponse
     *
     * @param request thông tin đăng nhập đã được validate
     * @return AuthResponse chứa token và thông tin user
     * @throws org.springframework.security.authentication.BadCredentialsException nếu sai thông tin
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Kiểm tra trạng thái tài khoản: Nếu tài khoản đã bị khóa, chặn ngay lập tức và ném LockedAccountException kèm lý do thực tế từ DB
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null && Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Đăng nhập thất bại do tài khoản bị khóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        // 2. Xác thực thông tin đăng nhập (email & password)
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (DisabledException ex) {
            String lockReason = (user != null && StringUtils.hasText(user.getLockReason()))
                    ? user.getLockReason().trim()
                    : null;
            log.warn("Đăng nhập thất bại do tài khoản bị vô hiệu hóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        if (user == null) {
            user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));
        }

        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Đăng nhập thành công: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Làm mới Access Token bằng Refresh Token (Token Rotation) - BUG-02 Fix.
     *
     * Luồng:
     *   1. Đọc và kiểm tra tính hợp lệ của Refresh Token từ Redis trước khi rotation
     *   2. Xác minh người dùng trong MySQL trước khi rotation (user tồn tại, active = true)
     *   3. Hòa giải trạng thái: kiểm tra mật khẩu có bị thay đổi sau khi token được cấp không
     *   4. Chỉ khi DB và người dùng hợp lệ mới thực hiện rotation nguyên tử trên Redis
     *   5. Sinh Access Token JWT mới (15 phút) và trả về AuthResponse kèm Refresh Token mới
     *
     * Lưu ý kiến trúc: Kiểm tra MySQL rồi xoay vòng Redis là quy trình 2 bước với fail-closed,
     * không phải giao dịch phân tán 2PC. Nếu DB lỗi, token chưa bị xoay và client có thể thử lại an toàn.
     *
     * @param rawRefreshToken chuỗi refresh token nhận từ cookie HttpOnly
     * @return AuthResponse chứa accessToken mới và refreshToken mới
     */
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc không tồn tại");
        }

        // 1. Kiểm tra tính hợp lệ của Refresh Token và đọc metadata từ Redis trước khi rotation
        RefreshTokenService.RefreshTokenData tokenData = refreshTokenService.getTokenData(rawRefreshToken);

        // 2. Xác minh người dùng trong MySQL trước khi rotation
        User user = userRepository.findById(tokenData.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        if (Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Làm mới token thất bại do tài khoản bị khóa: userId={}, lý do={}", user.getId(), lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        // 3. Hòa giải trạng thái DB - Redis: Nếu mật khẩu đã thay đổi sau khi token được cấp -> từ chối
        if (tokenData.getPasswordHash() != null && !tokenData.getPasswordHash().equals(user.getPassword())) {
            log.warn("Refresh token bị từ chối do mật khẩu đã thay đổi sau khi token được cấp: userId={}", user.getId());
            throw new BadCredentialsException("Mật khẩu tài khoản đã thay đổi. Vui lòng đăng nhập lại.");
        }

        // 4. Chỉ khi DB và người dùng hợp lệ mới thực hiện rotation nguyên tử trên Redis
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.rotateRefreshToken(rawRefreshToken);

        // 5. Sinh JWT Access Token mới và trả về
        UserDetails userDetails = buildUserDetailsForToken(user);
        String newAccessToken = jwtUtils.generateToken(userDetails);

        log.info("Làm mới Access Token thành công cho user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(rotationResult.getRawRefreshToken())
                .build();
    }

    /**
     * Đăng xuất: thu hồi Refresh Token trong Redis.
     *
     * @param rawRefreshToken token thô nhận từ cookie
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    // ---------- Private helpers ----------

    /**
     * Tạo Spring Security UserDetails từ User entity để generate JWT token.
     * Chỉ dùng sau khi đăng ký (trước khi có Authentication từ authenticationManager).
     */
    private UserDetails buildUserDetailsForToken(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
