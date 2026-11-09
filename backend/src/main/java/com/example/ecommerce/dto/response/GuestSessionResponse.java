package com.example.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload khi khởi tạo phiên chat mới cho khách vãng lai.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Tích hợp Google Gemini Client & Xây dựng API Chatbot Tư Vấn Bán Hàng.
 *
 * Nguyên tắc bảo mật:
 * 1. rawGuestToken chỉ được trả về DUY NHẤT một lần tại thời điểm tạo phiên này.
 * 2. Không trả về chuỗi băm SHA-256 hay bất kỳ cấu hình bảo mật nội bộ nào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestSessionResponse {

    /**
     * Mã định danh phiên chat của khách (chuẩn UUID v4).
     */
    private String sessionId;

    /**
     * Token bí mật của khách vãng lai (32 bytes entropy, Base64 URL-safe, không padding).
     * Dùng để chứng minh quyền sở hữu phiên ở các lượt chat tiếp theo và khi claim phiên.
     */
    private String guestToken;

    /**
     * Thời điểm phiên chat khách vãng lai hết hạn (mặc định 24 giờ sau khi tạo).
     */
    private LocalDateTime expiresAt;
}

