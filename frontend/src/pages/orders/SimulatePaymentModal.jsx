import React, { useState, useEffect } from 'react'
import { CreditCard, CheckCircle2, XCircle, AlertCircle, X, ShieldCheck } from 'lucide-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import paymentService from '../../services/paymentService'
import { formatPrice } from './OrderBadges'

/**
 * Modal Mô phỏng thanh toán trực tuyến (Simulated Payment).
 * Cho phép khách hàng hoặc kiểm thử viên giả lập thanh toán Thành công (PAID) hoặc Thất bại (FAILED).
 *
 * @param {boolean} isOpen Trạng thái hiển thị modal
 * @param {Object} order Đơn hàng cần thanh toán (id, orderCode, totalAmount)
 * @param {Function} onClose Callback đóng modal
 * @param {Function} onSuccess Callback khi thanh toán thành công
 * @param {Function} onError Callback khi thanh toán thất bại
 */
export const SimulatePaymentModal = ({
  isOpen,
  order,
  onClose,
  onSuccess,
  onError,
}) => {
  const [simulateSuccess, setSimulateSuccess] = useState(true)
  const [transactionNote, setTransactionNote] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (isOpen) {
      setSimulateSuccess(true)
      setTransactionNote('')
      setErrorMessage('')
      setSubmitting(false)
    }
  }, [isOpen, order?.id])

  if (!isOpen || !order) return null

  const handleConfirmPayment = async () => {
    if (!order?.id || submitting) return

    setSubmitting(true)
    setErrorMessage('')

    try {
      const note = transactionNote.trim() ||
        (simulateSuccess
          ? 'Mô phỏng thanh toán trực tuyến thành công (PAID)'
          : 'Mô phỏng thanh toán trực tuyến thất bại (FAILED)')

      const res = await paymentService.simulatePayment(order.id, simulateSuccess, note)
      const paymentData = res?.data || res

      if (paymentData?.status === 'PAID') {
        if (onSuccess) {
          onSuccess(paymentData)
        }
        onClose()
      } else {
        // Mô phỏng thất bại (status === FAILED)
        if (onError) {
          onError('Thanh toán mô phỏng thất bại (FAILED). Trạng thái đơn hàng không đổi, bạn có thể thực hiện lại.')
        }
        onClose()
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Lỗi khi xử lý thanh toán mô phỏng.'
      setErrorMessage(msg)
      if (onError) {
        onError(msg)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-lg w-full p-6 sm:p-7 space-y-5 animate-in zoom-in-95 duration-200">
        {/* Header Modal */}
        <div className="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-zinc-900 text-zinc-50 flex items-center justify-center shrink-0 shadow-2xs">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-slate-900">
                Cổng thanh toán mô phỏng
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                Đơn #{order.orderCode || order.id} • {formatPrice(order.totalAmount)}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer disabled:opacity-50"
            title="Đóng modal"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Security badge note */}
        <div className="p-3.5 rounded-2xl bg-blue-50/80 border border-blue-200/80 text-xs text-blue-900 flex items-start gap-2.5 leading-relaxed">
          <ShieldCheck className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
          <span>
            Hệ thống đang chạy ở chế độ <strong>Mô phỏng thanh toán trực tuyến</strong>. Mã giao dịch <strong>PAY-YYYYMMDD-XXXXXXXX</strong> sẽ được tạo và đồng bộ trạng thái an toàn tại Backend.
          </span>
        </div>

        {/* Kịch bản thanh toán */}
        <div className="space-y-3">
          <label className="block text-xs font-bold text-slate-800">
            Chọn kịch bản thanh toán:
          </label>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start gap-3 ${
                simulateSuccess
                  ? 'border-emerald-600 bg-emerald-50/60 ring-2 ring-emerald-200 shadow-xs'
                  : 'border-slate-200 hover:border-slate-300 bg-white'
              }`}
            >
              <input
                type="radio"
                name="simulateSuccessRadio"
                checked={simulateSuccess === true}
                onChange={() => setSimulateSuccess(true)}
                disabled={submitting}
                className="mt-0.5 text-emerald-600 focus:ring-emerald-600"
              />
              <div className="flex-1">
                <div className="flex items-center gap-1.5 font-bold text-xs text-emerald-950">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Thành công (PAID)</span>
                </div>
                <p className="text-[11px] text-emerald-800/80 mt-1 leading-snug">
                  Số dư đủ, thẻ hợp lệ. Đơn hàng chuyển sang Đã xác nhận.
                </p>
              </div>
            </label>

            <label
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start gap-3 ${
                !simulateSuccess
                  ? 'border-red-600 bg-red-50/60 ring-2 ring-red-200 shadow-xs'
                  : 'border-slate-200 hover:border-slate-300 bg-white'
              }`}
            >
              <input
                type="radio"
                name="simulateSuccessRadio"
                checked={simulateSuccess === false}
                onChange={() => setSimulateSuccess(false)}
                disabled={submitting}
                className="mt-0.5 text-red-600 focus:ring-red-600"
              />
              <div className="flex-1">
                <div className="flex items-center gap-1.5 font-bold text-xs text-red-950">
                  <XCircle className="w-3.5 h-3.5 text-red-600" />
                  <span>Thất bại (FAILED)</span>
                </div>
                <p className="text-[11px] text-red-800/80 mt-1 leading-snug">
                  Lỗi thẻ hoặc từ chối GD. Giữ nguyên giỏ hàng, cho phép thử lại.
                </p>
              </div>
            </label>
          </div>

          {/* Ghi chú giao dịch */}
          <div className="space-y-1.5 pt-2">
            <label className="block text-xs font-medium text-slate-700">
              Ghi chú giao dịch (Tùy chọn)
            </label>
            <input
              type="text"
              value={transactionNote}
              onChange={(e) => setTransactionNote(e.target.value)}
              disabled={submitting}
              placeholder={
                simulateSuccess
                  ? 'Thanh toán trực tuyến mô phỏng thành công'
                  : 'Thanh toán thất bại do từ chối giao dịch'
              }
              className="w-full text-xs p-2.5 rounded-xl border border-slate-200 bg-slate-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-900 transition-all placeholder:text-slate-400 disabled:opacity-50"
            />
          </div>

          {errorMessage && (
            <p className="text-[11px] font-semibold text-red-600 flex items-center gap-1.5 pt-1">
              <AlertCircle className="w-3.5 h-3.5 shrink-0" />
              <span>{errorMessage}</span>
            </p>
          )}
        </div>

        {/* Footer Buttons */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer disabled:opacity-50"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={handleConfirmPayment}
            disabled={submitting}
            className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 active:scale-[0.98] transition-all disabled:opacity-50 cursor-pointer shadow-sm"
          >
            {submitting ? (
              <>
                <LoadingSpinner size="sm" className="text-white" />
                <span>Đang xử lý...</span>
              </>
            ) : (
              <span>Xác nhận thanh toán</span>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default SimulatePaymentModal

