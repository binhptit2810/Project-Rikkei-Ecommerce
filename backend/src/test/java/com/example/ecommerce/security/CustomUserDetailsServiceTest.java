package com.example.ecommerce.security;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho CustomUserDetailsService.
 * Kiểm tra việc ánh xạ quyền (Authorities) và vô hiệu hóa tài khoản bị khóa.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User activeUser;
    private User lockedUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("active@example.com")
                .password("$2a$12$hashedPassword")
                .role(Role.ADMIN)
                .active(true)
                .build();

        lockedUser = User.builder()
                .id(2L)
                .email("locked@example.com")
                .password("$2a$12$hashedPassword")
                .role(Role.CUSTOMER)
                .active(false)
                .lockReason("Vi phạm chính sách bảo mật")
                .build();
    }

    @Test
    @DisplayName("FR-SEC-01: Tải UserDetails thành công cho tài khoản đang hoạt động")
    void loadUserByUsername_ActiveUser_ReturnsEnabledUserDetails() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("active@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("active@example.com");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("NFR-SEC-20: Tài khoản bị khóa có trạng thái isEnabled = false trong UserDetails")
    void loadUserByUsername_LockedUser_ReturnsDisabledUserDetails() {
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(lockedUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("locked@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("locked@example.com");
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("NFR-SEC-21: Người dùng không tồn tại ném UsernameNotFoundException bảo mật")
    void loadUserByUsername_NotFound_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("notfound@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Thông tin xác thực không hợp lệ");
    }
}
