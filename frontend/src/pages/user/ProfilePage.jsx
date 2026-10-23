import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  User,
  Lock,
  Phone,
  MapPin,
  Mail,
  Calendar,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Eye,
  EyeOff,
  Save,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import AddressMapModal from '../../components/common/AddressMapModal'

export const ProfilePage = () => {
  const navigate = useNavigate()
  const { user, fetchProfile, updateProfile, changePassword, isLoading } = useAuthStore()

  const [activeTab, setActiveTab] = useState('profile') // 'profile' | 'password'
  const [isMapModalOpen, setIsMapModalOpen] = useState(false)

  // Form Profile State
  const [profileData, setProfileData] = useState({
    fullName: '',
    phone: '',
    address: '',
  })
  const [profileErrors, setProfileErrors] = useState({})
  const [profileSuccessMsg, setProfileSuccessMsg] = useState('')
  const [profileErrorMsg, setProfileErrorMsg] = useState('')

  // Form Change Password State
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [passwordErrors, setPasswordErrors] = useState({})
  const [passwordSuccessMsg, setPasswordSuccessMsg] = useState('')
  const [passwordErrorMsg, setPasswordErrorMsg] = useState('')

  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  // Đồng bộ thông tin user vào form khi load
  useEffect(() => {
    fetchProfile()
  }, [])

  useEffect(() => {
    if (user) {
      setProfileData({
        fullName: user.fullName || '',
        phone: user.phone || '',
        address: user.address || '',
      })
    }
  }, [user])

  // --- Handlers cho Profile ---
  const handleProfileChange = (e) => {
    const { name, value } = e.target
    setProfileData((prev) => ({ ...prev, [name]: value }))
    if (profileErrors[name]) {
      setProfileErrors((prev) => ({ ...prev, [name]: '' }))
    }
    setProfileSuccessMsg('')
    setProfileErrorMsg('')
  }

  const handleConfirmMapAddress = ({ address }) => {
    setProfileData((prev) => ({ ...prev, address }))
    setProfileSuccessMsg('')
    setProfileErrorMsg('')
  }

  const validateProfile = () => {
    const errors = {}
    const phoneRegex = /^0\d{9}$/

    if (!profileData.fullName.trim()) {
      errors.fullName = 'Họ và tên không được để trống'
    } else if (profileData.fullName.trim().length > 100) {
      errors.fullName = 'Họ và tên không được vượt quá 100 ký tự'
    }

    if (profileData.phone && profileData.phone.trim()) {
      if (!phoneRegex.test(profileData.phone.trim())) {
        errors.phone = 'Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)'
      }
    }

    setProfileErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleProfileSubmit = async (e) => {
    e.preventDefault()
    if (!validateProfile()) return

    setProfileSuccessMsg('')
    setProfileErrorMsg('')

    try {
      const res = await updateProfile({
        fullName: profileData.fullName.trim(),
        phone: profileData.phone.trim() || null,
        address: profileData.address.trim() || null,
      })
      setProfileSuccessMsg(res.message || 'Cập nhật thông tin hồ sơ thành công!')
    } catch (err) {
      setProfileErrorMsg(err.message || 'Cập nhật thất bại. Vui lòng thử lại.')
    }
  }

  // --- Handlers cho Change Password ---
  const handlePasswordChange = (e) => {
    const { name, value } = e.target
    setPasswordData((prev) => ({ ...prev, [name]: value }))
    if (passwordErrors[name]) {
      setPasswordErrors((prev) => ({ ...prev, [name]: '' }))
    }
    setPasswordSuccessMsg('')
    setPasswordErrorMsg('')
  }

  const validatePassword = () => {
    const errors = {}

    if (!passwordData.currentPassword) {
      errors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại'
    }

    if (!passwordData.newPassword) {
      errors.newPassword = 'Vui lòng nhập mật khẩu mới'
    } else if (passwordData.newPassword.length < 6) {
      errors.newPassword = 'Mật khẩu mới phải có ít nhất 6 ký tự'
    } else if (passwordData.newPassword.length > 100) {
      errors.newPassword = 'Mật khẩu mới không được vượt quá 100 ký tự'
    }

    if (!passwordData.confirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới'
    } else if (passwordData.newPassword !== passwordData.confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp'
    }

    setPasswordErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handlePasswordSubmit = async (e) => {
    e.preventDefault()
    if (!validatePassword()) return

    setPasswordSuccessMsg('')
    setPasswordErrorMsg('')

    try {
      const res = await changePassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword,
      })
      navigate('/login', {
        state: {
          successMessage:
            res.message || 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.',
        },
      })
    } catch (err) {
      setPasswordErrorMsg(err.message || 'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại.')
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'Chưa cập nhật'
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch (e) {
      return dateString
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex-1 w-full">
      {/* Header Profile Title */}
      <div className="mb-8 pb-4 border-b border-slate-200">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
          Hồ sơ người dùng
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-1">
          Quản lý thông tin tài khoản cá nhân, sổ địa chỉ và cài đặt bảo mật.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-start">
        {/* Left Column: User Card Info & Tab navigation (4/12) */}
        <div className="md:col-span-4 space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-6 text-center">
            {/* Avatar */}
            <div className="w-20 h-20 rounded-2xl bg-orange-600 text-white font-extrabold text-2xl flex items-center justify-center mx-auto mb-4 shadow-2xs">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
            </div>

            <h2 className="text-base font-bold text-slate-900 leading-tight">
              {user?.fullName || 'Người dùng'}
            </h2>
            <p className="text-xs text-slate-500 mt-1 break-all">
              {user?.email}
            </p>

            <div className="mt-4 flex flex-wrap justify-center gap-2">
              <span
                className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-full uppercase tracking-wider ${
                  user?.role === 'ADMIN'
                    ? 'bg-purple-50 text-purple-700 border border-purple-200'
                    : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                }`}
              >
                {user?.role || 'CUSTOMER'}
              </span>

              <span
                className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-full ${
                  user?.active
                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                    : 'bg-red-50 text-red-700 border border-red-200'
                }`}
              >
                {user?.active ? 'Đang hoạt động' : 'Đã vô hiệu hóa'}
              </span>
            </div>

            <div className="mt-6 pt-5 border-t border-slate-100 text-left text-xs space-y-2.5 text-slate-600">
              <div className="flex justify-between">
                <span className="text-slate-400">Mã ID:</span>
                <span className="font-semibold text-slate-800">#{user?.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Ngày đăng ký:</span>
                <span className="font-medium text-slate-800 text-right">
                  {formatDate(user?.createdAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Tab buttons */}
          <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-2 flex flex-col gap-1">
            <button
              onClick={() => setActiveTab('profile')}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-xs font-semibold transition-all flex items-center gap-2.5 cursor-pointer ${
                activeTab === 'profile'
                  ? 'bg-orange-50 text-orange-600 shadow-2xs'
                  : 'text-slate-600 hover:bg-slate-50'
              }`}
            >
              <User className="w-4 h-4" />
              <span>Thông tin cá nhân</span>
            </button>
            <button
              onClick={() => setActiveTab('password')}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-xs font-semibold transition-all flex items-center gap-2.5 cursor-pointer ${
                activeTab === 'password'
                  ? 'bg-orange-50 text-orange-600 shadow-2xs'
                  : 'text-slate-600 hover:bg-slate-50'
              }`}
            >
              <Lock className="w-4 h-4" />
              <span>Đổi mật khẩu</span>
            </button>
          </div>
        </div>

        {/* Right Column: Active Tab Content (8/12) */}
        <div className="md:col-span-8">
          {activeTab === 'profile' && (
            <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-6 sm:p-8">
              <div className="border-b border-slate-100 pb-4 mb-6">
                <h3 className="text-base font-bold text-slate-900">Cập nhật hồ sơ</h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Bạn có thể cập nhật họ tên, số điện thoại và địa chỉ nhận hàng mặc định.
                </p>
              </div>

              {profileSuccessMsg && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2 shadow-2xs">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{profileSuccessMsg}</span>
                </div>
              )}

              {profileErrorMsg && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2 shadow-2xs">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{profileErrorMsg}</span>
                </div>
              )}

              <form onSubmit={handleProfileSubmit} className="space-y-4" noValidate>
                {/* Email (Read-only) */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Địa chỉ Email <span className="text-slate-400 font-normal lowercase">(cố định)</span>
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="email"
                      value={user?.email || ''}
                      disabled
                      className="w-full pl-10 pr-4 py-2.5 text-xs bg-slate-100 text-slate-500 border border-slate-200 rounded-xl cursor-not-allowed"
                    />
                  </div>
                  <p className="mt-1 text-[11px] text-slate-400">
                    Email là mã định danh tài khoản, không thể thay đổi trực tiếp.
                  </p>
                </div>

                {/* Full Name */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Họ và tên <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      name="fullName"
                      value={profileData.fullName}
                      onChange={handleProfileChange}
                      placeholder="Nguyễn Văn A"
                      className={`w-full pl-10 pr-4 py-2.5 text-xs bg-slate-50/70 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        profileErrors.fullName
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/20'
                          : 'border-slate-200 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800'
                      }`}
                    />
                  </div>
                  {profileErrors.fullName && (
                    <p className="mt-1 text-xs text-red-600 font-medium flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{profileErrors.fullName}</span>
                    </p>
                  )}
                </div>

                {/* Phone */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Số điện thoại
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="tel"
                      name="phone"
                      value={profileData.phone}
                      onChange={handleProfileChange}
                      placeholder="0912345678"
                      className={`w-full pl-10 pr-4 py-2.5 text-xs bg-slate-50/70 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        profileErrors.phone
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/20'
                          : 'border-slate-200 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800'
                      }`}
                    />
                  </div>
                  {profileErrors.phone && (
                    <p className="mt-1 text-xs text-red-600 font-medium flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{profileErrors.phone}</span>
                    </p>
                  )}
                </div>

                {/* Address */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider">
                      Địa chỉ nhận hàng mặc định
                    </label>
                    <button
                      type="button"
                      onClick={() => setIsMapModalOpen(true)}
                      className="inline-flex items-center gap-1.5 text-xs font-bold text-orange-600 hover:text-orange-700 bg-orange-50 hover:bg-orange-100 px-2.5 py-1 rounded-lg border border-orange-200/80 transition-all cursor-pointer shadow-2xs hover:shadow-xs active:scale-[0.98]"
                    >
                      <MapPin className="w-3.5 h-3.5 text-orange-600" />
                      <span>Chọn từ bản đồ</span>
                    </button>
                  </div>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                    <textarea
                      name="address"
                      rows="3"
                      value={profileData.address}
                      onChange={handleProfileChange}
                      placeholder="Nhập số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                      className="w-full pl-10 pr-4 py-2.5 text-xs bg-slate-50/70 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800 transition-all resize-none"
                    />
                  </div>
                  <div className="mt-1 flex items-center justify-between text-[11px] text-slate-400">
                    <span>Bạn có thể gõ trực tiếp hoặc mở bản đồ để chọn chính xác.</span>
                    {profileData.address && (
                      <button
                        type="button"
                        onClick={() => setIsMapModalOpen(true)}
                        className="text-orange-600 hover:underline font-semibold cursor-pointer"
                      >
                        Đổi vị trí trên bản đồ
                      </button>
                    )}
                  </div>
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-2xs hover:shadow-xs active:scale-[0.99] transition-all text-xs flex items-center gap-2 cursor-pointer disabled:opacity-60"
                  >
                    {isLoading ? (
                      <>
                        <LoadingSpinner size="sm" />
                        <span>Đang lưu...</span>
                      </>
                    ) : (
                      <>
                        <Save className="w-4 h-4" />
                        <span>Lưu thay đổi</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'password' && (
            <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-6 sm:p-8">
              <div className="border-b border-slate-100 pb-4 mb-6">
                <h3 className="text-base font-bold text-slate-900">Đổi mật khẩu</h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Khuyến nghị sử dụng mật khẩu mạnh kết hợp chữ cái, số và ký tự đặc biệt.
                </p>
              </div>

              {passwordSuccessMsg && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2 shadow-2xs">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{passwordSuccessMsg}</span>
                </div>
              )}

              {passwordErrorMsg && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2 shadow-2xs">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{passwordErrorMsg}</span>
                </div>
              )}

              <form onSubmit={handlePasswordSubmit} className="space-y-4" noValidate>
                {/* Current Password */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Mật khẩu hiện tại <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type={showCurrentPassword ? 'text' : 'password'}
                      name="currentPassword"
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                      placeholder="••••••••"
                      autoComplete="current-password"
                      className={`w-full pl-10 pr-11 py-2.5 text-xs bg-slate-50/70 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        passwordErrors.currentPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/20'
                          : 'border-slate-200 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                    >
                      {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {passwordErrors.currentPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{passwordErrors.currentPassword}</span>
                    </p>
                  )}
                </div>

                {/* New Password */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Mật khẩu mới <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      name="newPassword"
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      placeholder="Tối thiểu 6 ký tự"
                      autoComplete="new-password"
                      className={`w-full pl-10 pr-11 py-2.5 text-xs bg-slate-50/70 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        passwordErrors.newPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/20'
                          : 'border-slate-200 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showNewPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                    >
                      {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {passwordErrors.newPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{passwordErrors.newPassword}</span>
                    </p>
                  )}
                </div>

                {/* Confirm New Password */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                    Xác nhận mật khẩu mới <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      name="confirmPassword"
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                      placeholder="Nhập lại mật khẩu mới"
                      autoComplete="new-password"
                      className={`w-full pl-10 pr-11 py-2.5 text-xs bg-slate-50/70 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${
                        passwordErrors.confirmPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/20'
                          : 'border-slate-200 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800'
                      }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                    >
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {passwordErrors.confirmPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium flex items-center gap-1">
                      <AlertCircle className="w-3.5 h-3.5" />
                      <span>{passwordErrors.confirmPassword}</span>
                    </p>
                  )}
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="bg-slate-900 hover:bg-slate-800 text-white font-bold py-2.5 px-6 rounded-xl shadow-2xs hover:shadow-xs active:scale-[0.99] transition-all text-xs flex items-center gap-2 cursor-pointer disabled:opacity-60"
                  >
                    {isLoading ? (
                      <>
                        <LoadingSpinner size="sm" />
                        <span>Đang xử lý...</span>
                      </>
                    ) : (
                      <>
                        <Lock className="w-4 h-4" />
                        <span>Đổi mật khẩu</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>

      {/* Modal chọn vị trí trên bản đồ */}
      <AddressMapModal
        isOpen={isMapModalOpen}
        onClose={() => setIsMapModalOpen(false)}
        onConfirmAddress={handleConfirmMapAddress}
        initialAddress={profileData.address}
      />
    </div>
  )
}

export default ProfilePage
