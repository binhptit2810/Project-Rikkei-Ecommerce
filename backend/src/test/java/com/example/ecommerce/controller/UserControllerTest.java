package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.UpdateProfileRequest;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho UserController.
 * Kiểm tra các endpoint quản lý hồ sơ cá nhân, đổi mật khẩu và bảo mật xác thực người dùng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private com.example.ecommerce.service.RefreshTokenService refreshTokenService;

    @MockBean
    private com.example.ecommerce.repository.UserRepository userRepository;

    @Autowired
    private com.example.ecommerce.security.JwtUtils jwtUtils;

    @Test
    @DisplayName("NFR-SEC-04: Access Token JWT của tài khoản đã bị khóa (active=false) bị từ chối 401 UNAUTHORIZED")
    void requestWithJwt_LockedUser_Returns401() throws Exception {
        com.example.ecommerce.entity.User lockedUser = com.example.ecommerce.entity.User.builder()
                .id(99L)
                .email("jwt.locked@example.com")
                .password("$2a$12$someHashedPasswordHere")
                .fullName("Locked Customer")
                .active(false)
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail("jwt.locked@example.com")).thenReturn(java.util.Optional.of(lockedUser));

        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        "jwt.locked@example.com",
                        "password",
                        false,
                        true, true, true,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))
                );

        String token = jwtUtils.generateToken(userDetails);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Chưa đăng nhập hoặc token hết hạn"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("FR-USER-01: Lấy thông tin cá nhân hiện tại GET /api/v1/users/me thành công trả 200 OK")
    void getCurrentUser_Authenticated_Returns200() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Khách hàng A")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userService.getCurrentUser(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Khách hàng A"));
    }

    @Test
    @DisplayName("NFR-SEC: Truy cập GET /api/v1/users/me khi chưa đăng nhập trả 401 UNAUTHORIZED")
    void getCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("FR-USER-02: Cập nhật thông tin cá nhân PUT /api/v1/users/me thành công trả 200 OK")
    void updateProfile_ValidRequest_Returns200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Tên Mới");
        request.setPhone("0987654321");
        request.setAddress("Hà Nội");

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Tên Mới")
                .phone("0987654321")
                .address("Hà Nội")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userService.updateProfile(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ thành công"))
                .andExpect(jsonPath("$.data.fullName").value("Tên Mới"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("NFR-DATA: Cập nhật thông tin với số điện thoại sai định dạng trả 400 BAD REQUEST")
    void updateProfile_InvalidPhone_Returns400() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("12345"); // Sai định dạng SĐT Việt Nam

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("FR-USER-03: Đổi mật khẩu PUT /api/v1/users/me/password thành công trả 200 OK")
    void changePassword_ValidRequest_Returns200() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123@")
                .newPassword("NewPassword456@")
                .confirmPassword("NewPassword456@")
                .build();

        doNothing().when(userService).changePassword(any(), any());

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("NFR-DATA: Đổi mật khẩu với mật khẩu mới rỗng trả 400 BAD REQUEST")
    void changePassword_BlankNewPassword_Returns400() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123@")
                .newPassword("") // Rỗng
                .confirmPassword("")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
