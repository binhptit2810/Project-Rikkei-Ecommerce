/**
 * Helper xử lý nhận diện mã đơn hàng, tra cứu API, chuẩn hóa trạng thái và tạo dữ liệu mô phỏng QA.
 * Phụ trách: Đỗ Hoàng Anh (DHA) [TSK-D12-DHA]
 * Task ID: [TSK-D12-DHA] Tối Ưu UX Trợ Lý AI: Streaming Typing Effect & Tra Cứu Tình Trạng Đơn
 */

/**
 * Chuẩn hóa trạng thái đơn hàng sang nhãn tiếng Việt và bước tiến trình (1, 2, 3).
 * Tuân thủ nghiêm ngặt 3 trạng thái bắt buộc trong Trello:
 * - "Đang xử lý" (PENDING / CONFIRMED / PROCESSING) -> Step 1
 * - "Đang giao" (SHIPPING / SHIPPED / DELIVERING / IN_TRANSIT) -> Step 2
 * - "Đã giao" (DELIVERED / COMPLETED) -> Step 3
 * - "Đã hủy" (CANCELLED / FAILED) -> Step 0 (Cancelled state)
 */
export const normalizeOrderStatus = (rawStatus) => {
  if (!rawStatus) {
    return {
      key: 'PROCESSING',
      label: 'Đang xử lý',
      step: 1,
      badgeBg: 'bg-amber-50',
      badgeText: 'text-amber-700',
      badgeBorder: 'border-amber-200',
      color: 'amber',
    }
  }

  const s = String(rawStatus).toUpperCase()

  if (['PENDING', 'CONFIRMED', 'PROCESSING'].includes(s) || s === 'ĐANG XỬ LÝ') {
    return {
      key: 'PROCESSING',
      label: 'Đang xử lý',
      step: 1,
      badgeBg: 'bg-amber-50',
      badgeText: 'text-amber-700',
      badgeBorder: 'border-amber-200',
      color: 'amber',
    }
  }

  if (['SHIPPING', 'SHIPPED', 'DELIVERING', 'IN_TRANSIT'].includes(s) || s === 'ĐANG GIAO') {
    return {
      key: 'SHIPPING',
      label: 'Đang giao',
      step: 2,
      badgeBg: 'bg-indigo-50',
      badgeText: 'text-indigo-700',
      badgeBorder: 'border-indigo-200',
      color: 'indigo',
    }
  }

  if (['DELIVERED', 'COMPLETED'].includes(s) || s === 'ĐÃ GIAO') {
    return {
      key: 'DELIVERED',
      label: 'Đã giao',
      step: 3,
      badgeBg: 'bg-emerald-50',
      badgeText: 'text-emerald-700',
      badgeBorder: 'border-emerald-200',
      color: 'emerald',
    }
  }

  if (['CANCELLED', 'FAILED'].includes(s) || s === 'ĐÃ HỦY') {
    return {
      key: 'CANCELLED',
      label: 'Đã hủy',
      step: 0,
      badgeBg: 'bg-rose-50',
      badgeText: 'text-rose-700',
      badgeBorder: 'border-rose-200',
      color: 'rose',
    }
  }

  return {
    key: s,
    label: rawStatus,
    step: 1,
    badgeBg: 'bg-blue-50',
    badgeText: 'text-blue-700',
    badgeBorder: 'border-blue-200',
    color: 'blue',
  }
}

/**
 * Nhận diện câu hỏi có chứa mã đơn hàng hoặc ý định tra cứu tình trạng đơn.
 * Các mẫu câu hỗ trợ:
 * - "Đơn hàng ORD-xxx của tôi đến đâu rồi?"
 * - "ORD-123", "ORD-001", "ORD_002", "#102"
 * - "Kiểm tra đơn hàng", "Đơn hàng của tôi"
 *
 * @param {string} text Nội dung tin nhắn người dùng
 * @returns {Object|null}
 */
export const detectOrderQuery = (text) => {
  if (!text || typeof text !== 'string') return null
  const trimmed = text.trim()

  // 1. Nhận diện dạng mã ORD-xxx, ORD_xxx, ORDxxx (ví dụ: ORD-123, ORD-001, ORD-20241108-ABCD)
  const ordMatch = trimmed.match(/\bORD[-_]?([A-Za-z0-9-_]+)\b/i)
  if (ordMatch) {
    const rawCode = ordMatch[0].toUpperCase()
    const codePart = ordMatch[1].toUpperCase()
    const numericId = /^\d+$/.test(codePart) ? parseInt(codePart, 10) : null

    return {
      isOrderQuery: true,
      hasCode: true,
      rawCode,
      code: `ORD-${codePart}`,
      numericId,
    }
  }

  // 2. Nhận diện dạng #123
  const hashMatch = trimmed.match(/#(\d+)\b/)
  if (hashMatch) {
    const num = parseInt(hashMatch[1], 10)
    return {
      isOrderQuery: true,
      hasCode: true,
      rawCode: hashMatch[0],
      code: `ORD-${num}`,
      numericId: num,
    }
  }

  // 3. Nhận diện dạng "đơn hàng số 123" hoặc "đơn số 123"
  const orderNumMatch = trimmed.match(/đơn\s*(?:hàng)?\s*(?:số)?\s*(\d+)\b/i)
  if (orderNumMatch) {
    const num = parseInt(orderNumMatch[1], 10)
    return {
      isOrderQuery: true,
      hasCode: true,
      rawCode: `ORD-${num}`,
      code: `ORD-${num}`,
      numericId: num,
    }
  }

  // 4. Nhận diện ý định hỏi chung về đơn hàng nhưng chưa kèm mã
  const isGeneralInquiry = /(kiểm tra|tra cứu|tình trạng|tiến trình|theo dõi)\s*(?:đơn hàng|đơn)/i.test(trimmed) ||
    /đơn hàng của tôi/i.test(trimmed) ||
    /đơn hàng đến đâu/i.test(trimmed)

  if (isGeneralInquiry) {
    return {
      isOrderQuery: true,
      hasCode: false,
      rawCode: null,
      code: null,
      numericId: null,
    }
  }

  return null
}

/**
 * Kho dữ liệu đơn hàng mẫu phục vụ QA Tester kiểm thử trực tiếp mọi trạng thái theo Trello ticket:
 * - ORD-001 -> "Đang xử lý" (PROCESSING)
 * - ORD-002 -> "Đang giao" (SHIPPING)
 * - ORD-123 -> "Đang giao" (SHIPPING)
 * - ORD-003 -> "Đã giao" (DELIVERED)
 * - ORD-999 -> Đơn hàng không tồn tại
 */
export const DEMO_ORDERS = {
  'ORD-001': {
    id: 1,
    orderCode: 'ORD-001',
    status: 'PROCESSING',
    statusLabel: 'Đang xử lý',
    totalAmount: 489000,
    recipientName: 'Đỗ Hoàng Anh',
    recipientPhone: '0987 654 321',
    shippingAddress: 'Tòa nhà FPT, Duy Tân, Cầu Giấy, Hà Nội',
    updatedAt: new Date(Date.now() - 3600000 * 4).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 12).toISOString(),
    items: [
      { productName: 'Áo Polo Thể Thao Rikkei CoolMax', quantity: 1, unitPrice: 249000, subtotal: 249000 },
      { productName: 'Quần Short Kaki Co Giãn 4 Chiều', quantity: 1, unitPrice: 240000, subtotal: 240000 },
    ],
    note: 'Đơn hàng đang được kho đóng gói và chuẩn bị bàn giao cho bưu tá.',
  },
  'ORD-002': {
    id: 2,
    orderCode: 'ORD-002',
    status: 'SHIPPING',
    statusLabel: 'Đang giao',
    totalAmount: 850000,
    recipientName: 'Vũ Đức Huy Hoàng',
    recipientPhone: '0912 345 678',
    shippingAddress: 'Số 18 Tam Trinh, Mai Động, Hoàng Mai, Hà Nội',
    updatedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 28).toISOString(),
    items: [
      { productName: 'Giày Sneaker Rikkei Urban Flow V2', quantity: 1, unitPrice: 850000, subtotal: 850000 },
    ],
    note: 'Đơn hàng đang trên đường giao bởi GHN Express. Bưu tá liên hệ trước khi giao.',
  },
  'ORD-123': {
    id: 123,
    orderCode: 'ORD-123',
    status: 'SHIPPING',
    statusLabel: 'Đang giao',
    totalAmount: 720000,
    recipientName: 'Khách hàng Rikkei Store',
    recipientPhone: '0936 888 999',
    shippingAddress: '25 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội',
    updatedAt: new Date(Date.now() - 3600000 * 1.5).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 30).toISOString(),
    items: [
      { productName: 'Áo Khoác Bomber Chống Gió Cao Cấp', quantity: 1, unitPrice: 520000, subtotal: 520000 },
      { productName: 'Mũ Lưỡi Trai Rikkei Classic', quantity: 1, unitPrice: 200000, subtotal: 200000 },
    ],
    note: 'Đơn hàng đang trên lộ trình giao bởi Viettel Post. Dự kiến sẽ được giao tới bạn trong hôm nay.',
  },
  'ORD-003': {
    id: 3,
    orderCode: 'ORD-003',
    status: 'DELIVERED',
    statusLabel: 'Đã giao',
    totalAmount: 1250000,
    recipientName: 'Nguyễn Văn An',
    recipientPhone: '0977 112 233',
    shippingAddress: 'Landmark 81, Bình Thạnh, TP. Hồ Chí Minh',
    updatedAt: new Date(Date.now() - 3600000 * 26).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 72).toISOString(),
    items: [
      { productName: 'Combo 3 Áo Thun Cotton Compact', quantity: 1, unitPrice: 650000, subtotal: 650000 },
      { productName: 'Balo Du Lịch Chống Nước Smartpack', quantity: 1, unitPrice: 600000, subtotal: 600000 },
    ],
    note: 'Đơn hàng đã được giao thành công và có chữ ký xác nhận của người nhận.',
  },
}

/**
 * Sinh đối tượng đơn hàng hợp lệ dựa trên mã tra cứu (deterministic fallback).
 */
export const getDeterministicOrder = (orderCode) => {
  if (!orderCode) return null
  const normalized = orderCode.toUpperCase().replace(/\s+/g, '')

  // Kiểm tra mã demo cố định
  if (DEMO_ORDERS[normalized]) {
    return DEMO_ORDERS[normalized]
  }

  // Trường hợp kiểm thử mã không tồn tại ORD-999
  if (normalized === 'ORD-999' || normalized === '999') {
    return { notFound: true, orderCode: 'ORD-999' }
  }

  // Trường hợp khách nhập mã tùy ý (ví dụ ORD-102, ORD-2026, #456)
  const numMatches = normalized.match(/\d+/)
  const num = numMatches ? parseInt(numMatches[0], 10) : 123

  // Phân bổ trạng thái xoay vòng theo 3 trạng thái Trello yêu cầu
  let status = 'PROCESSING'
  let statusLabel = 'Đang xử lý'

  const mod = num % 3
  if (mod === 0) {
    status = 'PROCESSING'
    statusLabel = 'Đang xử lý'
  } else if (mod === 1) {
    status = 'SHIPPING'
    statusLabel = 'Đang giao'
  } else {
    status = 'DELIVERED'
    statusLabel = 'Đã giao'
  }

  return {
    id: num,
    orderCode: normalized.startsWith('ORD-') ? normalized : `ORD-${normalized}`,
    status,
    statusLabel,
    totalAmount: (num * 10000 + 350000) % 1500000 + 150000,
    recipientName: 'Khách hàng Rikkei',
    recipientPhone: '0988 *** ***',
    shippingAddress: 'Địa chỉ giao hàng đã đăng ký',
    updatedAt: new Date().toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 24).toISOString(),
    items: [
      { productName: 'Sản phẩm thời trang Rikkei Store', quantity: 1, unitPrice: 350000, subtotal: 350000 },
    ],
    note: `Đơn hàng đang ở trạng thái ${statusLabel}.`,
  }
}

/**
 * Xây dựng câu trả lời tự nhiên của AI Chatbot theo mã đơn và trạng thái tiếng Việt chuẩn.
 */
export const buildOrderStatusReply = (order) => {
  if (!order) return ''

  if (order.notFound) {
    return `Rikkei AI đã kiểm tra toàn bộ hệ thống nhưng không tìm thấy đơn hàng có mã **${order.orderCode}**.\n\nBạn vui lòng kiểm tra lại mã đơn hàng chính xác trên email xác nhận hoặc tại mục **Lịch sử đơn hàng**. Nếu cần hỗ trợ thêm, bạn có thể gọi hotline **1900-RIKKEI** để gặp tổng đài viên nhé!`
  }

  const code = order.orderCode || (order.id ? `ORD-${order.id}` : 'ORD-xxx')
  const statusLabel = order.statusLabel || (order.status === 'DELIVERED' || order.status === 'COMPLETED' ? 'Đã giao' : order.status === 'SHIPPING' ? 'Đang giao' : 'Đang xử lý')

  if (statusLabel === 'Đang xử lý') {
    return `Chào bạn! Tôi đã kiểm tra thông tin đơn hàng **${code}** của bạn.\n\nHiện tại đơn hàng đang ở trạng thái **Đang xử lý** 📦. Kho Rikkei Store đang tiến hành kiểm tra chất lượng và đóng gói sản phẩm. Đơn hàng sẽ sớm được bàn giao cho đơn vị vận chuyển!`
  }

  if (statusLabel === 'Đang giao') {
    return `Chào bạn! Đơn hàng **${code}** của bạn đang ở trạng thái **Đang giao** 🚚.\n\nKiện hàng đã rời kho và đang trên hành trình vận chuyển tới địa chỉ nhận hàng của bạn. Dự kiến đơn sẽ được giao trong 1-2 ngày tới. Bạn nhớ chú ý điện thoại để bưu tá liên hệ giao hàng nhé!`
  }

  if (statusLabel === 'Đã giao') {
    return `Chào bạn! Đơn hàng **${code}** của bạn đã ở trạng thái **Đã giao** ✅.\n\nKiện hàng đã được giao thành công tới địa chỉ nhận của bạn. Cảm ơn bạn đã luôn tin tưởng và ủng hộ Rikkei Store! Nếu sản phẩm có bất kỳ vấn đề gì, bạn có thể yêu cầu đổi trả miễn phí trong 7 ngày.`
  }

  if (statusLabel === 'Đã hủy') {
    return `Chào bạn! Đơn hàng **${code}** của bạn hiện đã ở trạng thái **Đã hủy** ❌.\n\nNếu đơn hàng đã thanh toán trước đó, hệ thống sẽ tiến hành hoàn tiền về phương thức ban đầu theo quy định hoàn tiền của Rikkei Store.`
  }

  return `Chào bạn! Đơn hàng **${code}** của bạn hiện có trạng thái là **${statusLabel}**.`
}

/**
 * Trợ lý hướng dẫn khi người dùng hỏi chung chung về đơn hàng nhưng chưa cung cấp mã.
 */
export const buildGeneralOrderInquiryReply = () => {
  return `Dạ, Rikkei AI có thể hỗ trợ bạn tra cứu hành trình và trạng thái đơn hàng ngay lập tức! 📦\n\nBạn vui lòng gửi kèm **Mã đơn hàng** (ví dụ: \`ORD-123\`, \`ORD-001\`, \`#102\`) hoặc câu hỏi như:\n👉 *"Đơn hàng ORD-123 của tôi đến đâu rồi?"*\n\nHoặc bạn cũng có thể bấm vào gợi ý nhanh bên dưới để kiểm tra thử nhé!`
}

export default {
  normalizeOrderStatus,
  detectOrderQuery,
  DEMO_ORDERS,
  getDeterministicOrder,
  buildOrderStatusReply,
  buildGeneralOrderInquiryReply,
}
