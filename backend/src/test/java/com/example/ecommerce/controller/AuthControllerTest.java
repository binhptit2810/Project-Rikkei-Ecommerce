package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho AuthController.
 * Kiểm tra xác thực (Authentication), Bean Validation,
 * Xử lý lỗi tập trung khi sai thông tin (401) hoặc tài khoản bị khóa (403).
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("FR-AUTH-03: Đăng ký tài khoản với dữ liệu hợp lệ trả 201 CREATED")
    void register_ValidRequest_Returns201AndToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("levanc@example.com");
        request.setPassword("Password123@");
        request.setPhone("0912345678");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-token-xyz")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.builder()
                        .id(10L)
                        .email("levanc@example.com")
                        .fullName("Lê Văn C")
                        .role(Role.CUSTOMER)
                        .active(true)
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-token-xyz"))
                .andExpect(jsonPath("$.data.user.email").value("levanc@example.com"));
    }

    @Test
    @DisplayName("NFR-DATA-07: Đăng ký với email sai định dạng bị Bean Validation trả 400 BAD REQUEST")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("not-an-email");
        request.setPassword("Password123@");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").value("Email không đúng định dạng"));
    }

    @Test
    @DisplayName("NFR-DATA-08: Đăng ký với mật khẩu ngắn hơn 6 ký tự bị Bean Validation trả 400 BAD REQUEST")
    void register_ShortPassword_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Lê Văn C");
        request.setEmail("valid@example.com");
        request.setPassword("12345"); // ít hơn 6 ký tự

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").value("Mật khẩu phải từ 6 đến 100 ký tự"));
    }

    @Test
    @DisplayName("FR-AUTH-04: Đăng nhập thành công trả 200 OK kèm theo Bearer JWT")
    void login_ValidCredentials_Returns200AndToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("valid@example.com");
        request.setPassword("Password123@");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-valid-login-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.builder()
                        .id(1L)
                        .email("valid@example.com")
                        .role(Role.CUSTOMER)
                        .active(true)
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock-valid-login-token"));
    }

    @Test
    @DisplayName("NFR-SEC-29: Đăng nhập với tài khoản bị khóa trả 403 FORBIDDEN kèm lý do khóa")
    void login_LockedAccount_Returns403WithLockReason() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("Password123@");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new LockedAccountException("Tài khoản của bạn đã bị khóa.", "Vi phạm quy chế bán hàng gian lận"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa."))
                .andExpect(jsonPath("$.data.reason").value("Vi phạm quy chế bán hàng gian lận"))
                .andExpect(jsonPath("$.data.lockReason").value("Vi phạm quy chế bán hàng gian lận"));
    }

    @Test
    @DisplayName("NFR-SEC-30: Đăng nhập sai email hoặc mật khẩu trả 401 UNAUTHORIZED")
    void login_WrongCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }
}
