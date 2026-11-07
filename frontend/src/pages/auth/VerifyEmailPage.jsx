import React, { useState, useRef, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Mail,
  AlertCircle,
  CheckCircle2,
  ArrowLeft,
  RotateCw,
  ShieldCheck,
} from 'lucide-react'
import authService from '../../services/authService'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

const PENDING_EMAIL_KEY = 'pending_verify_email'
const COOLDOWN_EXPIRY_KEY = 'verify_resend_cooldown_expiry'

export const VerifyEmailPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { verifyEmail, clearError } = useAuthStore()

  // Lấy email từ router state (nếu vừa chuyển trang) hoặc khôi phục từ sessionStorage (khi reload F5)
  const initialEmail =
    location.state?.email || sessionStorage.getItem(PENDING_EMAIL_KEY) || ''
  const initialMessage = location.state?.successMessage || ''

  const [email, setEmail] = useState(initialEmail)
  const [isEditingEmail, setIsEditingEmail] = useState(!initialEmail)

  // Lưu email đang chờ xác thực vào sessionStorage (không lưu mật khẩu, OTP hay token)
  useEffect(() => {
    if (email && email.trim()) {
      sessionStorage.setItem(PENDING_EMAIL_KEY, email.trim())
    }
  }, [email])

  // State cho 6 ô nhập OTP
  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const inputRefs = useRef([])

  // Validation & Error states
  const [otpError, setOtpError] = useState('')
  const [apiError, setApiError] = useState('')
  const [apiSuccessMessage, setApiSuccessMessage] = useState(initialMessage)
  const [isLoading, setIsLoading] = useState(false)
  const [isSuccess, setIsSuccess] = useState(false)

  // Tính toán thời gian cooldown thực tế còn lại từ timestamp trong sessionStorage
  const calculateInitialCooldown = () => {
    const storedExpiry = sessionStorage.getItem(COOLDOWN_EXPIRY_KEY)
    if (storedExpiry) {
      const remaining = Math.max(
        0,
        Math.ceil((parseInt(storedExpiry, 10) - Date.now()) / 1000)
      )
      return remaining
    }
    // Nếu vừa đăng ký thành công (có location.state.email và không phải luồng autoResend), khởi tạo 60s
    if (location.state?.email && !location.state?.autoResend) {
      const expiry = Date.now() + 60 * 1000
      sessionStorage.setItem(COOLDOWN_EXPIRY_KEY, expiry.toString())
      return 60
    }
    return 0
  }

  const [resendCooldown, setResendCooldown] = useState(calculateInitialCooldown)
  const [isResending, setIsResending] = useState(false)

  // Quản lý đếm ngược thời gian cooldown còn lại
  useEffect(() => {
    if (resendCooldown <= 0) return
    const timer = setInterval(() => {
      setResendCooldown((prev) => {
        if (prev <= 1) {
          sessionStorage.removeItem(COOLDOWN_EXPIRY_KEY)
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [resendCooldown])

  // Tự động focus vào ô OTP đầu tiên khi tải trang nếu đã có sẵn email
  useEffect(() => {
    if (initialEmail && !isEditingEmail) {
      inputRefs.current[0]?.focus()
    }
  }, [initialEmail, isEditingEmail])

  // Ref bảo vệ chống duplicate trigger do React 18 StrictMode (chạy effect 2 lần trên dev)
  // và tránh re-trigger khi component re-render
  const autoResendTriggeredRef = useRef(false)

  // Tự động gửi OTP mới khi chuyển sang từ thông báo tài khoản chưa xác thực ở LoginPage
  useEffect(() => {
    if (!location.state?.autoResend || autoResendTriggeredRef.current) {
      return
    }

    autoResendTriggeredRef.current = true

    // Tiêu thụ cờ autoResend trên router history state để F5 / reload không gửi lặp lại
    navigate(location.pathname, {
      replace: true,
      state: {
        ...location.state,
        autoResend: false,
      },
    })

    const targetEmail = (email || location.state?.email || '').trim().toLowerCase()
    if (!targetEmail) return

    const executeAutoResend = async () => {
      setIsResending(true)
      setApiError('')
      setApiSuccessMessage('Đang tự động gửi mã OTP xác thực mới đến email của bạn...')

      try {
        const res = await authService.resendVerification({ email: targetEmail })
        setApiSuccessMessage(
          res?.message ||
            'Mã OTP xác thực mới đã được tự động gửi đến email của bạn. Vui lòng kiểm tra hộp thư.'
        )
        const expiry = Date.now() + 60 * 1000
        sessionStorage.setItem(COOLDOWN_EXPIRY_KEY, expiry.toString())
        setResendCooldown(60)
        setOtp(['', '', '', '', '', ''])
        inputRefs.current[0]?.focus()
      } catch (err) {
        const msg =
          err.response?.data?.message ||
          'Không thể tự động gửi lại mã OTP lúc này. Vui lòng thử lại sau.'

        // Nếu mã vừa được gửi gần đây (dưới 60s) hoặc gặp 429
        if (msg.includes('60 giây') || err.response?.status === 429) {
          setApiSuccessMessage(
            'Mã OTP xác thực đã được gửi trước đó. Vui lòng kiểm tra email của bạn hoặc đợi hết thời gian đếm ngược để nhận mã mới.'
          )
          const storedExpiry = sessionStorage.getItem(COOLDOWN_EXPIRY_KEY)
          if (!storedExpiry) {
            const expiry = Date.now() + 60 * 1000
            sessionStorage.setItem(COOLDOWN_EXPIRY_KEY, expiry.toString())
            setResendCooldown(60)
          }
        } else {
          setApiError(msg)
        }
      } finally {
        setIsResending(false)
      }
    }

    executeAutoResend()
  }, [location.state, email, navigate])

  // Che bớt email để bảo vệ quyền riêng tư người dùng
  const maskEmail = (str) => {
    if (!str || !str.includes('@')) return str
    const [name, domain] = str.split('@')
    if (name.length <= 2) return `${name}***@${domain}`
    return `${name[0]}***${name[name.length - 1]}@${domain}`
  }

  // Xử lý nhập từng ô OTP (chỉ nhận số, tự nhảy focus, giữ số 0 đầu)
  const handleOtpChange = (index, value) => {
    const char = value.replace(/\D/g, '')

    const newOtp = [...otp]
    newOtp[index] = char ? char.slice(-1) : ''
    setOtp(newOtp)

    if (otpError) setOtpError('')
    if (apiError) setApiError('')

    // Tự động nhảy sang ô tiếp theo nếu vừa nhập số
    if (char && index < 5) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  // Xử lý các phím điều hướng và Backspace
  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace') {
      if (!otp[index] && index > 0) {
        inputRefs.current[index - 1]?.focus()
        const newOtp = [...otp]
        newOtp[index - 1] = ''
        setOtp(newOtp)
      } else if (otp[index]) {
        const newOtp = [...otp]
        newOtp[index] = ''
        setOtp(newOtp)
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus()
    } else if (e.key === 'ArrowRight' && index < 5) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  // Hỗ trợ dán toàn bộ mã OTP 6 số từ clipboard
  const handleOtpPaste = (e) => {
    e.preventDefault()
    const pastedData = e.clipboardData.getData('text/plain').trim()
    const digits = pastedData.replace(/\D/g, '').slice(0, 6)

    if (!digits) return

    const newOtp = [...otp]
    for (let i = 0; i < 6; i++) {
      newOtp[i] = digits[i] || ''
    }
    setOtp(newOtp)

    if (otpError) setOtpError('')
    if (apiError) setApiError('')

    const focusIndex = Math.min(digits.length, 5)
    inputRefs.current[focusIndex]?.focus()
  }

  // Xử lý gửi lại mã OTP (Cool-down 60s)
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || isResending || !email.trim()) return

    setIsResending(true)
    setApiError('')

    try {
      const res = await authService.resendVerification({ email: email.trim().toLowerCase() })
      setApiSuccessMessage(
        res?.message || 'Mã OTP mới đã được gửi đến email của bạn.'
      )
      const expiry = Date.now() + 60 * 1000
      sessionStorage.setItem(COOLDOWN_EXPIRY_KEY, expiry.toString())
      setResendCooldown(60)
      setOtp(['', '', '', '', '', ''])
      inputRefs.current[0]?.focus()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        'Không thể gửi lại mã OTP lúc này. Vui lòng thử lại sau.'

      if (msg.includes('60 giây') || err.response?.status === 429) {
        setApiSuccessMessage(
          'Mã OTP xác thực đã được gửi trước đó. Vui lòng kiểm tra email của bạn hoặc đợi hết thời gian đếm ngược để nhận mã mới.'
        )
        const storedExpiry = sessionStorage.getItem(COOLDOWN_EXPIRY_KEY)
        if (!storedExpiry) {
          const expiry = Date.now() + 60 * 1000
          sessionStorage.setItem(COOLDOWN_EXPIRY_KEY, expiry.toString())
          setResendCooldown(60)
        }
      } else {
        setApiError(msg)
      }
    } finally {
      setIsResending(false)
    }
  }

  // Submit xác thực OTP
  const handleSubmit = async (e) => {
    e.preventDefault()
    const otpValue = otp.join('')

    if (!email.trim()) {
      setApiError('Vui lòng nhập địa chỉ email cần xác thực')
      return
    }

    if (!otpValue || otpValue.length < 6) {
      setOtpError('Vui lòng nhập đủ 6 chữ số mã OTP')
      return
    }

    setIsLoading(true)
    setApiError('')
    clearError()

    try {
      await verifyEmail({
        email: email.trim().toLowerCase(),
        otp: otpValue,
      })

      // Dọn dẹp pending email và cooldown trong sessionStorage khi xác thực thành công
      sessionStorage.removeItem(PENDING_EMAIL_KEY)
      sessionStorage.removeItem(COOLDOWN_EXPIRY_KEY)

      setIsSuccess(true)
      // Tự động chuyển hướng về trang cá nhân sau 1.5 giây
      setTimeout(() => {
        navigate('/profile', { replace: true })
      }, 1500)
    } catch (err) {
      const message =
        err.response?.data?.message || err.message || 'Xác thực OTP không thành công'
      setApiError(message)
    } finally {
      setIsLoading(false)
    }
  }

  // Màn hình thông báo xác thực thành công
  if (isSuccess) {
    return (
      <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 text-center space-y-6">
            <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto ring-8 ring-emerald-50/50">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="space-y-2">
              <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
                Xác thực thành công!
              </h1>
              <p className="text-xs text-slate-500 leading-relaxed">
                Tài khoản của bạn đã được kích hoạt thành công. Hệ thống đang tự động đăng nhập và đưa bạn đến trang cá nhân...
              </p>
            </div>

            <div className="pt-2">
              <button
                type="button"
                onClick={() => navigate('/profile', { replace: true })}
                className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer"
              >
                <span>Chuyển đến trang cá nhân ngay</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
      <div className="w-full max-w-md">
        {/* Card Container */}
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6">
          {/* Logo & Header */}
          <div className="space-y-2 text-center">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center mx-auto shadow-md shadow-blue-500/25">
              <ShieldCheck className="w-6 h-6 fill-current text-white" />
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 pt-1">
              Xác thực địa chỉ Email
            </h1>
            <p className="text-xs text-slate-500 leading-relaxed">
              Mã xác thực OTP gồm 6 chữ số đã được gửi tới email của bạn. Vui lòng nhập mã để kích hoạt tài khoản.
            </p>
          </div>

          {/* Email Box & Thay đổi email */}
          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3.5 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-500 font-medium flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-slate-400" />
                <span>Gửi tới:</span>
              </span>
              <button
                type="button"
                onClick={() => setIsEditingEmail(!isEditingEmail)}
                className="text-blue-600 hover:text-blue-700 font-bold underline underline-offset-2 cursor-pointer"
              >
                {isEditingEmail ? 'Xong' : 'Sửa email'}
              </button>
            </div>

            {isEditingEmail ? (
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Nhập email của bạn"
                className="w-full h-9 rounded-lg border border-slate-200 bg-white px-3 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:border-blue-500"
              />
            ) : (
              <div className="font-bold text-slate-800 text-sm tracking-wide">
                {maskEmail(email) || 'Chưa có email'}
              </div>
            )}
          </div>

          {/* Success / Status Banner */}
          {apiSuccessMessage && (
            <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-start gap-2.5 shadow-xs">
              {isResending ? (
                <RotateCw className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5 animate-spin" />
              ) : (
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              )}
              <div className="flex-1">
                <span className="text-emerald-700 leading-relaxed font-medium">
                  {apiSuccessMessage}
                </span>
              </div>
            </div>
          )}

          {/* Error Banner */}
          {apiError && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span className="font-bold block text-red-900">Lỗi xác thực:</span>
                <span className="text-red-700 leading-relaxed font-medium">
                  {apiError}
                </span>
              </div>
            </div>
          )}

          {/* Form nhập 6 ô OTP */}
          <form onSubmit={handleSubmit} className="space-y-6" noValidate>
            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-800 block text-center">
                Mã xác thực OTP (6 chữ số)
              </label>

              {/* 6 ô nhập số */}
              <div
                className="flex items-center justify-center gap-2 sm:gap-2.5"
                onPaste={handleOtpPaste}
              >
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    ref={(el) => (inputRefs.current[index] = el)}
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleOtpChange(index, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(index, e)}
                    disabled={isLoading}
                    className={`w-11 h-13 sm:w-12 sm:h-14 text-center text-xl font-extrabold rounded-2xl border bg-slate-50 text-slate-900 shadow-xs transition-all focus:outline-none focus:bg-white focus:ring-2 disabled:opacity-50 ${
                      otpError
                        ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
                        : digit
                        ? 'border-blue-600 focus:border-blue-600 focus:ring-blue-600/20 bg-blue-50/20'
                        : 'border-slate-200 focus:border-blue-500 focus:ring-blue-500/20'
                    }`}
                  />
                ))}
              </div>

              {otpError && (
                <p className="text-xs text-red-600 font-semibold text-center flex items-center justify-center gap-1 pt-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{otpError}</span>
                </p>
              )}
            </div>

            {/* Countdown & Resend Button */}
            <div className="text-center space-y-1">
              <p className="text-xs text-slate-500">Chưa nhận được mã OTP?</p>
              <button
                type="button"
                onClick={handleResendOtp}
                disabled={resendCooldown > 0 || isResending || !email.trim()}
                className="inline-flex items-center gap-1.5 text-xs font-bold text-blue-600 hover:text-blue-700 transition-colors disabled:text-slate-400 disabled:pointer-events-none cursor-pointer"
              >
                <RotateCw
                  className={`w-3.5 h-3.5 ${isResending ? 'animate-spin' : ''}`}
                />
                {isResending ? (
                  <span>Đang gửi lại mã...</span>
                ) : resendCooldown > 0 ? (
                  <span>Gửi lại sau ({resendCooldown}s)</span>
                ) : (
                  <span>Gửi lại mã OTP</span>
                )}
              </button>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading || otp.join('').length < 6}
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer disabled:opacity-50 disabled:pointer-events-none mt-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="text-white" />
                  <span>Đang xác thực...</span>
                </>
              ) : (
                <span>Xác thực & Kích hoạt tài khoản</span>
              )}
            </button>
          </form>

          {/* Footer Back Link */}
          <div className="pt-2 text-center text-xs text-slate-500 flex items-center justify-center gap-3">
            <Link
              to="/register"
              className="inline-flex items-center gap-1 font-semibold text-slate-600 hover:text-slate-900 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Đăng ký lại</span>
            </Link>
            <span>&bull;</span>
            <Link
              to="/login"
              className="font-bold text-blue-600 hover:text-blue-700 transition-colors"
            >
              Đăng nhập
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default VerifyEmailPage

