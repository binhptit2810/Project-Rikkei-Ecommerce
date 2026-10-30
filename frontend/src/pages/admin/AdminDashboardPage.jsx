import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  Users,
  Package,
  Tags,
  ClipboardList,
  Boxes,
  Shield,
  ArrowRight,
  TrendingUp,
  Clock,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import apiClient from '../../services/apiClient'

export const AdminDashboardPage = () => {
  const { user } = useAuthStore()
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await apiClient.get('/admin/dashboard/stats')
        if (res.data?.success && res.data?.data) {
          setStats(res.data.data)
        }
      } catch (err) {
        console.error('Lỗi khi tải số liệu thống kê:', err)
      } finally {
        setLoading(false)
      }
    }
    fetchStats()
  }, [])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

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
      desc: 'Quản lý danh mục hàng hóa, tạo sản phẩm mới, cập nhật giá.',
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
    {
      title: 'Quản lý tồn kho',
      desc: 'Theo dõi xuất nhập kho, điều chỉnh số lượng tồn và xem nhật ký giao dịch.',
      path: '/admin/inventory',
      icon: Boxes,
      color: 'text-amber-600 bg-amber-50 border-amber-100',
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
            Chào mừng trở lại, <span className="font-semibold text-slate-900">{user?.fullName || 'Quản trị viên'}</span>.
          </p>
        </div>
      </div>

      {/* Real-time KPI Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Doanh thu thực tế</span>
              <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <TrendingUp className="w-4 h-4" />
              </div>
            </div>
            <div className="text-xl sm:text-2xl font-black text-emerald-600 mt-2 tabular-nums">
              {formatPrice(stats.totalRevenue)}
            </div>
            <div className="text-[11px] text-slate-400 mt-1">Tính từ {stats.completedOrders} đơn hoàn thành</div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Tổng số đơn hàng</span>
              <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                <ClipboardList className="w-4 h-4" />
              </div>
            </div>
            <div className="text-xl sm:text-2xl font-black text-slate-900 mt-2 tabular-nums">
              {stats.totalOrders}
            </div>
            <div className="text-[11px] text-slate-400 mt-1">
              {stats.pendingOrders} chờ xử lý | {stats.shippingOrders} đang giao
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Đơn chờ duyệt</span>
              <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
                <Clock className="w-4 h-4" />
              </div>
            </div>
            <div className="text-xl sm:text-2xl font-black text-amber-600 mt-2 tabular-nums">
              {stats.pendingOrders}
            </div>
            <div className="text-[11px] text-slate-400 mt-1">Cần xác nhận đơn sớm</div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Cảnh báo tồn kho</span>
              <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
                <AlertTriangle className="w-4 h-4" />
              </div>
            </div>
            <div className="text-xl sm:text-2xl font-black text-rose-600 mt-2 tabular-nums">
              {stats.outOfStockProducts + stats.lowStockProducts}
            </div>
            <div className="text-[11px] text-slate-400 mt-1">
              {stats.outOfStockProducts} hết hàng | {stats.lowStockProducts} sắp hết (&le; 5)
            </div>
          </div>
        </div>
      )}

      {/* Quick Action Navigation Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6">
        {quickLinks.map((item, idx) => {
          const Icon = item.icon
          return (
            <Link
              key={idx}
              to={item.path}
              className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-2xs hover:shadow-card hover:border-orange-300 transition-all flex flex-col justify-between group"
            >
              <div>
                <div className={`w-11 h-11 rounded-xl flex items-center justify-center mb-3.5 border ${item.color}`}>
                  <Icon className="w-5 h-5 stroke-[1.75]" />
                </div>
                <h3 className="font-bold text-slate-900 text-sm group-hover:text-orange-600 transition-colors mb-1">
                  {item.title}
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed line-clamp-2">
                  {item.desc}
                </p>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-orange-600">
                <span>Truy cập</span>
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
            Hệ thống liên kết đồng bộ giữa cơ chế trừ kho an toàn (PESSIMISTIC_WRITE), hoàn kho khi hủy đơn, xác thực State Machine và theo dõi biến động kho.
          </p>
        </div>

        <Link
          to="/admin/inventory"
          className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-colors shrink-0"
        >
          Quản lý tồn kho ngay &rarr;
        </Link>
      </div>
    </div>
  )
}

export default AdminDashboardPage
