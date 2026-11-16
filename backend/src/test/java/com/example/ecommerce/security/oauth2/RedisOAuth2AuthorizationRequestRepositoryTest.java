package com.example.ecommerce.security.oauth2;

import jakarta.servlet.http.Cookie;
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
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RedisOAuth2AuthorizationRequestRepository.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT
 */
@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationRequestRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        repository = new RedisOAuth2AuthorizationRequestRepository(redisTemplate);
    }

    private OAuth2AuthorizationRequest createSampleRequest(String state) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state(state)
                .build();
    }

    @Test
    @DisplayName("1. saveAuthorizationRequest với request hợp lệ -> Lưu vào Redis và set cookie state")
    void saveAuthorizationRequest_withValidRequest_savesToRedisAndSetsCookie() {
        OAuth2AuthorizationRequest authRequest = createSampleRequest("sample-state-123");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, request, response);

        verify(valueOperations, times(1)).set(
                eq("auth:oauth2_req:sample-state-123"),
                anyString(),
                eq(180L),
                eq(TimeUnit.SECONDS)
        );

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("oauth2_auth_state=sample-state-123");
        assertThat(setCookieHeader).contains("HttpOnly");
    }

    @Test
    @DisplayName("2. saveAuthorizationRequest với null request -> Xóa authorization request")
    void saveAuthorizationRequest_withNull_removesRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "existing-state");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        verify(redisTemplate, times(1)).delete("auth:oauth2_req:existing-state");
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    @Test
    @DisplayName("3. saveAuthorizationRequest với request không có state -> Bỏ qua không lưu")
    void saveAuthorizationRequest_withoutState_doesNotSave() {
        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .build(); // Không set state

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, request, response);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("4. loadAuthorizationRequest với state hợp lệ khớp cookie -> Đọc và deserialize thành công")
    void loadAuthorizationRequest_withMatchingCookie_returnsDeserializedRequest() throws Exception {
        OAuth2AuthorizationRequest original = createSampleRequest("valid-state-abc");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            oos.flush();
        }
        String serialized = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());

        when(valueOperations.get("auth:oauth2_req:valid-state-abc")).thenReturn(serialized);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "valid-state-abc");
        request.setCookies(new Cookie("oauth2_auth_state", "valid-state-abc"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("valid-state-abc");
        assertThat(loaded.getClientId()).isEqualTo("test-client-id");
    }

    @Test
    @DisplayName("5. loadAuthorizationRequest khi state URL không khớp cookie -> Trả về null (chống CSRF)")
    void loadAuthorizationRequest_whenStateMismatch_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "url-state");
        request.setCookies(new Cookie("oauth2_auth_state", "different-cookie-state"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("6. loadAuthorizationRequest khi không có state trong request -> Trả về null")
    void loadAuthorizationRequest_withoutState_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("7. loadAuthorizationRequest khi Redis không tìm thấy key -> Trả về null")
    void loadAuthorizationRequest_whenNotFoundInRedis_returnsNull() {
        when(valueOperations.get("auth:oauth2_req:expired-state")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "expired-state");
        request.setCookies(new Cookie("oauth2_auth_state", "expired-state"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
    }

    @Test
    @DisplayName("8. loadAuthorizationRequest khi dữ liệu trong Redis bị lỗi -> Trả về null")
    void loadAuthorizationRequest_whenCorruptedData_returnsNull() {
        when(valueOperations.get("auth:oauth2_req:corrupt-state")).thenReturn("invalid-base64-content!!!");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "corrupt-state");
        request.setCookies(new Cookie("oauth2_auth_state", "corrupt-state"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
    }

    @Test
    @DisplayName("9. removeAuthorizationRequest -> Xóa Redis, clear cookie và trả về original request")
    void removeAuthorizationRequest_deletesRedisAndClearsCookie() throws Exception {
        OAuth2AuthorizationRequest original = createSampleRequest("remove-state");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            oos.flush();
        }
        String serialized = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());

        when(valueOperations.get("auth:oauth2_req:remove-state")).thenReturn(serialized);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("state", "remove-state");
        request.setCookies(new Cookie("oauth2_auth_state", "remove-state"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("remove-state");
        verify(redisTemplate, times(1)).delete("auth:oauth2_req:remove-state");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).contains("Max-Age=0");
    }
}

