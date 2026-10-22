import React from 'react'
import { Link } from 'react-router-dom'
import {
  Users,
  Package,
  Tags,
  ClipboardList,
  Shield,
  ArrowRight,
  TrendingUp,
  LayoutDashboard,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

export const AdminDashboardPage = () => {
  const { user } = useAuthStore()

  const quickLinks = [
    {
      title: 'Quản lý người dùng',
      desc: 'Xem danh sách, kiểm tra quyền hạn, khóa và mở khóa tài khoản.',
      path: '/admin/users',
      icon: Users,
      color: 'text-blue-600 bg-blue-50 border-blue-100',
    },
    {
      title: 'Quản lý sản phẩm',
      desc: 'Quản lý kho hàng, tạo sản phẩm mới, cập nhật giá và số lượng.',
      path: '/admin/products',
      icon: Package,
      color: 'text-orange-600 bg-orange-50 border-orange-100',
    },
    {
      title: 'Quản lý danh mục',
      desc: 'Tổ chức phân loại hàng hóa, quản trị cây danh mục sản phẩm.',
      path: '/admin/categories',
      icon: Tags,
      color: 'text-emerald-600 bg-emerald-50 border-emerald-100',
    },
    {
      title: 'Quản lý đơn hàng',
      desc: 'Theo dõi đơn hàng, cập nhật trạng thái đóng gói và giao hàng.',
      path: '/admin/orders',
      icon: ClipboardList,
      color: 'text-purple-600 bg-purple-50 border-purple-100',
    },
  ]

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full space-y-8">
      {/* Page Header */}
      <div className="pb-6 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-orange-600 mb-1">
            <Shield className="w-4 h-4" />
            <span>Khu vực điều hành</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Bảng điều khiển quản trị
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Chào mừng trở lại, <span className="font-semibold text-slate-900">{user?.fullName || 'Quản trị viên'}</span>. Chọn phân hệ bên dưới để bắt đầu làm việc.
          </p>
        </div>
      </div>

      {/* Quick Action Navigation Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {quickLinks.map((item, idx) => {
          const Icon = item.icon
          return (
            <Link
              key={idx}
              to={item.path}
              className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-2xs hover:shadow-card hover:border-orange-300 transition-all flex flex-col justify-between group"
            >
              <div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4 border ${item.color}`}>
                  <Icon className="w-6 h-6 stroke-[1.75]" />
                </div>
                <h3 className="font-bold text-slate-900 text-sm group-hover:text-orange-600 transition-colors mb-1.5">
                  {item.title}
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  {item.desc}
                </p>
              </div>

              <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-orange-600">
                <span>Truy cập phân hệ</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </div>
            </Link>
          )
        })}
      </div>

      {/* System Note Box */}
      <div className="bg-slate-900 rounded-2xl text-white p-6 sm:p-8 shadow-card flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-1.5 max-w-2xl">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md bg-orange-600/20 text-orange-400 text-[11px] font-bold uppercase tracking-wider border border-orange-500/30">
            Hệ thống Rikkei E-commerce
          </div>
          <h2 className="text-base sm:text-lg font-bold">
            Kiểm soát giao dịch và vận hành kho hàng thời gian thực
          </h2>
          <p className="text-xs text-slate-400 leading-relaxed">
            Hệ thống liên kết đồng bộ giữa cơ chế giữ hàng (HOLD/RELEASE), trừ kho khi hoàn thành đơn (SALE_DEDUCT), và quản lý tài khoản người dùng theo kiến trúc chuẩn Enterprise.
          </p>
        </div>

        <Link
          to="/admin/orders"
          className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-colors shrink-0"
        >
          Xử lý đơn hàng ngay &rarr;
        </Link>
      </div>
    </div>
  )
}

export default AdminDashboardPage
