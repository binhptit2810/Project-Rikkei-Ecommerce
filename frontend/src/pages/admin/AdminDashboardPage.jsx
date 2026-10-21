import React from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Trang Dashboard Admin (Placeholder tối giản).
 * 
 * Yêu cầu:
 * - Tiêu đề: Dashboard
 * - Mô tả ngắn: Tổng quan hệ thống quản trị
 * - Nội dung tối giản, không tạo số liệu, biểu đồ hay dữ liệu giả.
 * - Không gọi API không cần thiết.
 * - Tích hợp chuẩn trong AdminLayout và AdminSidebar.
 */
export const AdminDashboardPage = () => {
  const { user } = useAuthStore()

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
      {/* Tiêu đề trang */}
      <div className="pb-6 border-b border-slate-200">
        <div className="flex items-center gap-2.5">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 tracking-tight">
            Dashboard
          </h1>
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200 uppercase tracking-wider">
            <span>🛡️</span> Admin Panel
          </span>
        </div>
        <p className="text-sm text-slate-500 mt-1">
          Tổng quan hệ thống quản trị
        </p>
      </div>

      {/* Khu vực nội dung placeholder tối giản */}
      <div className="my-8">
        <div className="bg-white rounded-3xl border border-slate-200/80 p-8 sm:p-12 text-center shadow-xs">
          <div className="w-16 h-16 bg-slate-100 text-slate-400 rounded-2xl flex items-center justify-center text-3xl mx-auto mb-4">
            📊
          </div>
          <h2 className="text-lg font-bold text-slate-800 mb-2">
            Chào mừng trở lại, {user?.fullName || 'Quản trị viên'}!
          </h2>
          <p className="text-sm text-slate-500 max-w-md mx-auto mb-6 leading-relaxed">
            Khu vực tổng quan hệ thống quản trị đang được phát triển. Hiện tại, bạn có thể sử dụng chức năng <strong>Quản lý người dùng</strong> để theo dõi các tài khoản trong hệ thống.
          </p>
          <Link
            to="/admin/users"
            className="inline-flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow-sm hover:shadow transition-all text-sm cursor-pointer"
          >
            <span>👥</span>
            <span>Đến Quản lý người dùng</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

export default AdminDashboardPage
