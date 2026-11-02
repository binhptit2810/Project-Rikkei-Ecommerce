import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import {
  Shield,
  Lock,
  Mail,
  Eye,
  EyeOff,
  AlertCircle,
  ShieldAlert,
  ArrowRight,
  ArrowLeft,
  KeyRound,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang đăng nhập độc lập dành riêng cho Quản trị viên (Admin Portal).
 * Route: /admin/login
 * 
 * Đặc điểm kiến trúc:
 * 1. Hoàn toàn độc lập với Customer Layout (không chứa Navbar/Footer mua sắm).
 * 2. Thiết kế giao diện Executive Dark Mode với phong cách an ninh mạng chuyên nghiệp.
 * 3. Kiểm soát quyền nghiêm ngặt: Chỉ tài khoản có role ADMIN mới được cấp quyền truy cập.
 * 4. Nếu tài khoản CUSTOMER đăng nhập đúng mật khẩu, hệ thống từ chối và thu hồi phiên an toàn.
 * 5. Đăng nhập thành công chuyển hướng đến /admin/dashboard.
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
      // Đối chiếu bắt buộc role ADMIN ngay sau khi Backend trả về User
      await login(
        {
          email: formData.email.trim().toLowerCase(),
          password: formData.password,
        },
        'ADMIN'
      )

      // Chuyển hướng vào trang quản trị (ưu tiên điểm đến trước đó nếu có)
      const adminDest =
        location.state?.from?.pathname?.startsWith('/admin') &&
        location.state?.from?.pathname !== '/admin/login'
          ? location.state.from.pathname
          : '/admin/dashboard'
      navigate(adminDest, { replace: true })
    } catch (err) {
      // useAuthStore đã thu hồi phiên nếu không phải ADMIN và thiết lập thông báo lỗi chi tiết
    }
  }

  const isCustomerAttempt =
    error &&
    typeof error === 'string' &&
    (error.includes('không có quyền Quản trị') || error.includes('CUSTOMER'))

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-between relative overflow-hidden font-sans text-slate-100 antialiased selection:bg-indigo-500 selection:text-white">
      {/* Background Ambient Glows & Grid Pattern */}
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b15_1px,transparent_1px),linear-gradient(to_bottom,#1e293b15_1px,transparent_1px)] bg-[size:32px_32px] pointer-events-none" />
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-violet-600/15 rounded-full blur-3xl pointer-events-none" />

      {/* Top Header / Back to Store Link */}
      <header className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-18 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20">
            <Shield className="w-5 h-5 stroke-[2.2]" />
          </div>
          <div>
            <span className="font-extrabold text-sm sm:text-base tracking-tight text-white block">
              Rikkei<span className="text-indigo-400">Admin</span>
            </span>
            <span className="text-[10px] font-mono tracking-wider text-slate-400 uppercase block">
              Control Portal v1.0
            </span>
          </div>
        </div>

        <Link
          to="/"
          className="inline-flex items-center gap-2 text-xs font-medium text-slate-400 hover:text-white bg-slate-900/60 hover:bg-slate-800/80 border border-slate-800/80 px-3.5 py-2 rounded-xl transition-all shadow-xs"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Quay lại</span> Cửa hàng
        </Link>
      </header>

      {/* Main Content Form */}
      <main className="relative z-10 flex-1 flex items-center justify-center px-4 py-8">
        <div className="w-full max-w-md">
          {/* Admin Login Card */}
          <div className="bg-slate-900/80 backdrop-blur-xl border border-slate-800 rounded-3xl shadow-2xl p-8 sm:p-10 relative">
            {/* Top Accent Strip */}
            <div className="absolute top-0 left-8 right-8 h-1 bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-500 rounded-b-full shadow-sm shadow-indigo-500/50" />

            {/* Portal Badge & Heading */}
            <div className="text-center mb-8">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-[11px] font-semibold uppercase tracking-wider mb-4">
                <KeyRound className="w-3.5 h-3.5 text-indigo-400" />
                <span>Khu vực bảo mật cao</span>
              </div>
              <h1 className="text-2xl font-black text-white tracking-tight">
                Cổng Quản Trị Hệ Thống
              </h1>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                Đăng nhập tài khoản Quản trị viên (ADMIN) để truy cập bảng điều khiển và vận hành nền tảng
              </p>
            </div>

            {/* Backend / Role Error Alert */}
            {error && (
              <div className="mb-6 p-4 rounded-2xl bg-rose-950/40 border border-rose-800/60 text-rose-200 text-xs flex items-start gap-3 shadow-lg animate-in fade-in zoom-in-95 duration-200">
                <div className="w-7 h-7 rounded-xl bg-rose-900/60 text-rose-400 flex items-center justify-center shrink-0 mt-0.5 border border-rose-800/50">
                  {lockReason ? <ShieldAlert className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                </div>
                <div className="flex-1 space-y-1.5">
                  {lockReason ? (
                    <>
                      <div className="font-bold text-rose-100 text-sm">
                        Tài khoản quản trị đã bị khóa
                      </div>
                      <div className="text-rose-300 bg-rose-900/40 p-2.5 rounded-xl border border-rose-800/40 leading-relaxed text-[11px]">
                        <span className="font-bold text-rose-200">Lý do:</span> {lockReason}
                      </div>
                    </>
                  ) : (
                    <>
                      <span className="font-bold block text-rose-100 text-sm">
                        {isCustomerAttempt ? 'Truy cập bị từ chối' : 'Xác thực không thành công'}
                      </span>
                      <p className="text-rose-300 leading-relaxed">
                        {typeof error === 'object' ? error.message : error}
                      </p>
                      {isCustomerAttempt && (
                        <div className="pt-2">
                          <Link
                            to="/login"
                            className="inline-flex items-center gap-1.5 font-bold text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 px-3 py-1.5 rounded-lg text-[11px] transition-colors"
                          >
                            <span>Chuyển sang Cổng Khách hàng</span>
                            <ArrowRight className="w-3.5 h-3.5 text-indigo-400" />
                          </Link>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Login Form */}
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              {/* Email */}
              <div>
                <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-1.5">
                  Email Quản trị <span className="text-rose-400">*</span>
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="admin@rikkei.vn"
                    autoComplete="email"
                    className={`w-full pl-10 pr-4 py-2.5 text-sm bg-slate-950/80 border rounded-xl focus:outline-none focus:ring-2 transition-all placeholder:text-slate-600 ${
                      formErrors.email
                        ? 'border-rose-500/80 focus:ring-rose-500/30 text-rose-200'
                        : 'border-slate-800 focus:border-indigo-500 focus:ring-indigo-500/20 text-slate-100'
                    }`}
                  />
                </div>
                {formErrors.email && (
                  <p className="mt-1.5 text-xs text-rose-400 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{formErrors.email}</span>
                  </p>
                )}
              </div>

              {/* Password */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-[11px] font-bold text-slate-300 uppercase tracking-wider">
                    Mật khẩu <span className="text-rose-400">*</span>
                  </label>
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="••••••••••••"
                    autoComplete="current-password"
                    className={`w-full pl-10 pr-11 py-2.5 text-sm bg-slate-950/80 border rounded-xl focus:outline-none focus:ring-2 transition-all placeholder:text-slate-600 ${
                      formErrors.password
                        ? 'border-rose-500/80 focus:ring-rose-500/30 text-rose-200'
                        : 'border-slate-800 focus:border-indigo-500 focus:ring-indigo-500/20 text-slate-100'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-300 cursor-pointer focus:outline-none"
                    title={showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {formErrors.password && (
                  <p className="mt-1.5 text-xs text-rose-400 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                    <span>{formErrors.password}</span>
                  </p>
                )}
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={isLoading}
                className="w-full mt-3 bg-gradient-to-r from-indigo-600 via-indigo-500 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white font-bold py-3 px-4 rounded-xl shadow-lg shadow-indigo-600/20 active:scale-[0.99] transition-all text-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <>
                    <LoadingSpinner size="sm" />
                    <span>Đang xác thực quyền Admin...</span>
                  </>
                ) : (
                  <>
                    <Shield className="w-4 h-4" />
                    <span>Đăng nhập Quản trị</span>
                  </>
                )}
              </button>
            </form>

            {/* Security Notice Box */}
            <div className="mt-8 pt-5 border-t border-slate-800/80 text-center">
              <p className="text-[11px] text-slate-500 leading-relaxed">
                Mọi truy cập trái phép hoặc cố tình dò quét mật khẩu quản trị đều bị giám sát và ghi lại địa chỉ IP theo tiêu chuẩn an ninh hệ thống.
              </p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer System Badges */}
      <footer className="relative z-10 py-5 text-center text-slate-600 text-[11px] border-t border-slate-900 bg-slate-950/80">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>&copy; 2026 Rikkei E-Commerce Platform. All Rights Reserved.</span>
          <div className="flex items-center gap-3 text-slate-500">
            <span className="flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              RBAC Protected
            </span>
            <span>•</span>
            <span>JWT + Opaque Rotation</span>
            <span>•</span>
            <span>TLS 1.3</span>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default AdminLoginPage
