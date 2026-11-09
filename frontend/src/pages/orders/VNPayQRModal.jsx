import React, { useState, useEffect } from 'react'
import {
  QrCode,
  Copy,
  Check,
  Clock,
  ShieldCheck,
  AlertCircle,
  X,
} from 'lucide-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import paymentService from '../../services/paymentService'
import { formatPrice } from './OrderBadges'

/**
 * Modal hiển thị mã QR VNPAY-QR / VietQR mô phỏng cho người dùng quét và xác nhận thanh toán.
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Cao Chí Thiện (CCT)
 */
export const VNPayQRModal = ({
  isOpen,
  order,
  paymentData,
  onClose,
  onSuccess,
  onError,
}) => {
  const [copiedField, setCopiedField] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [timeLeft, setTimeLeft] = useState(15 * 60) // 15 phút đếm ngược

  // Reset timer và state khi mở modal
  useEffect(() => {
    if (isOpen) {
      setTimeLeft(15 * 60)
      setErrorMessage('')
      setSubmitting(false)
      setCopiedField(null)
    }
  }, [isOpen, order?.id])

  // Bộ đếm thời gian hết hạn giao dịch
  useEffect(() => {
    if (!isOpen || timeLeft <= 0) return

    const timer = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)

    return () => clearInterval(timer)
  }, [isOpen, timeLeft])

  if (!isOpen || !order) return null

  // Định dạng thời gian mm:ss
  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  // Thông tin thanh toán hiển thị
  const bankCode = paymentData?.bankCode || 'NCB'
  const bankName = paymentData?.bankName || 'Ngân hàng Quốc Dân (NCB)'
  const accountNumber = paymentData?.accountNumber || '9704198526191432198'
  const accountName = paymentData?.accountName || 'CONG TY TNHH E-COMMERCE RIKKEI'
  const amount = paymentData?.amount || order.totalAmount || 0
  const memo = paymentData?.vnpTxnRef || order.orderCode || `ORD-${order.id}`

  // Link mã QR VietQR động
  const qrUrl =
    paymentData?.qrCodeUrl ||
    `https://img.vietqr.io/image/${bankCode}-${accountNumber}-compact2.png?amount=${Math.round(
      amount
    )}&addInfo=${encodeURIComponent(memo)}&accountName=${encodeURIComponent(
      accountName
    )}`

  // Sao chép thông tin
  const handleCopy = (field, text) => {
    if (!text) return
    navigator.clipboard.writeText(String(text))
    setCopiedField(field)
    setTimeout(() => {
      setCopiedField(null)
    }, 2000)
  }

  // Xác nhận đã thanh toán xong
  const handleConfirmPaid = async (isSuccess = true) => {
    if (!order?.id || submitting) return

    setSubmitting(true)
    setErrorMessage('')

    try {
      const note = isSuccess
        ? `Xác nhận thanh toán thành công qua VNPAY-QR (${bankCode})`
        : `Mô phỏng thanh toán thất bại qua VNPAY-QR (${bankCode})`

      const res = await paymentService.simulatePayment(order.id, isSuccess, note)
      const data = res?.data || res

      if (data?.status === 'PAID') {
        if (onSuccess) {
          onSuccess(data)
        }
        onClose()
      } else {
        if (onError) {
          onError('Giao dịch VNPAY-QR thất bại hoặc bị từ chối.')
        }
        onClose()
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Lỗi khi xác nhận giao dịch VNPAY.'
      setErrorMessage(msg)
      if (onError) {
        onError(msg)
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-xl w-full p-5 sm:p-7 space-y-4 max-h-[92vh] overflow-y-auto animate-in zoom-in-95 duration-200">
        {/* Header Modal */}
        <div className="flex items-center justify-between gap-3 pb-3 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 to-red-500 text-white flex items-center justify-center shrink-0 shadow-md">
              <QrCode className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-extrabold text-slate-900">
                  Cổng thanh toán VNPAY-QR
                </h3>
                <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">
                  Sandbox
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Quét mã QR bằng App Ngân hàng hoặc Ví VNPAY bất kỳ
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
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Đồng hồ đếm ngược */}
        <div className="flex items-center justify-between px-3.5 py-2 rounded-xl bg-amber-50 border border-amber-200/80 text-amber-900 text-xs font-semibold">
          <div className="flex items-center gap-1.5">
            <Clock className="w-4 h-4 text-amber-600 animate-pulse" />
            <span>Thời gian quét mã còn lại:</span>
          </div>
          <span className="font-mono font-extrabold text-sm text-amber-800">
            {formatTimer(timeLeft)}
          </span>
        </div>

        {/* Vùng QR Code trung tâm */}
        <div className="flex flex-col items-center justify-center p-4 bg-slate-50/80 rounded-2xl border border-slate-200/80 text-center relative overflow-hidden">
          <div className="p-3 bg-white rounded-2xl shadow-sm border border-slate-200 relative group">
            <img
              src={qrUrl}
              alt="Mã QR thanh toán VNPAY"
              className="w-56 h-56 sm:w-64 sm:h-64 object-contain rounded-xl transition-transform duration-200 group-hover:scale-[1.02]"
              onError={(e) => {
                // Fallback nếu không có mạng load VietQR
                e.target.onerror = null
                e.target.src =
                  'https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=' +
                  encodeURIComponent(
                    `VNPAY-ORDER:${memo}|AMOUNT:${amount}|ACC:${accountNumber}`
                  )
              }}
            />
          </div>

          <p className="text-[11px] text-slate-500 mt-2.5 flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
            <span>Mã QR mô phỏng chuẩn VietQR tương thích 40+ ngân hàng Việt Nam</span>
          </p>
        </div>

        {/* Bảng chi tiết thông tin chuyển khoản */}
        <div className="bg-slate-50 rounded-2xl p-3.5 border border-slate-100 space-y-2 text-xs text-slate-700">
          <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
            <span className="text-slate-500">Ngân hàng thụ hưởng:</span>
            <span className="font-bold text-slate-900">{bankName}</span>
          </div>

          <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
            <span className="text-slate-500">Chủ tài khoản:</span>
            <span className="font-bold text-slate-900">{accountName}</span>
          </div>

          <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
            <span className="text-slate-500">Số tài khoản:</span>
            <div className="flex items-center gap-1.5 font-mono font-bold text-slate-900">
              <span>{accountNumber}</span>
              <button
                type="button"
                onClick={() => handleCopy('accountNumber', accountNumber)}
                className="p-1 text-slate-400 hover:text-blue-600 rounded transition-colors cursor-pointer"
                title="Sao chép số tài khoản"
              >
                {copiedField === 'accountNumber' ? (
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
          </div>

          <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
            <span className="text-slate-500">Số tiền thanh toán:</span>
            <div className="flex items-center gap-1.5 font-extrabold text-sm text-emerald-700">
              <span>{formatPrice(amount)}</span>
              <button
                type="button"
                onClick={() => handleCopy('amount', amount)}
                className="p-1 text-slate-400 hover:text-blue-600 rounded transition-colors cursor-pointer"
                title="Sao chép số tiền"
              >
                {copiedField === 'amount' ? (
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
          </div>

          <div className="flex justify-between items-center py-1">
            <span className="text-slate-500">Nội dung chuyển khoản:</span>
            <div className="flex items-center gap-1.5 font-mono font-bold text-blue-700">
              <span>{memo}</span>
              <button
                type="button"
                onClick={() => handleCopy('memo', memo)}
                className="p-1 text-slate-400 hover:text-blue-600 rounded transition-colors cursor-pointer"
                title="Sao chép nội dung"
              >
                {copiedField === 'memo' ? (
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
          </div>
        </div>

        {errorMessage && (
          <p className="text-xs font-semibold text-red-600 flex items-center gap-1.5">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </p>
        )}

        {/* Nút hành động */}
        <div className="space-y-2 pt-1 border-t border-slate-100">
          <button
            type="button"
            onClick={() => handleConfirmPaid(true)}
            disabled={submitting || timeLeft <= 0}
            className="w-full py-3 px-4 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-md shadow-blue-500/20 disabled:opacity-50"
          >
            {submitting ? (
              <>
                <LoadingSpinner size="sm" className="text-white" />
                <span>Đang kiểm tra và ghi nhận thanh toán...</span>
              </>
            ) : (
              <>
                <Check className="w-4 h-4" />
                <span>Tôi đã chuyển khoản thành công</span>
              </>
            )}
          </button>

          <div className="flex items-center justify-between gap-2 text-xs">
            <button
              type="button"
              onClick={() => handleConfirmPaid(false)}
              disabled={submitting}
              className="py-2 px-3 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-xl transition-colors cursor-pointer disabled:opacity-50 font-medium"
            >
              ✗ Thử kịch bản lỗi
            </button>

            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="py-2 px-3 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer disabled:opacity-50 font-semibold"
            >
              Thanh toán sau
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default VNPayQRModal

