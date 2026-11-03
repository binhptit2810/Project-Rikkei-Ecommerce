import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  User,
  Mail,
  Lock,
  Phone,
  MapPin,
  Eye,
  EyeOff,
  AlertCircle,
} from 'lucide-react'
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

    // Số điện thoại (bắt buộc: 10 chữ số, bắt đầu bằng 0)
    if (!formData.phone || !formData.phone.trim()) {
      errors.phone = 'Số điện thoại không được để trống'
    } else if (!phoneRegex.test(formData.phone.trim())) {
      errors.phone = 'Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)'
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
        phone: formData.phone.trim(),
        address: formData.address.trim() || null,
      })
      navigate('/profile', { replace: true })
    } catch (err) {
      const responseData = err?.response?.data
      const backendFieldErrors = responseData?.data

      if (
        backendFieldErrors &&
        typeof backendFieldErrors === 'object' &&
        Object.keys(backendFieldErrors).length > 0
      ) {
        setFormErrors((prev) => ({
          ...prev,
          ...backendFieldErrors,
        }))
        clearError()
      }
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12 bg-zinc-50/50">
      <div className="w-full max-w-lg">
        {/* Card */}
        <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 sm:p-8 space-y-6">
          {/* Header */}
          <div className="space-y-1.5 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-zinc-950">
              Đăng ký tài khoản
            </h1>
            <p className="text-sm text-zinc-500">
              Tạo tài khoản để trải nghiệm mua sắm và theo dõi đơn hàng
            </p>
          </div>

          {/* Backend Error Banner */}
          {error && (
            <div className="p-3 rounded-md bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold block mb-0.5">Đăng ký thất bại:</span>
                <span>{error}</span>
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full Name */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-zinc-900 block">
                Họ và tên
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  placeholder="Nguyễn Văn A"
                  autoComplete="name"
                  className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-3 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                    formErrors.fullName
                      ? 'border-red-500 focus-visible:ring-red-500'
                      : 'border-zinc-200'
                  }`}
                />
              </div>
              {formErrors.fullName && (
                <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.fullName}</span>
                </p>
              )}
            </div>

            {/* Email */}
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

            {/* Password & Confirm Password (Grid 2 columns on sm+) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Password */}
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
                    placeholder="Tối thiểu 6 ký tự"
                    autoComplete="new-password"
                    className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-10 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                      formErrors.password
                        ? 'border-red-500 focus-visible:ring-red-500'
                        : 'border-zinc-200'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-zinc-400 hover:text-zinc-600 cursor-pointer"
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

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label className="text-sm font-medium text-zinc-900 block">
                  Xác nhận lại
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder="Nhập lại mật khẩu"
                    autoComplete="new-password"
                    className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-10 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                      formErrors.confirmPassword
                        ? 'border-red-500 focus-visible:ring-red-500'
                        : 'border-zinc-200'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-zinc-400 hover:text-zinc-600 cursor-pointer"
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {formErrors.confirmPassword && (
                  <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    <span>{formErrors.confirmPassword}</span>
                  </p>
                )}
              </div>
            </div>

            {/* Phone */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-zinc-900 block">
                Số điện thoại
              </label>
              <div className="relative">
                <Phone className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="0912345678"
                  autoComplete="tel"
                  className={`flex h-9 w-full rounded-md border bg-white pl-9 pr-3 py-1 text-sm shadow-sm transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 ${
                    formErrors.phone
                      ? 'border-red-500 focus-visible:ring-red-500'
                      : 'border-zinc-200'
                  }`}
                />
              </div>
              {formErrors.phone && (
                <p className="text-xs text-red-600 font-medium flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.phone}</span>
                </p>
              )}
            </div>

            {/* Address (Optional) */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-zinc-900 block">
                Địa chỉ giao hàng <span className="text-zinc-400 font-normal">(tùy chọn)</span>
              </label>
              <div className="relative">
                <MapPin className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
                <textarea
                  name="address"
                  rows="2"
                  value={formData.address}
                  onChange={handleChange}
                  placeholder="Số nhà, đường, phường/xã, quận/huyện..."
                  className="flex min-h-[60px] w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-2 text-sm shadow-sm placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 resize-none transition-colors"
                />
              </div>
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
                  <span>Đang xử lý đăng ký...</span>
                </>
              ) : (
                <span>Đăng ký tài khoản</span>
              )}
            </button>
          </form>

          {/* Footer Link to Login */}
          <div className="pt-2 text-center text-sm text-zinc-500">
            Đã có tài khoản?{' '}
            <Link
              to="/login"
              className="font-medium text-zinc-950 underline underline-offset-4 hover:text-zinc-800 transition-colors"
            >
              Đăng nhập tại đây
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RegisterPage
