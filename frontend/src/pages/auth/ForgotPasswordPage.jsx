import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, AlertCircle, ArrowLeft, Zap } from 'lucide-react'
import authService from '../../services/authService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const ForgotPasswordPage = () => {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')
  const [apiError, setApiError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const validate = () => {
    const trimmed = email.trim()
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

    if (!trimmed) {
      setEmailError('Vui lòng nhập địa chỉ email')
      return false
    }
    if (!emailRegex.test(trimmed)) {
      setEmailError('Email không đúng định dạng (VD: user@example.com)')
      return false
    }
    setEmailError('')
    return true
  }

  const handleChange = (e) => {
    setEmail(e.target.value)
    if (emailError) setEmailError('')
    if (apiError) setApiError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate() || isLoading) return

    setIsLoading(true)
    setApiError('')

    const normalizedEmail = email.trim().toLowerCase()

    try {
      const res = await authService.forgotPassword({ email: normalizedEmail })
      // Chuyển sang bước nhập OTP và đặt lại mật khẩu kèm thông điệp từ server
      navigate('/reset-password', {
        state: {
          email: normalizedEmail,
          infoMessage:
            res?.message ||
            'Nếu email tồn tại trong hệ thống và hợp lệ, bạn sẽ nhận được mã OTP qua email.',
        },
      })
    } catch (err) {
      const message =
        err.response?.data?.message ||
        'Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.'
      setApiError(message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
      <div className="w-full max-w-md">
        {/* Card Container */}
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6">
          {/* Logo & Header */}
          <div className="space-y-2 text-center">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center mx-auto shadow-md shadow-blue-500/25">
              <Zap className="w-6 h-6 fill-current text-white" />
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 pt-1">
              Quên mật khẩu?
            </h1>
            <p className="text-xs text-slate-500 leading-relaxed">
              Nhập địa chỉ email đã đăng ký. Chúng tôi sẽ gửi mã xác thực OTP gồm
              6 chữ số để bạn đặt lại mật khẩu mới.
            </p>
          </div>

          {/* Backend Error Banner */}
          {apiError && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div className="flex-1">
                <span className="font-bold block text-red-900">Thông báo:</span>
                <span className="text-red-700 leading-relaxed font-medium">
                  {apiError}
                </span>
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Email Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="forgot-email"
                className="text-xs font-bold text-slate-800 block"
              >
                Địa chỉ Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  id="forgot-email"
                  type="email"
                  name="email"
                  value={email}
                  onChange={handleChange}
                  placeholder="name@example.com"
                  autoComplete="email"
                  disabled={isLoading}
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:opacity-60 ${
                    emailError
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
              </div>
              {emailError && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{emailError}</span>
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer disabled:opacity-50 disabled:pointer-events-none mt-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="text-white" />
                  <span>Đang gửi mã OTP...</span>
                </>
              ) : (
                <span>Gửi mã OTP</span>
              )}
            </button>
          </form>

          {/* Back to Login Link */}
          <div className="pt-2 text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-blue-600 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Quay lại đăng nhập</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ForgotPasswordPage

