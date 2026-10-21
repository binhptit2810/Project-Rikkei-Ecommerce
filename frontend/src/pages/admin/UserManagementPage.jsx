import React, { useState, useEffect, useMemo } from 'react'
import adminUserService from '../../services/adminUserService'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang quản lý người dùng dành cho Admin (Admin User Management Page).
 * 
 * Bố cục:
 * 1. Tiêu đề và tổng quan hệ thống (Header & Admin Badge).
 * 2. Thẻ thống kê số lượng tài khoản (Stats Overview).
 * 3. Bộ lọc và tìm kiếm (Search & Filters).
 * 4. Bảng danh sách người dùng (User Data Table) theo cấu trúc DTO UserResponse.
 * 5. Modal chi tiết người dùng (User Details Modal).
 * 6. Xử lý trạng thái tải (Loading), lỗi kết nối (Error / 404 notice), và danh sách rỗng (Empty state).
 */
export const UserManagementPage = () => {
  const { user: authUser } = useAuthStore()

  // Dữ liệu người dùng từ API
  const [users, setUsers] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(10)

  // Trạng thái giao diện
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [backendNotImplemented, setBackendNotImplemented] = useState(false)
  const [currentAdminProfile, setCurrentAdminProfile] = useState(null)

  // Bộ lọc và tìm kiếm
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedRole, setSelectedRole] = useState('ALL') // 'ALL' | 'ADMIN' | 'CUSTOMER'
  const [selectedStatus, setSelectedStatus] = useState('ALL') // 'ALL' | 'ACTIVE' | 'INACTIVE'

  // Modal chi tiết
  const [selectedUser, setSelectedUser] = useState(null)

  // Modal khóa tài khoản
  const [lockingUser, setLockingUser] = useState(null)
  const [lockReason, setLockReason] = useState('')
  const [lockError, setLockError] = useState(null)
  const [isLockSubmitting, setIsLockSubmitting] = useState(false)

  // Modal mở khóa tài khoản
  const [unlockingUser, setUnlockingUser] = useState(null)
  const [unlockError, setUnlockError] = useState(null)
  const [isUnlockSubmitting, setIsUnlockSubmitting] = useState(false)

  // Thông báo kết quả thao tác
  const [actionSuccessMessage, setActionSuccessMessage] = useState(null)

  // Tải thông tin tài khoản Admin hiện tại qua GET /api/v1/users/me
  const fetchCurrentAdmin = async () => {
    try {
      const res = await adminUserService.getCurrentAdmin()
      if (res?.data) {
        setCurrentAdminProfile(res.data)
      }
    } catch (err) {
      // Bỏ qua nếu lỗi, authUser từ store vẫn có giá trị
    }
  }

  // Tải danh sách người dùng từ API
  const fetchUsers = async () => {
    setIsLoading(true)
    setError(null)
    setBackendNotImplemented(false)

    try {
      const res = await adminUserService.getUsers({
        page: currentPage,
        size: pageSize,
      })

      // Backend có thể trả về ApiResponse<PageResponse<UserResponse>> hoặc ApiResponse<UserResponse[]>
      if (res?.data) {
        if (Array.isArray(res.data)) {
          setUsers(res.data)
          setTotalElements(res.data.length)
          setTotalPages(1)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setUsers(res.data.content)
          setTotalElements(res.data.totalElements || res.data.content.length)
          setTotalPages(res.data.totalPages || 1)
        } else {
          setUsers([])
          setTotalElements(0)
          setTotalPages(1)
        }
      } else {
        setUsers([])
        setTotalElements(0)
        setTotalPages(1)
      }
    } catch (err) {
      const status = err.response?.status
      if (status === 404) {
        // Backend chưa có endpoint GET /api/v1/admin/users
        setBackendNotImplemented(true)
        setError(
          'Máy chủ Backend chưa triển khai endpoint GET /api/v1/admin/users (theo tài liệu docs/API.md, chức năng này đang trong trạng thái TODO).'
        )
      } else if (status === 403) {
        setError('Tài khoản của bạn không có quyền truy cập khu vực quản trị (Yêu cầu ROLE_ADMIN).')
      } else if (status === 401) {
        setError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      } else {
        setError(
          err.response?.data?.message || err.message || 'Không thể tải danh sách người dùng từ hệ thống.'
        )
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchCurrentAdmin()
    fetchUsers()
  }, [currentPage])

  // Lọc dữ liệu hiển thị (kết hợp client-side filter)
  const filteredUsers = useMemo(() => {
    if (!users || users.length === 0) return []

    return users.filter((u) => {
      // Lọc theo từ khóa tìm kiếm
      const term = searchTerm.trim().toLowerCase()
      const matchKeyword =
        !term ||
        (u.fullName && u.fullName.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        (u.phone && u.phone.includes(term))

      // Lọc theo Role
      const matchRole =
        selectedRole === 'ALL' || u.role === selectedRole

      // Lọc theo Status
      const matchStatus =
        selectedStatus === 'ALL' ||
        (selectedStatus === 'ACTIVE' && u.active === true) ||
        (selectedStatus === 'INACTIVE' && u.active === false)

      return matchKeyword && matchRole && matchStatus
    })
  }, [users, searchTerm, selectedRole, selectedStatus])

  // Thống kê nhanh từ tập dữ liệu thực tế
  const stats = useMemo(() => {
    if (!users || users.length === 0) {
      return { total: 0, admins: 0, customers: 0, active: 0 }
    }
    const admins = users.filter((u) => u.role === 'ADMIN').length
    const customers = users.filter((u) => u.role === 'CUSTOMER').length
    const active = users.filter((u) => u.active === true).length
    return {
      total: totalElements || users.length,
      admins,
      customers,
      active,
    }
  }, [users, totalElements])

  // Đặt lại toàn bộ bộ lọc
  const handleResetFilters = () => {
    setSearchTerm('')
    setSelectedRole('ALL')
    setSelectedStatus('ALL')
  }

  // Định dạng ngày giờ thân thiện
  const formatDate = (isoString) => {
    if (!isoString) return '--'
    try {
      const date = new Date(isoString)
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date)
    } catch {
      return isoString
    }
  }

  // Lấy ký tự avatar
  const getAvatarLetter = (name, email) => {
    if (name && name.trim().length > 0) {
      const parts = name.trim().split(' ')
      return parts[parts.length - 1].charAt(0).toUpperCase()
    }
    if (email && email.length > 0) {
      return email.charAt(0).toUpperCase()
    }
    return 'U'
  }

  const activeAdmin = currentAdminProfile || authUser

  // Kiểm tra tài khoản có phải chính Admin đang thao tác hay không
  const isSelf = (targetUser) => {
    if (!targetUser || !activeAdmin) return false
    if (activeAdmin.id && targetUser.id && String(activeAdmin.id) === String(targetUser.id)) return true
    if (activeAdmin.email && targetUser.email && activeAdmin.email.toLowerCase() === targetUser.email.toLowerCase()) return true
    return false
  }

  // Mở / Đóng modal Khóa tài khoản
  const handleOpenLock = (user) => {
    if (isSelf(user)) return
    setLockingUser(user)
    setLockReason('')
    setLockError(null)
  }

  const handleCloseLock = () => {
    if (isLockSubmitting) return
    setLockingUser(null)
    setLockReason('')
    setLockError(null)
  }

  // Xác nhận khóa tài khoản
  const handleConfirmLock = async (e) => {
    e?.preventDefault()
    if (!lockingUser) return

    const trimmedReason = lockReason.trim()
    if (!trimmedReason) {
      setLockError('Vui lòng nhập lý do khóa tài khoản (bắt buộc, không được để trống).')
      return
    }
    if (trimmedReason.length > 500) {
      setLockError('Lý do khóa tài khoản không được vượt quá 500 ký tự.')
      return
    }

    setIsLockSubmitting(true)
    setLockError(null)
    try {
      await adminUserService.lockUser(lockingUser.id, trimmedReason)
      setActionSuccessMessage(`Đã khóa tài khoản "${lockingUser.email}" thành công.`)
      setLockingUser(null)
      setLockReason('')
      if (selectedUser?.id === lockingUser.id) {
        setSelectedUser(null)
      }
      await fetchUsers()
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Không thể khóa tài khoản. Vui lòng thử lại sau.'
      setLockError(msg)
    } finally {
      setIsLockSubmitting(false)
    }
  }

  // Mở / Đóng modal Mở khóa tài khoản
  const handleOpenUnlock = (user) => {
    setUnlockingUser(user)
    setUnlockError(null)
  }

  const handleCloseUnlock = () => {
    if (isUnlockSubmitting) return
    setUnlockingUser(null)
    setUnlockError(null)
  }

  // Xác nhận mở khóa tài khoản
  const handleConfirmUnlock = async () => {
    if (!unlockingUser) return

    setIsUnlockSubmitting(true)
    setUnlockError(null)
    try {
      await adminUserService.unlockUser(unlockingUser.id)
      setActionSuccessMessage(`Đã mở khóa tài khoản "${unlockingUser.email}" thành công.`)
      setUnlockingUser(null)
      if (selectedUser?.id === unlockingUser.id) {
        setSelectedUser(null)
      }
      await fetchUsers()
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Không thể mở khóa tài khoản. Vui lòng thử lại sau.'
      setUnlockError(msg)
    } finally {
      setIsUnlockSubmitting(false)
    }
  }

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
      {/* 1. Header & Admin Identification */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 tracking-tight">
              Quản lý người dùng
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200 uppercase tracking-wider">
              <span>🛡️</span> Admin Portal
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi danh sách tài khoản, vai trò và thông tin người dùng trong hệ thống thương mại điện tử.
          </p>
        </div>

        {/* Nút thao tác đầu trang */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchUsers}
            disabled={isLoading}
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-orange-600 shadow-sm transition-all cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách từ máy chủ"
          >
            <svg
              className={`w-4 h-4 ${isLoading ? 'animate-spin text-orange-600' : 'text-slate-500'}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
            <span>{isLoading ? 'Đang tải...' : 'Làm mới'}</span>
          </button>
        </div>
      </div>

      {/* 2. Thẻ thống kê (Stats Cards) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 my-6">
        {/* Card 1: Tổng người dùng */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center text-xl shrink-0">
            👥
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Tổng người dùng
            </div>
            <div className="text-2xl font-extrabold text-slate-800 mt-0.5">
              {backendNotImplemented ? '--' : stats.total}
            </div>
          </div>
        </div>

        {/* Card 2: Quản trị viên */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center text-xl shrink-0">
            👑
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Quản trị viên
            </div>
            <div className="text-2xl font-extrabold text-purple-700 mt-0.5">
              {backendNotImplemented ? '--' : stats.admins}
            </div>
          </div>
        </div>

        {/* Card 3: Khách hàng */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center text-xl shrink-0">
            🛍️
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Khách hàng
            </div>
            <div className="text-2xl font-extrabold text-blue-700 mt-0.5">
              {backendNotImplemented ? '--' : stats.customers}
            </div>
          </div>
        </div>

        {/* Card 4: Đang hoạt động */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center text-xl shrink-0">
            ✅
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Đang hoạt động
            </div>
            <div className="text-2xl font-extrabold text-emerald-700 mt-0.5">
              {backendNotImplemented ? '--' : stats.active}
            </div>
          </div>
        </div>
      </div>

      {/* Thông báo trạng thái Backend (Nếu endpoint chưa được triển khai) */}
      {backendNotImplemented && (
        <div className="mb-6 bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200 rounded-2xl p-5 text-amber-900 shadow-sm">
          <div className="flex items-start gap-3.5">
            <span className="text-2xl shrink-0 mt-0.5">📡</span>
            <div className="space-y-1.5 flex-1">
              <h3 className="font-bold text-base text-amber-950">
                Thông tin kết nối API Backend
              </h3>
              <p className="text-sm text-amber-800 leading-relaxed">
                {error}
              </p>
              <div className="pt-2 text-xs text-amber-700 flex flex-wrap items-center gap-x-4 gap-y-1">
                <span>
                  • Tài khoản quản trị đang xác thực:{' '}
                  <strong>{activeAdmin?.email || 'Admin'}</strong>
                </span>
                <span>
                  • Quyền hệ thống: <strong>{activeAdmin?.role || 'ADMIN'}</strong>
                </span>
                <span>
                  • Trạng thái token:{' '}
                  <span className="text-emerald-700 font-semibold">Đã xác thực hợp lệ</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Thông báo thành công thao tác (Khóa / Mở khóa) */}
      {actionSuccessMessage && (
        <div className="mb-6 bg-emerald-50 border border-emerald-200 rounded-2xl p-4 text-emerald-800 flex items-center justify-between gap-3 shadow-sm animate-in fade-in duration-200">
          <div className="flex items-center gap-2.5 text-sm font-semibold">
            <span className="text-lg">✅</span>
            <span>{actionSuccessMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionSuccessMessage(null)}
            className="text-xs font-bold text-emerald-700 hover:text-emerald-900 p-1 rounded-lg hover:bg-emerald-100 transition-colors cursor-pointer"
            title="Đóng thông báo"
          >
            ✕
          </button>
        </div>
      )}

      {/* Thông báo lỗi khác nếu có */}
      {!backendNotImplemented && error && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-2xl p-4 text-red-800 flex items-center justify-between gap-3 shadow-sm">
          <div className="flex items-center gap-2.5 text-sm font-medium">
            <span className="text-lg">⚠️</span>
            <span>{error}</span>
          </div>
          <button
            onClick={fetchUsers}
            className="text-xs font-bold text-red-700 underline hover:text-red-900 shrink-0 cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* 3. Khối tìm kiếm và bộ lọc (Search & Filters) */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200/80 shadow-sm mb-6 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
          {/* Ô tìm kiếm theo tên, email, sđt */}
          <div className="lg:col-span-5 relative">
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo họ tên, email hoặc số điện thoại..."
              className="w-full pl-10 pr-9 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent text-slate-800 placeholder-slate-400 transition-all"
            />
            {searchTerm && (
              <button
                type="button"
                onClick={() => setSearchTerm('')}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
                title="Xóa tìm kiếm"
              >
                ✕
              </button>
            )}
          </div>

          {/* Lọc theo Vai trò (Role) */}
          <div className="lg:col-span-3">
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500 text-slate-700 transition-all cursor-pointer"
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
              <option value="CUSTOMER">Khách hàng (CUSTOMER)</option>
            </select>
          </div>

          {/* Lọc theo Trạng thái (Status) */}
          <div className="lg:col-span-3">
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500 text-slate-700 transition-all cursor-pointer"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Bị khóa / Vô hiệu hóa</option>
            </select>
          </div>

          {/* Nút đặt lại */}
          <div className="lg:col-span-1 flex justify-end">
            <button
              type="button"
              onClick={handleResetFilters}
              disabled={!searchTerm && selectedRole === 'ALL' && selectedStatus === 'ALL'}
              className="w-full py-2 px-3 text-xs font-semibold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed text-center"
              title="Đặt lại bộ lọc"
            >
              Đặt lại
            </button>
          </div>
        </div>

        {/* Thanh đếm kết quả tìm kiếm */}
        {(searchTerm || selectedRole !== 'ALL' || selectedStatus !== 'ALL') && (
          <div className="text-xs text-slate-500 pt-1 flex items-center justify-between">
            <span>
              Tìm thấy <strong>{filteredUsers.length}</strong> kết quả phù hợp với bộ lọc
            </span>
          </div>
        )}
      </div>

      {/* 4. Bảng danh sách người dùng (User Data Table) */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="py-20 text-center">
            <LoadingSpinner size="lg" className="text-orange-600 mx-auto mb-3" />
            <p className="text-sm font-semibold text-slate-600">Đang tải danh sách người dùng...</p>
          </div>
        ) : filteredUsers.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 text-[11px] font-bold uppercase tracking-wider">
                  <th className="py-3.5 px-4 w-16 text-center">ID</th>
                  <th className="py-3.5 px-4">Người dùng</th>
                  <th className="py-3.5 px-4">Email</th>
                  <th className="py-3.5 px-4">Số điện thoại</th>
                  <th className="py-3.5 px-4">Vai trò</th>
                  <th className="py-3.5 px-4">Trạng thái</th>
                  <th className="py-3.5 px-4">Ngày tạo</th>
                  <th className="py-3.5 px-4 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-sm">
                {filteredUsers.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                    {/* ID */}
                    <td className="py-3.5 px-4 text-center text-xs font-mono font-bold text-slate-400">
                      #{item.id}
                    </td>

                    {/* Người dùng: Avatar + Họ tên */}
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-orange-500 to-amber-400 text-white font-bold text-xs flex items-center justify-center shadow-xs shrink-0">
                          {getAvatarLetter(item.fullName, item.email)}
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold text-slate-800 text-sm truncate">
                            {item.fullName || 'Chưa đặt tên'}
                          </div>
                          <div className="text-xs text-slate-400 truncate sm:hidden">
                            {item.email}
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Email */}
                    <td className="py-3.5 px-4 text-slate-700 font-medium">
                      {item.email}
                    </td>

                    {/* Số điện thoại */}
                    <td className="py-3.5 px-4 text-slate-600 text-xs font-mono">
                      {item.phone || '--'}
                    </td>

                    {/* Vai trò (Role) */}
                    <td className="py-3.5 px-4">
                      {item.role === 'ADMIN' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200">
                          <span>👑</span> ADMIN
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-700 border border-blue-200">
                          <span>🛍️</span> CUSTOMER
                        </span>
                      )}
                    </td>

                    {/* Trạng thái (Active) */}
                    <td className="py-3.5 px-4">
                      {item.active !== false ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700 border border-emerald-200">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          Hoạt động
                        </span>
                      ) : (
                        <div>
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700 border border-red-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                            Bị khóa
                          </span>
                          {item.lockReason && (
                            <div
                              className="text-[11px] text-red-600 truncate max-w-[160px] mt-0.5"
                              title={`Lý do: ${item.lockReason}`}
                            >
                              Lý do: {item.lockReason}
                            </div>
                          )}
                        </div>
                      )}
                    </td>

                    {/* Ngày tạo */}
                    <td className="py-3.5 px-4 text-xs text-slate-500">
                      {formatDate(item.createdAt)}
                    </td>

                    {/* Thao tác */}
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        {/* Nút xem chi tiết */}
                        <button
                          type="button"
                          onClick={() => setSelectedUser(item)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:text-orange-600 bg-slate-100 hover:bg-orange-50 rounded-lg transition-colors cursor-pointer"
                          title="Xem chi tiết hồ sơ người dùng"
                        >
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                          <span className="hidden xl:inline">Chi tiết</span>
                        </button>

                        {/* Nút Khóa / Mở khóa */}
                        {item.active !== false ? (
                          <button
                            type="button"
                            disabled={isSelf(item)}
                            onClick={() => handleOpenLock(item)}
                            className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold rounded-lg transition-colors ${
                              isSelf(item)
                                ? 'bg-slate-100 text-slate-400 cursor-not-allowed opacity-60'
                                : 'text-red-700 bg-red-50 hover:bg-red-100 hover:text-red-800 cursor-pointer'
                            }`}
                            title={isSelf(item) ? 'Không thể tự khóa tài khoản của chính mình' : 'Khóa tài khoản này'}
                          >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                            <span>Khóa</span>
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleOpenUnlock(item)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 hover:text-emerald-800 rounded-lg transition-colors cursor-pointer"
                            title="Mở khóa tài khoản này"
                          >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z" />
                            </svg>
                            <span>Mở khóa</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          /* Empty state */
          <div className="py-16 px-4 text-center">
            <div className="w-16 h-16 bg-slate-100 text-slate-400 rounded-2xl flex items-center justify-center text-3xl mx-auto mb-3">
              👥
            </div>
            <h3 className="text-base font-bold text-slate-700 mb-1">
              {backendNotImplemented
                ? 'Chưa có dữ liệu người dùng từ máy chủ'
                : 'Không tìm thấy người dùng'}
            </h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto mb-4">
              {backendNotImplemented
                ? 'Giao diện đã sẵn sàng kết nối. Khi tầng Backend hoàn thiện controller danh sách người dùng, bảng dữ liệu sẽ tự động hiển thị tại đây.'
                : 'Không có tài khoản nào phù hợp với các tiêu chí tìm kiếm hoặc bộ lọc hiện tại của bạn.'}
            </p>
            {!backendNotImplemented && (searchTerm || selectedRole !== 'ALL' || selectedStatus !== 'ALL') && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all cursor-pointer"
              >
                Đặt lại bộ lọc
              </button>
            )}
          </div>
        )}

        {/* Phân trang (Pagination) */}
        {totalPages > 1 && (
          <div className="py-3.5 px-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <div>
              Trang <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong> (Tổng cộng {totalElements} người dùng)
            </div>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={currentPage === 0 || isLoading}
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                className="px-3 py-1.5 bg-white border border-slate-300 rounded-lg hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-semibold cursor-pointer"
              >
                Trang trước
              </button>
              <button
                type="button"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => setCurrentPage((prev) => prev + 1)}
                className="px-3 py-1.5 bg-white border border-slate-300 rounded-lg hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-semibold cursor-pointer"
              >
                Trang sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 5. Modal Chi tiết người dùng (User Details Modal) */}
      {selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-slate-100 transform transition-all">
            {/* Modal Header */}
            <div className="flex items-start justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-400 text-white font-extrabold text-base flex items-center justify-center shadow-md">
                  {getAvatarLetter(selectedUser.fullName, selectedUser.email)}
                </div>
                <div>
                  <h3 className="text-lg font-extrabold text-slate-800 leading-tight">
                    {selectedUser.fullName || 'Người dùng hệ thống'}
                  </h3>
                  <div className="text-xs text-slate-500 mt-0.5">
                    Mã tài khoản: <span className="font-mono font-bold text-slate-700">#{selectedUser.id}</span>
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setSelectedUser(null)}
                className="text-slate-400 hover:text-slate-600 p-1.5 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
                title="Đóng modal"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Modal Body - Thông tin chi tiết */}
            <div className="py-5 space-y-4">
              <div className="grid grid-cols-2 gap-4 text-xs">
                {/* Email */}
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <div className="font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Email đăng ký
                  </div>
                  <div className="font-semibold text-slate-800 break-all">
                    {selectedUser.email}
                  </div>
                </div>

                {/* Số điện thoại */}
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <div className="font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Số điện thoại
                  </div>
                  <div className="font-semibold text-slate-800 font-mono">
                    {selectedUser.phone || 'Chưa cập nhật'}
                  </div>
                </div>

                {/* Vai trò */}
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <div className="font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Vai trò hệ thống
                  </div>
                  <div>
                    {selectedUser.role === 'ADMIN' ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200">
                        👑 QUẢN TRỊ VIÊN (ADMIN)
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-700 border border-blue-200">
                        🛍️ KHÁCH HÀNG (CUSTOMER)
                      </span>
                    )}
                  </div>
                </div>

                {/* Trạng thái tài khoản */}
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                  <div className="font-bold text-slate-500 uppercase tracking-wider mb-1">
                    Trạng thái tài khoản
                  </div>
                  <div>
                    {selectedUser.active !== false ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700 border border-emerald-200">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                        Đang hoạt động
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-700 border border-red-200">
                        <span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>
                        Bị khóa / Vô hiệu
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Địa chỉ */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                <div className="font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Địa chỉ giao hàng / liên hệ
                </div>
                <div className="font-semibold text-slate-800 leading-relaxed">
                  {selectedUser.address || 'Chưa cung cấp địa chỉ'}
                </div>
              </div>

              {/* Thời gian tạo tài khoản */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs flex items-center justify-between">
                <span className="font-bold text-slate-500 uppercase tracking-wider">
                  Thời gian đăng ký tài khoản
                </span>
                <span className="font-semibold text-slate-700">
                  {formatDate(selectedUser.createdAt)}
                </span>
              </div>

              {/* Thông tin khóa tài khoản nếu bị khóa */}
              {selectedUser.active === false && (
                <div className="p-3.5 bg-red-50/90 border border-red-200 rounded-2xl text-xs space-y-1.5">
                  <div className="font-bold text-red-700 flex items-center gap-1.5 text-xs">
                    <span>🔒</span> THÔNG TIN KHÓA TÀI KHOẢN
                  </div>
                  <div className="text-red-900 leading-relaxed">
                    <strong>Lý do khóa:</strong> {selectedUser.lockReason || 'Không có lý do cụ thể'}
                  </div>
                  {selectedUser.lockedAt && (
                    <div className="text-[11px] text-red-600">
                      <strong>Thời điểm khóa:</strong> {formatDate(selectedUser.lockedAt)}
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
              <div>
                {selectedUser.active !== false ? (
                  <button
                    type="button"
                    disabled={isSelf(selectedUser)}
                    onClick={() => {
                      const u = selectedUser
                      setSelectedUser(null)
                      handleOpenLock(u)
                    }}
                    className={`inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold rounded-xl transition-colors ${
                      isSelf(selectedUser)
                        ? 'bg-slate-100 text-slate-400 cursor-not-allowed opacity-60'
                        : 'text-red-700 bg-red-50 hover:bg-red-100 cursor-pointer'
                    }`}
                    title={isSelf(selectedUser) ? 'Không thể tự khóa tài khoản của chính mình' : 'Khóa tài khoản này'}
                  >
                    <span>🔒</span>
                    <span>Khóa tài khoản</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      const u = selectedUser
                      setSelectedUser(null)
                      handleOpenUnlock(u)
                    }}
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-xl transition-colors cursor-pointer"
                    title="Mở khóa tài khoản này"
                  >
                    <span>🔓</span>
                    <span>Mở khóa tài khoản</span>
                  </button>
                )}
              </div>

              <button
                type="button"
                onClick={() => setSelectedUser(null)}
                className="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. Modal Khóa tài khoản (Lock User Modal) */}
      {lockingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl border border-slate-100 transform transition-all">
            <div className="flex items-start justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-xl shrink-0">
                  🔒
                </div>
                <div>
                  <h3 className="text-lg font-extrabold text-slate-800 leading-tight">
                    Khóa tài khoản
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Xác nhận tạm dừng quyền truy cập của người dùng
                  </p>
                </div>
              </div>
              <button
                type="button"
                disabled={isLockSubmitting}
                onClick={handleCloseLock}
                className="text-slate-400 hover:text-slate-600 p-1.5 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer disabled:opacity-50"
                title="Đóng modal"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleConfirmLock} className="py-5 space-y-4">
              {/* Thông tin tài khoản bị khóa */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                <div className="text-slate-500">Tài khoản bị ảnh hưởng:</div>
                <div className="font-bold text-slate-800 text-sm">{lockingUser.fullName || 'Người dùng'}</div>
                <div className="text-slate-600 font-mono">{lockingUser.email} (ID: #{lockingUser.id})</div>
              </div>

              {/* Cảnh báo bảo mật */}
              <div className="text-xs text-amber-800 bg-amber-50 border border-amber-200/80 rounded-xl p-3 leading-relaxed flex items-start gap-2">
                <span className="shrink-0 text-sm">⚠️</span>
                <span>
                  Sau khi bị khóa, người dùng này sẽ <strong>không thể đăng nhập</strong> hoặc thực hiện thao tác trên hệ thống (mọi token JWT cũ sẽ bị từ chối ngay lập tức).
                </span>
              </div>

              {/* Lý do khóa (Bắt buộc) */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <label htmlFor="lockReasonInput" className="font-bold text-slate-700">
                    Lý do khóa <span className="text-red-500">*</span>
                  </label>
                  <span className={`text-[11px] ${lockReason.length > 500 ? 'text-red-500 font-bold' : 'text-slate-400'}`}>
                    {lockReason.length} / 500 ký tự
                  </span>
                </div>
                <textarea
                  id="lockReasonInput"
                  rows={3}
                  value={lockReason}
                  onChange={(e) => {
                    setLockReason(e.target.value)
                    if (lockError) setLockError(null)
                  }}
                  placeholder="Nhập lý do khóa cụ thể (VD: Vi phạm chính sách thương mại, tài khoản gian lận...)"
                  maxLength={500}
                  disabled={isLockSubmitting}
                  className="w-full px-3 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent text-slate-800 placeholder-slate-400 transition-all resize-none"
                />
              </div>

              {/* Thông báo lỗi nếu có */}
              {lockError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium flex items-start gap-2">
                  <span className="shrink-0 text-sm">⚠️</span>
                  <span>{lockError}</span>
                </div>
              )}

              {/* Nút hành động */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  disabled={isLockSubmitting}
                  onClick={handleCloseLock}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors cursor-pointer disabled:opacity-50"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isLockSubmitting || !lockReason.trim()}
                  className="inline-flex items-center gap-1.5 px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLockSubmitting ? (
                    <>
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>Đang xử lý...</span>
                    </>
                  ) : (
                    <>
                      <span>🔒</span>
                      <span>Xác nhận khóa</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 7. Modal Mở khóa tài khoản (Unlock User Modal) */}
      {unlockingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl border border-slate-100 transform transition-all">
            <div className="flex items-start justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center text-xl shrink-0">
                  🔓
                </div>
                <div>
                  <h3 className="text-lg font-extrabold text-slate-800 leading-tight">
                    Mở khóa tài khoản
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Khôi phục quyền truy cập cho người dùng
                  </p>
                </div>
              </div>
              <button
                type="button"
                disabled={isUnlockSubmitting}
                onClick={handleCloseUnlock}
                className="text-slate-400 hover:text-slate-600 p-1.5 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer disabled:opacity-50"
                title="Đóng modal"
              >
                ✕
              </button>
            </div>

            <div className="py-5 space-y-4">
              {/* Thông tin tài khoản được mở khóa */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                <div className="text-slate-500">Tài khoản được mở khóa:</div>
                <div className="font-bold text-slate-800 text-sm">{unlockingUser.fullName || 'Người dùng'}</div>
                <div className="text-slate-600 font-mono">{unlockingUser.email} (ID: #{unlockingUser.id})</div>
              </div>

              {/* Lý do khóa trước đó nếu có */}
              {unlockingUser.lockReason && (
                <div className="p-3 bg-red-50/70 border border-red-200/80 rounded-xl text-xs text-red-900 space-y-1">
                  <div className="font-bold text-red-700 flex items-center gap-1">
                    <span>🔒</span> Lý do khóa trước đó:
                  </div>
                  <div className="italic leading-relaxed">{unlockingUser.lockReason}</div>
                  {unlockingUser.lockedAt && (
                    <div className="text-[11px] text-red-600 mt-1">
                      Thời điểm khóa: {formatDate(unlockingUser.lockedAt)}
                    </div>
                  )}
                </div>
              )}

              <div className="text-xs text-emerald-800 bg-emerald-50 border border-emerald-200/80 rounded-xl p-3 leading-relaxed flex items-start gap-2">
                <span className="shrink-0 text-sm">💡</span>
                <span>
                  Sau khi mở khóa, trạng thái tài khoản sẽ chuyển thành <strong>Đang hoạt động</strong> và người dùng có thể đăng nhập lại vào hệ thống bình thường.
                </span>
              </div>

              {/* Thông báo lỗi nếu có */}
              {unlockError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-medium flex items-start gap-2">
                  <span className="shrink-0 text-sm">⚠️</span>
                  <span>{unlockError}</span>
                </div>
              )}

              {/* Nút hành động */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  disabled={isUnlockSubmitting}
                  onClick={handleCloseUnlock}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition-colors cursor-pointer disabled:opacity-50"
                >
                  Hủy
                </button>
                <button
                  type="button"
                  disabled={isUnlockSubmitting}
                  onClick={handleConfirmUnlock}
                  className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-sm transition-all cursor-pointer disabled:opacity-50"
                >
                  {isUnlockSubmitting ? (
                    <>
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>Đang xử lý...</span>
                    </>
                  ) : (
                    <>
                      <span>🔓</span>
                      <span>Xác nhận mở khóa</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default UserManagementPage
