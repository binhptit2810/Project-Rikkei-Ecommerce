package com.example.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Tiện ích tạo và xác thực JWT Access Token.
 *
 * Cấu hình qua biến môi trường (application.yml):
 *   jwt.secret   — secret >= 32 ký tự (256-bit HMAC-SHA256)
 *   jwt.expiration — thời hạn token (millisecond, mặc định 15 phút = 900000ms)
 *
 * Token payload chuẩn theo hợp đồng bảo mật:
 *   - sub:    email của người dùng (tiêu chuẩn JWT)
 *   - email:  email của người dùng
 *   - userId: định danh duy nhất của người dùng
 *   - role:   vai trò (ROLE_CUSTOMER, ROLE_ADMIN)
 *   - iat:    thời điểm tạo
 *   - exp:    thời điểm hết hạn
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /** Tạo SecretKey từ chuỗi secret trong cấu hình. */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo JWT access token với đầy đủ claims: sub, email, role, userId (nếu có từ CustomUserDetails).
     *
     * @param userDetails đối tượng UserDetails đã xác thực
     * @return JWT token dạng chuỗi compact
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);
        String email = userDetails.getUsername();

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_CUSTOMER");

        Long userId = null;
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            userId = customUserDetails.getId();
        }

        var builder = Jwts.builder()
                .subject(email)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    /**
     * Tạo JWT access token với đầy đủ claims userId, email, role chỉ định tường minh.
     *
     * @param userId ID người dùng
     * @param email  email người dùng
     * @param role   vai trò (tự động chuẩn hóa tiền tố ROLE_)
     * @return JWT token dạng chuỗi compact
     */
    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);
        String formattedRole = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;

        var builder = Jwts.builder()
                .subject(email)
                .claim("email", email)
                .claim("role", formattedRole)
                .issuedAt(now)
                .expiration(expiry);

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    /**
     * Lấy email (subject) từ JWT token.
     *
     * @param token JWT token hợp lệ
     * @return email người dùng
     */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        String emailClaim = claims.get("email", String.class);
        return emailClaim != null ? emailClaim : claims.getSubject();
    }

    /**
     * Lấy userId từ JWT token claims.
     *
     * @param token JWT token hợp lệ
     * @return userId người dùng hoặc null nếu không tồn tại
     */
    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Lấy role từ JWT token claims.
     *
     * @param token JWT token hợp lệ
     * @return role (VD: ROLE_CUSTOMER, ROLE_ADMIN)
     */
    public String extractRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Trả về thời hạn token (giây) — dùng trong AuthResponse.
     */
    public long getExpirationInSeconds() {
        return jwtExpiration / 1000;
    }

    /**
     * Kiểm tra token hợp lệ: chữ ký đúng, chưa hết hạn, khớp username.
     *
     * @param token       JWT token cần kiểm tra
     * @param userDetails UserDetails từ database
     * @return true nếu token hợp lệ
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            return claims.getSubject().equals(userDetails.getUsername()) && notExpired;
        } catch (SignatureException e) {
            log.warn("JWT signature không hợp lệ: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT bị lỗi định dạng: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT đã hết hạn: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims rỗng: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Trích xuất Claims từ JWT token đã được ký và xác minh chữ ký.
     * Ném JwtException nếu token không hợp lệ, hết hạn, hoặc sai chữ ký.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }
}
