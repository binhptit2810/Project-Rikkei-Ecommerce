import React from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Sidebar quản trị hệ thống (Admin Sidebar).
 * 
 * Bố cục:
 * 1. Thương hiệu: Logo Rikkei Ecommerce & Nhãn ADMIN PANEL.
 * 2. Nhóm điều hướng:
 *    - Bảng điều khiển (Dashboard - Sắp ra mắt)
 *    - Quản lý người dùng (User Management - HOẠT ĐỘNG ĐẦY ĐỦ)
 *    - Quản lý sản phẩm (Sắp ra mắt)
 *    - Quản lý danh mục (Sắp ra mắt)
 *    - Quản lý đơn hàng (Sắp ra mắt)
 *    - Quản lý đánh giá (Sắp ra mắt)
 *    - Quản lý khuyến mãi (Sắp ra mắt)
 *    - Cài đặt (Sắp ra mắt)
 * 3. Chân trang: Thông tin tài khoản Admin đang đăng nhập.
 */
export const AdminSidebar = ({ onClose }) => {
  const location = useLocation()
  const { user } = useAuthStore()

  // Danh sách các mục điều hướng trên Sidebar
  const navigationGroups = [
    {
      groupTitle: 'TỔNG QUAN',
      items: [
        {
          key: 'dashboard',
          label: 'Bảng điều khiển',
          path: '/admin/dashboard',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
            </svg>
          ),
          isImplemented: true, // Kích hoạt chức năng Dashboard
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
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          ),
          isImplemented: true, // DUY NHẤT mục này có chức năng hoạt động
        },
        {
          key: 'products',
          label: 'Quản lý sản phẩm',
          path: '/admin/products',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          ),
          isImplemented: false,
        },
        {
          key: 'categories',
          label: 'Quản lý danh mục',
          path: '/admin/categories',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
          ),
          isImplemented: false,
        },
        {
          key: 'orders',
          label: 'Quản lý đơn hàng',
          path: '/admin/orders',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
            </svg>
          ),
          isImplemented: false,
        },
        {
          key: 'reviews',
          label: 'Quản lý đánh giá',
          path: '/admin/reviews',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
            </svg>
          ),
          isImplemented: false,
        },
        {
          key: 'promotions',
          label: 'Quản lý khuyến mãi',
          path: '/admin/promotions',
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7" />
            </svg>
          ),
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
          icon: (
            <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          ),
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
    <aside className="w-64 h-full bg-slate-900 text-slate-300 flex flex-col justify-between border-r border-slate-800 select-none">
      {/* 1. Brand Logo & Title */}
      <div>
        <div className="h-16 px-5 flex items-center justify-between border-b border-slate-800 bg-slate-950/60">
          <Link
            to="/admin/dashboard"
            onClick={onClose}
            className="flex items-center gap-3 group"
          >
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-orange-600 to-amber-500 text-white font-black text-lg flex items-center justify-center shadow-md shadow-orange-950/50">
              R
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-base text-white tracking-tight group-hover:text-orange-400 transition-colors">
                Rikkei Ecommerce
              </span>
              <span className="text-[10px] font-bold text-orange-400 tracking-wider uppercase">
                Admin Panel
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
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
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
                  const isActive = location.pathname === item.path

                  if (item.isImplemented) {
                    return (
                      <li key={item.key}>
                        <Link
                          to={item.path}
                          onClick={onClose}
                          className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all group ${
                            isActive
                              ? 'bg-orange-600 text-white font-semibold shadow-md shadow-orange-950/30'
                              : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <span className={isActive ? 'text-white' : 'text-slate-400 group-hover:text-orange-400 transition-colors'}>
                              {item.icon}
                            </span>
                            <span>{item.label}</span>
                          </div>
                          {isActive && (
                            <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse"></span>
                          )}
                        </Link>
                      </li>
                    )
                  }

                  // Mục chưa triển khai: hiển thị badge "Sắp ra mắt" và disabled
                  return (
                    <li key={item.key}>
                      <div
                        className="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-500 cursor-not-allowed hover:bg-slate-800/30 transition-colors group"
                        title="Chức năng đang phát triển — Sắp ra mắt"
                      >
                        <div className="flex items-center gap-3">
                          <span className="text-slate-600 group-hover:text-slate-500 transition-colors">
                            {item.icon}
                          </span>
                          <span className="text-slate-400">{item.label}</span>
                        </div>
                        <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-md bg-slate-800 text-slate-400 border border-slate-700/60 shrink-0">
                          Sắp ra mắt
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
      <div className="p-3 border-t border-slate-800 bg-slate-950/40">
        <div className="flex items-center gap-3 p-2 rounded-xl bg-slate-800/50 border border-slate-800">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-purple-600 to-indigo-500 text-white font-bold text-xs flex items-center justify-center shrink-0 shadow-sm">
            {getInitial(user?.fullName)}
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-xs font-bold text-white truncate">
              {user?.fullName || 'Quản trị viên'}
            </div>
            <div className="text-[10px] text-slate-400 truncate">
              {user?.email || 'admin@eshop.com'}
            </div>
          </div>
          <span className="px-1.5 py-0.5 text-[9px] font-bold rounded-md bg-purple-900/60 text-purple-300 border border-purple-700/50 uppercase tracking-wider shrink-0">
            ADMIN
          </span>
        </div>
      </div>
    </aside>
  )
}

export default AdminSidebar
