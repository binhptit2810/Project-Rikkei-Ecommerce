package com.example.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 *   jwt.expiration — thời hạn token (millisecond, mặc định 24h)
 *
 * Token payload:
 *   - subject: email của người dùng
 *   - iat:     thời điểm tạo
 *   - exp:     thời điểm hết hạn
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
     * Tạo JWT access token với subject = email người dùng.
     *
     * @param userDetails đối tượng UserDetails đã xác thực
     * @return JWT token dạng chuỗi compact
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(userDetails.getUsername())  // email
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Lấy email (subject) từ JWT token.
     *
     * @param token JWT token hợp lệ
     * @return email người dùng
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
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
