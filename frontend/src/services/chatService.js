import apiClient from './apiClient'

/**
 * Service quản lý các lời gọi API cho Chatbot tư vấn bán hàng Rikkei Ecommerce.
 * Tích hợp trực tiếp với Backend Spring AI và Google Gemini (TSK-D11-CCT).
 * 
 * Các API tương ứng tại Backend (ChatController.java):
 * 1. POST /api/v1/chat/sessions  - Khởi tạo phiên trò chuyện cho khách vãng lai (Guest).
 * 2. POST /api/v1/chat/messages  - Gửi tin nhắn và nhận câu trả lời tư vấn từ Gemini AI.
 * 3. POST /api/v1/chat/claim     - Nhận quyền sở hữu phiên trò chuyện khi người dùng đăng nhập.
 */
export const chatService = {
  /**
   * Khởi tạo phiên chat mới cho khách vãng lai (Guest).
   * POST /api/v1/chat/sessions
   * 
   * @returns {Promise<{ sessionId: string, guestToken: string, expiresAt: string }>}
   */
  async createGuestSession() {
    const response = await apiClient.post('/chat/sessions')
    return response.data?.data
  },

  /**
   * Gửi tin nhắn tới chatbot và nhận phản hồi tư vấn bán hàng từ Gemini AI.
   * POST /api/v1/chat/messages
   * 
   * @param {string} sessionId Mã định danh phiên chat chuẩn UUID v4
   * @param {string} message Nội dung câu hỏi (1 - 1000 ký tự)
   * @param {string|null} [guestToken=null] Token bí mật của khách vãng lai (truyền qua header X-Guest-Token nếu là Guest)
   * @param {string|null} [clientMessageId=null] Idempotency key duy nhất từ client để chống gửi trùng và hỗ trợ retry
   * @returns {Promise<{ sessionId: string, message: string, version: number, products?: Array }>}
   */
  async sendMessage(sessionId, message, guestToken = null, clientMessageId = null) {
    const headers = {}
    if (guestToken) {
      headers['X-Guest-Token'] = guestToken
    }

    const payload = {
      sessionId,
      message,
    }
    if (clientMessageId) {
      payload.clientMessageId = clientMessageId
    }

    const response = await apiClient.post(
      '/chat/messages',
      payload,
      {
        headers,
        timeout: 40000, // 40 giây timeout riêng cho Chatbot AI (Backend deadline là 32s)
      }
    )

    return response.data?.data
  },

  /**
   * Nhận quyền sở hữu phiên trò chuyện từ khách vãng lai khi người dùng đăng nhập tài khoản.
   * POST /api/v1/chat/claim
   * 
   * @param {string} sessionId Mã định danh phiên chat của khách (UUID v4)
   * @param {string} guestToken Mã token bí mật chứng minh quyền sở hữu phiên khách
   * @returns {Promise<any>}
   */
  async claimSession(sessionId, guestToken) {
    const response = await apiClient.post('/chat/claim', {
      sessionId,
      guestToken,
    })

    return response.data?.data
  },
}

export default chatService

