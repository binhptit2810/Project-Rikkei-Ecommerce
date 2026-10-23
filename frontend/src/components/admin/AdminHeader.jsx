import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Menu, LogOut, ChevronRight } from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

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
          breadcrumb: 'Người dùng',
        }
      case '/admin/products':
        return {
          title: 'Quản lý sản phẩm',
          breadcrumb: 'Sản phẩm',
        }
      case '/admin/categories':
        return {
          title: 'Quản lý danh mục',
          breadcrumb: 'Danh mục',
        }
      case '/admin/orders':
        return {
          title: 'Quản lý đơn hàng',
          breadcrumb: 'Đơn hàng',
        }
      case '/admin/inventory':
        return {
          title: 'Quản lý tồn kho',
          breadcrumb: 'Tồn kho',
        }
      case '/admin/dashboard':
      case '/admin':
        return {
          title: 'Bảng điều khiển',
          breadcrumb: 'Dashboard',
        }
      default:
        return {
          title: 'Quản trị hệ thống',
          breadcrumb: 'Hệ thống',
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
    <header className="h-16 bg-white border-b border-slate-200 sticky top-0 z-20 px-4 sm:px-6 lg:px-8 flex items-center justify-between shadow-2xs">
      {/* Phía trái: Hamburger Mobile & Breadcrumb */}
      <div className="flex items-center gap-3">
        {/* Mobile toggle button */}
        <button
          type="button"
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors cursor-pointer"
          aria-label="Mở menu quản trị"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div>
          <div className="flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 hidden sm:flex">
            <span>Admin</span>
            <ChevronRight className="w-3 h-3 text-slate-300" />
            <span className="text-slate-600">{breadcrumb}</span>
          </div>
          <h2 className="text-base sm:text-lg font-bold text-slate-900 leading-tight">
            {title}
          </h2>
        </div>
      </div>

      {/* Phía phải: Hồ sơ Admin, Nút Đăng xuất */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Thông tin Admin Pill */}
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-slate-900 text-white font-bold text-xs flex items-center justify-center">
            {getInitial(user?.fullName)}
          </div>
          <div className="hidden md:block text-left">
            <div className="text-xs font-bold text-slate-900 leading-tight max-w-[120px] truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-orange-600 font-bold uppercase tracking-wider">
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
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:inline">Đăng xuất</span>
          </button>
        </div>
      </div>
    </header>
  )
}

export default AdminHeader
