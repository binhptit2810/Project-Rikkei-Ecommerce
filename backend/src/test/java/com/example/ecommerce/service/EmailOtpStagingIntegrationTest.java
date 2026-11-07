package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.ResendVerificationRequest;
import com.example.ecommerce.dto.request.VerifyEmailRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.RegisterResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FINAL STAGING INTEGRATION TEST — EMAIL OTP AUTHENTICATION
 *
 * Kiểm thử tích hợp thực tế với real Redis 7 (127.0.0.1:6379):
 * 1. Redis Lua Atomic Lock (Compare-and-Delete)
 * 2. Redis Lua Verify and Claim OTP Lifecycle
 * 3. Concurrent Registration (50 threads)
 * 4. Concurrent Resend Verification (50 threads)
 * 5. Concurrent Distinct Emails Registration (50 threads)
 * 6. SMTP Exception Resilience and Lock Release
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:staging_otp_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=staging-test-secret-key-must-be-at-least-32-chars-long-1234567890",
    "jwt.expiration=3600000",
    "spring.data.redis.host=${REDIS_HOST:localhost}",
    "spring.data.redis.port=${REDIS_PORT:6379}"
})
class EmailOtpStagingIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private EmailService emailService;

    private static final String TEST_EMAIL_PREFIX = "staging_test_";

    @BeforeEach
    void setUp() {
        cleanupTestData();
        // Default Mock SMTP: no-op (emails are sent successfully)
        doNothing().when(emailService).sendVerificationOtpEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendOtpEmail(anyString(), anyString());
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            userRepository.deleteAll();
            Set<String> keys = redisTemplate.keys("*" + TEST_EMAIL_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            Set<String> luaKeys = redisTemplate.keys("test:lua:*");
            if (luaKeys != null && !luaKeys.isEmpty()) {
                redisTemplate.delete(luaKeys);
            }
        } catch (Exception ex) {
            // Ignore in cleanup
        }
    }

    // =========================================================================
    // SECTION B: REDIS LUA ATOMIC LOCK INTEGRATION TESTS (REAL REDIS 7)
    // =========================================================================

    @Test
    @DisplayName("Redis Lua Lock: Request A sở hữu lockValue A -> release trả về 1 và xóa key thành công")
    void testRedisLuaLock_RequestA_AcquiresAndReleasesSuccessfully() {
        String rateLimitKey = "test:lua:lock:" + UUID.randomUUID();
        String lockValueA = UUID.randomUUID().toString();

        // 1. Request A acquire lock thành công
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, lockValueA, 60, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        assertThat(redisTemplate.opsForValue().get(rateLimitKey)).isEqualTo(lockValueA);

        // 2. Request A release với đúng lockValue A
        boolean released = authService.releaseRateLimitLock(rateLimitKey, lockValueA);
        assertThat(released).isTrue();

        // Key phải bị xóa hoàn toàn khỏi Redis
        assertThat(redisTemplate.hasKey(rateLimitKey)).isFalse();
    }

    @Test
    @DisplayName("Redis Lua Lock: Request B dùng sai lockValue -> release trả về 0 và key không bị xóa")
    void testRedisLuaLock_RequestB_WrongLockValueFails() {
        String rateLimitKey = "test:lua:lock:" + UUID.randomUUID();
        String lockValueA = UUID.randomUUID().toString();
        String lockValueB = UUID.randomUUID().toString();

        // Request A acquire lock
        redisTemplate.opsForValue().set(rateLimitKey, lockValueA, 60, TimeUnit.SECONDS);

        // Request B cố tình release với wrong lockValue
        boolean released = authService.releaseRateLimitLock(rateLimitKey, lockValueB);
        assertThat(released).isFalse();

        // Key vẫn tồn tại nguyên vẹn trong Redis và thuộc sở hữu của Request A
        assertThat(redisTemplate.hasKey(rateLimitKey)).isTrue();
        assertThat(redisTemplate.opsForValue().get(rateLimitKey)).isEqualTo(lockValueA);
    }

    @Test
    @DisplayName("Redis Lua Lock: Lock đã hết hạn (expired key) -> release trả về 0")
    void testRedisLuaLock_ExpiredKeyReturnsFalse() {
        String rateLimitKey = "test:lua:lock:" + UUID.randomUUID();
        String lockValue = UUID.randomUUID().toString();

        // Key không tồn tại trong Redis
        assertThat(redisTemplate.hasKey(rateLimitKey)).isFalse();

        // Release thử -> trả về false (0)
        boolean released = authService.releaseRateLimitLock(rateLimitKey, lockValue);
        assertThat(released).isFalse();
    }

    @Test
    @DisplayName("Redis Lua Lock: Request A bị stale -> không xóa nhầm lockValue B của Request B")
    void testRedisLuaLock_StaleRequestCannotDeleteNewLock() {
        String rateLimitKey = "test:lua:lock:" + UUID.randomUUID();
        String lockValueA = UUID.randomUUID().toString();
        String lockValueB = UUID.randomUUID().toString();

        // Request A từng có lockValueA, nhưng đã hết hạn và Request B đã chiếm lock mới với lockValueB
        redisTemplate.opsForValue().set(rateLimitKey, lockValueB, 60, TimeUnit.SECONDS);

        // Request A bị stale hoàn thành sau và cố gắng xóa lock với lockValueA cũ
        boolean released = authService.releaseRateLimitLock(rateLimitKey, lockValueA);
        assertThat(released).isFalse();

        // Khóa của Request B vẫn còn nguyên vẹn 100%
        assertThat(redisTemplate.hasKey(rateLimitKey)).isTrue();
        assertThat(redisTemplate.opsForValue().get(rateLimitKey)).isEqualTo(lockValueB);
    }

    @Test
    @DisplayName("Redis Lua Verify & Claim: Chu trình xác thực nguyên tử, chống double-claim và brute-force")
    void testRedisLuaVerifyAndClaim_CompleteLifecycleOnRealRedis() {
        String email = TEST_EMAIL_PREFIX + "lua_claim@example.com";
        String otpKey = "auth:verify_otp:" + email;
        String attemptsKey = "auth:verify_attempts:" + email;
        String validHmac = "valid-hmac-sha256-hash-example";

        String luaScript =
                "local otp = redis.call('GET', KEYS[1])\n" +
                "if not otp then\n" +
                "    return 'NOT_FOUND'\n" +
                "end\n" +
                "if otp == 'CLAIMED' then\n" +
                "    return 'ALREADY_USED'\n" +
                "end\n" +
                "local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')\n" +
                "if attempts >= tonumber(ARGV[2]) then\n" +
                "    redis.call('DEL', KEYS[1])\n" +
                "    return 'MAX_ATTEMPTS_EXCEEDED'\n" +
                "end\n" +
                "if otp ~= ARGV[1] then\n" +
                "    local newAttempts = redis.call('INCR', KEYS[2])\n" +
                "    local otpTtl = redis.call('TTL', KEYS[1])\n" +
                "    if otpTtl > 0 then\n" +
                "        redis.call('EXPIRE', KEYS[2], otpTtl)\n" +
                "    end\n" +
                "    if newAttempts >= tonumber(ARGV[2]) then\n" +
                "        redis.call('DEL', KEYS[1])\n" +
                "        return 'MAX_ATTEMPTS_EXCEEDED'\n" +
                "    else\n" +
                "        return 'MISMATCH:' .. tostring(newAttempts)\n" +
                "    end\n" +
                "end\n" +
                "local otpTtl = redis.call('TTL', KEYS[1])\n" +
                "local claimTtl = math.max(tonumber(ARGV[3]), 30)\n" +
                "if otpTtl > 0 then\n" +
                "    claimTtl = math.min(otpTtl, claimTtl)\n" +
                "end\n" +
                "redis.call('SET', KEYS[1], 'CLAIMED', 'EX', claimTtl)\n" +
                "return 'SUCCESS:' .. tostring(otpTtl)";

        DefaultRedisScript<String> script = new DefaultRedisScript<>(luaScript, String.class);

        // Trường hợp 1: Nhập sai mã OTP -> MISMATCH:1, MISMATCH:2
        redisTemplate.opsForValue().set(otpKey, validHmac, 300, TimeUnit.SECONDS);

        String resultWrong1 = redisTemplate.execute(script, List.of(otpKey, attemptsKey), "wrong-hmac", "5", "60");
        assertThat(resultWrong1).isEqualTo("MISMATCH:1");

        String resultWrong2 = redisTemplate.execute(script, List.of(otpKey, attemptsKey), "wrong-hmac", "5", "60");
        assertThat(resultWrong2).isEqualTo("MISMATCH:2");

        // Trường hợp 2: Nhập đúng mã OTP -> SUCCESS:... và chuyển sang CLAIMED
        String resultSuccess = redisTemplate.execute(script, List.of(otpKey, attemptsKey), validHmac, "5", "60");
        assertThat(resultSuccess).startsWith("SUCCESS:");
        assertThat(redisTemplate.opsForValue().get(otpKey)).isEqualTo("CLAIMED");

        // Trường hợp 3: Double-claim (cố tình xác thực lại mã vừa claim) -> ALREADY_USED
        String resultDoubleClaim = redisTemplate.execute(script, List.of(otpKey, attemptsKey), validHmac, "5", "60");
        assertThat(resultDoubleClaim).isEqualTo("ALREADY_USED");

        // Trường hợp 4: Vượt quá 5 lần thử sai -> MAX_ATTEMPTS_EXCEEDED và OTP bị xóa
        String bruteEmail = TEST_EMAIL_PREFIX + "brute@example.com";
        String bruteOtpKey = "auth:verify_otp:" + bruteEmail;
        String bruteAttemptsKey = "auth:verify_attempts:" + bruteEmail;
        redisTemplate.opsForValue().set(bruteOtpKey, validHmac, 300, TimeUnit.SECONDS);

        for (int i = 1; i <= 4; i++) {
            String res = redisTemplate.execute(script, List.of(bruteOtpKey, bruteAttemptsKey), "wrong", "5", "60");
            assertThat(res).isEqualTo("MISMATCH:" + i);
        }
        String resMax = redisTemplate.execute(script, List.of(bruteOtpKey, bruteAttemptsKey), "wrong", "5", "60");
        assertThat(resMax).isEqualTo("MAX_ATTEMPTS_EXCEEDED");
        assertThat(redisTemplate.hasKey(bruteOtpKey)).isFalse();
    }

    // =========================================================================
    // SECTION C: CONCURRENT TESTS (50 REQUESTS)
    // =========================================================================

    @Test
    @DisplayName("Concurrent Register: 50 requests cùng đăng ký 1 email -> Đúng 1 thành công, 49 bị chặn, không duplicate DB")
    void testConcurrentRegister_SameEmail_50Requests() throws InterruptedException {
        int threadCount = 50;
        String email = TEST_EMAIL_PREFIX + "register_concurrent@example.com";

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    RegisterRequest request = new RegisterRequest();
                    request.setEmail(email);
                    request.setPassword("Password123!");
                    request.setFullName("Concurrent User " + index);
                    request.setPhone("0912345678");
                    request.setAddress("123 Staging Ave");

                    RegisterResponse resp = authService.register(request);
                    if (resp != null && email.equalsIgnoreCase(resp.getEmail())) {
                        successCount.incrementAndGet();
                    }
                } catch (BusinessException ex) {
                    rejectedCount.incrementAndGet();
                } catch (DataIntegrityViolationException ex) {
                    // Database unique constraint violation
                    rejectedCount.incrementAndGet();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Phát lệnh đồng loạt 50 requests
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(threadCount - 1);

        // Kiểm tra Database: Đúng 1 user duy nhất với emailVerified = false
        List<User> users = userRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .toList();
        assertThat(users).hasSize(1);
        User createdUser = users.get(0);
        assertThat(createdUser.getEmailVerified()).isFalse();
        assertThat(createdUser.getRole()).isEqualTo(Role.CUSTOMER);

        // Kiểm tra Redis: Đúng 1 mã OTP tồn tại
        String otpKey = "auth:verify_otp:" + email;
        assertThat(redisTemplate.hasKey(otpKey)).isTrue();
    }

    @Test
    @DisplayName("Concurrent Resend Verification: 50 requests cùng yêu cầu gửi lại OTP -> Đúng 1 thành công, 49 bị chặn 429")
    void testConcurrentResendVerification_SameEmail_50Requests() throws InterruptedException {
        String email = TEST_EMAIL_PREFIX + "resend_concurrent@example.com";

        // Tạo sẵn tài khoản pending verification trong DB
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .fullName("Pending Customer")
                .phone("0912345678")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(false)
                .build();
        userRepository.save(user);

        // Đảm bảo không có rate limit lock nào tồn tại trước khi test
        redisTemplate.delete("auth:verify_ratelimit:" + email);

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ResendVerificationRequest request = new ResendVerificationRequest(email);
                    authService.resendVerification(request);
                    successCount.incrementAndGet();
                } catch (BusinessException ex) {
                    if (ex.getMessage().contains("Vui lòng đợi 60 giây") || ex.getMessage().contains("quá thường xuyên")) {
                        rateLimitedCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(ex);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắn 50 requests cùng lúc
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(unexpectedErrors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rateLimitedCount.get()).isEqualTo(threadCount - 1);

        // Chỉ đúng 1 email OTP được gửi
        verify(emailService, times(1)).sendVerificationOtpEmail(eq(email), anyString(), eq("Pending Customer"));
    }

    @Test
    @DisplayName("Concurrent Distinct Emails: 50 requests đăng ký 50 email khác nhau -> Cả 50 đều thành công độc lập")
    void testConcurrentRegister_DistinctEmails_50Requests() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    String email = TEST_EMAIL_PREFIX + "user_" + index + "@staging.example.com";
                    RegisterRequest request = new RegisterRequest();
                    request.setEmail(email);
                    request.setPassword("Password123!");
                    request.setFullName("Distinct User " + index);
                    request.setPhone("09" + String.format("%08d", index));
                    request.setAddress("Hanoi, Vietnam");

                    RegisterResponse resp = authService.register(request);
                    if (resp != null && email.equalsIgnoreCase(resp.getEmail())) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Đảm bảo 50 tài khoản độc lập trong CSDL
        long count = userRepository.findAll().stream()
                .filter(u -> u.getEmail().startsWith(TEST_EMAIL_PREFIX))
                .count();
        assertThat(count).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("SMTP Exception Resilience: Lỗi gửi mail giải phóng lock nguyên tử, không làm treo 60s và có thể thử lại ngay")
    void testRegister_SmtpFailure_LockCleanlyReleasedViaLua() {
        String email = TEST_EMAIL_PREFIX + "smtp_fail@example.com";
        String rateLimitKey = "auth:verify_ratelimit:" + email;

        // Mô phỏng SMTP ném exception
        doThrow(new RuntimeException("SMTP Connection refused"))
                .when(emailService).sendVerificationOtpEmail(eq(email), anyString(), anyString());

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Password123!");
        request.setFullName("SMTP Fail User");
        request.setPhone("0912345678");

        try {
            authService.register(request);
        } catch (BusinessException ex) {
            assertThat(ex.getMessage()).contains("không thể gửi email xác thực");
        }

        // Nhờ releaseRateLimitLock với Lua script, khóa rate limit được giải phóng ngay lập tức
        assertThat(redisTemplate.hasKey(rateLimitKey)).isFalse();

        // Khôi phục SMTP hoạt động bình thường
        reset(emailService);
        doNothing().when(emailService).sendVerificationOtpEmail(anyString(), anyString(), anyString());

        // Người dùng gửi lại mã OTP ngay lập tức mà KHÔNG bị chặn 60s
        ResendVerificationRequest resendReq = new ResendVerificationRequest(email);
        authService.resendVerification(resendReq);

        // Resend thành công và sinh OTP mới
        assertThat(redisTemplate.hasKey("auth:verify_otp:" + email)).isTrue();
        verify(emailService, times(1)).sendVerificationOtpEmail(eq(email), anyString(), eq("SMTP Fail User"));
    }
}

