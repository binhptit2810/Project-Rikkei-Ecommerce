package com.example.ecommerce.security.oauth2;

import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.JwtUtils;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.EmailService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.GenericContainer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration Test cấp độ Production thực thi trực tiếp trên máy chủ Redis thật [FIX-01-TEST].
 * Xác minh tính nguyên tử và độ an toàn của Atomic Compare-And-Delete (Lua Script):
 *   - Thực thi trực tiếp Lua Script trên Redis thật (Testcontainers hoặc isolated test keyspace).
 *   - Xác minh trạng thái RAM Redis thực tế (key tồn tại / bị xóa, TTL tự nhiên).
 *   - Kiểm tra đa luồng Concurrency thực tế (không dùng Mockito giả lập kết quả).
 *   - Kiểm tra khả năng chống chịu với payload JSON dị dạng, sai kiểu dữ liệu.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class OAuth2TicketExchangeRedisIntegrationTest {

    private static GenericContainer<?> redisContainer;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static ObjectMapper objectMapper;

    // Các thành phần phục vụ kiểm thử tích hợp AuthService
    private static UserRepository userRepository;
    private static PasswordEncoder passwordEncoder;
    private static AuthenticationManager authenticationManager;
    private static JwtUtils jwtUtils;
    private static RefreshTokenService refreshTokenService;
    private static EmailService emailService;
    private static AuthService authService;

    private static final String TEST_KEY_PREFIX = "test:oauth2_ticket:";
    private final List<String> createdKeys = new CopyOnWriteArrayList<>();

    private static String redisHost = "localhost";
    private static int redisPort = 6379;
    private static boolean isTestcontainersRunning = false;

    @BeforeAll
    static void initRedis() {
        objectMapper = new ObjectMapper();

        // 1. Ưu tiên khởi tạo Testcontainers Redis container độc lập
        try {
            redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
            redisContainer.start();
            redisHost = redisContainer.getHost();
            redisPort = redisContainer.getMappedPort(6379);
            isTestcontainersRunning = true;
            System.out.println(">>> Đã khởi động thành công Testcontainers Redis tại: " + redisHost + ":" + redisPort);
        } catch (Exception e) {
            // Fallback an toàn sang Redis test instance cục bộ
            System.out.println(">>> Testcontainers không khả dụng (" + e.getMessage() + "), kết nối tới Redis test instance tại localhost:6379");
            redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
            try {
                redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
            } catch (NumberFormatException ignored) {
                redisPort = 6379;
            }
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        // 2. Khởi tạo AuthService với StringRedisTemplate THẬT
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtils = mock(JwtUtils.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailService = mock(EmailService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils,
                refreshTokenService,
                redisTemplate, // RedisTemplate THẬT kết nối Redis Engine
                emailService,
                objectMapper
        );
    }

    @AfterAll
    static void tearDownAll() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisContainer != null && redisContainer.isRunning()) {
            redisContainer.stop();
        }
    }

    @AfterEach
    void cleanupKeys() {
        for (String key : createdKeys) {
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
            }
        }
        createdKeys.clear();
    }

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

    private List<?> executeLuaCAD(String key, String expectedHash) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>(AuthService.COMPARE_AND_DELETE_TICKET_LUA_SCRIPT, List.class);
        return redisTemplate.execute(script, List.of(key), expectedHash);
    }

    // =========================================================================
    // FIX01-IT01 -> FIX01-IT12: CÁC TEST CASES TRỰC THI TRÊN REDIS THẬT
    // =========================================================================

    @Test
    @DisplayName("FIX01-IT01: Binding hợp lệ -> Lua trả status 1, key bị xóa khỏi Redis thật, TTL mất")
    void fix01_IT01_validBinding_success_delKey_andReturnsTicket() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String bindingHash = hashSha256("browser-binding-secret-01");
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(101L)
                .email("user01@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String json = objectMapper.writeValueAsString(ticketData);

        // Ghi trực tiếp vào Redis với TTL 60s
        redisTemplate.opsForValue().set(key, json, 60, TimeUnit.SECONDS);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Thực thi Lua Script CAD trên Redis thật
        List<?> result = executeLuaCAD(key, bindingHash);

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(1);
        assertThat(result.get(1).toString()).contains("user01@gmail.com");

        // BẰNG CHỨNG THỰC TẾ TRÊN RAM REDIS:
        // Key bị DEL hoàn toàn
        assertThat(redisTemplate.hasKey(key)).isFalse();
        // TTL trả về -2 (key không tồn tại)
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isEqualTo(-2L);
    }

    @Test
    @DisplayName("FIX01-IT02: Binding sai -> Lua trả status 2, key trong Redis thật VẪN TỒN TẠI, TTL giảm tự nhiên")
    void fix01_IT02_invalidBinding_returnsStatus2_keyPreserved_ttlNotReset() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String legitimateBindingHash = hashSha256("legitimate-binding");
        String attackerBindingHash = hashSha256("attacker-fake-binding");

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(102L)
                .email("user02@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        String json = objectMapper.writeValueAsString(ticketData);

        redisTemplate.opsForValue().set(key, json, 60, TimeUnit.SECONDS);

        // Kẻ tấn công gọi Lua Script với binding sai
        List<?> result = executeLuaCAD(key, attackerBindingHash);

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(2);

        // BẰNG CHỨNG THỰC TẾ TRÊN RAM REDIS:
        // Key KHÔNG HỀ BỊ XÓA!
        assertThat(redisTemplate.hasKey(key)).isTrue();
        String remainingData = redisTemplate.opsForValue().get(key);
        assertThat(remainingData).isEqualTo(json);

        // TTL vẫn dương và <= 60s (không bị reset về 60 hay gia hạn)
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(60L);
    }

    @Test
    @DisplayName("FIX01-IT03: Thiếu bindingSecret -> Bị từ chối tại Java, ticket trong Redis không bị động tới")
    void fix01_IT03_missingBindingSecret_rejectedInJava_ticketPreserved() throws Exception {
        String rawTicket = "ticket-for-missing-binding";
        String ticketHash = hashSha256(rawTicket);
        String redisKey = "auth:oauth2_ticket:" + ticketHash;
        createdKeys.add(redisKey);

        String bindingHash = hashSha256("valid-binding");
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(103L)
                .email("user03@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        // Gọi AuthService với binding rỗng / null
        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("thiếu thông tin xác thực trình duyệt");

        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, "   "))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("thiếu thông tin xác thực trình duyệt");

        // BẰNG CHỨNG: Key vẫn còn nguyên vẹn 100% trong Redis
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();
    }

    @Test
    @DisplayName("FIX01-IT04: Ticket không tồn tại trong Redis -> Lua trả status 0 an toàn")
    void fix01_IT04_ticketNotFound_returnsStatus0() throws Exception {
        String nonExistentKey = TEST_KEY_PREFIX + "non-existent-" + UUID.randomUUID();

        List<?> result = executeLuaCAD(nonExistentKey, hashSha256("any-hash"));

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(0);
        assertThat(result.get(1).toString()).isEmpty();
    }

    @Test
    @DisplayName("FIX01-IT05: Ticket hết hạn TTL thật trong Redis -> Lua trả status 0, xác nhận key đã hết hạn")
    void fix01_IT05_ticketExpired_returnsStatus0() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String bindingHash = hashSha256("binding-for-expire");
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(105L)
                .email("user05@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();

        // Đặt TTL cực ngắn: 1 giây
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ticketData), 1, TimeUnit.SECONDS);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Chờ 1.5 giây để Redis tự động expire key thật
        Thread.sleep(1500);

        // Xác nhận key đã bay màu trong Redis
        assertThat(redisTemplate.hasKey(key)).isFalse();

        // Chạy Lua Script
        List<?> result = executeLuaCAD(key, bindingHash);

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("FIX01-IT06: Ticket thiếu bindingHash -> Lua trả status 2, ticket không bị xóa")
    void fix01_IT06_ticketMissingBindingHash_returnsStatus2_keyPreserved() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        // JSON không có trường bindingHash
        String payloadWithoutBinding = "{\"userId\":106,\"email\":\"user06@gmail.com\",\"createdAt\":1700000000}";
        redisTemplate.opsForValue().set(key, payloadWithoutBinding, 60, TimeUnit.SECONDS);

        List<?> result = executeLuaCAD(key, hashSha256("some-expected-hash"));

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(2);

        // BẰNG CHỨNG: Vé không bị xóa
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    @DisplayName("FIX01-IT07: JSON dị dạng không hợp lệ -> pcall bắt lỗi an toàn, Lua trả status 0, không crash Redis")
    void fix01_IT07_malformedJson_handledGracefully_returnsStatus0() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        // Chuỗi hoàn toàn không phải JSON
        redisTemplate.opsForValue().set(key, "invalid-json-payload-{{{---not-closed", 60, TimeUnit.SECONDS);

        List<?> result = executeLuaCAD(key, hashSha256("any-hash"));

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(0);

        // Script không bị throw exception unhandled, Redis engine hoạt động bình thường
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    @DisplayName("FIX01-IT08: JSON hợp lệ nhưng sai kiểu dữ liệu (number, boolean, array, string) -> Lua xử lý an toàn không crash")
    void fix01_IT08_validJsonWrongDataTypes_handledSafely() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);
        String expectedHash = hashSha256("expected-hash");

        // 1. JSON number ("12345")
        redisTemplate.opsForValue().set(key, "12345", 60, TimeUnit.SECONDS);
        List<?> resNumber = executeLuaCAD(key, expectedHash);
        assertThat(((Number) resNumber.get(0)).intValue()).isEqualTo(0);

        // 2. JSON boolean ("true")
        redisTemplate.opsForValue().set(key, "true", 60, TimeUnit.SECONDS);
        List<?> resBool = executeLuaCAD(key, expectedHash);
        assertThat(((Number) resBool.get(0)).intValue()).isEqualTo(0);

        // 3. JSON array ("[1, 2, 3]")
        redisTemplate.opsForValue().set(key, "[1, 2, 3]", 60, TimeUnit.SECONDS);
        List<?> resArray = executeLuaCAD(key, expectedHash);
        assertThat(((Number) resArray.get(0)).intValue()).isEqualTo(2);

        // 4. JSON string ("\"hello world\"")
        redisTemplate.opsForValue().set(key, "\"hello world\"", 60, TimeUnit.SECONDS);
        List<?> resString = executeLuaCAD(key, expectedHash);
        assertThat(((Number) resString.get(0)).intValue()).isEqualTo(0);

        // 5. JSON object nhưng bindingHash là number thay vì string: {"bindingHash": 99999}
        redisTemplate.opsForValue().set(key, "{\"userId\":108,\"bindingHash\":99999}", 60, TimeUnit.SECONDS);
        List<?> resWrongFieldType = executeLuaCAD(key, expectedHash);
        assertThat(((Number) resWrongFieldType.get(0)).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("FIX01-IT09: Replay Attack trên Redis thật -> Lần 1 trả status 1, lần 2 trả status 0")
    void fix01_IT09_replayAttack_secondCallReturnsStatus0() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String bindingHash = hashSha256("replay-binding");
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(109L)
                .email("user09@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        // Lần 1: Thành công
        List<?> firstCall = executeLuaCAD(key, bindingHash);
        assertThat(((Number) firstCall.get(0)).intValue()).isEqualTo(1);
        assertThat(redisTemplate.hasKey(key)).isFalse();

        // Lần 2: Thất bại ngay
        List<?> secondCall = executeLuaCAD(key, bindingHash);
        assertThat(((Number) secondCall.get(0)).intValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("FIX01-IT10: Hai request hợp lệ đồng thời trên Redis thật -> Chính xác 1 request thành công, 1 request thất bại")
    void fix01_IT10_concurrentValidRequests_onlyOneGetsStatus1() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String bindingHash = hashSha256("concurrent-valid-binding");
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(110L)
                .email("user10@gmail.com")
                .bindingHash(bindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Integer> statuses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List<?> res = executeLuaCAD(key, bindingHash);
                    if (res != null && !res.isEmpty()) {
                        statuses.add(((Number) res.get(0)).intValue());
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Kích hoạt đồng thời 2 luồng bắn vào Redis server
        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(statuses).hasSize(2);

        // CHỨNG MINH TÍNH NGUYÊN TỬ THỰC TẾ TRÊN REDIS:
        // Đúng 1 request nhận status 1, đúng 1 request nhận status 0
        long count1 = statuses.stream().filter(s -> s == 1).count();
        long count0 = statuses.stream().filter(s -> s == 0).count();

        assertThat(count1).isEqualTo(1);
        assertThat(count0).isEqualTo(1);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("FIX01-IT11: Request sai và Request đúng đồng thời trên Redis thật -> Request sai không thể xóa vé của request đúng")
    void fix01_IT11_concurrentInvalidAndValidRequests_attackerCannotBlockValid() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String legitimateBindingHash = hashSha256("legitimate-binding-11");
        String attackerBindingHash = hashSha256("attacker-fake-binding-11");

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(111L)
                .email("user11@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger attackerStatus = new AtomicInteger(-1);
        AtomicInteger legitimateStatus = new AtomicInteger(-1);

        // Luồng Kẻ tấn công (Binding sai)
        executor.submit(() -> {
            try {
                startLatch.await();
                List<?> res = executeLuaCAD(key, attackerBindingHash);
                attackerStatus.set(((Number) res.get(0)).intValue());
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Luồng Người dùng chính chủ (Binding đúng)
        executor.submit(() -> {
            try {
                startLatch.await();
                List<?> res = executeLuaCAD(key, legitimateBindingHash);
                legitimateStatus.set(((Number) res.get(0)).intValue());
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        // CHỨNG MINH TUYỆT ĐỐI VỀ MẶT BẢO MẬT:
        // Request hợp lệ BẮT BUỘC phải nhận status 1 (luôn tiêu thụ thành công)
        assertThat(legitimateStatus.get()).isEqualTo(1);

        // Kẻ tấn công nhận status 2 (nếu đến trước) hoặc status 0 (nếu đến sau), KHÔNG BAO GIỜ nhận được 1
        assertThat(attackerStatus.get()).isIn(2, 0);

        // Cuối cùng vé bị xóa bởi người dùng chính chủ
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("FIX01-IT12: Kiểm tra TTL khi binding sai -> Key còn tồn tại, TTL không bị reset hoặc gia hạn")
    void fix01_IT12_ttlVerificationOnInvalidBinding() throws Exception {
        String key = TEST_KEY_PREFIX + UUID.randomUUID();
        createdKeys.add(key);

        String legitimateBindingHash = hashSha256("real-binding-12");
        String fakeBindingHash = hashSha256("fake-binding-12");

        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(112L)
                .email("user12@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();

        // Đặt TTL 60s
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        Long initialTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(initialTtl).isNotNull().isGreaterThan(0L).isLessThanOrEqualTo(60L);

        // Chờ 150ms trôi qua
        Thread.sleep(150);

        // Kẻ tấn công gọi với binding sai
        List<?> result = executeLuaCAD(key, fakeBindingHash);
        assertThat(((Number) result.get(0)).intValue()).isEqualTo(2);

        // Đọc lại TTL sau khi kẻ tấn công gọi
        Long afterTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(afterTtl).isNotNull().isGreaterThan(0L);

        // BẰNG CHỨNG: TTL tiếp tục giảm tự nhiên, không hề bị PEXPIRE/EXPIRE gia hạn lại thành 60
        assertThat(afterTtl).isLessThanOrEqualTo(initialTtl);
    }

    // =========================================================================
    // PHASE 5: TÍCH HỢP TOÀN BỘ VỚI AUTH SERVICE TRÊN REDIS THẬT
    // =========================================================================

    @Test
    @DisplayName("FIX01-IT13: AuthService End-to-End trên Redis thật - Attacker fail, Legitimate pass, Replay fail")
    void fix01_IT13_fullAuthServiceIntegration_withRealRedis() throws Exception {
        String rawTicket = "real-e2e-ticket-" + UUID.randomUUID();
        String ticketHash = hashSha256(rawTicket);
        String redisKey = "auth:oauth2_ticket:" + ticketHash;
        createdKeys.add(redisKey);

        String legitimateBinding = "legitimate-user-secret-13";
        String attackerBinding = "attacker-fake-secret-13";

        String legitimateBindingHash = hashSha256(legitimateBinding);

        // Ghi ticket vào Redis thật theo đúng chuẩn Success Handler
        OAuth2TicketData ticketData = OAuth2TicketData.builder()
                .userId(200L)
                .email("e2e@gmail.com")
                .bindingHash(legitimateBindingHash)
                .createdAt(System.currentTimeMillis())
                .build();
        redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(ticketData), 60, TimeUnit.SECONDS);

        // Chuẩn bị Mock User cho tầng Database
        User user = User.builder()
                .id(200L)
                .email("e2e@gmail.com")
                .fullName("E2E User")
                .password("$2a$12$dummyPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();
        when(userRepository.findById(200L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("e2e-jwt-access-token");
        when(jwtUtils.getExpirationInSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("e2e-refresh-token");

        // BƯỚC 1: Kẻ tấn công gọi AuthService với binding sai
        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, attackerBinding))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không khớp với phiên trình duyệt khởi tạo");

        // BẰNG CHỨNG: Vé trong Redis thật VẪN CÒN NGUYÊN!
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        // BƯỚC 2: Người dùng chính chủ gọi AuthService với binding đúng
        AuthResponse response = authService.exchangeOAuth2Ticket(rawTicket, legitimateBinding);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("e2e-jwt-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("e2e-refresh-token");

        // BẰNG CHỨNG: Sau khi người dùng chính chủ đổi vé thành công, vé trong Redis thật ĐÃ BỊ XÓA HOÀN TOÀN
        assertThat(redisTemplate.hasKey(redisKey)).isFalse();

        // BƯỚC 3: Thử Replay Attack bằng cùng vé và binding đúng -> Bị từ chối ngay lập tức
        assertThatThrownBy(() -> authService.exchangeOAuth2Ticket(rawTicket, legitimateBinding))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Mã xác thực không hợp lệ hoặc đã hết hạn");
    }
}

