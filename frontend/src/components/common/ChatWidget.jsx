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

// Lưu trữ lịch sử chat an toàn trong sessionStorage
const STORAGE_KEY = 'rikkei_store_ai_chat_history'

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

// Bộ máy phản hồi AI thông minh chuẩn ngữ cảnh (Deterministic Knowledge Engine)
const getAiResponse = (userInput) => {
  const query = userInput.toLowerCase().trim()

  // 1. Tư vấn chọn size
  if (
    query.includes('tư vấn chọn size') ||
    query.includes('chọn size') ||
    query.includes('bảng size') ||
    query.includes('size áo') ||
    query.includes('size quần') ||
    query.includes('chiều cao') ||
    query.includes('cân nặng') ||
    query.includes('mặc size gì')
  ) {
    return (
      '📏 **BẢNG TƯ VẤN CHỌN SIZE CHUẨN RIKKEI STORE**\n\n' +
      '👔 **Dành cho Nam:**\n' +
      '• **Size S**: Cao 1m55 - 1m65 | Nặng 48 - 55kg\n' +
      '• **Size M**: Cao 1m65 - 1m72 | Nặng 56 - 65kg\n' +
      '• **Size L**: Cao 1m70 - 1m77 | Nặng 66 - 75kg\n' +
      '• **Size XL**: Cao 1m75 - 1m85 | Nặng 76 - 85kg\n' +
      '• **Size XXL**: Cao trên 1m80 | Nặng trên 85kg\n\n' +
      '👗 **Dành cho Nữ:**\n' +
      '• **Size S**: Cao 1m50 - 1m58 | Nặng 40 - 47kg\n' +
      '• **Size M**: Cao 1m55 - 1m62 | Nặng 48 - 54kg\n' +
      '• **Size L**: Cao 1m60 - 1m68 | Nặng 55 - 62kg\n' +
      '• **Size XL**: Cao 1m65 - 1m72 | Nặng 63 - 70kg\n\n' +
      '💡 **Lời khuyên:** Nếu số đo của bạn ở giữa 2 size, hãy ưu tiên chọn size lớn hơn để có form mặc thoải mái nhất nhé!'
    )
  }

  // 2. Chính sách đổi trả
  if (
    query.includes('chính sách đổi trả') ||
    query.includes('đổi trả') ||
    query.includes('bảo hành') ||
    query.includes('hoàn tiền') ||
    query.includes('đổi hàng') ||
    query.includes('trả hàng')
  ) {
    return (
      '🔄 **CHÍNH SÁCH ĐỔI TRẢ & BẢO HÀNH CHÍNH HÃNG**\n\n' +
      '• **Thời hạn đổi trả:** Hỗ trợ đổi trả miễn phí trong vòng **7 - 30 ngày** kể từ ngày nhận hàng thành công.\n' +
      '• **Điều kiện:** Sản phẩm còn nguyên tem mác, hộp phụ kiện, hoá đơn và chưa qua giặt tẩy hay sử dụng.\n' +
      '• **Lỗi từ nhà sản xuất / giao sai hàng:** Rikkei Store chịu 100% phí ship 2 chiều, shipper đến tận nhà thu hồi hàng đổi.\n' +
      '• **Đổi size theo yêu cầu:** Quý khách được đổi sang size vừa vặn hoàn toàn thuận tiện.\n' +
      '• **Hoàn tiền:** Thực hiện qua chuyển khoản hoặc VNPay trong **24 - 48h** sau khi kho tiếp nhận hàng hoàn.\n\n' +
      '📞 Hotline hỗ trợ khiếu nại & đổi trả: **1900-RIKKEI** (8:00 - 22:00).'
    )
  }

  // 3. Kiểm tra đơn hàng
  if (
    query.includes('kiểm tra đơn hàng') ||
    query.includes('đơn hàng') ||
    query.includes('vận chuyển') ||
    query.includes('giao hàng') ||
    query.includes('bao lâu nhận được') ||
    query.includes('mã vận đơn')
  ) {
    return (
      '📦 **TRA CỨU HÀNH TRÌNH ĐƠN HÀNG**\n\n' +
      'Để kiểm tra tiến độ đơn hàng nhanh nhất:\n' +
      '1. Truy cập mục **"Lịch sử đơn hàng"** trong biểu tượng tài khoản ở thanh điều hướng trên cùng.\n' +
      '2. Xem trực tiếp mã vận đơn, trạng thái đóng gói và hành trình vận chuyển thực tế.\n' +
      '3. **Thời gian giao hàng tiêu chuẩn:**\n' +
      '   • Nội thành Hà Nội & TP.HCM: 1 - 2 ngày (có tuỳ chọn hỏa tốc 2 - 4h).\n' +
      '   • Các tỉnh thành khác: 2 - 4 ngày làm việc.\n\n' +
      '✨ Nếu bạn có mã đơn hàng (VD: #ORD-1234), hãy nhập trực tiếp vào khung chat để tôi hỗ trợ nhé!'
    )
  }

  // 4. Mã giảm giá hôm nay
  if (
    query.includes('mã giảm giá') ||
    query.includes('mã giảm giá hôm nay') ||
    query.includes('voucher') ||
    query.includes('coupon') ||
    query.includes('khuyến mãi') ||
    query.includes('ưu đãi') ||
    query.includes('giảm giá')
  ) {
    return (
      '🎟️ **DANH SÁCH MÃ GIẢM GIÁ HOT NHẤT HÔM NAY**\n\n' +
      '• **RIKKEINEW**: Giảm ngay **15%** (tối đa 100.000₫) cho đơn hàng đầu tiên.\n' +
      '• **FREESHIP**: Miễn phí vận chuyển toàn quốc cho đơn hàng từ **500.000₫**.\n' +
      '• **TECHNOVA50**: Giảm trực tiếp **50.000₫** cho đơn từ **800.000₫**.\n' +
      '• **VNPAY10**: Giảm thêm **10%** khi chọn thanh toán qua cổng quét mã VNPay-QR.\n\n' +
      '👉 Bạn hãy sao chép mã và dán vào ô *Mã giảm giá* tại trang Giỏ hàng / Thanh toán để hưởng ưu đãi ngay!'
    )
  }

  // 5. Thanh toán / VNPay / COD
  if (
    query.includes('thanh toán') ||
    query.includes('vnpay') ||
    query.includes('cod') ||
    query.includes('tiền mặt') ||
    query.includes('chuyển khoản')
  ) {
    return (
      '💳 **PHƯƠNG THỨC THANH TOÁN TIỆN LỢI TẠI RIKKEI STORE**\n\n' +
      '1. **COD (Thanh toán khi nhận hàng):** Nhận hàng kiểm tra xong mới trả tiền mặt cho shipper.\n' +
      '2. **VNPay-QR:** Quét mã tức thì qua 40+ ứng dụng ngân hàng và ví điện tử, thanh toán bảo mật 100% không mất phí giao dịch.\n\n' +
      'Mọi giao dịch đều được mã hoá bảo mật chuẩn PCI-DSS và xác thực SSL.'
    )
  }

  // 6. Chào hỏi
  if (
    query === 'hi' ||
    query === 'hello' ||
    query.startsWith('chào') ||
    query.startsWith('xin chào') ||
    query.includes('alo')
  ) {
    return (
      'Chào bạn! Rất vui được đồng hành cùng bạn tại Rikkei Store hôm nay. 😊\n\n' +
      'Bạn đang muốn tìm kiếm sản phẩm nào, cần tư vấn size, kiểm tra đơn hàng hay nhận ưu đãi đặc biệt? Hãy nhắn tin cho mình nhé!'
    )
  }

  // 7. Cảm ơn
  if (query.includes('cảm ơn') || query.includes('cam on') || query.includes('thanks') || query.includes('thank')) {
    return (
      'Không có chi ạ! Rất hân hạnh được phục vụ bạn. Chúc bạn có trải nghiệm mua sắm tuyệt vời tại Rikkei Store! ✨\n\n' +
      'Nếu cần thêm trợ giúp, bạn cứ nhắn tin cho tôi bất kỳ lúc nào nhé!'
    )
  }

  // 8. Trả lời mặc định / Fallback thông minh
  return (
    `Cảm ơn bạn đã đặt câu hỏi về: "${userInput}".\n\n` +
    'Hiện tại trợ lý AI đã ghi nhận yêu cầu của bạn. Bạn có thể nhấn vào các chủ đề gợi ý nhanh ở trên (chọn size, đổi trả, mã giảm giá, kiểm tra đơn hàng) hoặc liên hệ đội ngũ chuyên viên hỗ trợ 24/7 qua Hotline **1900-RIKKEI** để được phục vụ chu đáo nhất!'
  )
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

  // Gửi tin nhắn
  const handleSendMessage = (textToSend) => {
    const trimmed = (textToSend || inputValue).trim()
    if (!trimmed || isTyping) return

    const userMessage = {
      id: `user-${Date.now()}`,
      sender: 'user',
      text: trimmed,
      timestamp: new Date().toISOString(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInputValue('')
    setIsTyping(true)

    // Mô phỏng độ trễ suy nghĩ của AI tự nhiên (600ms - 900ms)
    setTimeout(() => {
      const aiResponseText = getAiResponse(trimmed)
      const aiMessage = {
        id: `ai-${Date.now()}`,
        sender: 'ai',
        text: aiResponseText,
        timestamp: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, aiMessage])
      setIsTyping(false)
    }, 750)
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
    setInputValue('')
    setIsTyping(false)
  }

  const charCount = inputValue.length
  const showCharWarning = charCount > 400

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
            return (
              <div
                key={msg.id}
                className={`flex items-start gap-2.5 ${
                  isAi ? 'justify-start' : 'justify-end'
                }`}
              >
                {/* AI Avatar */}
                {isAi && (
                  <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center shrink-0 mt-0.5 shadow-xs">
                    <Bot className="w-4 h-4" />
                  </div>
                )}

                {/* Message Bubble */}
                <div className={`max-w-[84%] flex flex-col ${isAi ? 'items-start' : 'items-end'}`}>
                  <div
                    className={`p-3 sm:p-3.5 text-sm leading-relaxed whitespace-pre-line break-words ${
                      isAi
                        ? 'bg-white border border-slate-200 text-slate-800 rounded-2xl rounded-tl-xs shadow-xs'
                        : 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-2xl rounded-tr-xs shadow-md shadow-blue-500/10'
                    }`}
                  >
                    {msg.text}
                  </div>
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
                <span className="text-xs text-slate-400 italic">Rikkei AI đang gõ...</span>
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
              maxLength={500}
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
              <span className={charCount >= 500 ? 'text-red-500 font-bold' : 'text-amber-500'}>
                {charCount}/500 ký tự
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
