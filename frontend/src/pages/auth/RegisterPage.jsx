import React, { useState, useEffect } from 'react'
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
  Zap,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const RegisterPage = () => {
  const navigate = useNavigate()
  const { register, sendRegistrationOtp, isLoading, error, clearError } = useAuthStore()

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    otp: '',
    password: '',
    confirmPassword: '',
    phone: '',
    address: '',
  })

  const [formErrors, setFormErrors] = useState({})
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  // Trạng thái gửi mã xác thực OTP
  const [isOtpSent, setIsOtpSent] = useState(false)
  const [isSendingOtp, setIsSendingOtp] = useState(false)
  const [otpCountdown, setOtpCountdown] = useState(0)
  const [otpSuccessNotice, setOtpSuccessNotice] = useState(null)

  // Countdown timer cho nút gửi lại mã OTP (60 giây)
  useEffect(() => {
    let timer
    if (otpCountdown > 0) {
      timer = setInterval(() => {
        setOtpCountdown((prev) => prev - 1)
      }, 1000)
    }
    return () => clearInterval(timer)
  }, [otpCountdown])

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

  // Xử lý gửi mã OTP xác thực email
  const handleSendOtp = async () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    const phoneRegex = /^0\d{9}$/

    if (!formData.email.trim()) {
      setFormErrors((prev) => ({ ...prev, email: 'Vui lòng nhập email trước khi lấy mã xác thực' }))
      return
    }
    if (!emailRegex.test(formData.email.trim())) {
      setFormErrors((prev) => ({ ...prev, email: 'Email không đúng định dạng (VD: user@example.com)' }))
      return
    }
    if (formData.phone && formData.phone.trim() && !phoneRegex.test(formData.phone.trim())) {
      setFormErrors((prev) => ({ ...prev, phone: 'Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng số 0)' }))
      return
    }

    setIsSendingOtp(true)
    setOtpSuccessNotice(null)
    if (error) clearError()

    try {
      await sendRegistrationOtp({
        email: formData.email.trim().toLowerCase(),
        fullName: formData.fullName.trim() || undefined,
        phone: formData.phone.trim() || undefined,
      })
      setIsOtpSent(true)
      setOtpCountdown(60)
      setOtpSuccessNotice('Mã xác thực OTP đã được gửi đến email của bạn! Vui lòng kiểm tra hộp thư.')
      setFormErrors((prev) => ({ ...prev, otp: '' }))
    } catch (err) {
      const errMsg = err.message || 'Không thể gửi mã xác thực lúc này. Vui lòng kiểm tra email hoặc thử lại sau.'
      if (errMsg.toLowerCase().includes('số điện thoại') || errMsg.toLowerCase().includes('phone')) {
        setFormErrors((prev) => ({ ...prev, phone: errMsg }))
      } else {
        setFormErrors((prev) => ({ ...prev, email: errMsg }))
      }
    } finally {
      setIsSendingOtp(false)
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

    // Bắt buộc xác thực OTP email trước khi tạo tài khoản vào CSDL
    if (!formData.otp || !formData.otp.trim()) {
      errors.otp = isOtpSent
        ? 'Vui lòng nhập mã OTP 6 chữ số vừa nhận được trong email'
        : 'Vui lòng bấm "Gửi mã xác thực" để nhận mã OTP qua email'
    } else if (formData.otp.trim().length !== 6 || !/^\d{6}$/.test(formData.otp.trim())) {
      errors.otp = 'Mã xác thực phải gồm đúng 6 chữ số'
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
        otp: formData.otp.trim(),
      })

      // Đăng ký và xác thực thành công ngay tại form -> Điều hướng sang Login
      navigate('/login', {
        state: {
          successMessage:
            'Đăng ký tài khoản và xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.',
        },
        replace: true,
      })
    } catch (err) {
      const responseData = err?.response?.data
      const backendFieldErrors = responseData?.data
      const rawMsg = responseData?.message || err?.message || ''

      if (rawMsg.toLowerCase().includes('số điện thoại') || rawMsg.toLowerCase().includes('phone')) {
        setFormErrors((prev) => ({ ...prev, phone: rawMsg }))
      }

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
    <div className="flex-1 flex items-center justify-center px-4 py-16 bg-slate-50/60">
      <div className="w-full max-w-lg">
        {/* Card */}
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10 space-y-6">
          
          {/* Header */}
          <div className="space-y-2 text-center">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center mx-auto shadow-md shadow-blue-500/25">
              <Zap className="w-6 h-6 fill-current text-white" />
            </div>
            <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 pt-1">
              Đăng ký tài khoản mới
            </h1>
            <p className="text-xs text-slate-500">
              Gia nhập hệ sinh thái Rikkei Store để nhận ưu đãi thành viên và bảo hành 2 năm
            </p>
          </div>

          {/* Backend Error Banner */}
          {error && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs flex items-start gap-2.5 shadow-xs">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-bold block mb-0.5">Đăng ký thất bại:</span>
                <span>{error}</span>
              </div>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Full Name */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-800 block">
                Họ và tên
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  placeholder="Nguyễn Văn A"
                  autoComplete="name"
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                    formErrors.fullName
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
              </div>
              {formErrors.fullName && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.fullName}</span>
                </p>
              )}
            </div>

            {/* Email */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-800 block">
                  Email đăng nhập <span className="text-red-500">*</span>
                </label>
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={isSendingOtp || otpCountdown > 0}
                  className="text-xs font-semibold text-blue-600 hover:text-blue-700 disabled:text-slate-400 disabled:cursor-not-allowed transition-colors cursor-pointer flex items-center gap-1"
                >
                  {isSendingOtp ? (
                    <>
                      <LoadingSpinner size="sm" className="w-3.5 h-3.5" />
                      <span>Đang gửi mã...</span>
                    </>
                  ) : otpCountdown > 0 ? (
                    <span>Gửi lại sau ({otpCountdown}s)</span>
                  ) : isOtpSent ? (
                    <span>Gửi lại mã OTP</span>
                  ) : (
                    <span>Gửi mã xác thực</span>
                  )}
                </button>
              </div>
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
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.email}</span>
                </p>
              )}
            </div>

            {/* OTP Verification Code */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-800 block">
                  Mã xác thực email (OTP) <span className="text-red-500">*</span>
                </label>
                {isOtpSent && (
                  <span className="text-[11px] font-medium text-emerald-600 flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Đã gửi OTP đến email
                  </span>
                )}
              </div>
              <div className="relative">
                <ShieldCheck className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="otp"
                  maxLength={6}
                  value={formData.otp}
                  onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '').slice(0, 6)
                    setFormData((prev) => ({ ...prev, otp: val }))
                    if (formErrors.otp) setFormErrors((prev) => ({ ...prev, otp: '' }))
                  }}
                  placeholder="Nhập 6 số OTP (VD: 123456)"
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-3 py-1 text-sm font-mono tracking-widest shadow-xs transition-colors placeholder:font-sans placeholder:tracking-normal placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                    formErrors.otp
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
              </div>
              {formErrors.otp && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{formErrors.otp}</span>
                </p>
              )}
              {otpSuccessNotice && !formErrors.otp && (
                <div className="text-xs text-emerald-800 bg-emerald-50 border border-emerald-200 p-2.5 rounded-xl flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{otpSuccessNotice}</span>
                </div>
              )}
            </div>

            {/* Password & Confirm Password (Grid 2 columns on sm+) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Password */}
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
                    placeholder="Tối thiểu 6 ký tự"
                    autoComplete="new-password"
                    className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-10 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                      formErrors.password
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-200'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
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

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-800 block">
                  Xác nhận lại
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder="Nhập lại mật khẩu"
                    autoComplete="new-password"
                    className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-10 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                      formErrors.confirmPassword
                        ? 'border-red-500 focus:ring-red-500/20'
                        : 'border-slate-200'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {formErrors.confirmPassword && (
                  <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    <span>{formErrors.confirmPassword}</span>
                  </p>
                )}
              </div>
            </div>

            {/* Phone */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-800 block">
                Số điện thoại
              </label>
              <div className="relative">
                <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="0912345678"
                  autoComplete="tel"
                  className={`flex h-11 w-full rounded-xl border bg-slate-50 pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 ${
                    formErrors.phone
                      ? 'border-red-500 focus:ring-red-500/20'
                      : 'border-slate-200'
                  }`}
                />
              </div>
              {formErrors.phone && (
                <p className="text-xs text-red-600 font-semibold flex items-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{formErrors.phone}</span>
                </p>
              )}
            </div>

            {/* Address (Optional) */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-800 block">
                Địa chỉ giao hàng <span className="text-slate-400 font-normal">(tùy chọn)</span>
              </label>
              <div className="relative">
                <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <textarea
                  name="address"
                  rows="2"
                  value={formData.address}
                  onChange={handleChange}
                  placeholder="Số nhà, đường, phường/xã, quận/huyện..."
                  className="flex min-h-[60px] w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-3 py-2 text-sm shadow-xs placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 resize-none transition-colors"
                />
              </div>
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
                  <span>Đang xử lý đăng ký...</span>
                </>
              ) : (
                <span>Đăng ký tài khoản</span>
              )}
            </button>
          </form>

          {/* Footer Link to Login */}
          <div className="pt-2 text-center text-xs text-slate-500">
            Đã có tài khoản?{' '}
            <Link
              to="/login"
              className="font-bold text-blue-600 hover:text-blue-700 underline underline-offset-4 transition-colors"
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
