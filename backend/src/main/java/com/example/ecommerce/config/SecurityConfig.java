package com.example.ecommerce.config;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.security.CustomUserDetailsService;
import com.example.ecommerce.security.JwtAuthenticationFilter;
import com.example.ecommerce.security.oauth2.CustomOidcUserService;
import com.example.ecommerce.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.ecommerce.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.example.ecommerce.security.oauth2.RedisOAuth2AuthorizationRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Cấu hình Spring Security cho toàn bộ ứng dụng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Kiến trúc xác thực:
 *   - Stateless: KHÔNG sử dụng session (SessionCreationPolicy.STATELESS)
 *   - JWT Bearer Token cho mọi request được bảo vệ
 *   - DaoAuthenticationProvider xác thực username/password khi đăng nhập
 *
 * Phân quyền endpoint:
 *   - PUBLIC:    /api/health, /api/v1/health, /actuator/**
 *                POST /api/v1/auth/register, POST /api/v1/auth/login
 *                GET /api/v1/products/**, GET /api/v1/categories/**  (xem mà không cần đăng nhập)
 *   - ADMIN:     /api/v1/admin/**
 *   - AUTH:      Mọi endpoint còn lại yêu cầu đã đăng nhập
 *
 * Contract role cho các module khác:
 *   - hasRole('CUSTOMER') → kiểm tra authority ROLE_CUSTOMER
 *   - hasRole('ADMIN')    → kiểm tra authority ROLE_ADMIN
 *   - Dùng @PreAuthorize tại Service hoặc Controller
 */
// [TSK-D02-CCT]
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // Cho phép @PreAuthorize, @PostAuthorize trong Service/Controller
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;  // Spring Boot auto-configure bean
    private final CustomOidcUserService customOidcUserService;
    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper,
                          @Lazy CustomOidcUserService customOidcUserService,
                          @Lazy RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                          @Lazy OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                          @Lazy OAuth2AuthenticationFailureHandler oauth2FailureHandler,
                          @Lazy ClientRegistrationRepository clientRegistrationRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.customOidcUserService = customOidcUserService;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Constructor tương thích ngược cho unit test.
     */
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper,
                          CustomOidcUserService customOidcUserService,
                          RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                          OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                          OAuth2AuthenticationFailureHandler oauth2FailureHandler) {
        this(userDetailsService, jwtAuthenticationFilter, objectMapper,
             customOidcUserService, authorizationRequestRepository,
             oauth2SuccessHandler, oauth2FailureHandler, null);
    }

    // =========================================================================
    // Authentication beans
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost=12 khớp với hash BCrypt trong 02-sample-data.sql
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — inject vào AuthService để xác thực login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // =========================================================================
    // Security Filter Chain
    // =========================================================================

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: tắt vì dùng JWT stateless (không có cookie session)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS: delegate sang CorsConfig (WebMvcConfigurer) hiện có
            .cors(Customizer.withDefaults())

            // Session: STATELESS — không tạo HTTP session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Phân quyền endpoint
            .authorizeHttpRequests(auth -> auth
                // --- Public: không cần token ---
                .requestMatchers("/api/health", "/api/v1/health").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/send-registration-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                // [TSK-D10-CCT] API Quên mật khẩu và Đặt lại mật khẩu bằng OTP qua Email
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
                // API Xác thực email đăng ký và Gửi lại mã OTP
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-verification").permitAll()
                // VNPay callback (Return URL) & IPN webhook endpoint (không yêu cầu JWT đăng nhập)
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/vnpay-callback", "/payments/vnpay-callback").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/vnpay-ipn", "/payments/vnpay-ipn").permitAll()
                // GET sản phẩm và danh mục: khách vãng lai được xem
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                // Static assets & uploads
                .requestMatchers("/api/v1/uploads/**", "/uploads/**").permitAll()
                // Coupons: xem và validate mã giảm giá
                .requestMatchers(HttpMethod.POST, "/api/v1/coupons/validate").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/coupons/**").permitAll()
                // [TSK-D11-CCT] Chatbot AI tư vấn bán hàng: cho phép khách vãng lai và người dùng truy cập
                .requestMatchers("/api/v1/chat", "/api/v1/chat/**").permitAll()
                // [TSK-D15-CCT] Google OAuth2 endpoints & One-Time Ticket Exchange
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/oauth2/exchange").permitAll()
                // [AUTH0-GOOGLE] Auth0 ID Token Exchange endpoint
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/auth0/exchange").permitAll()

                // --- Admin only ---
                .requestMatchers("/api/v1/admin/**", "/admin/**").hasRole("ADMIN")

                // --- Mọi request còn lại: phải đăng nhập ---
                .anyRequest().authenticated()
            )

            // Xử lý lỗi xác thực / phân quyền — trả ApiResponse JSON nhất quán
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                        objectMapper.writeValueAsString(
                            ApiResponse.error("Chưa đăng nhập hoặc token hết hạn")
                        )
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                        objectMapper.writeValueAsString(
                            ApiResponse.error("Bạn không có quyền thực hiện thao tác này")
                        )
                    );
                })
            )

            // Gắn DaoAuthenticationProvider và JWT filter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // [TSK-D15-CCT] Tích hợp đăng nhập nhanh bằng Google OAuth2 / OpenID Connect
        // Chỉ kích hoạt luồng trực tiếp cũ khi có ClientRegistration của Google (GOOGLE_CLIENT_ID đã được cấu hình)
        if (clientRegistrationRepository != null && clientRegistrationRepository.findByRegistrationId("google") != null) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authEndpoint -> authEndpoint
                    .authorizationRequestRepository(authorizationRequestRepository)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOidcUserService)
                )
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
            );
        }

        return http.build();
    }
}
