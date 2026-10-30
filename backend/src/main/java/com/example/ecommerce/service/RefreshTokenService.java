package com.example.ecommerce.service;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service quản lý Refresh Token trong Redis.
 *
 * Nguyên tắc kiến trúc & bảo mật:
 * 1. Opaque Token: Tạo bằng SecureRandom (64 bytes), Base64 URL-safe, không chứa thông tin người dùng.
 * 2. Bảo mật Redis: CHỈ lưu SHA-256 hash của token, không bao giờ lưu token thô.
 * 3. Atomic Token Rotation: Sử dụng Redis Lua script bảo đảm tính nguyên tử (chống race condition đa luồng/đa node).
 * 4. Tombstone & Reuse Detection: Token đã xoay vòng được giữ trạng thái ROTATED tối đa 120 giây.
 *    Nếu phát hiện token cũ được sử dụng lại sau grace period, lập tức thu hồi toàn bộ phiên của tài khoản.
 * 5. Fail-Closed: Mọi lỗi kết nối Redis đều được xử lý chặt chẽ, không cấp token không kiểm chứng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs; // 7 ngày = 604800000ms

    @Value("${jwt.grace-period:10000}")
    private long gracePeriodMs = 10000L; // 10 giây grace period chống mạng chập chờn

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String USER_SESSIONS_PREFIX = "auth:user_sessions:";
    private static final long TOMBSTONE_TTL_SECONDS = 120L; // 2 phút tombstone

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Lua script tạo Refresh Token mới và cập nhật danh bạ phiên ZSET nguyên tử (BUG-03 & SEC-02).
     * KEYS[1]: auth:refresh:<tokenHash>
     * KEYS[2]: auth:user_sessions:<userId>
     * ARGV[1]: json metadata token
     * ARGV[2]: ttlSeconds (604800)
     * ARGV[3]: tokenHash
     * ARGV[4]: expiresAt (score cho ZSET)
     * ARGV[5]: now (mốc dọn phiên hết hạn)
     */
    private static final String CREATE_LUA_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 1 then\n" +
            "    return 'KEY_EXISTS'\n" +
            "end\n" +
            "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])\n" +
            "redis.call('ZADD', KEYS[2], ARGV[4], ARGV[3])\n" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[5])\n" +
            "redis.call('EXPIRE', KEYS[2], ARGV[2])\n" +
            "return 'SUCCESS'";

    /**
     * Lua script thực thi Token Rotation nguyên tử trong Redis (BUG-01, SEC-02).
     * Trả về:
     * - 'SUCCESS': Xoay vòng thành công.
     * - 'ALREADY_ROTATED:<tombstoneJson>': Token đã bị xoay vòng từ trước kèm metadata mới nhất.
     * - 'NEW_TOKEN_COLLISION': Khóa token mới đã tồn tại.
     * - 'NOT_FOUND': Token không tồn tại hoặc đã hết hạn.
     */
    private static final String ROTATE_LUA_SCRIPT =
            "local oldData = redis.call('GET', KEYS[1])\n" +
            "if not oldData then\n" +
            "    return 'NOT_FOUND'\n" +
            "end\n" +
            "if string.find(oldData, '\"status\":\"ROTATED\"') or string.find(oldData, '\"status\": \"ROTATED\"') then\n" +
            "    return 'ALREADY_ROTATED:' .. oldData\n" +
            "end\n" +
            "if redis.call('EXISTS', KEYS[2]) == 1 then\n" +
            "    return 'NEW_TOKEN_COLLISION'\n" +
            "end\n" +
            "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])\n" +
            "redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4])\n" +
            "redis.call('ZREM', KEYS[3], ARGV[7])\n" +
            "redis.call('ZADD', KEYS[3], ARGV[6], ARGV[5])\n" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', ARGV[8])\n" +
            "redis.call('EXPIRE', KEYS[3], ARGV[4])\n" +
            "return 'SUCCESS'";

    /**
     * Lua script thu hồi toàn bộ phiên của người dùng nguyên tử (BUG-04).
     * KEYS[1]: auth:user_sessions:<userId>
     * ARGV[1]: prefix ("auth:refresh:")
     */
    private static final String REVOKE_ALL_LUA_SCRIPT =
            "local sessionHashes = redis.call('ZRANGE', KEYS[1], 0, -1)\n" +
            "local count = 0\n" +
            "if sessionHashes and #sessionHashes > 0 then\n" +
            "    for i, hash in ipairs(sessionHashes) do\n" +
            "        local tokenKey = ARGV[1] .. hash\n" +
            "        redis.call('DEL', tokenKey)\n" +
            "        count = count + 1\n" +
            "    end\n" +
            "end\n" +
            "redis.call('DEL', KEYS[1])\n" +
            "return count";

    /**
     * DTO lưu trữ metadata của Refresh Token trong Redis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RefreshTokenData {
        private Long userId;
        private String email;
        private String role;
        private String familyId;
        private String status; // "ACTIVE" hoặc "ROTATED"
        private Long createdAt;
        private Long expiresAt;
        private Long rotatedAt;
        private String replacedBy;
        private String passwordHash; // Hash mật khẩu tại thời điểm phát hành (hòa giải trạng thái với DB)
    }

    /**
     * Kết quả trả về sau khi tạo mới hoặc xoay vòng Refresh Token.
     */
    @Data
    @AllArgsConstructor
    public static class RotationResult {
        private String rawRefreshToken;
        private RefreshTokenData tokenData;
    }

    /**
     * Tạo Refresh Token mới khi đăng nhập hoặc đăng ký tài khoản (BUG-03, SEC-02).
     * Thao tác nguyên tử qua Lua script để tránh phiên mồ côi và kiểm tra va chạm khóa.
     *
     * @param user thông tin người dùng
     * @return chuỗi token thô (để gửi cookie HttpOnly về cho client)
     */
    public String createRefreshToken(User user) {
        String rawToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawToken);

        long now = System.currentTimeMillis();
        long expiresAt = now + refreshExpirationMs;
        long ttlSeconds = refreshExpirationMs / 1000;
        String familyId = UUID.randomUUID().toString();

        RefreshTokenData data = RefreshTokenData.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .familyId(familyId)
                .status("ACTIVE")
                .createdAt(now)
                .expiresAt(expiresAt)
                .passwordHash(user.getPassword())
                .build();

        String redisKey = REFRESH_KEY_PREFIX + tokenHash;
        String userSessionsKey = USER_SESSIONS_PREFIX + user.getId();

        try {
            String json = objectMapper.writeValueAsString(data);
            DefaultRedisScript<String> script = new DefaultRedisScript<>(CREATE_LUA_SCRIPT, String.class);
            List<String> keys = Arrays.asList(redisKey, userSessionsKey);

            String result = redisTemplate.execute(
                    script,
                    keys,
                    json,
                    String.valueOf(ttlSeconds),
                    tokenHash,
                    String.valueOf(expiresAt),
                    String.valueOf(now)
            );

            if ("KEY_EXISTS".equals(result)) {
                log.error("Xung đột khóa Refresh Token khi tạo mới: hash={}", tokenHash);
                throw new BusinessException("Lỗi hệ thống khi khởi tạo phiên đăng nhập, vui lòng thử lại");
            }

            if (!"SUCCESS".equals(result)) {
                log.error("Lỗi thực thi Lua script tạo Refresh Token: result={}", result);
                throw new BusinessException("Lỗi hệ thống khi khởi tạo phiên đăng nhập");
            }

            log.info("Đã tạo Refresh Token mới nguyên tử cho user ID={}, familyId={}", user.getId(), familyId);
            return rawToken;
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize RefreshTokenData: {}", e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi khởi tạo phiên đăng nhập");
        }
    }

    /**
     * Đọc và kiểm tra metadata của Refresh Token từ Redis (BUG-02).
     * Cho phép xác minh người dùng và mật khẩu trong DB trước khi thực hiện rotation.
     *
     * @param rawRefreshToken token thô nhận từ cookie
     * @return RefreshTokenData metadata của phiên
     */
    public RefreshTokenData getTokenData(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc không tồn tại");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        String redisKey = REFRESH_KEY_PREFIX + tokenHash;

        String existingJson = redisTemplate.opsForValue().get(redisKey);
        if (existingJson == null) {
            log.warn("Không tìm thấy Refresh Token trong Redis (đã hết hạn hoặc không hợp lệ): hash={}", tokenHash);
            throw new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
        }

        RefreshTokenData existingData;
        try {
            existingData = objectMapper.readValue(existingJson, RefreshTokenData.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse RefreshTokenData từ Redis: {}", e.getMessage());
            throw new BadCredentialsException("Dữ liệu phiên không hợp lệ");
        }

        // Nếu token đã ở trạng thái ROTATED từ trước khi kiểm tra MySQL
        if ("ROTATED".equals(existingData.getStatus())) {
            long now = System.currentTimeMillis();
            handleTokenReuse(existingData, now);
        }

        return existingData;
    }

    /**
     * Xoay vòng Refresh Token nguyên tử (Atomic Token Rotation).
     *
     * @param oldRawToken token cũ nhận từ cookie
     * @return RotationResult chứa token thô mới và metadata
     */
    public RotationResult rotateRefreshToken(String oldRawToken) {
        if (oldRawToken == null || oldRawToken.trim().isEmpty()) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc không tồn tại");
        }

        String oldHash = hashToken(oldRawToken.trim());
        String oldRedisKey = REFRESH_KEY_PREFIX + oldHash;

        // 1. Đọc dữ liệu token hiện tại để lấy thông tin userId, familyId
        String existingJson = redisTemplate.opsForValue().get(oldRedisKey);
        if (existingJson == null) {
            log.warn("Không tìm thấy Refresh Token trong Redis (đã hết hạn hoặc không hợp lệ): hash={}", oldHash);
            throw new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
        }

        RefreshTokenData existingData;
        try {
            existingData = objectMapper.readValue(existingJson, RefreshTokenData.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse RefreshTokenData từ Redis: {}", e.getMessage());
            throw new BadCredentialsException("Dữ liệu phiên không hợp lệ");
        }

        long now = System.currentTimeMillis();

        if ("ROTATED".equals(existingData.getStatus())) {
            handleTokenReuse(existingData, now);
        }

        long newExpiresAt = now + refreshExpirationMs;
        long newTtlSeconds = refreshExpirationMs / 1000;
        Long userId = existingData.getUserId();
        String userSessionsKey = USER_SESSIONS_PREFIX + userId;

        // 2. Chuẩn bị token mới
        String newRawToken = generateSecureRandomToken();
        String newHash = hashToken(newRawToken);
        String newRedisKey = REFRESH_KEY_PREFIX + newHash;

        RefreshTokenData newData = RefreshTokenData.builder()
                .userId(userId)
                .email(existingData.getEmail())
                .role(existingData.getRole())
                .familyId(existingData.getFamilyId())
                .status("ACTIVE")
                .createdAt(now)
                .expiresAt(newExpiresAt)
                .passwordHash(existingData.getPasswordHash())
                .build();

        // 3. Chuẩn bị dữ liệu Tombstone cho token cũ
        RefreshTokenData tombstoneData = RefreshTokenData.builder()
                .userId(userId)
                .email(existingData.getEmail())
                .role(existingData.getRole())
                .familyId(existingData.getFamilyId())
                .status("ROTATED")
                .createdAt(existingData.getCreatedAt())
                .expiresAt(existingData.getExpiresAt())
                .rotatedAt(now)
                .replacedBy(newHash)
                .passwordHash(existingData.getPasswordHash())
                .build();

        String tombstoneJson;
        String newJson;
        try {
            tombstoneJson = objectMapper.writeValueAsString(tombstoneData);
            newJson = objectMapper.writeValueAsString(newData);
        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize JSON khi xoay token: {}", e.getMessage());
            throw new BusinessException("Lỗi hệ thống khi xử lý phiên");
        }

        // 4. Thực thi Lua Script bảo đảm xoay token nguyên tử trên Redis
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>(ROTATE_LUA_SCRIPT, String.class);
        List<String> keys = Arrays.asList(oldRedisKey, newRedisKey, userSessionsKey);

        String result = redisTemplate.execute(
                redisScript,
                keys,
                tombstoneJson,
                String.valueOf(TOMBSTONE_TTL_SECONDS),
                newJson,
                String.valueOf(newTtlSeconds),
                newHash,
                String.valueOf(newExpiresAt),
                oldHash,
                String.valueOf(now)
        );

        if ("SUCCESS".equals(result)) {
            log.info("Xoay vòng Refresh Token thành công: userId={}, familyId={}", userId, existingData.getFamilyId());
            return new RotationResult(newRawToken, newData);
        }

        if (result != null && result.startsWith("ALREADY_ROTATED")) {
            // BUG-01 Fix: Không dùng existingData cũ có rotatedAt=null mà trích xuất tombstone mới nhất từ Lua script hoặc Redis
            RefreshTokenData currentTombstone = null;
            if (result.length() > "ALREADY_ROTATED:".length()) {
                try {
                    String extractedJson = result.substring("ALREADY_ROTATED:".length());
                    currentTombstone = objectMapper.readValue(extractedJson, RefreshTokenData.class);
                } catch (JsonProcessingException e) {
                    log.warn("Lỗi parse tombstone JSON từ kết quả Lua script: {}", e.getMessage());
                }
            }
            if (currentTombstone == null) {
                String latestJson = redisTemplate.opsForValue().get(oldRedisKey);
                if (latestJson != null) {
                    try {
                        currentTombstone = objectMapper.readValue(latestJson, RefreshTokenData.class);
                    } catch (JsonProcessingException ignored) {}
                }
            }
            if (currentTombstone == null) {
                currentTombstone = existingData;
            }
            handleTokenReuse(currentTombstone, now);
        }

        if ("NEW_TOKEN_COLLISION".equals(result)) {
            log.error("Xung đột khóa Refresh Token mới trong Redis: newHash={}", newHash);
            throw new BusinessException("Lỗi xung đột phiên đăng nhập, vui lòng thử lại");
        }

        log.warn("Thất bại khi xoay Refresh Token: result={}", result);
        throw new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    /**
     * Xử lý trường hợp token đã bị xoay vòng (Reuse Detection).
     */
    private void handleTokenReuse(RefreshTokenData data, long now) {
        Long rotatedAt = data.getRotatedAt();
        // Nếu trong grace period (10 giây) do chập chờn mạng hoặc nhiều tab refresh song song
        if (rotatedAt != null && (now - rotatedAt) >= 0 && (now - rotatedAt) < gracePeriodMs) {
            log.warn("Phát hiện request refresh đồng thời trong grace period ({}ms): userId={}", now - rotatedAt, data.getUserId());
            throw new BadCredentialsException("Phiên đăng nhập đang được cập nhật từ thiết bị khác, vui lòng thử lại");
        }

        // BUG-01 Fix: Nếu rotatedAt null do dữ liệu không xác định được thời gian xoay vòng, từ chối an toàn, không thu hồi nhầm
        if (rotatedAt == null) {
            log.warn("Phát hiện token ROTATED nhưng rotatedAt null: userId={}", data.getUserId());
            throw new BadCredentialsException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
        }

        // VƯỢT QUÁ GRACE PERIOD VÀ CÒN TRONG TOMBSTONE (120s) -> PHÁT HIỆN TẤN CÔNG TÁI SỬ DỤNG TOKEN!
        log.error("CẢNH BÁO AN NINH: Phát hiện Refresh Token bị tái sử dụng! Thu hồi toàn bộ phiên của userId={}, familyId={}",
                data.getUserId(), data.getFamilyId());

        // Lập tức thu hồi toàn bộ phiên của tài khoản bị xâm phạm
        revokeAllUserSessions(data.getUserId());

        throw new BadCredentialsException("Phát hiện phiên đăng nhập bị tái sử dụng bất thường. Toàn bộ phiên của bạn đã bị thu hồi vì lý do an toàn. Vui lòng đăng nhập lại.");
    }

    /**
     * Thu hồi một Refresh Token cụ thể (dành cho chức năng Logout).
     *
     * @param rawToken token thô nhận từ cookie
     */
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return;
        }

        try {
            String tokenHash = hashToken(rawToken.trim());
            String redisKey = REFRESH_KEY_PREFIX + tokenHash;

            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                RefreshTokenData data = objectMapper.readValue(json, RefreshTokenData.class);
                if (data.getUserId() != null) {
                    String userSessionsKey = USER_SESSIONS_PREFIX + data.getUserId();
                    redisTemplate.opsForZSet().remove(userSessionsKey, tokenHash);
                }
            }
            redisTemplate.delete(redisKey);
            log.info("Đã thu hồi Refresh Token (logout): hash={}", tokenHash);
        } catch (Exception ex) {
            log.warn("Không thể xóa Refresh Token trong Redis khi logout (tiếp tục xóa cookie): {}", ex.getMessage());
        }
    }

    /**
     * Thu hồi toàn bộ Refresh Token của một người dùng nguyên tử (BUG-04).
     * Sử dụng Lua script để đọc ZSET và xóa đồng thời token keys và sessions key trong một thao tác Redis nguyên tử.
     *
     * @param userId ID người dùng
     */
    public void revokeAllUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String userSessionsKey = USER_SESSIONS_PREFIX + userId;
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(REVOKE_ALL_LUA_SCRIPT, Long.class);
            Long deletedCount = redisTemplate.execute(script, Collections.singletonList(userSessionsKey), REFRESH_KEY_PREFIX);
            log.info("Đã thu hồi nguyên tử {} Refresh Token và xóa phiên ZSET cho user ID={}", deletedCount, userId);
        } catch (Exception ex) {
            log.error("Lỗi khi thu hồi toàn bộ phiên Redis cho user ID={}: {}", userId, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Sinh chuỗi Opaque Token ngẫu nhiên mật mã an toàn (64 bytes, Base64 URL-safe không padding).
     */
    private String generateSecureRandomToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Băm SHA-256 chuỗi token thô thành mã hex (lowercase).
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng trên JVM", e);
        }
    }
}
