package com.example.ecommerce.chat.controller;

import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.chat.service.ChatService;
import com.example.ecommerce.chat.service.RateLimiterService;
import com.example.ecommerce.dto.request.ChatRequest;
import com.example.ecommerce.dto.request.ClaimSessionRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.ChatResponse;
import com.example.ecommerce.dto.response.GuestSessionResponse;
import com.example.ecommerce.exception.ChatAuthenticationException;
import com.example.ecommerce.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp REST API cho Chatbot tư vấn bán hàng Rikkei Ecommerce.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Các API cung cấp:
 * 1. POST /api/v1/chat/sessions  - Khởi tạo phiên trò chuyện cho khách vãng lai (Guest).
 * 2. POST /api/v1/chat/messages  - Gửi tin nhắn và nhận câu trả lời tư vấn từ Gemini AI.
 * 3. POST /api/v1/chat/claim     - Nhận quyền sở hữu phiên trò chuyện khi người dùng đăng nhập.
 *
 * Nguyên tắc bảo mật:
 * - Không tin tưởng dữ liệu userId gửi từ client; chỉ trích xuất từ Spring SecurityContext.
 * - Kiểm soát chặt chẽ Authorization header: nếu có token nhưng sai/hết hạn thì từ chối ngay bằng 401,
 *   tuyệt đối không chuyển tiếp xử lý dưới danh nghĩa khách vãng lai.
 * - Áp dụng Rate Limiting phân tán qua Redis cho toàn bộ các endpoints.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final RateLimiterService rateLimiterService;

    /**
     * Khởi tạo phiên chat mới cho khách vãng lai (Guest).
     * POST /api/v1/chat/sessions
     *
     * @param request HTTP request để trích xuất remote IP phục vụ rate limit
     * @return 201 Created cùng sessionId và rawGuestToken (chỉ trả về một lần duy nhất)
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<GuestSessionResponse>> createGuestSession(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        rateLimiterService.checkRateLimit(clientIp, null);

        GuestSessionCreationResult result = chatService.createGuestSession();

        GuestSessionResponse response = GuestSessionResponse.builder()
                .sessionId(result.getSessionId())
                .guestToken(result.getRawGuestToken())
                .expiresAt(result.getMetadata().getExpiresAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo phiên trò chuyện khách vãng lai thành công", response));
    }

    /**
     * Gửi tin nhắn tới chatbot và nhận phản hồi tư vấn bán hàng từ Gemini AI.
     * Hỗ trợ đồng thời:
     * - POST /api/v1/chat           (Chuẩn theo Definition of Done)
     * - POST /api/v1/chat/messages  (Tương thích frontend hiện tại)
     *
     * @param guestTokenHeader Header "X-Guest-Token" chứa token của khách (bắt buộc nếu là Guest)
     * @param chatRequest      Payload chứa sessionId và message
     * @param request          HTTP request để trích xuất IP và Authorization header
     * @return 200 OK cùng câu trả lời của trợ lý ảo và version OCC mới
     */
    @PostMapping({"", "/messages"})
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @RequestHeader(value = "X-Guest-Token", required = false) String guestTokenHeader,
            @Valid @RequestBody ChatRequest chatRequest,
            HttpServletRequest request
    ) {
        String authenticatedUserId = resolveAuthenticatedUserId(request);
        String clientIp = resolveClientIp(request);

        // Kiểm tra Rate Limit phân tán
        rateLimiterService.checkRateLimit(clientIp, authenticatedUserId);

        // Khách vãng lai bắt buộc phải có guest token
        if (authenticatedUserId == null && (guestTokenHeader == null || guestTokenHeader.isBlank())) {
            throw new ChatAuthenticationException("Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)");
        }

        if (chatRequest.getSessionId() == null || chatRequest.getSessionId().isBlank()) {
            throw new IllegalArgumentException("Session ID không được để trống khi gửi tin nhắn");
        }

        ChatResponse response = chatService.sendMessage(
                chatRequest.getSessionId(),
                chatRequest.getMessage(),
                guestTokenHeader,
                authenticatedUserId,
                chatRequest.getClientMessageId()
        );

        return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", response));
    }

    /**
     * Nhận quyền sở hữu phiên trò chuyện từ khách vãng lai khi người dùng đăng nhập.
     * POST /api/v1/chat/claim
     *
     * @param claimRequest Payload chứa sessionId và guestToken
     * @param request      HTTP request để kiểm tra xác thực người dùng
     * @return 200 OK cùng metadata phiên đã được chuyển giao quyền sở hữu
     */
    @PostMapping("/claim")
    public ResponseEntity<ApiResponse<ChatSessionMetadata>> claimSession(
            @Valid @RequestBody ClaimSessionRequest claimRequest,
            HttpServletRequest request
    ) {
        String authenticatedUserId = resolveAuthenticatedUserId(request);
        if (authenticatedUserId == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập để nhận quyền sở hữu phiên trò chuyện");
        }

        String clientIp = resolveClientIp(request);
        rateLimiterService.checkRateLimit(clientIp, authenticatedUserId);

        ChatSessionMetadata updatedMeta = chatService.claimSession(
                claimRequest.getSessionId(),
                claimRequest.getGuestToken(),
                authenticatedUserId
        );

        return ResponseEntity.ok(ApiResponse.success("Nhận quyền sở hữu phiên trò chuyện thành công", updatedMeta));
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Trích xuất ID người dùng đã xác thực an toàn từ SecurityContext.
     * Nếu client truyền Authorization header nhưng token sai/hết hạn, ném BadCredentialsException ngay lập tức.
     */
    private String resolveAuthenticatedUserId(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authHeader != null && !authHeader.isBlank()) {
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
                    return String.valueOf(cud.getUserId());
                }
                return authentication.getName();
            }
            // Có Authorization header nhưng không xác thực thành công -> JWT không hợp lệ hoặc hết hạn
            log.warn("Invalid or expired Bearer token provided in request to {}", request.getRequestURI());
            throw new BadCredentialsException("Token xác thực không hợp lệ hoặc đã hết hạn");
        }

        // Không có Authorization header -> xử lý như khách vãng lai (Guest)
        return null;
    }

    /**
     * Trích xuất địa chỉ IP remote của client mà server nhận được trực tiếp (không tin tưởng spoofed headers).
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }
}

