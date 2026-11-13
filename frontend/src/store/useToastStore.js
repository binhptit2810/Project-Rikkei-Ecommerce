import { create } from 'zustand'

/**
 * Global Toast Notification Store
 * Hiển thị ô thông báo ở góc phải màn hình và tự động biến mất sau 2s theo yêu cầu.
 */
export const useToastStore = create((set, get) => ({
  toast: null,
  timeoutId: null,

  /**
   * Hiển thị thông báo
   * @param {Object} options
   * @param {string} options.title - Tiêu đề thông báo (vd: "Đã thêm vào giỏ hàng!", "Đặt hàng thành công!")
   * @param {string} options.message - Nội dung chi tiết
   * @param {('success'|'error'|'info'|'cart')} [options.type='success']
   * @param {number} [options.duration=2000] - Mặc định 2 giây (2000ms)
   * @param {{ to: string, label: string }} [options.link] - Đường dẫn xem chi tiết (vd: Xem giỏ hàng)
   */
  showToast: ({ title, message, type = 'success', duration = 2000, link = null }) => {
    // Xóa bộ đếm thời gian cũ nếu đang có thông báo trước đó
    const prevTimeout = get().timeoutId
    if (prevTimeout) {
      clearTimeout(prevTimeout)
    }

    const id = Date.now()
    const newTimeoutId = setTimeout(() => {
      set({ toast: null, timeoutId: null })
    }, duration)

    set({
      toast: {
        id,
        title,
        message,
        type,
        duration,
        link,
        createdAt: Date.now(),
      },
      timeoutId: newTimeoutId,
    })
  },

  hideToast: () => {
    const prevTimeout = get().timeoutId
    if (prevTimeout) {
      clearTimeout(prevTimeout)
    }
    set({ toast: null, timeoutId: null })
  },
}))

/**
 * Helper hàm tiện ích gọi nhanh từ bất kỳ đâu
 */
export const toast = {
  success: (title, message, link = null, duration = 2000) => {
    useToastStore.getState().showToast({
      title,
      message,
      type: 'success',
      duration,
      link,
    })
  },
  cart: (title, message, link = { to: '/cart', label: 'Xem giỏ hàng →' }, duration = 2000) => {
    useToastStore.getState().showToast({
      title,
      message,
      type: 'cart',
      duration,
      link,
    })
  },
  error: (title, message, duration = 2000) => {
    useToastStore.getState().showToast({
      title,
      message,
      type: 'error',
      duration,
    })
  },
  hide: () => {
    useToastStore.getState().hideToast()
  },
}

export default useToastStore

