package com.example.ecommerce.security;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tích hợp Spring Security với repository người dùng.
 *
 * Spring Security gọi loadUserByUsername(email) khi:
 *   1. DaoAuthenticationProvider xác thực trong AuthService.login()
 *   2. JwtAuthenticationFilter tải UserDetails để set SecurityContext
 *
 * Authority mapping:
 *   Role.CUSTOMER → GrantedAuthority("ROLE_CUSTOMER")
 *   Role.ADMIN    → GrantedAuthority("ROLE_ADMIN")
 *
 * Convention này cho phép dùng:
 *   @PreAuthorize("hasRole('CUSTOMER')")  — kiểm tra ROLE_CUSTOMER
 *   @PreAuthorize("hasRole('ADMIN')")     — kiểm tra ROLE_ADMIN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy user với email: {}", email);
                    // Không tiết lộ lý do cụ thể cho client để tránh user enumeration
                    return new UsernameNotFoundException("Thông tin xác thực không hợp lệ");
                });

        boolean isEnabled = Boolean.TRUE.equals(user.getActive());
        if (!isEnabled) {
            log.warn("Tài khoản đã bị vô hiệu hóa: {}", email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .disabled(!isEnabled)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
