package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ForgotPasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.SendRegistrationOtpRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.dto.request.ResendVerificationRequest;
import com.example.ecommerce.dto.request.VerifyEmailRequest;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.response.RegisterResponse;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.LockedAccountException;
import com.example.ecommerce.exception.UnverifiedAccountException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.security.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Nghiệp vụ xác thực người dùng: đăng ký và đăng nhập.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Nguyên tắc bảo mật:
 *   - Mật khẩu được hash BCrypt (cost=12) trước khi lưu DB.
 *   - Email được chuẩn hóa lowercase + trim để tránh đăng ký trùng.
 *   - Không ghi password hoặc JWT vào log.
 *   - Lỗi đăng nhập sai (BadCredentialsException) được xử lý tại GlobalExceptionHandler → 401.
 *   - Tài khoản mới mặc định role=CUSTOMER, active=true.
 *   - Không cho phép client tự gán role ADMIN qua API đăng ký.
 */
// TSK-AUTH-00
// [TSK-D02-CCT]
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.otp.hmac-secret:ecommerce-default-otp-hmac-secret-change-in-production-2024}")
    private String otpHmacSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private static final String DEFAULT_DEV_HMAC_SECRET = "ecommerce-default-otp-hmac-secret-change-in-production-2024";

    /**
     * Xác thực an toàn khóa bí mật HMAC khi khởi động ứng dụng.
     * Trên môi trường Production (docker, prod, production), bắt buộc phải cấu hình
     * biến môi trường OTP_HMAC_SECRET riêng biệt, tối thiểu 32 ký tự và không dùng secret mặc định.
     */
    @PostConstruct
    public void validateOtpHmacSecret() {
        boolean isProduction = "docker".equalsIgnoreCase(activeProfile)
                || "prod".equalsIgnoreCase(activeProfile)
                || "production".equalsIgnoreCase(activeProfile);

        if (!StringUtils.hasText(otpHmacSecret) || otpHmacSecret.trim().length() < 32) {
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: Biến môi trường OTP_HMAC_SECRET bắt buộc phải được cấu hình và có độ dài tối thiểu 32 ký tự trên môi trường Production.");
            } else {
                log.warn("CẢNH BÁO BẢO MẬT: OTP_HMAC_SECRET chưa được cấu hình hoặc ngắn hơn 32 ký tự. Đang sử dụng fallback tạm thời cho môi trường dev/test.");
            }
        } else if (isProduction && DEFAULT_DEV_HMAC_SECRET.equals(otpHmacSecret.trim())) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: Không được phép sử dụng secret mặc định cho OTP_HMAC_SECRET trên môi trường Production. Vui lòng thiết lập biến môi trường OTP_HMAC_SECRET an toàn.");
        }
    }

    @Value("${app.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${app.otp.rate-limit-seconds:60}")
    private long otpRateLimitSeconds;

    @Value("${app.otp.max-attempts:5}")
    private int otpMaxAttempts;

    private static final String OTP_KEY_PREFIX = "auth:otp:";
    private static final String OTP_RATELIMIT_PREFIX = "auth:otp_ratelimit:";
    private static final String OTP_ATTEMPTS_PREFIX = "auth:otp_attempts:";

    private static final String VERIFY_OTP_KEY_PREFIX = "auth:verify_otp:";
    private static final String VERIFY_RATELIMIT_PREFIX = "auth:verify_ratelimit:";
    private static final String VERIFY_ATTEMPTS_PREFIX = "auth:verify_attempts:";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Lua script xác thực và tiêu thụ OTP nguyên tử (TSK-D10-CCT).
     * Chống race condition đa luồng/đa node, kiểm soát brute-force 5 lần thử,
     * và khóa trạng thái CLAIMED trước khi thực thi cập nhật DB.
     *
     * KEYS[1]: auth:otp:<email>
     * KEYS[2]: auth:otp_attempts:<email>
     * ARGV[1]: expectedOtpHmac
     * ARGV[2]: maxAttempts (5)
     * ARGV[3]: claimTtlSeconds (60)
     */
    private static final String VERIFY_AND_CLAIM_LUA_SCRIPT =
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

    /**
     * Lua script giải phóng khóa rate limit nguyên tử (Compare-and-Delete).
     * Chỉ giải phóng khi khóa KEYS[1] có giá trị đúng bằng ARGV[1] (owner lockValue).
     * Tránh tuyệt đối race condition xóa nhầm khóa của request khác khi khóa cũ hết hạn.
     *
     * KEYS[1]: rateLimitKey
     * ARGV[1]: expectedLockValue
     * Trả về: 1 nếu xóa thành công, 0 nếu không khớp owner hoặc key không tồn tại.
     */
    private static final String RELEASE_RATE_LIMIT_LOCK_LUA_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call('DEL', KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    /**
     * Giải phóng khóa rate limit nguyên tử bằng Lua script (Compare-and-Delete).
     *
     * @param rateLimitKey Redis key của khóa rate limit
     * @param lockValue UUID đại diện cho request sở hữu khóa
     * @return true nếu giải phóng thành công (owner khớp), false nếu owner không khớp hoặc key không tồn tại
     */
    public boolean releaseRateLimitLock(String rateLimitKey, String lockValue) {
        if (!StringUtils.hasText(rateLimitKey) || !StringUtils.hasText(lockValue)) {
            return false;
        }
        try {
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(RELEASE_RATE_LIMIT_LOCK_LUA_SCRIPT, Long.class),
                    List.of(rateLimitKey),
                    lockValue
            );
            return result != null && result == 1L;
        } catch (Exception ex) {
            log.error("Lỗi khi thực thi Lua script giải phóng rate limit lock cho key {}: {}", rateLimitKey, ex.getMessage());
            return false;
        }
    }

    /**
     * Đăng ký tài khoản mới (CUSTOMER).
     *
     * Luồng:
     *   1. Kiểm tra email chưa tồn tại (hoặc từ chối nếu đã tồn tại).
     *   2. Hash password bằng BCrypt (cost 12).
     *   3. Lưu User với role=CUSTOMER, active=true, emailVerified=false (commit ngay vào DB).
     *   4. Tách biệt transaction DB và gọi SMTP: không giữ kết nối MySQL trong khi chờ gửi email.
     *   5. Sinh OTP 6 chữ số an toàn và băm HMAC-SHA256 lưu Redis.
     *   6. Gửi email xác thực OTP qua Resend SMTP (EmailService).
     *   7. Trả RegisterResponse yêu cầu xác thực email (KHÔNG cấp JWT / Refresh Token).
     *
     * @param request thông tin đăng ký đã được validate
     * @return RegisterResponse chứa thông tin tài khoản và cờ emailVerified=false
     * @throws BusinessException nếu email đã tồn tại hoặc đã đăng ký chờ xác thực
     */
    /**
     * Gửi mã OTP xác thực email trước khi tạo tài khoản vào CSDL.
     * Hoàn toàn không ghi dữ liệu người dùng vào MySQL tại bước này.
     */
    public void sendRegistrationOtp(SendRegistrationOtpRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null && Boolean.TRUE.equals(existingUser.getEmailVerified())) {
            throw new BusinessException("Email đã được sử dụng: " + normalizedEmail);
        }

        // Kiểm tra tính duy nhất của số điện thoại nếu được truyền lên
        if (StringUtils.hasText(request.getPhone())) {
            String normalizedPhone = request.getPhone().trim();
            if (userRepository.existsByPhone(normalizedPhone)) {
                throw new BusinessException("Số điện thoại này đã được sử dụng bởi một tài khoản khác: " + normalizedPhone);
            }
        }

        String rateLimitKey = VERIFY_RATELIMIT_PREFIX + normalizedEmail;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException("Vui lòng đợi 60 giây trước khi yêu cầu mã mới");
        }

        String otp = generateOtp();
        String otpHmac = calculateHmac(otp, otpHmacSecret);

        String verifyOtpKey = VERIFY_OTP_KEY_PREFIX + normalizedEmail;
        String verifyAttemptsKey = VERIFY_ATTEMPTS_PREFIX + normalizedEmail;

        redisTemplate.delete(verifyAttemptsKey);
        redisTemplate.opsForValue().set(verifyOtpKey, otpHmac, otpExpirationSeconds, TimeUnit.SECONDS);

        try {
            String fullName = request.getFullName() != null ? request.getFullName().trim() : "";
            emailService.sendVerificationOtpEmail(normalizedEmail, otp, fullName);
            log.info("Đã gửi mã OTP xác thực trước đăng ký tới: {}", normalizedEmail);
        } catch (Exception ex) {
            log.error("Gửi mã xác thực trước đăng ký thất bại cho email {}: {}", normalizedEmail, ex.getMessage());
            redisTemplate.delete(verifyOtpKey);
            releaseRateLimitLock(rateLimitKey, lockValue);
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng kiểm tra lại địa chỉ email hoặc thử lại sau.");
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // Kiểm tra số điện thoại không được trùng lặp giữa các tài khoản
        if (StringUtils.hasText(request.getPhone())) {
            String normalizedPhone = request.getPhone().trim();
            if (userRepository.existsByPhone(normalizedPhone)) {
                throw new BusinessException("Số điện thoại này đã được sử dụng bởi một tài khoản khác: " + normalizedPhone);
            }
        }

        // 1. Nếu có kèm mã OTP: Xác thực OTP ngay tại form đăng ký (KHÔNG lưu CSDL nếu chưa xác thực)
        if (StringUtils.hasText(request.getOtp())) {
            User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
            if (existingUser != null && Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                throw new BusinessException("Email đã được sử dụng: " + normalizedEmail);
            }

            String verifyOtpKey = VERIFY_OTP_KEY_PREFIX + normalizedEmail;
            String verifyAttemptsKey = VERIFY_ATTEMPTS_PREFIX + normalizedEmail;

            String storedHmac = redisTemplate.opsForValue().get(verifyOtpKey);
            if (storedHmac == null) {
                throw new BusinessException("Mã xác thực đã hết hạn hoặc chưa được gửi. Vui lòng bấm 'Gửi mã xác thực'.");
            }

            String inputHmac = calculateHmac(request.getOtp().trim(), otpHmacSecret);
            if (!storedHmac.equals(inputHmac)) {
                Long attempts = redisTemplate.opsForValue().increment(verifyAttemptsKey);
                if (attempts != null && attempts >= otpMaxAttempts) {
                    redisTemplate.delete(verifyOtpKey);
                    redisTemplate.delete(verifyAttemptsKey);
                    throw new BusinessException("Bạn đã nhập sai mã OTP quá 5 lần. Mã đã bị hủy, vui lòng bấm gửi lại mã mới.");
                }
                long remaining = Math.max(0, otpMaxAttempts - (attempts != null ? attempts : 1));
                throw new BusinessException("Mã xác thực không chính xác. Bạn còn " + remaining + " lần thử.");
            }

            // Xóa OTP trong Redis vì đã xác thực thành công
            redisTemplate.delete(verifyOtpKey);
            redisTemplate.delete(verifyAttemptsKey);

            // Dọn dẹp tài khoản chưa xác thực trước đó nếu có
            if (existingUser != null) {
                userRepository.delete(existingUser);
            }

            // CHỈ LƯU VÀO DATABASE KHI MÃ OTP ĐÃ ĐƯỢC XÁC THỰC THÀNH CÔNG!
            User user = User.builder()
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName().trim())
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .role(Role.CUSTOMER)
                    .active(true)
                    .emailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);
            log.info("Xác thực email thành công tại form đăng ký và tạo tài khoản vào CSDL: {}", normalizedEmail);

            return RegisterResponse.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .emailVerified(true)
                    .build();
        }

        // 2. Fallback luồng cũ (phục vụ tương thích ngược với unit tests):
        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (Boolean.TRUE.equals(existingUser.getEmailVerified())) {
                throw new BusinessException("Email đã được sử dụng: " + normalizedEmail);
            } else {
                throw new BusinessException("Email này đã được đăng ký nhưng chưa hoàn tất xác thực. Vui lòng kiểm tra hộp thư hoặc sử dụng tính năng gửi lại mã xác thực.");
            }
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(Role.CUSTOMER)  // Luôn là CUSTOMER — không tin client
                .active(true)
                .emailVerified(false)
                .emailVerifiedAt(null)
                .build();

        // Giai đoạn 1: Lưu tài khoản pending verification vào CSDL MySQL (commit ngay, không giữ transaction khi gọi SMTP)
        user = userRepository.save(user);
        log.info("Đăng ký tài khoản mới chờ xác thực email: {}", normalizedEmail);

        // Giai đoạn 2: Sinh OTP 6 chữ số ngẫu nhiên an toàn và băm HMAC-SHA256 lưu Redis
        String otp = generateOtp();
        String otpHmac = calculateHmac(otp, otpHmacSecret);

        String verifyOtpKey = VERIFY_OTP_KEY_PREFIX + normalizedEmail;
        String verifyRateLimitKey = VERIFY_RATELIMIT_PREFIX + normalizedEmail;
        String verifyAttemptsKey = VERIFY_ATTEMPTS_PREFIX + normalizedEmail;

        String lockValue = UUID.randomUUID().toString();
        // Lưu vào Redis với TTL 300s và rate-limit 60s
        redisTemplate.opsForValue().set(verifyOtpKey, otpHmac, otpExpirationSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(verifyRateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(verifyAttemptsKey);

        // Giai đoạn 3: Gửi email OTP xác thực tài khoản qua Resend SMTP
        try {
            emailService.sendVerificationOtpEmail(normalizedEmail, otp, user.getFullName());
            log.info("Đã gửi email chứa mã OTP xác thực đăng ký tài khoản tới: {}", normalizedEmail);
        } catch (Exception ex) {
            log.error("Gửi email xác thực thất bại cho email {}: {}", normalizedEmail, ex.getMessage());
            // Dọn dẹp OTP không hợp lệ vì người dùng không nhận được mã
            redisTemplate.delete(verifyOtpKey);
            // Giải phóng khóa rate limit nguyên tử bằng Lua script (Compare-and-Delete) do chính request này sở hữu
            releaseRateLimitLock(verifyRateLimitKey, lockValue);
            // Tài khoản vẫn tồn tại an toàn trong DB với emailVerified = false để người dùng có thể gửi lại OTP
            throw new BusinessException("Tài khoản đã được tạo nhưng không thể gửi email xác thực lúc này. Vui lòng bấm gửi lại mã để nhận OTP.");
        }

        return RegisterResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .emailVerified(false)
                .build();
    }

    /**
     * Xác thực địa chỉ email bằng mã OTP và kích hoạt phiên đăng nhập đầu tiên.
     *
     * @param request chứa email và OTP 6 chữ số
     * @return AuthResponse chứa Access Token và Refresh Token
     */
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        if (request == null) {
            throw new BusinessException("Dữ liệu yêu cầu không được để trống");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
        String otp = request.getOtp() != null ? request.getOtp().trim() : "";

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Tài khoản đã được xác thực email trước đó. Vui lòng đăng nhập.");
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Xác thực email thất bại do tài khoản bị khóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        // Xác thực OTP nguyên tử qua Redis Lua script (Chống race condition và brute-force)
        String verifyOtpKey = VERIFY_OTP_KEY_PREFIX + normalizedEmail;
        String verifyAttemptsKey = VERIFY_ATTEMPTS_PREFIX + normalizedEmail;
        String expectedOtpHmac = calculateHmac(otp, otpHmacSecret);

        DefaultRedisScript<String> script = new DefaultRedisScript<>(VERIFY_AND_CLAIM_LUA_SCRIPT, String.class);
        List<String> keys = Arrays.asList(verifyOtpKey, verifyAttemptsKey);

        String result;
        try {
            result = redisTemplate.execute(
                    script,
                    keys,
                    expectedOtpHmac,
                    String.valueOf(otpMaxAttempts),
                    "60" // 60 giây claim window
            );
        } catch (Exception e) {
            log.error("Lỗi khi thực thi script xác thực OTP trên Redis: {}", e.getMessage());
            throw e;
        }

        if ("NOT_FOUND".equals(result)) {
            throw new BusinessException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        if ("ALREADY_USED".equals(result)) {
            throw new BusinessException("Mã OTP đã được sử dụng");
        }
        if ("MAX_ATTEMPTS_EXCEEDED".equals(result)) {
            throw new BusinessException("Bạn đã nhập sai mã OTP quá 5 lần. Mã OTP đã bị hủy, vui lòng yêu cầu mã mới.");
        }
        if (result != null && result.startsWith("MISMATCH:")) {
            int attemptCount = Integer.parseInt(result.substring("MISMATCH:".length()));
            int remainingAttempts = Math.max(0, otpMaxAttempts - attemptCount);
            throw new BusinessException("Mã OTP không chính xác. Bạn còn " + remainingAttempts + " lần thử.");
        }

        if (result == null || !result.startsWith("SUCCESS")) {
            throw new BusinessException("Xác thực mã OTP thất bại, vui lòng thử lại");
        }

        // Cập nhật trạng thái xác thực vào Database
        try {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user = userRepository.save(user);
            log.info("Xác thực email thành công trong CSDL cho tài khoản: {}", normalizedEmail);

            // Xóa hoàn toàn OTP và Attempts key khỏi Redis sau khi DB thành công
            redisTemplate.delete(Arrays.asList(verifyOtpKey, verifyAttemptsKey));
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật trạng thái xác thực vào DB: {}", ex.getMessage());
            // Cơ chế phục hồi: khôi phục OTP trên Redis nếu DB thất bại
            try {
                long remainingTtl = 60;
                if (result.contains(":")) {
                    long parsedTtl = Long.parseLong(result.split(":")[1]);
                    if (parsedTtl > 0) {
                        remainingTtl = parsedTtl;
                    }
                }
                redisTemplate.opsForValue().set(verifyOtpKey, expectedOtpHmac, remainingTtl, TimeUnit.SECONDS);
            } catch (Exception redisRollbackEx) {
                log.warn("Không thể khôi phục lại OTP trong Redis sau lỗi DB: {}", redisRollbackEx.getMessage());
            }
            throw ex;
        }

        // Tự động cấp JWT và Refresh Token để đăng nhập người dùng ngay sau khi xác thực
        UserDetails userDetails = buildUserDetailsForToken(user);
        String token = jwtUtils.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Yêu cầu gửi lại mã OTP xác thực email (Rate limit 60s, Anti-Enumeration).
     *
     * @param request chứa địa chỉ email cần gửi lại mã
     */
    public void resendVerification(ResendVerificationRequest request) {
        if (request == null || !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("Email không được để trống");
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Kiểm tra và chiếm Rate Limit NGUYÊN TỬ (setIfAbsent - tương đương SET NX EX 60s)
        // Chống race condition: Chỉ 1 request đồng thời duy nhất chiếm được khóa, chặn hoàn toàn bypass
        String rateLimitKey = VERIFY_RATELIMIT_PREFIX + normalizedEmail;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(rateLimitKey, lockValue, otpRateLimitSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("Yêu cầu gửi lại OTP xác thực quá nhanh: email={}", normalizedEmail);
            throw new BusinessException("Vui lòng đợi 60 giây trước khi yêu cầu mã mới");
        }

        // 2. Kiểm tra điều kiện tài khoản trong CSDL
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean isEligibleUser = user != null
                && Boolean.TRUE.equals(user.getActive())
                && Boolean.FALSE.equals(user.getEmailVerified());

        if (!isEligibleUser) {
            // User Enumeration Protection:
            // Không gửi OTP, không tiết lộ tài khoản không tồn tại, bị khóa hay đã xác thực.
            // GIỮ NGUYÊN khóa rate limit vừa chiếm trong Redis để chống tấn công quét dò email.
            log.info("Yêu cầu gửi lại mã cho tài khoản không đủ điều kiện (đã xác thực hoặc không tồn tại): {}", normalizedEmail);
            return;
        }

        // 3. Sinh OTP 6 chữ số ngẫu nhiên an toàn và băm HMAC-SHA256
        String otp = generateOtp();
        String otpHmac = calculateHmac(otp, otpHmacSecret);

        String verifyOtpKey = VERIFY_OTP_KEY_PREFIX + normalizedEmail;
        String verifyAttemptsKey = VERIFY_ATTEMPTS_PREFIX + normalizedEmail;

        // 4. Xóa bộ đếm attempts cũ, sau đó lưu OTP mới vào Redis với TTL 300s
        redisTemplate.delete(verifyAttemptsKey);
        redisTemplate.opsForValue().set(verifyOtpKey, otpHmac, otpExpirationSeconds, TimeUnit.SECONDS);

        // 5. Gửi email OTP xác thực
        try {
            emailService.sendVerificationOtpEmail(normalizedEmail, otp, user.getFullName());
            log.info("Đã gửi lại email chứa mã OTP xác thực thành công tới: {}", normalizedEmail);
        } catch (Exception ex) {
            log.error("Gửi lại email OTP xác thực thất bại cho email {}: {}", normalizedEmail, ex.getMessage());
            // Xóa OTP không hợp lệ khỏi Redis
            redisTemplate.delete(verifyOtpKey);
            // Giải phóng khóa rate limit nguyên tử bằng Lua script (Compare-and-Delete) do chính request này sở hữu
            releaseRateLimitLock(rateLimitKey, lockValue);
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");
        }
    }

    /**
     * Đăng nhập bằng email và mật khẩu.
     *
     * Luồng:
     *   1. Gọi authenticationManager.authenticate()
     *      → DaoAuthenticationProvider gọi CustomUserDetailsService.loadUserByUsername()
     *      → So sánh password với BCrypt hash trong DB
     *   2. Nếu sai → ném BadCredentialsException → GlobalExceptionHandler → 401
     *   3. Nếu đúng → tạo JWT Access Token, tạo Refresh Token trong Redis và trả AuthResponse
     *
     * @param request thông tin đăng nhập đã được validate
     * @return AuthResponse chứa token và thông tin user
     * @throws org.springframework.security.authentication.BadCredentialsException nếu sai thông tin
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Kiểm tra trạng thái tài khoản: Nếu tài khoản đã bị khóa, chặn ngay lập tức và ném LockedAccountException kèm lý do thực tế từ DB
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null && Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Đăng nhập thất bại do tài khoản bị khóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        // 2. Xác thực thông tin đăng nhập (email & password)
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (DisabledException ex) {
            String lockReason = (user != null && StringUtils.hasText(user.getLockReason()))
                    ? user.getLockReason().trim()
                    : null;
            log.warn("Đăng nhập thất bại do tài khoản bị vô hiệu hóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (user == null) {
            user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));
        }

        // Kiểm tra xác thực email đối với CUSTOMER (ADMIN không bị ảnh hưởng)
        if (user.getRole() == Role.CUSTOMER && Boolean.FALSE.equals(user.getEmailVerified())) {
            log.warn("Đăng nhập thất bại do tài khoản CUSTOMER chưa xác thực email: email={}", normalizedEmail);
            throw new UnverifiedAccountException(
                    "Tài khoản của bạn chưa được xác thực email. Vui lòng xác thực email để kích hoạt tài khoản.",
                    normalizedEmail
            );
        }

        // 3. Phân tách và kiểm soát cổng đăng nhập phía Backend (Defense in Depth)
        if (StringUtils.hasText(request.getPortal())) {
            String portal = request.getPortal().trim();
            if ("CUSTOMER".equalsIgnoreCase(portal)) {
                // Chỉ cho phép tài khoản CUSTOMER đăng nhập tại cổng Customer
                if (user.getRole() != Role.CUSTOMER) {
                    log.warn("Từ chối truy cập cổng Customer cho tài khoản có vai trò {}: email={}",
                            user.getRole(), normalizedEmail);
                    // Thông báo chung tự nhiên, tuyệt đối không tiết lộ vai trò hoặc sự tồn tại của ADMIN
                    throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
                }
            } else if ("ADMIN".equalsIgnoreCase(portal)) {
                // Chỉ cho phép tài khoản ADMIN đăng nhập tại cổng Admin
                if (user.getRole() != Role.ADMIN) {
                    log.warn("Từ chối truy cập cổng Admin cho tài khoản không có quyền quản trị: email={}",
                            normalizedEmail);
                    throw new BadCredentialsException("Email hoặc mật khẩu không chính xác.");
                }
            }
        }

        String token = jwtUtils.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Đăng nhập thành công: {}", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Làm mới Access Token bằng Refresh Token (Token Rotation) - BUG-02 Fix.
     *
     * Luồng:
     *   1. Đọc và kiểm tra tính hợp lệ của Refresh Token từ Redis trước khi rotation
     *   2. Xác minh người dùng trong MySQL trước khi rotation (user tồn tại, active = true)
     *   3. Hòa giải trạng thái: kiểm tra mật khẩu có bị thay đổi sau khi token được cấp không
     *   4. Chỉ khi DB và người dùng hợp lệ mới thực hiện rotation nguyên tử trên Redis
     *   5. Sinh Access Token JWT mới (15 phút) và trả về AuthResponse kèm Refresh Token mới
     *
     * Lưu ý kiến trúc: Kiểm tra MySQL rồi xoay vòng Redis là quy trình 2 bước với fail-closed,
     * không phải giao dịch phân tán 2PC. Nếu DB lỗi, token chưa bị xoay và client có thể thử lại an toàn.
     *
     * @param rawRefreshToken chuỗi refresh token nhận từ cookie HttpOnly
     * @return AuthResponse chứa accessToken mới và refreshToken mới
     */
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc không tồn tại");
        }

        // 1. Kiểm tra tính hợp lệ của Refresh Token và đọc metadata từ Redis trước khi rotation
        RefreshTokenService.RefreshTokenData tokenData = refreshTokenService.getTokenData(rawRefreshToken);

        // 2. Xác minh người dùng trong MySQL trước khi rotation
        User user = userRepository.findById(tokenData.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng"));

        if (Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Làm mới token thất bại do tài khoản bị khóa: userId={}, lý do={}", user.getId(), lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        if (user.getRole() == Role.CUSTOMER && Boolean.FALSE.equals(user.getEmailVerified())) {
            log.warn("Làm mới token thất bại do tài khoản chưa xác thực email: userId={}", user.getId());
            throw new UnverifiedAccountException(
                    "Tài khoản của bạn chưa được xác thực email. Vui lòng xác thực email để kích hoạt tài khoản.",
                    user.getEmail()
            );
        }

        // 3. Hòa giải trạng thái DB - Redis: Nếu mật khẩu đã thay đổi sau khi token được cấp -> từ chối
        if (tokenData.getPasswordHash() != null && !tokenData.getPasswordHash().equals(user.getPassword())) {
            log.warn("Refresh token bị từ chối do mật khẩu đã thay đổi sau khi token được cấp: userId={}", user.getId());
            throw new BadCredentialsException("Mật khẩu tài khoản đã thay đổi. Vui lòng đăng nhập lại.");
        }

        // 4. Chỉ khi DB và người dùng hợp lệ mới thực hiện rotation nguyên tử trên Redis
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.rotateRefreshToken(rawRefreshToken);

        // 5. Sinh JWT Access Token mới và trả về
        UserDetails userDetails = buildUserDetailsForToken(user);
        String newAccessToken = jwtUtils.generateToken(userDetails);

        log.info("Làm mới Access Token thành công cho user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(UserResponse.fromEntity(user))
                .refreshToken(rotationResult.getRawRefreshToken())
                .build();
    }

    /**
     * Đăng xuất: thu hồi Refresh Token trong Redis.
     *
     * @param rawRefreshToken token thô nhận từ cookie
     */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    /**
     * Yêu cầu đặt lại mật khẩu và gửi OTP qua Email (TSK-D10-CCT).
     *
     * Nguyên tắc an ninh:
     *   - Email được chuẩn hóa lowercase và trim.
     *   - Áp dụng Rate Limit tối thiểu 60 giây giữa các lần yêu cầu gửi OTP.
     *   - User Enumeration Protection: Phản hồi giống nhau bất kể tài khoản có tồn tại, bị khóa hay không.
     *   - OTP gồm 6 chữ số ngẫu nhiên an toàn (giữ số 0 đầu) và được băm HMAC-SHA256 trước khi lưu Redis.
     *   - TTL OTP trong Redis là 300 giây (5 phút).
     *   - Khi gửi email thất bại, hủy OTP trong Redis nhưng giữ rate-limit để chống spam SMTP.
     *
     * @param request thông tin yêu cầu chứa email
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null || !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("Email không được để trống");
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        // 1. Kiểm tra Rate Limit (tối thiểu 60 giây giữa các lần yêu cầu)
        String rateLimitKey = OTP_RATELIMIT_PREFIX + normalizedEmail;
        Boolean isRateLimited = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(isRateLimited)) {
            log.warn("Yêu cầu gửi OTP quá nhanh bị từ chối do Rate Limit (60s): email={}", normalizedEmail);
            throw new BusinessException("Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới");
        }

        // 2. Kiểm tra điều kiện tài khoản trong CSDL
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean isEligibleUser = user != null && Boolean.TRUE.equals(user.getActive());

        if (!isEligibleUser) {
            // User Enumeration Protection:
            // Không gửi OTP, không tiết lộ tài khoản không tồn tại hoặc bị khóa
            log.info("Yêu cầu quên mật khẩu cho tài khoản không tồn tại hoặc không đủ điều kiện (bị khóa/inactive): {}", normalizedEmail);
            return;
        }

        // 3. Sinh OTP 6 chữ số an toàn và băm HMAC-SHA256
        String otp = generateOtp();
        String otpHmac = calculateHmac(otp, otpHmacSecret);

        String otpKey = OTP_KEY_PREFIX + normalizedEmail;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + normalizedEmail;

        // 4. Lưu OTP vào Redis với TTL 300s (5 phút) và rate-limit key 60s
        redisTemplate.opsForValue().set(otpKey, otpHmac, otpExpirationSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(rateLimitKey, "1", otpRateLimitSeconds, TimeUnit.SECONDS);
        redisTemplate.delete(attemptsKey);

        // 5. Gửi email OTP
        try {
            emailService.sendOtpEmail(normalizedEmail, otp);
            log.info("Đã gửi email chứa mã OTP đặt lại mật khẩu thành công tới: {}", normalizedEmail);
        } catch (Exception ex) {
            log.error("Gửi email OTP thất bại cho email {}: {}", normalizedEmail, ex.getMessage());
            // Xóa OTP khỏi Redis vì người dùng không nhận được mã
            redisTemplate.delete(otpKey);
            // KHÔNG xóa rateLimitKey để chống spam liên tục khi SMTP server gặp sự cố
            throw new BusinessException("Không thể gửi email xác thực lúc này, vui lòng thử lại sau ít phút");
        }
    }

    /**
     * Xác thực mã OTP và đặt lại mật khẩu mới (TSK-D10-CCT).
     *
     * Nguyên tắc an ninh:
     *   - Xác thực OTP nguyên tử (Atomic Verify & Claim) qua Redis Lua Script:
     *     + Chống race condition khi có nhiều request đồng thời dùng chung 1 OTP.
     *     + Giới hạn tối đa 5 lần nhập sai, tự động hủy OTP khi vượt quá.
     *     + Đánh dấu trạng thái CLAIMED trước khi thực thi cập nhật CSDL.
     *   - Kiểm tra xác nhận mật khẩu và không cho phép đặt lại trùng mật khẩu hiện tại.
     *   - Từ chối nếu tài khoản bị khóa trong CSDL.
     *   - Mã hóa mật khẩu mới bằng BCrypt strength 12.
     *   - Thu hồi toàn bộ Refresh Token của user thông qua RefreshTokenService.revokeAllUserSessions.
     *   - Xóa hoàn toàn OTP và Attempts key khỏi Redis sau khi hoàn tất.
     *   - Cơ chế phục hồi: Phục hồi lại OTP nếu transaction CSDL gặp sự cố.
     *
     * @param request thông tin đặt lại mật khẩu
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new BusinessException("Dữ liệu yêu cầu không được để trống");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
        String otp = request.getOtp() != null ? request.getOtp().trim() : "";
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. Kiểm tra xác nhận mật khẩu khớp
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // 2. Kiểm tra tài khoản trong CSDL trước khi tiêu thụ OTP
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));

        if (Boolean.FALSE.equals(user.getActive())) {
            String lockReason = StringUtils.hasText(user.getLockReason()) ? user.getLockReason().trim() : null;
            log.warn("Đặt lại mật khẩu thất bại do tài khoản bị khóa: email={}, lý do={}", normalizedEmail, lockReason);
            throw new LockedAccountException("Tài khoản của bạn đã bị khóa.", lockReason);
        }

        // 3. Kiểm tra mật khẩu mới không trùng mật khẩu hiện tại
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        // 4. Xác thực OTP nguyên tử qua Redis Lua script (Chống race condition và brute-force)
        String otpKey = OTP_KEY_PREFIX + normalizedEmail;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + normalizedEmail;
        String expectedOtpHmac = calculateHmac(otp, otpHmacSecret);

        DefaultRedisScript<String> script = new DefaultRedisScript<>(VERIFY_AND_CLAIM_LUA_SCRIPT, String.class);
        List<String> keys = Arrays.asList(otpKey, attemptsKey);

        String result;
        try {
            result = redisTemplate.execute(
                    script,
                    keys,
                    expectedOtpHmac,
                    String.valueOf(otpMaxAttempts),
                    "60" // 60 giây claim window
            );
        } catch (Exception e) {
            log.error("Lỗi khi thực thi script xác thực OTP trên Redis: {}", e.getMessage());
            throw e;
        }

        if ("NOT_FOUND".equals(result)) {
            throw new BusinessException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        if ("ALREADY_USED".equals(result)) {
            throw new BusinessException("Mã OTP đã được sử dụng");
        }
        if ("MAX_ATTEMPTS_EXCEEDED".equals(result)) {
            throw new BusinessException("Bạn đã nhập sai mã OTP quá 5 lần. Mã OTP đã bị hủy, vui lòng yêu cầu mã mới.");
        }
        if (result != null && result.startsWith("MISMATCH:")) {
            int attemptCount = Integer.parseInt(result.substring("MISMATCH:".length()));
            int remainingAttempts = Math.max(0, otpMaxAttempts - attemptCount);
            throw new BusinessException("Mã OTP không chính xác. Bạn còn " + remainingAttempts + " lần thử.");
        }

        if (result == null || !result.startsWith("SUCCESS")) {
            throw new BusinessException("Xác thực mã OTP thất bại, vui lòng thử lại");
        }

        // 5. Cập nhật mật khẩu mới vào Database và thu hồi các phiên đăng nhập
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("Đặt lại mật khẩu thành công trong CSDL cho tài khoản: {}", normalizedEmail);

            // Thu hồi toàn bộ Refresh Token của user trong Redis
            refreshTokenService.revokeAllUserSessions(user.getId());
            log.info("Đã thu hồi tất cả phiên đăng nhập Redis cho user ID={}", user.getId());

            // Xóa hoàn toàn OTP và Attempts key khỏi Redis sau khi DB thành công
            redisTemplate.delete(Arrays.asList(otpKey, attemptsKey));

            // Gửi email thông báo bảo mật đặt lại mật khẩu thành công
            try {
                emailService.sendSecurityAlertEmail(
                        user.getEmail(),
                        user.getFullName(),
                        "Đặt lại mật khẩu thành công",
                        "Mật khẩu tài khoản Rikkei Store của bạn vừa được đặt lại thành công thông qua mã xác thực OTP."
                );
            } catch (Exception mailEx) {
                log.warn("Không thể gửi email thông báo đặt lại mật khẩu cho {}: {}", user.getEmail(), mailEx.getMessage());
            }
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật mật khẩu mới vào DB: {}", ex.getMessage());
            // Cơ chế phục hồi: khôi phục OTP trên Redis nếu DB thất bại
            try {
                long remainingTtl = 60;
                if (result.contains(":")) {
                    long parsedTtl = Long.parseLong(result.split(":")[1]);
                    if (parsedTtl > 0) {
                        remainingTtl = parsedTtl;
                    }
                }
                redisTemplate.opsForValue().set(otpKey, expectedOtpHmac, remainingTtl, TimeUnit.SECONDS);
            } catch (Exception redisRollbackEx) {
                log.warn("Không thể khôi phục lại OTP trong Redis sau lỗi DB: {}", redisRollbackEx.getMessage());
            }
            throw ex;
        }
    }

    /**
     * Sinh mã OTP 6 chữ số ngẫu nhiên an toàn (000000 - 999999), bảo tồn số 0 ở đầu.
     */
    private String generateOtp() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    /**
     * Tính toán mã băm HMAC-SHA256 cho chuỗi dữ liệu.
     */
    public static String calculateHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi tính toán HMAC-SHA256", e);
        }
    }

    // ---------- Private helpers ----------

    /**
     * Tạo Spring Security UserDetails từ User entity để generate JWT token.
     * Chỉ dùng sau khi đăng ký (trước khi có Authentication từ authenticationManager).
     */
    private UserDetails buildUserDetailsForToken(User user) {
        boolean isEmailVerified = Boolean.TRUE.equals(user.getEmailVerified()) || user.getRole() == Role.ADMIN;
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive()),
                isEmailVerified
        );
    }
}
