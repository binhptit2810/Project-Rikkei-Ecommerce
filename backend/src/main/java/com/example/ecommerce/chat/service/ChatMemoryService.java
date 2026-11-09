package com.example.ecommerce.chat.service;

import com.example.ecommerce.chat.model.ChatMessage;
import com.example.ecommerce.chat.model.ChatMemoryData;
import com.example.ecommerce.chat.model.ChatSessionMetadata;
import com.example.ecommerce.chat.model.GuestSessionCreationResult;

/**
 * Service quản lý phiên trò chuyện và bộ nhớ hội thoại Redis cho Chatbot tư vấn bán hàng.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc kiến trúc & bảo mật:
 * 1. Hỗ trợ phiên khách (Guest) và phiên người dùng đã đăng nhập (User).
 * 2. Bảo đảm tính nguyên tử qua Redis Lua Scripting kết hợp Optimistic Concurrency Control (OCC).
 * 3. Chống race condition đa luồng qua Distributed Lock (TTL 45s, lock token UUID).
 * 4. Tự động cắt tỉa (trim) bảo toàn cặp tin nhắn chẵn (tối đa 20 messages = 10 cặp).
 * 5. Chuyển giao quyền sở hữu phiên từ Guest sang User nguyên tử (Claim Guest Session).
 */
public interface ChatMemoryService {

    /**
     * Khởi tạo phiên trò chuyện mới cho khách vãng lai (Guest).
     *
     * @return GuestSessionCreationResult chứa sessionId, rawGuestToken và metadata.
     */
    GuestSessionCreationResult createGuestSession();

    /**
     * Chiếm Distributed Lock cho một phiên hội thoại với TTL 45 giây.
     *
     * @param sessionId Mã định danh phiên.
     * @return lockToken UUID bí mật dùng để xác thực quyền ghi hoặc giải phóng lock.
     * @throws com.example.ecommerce.exception.ChatLockContentionException Nếu có request khác đang giữ lock.
     */
    String acquireLock(String sessionId);

    /**
     * Giải phóng Distributed Lock thông qua cơ chế compare-and-delete (an toàn).
     *
     * @param sessionId Mã định danh phiên.
     * @param lockToken Token sở hữu lock.
     */
    void releaseLock(String sessionId, String lockToken);

    /**
     * Đọc bộ nhớ hội thoại và metadata của phiên từ Redis.
     *
     * @param sessionId Mã định danh phiên.
     * @param guestToken Token của khách (nếu là phiên Guest, có thể null nếu là User).
     * @param authenticatedUserId ID của người dùng đã xác thực (null nếu là Guest).
     * @return ChatMemoryData chứa metadata và danh sách tin nhắn.
     */
    ChatMemoryData getChatMemory(String sessionId, String guestToken, String authenticatedUserId);

    /**
     * Lưu trữ một cặp đối thoại hoàn chỉnh (User Message + Assistant Message) vào Redis memory,
     * tự động trim về tối đa 20 tin nhắn gần nhất và tăng OCC version lên 1 đơn vị.
     *
     * @param sessionId Mã định danh phiên.
     * @param lockToken Token sở hữu Distributed Lock hiện tại.
     * @param expectedVersion Phiên bản OCC mong đợi tại thời điểm đọc.
     * @param userMessage Tin nhắn từ người dùng.
     * @param assistantMessage Tin nhắn phản hồi từ trợ lý ảo.
     * @param guestToken Token của khách (nếu là phiên Guest).
     * @param authenticatedUserId ID của người dùng đã xác thực (nếu là phiên User).
     * @return ChatSessionMetadata Metadata mới nhất sau khi cập nhật.
     */
    ChatSessionMetadata saveChatMemory(
            String sessionId,
            String lockToken,
            Long expectedVersion,
            ChatMessage userMessage,
            ChatMessage assistantMessage,
            String guestToken,
            String authenticatedUserId
    );

    /**
     * Chuyển quyền sở hữu phiên trò chuyện từ khách vãng lai sang người dùng đã xác thực (Atomic Claim).
     * Vô hiệu hóa guest token và gia hạn TTL lên 7 ngày.
     *
     * @param sessionId Mã định danh phiên.
     * @param guestToken Token của khách để chứng minh quyền sở hữu ban đầu.
     * @param authenticatedUserId ID của người dùng đăng nhập tiếp nhận phiên.
     * @return ChatSessionMetadata Metadata sau khi đã chuyển giao quyền sở hữu.
     */
    ChatSessionMetadata claimGuestSession(String sessionId, String guestToken, String authenticatedUserId);
}

