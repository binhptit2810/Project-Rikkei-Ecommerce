import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, HelpCircle, AlertCircle, X } from 'lucide-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import orderService from '../../services/orderService'

// Danh sách các lý do hủy đơn hàng gợi ý sẵn
export const PRESET_CANCEL_REASONS = [
  'Thay đổi địa chỉ nhận hàng',
  'Muốn thay đổi sản phẩm / số lượng',
  'Tìm thấy giá tốt hơn ở nơi khác',
  'Thay đổi phương thức thanh toán',
  'Đổi ý, không còn nhu cầu mua nữa',
  'Lý do khác',
]

/**
 * Component Modal hủy đơn hàng dùng chung cho cả OrderHistoryPage (danh sách)
 * và OrderDetailPage (chi tiết đơn hàng) để đảm bảo tính nhất quán nghiệp vụ & giao diện.
 *
 * @param {boolean} isOpen Trạng thái hiển thị modal
 * @param {Object} order Đơn hàng cần hủy (chứa id, orderCode, ...)
 * @param {Function} onClose Callback đóng modal
 * @param {Function} onSuccess Callback khi hủy đơn hàng thành công (updatedOrder, reason)
 * @param {Function} onError Callback khi hủy đơn hàng thất bại (errorMessage)
 */
export const CancelOrderModal = ({
  isOpen,
  order,
  onClose,
  onSuccess,
  onError,
}) => {
  const navigate = useNavigate()
  const [cancelReasonPreset, setCancelReasonPreset] = useState('')
  const [cancelReasonText, setCancelReasonText] = useState('')
  const [cancelReasonError, setCancelReasonError] = useState('')
  const [cancelling, setCancelling] = useState(false)

  // Reset form mỗi khi mở modal mới
  useEffect(() => {
    if (isOpen) {
      setCancelReasonPreset('')
      setCancelReasonText('')
      setCancelReasonError('')
      setCancelling(false)
    }
  }, [isOpen, order?.id])

  if (!isOpen || !order) return null

  // Chọn lý do gợi ý nhanh
  const handleSelectPresetReason = (reason) => {
    setCancelReasonPreset(reason)
    setCancelReasonError('')
    if (reason === 'Lý do khác') {
      if (PRESET_CANCEL_REASONS.slice(0, -1).includes(cancelReasonText)) {
        setCancelReasonText('')
      }
    } else {
      setCancelReasonText(reason)
    }
  }

  // Xác nhận hủy đơn hàng
  const handleConfirmCancel = async () => {
    if (!order?.id || cancelling) return

    const finalReason = (cancelReasonText || cancelReasonPreset || '').trim()
    if (!finalReason) {
      setCancelReasonError('Vui lòng chọn hoặc nhập lý do hủy đơn hàng.')
      return
    }

    if (finalReason.length > 500) {
      setCancelReasonError('Lý do hủy đơn không được vượt quá 500 ký tự.')
      return
    }

    setCancelling(true)
    setCancelReasonError('')

    try {
      const res = await orderService.cancelOrder(order.id, finalReason)
      const updatedOrder = res?.data || res
      if (onSuccess) {
        onSuccess(updatedOrder, finalReason)
      }
      onClose()
    } catch (err) {
      let errorMsg = 'Không thể hủy đơn hàng. Vui lòng thử lại.'
      if (err.response?.status === 409) {
        errorMsg =
          err.response?.data?.message ||
          'Không thể hủy đơn hàng do trạng thái đơn hàng đã thay đổi.'
      } else if (err.response?.status === 403) {
        errorMsg = 'Bạn không có quyền hủy đơn hàng này.'
      } else if (err.response?.status === 404) {
        errorMsg = 'Đơn hàng không tồn tại hoặc đã bị xóa.'
      } else if (err.response?.status === 401) {
        errorMsg = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
        navigate('/login')
      } else {
        errorMsg =
          err.response?.data?.message ||
          err.message ||
          'Không thể hủy đơn hàng. Vui lòng thử lại sau.'
      }

      if (onError) {
        onError(errorMsg)
      }
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-md transition-all duration-300 animate-in fade-in">
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-lg w-full p-6 sm:p-7 space-y-5 animate-in zoom-in-95 duration-200">
        {/* Header Modal */}
        <div className="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-100 shadow-2xs">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-slate-900">
                Xác nhận hủy đơn hàng
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                Mã đơn: #{order.orderCode || order.id}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={cancelling}
            className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer disabled:opacity-50"
            title="Đóng modal"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Warning notice */}
        <div className="p-3.5 rounded-2xl bg-amber-50/80 border border-amber-200/80 text-xs text-amber-900 flex items-start gap-2.5 leading-relaxed">
          <HelpCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
          <span>
            Sau khi xác nhận hủy, đơn hàng sẽ chuyển sang trạng thái <strong>Đã hủy</strong>.
            Toàn bộ số lượng sản phẩm trong đơn sẽ được <strong>tự động hoàn lại vào kho</strong>.
          </span>
        </div>

        {/* Lý do hủy: Chọn nhanh hoặc Tự nhập */}
        <div className="space-y-3">
          <label className="block text-xs font-bold text-slate-800">
            Lý do hủy đơn hàng <span className="text-red-500">*</span>
          </label>

          {/* Chips chọn nhanh */}
          <div className="flex flex-wrap gap-2">
            {PRESET_CANCEL_REASONS.map((reason) => {
              const isSelected =
                cancelReasonPreset === reason || cancelReasonText === reason
              return (
                <button
                  key={reason}
                  type="button"
                  onClick={() => handleSelectPresetReason(reason)}
                  disabled={cancelling}
                  className={`text-xs px-3 py-1.5 rounded-xl border transition-all text-left cursor-pointer disabled:opacity-50 ${
                    isSelected
                      ? 'bg-zinc-900 text-white border-zinc-900 shadow-2xs font-semibold'
                      : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  {reason}
                </button>
              )
            })}
          </div>

          {/* Textarea nhập chi tiết */}
          <div className="relative">
            <textarea
              rows={3}
              value={cancelReasonText}
              disabled={cancelling}
              onChange={(e) => {
                setCancelReasonText(e.target.value)
                if (cancelReasonError) setCancelReasonError('')
              }}
              placeholder="Vui lòng cung cấp thêm chi tiết lý do hủy đơn của bạn..."
              className="w-full text-xs p-3 rounded-2xl border border-slate-200 bg-slate-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-900 focus:border-transparent transition-all placeholder:text-slate-400 resize-none disabled:opacity-50"
            />
          </div>

          {cancelReasonError && (
            <p className="text-[11px] font-semibold text-red-600 flex items-center gap-1.5">
              <AlertCircle className="w-3.5 h-3.5" />
              <span>{cancelReasonError}</span>
            </p>
          )}
        </div>

        {/* Footer Buttons */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
          <button
            type="button"
            onClick={onClose}
            disabled={cancelling}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer disabled:opacity-50"
          >
            Quay lại
          </button>
          <button
            type="button"
            onClick={handleConfirmCancel}
            disabled={cancelling}
            className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 active:scale-[0.98] transition-all disabled:opacity-50 cursor-pointer shadow-sm shadow-red-200"
          >
            {cancelling ? (
              <>
                <LoadingSpinner size="sm" className="text-white" />
                <span>Đang xử lý hủy...</span>
              </>
            ) : (
              <span>Xác nhận hủy đơn</span>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default CancelOrderModal

