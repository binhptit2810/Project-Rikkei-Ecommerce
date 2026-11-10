import React, { useState, useEffect, useRef } from 'react'
import {
  X,
  Send,
  RotateCcw,
  Sparkles,
  Bot,
  User,
  Minus,
  CheckCircle2,
  Clock,
  HelpCircle,
  Tag,
  Package,
  Ruler,
  RefreshCw
} from 'lucide-react'
import chatService from '../../services/chatService'
import { useAuthStore } from '../../store/useAuthStore'
import { ChatProductList } from './ChatProductCard'

// Các câu hỏi gợi ý nhanh bắt buộc theo yêu cầu Trello và mở rộng tiện ích
const QUICK_PROMPTS = [
  { id: 'size', label: 'Tư vấn chọn size', icon: Ruler },
  { id: 'return', label: 'Chính sách đổi trả', icon: RefreshCw },
  { id: 'order', label: 'Kiểm tra đơn hàng', icon: Package },
  { id: 'coupon', label: 'Mã giảm giá hôm nay', icon: Tag },
]

// Khởi tạo tin nhắn chào mừng ban đầu
const INITIAL_MESSAGES = [
  {
    id: 'welcome-1',
    sender: 'ai',
    text: 'Xin chào quý khách! 👋 Tôi là **Rikkei AI** - Trợ lý mua sắm thông minh của bạn.\n\nTôi sẵn sàng hỗ trợ bạn tức thì:\n• 📏 Tư vấn chọn size chuẩn vóc dáng\n• 🔄 Chính sách đổi trả & bảo hành\n• 📦 Tra cứu hành trình đơn hàng\n• 🎟️ Săn mã giảm giá hot hôm nay\n\nBạn cần Rikkei AI hỗ trợ thông tin gì ạ?',
    timestamp: new Date().toISOString(),
  },
]

// Lưu trữ lịch sử chat và phiên an toàn trong sessionStorage
const STORAGE_KEY = 'rikkei_store_ai_chat_history'
const SESSION_STORAGE_KEY = 'rikkei_store_ai_chat_session'

const loadChatHistory = () => {
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch (_e) {
    // SessionStorage bị khoá hoặc lỗi JSON -> fallback về mặc định
  }
  return INITIAL_MESSAGES
}

const saveChatHistory = (messages) => {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages))
  } catch (_e) {
    // Bỏ qua lỗi hạn mức lưu trữ
  }
}

const loadChatSession = () => {
  try {
    const saved = sessionStorage.getItem(SESSION_STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (parsed && parsed.sessionId) {
        return parsed
      }
    }
  } catch (_e) {}
  return null
}

const saveChatSession = (session) => {
  try {
    if (!session) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY)
    } else {
      sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
    }
  } catch (_e) {}
}

/**
 * Định dạng thông báo lỗi thân thiện dựa trên mã phản hồi HTTP của Backend.
 */
const formatErrorMessage = (error) => {
  if (!error.response) {
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      return '⚠️ Dịch vụ tư vấn AI phản hồi quá thời gian. Bạn vui lòng thử lại câu hỏi nhé.'
    }
    return '⚠️ Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.'
  }

  const status = error.response.status
  const backendMsg = error.response.data?.message

  switch (status) {
    case 400:
      return backendMsg || '⚠️ Yêu cầu không hợp lệ. Vui lòng kiểm tra lại nội dung câu hỏi.'
    case 401:
      return '⚠️ Phiên trò chuyện hoặc phiên đăng nhập đã hết hạn. Vui lòng thử lại.'
    case 403:
      return '⚠️ Bạn không có quyền truy cập dữ liệu này. Vui lòng kiểm tra lại tài khoản.'
    case 409:
      return backendMsg || '⚠️ Phiên trò chuyện đang bận xử lý tin nhắn trước hoặc vừa được cập nhật. Vui lòng đợi trong giây lát.'
    case 429: {
      const retryAfter = error.response.headers?.['retry-after']
      if (retryAfter) {
        return `⚠️ Hệ thống AI đang bận do nhiều người sử dụng. Vui lòng đợi ${retryAfter} giây rồi bấm "Thử lại".`
      }
      return backendMsg || '⚠️ Hệ thống tư vấn AI đang nhận lượng yêu cầu lớn. Vui lòng bấm "Thử lại" sau ít giây.'
    }
    case 503:
      return '⚠️ Dịch vụ tư vấn AI tạm thời không khả dụng. Bạn vui lòng thử lại sau ít phút.'
    case 504:
      return '⚠️ Dịch vụ tư vấn AI phản hồi quá thời gian xử lý. Bạn vui lòng thử lại câu hỏi nhé.'
    default:
      return backendMsg || '⚠️ Đã xảy ra sự cố trong quá trình xử lý câu hỏi. Vui lòng thử lại sau.'
  }
}

// Format timestamp sang giờ:phút thân thiện
const formatTime = (isoString) => {
  try {
    const date = new Date(isoString)
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch (_e) {
    return ''
  }
}

export const ChatWidget = () => {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState(loadChatHistory)
  const [inputValue, setInputValue] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [hasUnread, setHasUnread] = useState(true)
  const [session, setSession] = useState(loadChatSession)

  const { isAuthenticated } = useAuthStore()
  const isClaimingRef = useRef(false)
  const isSubmittingRef = useRef(false)
  const prevAuthRef = useRef(isAuthenticated)

  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  // Lưu lịch sử mỗi khi danh sách tin nhắn thay đổi
  useEffect(() => {
    saveChatHistory(messages)
  }, [messages])

  // Tự động cuộn xuống tin nhắn mới nhất
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    if (isOpen) {
      scrollToBottom()
      // Tự động focus vào input khi mở modal trên thiết bị không phải mobile
      if (window.innerWidth >= 640) {
        setTimeout(() => inputRef.current?.focus(), 200)
      }
    }
  }, [isOpen, messages, isTyping])

  // Lắng nghe phím Escape để đóng/thu gọn widget
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  // Xử lý chuyển đổi trạng thái xác thực: Đăng xuất -> Xóa lịch sử bảo mật; Đăng nhập -> Claim Session
  useEffect(() => {
    // Trường hợp 1: Người dùng đăng xuất (true -> false)
    if (prevAuthRef.current && !isAuthenticated) {
      setSession(null)
      saveChatSession(null)
      setMessages(INITIAL_MESSAGES)
      saveChatHistory(INITIAL_MESSAGES)
    }

    // Trường hợp 2: Người dùng đăng nhập thành công (false -> true) -> Claim phiên Guest nếu có
    if (!prevAuthRef.current && isAuthenticated) {
      const handleClaimOnLogin = async () => {
        if (isClaimingRef.current) return
        const currentSession = session || loadChatSession()
        if (currentSession?.sessionId && currentSession?.guestToken && !currentSession?.isClaimed) {
          isClaimingRef.current = true
          try {
            await chatService.claimSession(currentSession.sessionId, currentSession.guestToken)
            const updated = {
              ...currentSession,
              guestToken: null,
              isClaimed: true,
            }
            setSession(updated)
            saveChatSession(updated)
          } catch (err) {
            console.warn('Failed to claim guest chat session on login:', err)
          } finally {
            isClaimingRef.current = false
          }
        }
      }
      handleClaimOnLogin()
    }

    prevAuthRef.current = isAuthenticated
  }, [isAuthenticated, session])

  /**
   * Đảm bảo luôn có một phiên chat hợp lệ.
   * Nếu chưa có session: Gọi Backend POST /chat/sessions.
   * Nếu người dùng đã đăng nhập, tự động claim phiên ngay lập tức.
   */
  const ensureSession = async () => {
    let currentSession = session || loadChatSession()

    if (!currentSession || !currentSession.sessionId) {
      const guestData = await chatService.createGuestSession()
      let newSession = {
        sessionId: guestData.sessionId,
        guestToken: guestData.guestToken,
        expiresAt: guestData.expiresAt,
        isClaimed: false,
      }

      // Nếu đã đăng nhập, claim ngay phiên vừa tạo để gắn với userId
      if (isAuthenticated) {
        try {
          await chatService.claimSession(newSession.sessionId, newSession.guestToken)
          newSession = {
            ...newSession,
            guestToken: null,
            isClaimed: true,
          }
        } catch (claimErr) {
          console.warn('Auto claim session for authenticated user failed:', claimErr)
        }
      }

      setSession(newSession)
      saveChatSession(newSession)
      return newSession
    }

    // Nếu đã đăng nhập nhưng phiên khách chưa được claim
    if (isAuthenticated && currentSession.guestToken && !currentSession.isClaimed && !isClaimingRef.current) {
      isClaimingRef.current = true
      try {
        await chatService.claimSession(currentSession.sessionId, currentSession.guestToken)
        currentSession = {
          ...currentSession,
          guestToken: null,
          isClaimed: true,
        }
        setSession(currentSession)
        saveChatSession(currentSession)
      } catch (err) {
        console.warn('Auto claim existing guest session failed:', err)
      } finally {
        isClaimingRef.current = false
      }
    }

    return currentSession
  }

  // Mở/Đóng widget
  const handleToggle = () => {
    setIsOpen((prev) => {
      const next = !prev
      if (next) {
        setHasUnread(false)
      }
      return next
    })
  }

  // Gửi tin nhắn thực tế tới Backend Spring AI
  const handleSendMessage = async (textToSend, existingMessageId = null) => {
    const trimmed = (textToSend || inputValue).trim()
    if (!trimmed || isTyping || isSubmittingRef.current) return
    isSubmittingRef.current = true

    if (trimmed.length > 1000) {
      alert('Nội dung tin nhắn không được vượt quá 1000 ký tự')
      isSubmittingRef.current = false
      return
    }

    const messageId = existingMessageId || `msg-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`

    // Chỉ thêm tin nhắn người dùng mới nếu không phải đang Retry tin nhắn cũ
    if (!existingMessageId) {
      const userMessage = {
        id: `user-${Date.now()}`,
        sender: 'user',
        text: trimmed,
        clientMessageId: messageId,
        timestamp: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, userMessage])
      setInputValue('')
    }

    setIsTyping(true)

    try {
      // 1. Đảm bảo session hợp lệ
      const currentSession = await ensureSession()

      // 2. Xác định token gửi kèm (Guest gửi guestToken, User đã đăng nhập gửi Bearer qua apiClient)
      const tokenToSend = isAuthenticated ? null : currentSession.guestToken

      // 3. Gọi Backend Spring AI + Google Gemini (kèm clientMessageId để chống duplicate)
      const responseData = await chatService.sendMessage(
        currentSession.sessionId,
        trimmed,
        tokenToSend,
        messageId
      )

      if (responseData && responseData.message) {
        const aiMessage = {
          id: `ai-${Date.now()}`,
          sender: 'ai',
          text: responseData.message,
          products: Array.isArray(responseData.products) ? responseData.products : [],
          timestamp: new Date().toISOString(),
        }
        setMessages((prev) => [...prev, aiMessage])
      } else {
        throw new Error('Phản hồi từ dịch vụ AI không hợp lệ')
      }
    } catch (error) {
      console.error('Lỗi gửi tin nhắn tới Chatbot AI:', error)
      const errorText = formatErrorMessage(error)
      const retryAfter = error.response?.headers?.['retry-after']
      const aiErrorMessage = {
        id: `ai-err-${Date.now()}`,
        sender: 'ai',
        text: errorText,
        isError: true,
        failedText: trimmed,
        clientMessageId: messageId,
        retryAfter: retryAfter ? parseInt(retryAfter, 10) : null,
        timestamp: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, aiErrorMessage])

      // Nếu session bị 404 (hết hạn trong Redis) hoặc 401 không hợp lệ, xóa session cũ để lượt sau tạo mới
      if (error.response?.status === 404 || error.response?.status === 401) {
        setSession(null)
        saveChatSession(null)
      }
    } finally {
      setIsTyping(false)
      isSubmittingRef.current = false
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    handleSendMessage(inputValue)
  }

  // Xử lý gửi gợi ý nhanh
  const handleQuickPromptClick = (label) => {
    if (isTyping) return
    handleSendMessage(label)
  }

  // Làm mới đoạn chat
  const handleResetChat = () => {
    const resetList = [
      {
        id: `welcome-${Date.now()}`,
        sender: 'ai',
        text: 'Đoạn hội thoại đã được làm mới! 👋 Tôi là Rikkei AI, tôi có thể giúp gì cho bạn ngay bây giờ?',
        timestamp: new Date().toISOString(),
      },
    ]
    setMessages(resetList)
    saveChatHistory(resetList)
    setSession(null)
    saveChatSession(null)
    setInputValue('')
    setIsTyping(false)
  }

  const charCount = inputValue.length
  const showCharWarning = charCount > 800

  return (
    <>
      {/* 1. Expand/Collapse Modal Dialog */}
      <div
        className={`fixed bottom-24 right-4 sm:right-6 z-50 flex flex-col bg-white rounded-2xl shadow-2xl border border-slate-200/90 overflow-hidden w-[calc(100vw-2rem)] sm:w-[380px] md:w-[400px] h-[560px] max-h-[calc(100vh-7.5rem)] transition-all duration-300 ease-out transform origin-bottom-right ${
          isOpen
            ? 'opacity-100 scale-100 translate-y-0 pointer-events-auto'
            : 'opacity-0 scale-95 translate-y-4 pointer-events-none'
        }`}
        role="dialog"
        aria-modal="true"
        aria-label="Cửa sổ hội thoại trợ lý ảo Rikkei AI"
      >
        {/* Header Bar */}
        <div className="bg-gradient-to-r from-[#070d1e] via-[#0b1633] to-[#11224d] text-white p-3.5 sm:p-4 flex items-center justify-between border-b border-slate-800 shadow-sm shrink-0">
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-blue-600 via-indigo-600 to-blue-400 p-0.5 shadow-md flex items-center justify-center">
                <div className="w-full h-full rounded-full bg-[#0b1633] flex items-center justify-center">
                  <Bot className="w-5 h-5 text-blue-400" />
                </div>
              </div>
              {/* Online Status Dot */}
              <span
                className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-400 rounded-full ring-2 ring-[#070d1e] animate-pulse"
                title="Đang trực tuyến"
              />
            </div>
            <div className="flex flex-col">
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-sm tracking-tight text-white">
                  Trợ Lý Mua Sắm Rikkei AI
                </h3>
                <Sparkles className="w-3.5 h-3.5 text-yellow-400 fill-current" />
              </div>
              <p className="text-[11px] text-blue-200/80 flex items-center gap-1">
                <span>Phản hồi tức thì • 24/7</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1">
            {/* Làm mới đoạn chat Button */}
            <button
              type="button"
              onClick={handleResetChat}
              title="Làm mới đoạn chat"
              aria-label="Làm mới đoạn chat"
              className="p-1.5 text-slate-300 hover:text-white hover:bg-white/10 rounded-lg transition-colors cursor-pointer"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
            {/* Thu gọn / Đóng Button */}
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              title="Thu gọn (Esc)"
              aria-label="Đóng trợ lý ảo"
              className="p-1.5 text-slate-300 hover:text-white hover:bg-white/10 rounded-lg transition-colors cursor-pointer"
            >
              <Minus className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Message List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/70 scroll-smooth">
          {messages.map((msg) => {
            const isAi = msg.sender === 'ai'
            const isErrMsg = Boolean(msg.isError)

            return (
              <div
                key={msg.id}
                className={`flex items-start gap-2.5 ${
                  isAi ? 'justify-start' : 'justify-end'
                }`}
              >
                {/* AI Avatar */}
                {isAi && (
                  <div className={`w-7 h-7 rounded-full text-white flex items-center justify-center shrink-0 mt-0.5 shadow-xs ${
                    isErrMsg
                      ? 'bg-gradient-to-tr from-rose-500 to-red-600'
                      : 'bg-gradient-to-tr from-blue-600 to-indigo-600'
                  }`}>
                    <Bot className="w-4 h-4" />
                  </div>
                )}

                {/* Message Bubble */}
                <div className={`max-w-[84%] flex flex-col ${isAi ? 'items-start' : 'items-end'}`}>
                  <div
                    className={`p-3 sm:p-3.5 text-sm leading-relaxed whitespace-pre-line break-words ${
                      isAi
                        ? isErrMsg
                          ? 'bg-rose-50 border border-rose-200 text-rose-800 rounded-2xl rounded-tl-xs shadow-xs'
                          : 'bg-white border border-slate-200 text-slate-800 rounded-2xl rounded-tl-xs shadow-xs'
                        : 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-2xl rounded-tr-xs shadow-md shadow-blue-500/10'
                    }`}
                  >
                    {msg.text}
                    {isErrMsg && msg.failedText && (
                      <div className="mt-2 pt-2 border-t border-rose-200/60">
                        <button
                          type="button"
                          disabled={isTyping}
                          onClick={() => handleSendMessage(msg.failedText, msg.clientMessageId)}
                          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-rose-700 bg-white border border-rose-300 rounded-lg hover:bg-rose-100/60 transition-all shadow-2xs cursor-pointer disabled:opacity-50 active:scale-95"
                        >
                          <RefreshCw className="w-3 h-3 text-rose-600" />
                          <span>Thử lại</span>
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Danh sách Product Cards bên dưới câu trả lời của AI */}
                  {isAi && Array.isArray(msg.products) && msg.products.length > 0 && (
                    <ChatProductList products={msg.products} />
                  )}

                  <div
                    className={`flex items-center gap-1 mt-1 text-[10px] ${
                      isAi ? 'text-slate-400 pl-1' : 'text-slate-400 pr-1'
                    }`}
                  >
                    <Clock className="w-2.5 h-2.5" />
                    <span>{formatTime(msg.timestamp)}</span>
                    {!isAi && <CheckCircle2 className="w-2.5 h-2.5 text-blue-500 ml-0.5" />}
                  </div>
                </div>

                {/* User Avatar */}
                {!isAi && (
                  <div className="w-7 h-7 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center shrink-0 mt-0.5 shadow-xs">
                    <User className="w-4 h-4" />
                  </div>
                )}
              </div>
            )
          })}

          {/* Typing Indicator */}
          {isTyping && (
            <div className="flex items-start gap-2.5 justify-start">
              <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center shrink-0 mt-0.5 shadow-xs">
                <Bot className="w-4 h-4" />
              </div>
              <div className="bg-white border border-slate-200 text-slate-800 rounded-2xl rounded-tl-xs p-3 shadow-xs flex items-center gap-2">
                <div className="flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce [animation-delay:-0.3s]" />
                  <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce [animation-delay:-0.15s]" />
                  <span className="w-1.5 h-1.5 rounded-full bg-indigo-600 animate-bounce" />
                </div>
                <span className="text-xs text-slate-400 italic">Rikkei AI đang suy nghĩ và phản hồi...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Prompts Bar (Gợi ý nhanh) */}
        <div className="px-3 pt-2 pb-1.5 bg-white border-t border-slate-100 shrink-0">
          <div className="flex items-center gap-1.5 mb-1.5 text-[11px] font-medium text-slate-500">
            <HelpCircle className="w-3 h-3 text-indigo-500" />
            <span>Gợi ý câu hỏi nhanh:</span>
          </div>
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
            {QUICK_PROMPTS.map((prompt) => {
              const IconComponent = prompt.icon
              return (
                <button
                  key={prompt.id}
                  type="button"
                  disabled={isTyping}
                  onClick={() => handleQuickPromptClick(prompt.label)}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border border-slate-200 bg-slate-50 hover:bg-blue-50 hover:border-blue-300 hover:text-blue-700 text-slate-700 transition-all shrink-0 disabled:opacity-50 disabled:cursor-not-allowed shadow-2xs active:scale-95 cursor-pointer"
                >
                  <IconComponent className="w-3 h-3 text-slate-500" />
                  <span>{prompt.label}</span>
                </button>
              )
            })}
          </div>
        </div>

        {/* Chat Input Bar */}
        <form
          onSubmit={handleSubmit}
          className="p-3 bg-white border-t border-slate-200/90 flex flex-col gap-1 shrink-0"
        >
          <div className="flex items-center gap-2">
            <input
              ref={inputRef}
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder="Nhập câu hỏi cho Rikkei AI..."
              maxLength={1000}
              disabled={isTyping}
              className="flex-1 h-10 px-3.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 transition-all text-slate-800 placeholder:text-slate-400 disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={!inputValue.trim() || isTyping}
              aria-label="Gửi tin nhắn"
              className="h-10 w-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white flex items-center justify-center shadow-md shadow-blue-500/20 disabled:opacity-40 disabled:cursor-not-allowed active:scale-95 transition-all shrink-0 cursor-pointer"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>

          {/* Character counter & Hint */}
          <div className="flex items-center justify-between px-1 text-[10px] text-slate-400">
            <span>Nhấn Enter để gửi tin nhắn</span>
            {showCharWarning && (
              <span className={charCount >= 1000 ? 'text-red-500 font-bold' : 'text-amber-500'}>
                {charCount}/1000 ký tự
              </span>
            )}
          </div>
        </form>
      </div>

      {/* 2. Floating Trigger Button (Collapsed State) */}
      <div className="fixed bottom-6 right-6 z-50">
        <button
          type="button"
          onClick={handleToggle}
          aria-label={isOpen ? 'Đóng trợ lý ảo' : 'Mở trợ lý ảo AI'}
          className="relative group p-3.5 sm:p-4 rounded-full bg-gradient-to-tr from-blue-600 via-indigo-600 to-blue-500 text-white shadow-xl hover:shadow-2xl hover:scale-105 active:scale-95 transition-all duration-300 ring-4 ring-blue-500/20 hover:ring-blue-500/40 flex items-center justify-center cursor-pointer"
        >
          {isOpen ? (
            <X className="w-6 h-6 transition-transform duration-200 group-hover:rotate-90" />
          ) : (
            <div className="relative">
              <Bot className="w-6 h-6 transition-transform duration-200 group-hover:scale-110" />
              {/* Sparkle badge */}
              <Sparkles className="w-3.5 h-3.5 text-yellow-300 fill-current absolute -top-1.5 -right-2 animate-bounce" />
            </div>
          )}

          {/* Unread notification badge when collapsed */}
          {!isOpen && hasUnread && (
            <span className="absolute -top-1 -right-1 flex h-4 w-4">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-4 w-4 bg-emerald-500 text-white text-[9px] font-bold items-center justify-center shadow-xs">
                1
              </span>
            </span>
          )}

          {/* Hover Tooltip when closed */}
          {!isOpen && (
            <span className="absolute right-full mr-3 top-1/2 -translate-y-1/2 px-3 py-1.5 bg-slate-900 text-white text-xs font-medium rounded-lg shadow-lg whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none hidden sm:block">
              Hỏi trợ lý Rikkei AI
              <span className="absolute left-full top-1/2 -translate-y-1/2 border-4 border-transparent border-l-slate-900" />
            </span>
          )}
        </button>
      </div>
    </>
  )
}

export default ChatWidget
