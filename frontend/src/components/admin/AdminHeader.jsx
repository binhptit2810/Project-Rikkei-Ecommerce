import React from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Header dành riêng cho khu vực quản trị Admin (Admin Header).
 * 
 * Chức năng:
 * 1. Nút mở Sidebar Drawer trên mobile/tablet.
 * 2. Tiêu đề phân hệ & Breadcrumb điều hướng.
 * 3. Nút chuyển nhanh sang giao diện khách hàng (Xem cửa hàng).
 * 4. Thông tin tài khoản Admin đang đăng nhập.
 * 5. Nút đăng xuất an toàn khỏi phiên làm việc quản trị.
 */
export const AdminHeader = ({ onToggleSidebar }) => {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  // Xác định tiêu đề theo route hiện tại
  const getPageInfo = () => {
    switch (location.pathname) {
      case '/admin/users':
        return {
          title: 'Quản lý người dùng',
          breadcrumb: 'Quản trị hệ thống / Quản lý người dùng',
        }
      case '/admin/dashboard':
      case '/admin':
        return {
          title: 'Dashboard',
          breadcrumb: 'Quản trị hệ thống / Dashboard',
        }
      default:
        return {
          title: 'Khu vực quản trị',
          breadcrumb: 'Quản trị hệ thống',
        }
    }
  }

  const { title, breadcrumb } = getPageInfo()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const getInitial = (name) => {
    if (!name) return 'A'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  return (
    <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-20 px-4 sm:px-6 lg:px-8 flex items-center justify-between shadow-xs">
      {/* Phía trái: Hamburger Mobile & Breadcrumb */}
      <div className="flex items-center gap-3">
        {/* Mobile toggle button */}
        <button
          type="button"
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors cursor-pointer"
          aria-label="Mở menu quản trị"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>

        <div>
          <div className="text-[11px] font-semibold text-slate-400 hidden sm:block">
            {breadcrumb}
          </div>
          <h2 className="text-base sm:text-lg font-extrabold text-slate-800 leading-tight">
            {title}
          </h2>
        </div>
      </div>

      {/* Phía phải: Xem cửa hàng, Hồ sơ Admin, Nút Đăng xuất */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Nút Xem cửa hàng (Trang Customer) */}
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:text-orange-600 hover:bg-orange-50 border border-slate-200/80 transition-all shadow-2xs"
          title="Chuyển sang giao diện cửa hàng"
        >
          <span className="text-sm">🛒</span>
          <span className="hidden sm:inline">Xem cửa hàng</span>
        </Link>

        {/* Thông tin Admin Pill */}
        <div className="flex items-center gap-2 pl-2 sm:pl-3 border-l border-slate-200">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-600 to-indigo-500 text-white font-bold text-xs flex items-center justify-center shadow-xs">
            {getInitial(user?.fullName)}
          </div>
          <div className="hidden md:block text-left">
            <div className="text-xs font-bold text-slate-800 leading-tight max-w-[120px] truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-purple-600 font-bold uppercase tracking-wider">
              {user?.role || 'ADMIN'}
            </div>
          </div>

          {/* Nút Đăng xuất */}
          <button
            type="button"
            onClick={handleLogout}
            className="p-2 sm:px-2.5 sm:py-1.5 text-xs font-semibold text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors flex items-center gap-1.5 cursor-pointer"
            title="Đăng xuất khỏi hệ thống"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            <span className="hidden sm:inline">Đăng xuất</span>
          </button>
        </div>
      </div>
    </header>
  )
}

export default AdminHeader
