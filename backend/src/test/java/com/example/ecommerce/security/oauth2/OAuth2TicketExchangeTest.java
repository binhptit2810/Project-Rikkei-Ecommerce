package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.EmailService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test kiểm toán cho cơ chế Atomic Compare-And-Delete trong trao đổi OAuth2 One-Time Ticket [FIX-01].
 * Khắc phục dứt điểm lỗ hổng [SEC-FIND-01]: Ticket Invalidation / Denial of Service.
 *
 * Kiểm tra đầy đủ 12 kịch bản an ninh:
 * - Nhóm A (Functional Security): FIX01-T01 đến FIX01-T08
 * - Nhóm B (Security Regression & Concurrency): FIX01-T09 đến FIX01-T12
 */
@ExtendWith(MockitoExtension.class)
class OAuth2TicketExchangeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private EmailService emailService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthService authService;

    private String hashSha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // =========================================================================
    // NHÓM A: FUNCTIONAL SECURITY TESTS (FIX01-T01 -> FIX01-T08)
    // =========================================================================

    @Test
    @DisplayName("FIX01-T01: Ticket hợp lệ + binding hợp lệ -> Tiêu thụ vé và Exchange thành công")
    void fix01_T01_validTicketAndBinding_successIssuesTokens() throws Exception {
        String rawTicket = "sample-secure-ticket-value-123456";
        String bindingSecret = "sample-browser-binding-secret-789";

        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(100L)
                .email("customer@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        // Lua script CAD trả về status 1 (thành công) và ticketJson
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(1L, ticketJson));

        User user = User.builder()
                .id(100L)
                .email("customer@gmail.com")
                .fullName("Customer Name")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("jwt-token-xyz");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token-xyz");

        AuthResponse response = authService.exchangeOAuth2Ticket(rawTicket, bindingSecret);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token-xyz");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-xyz");
        assertThat(response.getUser().getEmail()).isEqualTo("customer@gmail.com");
    }

    @Test
    @DisplayName("FIX01-T02: Ticket hợp lệ + binding sai -> Trả 401, Lua CAD trả status 2 (Ticket không bị xóa)")
    void fix01_T02_validTicket_bindingMismatch_throwsBadCredentials_andPreservesTicket() throws Exception {
        String rawTicket = "legitimate-ticket-value";
        String attackerBinding = "attacker-fake-binding-secret";

        String ticketHash = hashSha256(rawTicket);
        String attackerBindingHash = hashSha256(attackerBinding);

        // Lua script CAD phát hiện mismatch: trả về status 2 (không gọi DEL)
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(attackerBindingHash)))
                .thenReturn(List.of(2L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, attackerBinding))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không khớp với phiên trình duyệt khởi tạo");

        // Khẳng định không cấp token
        verify(jwtUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("FIX01-T03: Ticket hợp lệ + thiếu cookie binding -> Trả 401 ngay, không gọi Redis")
    void fix01_T03_validTicket_missingCookie_throwsBadCredentials_withoutTouchingRedis() {
        String rawTicket = "sample-ticket-value";

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("thiếu thông tin xác thực trình duyệt");

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, "   "))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("thiếu thông tin xác thực trình duyệt");

        // Khẳng định tuyệt đối không tương tác với Redis
        verify(redisTemplate, never()).execute(any(), any(), any());
    }

    @Test
    @DisplayName("FIX01-T04: Ticket không tồn tại -> Trả 401 Unauthorized")
    void fix01_T04_ticketNotFound_throwsBadCredentials() throws Exception {
        String rawTicket = "non-existent-ticket";
        String bindingSecret = "valid-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        // Lua script trả về status 0 (không tìm thấy)
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(0L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("FIX01-T05: Ticket hết hạn (TTL Redis hết) -> Trả 401 Unauthorized")
    void fix01_T05_ticketExpired_throwsBadCredentials() throws Exception {
        String rawTicket = "expired-ticket";
        String bindingSecret = "valid-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(0L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("FIX01-T06: Ticket đã được sử dụng trước đó (Used ticket) -> Trả 401")
    void fix01_T06_ticketAlreadyUsed_throwsBadCredentials() throws Exception {
        String rawTicket = "already-burned-ticket";
        String bindingSecret = "valid-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(0L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("FIX01-T07: Ticket thiếu bindingHash trong dữ liệu Redis -> Trả 401, không tiêu thụ vé")
    void fix01_T07_ticketMissingBindingHash_throwsBadCredentials_notConsumed() throws Exception {
        String rawTicket = "ticket-without-binding";
        String bindingSecret = "any-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        // Lua script đối chiếu `not storedHash` -> trả về status 2
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(2L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không khớp với phiên trình duyệt khởi tạo");
    }

    @Test
    @DisplayName("FIX01-T08: Binding đúng nhưng tài khoản bị Admin khóa trong DB -> Từ chối đăng nhập")
    void fix01_T08_bindingValid_userLockedInDb_throwsLockedAccountException() throws Exception {
        String rawTicket = "valid-ticket-locked-user";
        String bindingSecret = "valid-binding";

        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(55L)
                .email("banned@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(1L, ticketJson));

        User bannedUser = User.builder()
                .id(55L)
                .email("banned@gmail.com")
                .role(Role.CUSTOMER)
                .active(false)
                .lockReason("Vi phạm điều khoản cộng đồng")
                .build();

        when(userRepository.findById(55L)).thenReturn(Optional.of(bannedUser));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(LockedAccountException.class)
                .hasMessageContaining("Tài khoản của bạn đã bị khóa.");

        verify(jwtUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    // =========================================================================
    // NHÓM B: SECURITY REGRESSION & CONCURRENCY TESTS (FIX01-T09 -> FIX01-T12)
    // =========================================================================

    @Test
    @DisplayName("FIX01-T09: Ticket Theft / DoS Prevention - Kẻ tấn công gửi binding sai không làm mất vé của người dùng chính chủ")
    void fix01_T09_ticketTheft_dosPrevention_attackerFails_legitimateSucceeds() throws Exception {
        String rawTicket = "intercepted-ticket-xyz";
        String attackerBinding = "attacker-fake-binding";
        String legitimateBinding = "legitimate-user-binding";

        String ticketHash = hashSha256(rawTicket);
        String attackerBindingHash = hashSha256(attackerBinding);
        String legitimateBindingHash = hashSha256(legitimateBinding);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(101L)
                .email("victim@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        // BƯỚC 1: Kẻ tấn công gửi request với binding giả mạo
        // Lua CAD kiểm tra hash mismatch: Trả status 2, KHÔNG XÓA VÉ khỏi Redis
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(attackerBindingHash)))
                .thenReturn(List.of(2L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, attackerBinding))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không khớp với phiên trình duyệt khởi tạo");

        // BƯỚC 2: Người dùng chính chủ gửi request với binding thật
        // Vì vé không bị kẻ tấn công làm mất, Lua CAD so khớp thành công: trả status 1 và ticketJson
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(legitimateBindingHash)))
                .thenReturn(List.of(1L, ticketJson));

        User user = User.builder()
                .id(101L)
                .email("victim@gmail.com")
                .fullName("Victim User")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        when(userRepository.findById(101L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any())).thenReturn("valid-jwt");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("valid-refresh");

        AuthResponse successResponse = authService.exchangeOAuth2Ticket(rawTicket, legitimateBinding);

        assertThat(successResponse).isNotNull();
        assertThat(successResponse.getAccessToken()).isEqualTo("valid-jwt");
        assertThat(successResponse.getUser().getEmail()).isEqualTo("victim@gmail.com");
    }

    @Test
    @DisplayName("FIX01-T10: Replay Attack Prevention - Không thể sử dụng lại vé đã exchange thành công")
    void fix01_T10_replayAttack_secondAttemptFails() throws Exception {
        String rawTicket = "single-use-ticket";
        String bindingSecret = "valid-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(102L)
                .email("singleuse@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        User user = User.builder()
                .id(102L)
                .email("singleuse@gmail.com")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        // Lần 1: Thành công
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(1L, ticketJson));
        when(userRepository.findById(102L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token-1");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token-1");

        AuthResponse firstExchange = authService.exchangeOAuth2Ticket(rawTicket, bindingSecret);
        assertThat(firstExchange).isNotNull();

        // Lần 2: Vé đã bị DEL khỏi Redis, Lua CAD trả status 0
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenReturn(List.of(0L, ""));

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, bindingSecret))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("FIX01-T11: Concurrent Exchange - Hai request đồng thời cùng ticket và binding đúng, chỉ 1 request thành công")
    void fix01_T11_concurrentExchange_onlyOneSucceeds() throws Exception {
        String rawTicket = "concurrent-ticket";
        String bindingSecret = "concurrent-binding";
        String ticketHash = hashSha256(rawTicket);
        String bindingHash = hashSha256(bindingSecret);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(103L)
                .email("concurrent@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        User user = User.builder()
                .id(103L)
                .email("concurrent@gmail.com")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        // Giả lập tính nguyên tử của Redis Lua Script:
        // Lần gọi đầu tiên trả về status 1 (DEL thành công), các lần gọi sau trả về status 0 (đã bị xóa)
        AtomicInteger invocationCount = new AtomicInteger(0);
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(bindingHash)))
                .thenAnswer(invocation -> {
                    if (invocationCount.getAndIncrement() == 0) {
                        return List.of(1L, ticketJson);
                    } else {
                        return List.of(0L, "");
                    }
                });

        when(userRepository.findById(103L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any())).thenReturn("jwt-concurrent");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-concurrent");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    authService.exchangeOAuth2Ticket(rawTicket, bindingSecret);
                    successCount.incrementAndGet();
                } catch (BadCredentialsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Bắn 2 request đồng thời
        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("FIX01-T12: Concurrent Invalid and Valid Binding - Request sai và đúng chạy đồng thời, request sai không chặn request đúng")
    void fix01_T12_concurrentInvalidAndValidBinding_attackerCannotBlockLegitimateUser() throws Exception {
        String rawTicket = "concurrent-dos-ticket";
        String attackerBinding = "bad-binding";
        String legitimateBinding = "good-binding";

        String ticketHash = hashSha256(rawTicket);
        String attackerBindingHash = hashSha256(attackerBinding);
        String legitimateBindingHash = hashSha256(legitimateBinding);

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(104L)
                .email("gooduser@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String ticketJson = objectMapper.writeValueAsString(ticketData);

        User user = User.builder()
                .id(104L)
                .email("gooduser@gmail.com")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        // Kẻ tấn công gọi trước hoặc đồng thời -> Luôn nhận status 2, vé không bị xóa
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(attackerBindingHash)))
                .thenReturn(List.of(2L, ""));

        // Người dùng hợp lệ gọi -> Nhận status 1, exchange thành công
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("auth:oauth2_ticket:" + ticketHash)), eq(legitimateBindingHash)))
                .thenReturn(List.of(1L, ticketJson));

        when(userRepository.findById(104L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger attackerRejected = new AtomicInteger(0);
        AtomicInteger legitimateSuccess = new AtomicInteger(0);

        // Luồng 1: Kẻ tấn công
        executor.submit(() -> {
            try {
                startLatch.await();
                authService.exchangeOAuth2Ticket(rawTicket, attackerBinding);
            } catch (BadCredentialsException e) {
                if (e.getMessage().contains("không khớp với phiên trình duyệt")) {
                    attackerRejected.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Luồng 2: Người dùng chính chủ
        executor.submit(() -> {
            try {
                startLatch.await();
                AuthResponse res = authService.exchangeOAuth2Ticket(rawTicket, legitimateBinding);
                if (res != null && "jwt-token".equals(res.getAccessToken())) {
                    legitimateSuccess.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(attackerRejected.get()).isEqualTo(1);
        assertThat(legitimateSuccess.get()).isEqualTo(1);
    }
}
