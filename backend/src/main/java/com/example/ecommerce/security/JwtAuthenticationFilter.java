package com.example.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

/**
 * Filter xác thực JWT — chạy một lần cho mỗi HTTP request.
 *
 * Luồng xử lý:
 *   1. Đọc header Authorization: Bearer <token>
 *   2. Nếu có token → phân tích Claims một lần (xác minh chữ ký và format)
 *   3. Trích xuất email từ Claims
 *   4. Load UserDetails từ database (email làm key)
 *   5. Kiểm tra tài khoản active/enabled và token chưa hết hạn
 *   6. Nếu hợp lệ → ghi Authentication vào SecurityContext
 *   7. Tiếp tục filter chain (không chặn ở đây, SecurityConfig sẽ phân quyền)
 *
 * Bảo mật:
 *   - Mọi lỗi trong quá trình xác thực đều được log debug và filter tiếp tục.
 *   - SecurityConfig sẽ từ chối request nếu SecurityContext rỗng.
 *   - Không ghi JWT hoặc thông tin nhạy cảm vào log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);

            if (StringUtils.hasText(token)) {
                Claims claims = jwtUtils.parseClaims(token);
                String email = claims.getSubject();

                // Chỉ xác thực nếu chưa có Authentication trong context
                if (StringUtils.hasText(email)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    boolean notExpired = claims.getExpiration() != null && claims.getExpiration().after(new Date());
                    if (userDetails.isEnabled() && email.equals(userDetails.getUsername()) && notExpired) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,  // credentials = null sau khi đã xác thực
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            log.debug("JWT đã hết hạn cho request {}: {}", request.getRequestURI(), e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT không hợp lệ cho request {}: {}", request.getRequestURI(), e.getMessage());
        } catch (Exception e) {
            // Log lỗi nhưng tiếp tục filter chain — SecurityConfig sẽ từ chối nếu cần
            log.debug("Không thể xác thực JWT cho request {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất token từ header Authorization: Bearer <token>.
     * Trả về null nếu header không hợp lệ hoặc không có.
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
