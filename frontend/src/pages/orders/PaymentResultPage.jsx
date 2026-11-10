import React, { useEffect, useState } from 'react'
import { useSearchParams, Link, useNavigate } from 'react-router-dom'
import {
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ArrowRight,
  ShoppingBag,
  CreditCard,
  Copy,
  Check,
  RefreshCw,
  Home,
  ShieldCheck,
  Calendar,
  FileText,
  Building,
} from 'lucide-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import paymentService from '../../services/paymentService'
import { formatPrice } from '../orders/OrderBadges'

/**
 * Trang Kết quả Thanh toán VNPay (PaymentResultPage).
 * Hứng mã vnp_ResponseCode từ Cổng thanh toán VNPay Sandbox và hiển thị kết quả giao dịch rõ ràng.
 *
 * Tuyến đường (Routes):
 * - /payment/result
 * - /orders/vnpay-return
 * - /payment-result
 *
 * Phụ trách: Đặng Thanh Bình (DTB)
 * Phối hợp: Đỗ Đức Mạnh (DDM)
 *
 * Tiêu chí nghiệm thu (DoD):
 * Trang kết quả thanh toán hiển thị rõ ràng mã giao dịch; thông báo thành công/thất bại chính xác.
 */
export const PaymentResultPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState(false)
  const [paymentData, setPaymentData] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [copiedCode, setCopiedCode] = useState(false)

  // Trích xuất các tham số chuẩn từ VNPay Callback
  const vnpResponseCode = searchParams.get('vnp_ResponseCode')
  const vnpTxnRef = searchParams.get('vnp_TxnRef')
  const vnpTransactionNo = searchParams.get('vnp_TransactionNo')
  const vnpBankCode = searchParams.get('vnp_BankCode')
  const vnpBankTranNo = searchParams.get('vnp_BankTranNo')
  const vnpCardType = searchParams.get('vnp_CardType')
  const vnpOrderInfo = searchParams.get('vnp_OrderInfo')
  const vnpPayDate = searchParams.get('vnp_PayDate')
  const rawAmount = searchParams.get('vnp_Amount')
  const vnpSecureHash = searchParams.get('vnp_SecureHash')

  // Số tiền quy đổi (VNPay nhân 100 khi truyền qua URL)
  const amount = rawAmount ? Number(rawAmount) / 100 : 0

  // Format ngày giờ từ định dạng yyyyMMddHHmmss của VNPay
  const formatVNPayDate = (dateStr) => {
    if (!dateStr || dateStr.length < 14) return 'N/A'
    const y = dateStr.substring(0, 4)
    const m = dateStr.substring(4, 6)
    const d = dateStr.substring(6, 8)
    const h = dateStr.substring(8, 10)
    const mi = dateStr.substring(10, 12)
    const s = dateStr.substring(12, 14)
    return `${d}/${m}/${y} ${h}:${mi}:${s}`
  }

  // Bảng tra cứu mô tả mã lỗi chi tiết của VNPay
  const getVNPayErrorMessage = (code) => {
    switch (code) {
      case '00':
        return 'Giao dịch thanh toán được thực hiện thành công.'
      case '07':
        return 'Trừ tiền thành công nhưng giao dịch bị nghi ngờ gian lận (bất thường).'
      case '09':
        return 'Thẻ / Tài khoản của quý khách chưa đăng ký dịch vụ InternetBanking tại ngân hàng.'
      case '10':
        return 'Xác thực thông tin thẻ / tài khoản không đúng quá 3 lần.'
      case '11':
        return 'Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.'
      case '12':
        return 'Thẻ / Tài khoản của quý khách hiện đang bị khóa.'
      case '13':
        return 'Quý khách nhập sai mật khẩu xác thực giao dịch (mã OTP).'
      case '24':
        return 'Giao dịch không thành công do khách hàng đã bấm hủy thanh toán.'
      case '51':
        return 'Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.'
      case '65':
        return 'Tài khoản của quý khách đã vượt quá hạn mức giao dịch trong ngày.'
      case '75':
        return 'Ngân hàng thanh toán đang trong thời gian bảo trì hệ thống.'
      case '79':
        return 'Nhập sai mật khẩu thanh toán quá số lần quy định. Xin vui lòng thử lại sau.'
      default:
        return `Giao dịch không thành công hoặc bị từ chối bởi cổng VNPay (Mã lỗi: ${code || 'N/A'}).`
    }
  }

  useEffect(() => {
    const verifyAndProcessPayment = async () => {
      // Nếu không có tham số nào (người dùng truy cập trực tiếp url)
      if (!vnpResponseCode && !vnpTxnRef) {
        setLoading(false)
        setSuccess(false)
        setErrorMessage('Không tìm thấy thông tin giao dịch thanh toán trong URL.')
        return
      }

      const params = Object.fromEntries(searchParams.entries())

      try {
        // Gửi toàn bộ tham số callback lên backend để xác thực chữ ký HMAC-SHA512 & cập nhật đơn hàng
        let res
        try {
          res = await paymentService.handleVNPayCallback(params)
        } catch (apiErr) {
          console.warn('API xác thực callback trả về lỗi hoặc đã được xử lý:', apiErr)
        }

        const data = res?.data || res
        if (data) {
          setPaymentData(data)
        }

        // Đánh giá thành công hay thất bại dựa trên vnp_ResponseCode
        if (vnpResponseCode === '00') {
          setSuccess(true)
        } else {
          setSuccess(false)
          setErrorMessage(getVNPayErrorMessage(vnpResponseCode))
        }
      } catch (err) {
        setSuccess(false)
        setErrorMessage(
          err?.response?.data?.message ||
            getVNPayErrorMessage(vnpResponseCode) ||
            'Không thể xác thực kết quả giao dịch thanh toán.'
        )
      } finally {
        setLoading(false)
      }
    }

    verifyAndProcessPayment()
  }, [searchParams, vnpResponseCode, vnpTxnRef])

  // Sao chép mã giao dịch
  const handleCopy = (text) => {
    if (!text) return
    navigator.clipboard.writeText(text)
    setCopiedCode(true)
    setTimeout(() => setCopiedCode(false), 2000)
  }

  // Trích xuất Order ID (nếu vnpTxnRef có dạng ORD-123 hoặc số)
  const extractOrderId = () => {
    if (paymentData?.orderId) return paymentData.orderId
    if (vnpTxnRef) {
      const match = vnpTxnRef.match(/\d+/)
      if (match) return match[0]
    }
    return null
  }

  const orderId = extractOrderId()

  // Màn hình đang xác thực
  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center p-6 text-center">
        <LoadingSpinner size="lg" />
        <h3 className="mt-4 text-base font-bold text-slate-800">
          Đang xác thực giao dịch từ Cổng VNPay...
        </h3>
        <p className="mt-1 text-xs text-slate-500 max-w-sm">
          Hệ thống đang kiểm tra chữ ký bảo mật và cập nhật trạng thái đơn hàng của bạn. Vui lòng chờ trong giây lát.
        </p>
      </div>
    )
  }

  return (
    <div className="min-h-[75vh] flex items-center justify-center py-10 px-4 sm:px-6">
      <div className="bg-white max-w-xl w-full rounded-3xl border border-slate-200/90 shadow-2xl p-6 sm:p-8 space-y-6 text-center animate-in zoom-in-95 duration-200">
        {/* Status Icon & Header */}
        {success ? (
          <>
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner animate-in zoom-in duration-300">
              <CheckCircle2 className="w-10 h-10 sm:w-12 sm:h-12" />
            </div>

            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Giao dịch thành công (Mã 00)</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                Thanh toán thành công!
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
                Cảm ơn bạn đã mua sắm tại Rikkei Store. Đơn hàng của bạn đã được thanh toán và chuyển sang trạng thái xử lý đóng gói.
              </p>
            </div>
          </>
        ) : (
          <>
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto shadow-inner animate-in zoom-in duration-300">
              <XCircle className="w-10 h-10 sm:w-12 sm:h-12" />
            </div>

            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-red-100 text-red-800">
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>Giao dịch không thành công (Mã: {vnpResponseCode || 'LỖI'})</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                Thanh toán chưa hoàn tất
              </h1>
              <p className="text-xs sm:text-sm text-red-600 font-medium max-w-md mx-auto bg-red-50 p-2.5 rounded-xl border border-red-100">
                {errorMessage || 'Giao dịch thanh toán qua cổng VNPay không thành công hoặc bị gián đoạn.'}
              </p>
            </div>
          </>
        )}

        {/* Transaction Detail Receipt Card (Hiển thị rõ ràng mã giao dịch) */}
        {(vnpTxnRef || vnpTransactionNo) && (
          <div className="bg-slate-50/80 border border-slate-200/90 rounded-2xl p-4 sm:p-5 text-left space-y-2.5 text-xs text-slate-700 shadow-2xs">
            <div className="flex items-center justify-between pb-2 border-b border-slate-200 text-slate-400 text-[11px] font-bold uppercase tracking-wider">
              <span>Thông tin biên lai giao dịch</span>
              <span className="font-mono text-blue-700 font-extrabold">VNPay Sandbox</span>
            </div>

            {/* Mã giao dịch / Đơn hàng */}
            <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
              <span className="text-slate-500">Mã đơn / Tham chiếu (TxnRef):</span>
              <div className="flex items-center gap-1.5 font-mono font-bold text-slate-900">
                <span>{vnpTxnRef || 'N/A'}</span>
                {vnpTxnRef && (
                  <button
                    type="button"
                    onClick={() => handleCopy(vnpTxnRef)}
                    title="Sao chép mã"
                    className="p-1 rounded-md text-slate-400 hover:text-slate-700 hover:bg-slate-200 transition-colors cursor-pointer"
                  >
                    {copiedCode ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                )}
              </div>
            </div>

            {/* Mã giao dịch tại VNPAY */}
            {vnpTransactionNo && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Mã giao dịch VNPAY:</span>
                <span className="font-mono font-semibold text-slate-800">{vnpTransactionNo}</span>
              </div>
            )}

            {/* Ngân hàng thanh toán */}
            {vnpBankCode && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Ngân hàng thanh toán:</span>
                <span className="font-bold text-blue-700">{vnpBankCode}</span>
              </div>
            )}

            {/* Loại thẻ / Phương thức */}
            {vnpCardType && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Phương thức thanh toán:</span>
                <span className="font-semibold text-slate-800">{vnpCardType}</span>
              </div>
            )}

            {/* Mã giao dịch ngân hàng */}
            {vnpBankTranNo && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Mã giao dịch ngân hàng:</span>
                <span className="font-mono text-slate-800">{vnpBankTranNo}</span>
              </div>
            )}

            {/* Thời gian thanh toán */}
            {vnpPayDate && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Thời gian thực hiện:</span>
                <span className="font-medium text-slate-700">{formatVNPayDate(vnpPayDate)}</span>
              </div>
            )}

            {/* Nội dung thanh toán */}
            {vnpOrderInfo && (
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Nội dung thanh toán:</span>
                <span className="font-medium text-slate-800 max-w-[240px] truncate text-right">
                  {vnpOrderInfo}
                </span>
              </div>
            )}

            {/* Số tiền thanh toán */}
            <div className="flex justify-between items-center pt-1.5">
              <span className="text-slate-500 font-bold">Tổng tiền thanh toán:</span>
              <span
                className={`font-black text-base sm:text-lg ${
                  success ? 'text-emerald-600' : 'text-slate-900'
                }`}
              >
                {formatPrice(amount)}
              </span>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          {orderId ? (
            <Link
              to={`/orders/${orderId}`}
              className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-md shadow-blue-500/20"
            >
              <span>{success ? 'Xem chi tiết đơn hàng' : 'Quay lại đơn hàng & Thử lại'}</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          ) : (
            <Link
              to="/orders"
              className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-md shadow-blue-500/20"
            >
              <span>Xem danh sách đơn hàng</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          )}

          <Link
            to="/products"
            className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors flex items-center justify-center gap-2"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Tiếp tục mua sắm</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

export default PaymentResultPage

