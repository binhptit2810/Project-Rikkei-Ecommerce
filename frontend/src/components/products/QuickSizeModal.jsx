import React, { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { ShoppingCart, X, Minus, Plus, AlertCircle, Check, ShoppingBag } from 'lucide-react'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * QuickSizeModal: Modal chọn biến thể kích cỡ nhanh theo chuẩn TechNova.
 * Sử dụng React createPortal gắn trực tiếp vào document.body để:
 * - Tránh hoàn toàn lỗi CSS Containing Block do `transform` của thẻ cha (ProductCard whileHover).
 * - Tránh lỗi overflow-hidden cắt modal hoặc flickering khi di chuột qua lại giữa Card và Modal.
 * - Cô lập sự kiện chuột và bàn phím, đảm bảo trải nghiệm mượt mà, không nhấp nháy.
 */
export const QuickSizeModal = ({
  isOpen,
  onClose,
  product,
  variants = [],
  onConfirmAddToCart,
}) => {
  const [selectedSize, setSelectedSize] = useState(null)
  const [selectedVariant, setSelectedVariant] = useState(null)
  const [quantity, setQuantity] = useState(1)
  const [errorMsg, setErrorMsg] = useState(null)

  // Khởi tạo lựa chọn size khi mở modal
  useEffect(() => {
    if (isOpen && Array.isArray(variants) && variants.length > 0) {
      setErrorMsg(null)
      setQuantity(1)
      // Tự động chọn size đầu tiên còn hàng (nếu có)
      const firstAvailable = variants.find((v) => Number(v.stockQuantity || 0) > 0)
      if (firstAvailable) {
        setSelectedSize(firstAvailable.size)
        setSelectedVariant(firstAvailable)
      } else {
        setSelectedSize(null)
        setSelectedVariant(null)
      }
    } else {
      setSelectedSize(null)
      setSelectedVariant(null)
      setQuantity(1)
      setErrorMsg(null)
    }
  }, [isOpen, variants])

  // Khóa cuộn trang khi modal mở (Body scroll locking)
  useEffect(() => {
    if (isOpen && typeof document !== 'undefined') {
      const originalOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      return () => {
        document.body.style.overflow = originalOverflow
      }
    }
  }, [isOpen])

  // Lắng nghe phím Escape để đóng modal
  useEffect(() => {
    if (!isOpen) return
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (typeof document === 'undefined') return null

  const availableStock = selectedVariant ? Number(selectedVariant.stockQuantity || 0) : 0
  const isOutOfStock = availableStock <= 0

  const handleSelectSize = (v) => {
    if (Number(v.stockQuantity || 0) <= 0) return
    setSelectedSize(v.size)
    setSelectedVariant(v)
    setErrorMsg(null)
    if (quantity > Number(v.stockQuantity || 0)) {
      setQuantity(Math.max(1, Number(v.stockQuantity || 0)))
    }
  }

  const handleConfirm = () => {
    if (!selectedSize || !selectedVariant) {
      setErrorMsg('Vui lòng chọn kích cỡ sản phẩm.')
      return
    }
    if (availableStock <= 0) {
      setErrorMsg('Kích cỡ này hiện đã hết hàng.')
      return
    }
    if (quantity > availableStock) {
      setErrorMsg(`Số lượng chọn vượt quá tồn kho hiện có (${availableStock}).`)
      return
    }

    onConfirmAddToCart({
      product,
      quantity,
      selectedSize,
      variant: selectedVariant,
    })
    onClose()
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  return createPortal(
    <AnimatePresence>
      {isOpen && product && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto"
          onClick={(e) => e.stopPropagation()}
          onMouseEnter={(e) => e.stopPropagation()}
          onMouseLeave={(e) => e.stopPropagation()}
        >
          {/* Backdrop mờ nền */}
          <motion.div
            key="quick-size-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={onClose}
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
          />

          {/* Hộp thoại Modal */}
          <motion.div
            key="quick-size-dialog"
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl border border-slate-200/90 overflow-hidden z-10"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-600 animate-pulse"></span>
                <h3 className="text-sm sm:text-base font-bold text-slate-900">
                  Chọn kích cỡ sản phẩm
                </h3>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition-colors cursor-pointer"
                aria-label="Đóng"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-6 space-y-5">
              {/* Thông tin tóm tắt sản phẩm */}
              <div className="flex items-center gap-4 p-3 bg-slate-50/80 rounded-2xl border border-slate-200/60">
                <div className="w-16 h-16 rounded-xl bg-white border border-slate-200/70 p-2 flex items-center justify-center shrink-0 overflow-hidden">
                  {product.imageUrl ? (
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      onError={(e) => {
                        e.target.onerror = null
                        e.target.src = FALLBACK_IMAGE
                      }}
                      className="w-full h-full object-contain mix-blend-multiply"
                    />
                  ) : (
                    <ShoppingBag className="w-6 h-6 text-slate-300" />
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-slate-400 truncate">
                    {product.categoryName || 'Rikkei Store'}
                  </p>
                  <h4 className="font-bold text-slate-900 text-sm truncate leading-snug">
                    {product.name}
                  </h4>
                  <p className="text-sm font-extrabold text-blue-600 mt-0.5">
                    {formatPrice(product.price)}
                  </p>
                </div>
              </div>

              {/* Danh sách kích cỡ (Size Selector) */}
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                    <span>Kích thước (Size)</span>
                    {selectedSize ? (
                      <span className="text-blue-600 font-extrabold normal-case">
                        : {selectedSize}
                      </span>
                    ) : (
                      <span className="text-rose-500 font-medium normal-case">
                        (Vui lòng chọn)
                      </span>
                    )}
                  </span>
                  {selectedVariant && (
                    <span className="text-slate-500">
                      Tồn kho: <span className="font-bold text-slate-900">{availableStock}</span>
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-2.5 flex-wrap">
                  {Array.isArray(variants) && variants.map((v) => {
                    const isVariantOutOfStock = Number(v.stockQuantity || 0) <= 0
                    const isSelected = selectedSize === v.size

                    return (
                      <button
                        key={v.id || v.size}
                        type="button"
                        onClick={() => handleSelectSize(v)}
                        disabled={isVariantOutOfStock}
                        className={`min-w-[48px] h-11 px-3.5 rounded-xl border text-sm font-bold transition-all flex items-center justify-center gap-1 relative cursor-pointer ${
                          isSelected
                            ? 'border-blue-600 bg-blue-600 text-white shadow-sm ring-2 ring-blue-600/30'
                            : isVariantOutOfStock
                            ? 'border-slate-200 bg-slate-100 text-slate-400 opacity-40 cursor-not-allowed line-through'
                            : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50'
                        }`}
                        title={
                          isVariantOutOfStock
                            ? `Size ${v.size} (Hết hàng)`
                            : `Size ${v.size} (Còn ${v.stockQuantity} sản phẩm)`
                        }
                      >
                        <span>{v.size}</span>
                        {isSelected && <Check className="w-3.5 h-3.5 ml-0.5 stroke-[2.5]" />}
                        {isVariantOutOfStock && (
                          <span className="text-[9px] font-semibold text-rose-500 no-underline ml-0.5">
                            (Hết)
                          </span>
                        )}
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Bộ điều khiển số lượng (Quantity Selector) */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Số lượng
                </span>
                <div className="flex items-center border border-slate-200 rounded-full bg-slate-50 px-2 py-1 shadow-xs">
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                    disabled={quantity <= 1 || isOutOfStock}
                    className="w-7 h-7 rounded-full flex items-center justify-center text-slate-600 hover:bg-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Giảm số lượng"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="px-3 text-sm font-bold text-slate-900 select-none min-w-[32px] text-center tabular-nums">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.min(availableStock, prev + 1))}
                    disabled={isOutOfStock || quantity >= availableStock}
                    className="w-7 h-7 rounded-full flex items-center justify-center text-slate-600 hover:bg-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Tăng số lượng"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Thông báo lỗi hợp lệ (Validation Alert) */}
              {errorMsg && (
                <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-center gap-2 text-xs font-semibold animate-in fade-in">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>{errorMsg}</span>
                </div>
              )}
            </div>

            {/* Footer Actions */}
            <div className="flex items-center gap-3 px-6 py-4 bg-slate-50/70 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-3 px-4 border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 font-bold text-xs rounded-full transition-colors cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                disabled={isOutOfStock || !selectedSize}
                className="flex-2 py-3 px-6 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-full shadow-md shadow-blue-500/20 disabled:opacity-40 disabled:pointer-events-none transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2"
              >
                <ShoppingCart className="w-4 h-4" />
                <span>Thêm vào giỏ</span>
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  )
}

export default QuickSizeModal
