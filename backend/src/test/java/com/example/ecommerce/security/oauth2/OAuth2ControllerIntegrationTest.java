package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.dto.request.OAuth2TicketExchangeRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test cho endpoint POST /api/v1/auth/oauth2/exchange [TSK-D15-CCT].
 * Kiểm tra:
 * 1. Trao đổi ticket hợp lệ -> 200 OK kèm JSON ApiResponse và Set-Cookie HttpOnly Refresh Token.
 * 2. Trao đổi ticket không hợp lệ / hết hạn -> 401 Unauthorized theo chuẩn ApiResponse.
 * 3. Validation ticket rỗng -> 400 Bad Request.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:oauth2testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-test",
    "app.otp.hmac-secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-test"
})
class OAuth2ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("TC-CTRL-01: POST /api/v1/auth/oauth2/exchange thành công -> 200 OK + HttpOnly cookie")
    void exchangeOAuth2Ticket_Success_Returns200AndHttpOnlyCookie() throws Exception {
        String ticket = "valid-ticket-123456";
        String binding = "valid-binding-secret";

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("googleuser@gmail.com")
                .fullName("Google Customer")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .avatarUrl("https://lh3.googleusercontent.com/avatar.jpg")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mocked-jwt-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .refreshToken("mocked-raw-refresh-token")
                .build();

        when(authService.exchangeOAuth2Ticket(eq(ticket), eq(binding))).thenReturn(authResponse);

        OAuth2TicketExchangeRequest request = new OAuth2TicketExchangeRequest(ticket);

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("oauth2_binding", binding)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("googleuser@gmail.com"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://lh3.googleusercontent.com/avatar.jpg"))
                // Refresh token tuyệt đối không lộ trong JSON data
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                // Refresh token được gắn vào HttpOnly cookie
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @DisplayName("TC-CTRL-02: POST /api/v1/auth/oauth2/exchange thất bại do ticket sai/hết hạn -> 401 Unauthorized")
    void exchangeOAuth2Ticket_InvalidTicket_Returns401() throws Exception {
        String invalidTicket = "invalid-or-expired-ticket";
        String binding = "any-binding";

        when(authService.exchangeOAuth2Ticket(eq(invalidTicket), eq(binding)))
                .thenThrow(new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn"));

        OAuth2TicketExchangeRequest request = new OAuth2TicketExchangeRequest(invalidTicket);

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(new Cookie("oauth2_binding", binding)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    @DisplayName("TC-CTRL-03: POST /api/v1/auth/oauth2/exchange validation error khi ticket rỗng -> 400 Bad Request")
    void exchangeOAuth2Ticket_EmptyTicket_Returns400() throws Exception {
        OAuth2TicketExchangeRequest request = new OAuth2TicketExchangeRequest("");

        mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

