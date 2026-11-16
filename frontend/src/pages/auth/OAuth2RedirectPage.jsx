import React, { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { AlertCircle, ArrowLeft, CheckCircle2, ShieldAlert } from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang trung gian tiếp nhận chuyển hướng sau khi đăng nhập Google OAuth2 [TSK-D15-CCT].
 * Tiếp nhận One-Time Ticket từ query param, gọi API exchange an toàn và cập nhật trạng thái đăng nhập.
 */
export const OAuth2RedirectPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { loginWithOAuth2Ticket } = useAuthStore()

  const [status, setStatus] = useState('processing') // 'processing' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('')
  const [lockReason, setLockReason] = useState(null)

  const exchangeExecuted = useRef(false)

  useEffect(() => {
    // Ngăn chặn React StrictMode thực thi exchange 2 lần gây lỗi tiêu thụ ticket
    if (exchangeExecuted.current) {
      return
    }

    const ticket = searchParams.get('ticket')
    const oauth2Error = searchParams.get('oauth2Error') || searchParams.get('error')

    if (oauth2Error) {
      exchangeExecuted.current = true
      setStatus('error')
      setErrorMessage(oauth2Error)
      return
    }

    if (!ticket) {
      exchangeExecuted.current = true
      setStatus('error')
      setErrorMessage('Không tìm thấy mã xác thực Google (Ticket). Vui lòng thử đăng nhập lại.')
      return
    }

    exchangeExecuted.current = true

    const executeExchange = async () => {
      try {
        await loginWithOAuth2Ticket(ticket)
        setStatus('success')
        // Chuyển hướng về trang chủ sau 500ms và xóa sạch query param khỏi URL
        setTimeout(() => {
          navigate('/', { replace: true })
        }, 500)
      } catch (err) {
        setStatus('error')
        const errData = err.response?.data
        setErrorMessage(
          errData?.message || err.message || 'Xác thực tài khoản Google thất bại.'
        )
        const reason = errData?.data?.reason || errData?.data?.lockReason || null
        setLockReason(reason)
      }
    }

    executeExchange()
  }, [searchParams, loginWithOAuth2Ticket, navigate])

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-16 bg-slate-50/60">
      <div className="w-full max-w-md bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 text-center space-y-6">
        {status === 'processing' && (
          <div className="space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto shadow-xs">
              <LoadingSpinner size="lg" className="text-blue-600" />
            </div>
            <div className="space-y-1">
              <h2 className="text-xl font-extrabold text-slate-900">
                Đang xác thực tài khoản Google...
              </h2>
              <p className="text-xs text-slate-500 font-medium">
                Vui lòng đợi trong giây lát trong khi chúng tôi hoàn tất phiên đăng nhập của bạn.
              </p>
            </div>
          </div>
        )}

        {status === 'success' && (
          <div className="space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs">
              <CheckCircle2 className="w-10 h-10" />
            </div>
            <div className="space-y-1">
              <h2 className="text-xl font-extrabold text-slate-900">
                Đăng nhập thành công!
              </h2>
              <p className="text-xs text-slate-500 font-medium">
                Đang chuyển hướng bạn về trang chủ...
              </p>
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="space-y-5 text-left">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                {lockReason ? <ShieldAlert className="w-6 h-6" /> : <AlertCircle className="w-6 h-6" />}
              </div>
              <div>
                <h2 className="text-lg font-extrabold text-slate-900">
                  Đăng nhập thất bại
                </h2>
                <p className="text-xs text-slate-500">
                  Có lỗi xảy ra trong quá trình xác thực với Google
                </p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-900 text-xs space-y-2">
              <p className="font-semibold leading-relaxed">
                {errorMessage}
              </p>
              {lockReason && (
                <div className="bg-red-100/60 p-2.5 rounded-xl border border-red-200">
                  <span className="font-bold">Lý do khóa:</span> {lockReason}
                </div>
              )}
            </div>

            <Link
              to="/login"
              replace
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow-md cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại trang Đăng nhập</span>
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}

export default OAuth2RedirectPage

