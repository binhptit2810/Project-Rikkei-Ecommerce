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
    navigate('/admin/login')
  }

  const getInitial = (name) => {
    if (!name) return 'A'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  return (
    <header className="h-14 bg-white border-b border-zinc-200 sticky top-0 z-20 px-4 sm:px-6 flex items-center justify-between">
      {/* Phía trái: Hamburger Mobile & Breadcrumb */}
      <div className="flex items-center gap-3">
        {/* Mobile toggle button */}
        <button
          type="button"
          onClick={onToggleSidebar}
          className="lg:hidden p-1.5 rounded-md text-zinc-600 hover:text-zinc-950 hover:bg-zinc-100 transition-colors cursor-pointer"
          aria-label="Mở menu quản trị"
        >
          <Menu className="w-4 h-4" />
        </button>

        <div>
          <div className="flex items-center gap-1.5 text-xs text-zinc-500 hidden sm:flex">
            <span>Admin</span>
            <ChevronRight className="w-3 h-3 text-zinc-400" />
            <span className="text-zinc-700 font-medium">{breadcrumb}</span>
          </div>
          <h2 className="text-sm sm:text-base font-semibold text-zinc-950 tracking-tight leading-none sm:mt-0.5">
            {title}
          </h2>
        </div>
      </div>

      {/* Phía phải: Hồ sơ Admin, Nút Đăng xuất */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Thông tin Admin Pill */}
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-md bg-zinc-900 text-zinc-50 font-medium text-xs flex items-center justify-center">
            {getInitial(user?.fullName)}
          </div>
          <div className="hidden md:block text-left">
            <div className="text-xs font-medium text-zinc-900 leading-tight max-w-[120px] truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-zinc-500 font-medium uppercase tracking-wider">
              {user?.role || 'ADMIN'}
            </div>
          </div>

          {/* Nút Đăng xuất */}
          <button
            type="button"
            onClick={handleLogout}
            className="p-1.5 sm:px-2.5 sm:py-1.5 text-xs font-medium text-zinc-600 hover:text-red-600 hover:bg-zinc-100 rounded-md transition-colors flex items-center gap-1.5 cursor-pointer border border-transparent hover:border-zinc-200"
            title="Đăng xuất khỏi hệ thống"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Đăng xuất</span>
          </button>
        </div>
      </div>
    </header>
  )
}

export default AdminHeader
