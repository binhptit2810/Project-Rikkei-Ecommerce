package com.example.ecommerce.config;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.security.CustomUserDetailsService;
import com.example.ecommerce.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Cấu hình Spring Security cho toàn bộ ứng dụng.
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
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // Cho phép @PreAuthorize, @PostAuthorize trong Service/Controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;  // Spring Boot auto-configure bean

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
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                // GET sản phẩm và danh mục: khách vãng lai được xem
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                // --- Admin only ---
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

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

        return http.build();
    }
}
