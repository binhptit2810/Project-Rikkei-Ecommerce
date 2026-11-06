import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
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
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const LoginPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { login, isLoading, error, lockReason, clearError } = useAuthStore()

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })

  const [formErrors, setFormErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }))
    }
    if (error || lockReason) {
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

          {/* Backend Error Banner */}
          {error && (
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
              <label className="text-xs font-bold text-slate-800 block">
                Mật khẩu
              </label>
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
              disabled={isLoading}
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
