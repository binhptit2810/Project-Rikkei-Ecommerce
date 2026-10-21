import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const ProfilePage = () => {
  const navigate = useNavigate()
  const { user, fetchProfile, updateProfile, changePassword, isLoading } = useAuthStore()

  const [activeTab, setActiveTab] = useState('profile') // 'profile' | 'password'

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
    <div className="max-w-4xl mx-auto px-4 py-10 flex-1 w-full">
      {/* Header Profile Title */}
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold text-slate-800 tracking-tight">
          Hồ sơ người dùng
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Quản lý thông tin cá nhân và bảo mật tài khoản của bạn
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {/* Left Column: User Card Info (Read-only summary) */}
        <div className="md:col-span-1 space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 text-center">
            {/* Avatar */}
            <div className="w-20 h-20 rounded-full bg-gradient-to-tr from-orange-500 to-amber-400 text-white font-extrabold text-2xl flex items-center justify-center mx-auto mb-4 shadow-md">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
            </div>

            <h2 className="text-lg font-bold text-slate-800 leading-tight">
              {user?.fullName || 'Người dùng'}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5 break-all font-mono">
              {user?.email}
            </p>

            <div className="mt-4 flex flex-wrap justify-center gap-2">
              <span
                className={`text-xs font-semibold px-2.5 py-1 rounded-full uppercase tracking-wider ${user?.role === 'ADMIN'
                    ? 'bg-purple-100 text-purple-700 border border-purple-200'
                    : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                  }`}
              >
                {user?.role || 'CUSTOMER'}
              </span>

              <span
                className={`text-xs font-semibold px-2.5 py-1 rounded-full ${user?.active
                    ? 'bg-blue-100 text-blue-700 border border-blue-200'
                    : 'bg-red-100 text-red-700 border border-red-200'
                  }`}
              >
                {user?.active ? 'Đang hoạt động' : 'Đã vô hiệu hóa'}
              </span>
            </div>

            <div className="mt-6 pt-5 border-t border-slate-100 text-left text-xs space-y-2.5 text-slate-600">
              <div className="flex justify-between">
                <span className="text-slate-400">Mã ID:</span>
                <span className="font-mono font-semibold text-slate-700">#{user?.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Ngày đăng ký:</span>
                <span className="font-medium text-slate-700 text-right">
                  {formatDate(user?.createdAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Tab buttons */}
          <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-2 flex flex-col gap-1">
            <button
              onClick={() => setActiveTab('profile')}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition-all flex items-center gap-2.5 cursor-pointer ${activeTab === 'profile'
                  ? 'bg-orange-50 text-orange-600 shadow-xs'
                  : 'text-slate-600 hover:bg-slate-50'
                }`}
            >
              <span>👤</span> Thông tin cá nhân
            </button>
            <button
              onClick={() => setActiveTab('password')}
              className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-semibold transition-all flex items-center gap-2.5 cursor-pointer ${activeTab === 'password'
                  ? 'bg-orange-50 text-orange-600 shadow-xs'
                  : 'text-slate-600 hover:bg-slate-50'
                }`}
            >
              <span>🔒</span> Đổi mật khẩu
            </button>
          </div>
        </div>

        {/* Right Column: Active Tab Content */}
        <div className="md:col-span-2">
          {activeTab === 'profile' && (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8">
              <div className="border-b border-slate-100 pb-4 mb-6">
                <h3 className="text-lg font-bold text-slate-800">Cập nhật hồ sơ</h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Bạn có thể cập nhật họ tên, số điện thoại và địa chỉ giao hàng
                </p>
              </div>

              {profileSuccessMsg && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
                  <span>✅</span> {profileSuccessMsg}
                </div>
              )}

              {profileErrorMsg && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <span>⚠️</span> {profileErrorMsg}
                </div>
              )}

              <form onSubmit={handleProfileSubmit} className="space-y-5" noValidate>
                {/* Email (Read-only) */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Địa chỉ Email <span className="text-slate-400 font-normal lowercase">(cố định)</span>
                  </label>
                  <input
                    type="email"
                    value={user?.email || ''}
                    disabled
                    className="w-full px-4 py-2.5 text-sm bg-slate-100 text-slate-500 border border-slate-200 rounded-xl cursor-not-allowed font-mono"
                  />
                  <p className="mt-1 text-[11px] text-slate-400">
                    Email là định danh tài khoản, không thể thay đổi trực tiếp.
                  </p>
                </div>

                {/* Full Name */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Họ và tên <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="fullName"
                    value={profileData.fullName}
                    onChange={handleProfileChange}
                    placeholder="Nguyễn Văn A"
                    className={`w-full px-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${profileErrors.fullName
                        ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                        : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                      }`}
                  />
                  {profileErrors.fullName && (
                    <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {profileErrors.fullName}</p>
                  )}
                </div>

                {/* Phone */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Số điện thoại
                  </label>
                  <input
                    type="tel"
                    name="phone"
                    value={profileData.phone}
                    onChange={handleProfileChange}
                    placeholder="0912345678"
                    className={`w-full px-4 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${profileErrors.phone
                        ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                        : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                      }`}
                  />
                  {profileErrors.phone && (
                    <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {profileErrors.phone}</p>
                  )}
                </div>

                {/* Address */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Địa chỉ nhận hàng mặc định
                  </label>
                  <textarea
                    name="address"
                    rows="3"
                    value={profileData.address}
                    onChange={handleProfileChange}
                    placeholder="Nhập địa chỉ nhận hàng chi tiết..."
                    className="w-full px-4 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500 text-slate-800 transition-all resize-none"
                  />
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-md hover:shadow-lg active:scale-95 transition-all text-sm flex items-center gap-2 cursor-pointer disabled:opacity-60"
                  >
                    {isLoading ? (
                      <>
                        <LoadingSpinner size="sm" />
                        <span>Đang lưu...</span>
                      </>
                    ) : (
                      <span>Lưu thay đổi</span>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'password' && (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8">
              <div className="border-b border-slate-100 pb-4 mb-6">
                <h3 className="text-lg font-bold text-slate-800">Đổi mật khẩu</h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Để bảo mật tài khoản, vui lòng không chia sẻ mật khẩu cho người khác
                </p>
              </div>

              {passwordSuccessMsg && (
                <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
                  <span>✅</span> {passwordSuccessMsg}
                </div>
              )}

              {passwordErrorMsg && (
                <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <span>⚠️</span> {passwordErrorMsg}
                </div>
              )}

              <form onSubmit={handlePasswordSubmit} className="space-y-4" noValidate>
                {/* Current Password */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Mật khẩu hiện tại <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showCurrentPassword ? 'text' : 'password'}
                      name="currentPassword"
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                      placeholder="••••••••"
                      autoComplete="current-password"
                      className={`w-full pl-3.5 pr-11 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${passwordErrors.currentPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                          : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                        }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                    >
                      {showCurrentPassword ? (
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
                  {passwordErrors.currentPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {passwordErrors.currentPassword}</p>
                  )}
                </div>

                {/* New Password */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Mật khẩu mới <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      name="newPassword"
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      placeholder="Tối thiểu 6 ký tự"
                      autoComplete="new-password"
                      className={`w-full pl-3.5 pr-11 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${passwordErrors.newPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                          : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                        }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showNewPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
                    >
                      {showNewPassword ? (
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
                  {passwordErrors.newPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {passwordErrors.newPassword}</p>
                  )}
                </div>

                {/* Confirm New Password */}
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Xác nhận mật khẩu mới <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      name="confirmPassword"
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                      placeholder="Nhập lại mật khẩu mới"
                      autoComplete="new-password"
                      className={`w-full pl-3.5 pr-11 py-2.5 text-sm bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 transition-all ${passwordErrors.confirmPassword
                          ? 'border-red-300 focus:ring-red-400 text-red-900 bg-red-50/30'
                          : 'border-slate-200 focus:ring-orange-500 text-slate-800'
                        }`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer focus:outline-none"
                      title={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'}
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
                  {passwordErrors.confirmPassword && (
                    <p className="mt-1 text-xs text-red-600 font-medium">⚠️ {passwordErrors.confirmPassword}</p>
                  )}
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-md hover:shadow-lg active:scale-95 transition-all text-sm flex items-center gap-2 cursor-pointer disabled:opacity-60"
                  >
                    {isLoading ? (
                      <>
                        <LoadingSpinner size="sm" />
                        <span>Đang xử lý...</span>
                      </>
                    ) : (
                      <span>Đổi mật khẩu</span>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default ProfilePage
