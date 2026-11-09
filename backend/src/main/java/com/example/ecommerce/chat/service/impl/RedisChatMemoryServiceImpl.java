package com.example.ecommerce.chat.service.impl;

import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.ChatMemoryService;
import com.example.ecommerce.exception.ChatLockContentionException;
import com.example.ecommerce.exception.ChatVersionConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hiện thực hóa tầng quản lý bộ nhớ hội thoại Redis và phiên chat.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc kỹ thuật & an toàn:
 * 1. Mọi Redis key liên quan đến session đều dùng chung Hash Tag {sessionId} đảm bảo tương thích Cluster.
 * 2. ownerId được so sánh thuần chuỗi (String), không dùng tonumber() trong Lua.
 * 3. Phiên bản version (OCC) kiểm tra tràn số Lua safe integer (2^53 - 1).
 * 4. Kiểm tra toàn bộ điều kiện hợp lệ ở chế độ READ-ONLY trước khi ghi bất kỳ dữ liệu nào vào Redis.
 * 5. Cắt tỉa (trim) bảo toàn cặp tin nhắn chẵn (tối đa 20 messages = 10 cặp).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisChatMemoryServiceImpl implements ChatMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public static final String OWNER_GUEST = "GUEST";
    public static final long GUEST_TTL_SECONDS = 86400L;    // 24 giờ
    public static final long USER_TTL_SECONDS = 604800L;    // 7 ngày
    public static final long LOCK_TTL_MILLIS = 45000L;      // 45 giây

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =========================================================================
    // LUA SCRIPTS NGUYÊN TỬ
    // =========================================================================

    /**
     * Script 1: Khởi tạo Guest Session nguyên tử
     * KEYS[1]: chat:session:{sessionId}
     * KEYS[2]: chat:guest-token:{sessionId}
     * ARGV[1]: metadataJson
     * ARGV[2]: tokenHash
     * ARGV[3]: ttlSeconds
     */
    private static final DefaultRedisScript<Long> CREATE_GUEST_SESSION_SCRIPT;

    /**
     * Script 2: Đọc Chat Memory & Metadata nguyên tử
     * KEYS[1]: chat:session:{sessionId}
     * KEYS[2]: chat:memory:{sessionId}
     * KEYS[3]: chat:guest-token:{sessionId}
     * ARGV[1]: expectedOwnerId ("GUEST" hoặc String userId)
     * ARGV[2]: guestTokenHash (nếu là GUEST, rỗng nếu User)
     */
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> READ_CHAT_MEMORY_SCRIPT;

    /**
     * Script 3: Ghi cặp tin nhắn, LTRIM -20 -1 và tăng OCC Version nguyên tử
     * KEYS[1]: chat:session:{sessionId}
     * KEYS[2]: chat:memory:{sessionId}
     * KEYS[3]: chat:lock:{sessionId}
     * KEYS[4]: chat:guest-token:{sessionId}
     * ARGV[1]: lockToken
     * ARGV[2]: expectedVersion (String)
     * ARGV[3]: expectedOwnerId ("GUEST" hoặc String userId)
     * ARGV[4]: guestTokenHash (nếu GUEST, rỗng nếu User)
     * ARGV[5]: userMsgJson
     * ARGV[6]: assistantMsgJson
     * ARGV[7]: nowIsoString
     * ARGV[8]: expiresAtIsoString
     * ARGV[9]: ttlSeconds
     */
    private static final DefaultRedisScript<Long> ATOMIC_WRITE_CHAT_MEMORY_SCRIPT;

    /**
     * Script 4: Claim Guest Session chuyển sang User nguyên tử
     * KEYS[1]: chat:session:{sessionId}
     * KEYS[2]: chat:memory:{sessionId}
     * KEYS[3]: chat:lock:{sessionId}
     * KEYS[4]: chat:guest-token:{sessionId}
     * ARGV[1]: lockToken
     * ARGV[2]: guestTokenHash
     * ARGV[3]: newOwnerId (String userId)
     * ARGV[4]: nowIsoString
     * ARGV[5]: expiresAtIsoString
     * ARGV[6]: newTtlSeconds
     */
    private static final DefaultRedisScript<Long> CLAIM_GUEST_SESSION_SCRIPT;

    /**
     * Script 5: Release Lock an toàn bằng Compare-and-Delete
     * KEYS[1]: chat:lock:{sessionId}
     * ARGV[1]: lockToken
     */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;

    static {
        // 1. CREATE_GUEST_SESSION
        CREATE_GUEST_SESSION_SCRIPT = new DefaultRedisScript<>();
        CREATE_GUEST_SESSION_SCRIPT.setResultType(Long.class);
        CREATE_GUEST_SESSION_SCRIPT.setScriptText("""
            local sExists = redis.call('EXISTS', KEYS[1])
            local tExists = redis.call('EXISTS', KEYS[2])
            if sExists == 1 or tExists == 1 then
                return -999
            end
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
            return 1
            """);

        // 2. READ_CHAT_MEMORY
        READ_CHAT_MEMORY_SCRIPT = new DefaultRedisScript<>();
        READ_CHAT_MEMORY_SCRIPT.setResultType(List.class);
        READ_CHAT_MEMORY_SCRIPT.setScriptText("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return {-1, ''}
            end
            local sType = redis.call('TYPE', KEYS[1]).ok
            if sType ~= 'string' then
                return {-2, ''}
            end
            local mType = redis.call('TYPE', KEYS[2]).ok
            if mType ~= 'list' and mType ~= 'none' then
                return {-2, ''}
            end
            local metaStr = redis.call('GET', KEYS[1])
            if not metaStr then
                return {-1, ''}
            end
            local ok, meta = pcall(cjson.decode, metaStr)
            if not ok or type(meta) ~= 'table' or not meta.ownerId then
                return {-6, ''}
            end
            local owner = tostring(meta.ownerId)
            local expectedOwner = tostring(ARGV[1])
            if owner == 'GUEST' then
                if expectedOwner ~= 'GUEST' then
                    return {-4, ''}
                end
                local storedHash = redis.call('GET', KEYS[3])
                if not storedHash or storedHash ~= ARGV[2] then
                    return {-11, ''}
                end
            else
                if owner ~= expectedOwner then
                    return {-4, ''}
                end
            end
            local messages = redis.call('LRANGE', KEYS[2], 0, -1)
            return {1, metaStr, messages}
            """);

        // 3. ATOMIC_WRITE_CHAT_MEMORY
        ATOMIC_WRITE_CHAT_MEMORY_SCRIPT = new DefaultRedisScript<>();
        ATOMIC_WRITE_CHAT_MEMORY_SCRIPT.setResultType(Long.class);
        ATOMIC_WRITE_CHAT_MEMORY_SCRIPT.setScriptText("""
            local lockType = redis.call('TYPE', KEYS[3]).ok
            if lockType ~= 'string' and lockType ~= 'none' then
                return -2
            end
            local currentLock = redis.call('GET', KEYS[3])
            if not currentLock then
                return -101
            end
            if currentLock ~= ARGV[1] then
                return -102
            end
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return -1
            end
            local sType = redis.call('TYPE', KEYS[1]).ok
            if sType ~= 'string' then
                return -2
            end
            local mType = redis.call('TYPE', KEYS[2]).ok
            if mType ~= 'list' and mType ~= 'none' then
                return -2
            end
            local metaStr = redis.call('GET', KEYS[1])
            local ok, meta = pcall(cjson.decode, metaStr)
            if not ok or type(meta) ~= 'table' or not meta.ownerId or meta.version == nil then
                return -6
            end
            local owner = tostring(meta.ownerId)
            local expectedOwner = tostring(ARGV[3])
            if owner ~= expectedOwner then
                return -4
            end
            if owner == 'GUEST' then
                local tType = redis.call('TYPE', KEYS[4]).ok
                if tType ~= 'string' then
                    return -2
                end
                local storedHash = redis.call('GET', KEYS[4])
                if not storedHash or storedHash ~= ARGV[4] then
                    return -11
                end
            end
            local currentVersion = tonumber(meta.version)
            local expVersion = tonumber(ARGV[2])
            if not currentVersion or not expVersion or currentVersion ~= expVersion then
                return -3
            end
            if currentVersion >= 9007199254740991 then
                return -7
            end
            local curLen = redis.call('LLEN', KEYS[2])
            if curLen % 2 ~= 0 then
                return -5
            end
            redis.call('RPUSH', KEYS[2], ARGV[5])
            redis.call('RPUSH', KEYS[2], ARGV[6])
            local newLen = curLen + 2
            if newLen > 20 then
                redis.call('LTRIM', KEYS[2], -20, -1)
            end
            local newVersion = currentVersion + 1
            meta.version = newVersion
            meta.updatedAt = ARGV[7]
            meta.expiresAt = ARGV[8]
            local newMetaStr = cjson.encode(meta)
            local ttl = tonumber(ARGV[9])
            redis.call('SET', KEYS[1], newMetaStr, 'EX', ttl)
            redis.call('EXPIRE', KEYS[2], ttl)
            if owner == 'GUEST' then
                redis.call('EXPIRE', KEYS[4], ttl)
            end
            return newVersion
            """);

        // 4. CLAIM_GUEST_SESSION
        CLAIM_GUEST_SESSION_SCRIPT = new DefaultRedisScript<>();
        CLAIM_GUEST_SESSION_SCRIPT.setResultType(Long.class);
        CLAIM_GUEST_SESSION_SCRIPT.setScriptText("""
            local currentLock = redis.call('GET', KEYS[3])
            if not currentLock then
                return -101
            end
            if currentLock ~= ARGV[1] then
                return -102
            end
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return -1
            end
            local sType = redis.call('TYPE', KEYS[1]).ok
            if sType ~= 'string' then
                return -2
            end
            local mType = redis.call('TYPE', KEYS[2]).ok
            if mType ~= 'list' and mType ~= 'none' then
                return -2
            end
            local metaStr = redis.call('GET', KEYS[1])
            local ok, meta = pcall(cjson.decode, metaStr)
            if not ok or type(meta) ~= 'table' or not meta.ownerId or meta.version == nil then
                return -6
            end
            local owner = tostring(meta.ownerId)
            if owner ~= 'GUEST' then
                return -10
            end
            local tType = redis.call('TYPE', KEYS[4]).ok
            if tType ~= 'string' then
                return -2
            end
            local storedHash = redis.call('GET', KEYS[4])
            if not storedHash or storedHash ~= ARGV[2] then
                return -11
            end
            local currentVersion = tonumber(meta.version)
            if not currentVersion or currentVersion >= 9007199254740991 then
                return -7
            end
            redis.call('DEL', KEYS[4])
            local newVersion = currentVersion + 1
            meta.ownerId = tostring(ARGV[3])
            meta.version = newVersion
            meta.updatedAt = ARGV[4]
            meta.expiresAt = ARGV[5]
            local newMetaStr = cjson.encode(meta)
            local ttl = tonumber(ARGV[6])
            redis.call('SET', KEYS[1], newMetaStr, 'EX', ttl)
            if redis.call('EXISTS', KEYS[2]) == 1 then
                redis.call('EXPIRE', KEYS[2], ttl)
            end
            return newVersion
            """);

        // 5. RELEASE_LOCK
        RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
        RELEASE_LOCK_SCRIPT.setScriptText("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
            """);
    }

    // =========================================================================
    // SERVICE IMPLEMENTATION
    // =========================================================================

    @Override
    public GuestSessionCreationResult createGuestSession() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            String sessionId = UUID.randomUUID().toString();
            byte[] tokenBytes = new byte[32];
            SECURE_RANDOM.nextBytes(tokenBytes);
            String rawGuestToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            String tokenHash = sha256Hex(rawGuestToken);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusSeconds(GUEST_TTL_SECONDS);

            ChatSessionMetadata metadata = ChatSessionMetadata.builder()
                    .sessionId(sessionId)
                    .ownerId(OWNER_GUEST)
                    .version(1L)
                    .createdAt(now)
                    .updatedAt(now)
                    .expiresAt(expiresAt)
                    .build();

            String metadataJson;
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize ChatSessionMetadata to JSON: {}", e.getMessage());
                throw new IllegalStateException("Lỗi tuần tự hóa metadata phiên trò chuyện", e);
            }

            List<String> keys = List.of(buildSessionKey(sessionId), buildGuestTokenKey(sessionId));
            Long result = redisTemplate.execute(
                    CREATE_GUEST_SESSION_SCRIPT,
                    keys,
                    metadataJson,
                    tokenHash,
                    String.valueOf(GUEST_TTL_SECONDS)
            );

            if (result != null && result == 1L) {
                log.info("Created new Guest Chat Session successfully with sessionId={}", sessionId);
                return GuestSessionCreationResult.builder()
                        .sessionId(sessionId)
                        .rawGuestToken(rawGuestToken)
                        .metadata(metadata)
                        .build();
            }

            log.warn("Session ID collision detected during guest session creation (attempt {}/{}), retrying...", i + 1, maxRetries);
        }

        throw new IllegalStateException("Không thể khởi tạo phiên trò chuyện do va chạm khóa liên tục");
    }

    @Override
    public String acquireLock(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID không được để trống khi acquire lock");
        }

        String lockKey = buildLockKey(sessionId);
        String lockToken = UUID.randomUUID().toString();

        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockToken, LOCK_TTL_MILLIS, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                return lockToken;
            }

            log.warn("Lock contention detected for sessionId={}", sessionId);
            throw new ChatLockContentionException("Hệ thống đang xử lý yêu cầu trước trên phiên này, vui lòng thử lại sau giây lát");
        } catch (ChatLockContentionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis error while acquiring lock for sessionId={}: {}", sessionId, e.getMessage());
            throw new RedisSystemException("Lỗi kết nối Redis khi acquire lock", e);
        }
    }

    @Override
    public void releaseLock(String sessionId, String lockToken) {
        if (sessionId == null || sessionId.isBlank() || lockToken == null || lockToken.isBlank()) {
            return;
        }

        String lockKey = buildLockKey(sessionId);
        try {
            redisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockToken
            );
        } catch (Exception e) {
            log.warn("Failed to release lock for sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public ChatMemoryData getChatMemory(String sessionId, String guestToken, String authenticatedUserId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID không được để trống khi đọc memory");
        }

        String expectedOwnerId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId.trim()
                : OWNER_GUEST;

        String guestTokenHash = "";
        if (OWNER_GUEST.equals(expectedOwnerId)) {
            if (guestToken == null || guestToken.isBlank()) {
                throw new BadCredentialsException("Guest token bắt buộc đối với phiên khách vãng lai");
            }
            guestTokenHash = sha256Hex(guestToken.trim());
        }

        List<String> keys = List.of(
                buildSessionKey(sessionId),
                buildMemoryKey(sessionId),
                buildGuestTokenKey(sessionId)
        );

        @SuppressWarnings("rawtypes")
        List result = redisTemplate.execute(
                READ_CHAT_MEMORY_SCRIPT,
                keys,
                expectedOwnerId,
                guestTokenHash
        );

        if (result == null || result.isEmpty()) {
            throw new ResourceNotFoundException("Phiên trò chuyện không tồn tại hoặc đã hết hạn");
        }

        long status = ((Number) result.get(0)).longValue();
        mapReadStatusToException(status);

        String metaStr = (String) result.get(1);
        ChatSessionMetadata metadata;
        try {
            metadata = objectMapper.readValue(metaStr, ChatSessionMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ChatSessionMetadata: {}", e.getMessage());
            throw new IllegalStateException("Metadata phiên trò chuyện trong Redis bị hỏng hoặc sai định dạng JSON", e);
        }

        @SuppressWarnings("unchecked")
        List<Object> rawMessages = (List<Object>) result.get(2);
        List<ChatMessage> messages = new ArrayList<>();
        if (rawMessages != null) {
            for (Object raw : rawMessages) {
                if (raw instanceof String msgJson) {
                    try {
                        messages.add(objectMapper.readValue(msgJson, ChatMessage.class));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize ChatMessage: {}", e.getMessage());
                        throw new IllegalStateException("Dữ liệu tin nhắn trong Redis bị hỏng", e);
                    }
                }
            }
        }

        return ChatMemoryData.builder()
                .metadata(metadata)
                .messages(messages)
                .build();
    }

    @Override
    public ChatSessionMetadata saveChatMemory(
            String sessionId,
            String lockToken,
            Long expectedVersion,
            ChatMessage userMessage,
            ChatMessage assistantMessage,
            String guestToken,
            String authenticatedUserId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID không được để trống");
        }
        if (lockToken == null || lockToken.isBlank()) {
            throw new IllegalArgumentException("Lock token không được để trống");
        }
        if (expectedVersion == null || expectedVersion < 1L) {
            throw new IllegalArgumentException("Expected version không hợp lệ");
        }

        // Validate incoming messages
        validateMessages(userMessage, assistantMessage);

        String expectedOwnerId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId.trim()
                : OWNER_GUEST;

        String guestTokenHash = "";
        long ttlSeconds = USER_TTL_SECONDS;

        if (OWNER_GUEST.equals(expectedOwnerId)) {
            if (guestToken == null || guestToken.isBlank()) {
                throw new BadCredentialsException("Guest token bắt buộc đối với phiên khách vãng lai");
            }
            guestTokenHash = sha256Hex(guestToken.trim());
            ttlSeconds = GUEST_TTL_SECONDS;
        }

        String userMsgJson;
        String assistantMsgJson;
        try {
            userMsgJson = objectMapper.writeValueAsString(userMessage);
            assistantMsgJson = objectMapper.writeValueAsString(assistantMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChatMessage: {}", e.getMessage());
            throw new IllegalStateException("Lỗi tuần tự hóa tin nhắn trò chuyện", e);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);

        List<String> keys = List.of(
                buildSessionKey(sessionId),
                buildMemoryKey(sessionId),
                buildLockKey(sessionId),
                buildGuestTokenKey(sessionId)
        );

        Long result = redisTemplate.execute(
                ATOMIC_WRITE_CHAT_MEMORY_SCRIPT,
                keys,
                lockToken,
                String.valueOf(expectedVersion),
                expectedOwnerId,
                guestTokenHash,
                userMsgJson,
                assistantMsgJson,
                now.toString(),
                expiresAt.toString(),
                String.valueOf(ttlSeconds)
        );

        if (result == null) {
            throw new IllegalStateException("Không nhận được phản hồi từ Redis khi ghi memory");
        }

        mapWriteStatusToException(result);

        // Success: result is newVersion
        return ChatSessionMetadata.builder()
                .sessionId(sessionId)
                .ownerId(expectedOwnerId)
                .version(result)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public ChatSessionMetadata claimGuestSession(String sessionId, String guestToken, String authenticatedUserId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID không được để trống khi claim");
        }
        if (guestToken == null || guestToken.isBlank()) {
            throw new BadCredentialsException("Guest token không được để trống khi claim");
        }
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new AccessDeniedException("Yêu cầu thông tin người dùng đã xác thực để claim phiên");
        }

        String lockToken = acquireLock(sessionId);
        try {
            String guestTokenHash = sha256Hex(guestToken.trim());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusSeconds(USER_TTL_SECONDS);

            List<String> keys = List.of(
                    buildSessionKey(sessionId),
                    buildMemoryKey(sessionId),
                    buildLockKey(sessionId),
                    buildGuestTokenKey(sessionId)
            );

            Long result = redisTemplate.execute(
                    CLAIM_GUEST_SESSION_SCRIPT,
                    keys,
                    lockToken,
                    guestTokenHash,
                    authenticatedUserId.trim(),
                    now.toString(),
                    expiresAt.toString(),
                    String.valueOf(USER_TTL_SECONDS)
            );

            if (result == null) {
                throw new IllegalStateException("Không nhận được phản hồi từ Redis khi claim session");
            }

            mapClaimStatusToException(result);

            log.info("Claimed Guest session successfully: sessionId={}, newOwnerId={}, newVersion={}",
                    sessionId, authenticatedUserId, result);

            return ChatSessionMetadata.builder()
                    .sessionId(sessionId)
                    .ownerId(authenticatedUserId.trim())
                    .version(result)
                    .updatedAt(now)
                    .expiresAt(expiresAt)
                    .build();
        } finally {
            releaseLock(sessionId, lockToken);
        }
    }

    // =========================================================================
    // HELPER & VALIDATION METHODS
    // =========================================================================

    private void validateMessages(ChatMessage userMsg, ChatMessage assistantMsg) {
        if (userMsg == null || !"user".equalsIgnoreCase(userMsg.getRole())) {
            throw new IllegalArgumentException("Tin nhắn người dùng không hợp lệ hoặc sai role (yêu cầu role='user')");
        }
        if (userMsg.getContent() == null || userMsg.getContent().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn người dùng không được để trống");
        }
        if (userMsg.getContent().length() > 1000) {
            throw new IllegalArgumentException("Nội dung tin nhắn người dùng vượt quá 1000 ký tự");
        }

        if (assistantMsg == null || !"assistant".equalsIgnoreCase(assistantMsg.getRole())) {
            throw new IllegalArgumentException("Tin nhắn trợ lý không hợp lệ hoặc sai role (yêu cầu role='assistant')");
        }
        if (assistantMsg.getContent() == null || assistantMsg.getContent().isBlank()) {
            throw new IllegalArgumentException("Nội dung tin nhắn trợ lý ảo không được để trống");
        }
    }

    private void mapReadStatusToException(long status) {
        if (status == 1L) {
            return;
        }
        if (status == -1L) {
            throw new ResourceNotFoundException("Phiên trò chuyện không tồn tại hoặc đã hết hạn");
        }
        if (status == -2L) {
            throw new IllegalStateException("Cấu trúc dữ liệu Redis không hợp lệ hoặc sai kiểu");
        }
        if (status == -4L) {
            throw new AccessDeniedException("Bạn không có quyền truy cập phiên trò chuyện này");
        }
        if (status == -6L) {
            throw new IllegalStateException("Metadata phiên trò chuyện trong Redis bị hỏng hoặc sai định dạng JSON");
        }
        if (status == -11L) {
            throw new BadCredentialsException("Guest token không hợp lệ hoặc không khớp với phiên trò chuyện");
        }
        throw new IllegalStateException("Mã lỗi không xác định từ Redis Lua Script: " + status);
    }

    private void mapWriteStatusToException(long status) {
        if (status > 0) {
            return;
        }
        if (status == -101L) {
            throw new ChatLockContentionException("Distributed Lock đã hết hạn trước khi hoàn tất ghi dữ liệu");
        }
        if (status == -102L) {
            throw new ChatLockContentionException("Distributed Lock đã bị chiếm bởi một tiến trình khác");
        }
        if (status == -1L) {
            throw new ResourceNotFoundException("Phiên trò chuyện không tồn tại hoặc đã hết hạn");
        }
        if (status == -2L) {
            throw new IllegalStateException("Cấu trúc dữ liệu Redis không hợp lệ hoặc sai kiểu");
        }
        if (status == -3L) {
            throw new ChatVersionConflictException("Xung đột phiên bản hội thoại (OCC conflict), dữ liệu đã được cập nhật bởi một yêu cầu khác");
        }
        if (status == -4L) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật phiên trò chuyện này");
        }
        if (status == -5L) {
            throw new IllegalStateException("Lịch sử hội thoại trong Redis bị lỗi cấu trúc lẻ tin nhắn");
        }
        if (status == -6L) {
            throw new IllegalStateException("Metadata phiên trò chuyện trong Redis bị hỏng hoặc sai định dạng JSON");
        }
        if (status == -7L) {
            throw new IllegalStateException("Phiên bản hội thoại đã đạt giới hạn an toàn tối đa");
        }
        if (status == -11L) {
            throw new BadCredentialsException("Guest token không hợp lệ hoặc không khớp với phiên trò chuyện");
        }
        throw new IllegalStateException("Mã lỗi không xác định từ Redis Lua Script: " + status);
    }

    private void mapClaimStatusToException(long status) {
        if (status > 0) {
            return;
        }
        if (status == -101L || status == -102L) {
            throw new ChatLockContentionException("Distributed Lock không hợp lệ hoặc bị xung đột khi claim phiên");
        }
        if (status == -1L) {
            throw new ResourceNotFoundException("Phiên trò chuyện không tồn tại hoặc đã hết hạn");
        }
        if (status == -2L || status == -6L) {
            throw new IllegalStateException("Cấu trúc dữ liệu Redis không hợp lệ hoặc bị hỏng khi claim phiên");
        }
        if (status == -7L) {
            throw new IllegalStateException("Phiên bản hội thoại đã đạt giới hạn an toàn tối đa");
        }
        if (status == -10L) {
            throw new AccessDeniedException("Phiên trò chuyện đã được chuyển giao cho người dùng khác hoặc không phải phiên khách");
        }
        if (status == -11L) {
            throw new BadCredentialsException("Guest token không hợp lệ hoặc không khớp với phiên trò chuyện");
        }
        throw new IllegalStateException("Mã lỗi không xác định từ Redis Lua Script: " + status);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
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

    public static String buildSessionKey(String sessionId) {
        return "chat:session:{" + sessionId + "}";
    }

    public static String buildMemoryKey(String sessionId) {
        return "chat:memory:{" + sessionId + "}";
    }

    public static String buildLockKey(String sessionId) {
        return "chat:lock:{" + sessionId + "}";
    }

    public static String buildGuestTokenKey(String sessionId) {
        return "chat:guest-token:{" + sessionId + "}";
    }
}

