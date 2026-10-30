package com.example.ecommerce.config;

import com.example.ecommerce.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test cho cấu hình CORS trên toàn bộ hệ thống.
 * Xác minh:
 * - Origin được phép: http://localhost:3000 (và http://localhost:5173).
 * - Origin không được phép: không được cấp header Access-Control-Allow-Origin.
 * - Preflight OPTIONS request được Spring Security xử lý đúng trước khi tới filter xác thực.
 * - Các HTTP methods: GET, POST, PUT, DELETE, PATCH, OPTIONS.
 * - Header Authorization và Content-Type được phép.
 * - allowCredentials = true.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_cors;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000",
    "cors.allowed-origins=http://localhost:3000,http://localhost:5173"
})
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://evil-attacker.com";

    @Test
    @DisplayName("CORS-01: Preflight OPTIONS request từ origin http://localhost:3000 thành công và trả đầy đủ header CORS")
    void preflightOptions_AllowedOrigin_ReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    @DisplayName("CORS-02: Preflight OPTIONS request từ origin không được phép không được cấp Access-Control-Allow-Origin")
    void preflightOptions_DisallowedOrigin_DoesNotReturnAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("CORS-03: Request GET công khai từ http://localhost:3000 nhận được header CORS")
    void getPublicEndpoint_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS-04: Request POST từ http://localhost:3000 nhận được header CORS")
    void postEndpoint_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\",\"password\":\"wrongpass\"}"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CORS-05: Request PUT hồ sơ cá nhân từ http://localhost:3000 nhận được header CORS")
    void putEndpoint_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test User\"}"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("CORS-06: Request PATCH endpoint Admin từ http://localhost:3000 nhận được header CORS")
    void patchEndpoint_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/999/unlock")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS-07: Preflight OPTIONS request tới endpoint yêu cầu xác thực không bị chặn bởi JWT filter")
    void preflightOptions_ProtectedEndpoint_NotBlockedByAuth() throws Exception {
        mockMvc.perform(options("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("CORS-08: Preflight OPTIONS request tới endpoint Admin không bị chặn bởi phân quyền")
    void preflightOptions_AdminEndpoint_NotBlockedByRole() throws Exception {
        mockMvc.perform(options("/api/v1/admin/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @MockBean
    private com.example.ecommerce.service.ProductService productService;

    @Test
    @DisplayName("CORS-09: Request bị lỗi 401 Unauthorized vẫn giữ nguyên header CORS nếu origin hợp lệ")
    void unauthenticatedRequest_Returns401WithCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("CORS-10: Request bị lỗi 403 Forbidden vẫn giữ nguyên header CORS nếu origin hợp lệ")
    void forbiddenRequest_Returns403WithCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("CORS-11: Request DELETE thực tế từ http://localhost:3000 nhận được đầy đủ header CORS")
    void deleteEndpoint_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/products/1")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mock-admin-token"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("CORS-12: Preflight OPTIONS request cho method DELETE từ http://localhost:3000 thành công")
    void preflightOptions_ForDeleteMethod_ReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/admin/products/1")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
