package com.example.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho JwtAuthenticationFilter.
 * Kiểm tra các nhánh điều kiện trích xuất header, phân tích claims, kiểm tra hạn token,
 * trạng thái kích hoạt của UserDetails và xử lý ngoại lệ trong Filter Chain.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Branch: Header Authorization không tồn tại -> Bỏ qua xác thực, tiếp tục filter chain")
    void doFilter_NoAuthorizationHeader_ContinuesFilterChain() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("Branch: Header Authorization rỗng hoặc chỉ chứa khoảng trắng -> Bỏ qua xác thực")
    void doFilter_BlankAuthorizationHeader_ContinuesFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "   ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("Branch: Header Authorization không bắt đầu bằng Bearer -> Bỏ qua xác thực")
    void doFilter_NonBearerAuthorizationHeader_ContinuesFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("Branch: Header Bearer nhưng chuỗi token phía sau rỗng -> Bỏ qua xác thực")
    void doFilter_BearerWithEmptyToken_ContinuesFilterChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer    ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("Branch: Token hợp lệ đầy đủ điều kiện -> Thiết lập Authentication thành công vào SecurityContext")
    void doFilter_ValidToken_SetsAuthenticationInSecurityContext() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        UserDetails userDetails = new User(
                "user@example.com",
                "password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@example.com");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Branch: Token hợp lệ nhưng SecurityContext đã có Authentication từ trước -> Không ghi đè")
    void doFilter_AuthenticationAlreadyExists_DoesNotOverwrite() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Authentication existingAuth = new UsernamePasswordAuthenticationToken("existing@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Branch: Token có subject (email) rỗng -> Bỏ qua không xác thực")
    void doFilter_EmptySubjectInClaims_DoesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("");
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Branch: UserDetails bị disabled (tài khoản bị khóa) -> Không thiết lập Authentication")
    void doFilter_UserDisabled_DoesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("locked@example.com");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        UserDetails disabledUser = new User(
                "locked@example.com",
                "password",
                false, // disabled!
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(userDetailsService.loadUserByUsername("locked@example.com")).thenReturn(disabledUser);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Branch: Email trong claims không khớp với username trong UserDetails -> Không thiết lập Authentication")
    void doFilter_EmailMismatch_DoesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user1@example.com");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3600000));
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        UserDetails userDetails = new User(
                "user2@example.com",
                "password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(userDetailsService.loadUserByUsername("user1@example.com")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Branch: Claims có expiration là null hoặc đã quá hạn -> Không thiết lập Authentication")
    void doFilter_ExpirationNullOrPast_DoesNotAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.getExpiration()).thenReturn(null);
        when(jwtUtils.parseClaims("valid.jwt.token")).thenReturn(claims);

        UserDetails userDetails = new User(
                "user@example.com",
                "password",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Branch: Token hết hạn ném ExpiredJwtException -> Bắt ngoại lệ và tiếp tục filter chain")
    void doFilter_ExpiredJwtException_HandledGracefully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        when(jwtUtils.parseClaims("expired.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token has expired"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Branch: Token lỗi định dạng ném MalformedJwtException -> Bắt ngoại lệ và tiếp tục filter chain")
    void doFilter_MalformedJwtException_HandledGracefully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer malformed.jwt.token");
        when(jwtUtils.parseClaims("malformed.jwt.token"))
                .thenThrow(new MalformedJwtException("Token is malformed"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Branch: Lỗi không xác định khi parse token -> Bắt Exception chung và tiếp tục filter chain")
    void doFilter_GeneralException_HandledGracefully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer error.jwt.token");
        when(jwtUtils.parseClaims("error.jwt.token"))
                .thenThrow(new RuntimeException("Database error or unexpected failure"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
