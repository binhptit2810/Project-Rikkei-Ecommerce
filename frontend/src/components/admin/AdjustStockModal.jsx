import React, { useState, useEffect, useMemo } from 'react'
import {
  X,
  PackagePlus,
  PackageMinus,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  Sparkles,
  Loader2,
} from 'lucide-react'
import inventoryService from '../../services/inventoryService'

const DEFAULT_FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="%23a1a1aa" stroke-width="1.5"><rect width="18" height="18" x="3" y="3" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>'

/**
 * Modal Điều chỉnh Tồn kho Thủ công (AdjustStockModal).
 * Phục vụ Checklist #3: Nhập kho / Xuất kho kèm lý do, validation nghiêm ngặt và tính toán real-time.
 *
 * @param {Object} props
 * @param {boolean} props.isOpen - Trạng thái mở modal
 * @param {Function} props.onClose - Hàm đóng modal
 * @param {Object} props.product - Thông tin sản phẩm được chọn điều chỉnh
 * @param {Function} props.onSuccess - Callback khi điều chỉnh thành công
 * @param {Function} props.onError - Callback khi xảy ra lỗi
 */
export const AdjustStockModal = ({
  isOpen,
  onClose,
  product,
  onSuccess,
  onError,
}) => {
  // Chế độ thao tác: 'IMPORT' (Nhập kho) hoặc 'EXPORT' (Xuất kho)
  const [mode, setMode] = useState('IMPORT')
  // Số lượng điều chỉnh (dạng chuỗi để dễ gõ trong input)
  const [quantity, setQuantity] = useState('1')
  // Ghi chú / Lý do điều chỉnh
  const [note, setNote] = useState('')
  // Trạng thái đang gửi API
  const [isSubmitting, setIsSubmitting] = useState(false)
  // Lỗi validation cục bộ
  const [validationError, setValidationError] = useState('')

  // Gợi ý lý do nhanh tùy theo chế độ
  const quickReasons = useMemo(() => {
    if (mode === 'IMPORT') {
      return [
        'Nhập thêm hàng từ nhà cung cấp',
        'Kiểm kê kho định kỳ (phát hiện dư)',
        'Nhập hàng hoàn trả từ khách hàng',
        'Nhập bổ sung từ kho tổng',
      ]
    }
    return [
      'Xuất hủy hàng lỗi / hư hỏng',
      'Kiểm kê kho định kỳ (hao hụt thất thoát)',
      'Xuất hàng mẫu trưng bày / nội bộ',
      'Xuất chuyển sang kho chi nhánh',
    ]
  }, [mode])

  // Reset form khi mở modal hoặc đổi sản phẩm
  useEffect(() => {
    if (isOpen && product) {
      setMode('IMPORT')
      setQuantity('1')
      setNote('')
      setValidationError('')
      setIsSubmitting(false)
    }
  }, [isOpen, product])

  // Xử lý phím ESC để đóng modal
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        onClose()
      }
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isSubmitting, onClose])

  if (!isOpen || !product) return null

  const currentStock = Number(product.stockQuantity ?? 0)
  const parsedQty = parseInt(quantity, 10)
  const validQty = !isNaN(parsedQty) && parsedQty > 0 ? parsedQty : 0

  // Tính toán tồn kho sau điều chỉnh
  const projectedStock =
    mode === 'IMPORT' ? currentStock + validQty : currentStock - validQty

  // Kiểm tra tính hợp lệ
  const isExportExceedsStock = mode === 'EXPORT' && validQty > currentStock
  const isLowStockWarning = projectedStock >= 0 && projectedStock <= 5
  const isOutOfStockProjected = projectedStock === 0

  // Thay đổi số lượng qua nút cộng nhanh
  const handleQuickAdd = (amount) => {
    const currentVal = parseInt(quantity, 10) || 0
    const nextVal = currentVal + amount
    setQuantity(String(nextVal))
    setValidationError('')
  }

  // Submit gửi API
  const handleSubmit = async (e) => {
    e.preventDefault()
    setValidationError('')

    if (!validQty || validQty <= 0) {
      setValidationError('Vui lòng nhập số lượng điều chỉnh lớn hơn 0.')
      return
    }

    if (isExportExceedsStock) {
      setValidationError(
        `Không thể xuất kho vượt quá số lượng tồn hiện có (${currentStock} sản phẩm).`
      )
      return
    }

    const trimmedNote = note.trim()
    if (!trimmedNote) {
      setValidationError('Vui lòng điền hoặc chọn lý do điều chỉnh kho (bắt buộc).')
      return
    }

    if (trimmedNote.length > 255) {
      setValidationError('Lý do điều chỉnh không được vượt quá 255 ký tự.')
      return
    }

    setIsSubmitting(true)
    try {
      const changeQuantity = mode === 'IMPORT' ? validQty : -validQty
      const transactionType =
        mode === 'IMPORT' ? 'MANUAL_IMPORT' : 'MANUAL_EXPORT'

      const response = await inventoryService.adjustStock(product.id, {
        changeQuantity,
        type: transactionType,
        reason: trimmedNote,
        note: trimmedNote,
      })

      const successMsg =
        response?.message ||
        `Đã ${mode === 'IMPORT' ? 'nhập' : 'xuất'} thành công ${validQty} sản phẩm "${product.name}". Tồn mới: ${projectedStock}.`

      onSuccess(successMsg)
      onClose()
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi điều chỉnh tồn kho. Vui lòng thử lại.'
      setValidationError(errorMsg)
      if (onError) onError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/60 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSubmitting) onClose()
      }}
    >
      <div
        className="bg-white rounded-2xl border border-zinc-200 shadow-2xl max-w-lg w-full overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[92vh]"
        role="dialog"
        aria-modal="true"
      >
        {/* Header Modal */}
        <div className="px-6 py-4.5 border-b border-zinc-200 flex items-center justify-between bg-zinc-50/60">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-zinc-900 text-zinc-50 flex items-center justify-center shadow-xs">
              {mode === 'IMPORT' ? (
                <PackagePlus className="w-5 h-5 text-emerald-400" />
              ) : (
                <PackageMinus className="w-5 h-5 text-rose-400" />
              )}
            </div>
            <div>
              <h3 className="text-base font-bold text-zinc-900 tracking-tight">
                Điều chỉnh Tồn kho Thủ công
              </h3>
              <p className="text-xs text-zinc-500 font-mono">
                Mã sản phẩm: #{product.id}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="p-1.5 rounded-lg text-zinc-400 hover:text-zinc-700 hover:bg-zinc-200/60 transition-colors disabled:opacity-50 cursor-pointer"
            title="Đóng modal (ESC)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Nội dung form cuộn mượt */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5 overflow-y-auto">
          {/* Tóm tắt thông tin sản phẩm */}
          <div className="flex items-center gap-3.5 p-3 rounded-xl bg-zinc-50 border border-zinc-200/80">
            <img
              src={product.imageUrl || DEFAULT_FALLBACK_IMAGE}
              alt={product.name}
              onError={(e) => {
                e.currentTarget.onerror = null
                e.currentTarget.src = DEFAULT_FALLBACK_IMAGE
              }}
              className="w-12 h-12 rounded-lg object-cover bg-white border border-zinc-200 shrink-0"
            />
            <div className="flex-1 min-w-0">
              <h4 className="text-sm font-semibold text-zinc-900 truncate">
                {product.name}
              </h4>
              <div className="flex items-center gap-3 mt-1 text-xs">
                <span className="text-zinc-500">
                  Tồn hiện tại:{' '}
                  <strong className="text-zinc-900 font-bold tabular-nums">
                    {currentStock}
                  </strong>{' '}
                  đơn vị
                </span>
                {currentStock <= 5 && (
                  <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-rose-700 bg-rose-50 border border-rose-200 px-1.5 py-0.2 rounded">
                    <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-pulse"></span>
                    {currentStock === 0 ? 'Hết hàng' : 'Tồn thấp'}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Chọn loại thao tác: Nhập kho vs Xuất kho (2 Toggle Buttons lớn) */}
          <div>
            <label className="block text-xs font-semibold text-zinc-700 uppercase tracking-wider mb-2">
              Chọn Loại Thao Tác
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => {
                  setMode('IMPORT')
                  setValidationError('')
                }}
                className={`flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-semibold transition-all border cursor-pointer ${
                  mode === 'IMPORT'
                    ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm shadow-emerald-600/20 ring-2 ring-emerald-500/20'
                    : 'bg-zinc-50 text-zinc-600 border-zinc-200 hover:bg-zinc-100 hover:text-zinc-900'
                }`}
              >
                <PackagePlus className="w-4 h-4" />
                <span>Nhập kho (+ Tăng tồn)</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setMode('EXPORT')
                  setValidationError('')
                }}
                className={`flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs sm:text-sm font-semibold transition-all border cursor-pointer ${
                  mode === 'EXPORT'
                    ? 'bg-rose-600 text-white border-rose-600 shadow-sm shadow-rose-600/20 ring-2 ring-rose-500/20'
                    : 'bg-zinc-50 text-zinc-600 border-zinc-200 hover:bg-zinc-100 hover:text-zinc-900'
                }`}
              >
                <PackageMinus className="w-4 h-4" />
                <span>Xuất kho (- Giảm tồn)</span>
              </button>
            </div>
          </div>

          {/* Ô nhập số lượng & Nút cộng nhanh */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label
                htmlFor="adjustQuantity"
                className="text-xs font-semibold text-zinc-700 uppercase tracking-wider"
              >
                Số lượng {mode === 'IMPORT' ? 'nhập thêm' : 'xuất đi'}{' '}
                <span className="text-rose-500">*</span>
              </label>
              <span className="text-[11px] text-zinc-500 font-mono">
                Tối thiểu: 1
              </span>
            </div>

            <div className="relative">
              <input
                id="adjustQuantity"
                type="number"
                min="1"
                step="1"
                value={quantity}
                onChange={(e) => {
                  setQuantity(e.target.value)
                  setValidationError('')
                }}
                placeholder="Nhập số lượng..."
                className="w-full px-3.5 py-2.5 text-base font-bold tabular-nums text-zinc-900 bg-white border border-zinc-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all placeholder:font-normal placeholder:text-zinc-400"
              />
              <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-semibold text-zinc-500 pointer-events-none">
                Sản phẩm
              </span>
            </div>

            {/* Quick chips cộng nhanh */}
            <div className="flex items-center gap-2 mt-2 pt-1 flex-wrap">
              <span className="text-[11px] text-zinc-400 font-medium">
                Cộng nhanh:
              </span>
              {[5, 10, 20, 50, 100].map((amt) => (
                <button
                  key={amt}
                  type="button"
                  onClick={() => handleQuickAdd(amt)}
                  className="px-2 py-0.5 text-xs font-semibold font-mono rounded-md bg-zinc-100 hover:bg-zinc-200 text-zinc-700 border border-zinc-200 transition-colors cursor-pointer"
                >
                  +{amt}
                </button>
              ))}
              <button
                type="button"
                onClick={() => setQuantity('1')}
                className="px-2 py-0.5 text-xs font-medium rounded-md text-zinc-500 hover:text-zinc-800 transition-colors cursor-pointer ml-auto"
              >
                Đặt lại (1)
              </button>
            </div>
          </div>

          {/* Khung Preview Tính toán Thời gian thực (Real-time Stock Preview) */}
          <div className="p-4 rounded-xl bg-zinc-900 text-zinc-50 border border-zinc-800 space-y-3">
            <div className="flex items-center justify-between text-xs text-zinc-400 font-medium">
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                Dự báo Tồn kho Thời gian thực
              </span>
              <span className="font-mono text-[11px]">Công thức chuẩn</span>
            </div>

            <div className="grid grid-cols-3 gap-2 items-center text-center">
              {/* Cột 1: Tồn hiện tại */}
              <div className="p-2.5 rounded-lg bg-zinc-800/80 border border-zinc-700/60">
                <div className="text-[10px] text-zinc-400 font-medium uppercase tracking-wider">
                  Hiện tại
                </div>
                <div className="text-base sm:text-lg font-bold text-zinc-100 tabular-nums mt-0.5">
                  {currentStock}
                </div>
              </div>

              {/* Cột 2: Biến động */}
              <div className="p-2.5 rounded-lg bg-zinc-800/80 border border-zinc-700/60">
                <div className="text-[10px] text-zinc-400 font-medium uppercase tracking-wider">
                  {mode === 'IMPORT' ? 'Nhập thêm' : 'Xuất bớt'}
                </div>
                <div
                  className={`text-base sm:text-lg font-bold tabular-nums mt-0.5 ${
                    mode === 'IMPORT' ? 'text-emerald-400' : 'text-rose-400'
                  }`}
                >
                  {mode === 'IMPORT' ? `+${validQty}` : `-${validQty}`}
                </div>
              </div>

              {/* Cột 3: Tồn sau điều chỉnh */}
              <div
                className={`p-2.5 rounded-lg border ${
                  isExportExceedsStock
                    ? 'bg-rose-950/40 border-rose-700 text-rose-300'
                    : isLowStockWarning
                    ? 'bg-amber-950/40 border-amber-600 text-amber-300'
                    : 'bg-emerald-950/40 border-emerald-600 text-emerald-300'
                }`}
              >
                <div className="text-[10px] font-medium uppercase tracking-wider opacity-80">
                  Dự kiến mới
                </div>
                <div className="text-base sm:text-lg font-bold tabular-nums mt-0.5">
                  {projectedStock}
                </div>
              </div>
            </div>

            {/* Cảnh báo ngữ cảnh theo tồn kho mới */}
            {isExportExceedsStock && (
              <div className="flex items-start gap-2 p-2.5 rounded-lg bg-rose-900/40 border border-rose-700/60 text-xs text-rose-200">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <span>
                  <strong>Không thể xuất kho:</strong> Số lượng xuất ({validQty}) vượt
                  quá tồn kho hiện tại ({currentStock}). Tồn kho không được âm.
                </span>
              </div>
            )}

            {!isExportExceedsStock && isLowStockWarning && (
              <div className="flex items-start gap-2 p-2.5 rounded-lg bg-amber-900/40 border border-amber-700/60 text-xs text-amber-200">
                <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                <span>
                  {isOutOfStockProjected ? (
                    <strong>Nguy hiểm:</strong>
                  ) : (
                    <strong>Cảnh báo tồn thấp:</strong>
                  )}{' '}
                  Sau khi điều chỉnh, sản phẩm sẽ{' '}
                  {isOutOfStockProjected
                    ? 'hết hàng (0 sản phẩm)'
                    : `rơi vào ngưỡng cảnh báo sắp hết hàng (còn ${projectedStock} <= 5)`}
                  .
                </span>
              </div>
            )}

            {!isExportExceedsStock && !isLowStockWarning && (
              <div className="flex items-center gap-2 text-xs text-emerald-400/90 pt-0.5">
                <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                <span>Mức tồn kho an toàn &gt; 5 sản phẩm.</span>
              </div>
            )}
          </div>

          {/* Ô nhập Lý do điều chỉnh (Bắt buộc) kèm Chips gợi ý */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label
                htmlFor="adjustReason"
                className="text-xs font-semibold text-zinc-700 uppercase tracking-wider"
              >
                Lý do điều chỉnh kho <span className="text-rose-500">*</span>
              </label>
              <span className="text-[11px] text-zinc-400 font-mono">
                {note.length}/255
              </span>
            </div>

            <textarea
              id="adjustReason"
              rows={3}
              value={note}
              onChange={(e) => {
                setNote(e.target.value)
                setValidationError('')
              }}
              placeholder={`Nhập lý do chi tiết (ví dụ: ${
                mode === 'IMPORT'
                  ? 'Nhập lô hàng mới từ NCC...'
                  : 'Xuất hủy hàng móp méo khi vận chuyển...'
              })`}
              maxLength={255}
              className="w-full px-3.5 py-2.5 text-xs sm:text-sm text-zinc-900 bg-white border border-zinc-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all placeholder:text-zinc-400 resize-none"
            />

            {/* Chips gợi ý lý do */}
            <div className="mt-2 space-y-1">
              <span className="text-[11px] text-zinc-400 font-medium">
                Gợi ý lý do nhanh (bấm để chọn):
              </span>
              <div className="flex flex-wrap gap-1.5 pt-1">
                {quickReasons.map((reasonText, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setNote(reasonText)
                      setValidationError('')
                    }}
                    className="px-2.5 py-1 text-xs rounded-lg bg-zinc-100 hover:bg-zinc-200 text-zinc-700 border border-zinc-200/80 transition-colors text-left cursor-pointer"
                  >
                    {reasonText}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Báo lỗi validation nếu có */}
          {validationError && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium flex items-start gap-2 animate-in fade-in duration-150">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{validationError}</span>
            </div>
          )}

          {/* Nút thao tác Footer */}
          <div className="pt-3 border-t border-zinc-200 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs sm:text-sm font-semibold rounded-xl text-zinc-700 bg-zinc-100 hover:bg-zinc-200 transition-colors disabled:opacity-50 cursor-pointer"
            >
              Hủy bỏ
            </button>

            <button
              type="submit"
              disabled={
                isSubmitting ||
                !validQty ||
                validQty <= 0 ||
                isExportExceedsStock ||
                !note.trim()
              }
              className={`inline-flex items-center gap-2 px-5 py-2 text-xs sm:text-sm font-semibold rounded-xl text-white shadow-xs transition-all disabled:opacity-50 disabled:pointer-events-none cursor-pointer ${
                mode === 'IMPORT'
                  ? 'bg-emerald-600 hover:bg-emerald-700 active:scale-[0.99]'
                  : 'bg-rose-600 hover:bg-rose-700 active:scale-[0.99]'
              }`}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <span>
                    Xác nhận {mode === 'IMPORT' ? 'Nhập kho' : 'Xuất kho'}
                  </span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AdjustStockModal
