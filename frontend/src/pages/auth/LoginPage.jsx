import React, { useState } from 'react'
import { Link, useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import {
  Lock,
  Mail,
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle2,
  ShieldAlert,
  Info,
  Zap,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import { useAuth0 } from '@auth0/auth0-react'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const LoginPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const { loginWithPopup, getIdTokenClaims } = useAuth0()
  const { login, loginWithAuth0, isLoading, error, lockReason, isUnverified, unverifiedEmail, clearError } = useAuthStore()

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })

  const [formErrors, setFormErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [isGoogleLoading, setIsGoogleLoading] = useState(false)

  const oauth2ErrorParam = searchParams.get('oauth2Error') || searchParams.get('error')

  const handleGoogleLogin = async () => {
    setIsGoogleLoading(true)
    clearError()

    const hasAuth0Config =
      import.meta.env.VITE_AUTH0_DOMAIN &&
      import.meta.env.VITE_AUTH0_CLIENT_ID &&
      !import.meta.env.VITE_AUTH0_DOMAIN.includes('placeholder')

    if (!hasAuth0Config) {
      // Fallback khi chưa cấu hình Auth0: dùng luồng Spring OAuth2 hiện tại
      window.location.href = '/oauth2/authorization/google'
      return
    }

    try {
      // Xác thực Google qua Auth0 với Authorization Code Flow + PKCE
      await loginWithPopup({
        authorizationParams: {
          connection: 'google-oauth2',
        },
      })

      // Lấy ID Token được ký RS256 từ Auth0
      const claims = await getIdTokenClaims()
      if (!claims || !claims.__raw) {
        throw new Error('Không nhận được mã xác thực Auth0 ID Token')
      }

      // Gửi ID Token đến Backend để trao đổi lấy Access Token và Refresh Token nội bộ
      await loginWithAuth0(claims.__raw)

      // Chuyển hướng về trang đích hoặc trang chủ
      const customerDest =
        location.state?.from?.pathname &&
        !location.state?.from?.pathname?.startsWith('/admin')
          ? location.state.from.pathname
          : '/'
      navigate(customerDest, { replace: true })
    } catch (err) {
      if (err.message?.includes('closed') || err.error === 'popup_closed_by_user') {
        // Người dùng chủ động đóng popup, không hiển thị lỗi
      } else {
        // Lỗi đã được store loginWithAuth0 bắt và set vào state error
      }
    } finally {
      setIsGoogleLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }))
    }
    if (error || lockReason || isUnverified) {
      clearError()
    }
  }

  const validate = () => {
    const errors = {}
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

    if (!formData.email.trim()) {
      errors.email = 'Vui lòng nhập địa chỉ email'
    } else if (!emailRegex.test(formData.email.trim())) {
      errors.email = 'Email không đúng định dạng (VD: user@example.com)'
    }

    if (!formData.password) {
      errors.password = 'Vui lòng nhập mật khẩu'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    try {
      await login(
        {
          email: formData.email.trim().toLowerCase(),
          password: formData.password,
        },
        'CUSTOMER'
      )

      // Đăng nhập CUSTOMER thành công -> chuyển về trang đích hoặc trang chủ
      const customerDest =
        location.state?.from?.pathname &&
        !location.state?.from?.pathname?.startsWith('/admin')
          ? location.state.from.pathname
          : '/'
      navigate(customerDest, { replace: true })
    } catch (err) {
      // Store tự động xử lý lỗi
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
              Đăng nhập tài khoản
            </h1>
            <p className="text-xs text-slate-500">
              Nhập email và mật khẩu để quản lý giỏ hàng & đơn hàng của bạn
            </p>
          </div>

          {/* Require Login / Cart Prompt Banner */}
          {location.state?.message && (
            <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs flex flex-wrap items-center justify-between gap-2 shadow-xs">
              <div className="flex items-center gap-2">
                <Info className="w-4 h-4 text-amber-600 shrink-0" />
                <span className="font-semibold">{location.state.message}</span>
              </div>
              <Link
                to="/register"
                className="text-xs font-bold text-amber-950 underline underline-offset-2 shrink-0 hover:text-amber-700"
              >
                Đăng ký ngay &rarr;
              </Link>
            </div>
          )}

          {/* Success Banner */}
          {location.state?.successMessage && (
            <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2 shadow-xs">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span className="font-bold">{location.state.successMessage}</span>
            </div>
          )}

          {/* Unverified Account Warning Banner */}
          {isUnverified && (
            <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-amber-900 text-xs shadow-xs space-y-3">
              <div className="flex items-start gap-2.5">
                <Mail className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div className="flex-1 space-y-1">
                  <div className="font-bold text-amber-950">
                    Tài khoản chưa kích hoạt
                  </div>
                  <p className="text-amber-800 leading-relaxed font-medium">
                    {error || 'Địa chỉ email này chưa được xác thực. Vui lòng nhập mã OTP để kích hoạt tài khoản của bạn.'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() =>
                  navigate('/verify-email', {
                    state: {
                      email: unverifiedEmail || formData.email.trim(),
                      autoResend: true,
                    },
                  })
                }
                className="w-full inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs transition-colors shadow-xs cursor-pointer"
              >
                <span>Xác thực email ngay</span>
                <span aria-hidden="true">&rarr;</span>
              </button>
            </div>
          )}

          {/* Backend Error Banner */}
          {error && !isUnverified && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <div className="shrink-0 mt-0.5">
                {lockReason ? <ShieldAlert className="w-4 h-4 text-red-600" /> : <AlertCircle className="w-4 h-4 text-red-600" />}
              </div>
              <div className="flex-1 space-y-1">
                {lockReason ? (
                  <>
                    <div className="font-bold text-red-900">
                      {error.endsWith('.') ? error : `${error}.`}
                    </div>
                    <div className="text-red-800 bg-red-100/60 p-2 rounded-xl border border-red-200 text-xs">
                      <span className="font-bold">Lý do:</span> {lockReason}
                    </div>
                    <p className="text-[11px] text-red-700">
                      Vui lòng liên hệ Quản trị viên để được hỗ trợ mở khóa.
                    </p>
                  </>
                ) : (
                  <>
                    <span className="font-bold block text-red-900">Lỗi xác thực:</span>
                    <span className="text-red-700 leading-relaxed font-medium">
                      {typeof error === 'object' ? error.message : error}
                    </span>
                  </>
                )}
              </div>
            </div>
          )}

          {/* OAuth2 Error Banner [TSK-D15-CCT] */}
          {oauth2ErrorParam && !error && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <div className="shrink-0 mt-0.5">
                <AlertCircle className="w-4 h-4 text-red-600" />
              </div>
              <div className="flex-1 space-y-1">
                <span className="font-bold block text-red-900">Đăng nhập Google không thành công:</span>
                <span className="text-red-700 leading-relaxed font-medium">
                  {oauth2ErrorParam}
                </span>
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Email Field */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-800 block">
                Địa chỉ Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="name@example.com"
                  autoComplete="email"
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                    formErrors.email
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
              </div>
              {formErrors.email && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.email}</span>
                </p>
              )}
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-800 block">
                  Mật khẩu
                </label>
                <Link
                  to="/forgot-password"
                  className="text-xs font-semibold text-blue-600 hover:text-blue-700 underline underline-offset-2 transition-colors"
                >
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-11 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                    formErrors.password
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
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {formErrors.password && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.password}</span>
                </p>
              )}
            </div>

            {/* Submit Button: Royal Blue Pill */}
            <button
              type="submit"
              disabled={isLoading || isGoogleLoading}
              className="inline-flex items-center justify-center gap-2 h-11 w-full rounded-full bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-500/20 transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer disabled:opacity-50 disabled:pointer-events-none mt-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="text-white" />
                  <span>Đang đăng nhập...</span>
                </>
              ) : (
                <span>Đăng nhập</span>
              )}
            </button>
          </form>

          {/* Divider [TSK-D15-CCT] */}
          <div className="relative flex items-center justify-center my-2">
            <div className="border-t border-slate-200 w-full" />
            <span className="bg-white px-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider shrink-0">
              Hoặc đăng nhập nhanh bằng
            </span>
            <div className="border-t border-slate-200 w-full" />
          </div>

          {/* Google Login Button [TSK-D15-CCT] */}
          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={isLoading || isGoogleLoading}
            className="inline-flex items-center justify-center gap-3 h-11 w-full rounded-full border border-slate-300/90 bg-white hover:bg-slate-50 text-slate-700 text-sm font-bold shadow-xs transition-all hover:scale-[1.01] active:scale-[0.98] cursor-pointer disabled:opacity-50 disabled:pointer-events-none"
          >
            {isGoogleLoading ? (
              <>
                <LoadingSpinner size="sm" className="text-slate-600" />
                <span>Đang kết nối Google...</span>
              </>
            ) : (
              <>
                <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                  <path
                    fill="#4285F4"
                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  />
                  <path
                    fill="#34A853"
                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  />
                  <path
                    fill="#FBBC05"
                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                  />
                  <path
                    fill="#EA4335"
                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                  />
                </svg>
                <span>Đăng nhập bằng Google</span>
              </>
            )}
          </button>

          {/* Footer Link to Register */}
          <div className="pt-2 text-center text-xs text-slate-500">
            Chưa có tài khoản?{' '}
            <Link
              to="/register"
              className="font-bold text-blue-600 hover:text-blue-700 underline underline-offset-4 transition-colors"
            >
              Đăng ký tài khoản mới
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
