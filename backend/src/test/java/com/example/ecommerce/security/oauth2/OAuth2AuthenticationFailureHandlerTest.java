package com.example.ecommerce.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit Test cho OAuth2AuthenticationFailureHandler.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    private OAuth2AuthenticationFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2AuthenticationFailureHandler(authorizationRequestRepository);
        ReflectionTestUtils.setField(failureHandler, "frontendFailureUrl", "http://localhost:3000/login");
    }

    @Test
    @DisplayName("1. onAuthenticationFailure với OAuth2AuthenticationException có error description -> Redirect kèm error param")
    void onAuthenticationFailure_withOAuth2Exception_redirectsWithDescription() throws IOException {
        OAuth2Error error = new OAuth2Error("access_denied", "Người dùng từ chối cấp quyền", null);
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, exception);

        verify(authorizationRequestRepository, times(1)).removeAuthorizationRequest(request, response);
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).contains("http://localhost:3000/login?oauth2Error=");
        String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
        assertThat(decodedUrl).contains("Người dùng từ chối cấp quyền");
    }

    @Test
    @DisplayName("2. onAuthenticationFailure với AuthenticationException chung -> Redirect kèm message")
    void onAuthenticationFailure_withGeneralAuthException_redirectsWithMessage() throws IOException {
        BadCredentialsException exception = new BadCredentialsException("Sai thông tin chứng thực");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, exception);

        verify(authorizationRequestRepository, times(1)).removeAuthorizationRequest(request, response);
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).contains("http://localhost:3000/login?oauth2Error=");
    }

    @Test
    @DisplayName("3. onAuthenticationFailure khi exception không có message -> Redirect kèm thông báo mặc định")
    void onAuthenticationFailure_withNullMessage_usesDefaultMessage() throws IOException {
        BadCredentialsException exception = new BadCredentialsException(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, exception);

        verify(authorizationRequestRepository, times(1)).removeAuthorizationRequest(request, response);
        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).contains("oauth2Error");
    }
}
