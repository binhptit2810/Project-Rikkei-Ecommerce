package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
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
     *   3. Nếu đúng → tạo JWT và trả AuthResponse
     *
     * @param request thông tin đăng nhập đã được validate
     * @return AuthResponse chứa token và thông tin user
     * @throws org.springframework.security.authentication.BadCredentialsException nếu sai thông tin
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // authenticationManager ném BadCredentialsException nếu sai → GlobalExceptionHandler xử lý
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        log.info("Đăng nhập thành công: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .build();
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
