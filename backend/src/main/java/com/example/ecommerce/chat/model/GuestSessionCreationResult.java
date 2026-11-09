package com.example.ecommerce.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nội bộ chứa kết quả khởi tạo phiên trò chuyện cho khách vãng lai (Guest).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật:
 * 1. rawGuestToken chỉ được trả về một lần duy nhất khi tạo phiên thành công.
 * 2. Tuyệt đối không log hoặc lưu rawGuestToken vào Redis hay bất kỳ kho lưu trữ bền vững nào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestSessionCreationResult {

    /**
     * Mã định danh phiên chat (UUID v4).
     */
    private String sessionId;

    /**
     * Mã token sở hữu phiên của khách (32 bytes entropy, Base64 URL-safe, unpadded).
     */
    private String rawGuestToken;

    /**
     * Metadata chi tiết của phiên vừa được khởi tạo.
     */
    private ChatSessionMetadata metadata;
}

