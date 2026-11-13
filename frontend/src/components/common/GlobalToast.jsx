import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { CheckCircle2, ShoppingCart, AlertCircle, X, ArrowRight, PackageCheck } from 'lucide-react'
import { useToastStore } from '../../store/useToastStore'

/**
 * Ô thông báo nổi ở góc bên phải màn hình (Right-side Toast Notification)
 * - Tự động hiển thị khi đặt hàng thành công hoặc thêm vào giỏ hàng thành công
 * - Tự động biến mất sau đúng 2 giây (2000ms)
 * - Kèm thanh tiến trình (progress bar) 2s mượt mà
 */
export const GlobalToast = () => {
  const { toast, hideToast } = useToastStore()
  const [progress, setProgress] = useState(100)

  useEffect(() => {
    if (!toast) {
      setProgress(100)
      return
    }

    setProgress(100)
    const duration = toast.duration || 2000
    const startTime = Date.now()

    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime
      const remaining = Math.max(0, 100 - (elapsed / duration) * 100)
      setProgress(remaining)
      if (elapsed >= duration) {
        clearInterval(interval)
      }
    }, 25)

    return () => clearInterval(interval)
  }, [toast])

  return (
    <div className="fixed top-20 right-4 sm:top-24 sm:right-6 z-50 pointer-events-none max-w-sm w-full">
      <AnimatePresence>
        {toast && (
          <motion.div
            key={toast.id}
            initial={{ opacity: 0, x: 50, scale: 0.95 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 40, scale: 0.95, transition: { duration: 0.2 } }}
            transition={{ type: 'spring', stiffness: 450, damping: 32 }}
            className="pointer-events-auto relative overflow-hidden rounded-2xl bg-white/95 backdrop-blur-md border border-slate-200/90 shadow-2xl shadow-slate-900/15 p-4 sm:p-4.5"
          >
            <div className="flex items-start gap-3.5">
              {/* Icon tương ứng với loại thông báo */}
              {toast.type === 'cart' ? (
                <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 border border-blue-200/80 flex items-center justify-center shrink-0 shadow-xs">
                  <ShoppingCart className="w-5 h-5" />
                </div>
              ) : toast.type === 'error' ? (
                <div className="w-10 h-10 rounded-xl bg-red-50 text-red-600 border border-red-200/80 flex items-center justify-center shrink-0 shadow-xs">
                  <AlertCircle className="w-5 h-5" />
                </div>
              ) : toast.title?.includes('Đặt hàng') || toast.title?.includes('đơn hàng') ? (
                <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-200/80 flex items-center justify-center shrink-0 shadow-xs">
                  <PackageCheck className="w-5 h-5" />
                </div>
              ) : (
                <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-200/80 flex items-center justify-center shrink-0 shadow-xs">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
              )}

              {/* Nội dung thông báo */}
              <div className="flex-1 min-w-0 pr-1">
                <h4 className="text-sm font-bold text-slate-900 leading-snug">
                  {toast.title}
                </h4>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                  {toast.message}
                </p>

                {/* Đường dẫn phụ (nếu có, ví dụ: Xem giỏ hàng) */}
                {toast.link && (
                  <div className="mt-2">
                    <Link
                      to={toast.link.to}
                      onClick={hideToast}
                      className="inline-flex items-center gap-1 text-xs font-bold text-blue-600 hover:text-blue-700 transition-colors"
                    >
                      <span>{toast.link.label}</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                )}
              </div>

              {/* Nút đóng thông báo nhanh */}
              <button
                type="button"
                onClick={hideToast}
                className="text-slate-400 hover:text-slate-700 p-1 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer shrink-0 -mr-1 -mt-1"
                title="Đóng thông báo"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Thanh tiến trình tự động biến mất sau 2s */}
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-slate-100">
              <div
                className={`h-full transition-all duration-75 ease-linear ${
                  toast.type === 'error'
                    ? 'bg-red-500'
                    : toast.type === 'cart'
                    ? 'bg-blue-600'
                    : 'bg-emerald-500'
                }`}
                style={{ width: `${progress}%` }}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default GlobalToast

