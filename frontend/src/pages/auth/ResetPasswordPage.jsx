import React, { useState, useRef, useEffect } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Lock,
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle2,
  ArrowLeft,
  RotateCw,
  Zap,
  Info,
} from 'lucide-react'
import authService from '../../services/authService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const ResetPasswordPage = () => {
  const location = useLocation()
  const navigate = useNavigate()

  // Lấy email được truyền từ ForgotPasswordPage qua router state
  const email = location.state?.email || ''
  const infoMessage = location.state?.infoMessage || ''

  // State cho 6 ô nhập OTP
  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const inputRefs = useRef([])

  // State cho form mật khẩu
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  // Validation & Error states
  const [formErrors, setFormErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [apiSuccessMessage, setApiSuccessMessage] = useState(infoMessage)
  const [isLoading, setIsLoading] = useState(false)
  const [isSuccess, setIsSuccess] = useState(false)

  // Cooldown đếm ngược 60s cho nút gửi lại OTP
  const [resendCooldown, setResendCooldown] = useState(60)
  const [isResending, setIsResending] = useState(false)

  // Quản lý đếm ngược 60s
  useEffect(() => {
    if (resendCooldown <= 0) return
    const timer = setInterval(() => {
      setResendCooldown((prev) => prev - 1)
    }, 1000)
    return () => clearInterval(timer)
  }, [resendCooldown])

  // Che bớt email để bảo vệ quyền riêng tư người dùng
  const maskEmail = (str) => {
    if (!str || !str.includes('@')) return str
    const [name, domain] = str.split('@')
    if (name.length <= 2) return `${name}***@${domain}`
    return `${name[0]}***${name[name.length - 1]}@${domain}`
  }

  // Xử lý nhập từng ô OTP (chỉ nhận số, tự nhảy focus, giữ số 0 đầu)
  const handleOtpChange = (index, value) => {
    // Chỉ chấp nhận ký tự số
    const char = value.replace(/\D/g, '')

    const newOtp = [...otp]
    newOtp[index] = char ? char.slice(-1) : ''
    setOtp(newOtp)

    if (formErrors.otp) {
      setFormErrors((prev) => ({ ...prev, otp: '' }))
    }
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
        // Nếu ô hiện tại rỗng, quay lại ô trước và xóa
        inputRefs.current[index - 1]?.focus()
        const newOtp = [...otp]
        newOtp[index - 1] = ''
        setOtp(newOtp)
      } else if (otp[index]) {
        // Xóa ô hiện tại
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

    if (formErrors.otp) {
      setFormErrors((prev) => ({ ...prev, otp: '' }))
    }
    if (apiError) setApiError('')

    // Focus vào ô tiếp theo hoặc ô cuối cùng
    const focusIndex = Math.min(digits.length, 5)
    inputRefs.current[focusIndex]?.focus()
  }

  // Xử lý gửi lại mã OTP (Cool-down 60s)
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || isResending || !email) return

    setIsResending(true)
    setApiError('')

    try {
      const res = await authService.forgotPassword({ email })
      setApiSuccessMessage(
        res?.message || 'Mã OTP mới đã được gửi đến email của bạn.'
      )
      setResendCooldown(60)
      // Reset lại các ô OTP
      setOtp(['', '', '', '', '', ''])
      inputRefs.current[0]?.focus()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        'Không thể gửi lại mã OTP lúc này. Vui lòng thử lại sau.'
      setApiError(msg)
    } finally {
      setIsResending(false)
    }
  }

  // Validate form client-side
  const validateForm = () => {
    const errors = {}
    const otpValue = otp.join('')

    if (!otpValue || otpValue.length < 6) {
      errors.otp = 'Vui lòng nhập đủ 6 chữ số mã OTP'
    }

    if (!newPassword) {
      errors.newPassword = 'Vui lòng nhập mật khẩu mới'
    } else if (newPassword.length < 6) {
      errors.newPassword = 'Mật khẩu phải từ 6 đến 100 ký tự'
    } else if (newPassword.length > 100) {
      errors.newPassword = 'Mật khẩu không được vượt quá 100 ký tự'
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới'
    } else if (newPassword && newPassword !== confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  // Submit form đặt lại mật khẩu
  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validateForm() || isLoading) return

    setIsLoading(true)
    setApiError('')

    const payload = {
      email,
      otp: otp.join(''),
      newPassword,
      confirmPassword,
    }

    try {
      await authService.resetPassword(payload)
      setIsSuccess(true)
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        'Không thể đặt lại mật khẩu. Vui lòng kiểm tra lại mã OTP.'
      setApiError(msg)

      // Nếu lỗi liên quan đến OTP sai hoặc hết hạn -> highlight lỗi vào ô OTP
      if (
        msg.toLowerCase().includes('otp') ||
        msg.toLowerCase().includes('mã')
      ) {
        setFormErrors((prev) => ({ ...prev, otp: msg }))
      }
    } finally {
      setIsLoading(false)
    }
  }

  // TRƯỜNG HỢP 1: Người dùng truy cập trực tiếp route mà không có email
  if (!email) {
    return (
      <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6 text-center">
            <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center mx-auto">
              <Info className="w-6 h-6" />
            </div>
            <div className="space-y-2">
              <h2 className="text-xl font-extrabold text-slate-900">
                Chưa có yêu cầu đặt lại mật khẩu
              </h2>
              <p className="text-xs text-slate-500 leading-relaxed">
                Vui lòng nhập địa chỉ email của bạn trước để chúng tôi gửi mã xác
                thực OTP.
              </p>
            </div>
            <Link
              to="/forgot-password"
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98]"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại trang Quên mật khẩu</span>
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // TRƯỜNG HỢP 2: Đặt lại mật khẩu thành công
  if (isSuccess) {
    return (
      <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6 text-center">
            <div className="w-14 h-14 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-sm">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl font-extrabold text-slate-900">
                Đặt lại mật khẩu thành công!
              </h2>
              <p className="text-xs text-slate-500 leading-relaxed">
                Mật khẩu của tài khoản <strong>{email}</strong> đã được cập
                nhật thành công. Toàn bộ phiên đăng nhập cũ đã được thu hồi an
                toàn.
              </p>
            </div>
            <button
              type="button"
              onClick={() =>
                navigate('/login', {
                  replace: true,
                  state: {
                    successMessage:
                      'Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.',
                  },
                })
              }
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer"
            >
              <span>Đăng nhập ngay</span>
            </button>
          </div>
        </div>
      </div>
    )
  }

  // TRƯỜNG HỢP 3: Form xác thực OTP & Nhập mật khẩu mới
  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6">
          {/* Logo & Header */}
          <div className="space-y-2 text-center">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center mx-auto shadow-md shadow-blue-500/25">
              <Zap className="w-6 h-6 fill-current text-white" />
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 pt-1">
              Xác thực OTP & Đổi mật khẩu
            </h1>
            <p className="text-xs text-slate-500 leading-relaxed">
              Mã xác thực gồm 6 chữ số đã được gửi tới{' '}
              <span className="font-bold text-slate-800">
                {maskEmail(email)}
              </span>
              . Hiệu lực trong 5 phút.
            </p>
          </div>

          {/* Success / Info Banner */}
          {apiSuccessMessage && (
            <div className="p-3.5 rounded-2xl bg-blue-50 border border-blue-200 text-blue-900 text-xs flex items-start gap-2 shadow-xs">
              <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
              <span className="font-medium leading-relaxed">
                {apiSuccessMessage}
              </span>
            </div>
          )}

          {/* Error Banner */}
          {apiError && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span className="font-bold block text-red-900">Lỗi:</span>
                <span className="text-red-700 leading-relaxed font-medium">
                  {apiError}
                </span>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {/* 6 Ô NHẬP MÃ OTP */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-800 block">
                  Mã xác thực OTP (6 chữ số)
                </label>
                {/* Nút Gửi lại mã OTP */}
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={resendCooldown > 0 || isResending}
                  className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 disabled:text-slate-400 disabled:pointer-events-none transition-colors"
                >
                  <RotateCw
                    className={`w-3 h-3 ${isResending ? 'animate-spin' : ''}`}
                  />
                  {resendCooldown > 0 ? (
                    <span>Gửi lại sau {resendCooldown}s</span>
                  ) : (
                    <span>Gửi lại mã OTP</span>
                  )}
                </button>
              </div>

              {/* Dãy 6 ô input */}
              <div
                className="flex items-center justify-between gap-1.5 sm:gap-2"
                onPaste={handleOtpPaste}
              >
                {otp.map((digit, idx) => (
                  <input
                    key={idx}
                    ref={(el) => (inputRefs.current[idx] = el)}
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleOtpChange(idx, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                    disabled={isLoading}
                    className={`w-11 h-12 sm:w-12 sm:h-13 text-center text-xl font-bold rounded-xl border bg-slate-50 transition-all focus:outline-none focus:bg-white focus:ring-2 disabled:opacity-60 ${
                      formErrors.otp || apiError
                        ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20 text-red-600'
                        : digit
                        ? 'border-blue-500 bg-blue-50/30 text-blue-900 focus:ring-blue-500/20'
                        : 'border-slate-200 text-slate-900 focus:border-blue-500 focus:ring-blue-500/20'
                    }`}
                  />
                ))}
              </div>
              {formErrors.otp && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.otp}</span>
                </p>
              )}
            </div>

            {/* MẬT KHẨU MỚI */}
            <div className="space-y-1.5">
              <label
                htmlFor="new-password"
                className="text-xs font-bold text-slate-800 block"
              >
                Mật khẩu mới
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  id="new-password"
                  type={showPassword ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value)
                    if (formErrors.newPassword) {
                      setFormErrors((prev) => ({ ...prev, newPassword: '' }))
                    }
                    if (apiError) setApiError('')
                  }}
                  placeholder="Tối thiểu 6 ký tự"
                  autoComplete="new-password"
                  disabled={isLoading}
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-11 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:opacity-60 ${
                    formErrors.newPassword
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                  title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                >
                  {showPassword ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              </div>
              {formErrors.newPassword && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.newPassword}</span>
                </p>
              )}
            </div>

            {/* XÁC NHẬN MẬT KHẨU MỚI */}
            <div className="space-y-1.5">
              <label
                htmlFor="confirm-password"
                className="text-xs font-bold text-slate-800 block"
              >
                Xác nhận mật khẩu mới
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  id="confirm-password"
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value)
                    if (formErrors.confirmPassword) {
                      setFormErrors((prev) => ({
                        ...prev,
                        confirmPassword: '',
                      }))
                    }
                    if (apiError) setApiError('')
                  }}
                  placeholder="Nhập lại mật khẩu mới"
                  autoComplete="new-password"
                  disabled={isLoading}
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-11 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:opacity-60 ${
                    formErrors.confirmPassword
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                  title={
                    showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'
                  }
                >
                  {showConfirmPassword ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              </div>
              {formErrors.confirmPassword && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.confirmPassword}</span>
                </p>
              )}
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
                  <span>Đang đặt lại mật khẩu...</span>
                </>
              ) : (
                <span>Đặt lại mật khẩu</span>
              )}
            </button>
          </form>

          {/* Links */}
          <div className="pt-2 flex items-center justify-between text-xs font-bold text-slate-600">
            <Link
              to="/forgot-password"
              className="inline-flex items-center gap-1 hover:text-blue-600 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Đổi email khác</span>
            </Link>
            <Link
              to="/login"
              className="hover:text-blue-600 transition-colors"
            >
              Quay lại đăng nhập
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ResetPasswordPage

