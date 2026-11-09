package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;
import com.example.ecommerce.dto.response.ChatResponse;

/**
 * Interface điều phối nghiệp vụ Chatbot tư vấn bán hàng Rikkei Ecommerce.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nhiệm vụ chính:
 * 1. Khởi tạo phiên khách vãng lai và cấp phát guest token.
 * 2. Tiếp nhận tin nhắn, chiếm Distributed Lock, đọc bộ nhớ từ Redis, gọi Gemini AI và ghi nhận cặp tin nhắn.
 * 3. Chuyển giao quyền sở hữu phiên từ khách sang tài khoản đã đăng nhập.
 */
public interface ChatService {

    /**
     * Khởi tạo phiên trò chuyện mới cho khách vãng lai (Guest).
     *
     * @return Kết quả gồm sessionId, rawGuestToken và metadata ban đầu.
     */
    GuestSessionCreationResult createGuestSession();

    /**
     * Xử lý lượt chat tư vấn bán hàng: tương tác với Google Gemini và quản lý lịch sử hội thoại trên Redis.
     *
     * @param sessionId           Mã định danh phiên chat (UUID v4)
     * @param message             Nội dung câu hỏi của khách hàng
     * @param guestToken          Token xác thực của khách vãng lai (bắt buộc nếu là Guest)
     * @param authenticatedUserId ID của người dùng đã xác thực (null nếu là Guest)
     * @return ChatResponse chứa câu trả lời từ trợ lý ảo AI và phiên bản OCC mới
     */
    ChatResponse sendMessage(String sessionId, String message, String guestToken, String authenticatedUserId);

    /**
     * Chuyển quyền sở hữu phiên chat từ khách vãng lai sang người dùng đã đăng nhập.
     *
     * @param sessionId           Mã định danh phiên chat của khách (UUID v4)
     * @param guestToken          Token chứng minh quyền sở hữu phiên của khách vãng lai
     * @param authenticatedUserId ID của người dùng đã xác thực
     * @return Metadata phiên đã được cập nhật quyền sở hữu và kéo dài thời hạn TTL lên 7 ngày
     */
    ChatSessionMetadata claimSession(String sessionId, String guestToken, String authenticatedUserId);
}

