package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho OAuth2AuthenticationSuccessHandler.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomOidcUser customOidcUser;

    private ObjectMapper objectMapper;
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        handler = new OAuth2AuthenticationSuccessHandler(
                redisTemplate,
                objectMapper,
                authorizationRequestRepository
        );

        ReflectionTestUtils.setField(handler, "frontendSuccessUrl", "http://localhost:3000/oauth2/redirect");
        ReflectionTestUtils.setField(handler, "cookieSecure", false);
        ReflectionTestUtils.setField(handler, "cookieSameSite", "Lax");
    }

    @Test
    @DisplayName("1. onAuthenticationSuccess khi principal hợp lệ -> Lưu ticket Redis, set cookie và redirect ticket")
    void onAuthenticationSuccess_withValidPrincipal_generatesTicketAndRedirects() throws IOException {
        User user = User.builder()
                .id(42L)
                .email("test.user@example.com")
                .role(Role.CUSTOMER)
                .build();

        when(authentication.getPrincipal()).thenReturn(customOidcUser);
        when(customOidcUser.getUserEntity()).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        // Kiểm tra lưu ticket vào Redis với TTL 60s
        verify(valueOperations, times(1)).set(
                startsWith("auth:oauth2_ticket:"),
                anyString(),
                eq(60L),
                eq(TimeUnit.SECONDS)
        );

        // Kiểm tra cookie oauth2_binding được set
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("oauth2_binding=");
        assertThat(setCookie).contains("Path=/api/v1/auth/oauth2");
        assertThat(setCookie).contains("HttpOnly");

        // Kiểm tra dọn dẹp authorization request
        verify(authorizationRequestRepository, times(1)).removeAuthorizationRequest(request, response);

        // Kiểm tra redirect URL chứa tham số ticket
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).startsWith("http://localhost:3000/oauth2/redirect?ticket=");
    }

    @Test
    @DisplayName("2. onAuthenticationSuccess khi principal không phải CustomOidcUser -> Redirect lỗi invalid_principal")
    void onAuthenticationSuccess_withInvalidPrincipal_redirectsWithError() throws IOException {
        when(authentication.getPrincipal()).thenReturn("string-principal-not-custom-oidc-user");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("http://localhost:3000/oauth2/redirect?error=invalid_principal");
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("3. onAuthenticationSuccess khi Redis ném lỗi -> Redirect lỗi ticket_generation_failed")
    void onAuthenticationSuccess_whenRedisThrows_redirectsWithError() throws IOException {
        User user = User.builder()
                .id(42L)
                .email("test.user@example.com")
                .role(Role.CUSTOMER)
                .build();

        when(authentication.getPrincipal()).thenReturn(customOidcUser);
        when(customOidcUser.getUserEntity()).thenReturn(user);
        doThrow(new RuntimeException("Redis connection refused"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isEqualTo("http://localhost:3000/oauth2/redirect?error=ticket_generation_failed");
    }
}

