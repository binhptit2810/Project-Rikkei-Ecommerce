import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const RegisterPage = () => {
  const navigate = useNavigate()
  const { register, isLoading, error, clearError } = useAuthStore()

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone: '',
    address: '',
  })

  const [formErrors, setFormErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))

    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: '' }))
    }
    if (error) {
      clearError()
    }
  }

  const validate = () => {
    const errors = {}
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    const phoneRegex = /^0\d{9}$/

    // Họ tên
    if (!formData.fullName.trim()) {
      errors.fullName = 'Họ và tên không được để trống'
    } else if (formData.fullName.trim().length > 100) {
      errors.fullName = 'Họ và tên không được vượt quá 100 ký tự'
    }

    // Email
    if (!formData.email.trim()) {
      errors.email = 'Email không được để trống'
    } else if (!emailRegex.test(formData.email.trim())) {
      errors.email = 'Email không đúng định dạng (VD: user@example.com)'
    } else if (formData.email.trim().length > 100) {
      errors.email = 'Email không được vượt quá 100 ký tự'
    }

    // Mật khẩu
    if (!formData.password) {
      errors.password = 'Mật khẩu không được để trống'
    } else if (formData.password.length < 6) {
      errors.password = 'Mật khẩu phải có ít nhất 6 ký tự'
    } else if (formData.password.length > 100) {
      errors.password = 'Mật khẩu không được vượt quá 100 ký tự'
    }

    // Xác nhận mật khẩu
    if (!formData.confirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu'
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp'
    }

    // Số điện thoại (tùy chọn)
    if (formData.phone && formData.phone.trim()) {
      if (!phoneRegex.test(formData.phone.trim())) {
        errors.phone = 'Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)'
      }
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return

    try {
      await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        phone: formData.phone.trim() || null,
        address: formData.address.trim() || null,
      })
      // Đăng ký thành công -> tự động vào hồ sơ
      navigate('/profile', { replace: true })
    } catch (err) {
      // Lỗi hiển thị qua error state trong store
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        {/* Card */}
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="w-14 h-14 bg-orange-100 text-orange-600 rounded-2xl flex items-center justify-center mx-auto mb-3 shadow-inner text-2xl">
              📝
            </div>
            <h1 className="text-2xl font-extrabold text-slate-800 tracking-tight">
              Đăng ký tài khoản
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Tạo tài khoản để trải nghiệm mua sắm và quản lý đơn hàng
            </p>
          </div>

          {/* Backend Error Banner */}
          {error && (
            <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-start gap-3">
              <svg className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <span className="font-semibold block mb-0.5">Đăng ký thất bại:</span>
                <span>{error}</span>
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full Name */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Họ và tên <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Nguyễn Văn A"
                autoComplete="name"
                className={`w-full px-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.fullName
                    ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                    : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                  }`}
              />
              {formErrors.fullName && (
                <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {formErrors.fullName}</p>
              )}
            </div>

            {/* Email */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Email <span className="text-red-500">*</span>
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="user@example.com"
                autoComplete="email"
                className={`w-full px-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.email
                    ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                    : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                  }`}
              />
              {formErrors.email && (
                <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {formErrors.email}</p>
              )}
            </div>

            {/* Password & Confirm Password (Grid 2 columns on sm+) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Password */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Mật khẩu <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="Tối thiểu 6 ký tự"
                    autoComplete="new-password"
                    className={`w-full pl-3.5 pr-10 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.password
                        ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                        : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                      }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
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
                  <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {formErrors.password}</p>
                )}
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Xác nhận lại <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder="Nhập lại mật khẩu"
                    autoComplete="new-password"
                    className={`w-full pl-3.5 pr-10 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.confirmPassword
                        ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                        : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                      }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
                  >
                    {showConfirmPassword ? (
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
                {formErrors.confirmPassword && (
                  <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {formErrors.confirmPassword}</p>
                )}
              </div>
            </div>

            {/* Phone (Optional) */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Số điện thoại <span className="text-slate-400 font-normal lowercase">(tùy chọn)</span>
              </label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                placeholder="0912345678"
                autoComplete="tel"
                className={`w-full px-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${formErrors.phone
                    ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                    : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                  }`}
              />
              {formErrors.phone && (
                <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {formErrors.phone}</p>
              )}
            </div>

            {/* Address (Optional) */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Địa chỉ giao hàng <span className="text-slate-400 font-normal lowercase">(tùy chọn)</span>
              </label>
              <textarea
                name="address"
                rows="2"
                value={formData.address}
                onChange={handleChange}
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành..."
                className="w-full px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500 text-slate-800 transition-all resize-none"
              />
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 text-white font-bold py-3 px-4 rounded-xl shadow-md hover:shadow-lg active:scale-[0.99] transition-all text-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed pt-3 mt-4"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Đang xử lý đăng ký...</span>
                </>
              ) : (
                <span>Đăng ký tài khoản</span>
              )}
            </button>
          </form>

          {/* Footer Link to Login */}
          <div className="mt-8 pt-6 border-t border-slate-100 text-center">
            <p className="text-xs text-slate-600">
              Đã có tài khoản?{' '}
              <Link
                to="/login"
                className="font-bold text-orange-600 hover:text-orange-700 transition-colors"
              >
                Đăng nhập tại đây
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RegisterPage
