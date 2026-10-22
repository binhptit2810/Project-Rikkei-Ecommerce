import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  Package,
  Tags,
  ClipboardList,
  Star,
  Percent,
  Settings,
  X,
  Store,
  Shield,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

export const AdminSidebar = ({ onClose }) => {
  const location = useLocation()
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
          key: 'reviews',
          label: 'Quản lý đánh giá',
          path: '/admin/reviews',
          icon: Star,
          isImplemented: false,
        },
        {
          key: 'promotions',
          label: 'Quản lý khuyến mãi',
          path: '/admin/promotions',
          icon: Percent,
          isImplemented: false,
        },
      ],
    },
    {
      groupTitle: 'HỆ THỐNG',
      items: [
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
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  return (
    <aside className="w-64 h-full bg-slate-950 text-slate-300 flex flex-col justify-between border-r border-slate-800 select-none">
      {/* 1. Brand Logo & Title */}
      <div>
        <div className="h-16 px-5 flex items-center justify-between border-b border-slate-800/80 bg-slate-950">
          <Link
            to="/admin/dashboard"
            onClick={onClose}
            className="flex items-center gap-3 group"
          >
            <div className="w-9 h-9 rounded-xl bg-orange-600 text-white font-extrabold text-base flex items-center justify-center shadow-2xs">
              R
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-sm text-white tracking-tight group-hover:text-orange-400 transition-colors">
                Rikkei Commerce
              </span>
              <span className="text-[10px] font-bold text-orange-500 tracking-wider uppercase">
                Admin Console
              </span>
            </div>
          </Link>

          {/* Close button on mobile */}
          {onClose && (
            <button
              type="button"
              onClick={onClose}
              className="lg:hidden p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              title="Đóng sidebar"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* 2. Navigation Items List */}
        <nav className="p-3 space-y-6 overflow-y-auto max-h-[calc(100vh-140px)]">
          {navigationGroups.map((group, groupIdx) => (
            <div key={groupIdx}>
              <div className="text-[10px] font-bold text-slate-500 uppercase tracking-widest px-3 mb-2">
                {group.groupTitle}
              </div>
              <ul className="space-y-1">
                {group.items.map((item) => {
                  const Icon = item.icon
                  const isActive = location.pathname === item.path

                  if (item.isImplemented) {
                    return (
                      <li key={item.key}>
                        <Link
                          to={item.path}
                          onClick={onClose}
                          className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all group ${
                            isActive
                              ? 'bg-orange-600 text-white shadow-2xs'
                              : 'text-slate-300 hover:bg-slate-900 hover:text-white'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400 group-hover:text-orange-400 transition-colors'}`} />
                            <span>{item.label}</span>
                          </div>
                          {isActive && (
                            <span className="w-1.5 h-1.5 rounded-full bg-white"></span>
                          )}
                        </Link>
                      </li>
                    )
                  }

                  // Non-implemented item
                  return (
                    <li key={item.key}>
                      <div
                        className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-medium text-slate-500 cursor-not-allowed hover:bg-slate-900/40 transition-colors group"
                        title="Chức năng đang phát triển — Sắp ra mắt"
                      >
                        <div className="flex items-center gap-3">
                          <Icon className="w-4 h-4 text-slate-600 group-hover:text-slate-500 transition-colors" />
                          <span className="text-slate-400">{item.label}</span>
                        </div>
                        <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-md bg-slate-900 text-slate-500 border border-slate-800 shrink-0">
                          Sắp tới
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
      <div className="p-3 border-t border-slate-800/80 bg-slate-950">
        <div className="flex items-center gap-3 p-2 rounded-xl bg-slate-900 border border-slate-800">
          <div className="w-8 h-8 rounded-lg bg-orange-600 text-white font-bold text-xs flex items-center justify-center shrink-0">
            {getInitial(user?.fullName)}
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-xs font-bold text-white truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-slate-400 truncate">
              {user?.email || 'admin@rikkei.vn'}
            </div>
          </div>
          <span className="px-1.5 py-0.5 text-[9px] font-bold rounded bg-slate-800 text-orange-400 border border-slate-700 uppercase tracking-wider shrink-0">
            ADMIN
          </span>
        </div>
      </div>
    </aside>
  )
}

export default AdminSidebar
