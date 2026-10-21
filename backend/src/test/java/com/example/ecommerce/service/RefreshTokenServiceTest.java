package com.example.ecommerce.service;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RefreshTokenService (Giai đoạn 2.2).
 * Kiểm tra việc sinh Opaque Token, mã hóa SHA-256, xoay vòng nguyên tử (Lua script),
 * xử lý Grace Period, phát hiện tái sử dụng (Reuse Detection), va chạm khóa và thu hồi phiên người dùng.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User mockUser;
    private RefreshTokenService.RefreshTokenData mockActiveData;
    private String activeJson;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L); // 7 days

        mockUser = User.builder()
                .id(100L)
                .email("user100@example.com")
                .role(Role.CUSTOMER)
                .password("$2a$12$hashedPasswordExample")
                .active(true)
                .build();

        mockActiveData = RefreshTokenService.RefreshTokenData.builder()
                .userId(100L)
                .email("user100@example.com")
                .role("CUSTOMER")
                .familyId("family-uuid-1")
                .status("ACTIVE")
                .createdAt(System.currentTimeMillis() - 100000)
                .expiresAt(System.currentTimeMillis() + 500000)
                .passwordHash("$2a$12$hashedPasswordExample")
                .build();

        activeJson = objectMapper.writeValueAsString(mockActiveData);
    }

    @Test
    @DisplayName("BUG-03 & NFR-SEC-RT-01: Tạo Refresh Token mới thành công qua Lua script nguyên tử")
    void createRefreshToken_Success() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn("SUCCESS");

        String rawToken = refreshTokenService.createRefreshToken(mockUser);

        assertThat(rawToken).isNotNull().isNotBlank();
        // Opaque token encode Base64 URL-safe từ 64 byte ngẫu nhiên
        assertThat(rawToken.length()).isGreaterThanOrEqualTo(64);

        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("SEC-02: Tạo token thất bại khi khóa token ngẫu nhiên bị va chạm (KEY_EXISTS)")
    void createRefreshToken_KeyExists_ThrowsBusinessException() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn("KEY_EXISTS");

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(mockUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lỗi hệ thống khi khởi tạo phiên đăng nhập");
    }

    @Test
    @DisplayName("NFR-SEC-RT-02: Xoay vòng Refresh Token thành công qua Redis Lua script nguyên tử")
    void rotateRefreshToken_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(activeJson);

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("SUCCESS");

        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken("dummy-raw-token");

        assertThat(result).isNotNull();
        assertThat(result.getRawRefreshToken()).isNotBlank();
        assertThat(result.getTokenData().getUserId()).isEqualTo(100L);
        assertThat(result.getTokenData().getFamilyId()).isEqualTo("family-uuid-1");
    }

    @Test
    @DisplayName("NFR-SEC-RT-03: Xoay vòng token thất bại khi token không tồn tại trong Redis")
    void rotateRefreshToken_NotFound_ThrowsBadCredentialsException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("non-existent-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("BUG-01: Token đã xoay vòng nhưng TRONG grace period (10s) -> Báo phiên đang cập nhật, KHÔNG thu hồi toàn bộ")
    void rotateRefreshToken_AlreadyRotated_InGracePeriod_ThrowsBadCredentialsWithoutRevoking() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(activeJson);

        RefreshTokenService.RefreshTokenData recentTombstone = RefreshTokenService.RefreshTokenData.builder()
                .userId(100L)
                .email("user100@example.com")
                .role("CUSTOMER")
                .familyId("family-uuid-1")
                .status("ROTATED")
                .createdAt(System.currentTimeMillis() - 100000)
                .expiresAt(System.currentTimeMillis() + 500000)
                .rotatedAt(System.currentTimeMillis() - 2000) // 2 giây trước (nằm trong grace period 10s)
                .replacedBy("newer-token-hash")
                .passwordHash("$2a$12$hashedPasswordExample")
                .build();

        String recentTombstoneJson = objectMapper.writeValueAsString(recentTombstone);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("ALREADY_ROTATED:" + recentTombstoneJson);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("concurrent-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Phiên đăng nhập đang được cập nhật từ thiết bị khác");

        // Xác nhận không kích hoạt thu hồi toàn bộ phiên
        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), eq(List.of("auth:user_sessions:100")), eq("auth:refresh:"));
    }

    @Test
    @DisplayName("NFR-SEC-RT-04: Phát hiện Refresh Token bị tái sử dụng ngoài grace period -> Thu hồi toàn bộ phiên qua Lua script")
    void rotateRefreshToken_ReuseDetected_RevokesAllSessions() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(activeJson);

        RefreshTokenService.RefreshTokenData oldTombstone = RefreshTokenService.RefreshTokenData.builder()
                .userId(100L)
                .email("user100@example.com")
                .role("CUSTOMER")
                .familyId("family-uuid-1")
                .status("ROTATED")
                .createdAt(System.currentTimeMillis() - 100000)
                .expiresAt(System.currentTimeMillis() + 500000)
                .rotatedAt(System.currentTimeMillis() - 30000) // Đã xoay cách đây 30 giây (ngoài grace period 10s)
                .replacedBy("new-token-hash")
                .passwordHash("$2a$12$hashedPasswordExample")
                .build();

        String oldTombstoneJson = objectMapper.writeValueAsString(oldTombstone);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("ALREADY_ROTATED:" + oldTombstoneJson);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("reused-old-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Phát hiện phiên đăng nhập bị tái sử dụng bất thường");

        // Xác nhận đã gọi Lua script thu hồi toàn bộ phiên
        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("auth:user_sessions:100")), eq("auth:refresh:"));
    }

    @Test
    @DisplayName("SEC-02: Xoay vòng thất bại khi token mới bị va chạm khóa trong Redis (NEW_TOKEN_COLLISION)")
    void rotateRefreshToken_NewTokenCollision_ThrowsBusinessException() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(activeJson);

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("NEW_TOKEN_COLLISION");

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("dummy-raw-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lỗi xung đột phiên đăng nhập");
    }

    @Test
    @DisplayName("BUG-02 & FR-AUTH: Đọc metadata phiên hợp lệ bằng getTokenData thành công")
    void getTokenData_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(activeJson);

        RefreshTokenService.RefreshTokenData data = refreshTokenService.getTokenData("valid-raw-token");

        assertThat(data).isNotNull();
        assertThat(data.getUserId()).isEqualTo(100L);
        assertThat(data.getEmail()).isEqualTo("user100@example.com");
    }

    @Test
    @DisplayName("BUG-02: getTokenData ném BadCredentialsException khi token không tồn tại")
    void getTokenData_NotFound_ThrowsBadCredentialsException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.getTokenData("non-existent-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("BUG-02: getTokenData phát hiện token ROTATED trong grace period -> Báo cập nhật từ thiết bị khác")
    void getTokenData_AlreadyRotated_InGracePeriod_ThrowsBadCredentialsWithoutRevoking() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RefreshTokenService.RefreshTokenData tombstone = RefreshTokenService.RefreshTokenData.builder()
                .userId(100L)
                .familyId("family-uuid-1")
                .status("ROTATED")
                .rotatedAt(System.currentTimeMillis() - 1500) // 1.5s ago
                .build();

        String tombstoneJson = objectMapper.writeValueAsString(tombstone);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(tombstoneJson);

        assertThatThrownBy(() -> refreshTokenService.getTokenData("rotated-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Phiên đăng nhập đang được cập nhật từ thiết bị khác");

        verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), eq(List.of("auth:user_sessions:100")), eq("auth:refresh:"));
    }

    @Test
    @DisplayName("NFR-SEC-RT-05: Đăng xuất (Logout) thu hồi đơn phiên Refresh Token")
    void revokeToken_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        RefreshTokenService.RefreshTokenData data = RefreshTokenService.RefreshTokenData.builder()
                .userId(100L)
                .build();

        String dataJson = objectMapper.writeValueAsString(data);
        when(valueOperations.get(startsWith("auth:refresh:"))).thenReturn(dataJson);

        refreshTokenService.revokeToken("valid-token-to-revoke");

        verify(zSetOperations).remove(eq("auth:user_sessions:100"), anyString());
        verify(redisTemplate).delete(startsWith("auth:refresh:"));
    }

    @Test
    @DisplayName("BUG-04 & NFR-SEC-RT-06: Thu hồi toàn bộ phiên (Revoke All Sessions) qua Lua script nguyên tử")
    void revokeAllUserSessions_Success() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:user_sessions:100")), eq("auth:refresh:")))
                .thenReturn(3L);

        refreshTokenService.revokeAllUserSessions(100L);

        verify(redisTemplate).execute(any(DefaultRedisScript.class), eq(List.of("auth:user_sessions:100")), eq("auth:refresh:"));
    }
}
