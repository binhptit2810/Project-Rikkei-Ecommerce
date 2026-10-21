package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.LockUserRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho UserService.
 * Kiểm tra các tính năng quản lý tài khoản, phân trang,
 * cơ chế khóa/mở khóa bảo mật, ngăn tự khóa, và đổi mật khẩu.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User customerUser;
    private UserDetails adminUserDetails;
    private UserDetails customerUserDetails;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@example.com")
                .password("$2a$12$hashedAdminPassword")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .active(true)
                .build();

        customerUser = User.builder()
                .id(2L)
                .email("customer@example.com")
                .password("$2a$12$hashedCustomerPassword")
                .fullName("Khách hàng A")
                .phone("0912345678")
                .address("456 Cầu Giấy, Hà Nội")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        adminUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();

        customerUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("customer@example.com")
                .password("password")
                .authorities("ROLE_CUSTOMER")
                .build();
    }

    @Test
    @DisplayName("FR-USER-01: Lấy hồ sơ người dùng đang đăng nhập thành công")
    void getCurrentUser_Success() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));

        UserResponse response = userService.getCurrentUser(customerUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(response.getFullName()).isEqualTo("Khách hàng A");
    }

    @Test
    @DisplayName("NFR-REL-01: Lấy hồ sơ người dùng ném ResourceNotFoundException nếu không tìm thấy trong DB")
    void getCurrentUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(customerUserDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại");
    }

    @Test
    @DisplayName("NFR-PERF-01 & FR-ADMIN-01: Lấy danh sách toàn bộ người dùng có phân trang (Pagination)")
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> mockPage = new PageImpl<>(List.of(adminUser, customerUser), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(mockPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getContent().get(1).getEmail()).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("FR-ADMIN-02 & NFR-SEC-06: Khóa tài khoản thành công - Lưu lý do và thời điểm khóa")
    void lockUser_Success() {
        LockUserRequest request = new LockUserRequest("Có dấu hiệu spam đơn hàng và giả mạo thanh toán");

        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.lockUser(2L, request, adminUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getActive()).isFalse();
        assertThat(response.getLockReason()).isEqualTo("Có dấu hiệu spam đơn hàng và giả mạo thanh toán");
        assertThat(response.getLockedAt()).isNotNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getActive()).isFalse();
        assertThat(savedUser.getLockReason()).isEqualTo("Có dấu hiệu spam đơn hàng và giả mạo thanh toán");
        assertThat(savedUser.getLockedAt()).isNotNull();
    }

    @Test
    @DisplayName("NFR-SEC-07: Admin không thể tự khóa tài khoản của chính mình (Self-Locking Prevention)")
    void lockUser_SelfLocking_ThrowsBusinessException() {
        LockUserRequest request = new LockUserRequest("Tự khóa");
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.lockUser(1L, request, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn không thể tự khóa tài khoản của chính mình");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-SEC-08: Không thể khóa tài khoản đã bị khóa từ trước")
    void lockUser_AlreadyLocked_ThrowsBusinessException() {
        customerUser.setActive(false);
        customerUser.setLockReason("Đã bị khóa trước đó");

        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        LockUserRequest request = new LockUserRequest("Khóa tiếp");

        assertThatThrownBy(() -> userService.lockUser(2L, request, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản này đã bị khóa từ trước");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-DATA-03: Khóa tài khoản thất bại nếu lý do khóa rỗng hoặc null")
    void lockUser_EmptyReason_ThrowsBusinessException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        LockUserRequest emptyRequest = new LockUserRequest("   ");

        assertThatThrownBy(() -> userService.lockUser(2L, emptyRequest, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do khóa tài khoản không được để trống");
    }

    @Test
    @DisplayName("NFR-DATA-04: Khóa tài khoản thất bại nếu lý do khóa vượt quá 500 ký tự")
    void lockUser_ReasonExceeds500Chars_ThrowsBusinessException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        String longReason = "A".repeat(501);
        LockUserRequest request = new LockUserRequest(longReason);

        assertThatThrownBy(() -> userService.lockUser(2L, request, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do khóa tài khoản không được vượt quá 500 ký tự");
    }

    @Test
    @DisplayName("NFR-REL-02: Khóa tài khoản ném ResourceNotFoundException nếu ID người dùng không tồn tại")
    void lockUser_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        LockUserRequest request = new LockUserRequest("Lý do");

        assertThatThrownBy(() -> userService.lockUser(999L, request, adminUserDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại với ID: 999");
    }

    @Test
    @DisplayName("FR-ADMIN-03 & NFR-SEC-09: Mở khóa tài khoản thành công - Xóa lý do và thời điểm khóa")
    void unlockUser_Success() {
        customerUser.setActive(false);
        customerUser.setLockReason("Lý do cũ");
        customerUser.setLockedAt(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.unlockUser(2L, adminUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getActive()).isTrue();
        assertThat(response.getLockReason()).isNull();
        assertThat(response.getLockedAt()).isNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getActive()).isTrue();
        assertThat(savedUser.getLockReason()).isNull();
        assertThat(savedUser.getLockedAt()).isNull();
    }

    @Test
    @DisplayName("NFR-SEC-10: Mở khóa tài khoản thất bại nếu tài khoản đang hoạt động bình thường")
    void unlockUser_AlreadyActive_ThrowsBusinessException() {
        customerUser.setActive(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> userService.unlockUser(2L, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tài khoản này đang hoạt động bình thường, không cần mở khóa");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-REL-03: Mở khóa tài khoản ném ResourceNotFoundException nếu ID không tồn tại")
    void unlockUser_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.unlockUser(999L, adminUserDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại với ID: 999");
    }

    @Test
    @DisplayName("FR-USER-02 & NFR-DATA-05: Cập nhật hồ sơ người dùng (Partial Update) thành công")
    void updateProfile_Success() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Khách hàng B");
        request.setPhone("0999888777");
        request.setAddress("789 Ba Đình, Hà Nội");

        UserResponse response = userService.updateProfile(customerUserDetails, request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Khách hàng B");
        assertThat(response.getPhone()).isEqualTo("0999888777");
        assertThat(response.getAddress()).isEqualTo("789 Ba Đình, Hà Nội");
    }

    @Test
    @DisplayName("FR-USER-03 & NFR-SEC-11: Đổi mật khẩu thành công - Băm mật khẩu mới bằng BCrypt")
    void changePassword_Success() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("OldPassword123@", "$2a$12$hashedCustomerPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123@", "$2a$12$hashedCustomerPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123@")).thenReturn("$2a$12$hashedNewPassword");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123@")
                .newPassword("NewPassword123@")
                .confirmPassword("NewPassword123@")
                .build();

        userService.changePassword(customerUserDetails, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$12$hashedNewPassword");
    }

    @Test
    @DisplayName("NFR-SEC-12: Đổi mật khẩu thất bại khi mật khẩu xác nhận không khớp")
    void changePassword_ConfirmPasswordMismatch_ThrowsBusinessException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123@")
                .newPassword("NewPassword123@")
                .confirmPassword("DifferentPassword123@")
                .build();

        assertThatThrownBy(() -> userService.changePassword(customerUserDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới và xác nhận mật khẩu không khớp");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-SEC-13: Đổi mật khẩu thất bại khi mật khẩu hiện tại không đúng")
    void changePassword_WrongCurrentPassword_ThrowsBusinessException() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("WrongPassword123@", "$2a$12$hashedCustomerPassword")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123@")
                .newPassword("NewPassword123@")
                .confirmPassword("NewPassword123@")
                .build();

        assertThatThrownBy(() -> userService.changePassword(customerUserDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu hiện tại không chính xác");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("NFR-SEC-14: Đổi mật khẩu thất bại khi mật khẩu mới trùng với mật khẩu cũ")
    void changePassword_SameAsOldPassword_ThrowsBusinessException() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("OldPassword123@", "$2a$12$hashedCustomerPassword")).thenReturn(true);
        when(passwordEncoder.matches("OldPassword123@", "$2a$12$hashedCustomerPassword")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123@")
                .newPassword("OldPassword123@")
                .confirmPassword("OldPassword123@")
                .build();

        assertThatThrownBy(() -> userService.changePassword(customerUserDetails, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Mật khẩu mới không được trùng với mật khẩu hiện tại");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Branch: Khóa tài khoản thành công khi adminUserDetails là null (log UNKNOWN)")
    void lockUser_WithNullAdminUserDetails_Success() {
        LockUserRequest request = new LockUserRequest("Lý do hợp lệ");
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.lockUser(2L, request, null);

        assertThat(response).isNotNull();
        assertThat(response.getActive()).isFalse();
        assertThat(response.getLockReason()).isEqualTo("Lý do hợp lệ");
    }

    @Test
    @DisplayName("Branch: Khóa tài khoản với request là null -> ném BusinessException lý do trống")
    void lockUser_WithNullRequest_ThrowsBusinessException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> userService.lockUser(2L, null, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do khóa tài khoản không được để trống");
    }

    @Test
    @DisplayName("Branch: Khóa tài khoản với request.getReason() là null -> ném BusinessException lý do trống")
    void lockUser_WithNullReason_ThrowsBusinessException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));

        LockUserRequest request = new LockUserRequest(null);

        assertThatThrownBy(() -> userService.lockUser(2L, request, adminUserDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do khóa tài khoản không được để trống");
    }

    @Test
    @DisplayName("Branch: Mở khóa tài khoản khi adminUserDetails là null (log UNKNOWN)")
    void unlockUser_WithNullAdminUserDetails_Success() {
        customerUser.setActive(false);
        customerUser.setLockReason("Lý do");
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.unlockUser(2L, null);

        assertThat(response).isNotNull();
        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("Branch: Cập nhật hồ sơ với tất cả các trường null -> giữ nguyên toàn bộ giá trị cũ")
    void updateProfile_WithAllNullFields_KeepsOriginalValues() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(); // fullName=null, phone=null, address=null

        UserResponse response = userService.updateProfile(customerUserDetails, request);

        assertThat(response.getFullName()).isEqualTo("Khách hàng A");
        assertThat(response.getPhone()).isEqualTo("0912345678");
        assertThat(response.getAddress()).isEqualTo("456 Cầu Giấy, Hà Nội");
    }

    @Test
    @DisplayName("Branch: Cập nhật hồ sơ với fullName là khoảng trắng -> giữ nguyên fullName cũ")
    void updateProfile_WithBlankFullName_KeepsOriginalFullName() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("     ");

        UserResponse response = userService.updateProfile(customerUserDetails, request);

        assertThat(response.getFullName()).isEqualTo("Khách hàng A");
    }
}

