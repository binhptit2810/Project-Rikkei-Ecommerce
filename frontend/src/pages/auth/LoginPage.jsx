import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
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
    // Xóa lỗi của trường khi người dùng gõ
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
      const result = await login({
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
      })

      const loggedInUser = result?.user
      if (loggedInUser?.role === 'ADMIN') {
        // Tài khoản Admin -> chuyển đến trang Dashboard Admin /admin/dashboard
        const adminDest = location.state?.from?.pathname?.startsWith('/admin')
          ? location.state.from.pathname
          : '/admin/dashboard'
        navigate(adminDest, { replace: true })
      } else {
        // Tài khoản Customer -> chuyển về trang chủ Customer / (không vào /profile, không vào admin)
        const customerDest =
          location.state?.from?.pathname && !location.state?.from?.pathname?.startsWith('/admin')
            ? location.state.from.pathname
            : '/'
        navigate(customerDest, { replace: true })
      }
    } catch (err) {
      // Lỗi đã được set trong store và hiển thị trên UI
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        {/* Card Container */}
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="w-14 h-14 bg-orange-100 text-orange-600 rounded-2xl flex items-center justify-center mx-auto mb-3 shadow-inner text-2xl">
              🔐
            </div>
            <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">
              Đăng nhập tài khoản
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Nhập email và mật khẩu để tiếp tục sử dụng hệ thống
            </p>
          </div>

          {/* Success Banner */}
          {location.state?.successMessage && (
            <div className="mb-6 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-3 shadow-xs animate-in fade-in duration-200">
              <span className="text-base">✅</span>
              <span className="font-semibold">{location.state.successMessage}</span>
            </div>
          )}

          {/* Backend Error Banner */}
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-3.5 shadow-xs animate-in fade-in duration-200">
              <div className="w-8 h-8 rounded-xl bg-red-100 text-red-600 flex items-center justify-center text-base shrink-0 mt-0.5">
                {lockReason ? '🔒' : '⚠️'}
              </div>
              <div className="flex-1 space-y-1.5">
                {lockReason ? (
                  <>
                    <div className="font-extrabold text-red-900 text-sm leading-tight">
                      {error.endsWith('.') ? error : `${error}.`}
                    </div>
                    <div className="text-red-800 bg-red-100/70 p-3 rounded-xl border border-red-200/80 leading-relaxed text-xs">
                      <span className="font-bold text-red-900">Lý do:</span> {lockReason}
                    </div>
                    <p className="text-[11px] text-red-600 mt-0.5">
                      Vui lòng liên hệ Quản trị viên hệ thống để được hỗ trợ mở khóa tài khoản.
                    </p>
                  </>
                ) : (
                  <>
                    <span className="font-bold block text-red-900">Xác thực không thành công:</span>
                    <span className="text-red-700 leading-relaxed">{typeof error === 'object' ? error.message : error}</span>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {/* Email Field */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Email <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
                  </svg>
                </div>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="user@example.com"
                  autoComplete="email"
                  className={`w-full pl-10 pr-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.email
                      ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                      : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                    }`}
                />
              </div>
              {formErrors.email && (
                <p className="mt-1.5 text-xs text-red-600 font-medium flex items-center gap-1">
                  <span>⚠️</span> {formErrors.email}
                </p>
              )}
            </div>

            {/* Password Field */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Mật khẩu <span className="text-red-500">*</span>
                </label>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className={`w-full pl-10 pr-11 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.password
                      ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                      : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                    }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                  title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                >
                  {showPassword ? (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
                    </svg>
                  ) : (
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  )}
                </button>
              </div>
              {formErrors.password && (
                <p className="mt-1.5 text-xs text-red-600 font-medium flex items-center gap-1">
                  <span>⚠️</span> {formErrors.password}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 text-white font-bold py-3 px-4 rounded-xl shadow-md hover:shadow-lg active:scale-[0.99] transition-all text-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed mt-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Đang xác thực...</span>
                </>
              ) : (
                <span>Đăng nhập</span>
              )}
            </button>
          </form>

          {/* Footer Link to Register */}
          <div className="mt-8 pt-6 border-t border-slate-100 text-center">
            <p className="text-xs text-slate-600">
              Chưa có tài khoản?{' '}
              <Link
                to="/register"
                className="font-bold text-orange-600 hover:text-orange-700 transition-colors"
              >
                Đăng ký ngay
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
