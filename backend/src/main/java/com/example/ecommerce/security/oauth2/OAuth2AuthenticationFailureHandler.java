package com.example.ecommerce.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Xử lý khi xác thực Google OAuth2/OIDC thất bại hoặc bị từ chối.
 * Chuyển hướng người dùng về trang đăng nhập Frontend kèm thông báo lỗi thân thiện.
 *
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.oauth2.frontend-failure-url:http://localhost:3000/login}")
    private String frontendFailureUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        // Dọn dẹp trạng thái và cookie authorization request
        authorizationRequestRepository.removeAuthorizationRequest(request, response);

        String errorMessage = "Đăng nhập bằng tài khoản Google thất bại. Vui lòng thử lại.";

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            OAuth2Error error = oAuth2Exception.getError();
            if (error != null && error.getDescription() != null) {
                errorMessage = error.getDescription();
            }
        } else if (exception.getMessage() != null) {
            errorMessage = exception.getMessage();
        }

        log.warn("Xác thực Google OAuth2 thất bại: {}", errorMessage);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendFailureUrl)
                .queryParam("oauth2Error", errorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

