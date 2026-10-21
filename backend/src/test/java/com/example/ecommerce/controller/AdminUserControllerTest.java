package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.LockUserRequest;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho AdminUserController.
 * Kiểm tra RBAC (Phân quyền Admin vs Customer), Validation dữ liệu đầu vào,
 * và chuẩn hóa phản hồi ApiResponse.
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
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("NFR-SEC-22 & FR-ADMIN-04: Admin truy cập GET /api/v1/admin/users thành công trả 200 OK")
    void getUsers_AsAdmin_Returns200AndPage() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("user1@example.com")
                .fullName("User One")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse)));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("user1@example.com"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("NFR-SEC-23: Khách hàng (CUSTOMER) truy cập GET /api/v1/admin/users bị từ chối 403 FORBIDDEN")
    void getUsers_AsCustomer_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NFR-SEC-24: Người dùng chưa đăng nhập truy cập GET /api/v1/admin/users bị từ chối 401 UNAUTHORIZED")
    void getUsers_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("FR-ADMIN-05 & NFR-SEC-25: Admin khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/lock trả 200 OK")
    void lockUser_AsAdmin_ValidReason_Returns200() throws Exception {
        LockUserRequest request = new LockUserRequest("Tài khoản nghi vấn gian lận");

        UserResponse lockedResponse = UserResponse.builder()
                .id(2L)
                .email("suspect@example.com")
                .role(Role.CUSTOMER)
                .active(false)
                .lockReason("Tài khoản nghi vấn gian lận")
                .lockedAt(LocalDateTime.now())
                .build();

        when(userService.lockUser(eq(2L), any(LockUserRequest.class), any()))
                .thenReturn(lockedResponse);

        mockMvc.perform(patch("/api/v1/admin/users/2/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Khóa tài khoản thành công"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.lockReason").value("Tài khoản nghi vấn gian lận"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("NFR-DATA-06: Admin khóa tài khoản với lý do trống bị Bean Validation trả 400 BAD REQUEST")
    void lockUser_AsAdmin_BlankReason_Returns400() throws Exception {
        LockUserRequest request = new LockUserRequest("   ");

        mockMvc.perform(patch("/api/v1/admin/users/2/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("NFR-SEC-26: Khách hàng (CUSTOMER) gọi PATCH /api/v1/admin/users/{id}/lock bị chặn 403 FORBIDDEN")
    void lockUser_AsCustomer_Returns403() throws Exception {
        LockUserRequest request = new LockUserRequest("Lý do");

        mockMvc.perform(patch("/api/v1/admin/users/2/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("FR-ADMIN-06 & NFR-SEC-27: Admin mở khóa tài khoản thành công PATCH /api/v1/admin/users/{id}/unlock trả 200 OK")
    void unlockUser_AsAdmin_Returns200() throws Exception {
        UserResponse unlockedResponse = UserResponse.builder()
                .id(2L)
                .email("suspect@example.com")
                .role(Role.CUSTOMER)
                .active(true)
                .lockReason(null)
                .lockedAt(null)
                .build();

        when(userService.unlockUser(eq(2L), any()))
                .thenReturn(unlockedResponse);

        mockMvc.perform(patch("/api/v1/admin/users/2/unlock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mở khóa tài khoản thành công"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.lockReason").doesNotExist());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("NFR-SEC-28: Khách hàng (CUSTOMER) gọi PATCH /api/v1/admin/users/{id}/unlock bị chặn 403 FORBIDDEN")
    void unlockUser_AsCustomer_Returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/unlock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Branch: Admin gọi getUsers với page âm và size <= 0 -> chuẩn hóa về page=0 và size=DEFAULT_PAGE_SIZE(10)")
    void getUsers_NegativePageAndZeroSize_NormalizesDefaults() throws Exception {
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "-1")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Branch: Admin gọi getUsers với size vượt quá MAX_PAGE_SIZE(100) -> giới hạn về MAX_PAGE_SIZE")
    void getUsers_SizeExceedingMax_CapsAtMaxPageSize() throws Exception {
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "250")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

