import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  Shield,
  Users,
  ShieldCheck,
  ShoppingBag,
  CheckCircle2,
  Lock,
  ArrowRight,
  Package,
  Boxes,
  ClipboardList,
  RefreshCw,
  User,
  AlertTriangle,
  Clock,
  DollarSign,
  TrendingUp,
  Warehouse,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import adminUserService from '../../services/adminUserService'
import dashboardService from '../../services/dashboardService'

/**
 * Trang Bảng điều khiển Quản trị viên (Admin Dashboard Page).
 * Route: /admin/dashboard
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 * Phong cách: shadcn/ui New York - Neutral/Zinc Monochrome & Monospace typography.
 *
 * Chỉ số thống kê:
 * - Tổng đơn hàng (totalOrders)
 * - Doanh thu thực tế (actualRevenue - đơn đã thanh toán PAID)
 * - Đơn chờ duyệt (pendingOrders - trạng thái PENDING)
 * - Danh sách sản phẩm có nguy cơ hết hàng (stock_quantity <= 5)
 */
export const AdminDashboardPage = () => {
  const { user: authUser } = useAuthStore()

  // Dữ liệu thống kê Dashboard từ API GET /admin/dashboard/stats
  const [dashboardStats, setDashboardStats] = useState({
    totalOrders: 0,
    actualRevenue: 0,
    totalRevenue: 0,
    pendingOrders: 0,
    confirmedOrders: 0,
    processingOrders: 0,
    shippingOrders: 0,
    completedOrders: 0,
    cancelledOrders: 0,
    totalProducts: 0,
    lowStockCount: 0,
    lowStockProducts: [],
    totalUsers: 0,
    totalCustomers: 0,
  })

  // Dữ liệu người dùng & thông tin admin
  const [stats, setStats] = useState({
    total: 0,
    admins: 0,
    customers: 0,
    active: 0,
  })
  const [currentAdminProfile, setCurrentAdminProfile] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [backendNotImplemented, setBackendNotImplemented] = useState(false)

  // Tải dữ liệu thống kê từ API thực tế
  const fetchDashboardData = async () => {
    setIsLoading(true)
    setBackendNotImplemented(false)

    try {
      // 1. Tải thông tin hồ sơ Admin hiện tại
      try {
        const adminRes = await adminUserService.getCurrentAdmin()
        if (adminRes?.data) {
          setCurrentAdminProfile(adminRes.data)
        }
      } catch (err) {
        // Bỏ qua nếu lỗi, authUser từ store vẫn có giá trị
      }

      // 2. Tải thống kê Bảng điều khiển nghiệp vụ (Đơn hàng, Doanh thu thực tế, Tồn kho thấp)
      try {
        const statsRes = await dashboardService.getStats(5)
        if (statsRes?.data) {
          setDashboardStats(statsRes.data)
        }
      } catch (err) {
        console.warn('Không thể tải dữ liệu GET /admin/dashboard/stats:', err)
      }

      // 3. Tải danh sách người dùng
      const usersRes = await adminUserService.getUsers({ page: 0, size: 100 })
      if (usersRes?.data) {
        let userList = []
        if (Array.isArray(usersRes.data)) {
          userList = usersRes.data
        } else if (usersRes.data.content && Array.isArray(usersRes.data.content)) {
          userList = usersRes.data.content
        }

        const total = userList.length
        const admins = userList.filter((u) => u.role === 'ADMIN').length
        const customers = userList.filter((u) => u.role === 'CUSTOMER').length
        const active = userList.filter((u) => u.active !== false).length

        setStats({ total, admins, customers, active })
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setBackendNotImplemented(true)
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const activeAdmin = currentAdminProfile || authUser

  const getAvatarLetter = (name, email) => {
    if (name && name.trim().length > 0) {
      const parts = name.trim().split(' ')
      return parts[parts.length - 1].charAt(0).toUpperCase()
    }
    if (email && email.length > 0) {
      return email.charAt(0).toUpperCase()
    }
    return 'A'
  }

  const formatCurrency = (val) => {
    const num = Number(val) || 0
    return `${num.toLocaleString('vi-VN')} ₫`
  }

  const securityFeatures = [
    {
      title: 'Bảo mật JWT & Token Rotation',
      desc: 'Access Token In-Memory, Refresh Token luân phiên nguyên tử qua Redis',
      status: 'Kích hoạt',
      badgeClass: 'bg-zinc-100 text-zinc-900 border-zinc-200',
    },
    {
      title: 'Kiểm soát truy cập phân tầng (RBAC)',
      desc: 'Bảo vệ endpoint bằng @PreAuthorize và Spring Security Context',
      status: 'Kích hoạt',
      badgeClass: 'bg-zinc-100 text-zinc-900 border-zinc-200',
    },
    {
      title: 'Khóa tài khoản tự động (Brute-Force)',
      desc: 'Tự động khóa tài khoản tạm thời khi đăng nhập thất bại liên tiếp',
      status: 'Kích hoạt',
      badgeClass: 'bg-zinc-100 text-zinc-900 border-zinc-200',
    },
    {
      title: 'Mã hóa mật khẩu chuẩn BCrypt',
      desc: 'Mật khẩu người dùng được băm an toàn với độ phức tạp cao',
      status: 'Kích hoạt',
      badgeClass: 'bg-zinc-100 text-zinc-900 border-zinc-200',
    },
  ]

  const moduleRoadmap = [
    {
      name: 'Quản lý Người dùng',
      desc: 'Xem danh sách, phân quyền, khóa và mở khóa tài khoản',
      sprint: 'Sprint 1',
      icon: Users,
      badgeText: 'Hoàn thành',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      link: '/admin/users',
      isReady: true,
    },
    {
      name: 'Sản phẩm & Danh mục',
      desc: 'Quản lý danh mục hàng hóa, thông tin sản phẩm và giá bán',
      sprint: 'Sprint 1',
      icon: Package,
      badgeText: 'Hoàn thành',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      link: '/admin/products',
      isReady: true,
    },
    {
      name: 'Đơn hàng & Tồn kho',
      desc: 'Xử lý trạng thái đơn hàng, kho bãi và giao hàng',
      sprint: 'Sprint 2',
      icon: ClipboardList,
      badgeText: 'Hoàn thành',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      link: '/admin/orders',
      isReady: true,
    },
    {
      name: 'Thanh toán & Điều chỉnh kho',
      desc: 'Mô phỏng thanh toán trực tuyến, nhật ký biến động kho',
      sprint: 'Sprint 2',
      icon: Warehouse,
      badgeText: 'Hoàn thành',
      badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      link: '/admin/inventory',
      isReady: true,
    },
  ]

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full animate-in fade-in duration-200">
      {/* 1. Tiêu đề trang & Nút thao tác */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-zinc-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-zinc-950 tracking-tight font-mono">
              Bảng điều khiển
            </h1>
            <span className="badge-neutral font-mono">
              <Shield className="w-3.5 h-3.5 text-zinc-700" /> Admin Portal
            </span>
          </div>
          <p className="text-sm text-zinc-500 mt-1">
            Tổng quan nghiệp vụ, doanh thu thực tế, trạng thái đơn hàng và cảnh báo tồn kho.
          </p>
        </div>

        {/* Nút thao tác đầu trang */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchDashboardData}
            disabled={isLoading}
            className="btn-outline font-mono"
            title="Tải lại dữ liệu hệ thống"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-zinc-900' : 'text-zinc-500'}`} />
            <span>{isLoading ? 'Đang tải...' : 'Làm mới'}</span>
          </button>

          <Link
            to="/admin/orders"
            className="btn-primary font-mono"
          >
            <ClipboardList className="w-4 h-4" />
            <span>Xem đơn hàng</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>

      {/* 2. Thẻ thống kê Nghiệp vụ Cốt lõi (Core Business KPI Cards - Monospace & shadcn/ui style) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 my-6">
        {/* Card 1: Doanh thu thực tế */}
        <div className="bg-white p-5 rounded-lg border border-zinc-200 shadow-sm flex flex-col justify-between hover:border-zinc-300 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
              Doanh thu thực tế
            </span>
            <span className="inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
              <TrendingUp className="w-3 h-3" /> PAID
            </span>
          </div>
          <div className="my-2">
            <div className="text-2xl font-bold text-zinc-950 font-mono tabular-nums">
              {isLoading ? '--' : formatCurrency(dashboardStats.actualRevenue)}
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              Thực thu từ các đơn đã thanh toán thành công
            </p>
          </div>
          <div className="pt-2 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
            <span>Đơn hoàn tất: {dashboardStats.completedOrders || 0}</span>
            <Link to="/admin/orders" className="text-zinc-900 hover:underline">Chi tiết &rarr;</Link>
          </div>
        </div>

        {/* Card 2: Tổng đơn hàng */}
        <div className="bg-white p-5 rounded-lg border border-zinc-200 shadow-sm flex flex-col justify-between hover:border-zinc-300 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
              Tổng đơn hàng
            </span>
            <span className="inline-flex items-center text-[11px] font-mono font-medium px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200">
              Đơn hàng
            </span>
          </div>
          <div className="my-2">
            <div className="text-2xl font-bold text-zinc-950 font-mono tabular-nums">
              {isLoading ? '--' : dashboardStats.totalOrders}
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              Tổng số đơn đặt trên toàn hệ thống
            </p>
          </div>
          <div className="pt-2 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
            <span>Đã xác nhận: {dashboardStats.confirmedOrders || 0}</span>
            <Link to="/admin/orders" className="text-zinc-900 hover:underline">Chi tiết &rarr;</Link>
          </div>
        </div>

        {/* Card 3: Đơn chờ duyệt */}
        <div className="bg-white p-5 rounded-lg border border-zinc-200 shadow-sm flex flex-col justify-between hover:border-zinc-300 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
              Đơn chờ duyệt
            </span>
            <span className="inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200">
              <Clock className="w-3 h-3" /> PENDING
            </span>
          </div>
          <div className="my-2">
            <div className="text-2xl font-bold text-zinc-950 font-mono tabular-nums">
              {isLoading ? '--' : dashboardStats.pendingOrders}
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              Đang chờ Admin duyệt và chuyển sang xử lý
            </p>
          </div>
          <div className="pt-2 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
            <span>Đang xử lý: {dashboardStats.processingOrders || 0}</span>
            <Link to="/admin/orders?status=PENDING" className="text-zinc-900 hover:underline">Duyệt ngay &rarr;</Link>
          </div>
        </div>

        {/* Card 4: Cảnh báo tồn kho nguy cơ hết hàng */}
        <div className="bg-white p-5 rounded-lg border border-zinc-200 shadow-sm flex flex-col justify-between hover:border-zinc-300 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
              Cảnh báo kho
            </span>
            <span className={`inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded border ${
              (dashboardStats.lowStockCount || dashboardStats.lowStockProducts?.length) > 0
                ? 'bg-red-50 text-red-700 border-red-200'
                : 'bg-emerald-50 text-emerald-700 border-emerald-200'
            }`}>
              <AlertTriangle className="w-3 h-3" /> Kho &le; 5
            </span>
          </div>
          <div className="my-2">
            <div className="text-2xl font-bold text-zinc-950 font-mono tabular-nums">
              {isLoading ? '--' : (dashboardStats.lowStockCount || dashboardStats.lowStockProducts?.length || 0)}
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              Sản phẩm có tồn kho thấp cần bổ sung
            </p>
          </div>
          <div className="pt-2 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
            <span>Tổng SP: {dashboardStats.totalProducts || 0}</span>
            <Link to="/admin/inventory" className="text-zinc-900 hover:underline">Nhập kho &rarr;</Link>
          </div>
        </div>
      </div>

      {/* 3. Bảng Danh sách Sản phẩm có nguy cơ hết hàng (stock_quantity <= 5) */}
      <div className="mb-6 bg-white rounded-lg border border-zinc-200 shadow-sm p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 mb-4 border-b border-zinc-200">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-md bg-amber-50 text-amber-700 border border-amber-200 flex items-center justify-center shrink-0">
              <AlertTriangle className="w-4 h-4 stroke-[2]" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                Sản phẩm có nguy cơ hết hàng (Tồn kho &le; 5)
              </h2>
              <p className="text-xs text-zinc-500">
                Danh sách các mặt hàng cần nhập bổ sung kho thủ công để tránh gián đoạn kinh doanh.
              </p>
            </div>
          </div>
          <Link
            to="/admin/inventory"
            className="btn-outline text-xs font-mono shrink-0"
          >
            <Warehouse className="w-3.5 h-3.5 text-zinc-600" />
            <span>Quản lý nhập/xuất kho</span>
          </Link>
        </div>

        {dashboardStats.lowStockProducts && dashboardStats.lowStockProducts.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs sm:text-sm">
              <thead className="bg-zinc-50 border-y border-zinc-200 font-mono text-zinc-500 uppercase text-[11px]">
                <tr>
                  <th className="py-2.5 px-3">Mã / Sản phẩm</th>
                  <th className="py-2.5 px-3">Danh mục</th>
                  <th className="py-2.5 px-3 text-right">Giá bán</th>
                  <th className="py-2.5 px-3 text-center">Tồn kho hiện tại</th>
                  <th className="py-2.5 px-3 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-200 font-mono">
                {dashboardStats.lowStockProducts.map((p) => (
                  <tr key={p.id} className="hover:bg-zinc-50/80 transition-colors">
                    <td className="py-3 px-3">
                      <div className="flex items-center gap-3">
                        {p.imageUrl ? (
                          <img
                            src={p.imageUrl}
                            alt={p.name}
                            className="w-9 h-9 rounded object-cover border border-zinc-200 shrink-0"
                          />
                        ) : (
                          <div className="w-9 h-9 rounded bg-zinc-100 text-zinc-400 flex items-center justify-center border border-zinc-200 shrink-0">
                            <Package className="w-4 h-4" />
                          </div>
                        )}
                        <div>
                          <div className="font-medium text-zinc-950">{p.name}</div>
                          <div className="text-[11px] text-zinc-400">ID: #{p.id}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-3 text-zinc-600">
                      {p.categoryName || 'Chung'}
                    </td>
                    <td className="py-3 px-3 text-right text-zinc-950 font-medium tabular-nums">
                      {formatCurrency(p.price)}
                    </td>
                    <td className="py-3 px-3 text-center">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${
                        p.stockQuantity === 0
                          ? 'bg-red-100 text-red-800 border border-red-200'
                          : 'bg-amber-100 text-amber-800 border border-amber-200'
                      }`}>
                        {p.stockQuantity === 0 ? 'Hết hàng (0)' : `Còn ${p.stockQuantity}`}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-right">
                      <Link
                        to="/admin/inventory"
                        className="inline-flex items-center gap-1 text-xs font-medium text-zinc-900 hover:underline"
                      >
                        <span>Nhập kho</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-8 text-center text-xs sm:text-sm text-zinc-500 font-mono">
            <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-600 mb-2" />
            <p>Tất cả sản phẩm hiện đều có số lượng tồn kho an toàn (&gt; 5).</p>
          </div>
        )}
      </div>

      {/* 4. Banner Chào mừng & Thông tin Phiên Quản trị */}
      <div className="mb-6 bg-zinc-900 border border-zinc-800 text-zinc-100 rounded-lg p-5 shadow-sm">
        <div className="flex items-start gap-3.5">
          <span className="text-xl shrink-0 mt-0.5">⚡</span>
          <div className="space-y-1.5 flex-1">
            <h3 className="font-semibold text-sm sm:text-base text-zinc-100 font-mono">
              Hệ thống Quản trị Trung tâm Rikkei E-commerce
            </h3>
            <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">
              Phiên làm việc quản trị đang được bảo vệ nghiêm ngặt bởi cơ chế xác thực JWT In-Memory và Redis Token Rotation.
            </p>
            <div className="pt-2 text-xs text-zinc-400 flex flex-wrap items-center gap-x-4 gap-y-1 font-mono">
              <span>
                • Quản trị viên: <strong className="text-zinc-200">{activeAdmin?.fullName || 'Quản trị viên'}</strong> ({activeAdmin?.email || 'admin@ecommerce.com'})
              </span>
              <span>
                • Quyền hệ thống: <strong className="text-zinc-200">{activeAdmin?.role || 'ADMIN'}</strong>
              </span>
              <span>
                • Trạng thái token:{' '}
                <span className="text-emerald-400 font-medium">Đã xác thực hợp lệ</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 5. Chi tiết Kiến trúc Bảo mật & Hồ sơ Admin (Layout 2 cột) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cột trái (2/3): Kiến trúc Bảo mật & Phân quyền */}
        <div className="lg:col-span-2 bg-white rounded-lg border border-zinc-200 shadow-sm p-6">
          <div className="flex items-center justify-between pb-4 mb-5 border-b border-zinc-200">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-4 h-4 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">Kiến trúc Bảo mật & Phân quyền</h2>
                <p className="text-xs text-zinc-500">Trạng thái bảo vệ phiên làm việc và kiểm soát truy cập phân tầng (RBAC)</p>
              </div>
            </div>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200 font-mono">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
              Toàn vẹn 100%
            </span>
          </div>

          <div className="space-y-3 font-mono">
            {securityFeatures.map((sec, idx) => (
              <div
                key={idx}
                className="flex items-start justify-between gap-4 p-3.5 rounded-md bg-zinc-50 border border-zinc-200/80 hover:bg-zinc-100/60 transition-colors"
              >
                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                    <CheckCircle2 className="w-3.5 h-3.5 stroke-[2]" />
                  </div>
                  <div>
                    <h4 className="text-xs sm:text-sm font-semibold text-zinc-900">{sec.title}</h4>
                    <p className="text-xs text-zinc-500 mt-0.5 leading-relaxed">{sec.desc}</p>
                  </div>
                </div>
                <span className={`shrink-0 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${sec.badgeClass}`}>
                  {sec.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Cột phải (1/3): Hồ sơ Admin đang hoạt động */}
        <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2.5 pb-4 mb-5 border-b border-zinc-200">
              <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                <Lock className="w-4 h-4 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">Thông tin Phiên Admin</h2>
                <p className="text-xs text-zinc-500">Tài khoản quản trị đang thao tác</p>
              </div>
            </div>

            {/* Avatar & Thông tin */}
            <div className="text-center py-2">
              <div className="w-14 h-14 rounded-full bg-zinc-900 text-zinc-50 font-bold text-xl flex items-center justify-center shadow-sm mx-auto mb-3 font-mono">
                {getAvatarLetter(activeAdmin?.fullName, activeAdmin?.email)}
              </div>
              <h3 className="font-semibold text-zinc-950 text-sm sm:text-base font-mono">
                {activeAdmin?.fullName || 'Quản trị viên'}
              </h3>
              <p className="text-xs text-zinc-500 font-mono mt-0.5 break-all">
                {activeAdmin?.email || 'admin@ecommerce.com'}
              </p>
            </div>

            <div className="mt-4 p-3.5 rounded-md bg-zinc-50 border border-zinc-200 space-y-2 text-xs text-zinc-700 font-mono">
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Vai trò:</span>
                <span className="badge-neutral">
                  <ShieldCheck className="w-3 h-3" /> {activeAdmin?.role || 'ADMIN'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Trạng thái:</span>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                  Hoạt động
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Phiên bảo mật:</span>
                <span className="font-mono text-zinc-900 font-medium text-xs">Đã xác thực</span>
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-zinc-200 flex flex-col gap-2">
            <Link
              to="/admin/users"
              className="btn-primary w-full font-mono"
            >
              <Users className="w-4 h-4" />
              <span>Chuyển tới Quản lý người dùng</span>
            </Link>

            <Link
              to="/profile"
              className="btn-outline w-full font-mono"
            >
              <User className="w-3.5 h-3.5 text-zinc-500" />
              <span>Xem hồ sơ cá nhân</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 6. Khối Lộ trình phân hệ quản trị (Module Roadmap Grid) */}
      <div className="mt-6 bg-white rounded-lg border border-zinc-200 shadow-sm p-6">
        <div className="flex items-center justify-between pb-4 mb-5 border-b border-zinc-200">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
              <Boxes className="w-4 h-4 stroke-[1.75]" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">Lộ trình Triển khai Phân hệ Quản trị</h2>
              <p className="text-xs text-zinc-500">Kế hoạch bàn giao các tính năng E-commerce theo từng Sprint</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 font-mono">
          {moduleRoadmap.map((mod, idx) => {
            const ModIcon = mod.icon
            return (
              <div
                key={idx}
                className="p-4 rounded-lg bg-zinc-50 border border-zinc-200 hover:bg-zinc-100/60 transition-colors flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="w-9 h-9 rounded-md bg-white border border-zinc-200 text-zinc-700 flex items-center justify-center shadow-xs">
                      <ModIcon className="w-4 h-4 text-zinc-700" />
                    </div>
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium border ${mod.badgeClass}`}>
                      {mod.badgeText}
                    </span>
                  </div>
                  <h4 className="text-xs sm:text-sm font-semibold text-zinc-950">{mod.name}</h4>
                  <p className="text-xs text-zinc-500 mt-1 leading-relaxed">{mod.desc}</p>
                </div>

                <div className="mt-4 pt-3 border-t border-zinc-200 flex items-center justify-between">
                  <span className="text-xs font-mono text-zinc-400">{mod.sprint}</span>
                  {mod.isReady ? (
                    <Link
                      to={mod.link}
                      className="inline-flex items-center gap-1 text-xs font-medium text-zinc-950 hover:underline"
                    >
                      <span>Truy cập</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  ) : (
                    <span className="text-xs text-zinc-400">Đang phát triển</span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

export default AdminDashboardPage
