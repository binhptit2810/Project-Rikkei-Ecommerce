import React, { useEffect, useState } from 'react'
import { useSearchParams, Link, useNavigate } from 'react-router-dom'
import { CheckCircle2, XCircle, ArrowRight, Home, CreditCard, ShieldCheck } from 'lucide-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import paymentService from '../../services/paymentService'
import { formatPrice } from './OrderBadges'

/**
 * Trang nhận kết quả chuyển hướng từ Cổng thanh toán VNPay Sandbox.
 * Phụ trách: Đặng Thanh Bình (DTB)
 */
export const VNPayReturnPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState(false)
  const [paymentData, setPaymentData] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')

  const vnpResponseCode = searchParams.get('vnp_ResponseCode')
  const vnpTxnRef = searchParams.get('vnp_TxnRef')
  const vnpTransactionNo = searchParams.get('vnp_TransactionNo')
  const vnpBankCode = searchParams.get('vnp_BankCode')
  const rawAmount = searchParams.get('vnp_Amount')
  const amount = rawAmount ? Number(rawAmount) / 100 : 0

  useEffect(() => {
    const processReturn = async () => {
      // Chuyển toàn bộ query params thành object
      const params = Object.fromEntries(searchParams.entries())

      if (!params.vnp_TxnRef || !params.vnp_SecureHash) {
        setErrorMessage('Tham số phản hồi từ cổng VNPay không đầy đủ.')
        setLoading(false)
        return
      }

      try {
        const res = await paymentService.handleVNPayCallback(params)
        const data = res?.data || res
        setPaymentData(data)

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
            'Không thể xác thực giao dịch VNPay với hệ thống máy chủ.'
        )
      } finally {
        setLoading(false)
      }
    }

    processReturn()
  }, [searchParams, vnpResponseCode])

  const getVNPayErrorMessage = (code) => {
    switch (code) {
      case '07':
        return 'Trừ tiền thành công nhưng giao dịch bị nghi ngờ gian lận.'
      case '09':
        return 'Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.'
      case '10':
        return 'Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.'
      case '11':
        return 'Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.'
      case '12':
        return 'Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.'
      case '24':
        return 'Giao dịch không thành công do: Khách hàng hủy giao dịch.'
      case '51':
        return 'Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.'
      case '65':
        return 'Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.'
      case '75':
        return 'Ngân hàng thanh toán đang bảo trì.'
      default:
        return `Giao dịch thanh toán qua VNPay không thành công (Mã lỗi: ${code || 'N/A'}).`
    }
  }

  if (loading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center p-6 text-center">
        <LoadingSpinner />
        <p className="mt-4 text-sm font-semibold text-slate-700">
          Đang xác thực chữ ký bảo mật và cập nhật kết quả thanh toán VNPay...
        </p>
      </div>
    )
  }

  return (
    <div className="min-h-[75vh] flex items-center justify-center py-12 px-4 sm:px-6">
      <div className="bg-white max-w-lg w-full rounded-3xl border border-slate-200/90 shadow-xl p-6 sm:p-8 space-y-6 text-center animate-in zoom-in-95 duration-200">
        {success ? (
          <>
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div className="space-y-1.5">
              <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                Thanh toán thành công!
              </h2>
              <p className="text-xs text-slate-500">
                Giao dịch của bạn đã được Cổng thanh toán VNPay ghi nhận và xác thực thành công.
              </p>
            </div>

            <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-left space-y-2.5 text-xs text-slate-700">
              <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Mã đơn / Giao dịch:</span>
                <span className="font-mono font-bold text-slate-900">{vnpTxnRef}</span>
              </div>
              {vnpTransactionNo && (
                <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                  <span className="text-slate-500">Mã giao dịch VNPay:</span>
                  <span className="font-mono font-semibold text-slate-800">{vnpTransactionNo}</span>
                </div>
              )}
              {vnpBankCode && (
                <div className="flex justify-between items-center py-1 border-b border-slate-200/60">
                  <span className="text-slate-500">Ngân hàng thanh toán:</span>
                  <span className="font-bold text-blue-700">{vnpBankCode}</span>
                </div>
              )}
              <div className="flex justify-between items-center py-1">
                <span className="text-slate-500">Số tiền thanh toán:</span>
                <span className="font-extrabold text-sm text-emerald-600">
                  {formatPrice(amount)}
                </span>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              {paymentData?.orderId && (
                <Link
                  to={`/orders/${paymentData.orderId}`}
                  className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 shadow-md shadow-blue-500/20"
                >
                  <span>Xem chi tiết đơn hàng</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
              )}
              <Link
                to="/"
                className="py-3 px-4 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors flex items-center justify-center gap-2"
              >
                <Home className="w-4 h-4" />
                <span>Trang chủ</span>
              </Link>
            </div>
          </>
        ) : (
          <>
            <div className="w-16 h-16 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto shadow-inner">
              <XCircle className="w-10 h-10" />
            </div>

            <div className="space-y-1.5">
              <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                Thanh toán không thành công
              </h2>
              <p className="text-xs text-red-600 font-medium">
                {errorMessage || 'Giao dịch qua VNPay đã bị hủy hoặc xảy ra lỗi kỹ thuật.'}
              </p>
            </div>

            <div className="bg-red-50/60 border border-red-200/60 rounded-2xl p-4 text-left space-y-2 text-xs text-slate-700">
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Mã tham chiếu:</span>
                <span className="font-mono font-bold text-slate-900">{vnpTxnRef || 'N/A'}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-500">Mã phản hồi:</span>
                <span className="font-mono font-semibold text-red-600">{vnpResponseCode || 'Lỗi'}</span>
              </div>
              <p className="text-[11px] text-slate-500 pt-1">
                Lưu ý: Đơn hàng của bạn vẫn được lưu lại. Bạn có thể mở chi tiết đơn hàng để thực hiện thanh toán lại bất kỳ lúc nào.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              {paymentData?.orderId ? (
                <Link
                  to={`/orders/${paymentData.orderId}`}
                  className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 transition-colors flex items-center justify-center gap-2"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Mở đơn hàng & Thử lại</span>
                </Link>
              ) : (
                <Link
                  to="/orders"
                  className="flex-1 py-3 px-4 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 transition-colors flex items-center justify-center gap-2"
                >
                  <span>Danh sách đơn hàng</span>
                </Link>
              )}
              <Link
                to="/"
                className="py-3 px-4 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors flex items-center justify-center gap-2"
              >
                <Home className="w-4 h-4" />
                <span>Trang chủ</span>
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default VNPayReturnPage

