import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Shield,
  Lock,
  Mail,
  Eye,
  EyeOff,
  AlertCircle,
  ShieldAlert,
  KeyRound,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang đăng nhập độc lập dành riêng cho Quản trị viên (Admin Portal).
 * Thiết kế theo chuẩn shadcn/ui New York Style (Neutral / Zinc).
 */
export const AdminLoginPage = () => {
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
      errors.email = 'Vui lòng nhập email quản trị viên'
    } else if (!emailRegex.test(formData.email.trim())) {
      errors.email = 'Email không đúng định dạng (VD: admin@example.com)'
    }

    if (!formData.password) {
      errors.password = 'Vui lòng nhập mật khẩu quản trị'
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
        'ADMIN'
      )

      const adminDest =
        location.state?.from?.pathname?.startsWith('/admin') &&
        location.state?.from?.pathname !== '/admin/login'
          ? location.state.from.pathname
          : '/admin/dashboard'
      navigate(adminDest, { replace: true })
    } catch (err) {
      // Store xử lý thiết lập thông báo lỗi và thu hồi phiên sai quyền
    }
  }

  const isCustomerAttempt =
    error &&
    typeof error === 'string' &&
    (error.includes('không có quyền Quản trị') || error.includes('CUSTOMER'))

  return (
    <div className="min-h-screen bg-zinc-950 flex flex-col justify-between font-sans text-zinc-100 antialiased selection:bg-zinc-800 selection:text-white">
      {/* Top Header / Back to Store Link */}
      <header className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between border-b border-zinc-900">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-950 flex items-center justify-center font-bold">
            <Shield className="w-4 h-4 text-zinc-900" />
          </div>
          <div>
            <span className="font-semibold text-sm tracking-tight text-white block leading-none">
              Rikkei Store
            </span>
            <span className="text-[10px] font-medium text-zinc-500 uppercase tracking-wider block mt-0.5">
              Admin Portal
            </span>
          </div>
        </div>
      </header>

      {/* Main Content Form */}
      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm">
          {/* Admin Login Card */}
          <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-6 sm:p-8 space-y-6 shadow-sm">
            {/* Heading */}
            <div className="space-y-1.5 text-center">
              <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-zinc-800 text-zinc-300 text-[11px] font-medium mb-2 border border-zinc-700">
                <KeyRound className="w-3 h-3 text-zinc-400" />
                <span>Khu vực bảo mật</span>
              </div>
              <h1 className="text-xl font-semibold text-white tracking-tight">
                Đăng nhập Quản trị
              </h1>
              <p className="text-xs text-zinc-400">
                Chỉ dành riêng cho tài khoản Quản trị viên (ADMIN)
              </p>
            </div>

            {/* Error Alert */}
            {error && (
              <div className="p-3 rounded-md bg-red-950/50 border border-red-800/80 text-red-200 text-xs flex items-start gap-2.5">
                <div className="shrink-0 mt-0.5">
                  {lockReason ? <ShieldAlert className="w-4 h-4 text-red-400" /> : <AlertCircle className="w-4 h-4 text-red-400" />}
                </div>
                <div className="flex-1 space-y-1">
                  {lockReason ? (
                    <>
                      <div className="font-semibold text-red-100">
                        Tài khoản đã bị khóa
                      </div>
                      <div className="text-red-300 bg-red-900/30 p-2 rounded border border-red-800/50 text-[11px]">
                        <span className="font-semibold">Lý do:</span> {lockReason}
                      </div>
                    </>
                  ) : (
                    <>
                      <span className="font-semibold block text-red-100">
                        {isCustomerAttempt ? 'Truy cập bị từ chối' : 'Lỗi xác thực'}
                      </span>
                      <p className="text-red-300 leading-relaxed">
                        {typeof error === 'object' ? error.message : error}
                      </p>
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Login Form */}
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              {/* Email */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-zinc-300 block">
                  Email Quản trị
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="admin@eshop.com"
                    autoComplete="email"
                    className={`flex h-9 w-full rounded-md border bg-zinc-950 pl-9 pr-3 py-1 text-sm text-zinc-100 shadow-sm transition-colors placeholder:text-zinc-600 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-400 ${
                      formErrors.email
                        ? 'border-red-500'
                        : 'border-zinc-800'
                    }`}
                  />
                </div>
                {formErrors.email && (
                  <p className="text-xs text-red-400 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{formErrors.email}</span>
                  </p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-zinc-300 block">
                  Mật khẩu
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="••••••••••••"
                    autoComplete="current-password"
                    className={`flex h-9 w-full rounded-md border bg-zinc-950 pl-9 pr-10 py-1 text-sm text-zinc-100 shadow-sm transition-colors placeholder:text-zinc-600 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-400 ${
                      formErrors.password
                        ? 'border-red-500'
                        : 'border-zinc-800'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-zinc-500 hover:text-zinc-300 cursor-pointer focus:outline-none"
                    title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {formErrors.password && (
                  <p className="text-xs text-red-400 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{formErrors.password}</span>
                  </p>
                )}
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={isLoading}
                className="inline-flex items-center justify-center gap-2 h-9 w-full rounded-md bg-zinc-100 text-zinc-950 hover:bg-white text-sm font-medium transition-colors shadow-sm cursor-pointer disabled:opacity-50 disabled:pointer-events-none mt-2"
              >
                {isLoading ? (
                  <>
                    <LoadingSpinner size="sm" />
                    <span>Đang xác thực...</span>
                  </>
                ) : (
                  <>
                    <Shield className="w-3.5 h-3.5 text-zinc-950" />
                    <span>Đăng nhập</span>
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="py-4 text-center text-zinc-500 text-xs border-t border-zinc-900">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>&copy; {new Date().getFullYear()} Rikkei E-Commerce Platform. All Rights Reserved.</span>
          <span className="text-[11px] text-zinc-600">RBAC Protected · JWT Authentication</span>
        </div>
      </footer>
    </div>
  )
}

export default AdminLoginPage
