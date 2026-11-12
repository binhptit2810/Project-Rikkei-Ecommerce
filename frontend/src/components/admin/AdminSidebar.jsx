import React from 'react'
import { Link, NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  Package,
  Tags,
  ClipboardList,
  Boxes,
  Star,
  Percent,
  Settings,
  X,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

export const AdminSidebar = ({ onClose }) => {
  const { user } = useAuthStore()

  const navigationGroups = [
    {
      groupTitle: 'TỔNG QUAN',
      items: [
        {
          key: 'dashboard',
          label: 'Bảng điều khiển',
          path: '/admin/dashboard',
          icon: LayoutDashboard,
          isImplemented: true,
        },
      ],
    },
    {
      groupTitle: 'QUẢN LÝ DỮ LIỆU',
      items: [
        {
          key: 'users',
          label: 'Quản lý người dùng',
          path: '/admin/users',
          icon: Users,
          isImplemented: true,
        },
        {
          key: 'products',
          label: 'Quản lý sản phẩm',
          path: '/admin/products',
          icon: Package,
          isImplemented: true,
        },
        {
          key: 'categories',
          label: 'Quản lý danh mục',
          path: '/admin/categories',
          icon: Tags,
          isImplemented: true,
        },
        {
          key: 'orders',
          label: 'Quản lý đơn hàng',
          path: '/admin/orders',
          icon: ClipboardList,
          isImplemented: true,
        },
        {
          key: 'inventory',
          label: 'Quản lý tồn kho',
          path: '/admin/inventory',
          icon: Boxes,
          isImplemented: true,
        },
        {
          key: 'reviews',
          label: 'Quản lý đánh giá',
          path: '/admin/reviews',
          icon: Star,
          isImplemented: true,
        },
      ],
    },
    {
      groupTitle: 'TIẾP THỊ & HỆ THỐNG',
      items: [
        {
          key: 'vouchers',
          label: 'Mã giảm giá (Vouchers)',
          path: '/admin/vouchers',
          icon: Percent,
          isImplemented: true,
        },
        {
          key: 'settings',
          label: 'Cài đặt hệ thống',
          path: '/admin/settings',
          icon: Settings,
          isImplemented: false,
        },
      ],
    },
  ]

  const getInitial = (name) => {
    if (!name) return 'A'
    return name.trim().charAt(0).toUpperCase()
  }

  return (
    <aside className="w-64 h-screen bg-zinc-950 border-r border-zinc-850 flex flex-col justify-between select-none">
      <div>
        {/* 1. Brand Logo */}
        <div className="h-16 px-5 border-b border-zinc-850 flex items-center justify-between">
          <Link
            to="/admin/dashboard"
            onClick={onClose}
            className="flex items-center gap-2.5 group"
          >
            <div className="w-7 h-7 rounded-md bg-zinc-100 text-zinc-950 font-semibold text-xs flex items-center justify-center">
              R
            </div>
            <div className="flex flex-col">
              <span className="font-semibold text-sm text-zinc-100 tracking-tight leading-none">
                Rikkei Store
              </span>
              <span className="text-[10px] font-medium text-zinc-500 tracking-wider uppercase mt-0.5">
                Admin Console
              </span>
            </div>
          </Link>

          {/* Close button on mobile */}
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="lg:hidden p-1.5 rounded-md text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors cursor-pointer"
              title="Đóng sidebar"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* 2. Navigation Items List */}
        <nav className="p-3 space-y-5 overflow-y-auto max-h-[calc(100vh-140px)]">
          {navigationGroups.map((group, groupIdx) => (
            <div key={groupIdx}>
              <div className="text-[10px] font-medium text-zinc-500 uppercase tracking-wider px-2.5 mb-1.5">
                {group.groupTitle}
              </div>
              <ul className="space-y-0.5">
                {group.items.map((item) => {
                  const Icon = item.icon

                  if (item.isImplemented) {
                    return (
                      <li key={item.key}>
                        <NavLink
                          to={item.path}
                          onClick={onClose}
                          className={({ isActive }) =>
                            `flex items-center justify-between px-2.5 py-2 rounded-md text-xs font-medium transition-colors group ${
                              isActive
                                ? 'bg-zinc-800 text-zinc-50 shadow-xs'
                                : 'text-zinc-400 hover:bg-zinc-900 hover:text-zinc-200'
                            }`
                          }
                        >
                          {({ isActive }) => (
                            <>
                              <div className="flex items-center gap-2.5">
                                <Icon
                                  className={`w-4 h-4 ${
                                    isActive
                                      ? 'text-zinc-50'
                                      : 'text-zinc-500 group-hover:text-zinc-300 transition-colors'
                                  }`}
                                />
                                <span>{item.label}</span>
                              </div>
                              {isActive && (
                                <span className="w-1.5 h-1.5 rounded-full bg-zinc-200"></span>
                              )}
                            </>
                          )}
                        </NavLink>
                      </li>
                    )
                  }

                  // Non-implemented item
                  return (
                    <li key={item.key}>
                      <div
                        className="flex items-center justify-between px-2.5 py-2 rounded-md text-xs font-medium text-zinc-600 cursor-not-allowed hover:bg-zinc-900/40 transition-colors group"
                        title="Chức năng đang phát triển trong Sprint 2"
                      >
                        <div className="flex items-center gap-2.5">
                          <Icon className="w-4 h-4 text-zinc-600 group-hover:text-zinc-500 transition-colors" />
                          <span className="text-zinc-500">{item.label}</span>
                        </div>
                        <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-zinc-900 text-zinc-500 border border-zinc-850 shrink-0">
                          Sprint 2
                        </span>
                      </div>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </nav>
      </div>

      {/* 3. Footer: Admin Info Card */}
      <div className="p-3 border-t border-zinc-850 bg-zinc-950">
        <div className="flex items-center gap-2.5 p-2 rounded-md bg-zinc-900 border border-zinc-800">
          <div className="w-7 h-7 rounded-md bg-zinc-800 text-zinc-200 font-semibold text-xs flex items-center justify-center shrink-0 border border-zinc-700">
            {getInitial(user?.fullName)}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-semibold text-zinc-200 truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-zinc-500 truncate">
              {user?.email || 'admin@ecommerce.com'}
            </div>
          </div>
        </div>
      </div>
    </aside>
  )
}

export default AdminSidebar
