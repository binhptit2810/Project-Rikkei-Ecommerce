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

      // Đăng nhập CUSTOMER thành công -> chuyển về trang chủ hoặc trang khách hàng trước đó
      const customerDest =
        location.state?.from?.pathname &&
        !location.state?.from?.pathname?.startsWith('/admin')
          ? location.state.from.pathname
          : '/'
      navigate(customerDest, { replace: true })
    } catch (err) {
      // Store xử lý thiết lập thông báo lỗi và thu hồi phiên sai quyền
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12 bg-zinc-50/50">
      <div className="w-full max-w-sm">
        {/* Card Container */}
        <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 sm:p-8 space-y-6">
          {/* Header */}
          <div className="space-y-1.5 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-zinc-950">
              Đăng nhập
            </h1>
            <p className="text-sm text-zinc-500">
              Nhập email và mật khẩu để truy cập tài khoản của bạn
            </p>
          </div>

          {/* Success Banner */}
          {location.state?.successMessage && (
            <div className="p-3 rounded-md bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span className="font-medium">{location.state.successMessage}</span>
            </div>
          )}

          {/* Backend Error Banner */}
          {error && (
            <div className="p-3 rounded-md bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5">
              <div className="shrink-0 mt-0.5">
                {lockReason ? <ShieldAlert className="w-4 h-4 text-red-600" /> : <AlertCircle className="w-4 h-4 text-red-600" />}
              </div>
              <div className="flex-1 space-y-1">
                {lockReason ? (
                  <>
                    <div className="font-semibold text-red-900">
                      {error.endsWith('.') ? error : `${error}.`}
                    </div>
                    <div className="text-red-800 bg-red-100/60 p-2 rounded border border-red-200 text-xs">
                      <span className="font-semibold">Lý do:</span> {lockReason}
                    </div>
                    <p className="text-[11px] text-red-700">
                      Vui lòng liên hệ Quản trị viên để được hỗ trợ mở khóa.
                    </p>
                  </>
                ) : (
                  <>
                    <span className="font-semibold block text-red-900">Lỗi xác thực:</span>
                    <span className="text-red-700 leading-relaxed">
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
              <label className="text-sm font-medium text-zinc-900 block">
                Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="name@example.com"
                  autoComplete="email"
                  className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-3 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                    formErrors.email
                      ? 'border-red-500 focus-visible:ring-red-500'
                      : 'border-zinc-200'
                  }`}
                />
              </div>
              {formErrors.email && (
                <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.email}</span>
                </p>
              )}
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-zinc-900 block">
                Mật khẩu
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-10 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                    formErrors.password
                      ? 'border-red-500 focus-visible:ring-red-500'
                      : 'border-zinc-200'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-zinc-400 hover:text-zinc-600 cursor-pointer focus:outline-none"
                  title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {formErrors.password && (
                <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.password}</span>
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="inline-flex items-center justify-center gap-2 h-9 w-full rounded-md bg-zinc-900 text-zinc-50 hover:bg-zinc-800 text-sm font-medium transition-colors shadow-sm cursor-pointer disabled:opacity-50 disabled:pointer-events-none mt-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Đang đăng nhập...</span>
                </>
              ) : (
                <span>Đăng nhập</span>
              )}
            </button>
          </form>

          {/* Footer Link to Register */}
          <div className="pt-2 text-center text-sm text-zinc-500">
            Chưa có tài khoản?{' '}
            <Link
              to="/register"
              className="font-medium text-zinc-950 underline underline-offset-4 hover:text-zinc-800 transition-colors"
            >
              Đăng ký ngay
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
