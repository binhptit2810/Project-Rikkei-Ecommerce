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
  Sparkles,
  RefreshCw,
  User,
  KeyRound,
  ExternalLink,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import adminUserService from '../../services/adminUserService'

/**
 * Trang Bảng điều khiển Quản trị viên (Admin Dashboard Page).
 * Route: /admin/dashboard
 * 
 * Thiết kế đồng bộ 100% với Trang Quản lý người dùng (UserManagementPage - Design Reference):
 * - Bảng màu chủ đạo: Nền sáng slate-100/70, thẻ trắng bg-white, viền border-slate-200/80, đổ bóng shadow-sm.
 * - Điểm nhấn: Cam thương hiệu (orange-600, orange-100), tím quản trị (purple-700, purple-100), xanh lá hoạt động (emerald-700, emerald-100).
 * - Typography, border-radius (rounded-2xl, rounded-xl) và cấu trúc card đồng nhất tuyệt đối.
 */
export const AdminDashboardPage = () => {
  const { user: authUser } = useAuthStore()

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

  // Tải dữ liệu thống kê từ API thực tế giống UserManagementPage
  const fetchDashboardData = async () => {
    setIsLoading(true)
    setBackendNotImplemented(false)

    try {
      // 1. Tải hồ sơ admin hiện tại qua GET /api/v1/users/me
      try {
        const adminRes = await adminUserService.getCurrentAdmin()
        if (adminRes?.data) {
          setCurrentAdminProfile(adminRes.data)
        }
      } catch (err) {
        // Bỏ qua nếu lỗi, authUser từ store vẫn có giá trị
      }

      // 2. Tải danh sách người dùng để trích xuất số liệu thống kê
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

  // Danh sách hạng mục bảo mật hệ thống
  const securityFeatures = [
    {
      title: 'Spring Security Filter Chain & RBAC',
      desc: 'Bảo vệ các REST API endpoint /api/v1/admin/** nghiêm ngặt theo phân quyền hasRole(\'ADMIN\').',
      status: 'Hoạt động',
      badgeClass: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    },
    {
      title: 'In-Memory JWT Access Token',
      desc: 'Token truy cập được lưu trữ trong RAM trình duyệt, triệt tiêu nguy cơ rò rỉ qua tấn công XSS.',
      status: 'An toàn',
      badgeClass: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    },
    {
      title: 'HttpOnly Cookie Token Rotation',
      desc: 'Refresh Token xoay vòng nguyên tử (Atomic Token Rotation) qua Redis, bảo vệ tuyệt đối.',
      status: 'Bảo mật',
      badgeClass: 'bg-emerald-100 text-emerald-700 border-emerald-200',
    },
    {
      title: 'Cổng Đăng nhập Quản trị Độc lập',
      desc: 'Phân tách hoàn toàn route /admin/login và /login, tự động kiểm tra vai trò người dùng từ Backend.',
      status: 'Đã phân tách',
      badgeClass: 'bg-purple-100 text-purple-700 border-purple-200',
    },
  ]

  // Lộ trình các phân hệ quản trị theo Sprint
  const moduleRoadmap = [
    {
      name: 'Quản lý người dùng',
      desc: 'Danh sách, phân quyền, khóa và mở khóa tài khoản',
      sprint: 'Sprint 1 (Hoàn thành)',
      icon: Users,
      badgeText: 'Đã sẵn sàng',
      badgeClass: 'bg-emerald-100 text-emerald-700 border-emerald-200',
      link: '/admin/users',
      isReady: true,
    },
    {
      name: 'Sản phẩm & Danh mục',
      desc: 'Quản lý danh mục hàng hóa, thuộc tính và hình ảnh',
      sprint: 'Sprint 2',
      icon: Package,
      badgeText: 'Đang triển khai',
      badgeClass: 'bg-amber-100 text-amber-700 border-amber-200',
      isReady: false,
    },
    {
      name: 'Đơn hàng & Tồn kho',
      desc: 'Xử lý trạng thái đơn hàng, kho bãi và giao hàng',
      sprint: 'Sprint 2',
      icon: ClipboardList,
      badgeText: 'Theo kế hoạch',
      badgeClass: 'bg-blue-100 text-blue-700 border-blue-200',
      isReady: false,
    },
    {
      name: 'Mã giảm giá & Cài đặt',
      desc: 'Chương trình khuyến mãi và tham số hệ thống',
      sprint: 'Sprint 3',
      icon: Boxes,
      badgeText: 'Theo kế hoạch',
      badgeClass: 'bg-slate-100 text-slate-600 border-slate-200',
      isReady: false,
    },
  ]

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full animate-in fade-in duration-200">
      {/* 1. Tiêu đề trang & Huy hiệu Admin Portal (Đồng bộ với UserManagementPage) */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 tracking-tight">
              Bảng điều khiển
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200 uppercase tracking-wider">
              <Shield className="w-3.5 h-3.5" /> Admin Portal
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Tổng quan hệ thống, trạng thái dịch vụ bảo mật và các hoạt động quản trị trên Rikkei E-commerce.
          </p>
        </div>

        {/* Nút thao tác đầu trang */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchDashboardData}
            disabled={isLoading}
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-orange-600 shadow-sm transition-all cursor-pointer disabled:opacity-50"
            title="Tải lại dữ liệu hệ thống"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-orange-600' : 'text-slate-500'}`} />
            <span>{isLoading ? 'Đang tải...' : 'Làm mới'}</span>
          </button>

          <Link
            to="/admin/users"
            className="inline-flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-sm font-semibold shadow-sm transition-all cursor-pointer"
          >
            <Users className="w-4 h-4" />
            <span>Quản lý người dùng</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>

      {/* 2. Thẻ thống kê (Stats Cards - Cùng phong cách thiết kế với UserManagementPage) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 my-6">
        {/* Card 1: Tổng người dùng */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
            <Users className="w-6 h-6 stroke-[1.75]" />
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Tổng người dùng
            </div>
            <div className="text-2xl font-extrabold text-slate-800 mt-0.5 tabular-nums">
              {backendNotImplemented ? '--' : stats.total}
            </div>
          </div>
        </div>

        {/* Card 2: Quản trị viên */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-6 h-6 stroke-[1.75]" />
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Quản trị viên
            </div>
            <div className="text-2xl font-extrabold text-purple-700 mt-0.5 tabular-nums">
              {backendNotImplemented ? '--' : stats.admins}
            </div>
          </div>
        </div>

        {/* Card 3: Khách hàng */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center shrink-0">
            <ShoppingBag className="w-6 h-6 stroke-[1.75]" />
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Khách hàng
            </div>
            <div className="text-2xl font-extrabold text-blue-700 mt-0.5 tabular-nums">
              {backendNotImplemented ? '--' : stats.customers}
            </div>
          </div>
        </div>

        {/* Card 4: Đang hoạt động */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-6 h-6 stroke-[1.75]" />
          </div>
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Đang hoạt động
            </div>
            <div className="text-2xl font-extrabold text-emerald-700 mt-0.5 tabular-nums">
              {backendNotImplemented ? '--' : stats.active}
            </div>
          </div>
        </div>
      </div>

      {/* 3. Banner Chào mừng & Thông tin Kết nối Backend (Chuẩn style từ UserManagementPage) */}
      <div className="mb-6 bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200 rounded-2xl p-5 text-amber-900 shadow-sm">
        <div className="flex items-start gap-3.5">
          <span className="text-2xl shrink-0 mt-0.5">⚡</span>
          <div className="space-y-1.5 flex-1">
            <h3 className="font-bold text-base text-amber-950">
              Hệ thống Quản trị Trung tâm Rikkei E-commerce
            </h3>
            <p className="text-sm text-amber-800 leading-relaxed">
              Chào mừng bạn đến với khu vực quản trị cao cấp. Phiên làm việc của bạn đang được bảo vệ nghiêm ngặt 
              bởi cơ chế xác thực JWT In-Memory và Redis Token Rotation.
            </p>
            <div className="pt-2 text-xs text-amber-700 flex flex-wrap items-center gap-x-4 gap-y-1">
              <span>
                • Quản trị viên: <strong>{activeAdmin?.fullName || 'Quản trị viên'}</strong> ({activeAdmin?.email || 'admin@ecommerce.com'})
              </span>
              <span>
                • Quyền hệ thống: <strong className="text-purple-800">{activeAdmin?.role || 'ADMIN'}</strong>
              </span>
              <span>
                • Trạng thái token:{' '}
                <span className="text-emerald-700 font-semibold">Đã xác thực hợp lệ</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 4. Chi tiết Kiến trúc Bảo mật & Hồ sơ Admin (Layout 2 cột) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cột trái (2/3): Kiến trúc Bảo mật & Phân quyền */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6">
          <div className="flex items-center justify-between pb-4 mb-5 border-b border-slate-200">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-5 h-5 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-800">Kiến trúc Bảo mật & Phân quyền</h2>
                <p className="text-xs text-slate-500">Trạng thái bảo vệ phiên làm việc và kiểm soát truy cập phân tầng (RBAC)</p>
              </div>
            </div>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700 border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Toàn vẹn 100%
            </span>
          </div>

          <div className="space-y-3">
            {securityFeatures.map((sec, idx) => (
              <div
                key={idx}
                className="flex items-start justify-between gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80 hover:bg-slate-100/60 transition-colors"
              >
                <div className="flex items-start gap-3">
                  <div className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                    <CheckCircle2 className="w-4 h-4 stroke-[2]" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-800">{sec.title}</h4>
                    <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">{sec.desc}</p>
                  </div>
                </div>
                <span className={`shrink-0 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${sec.badgeClass}`}>
                  {sec.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Cột phải (1/3): Hồ sơ Admin đang hoạt động */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2.5 pb-4 mb-5 border-b border-slate-200">
              <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
                <Lock className="w-5 h-5 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-800">Thông tin Phiên Admin</h2>
                <p className="text-xs text-slate-500">Tài khoản quản trị đang thao tác</p>
              </div>
            </div>

            {/* Avatar & Thông tin */}
            <div className="text-center py-2">
              <div className="w-16 h-16 rounded-full bg-gradient-to-tr from-orange-500 to-amber-400 text-white font-extrabold text-2xl flex items-center justify-center shadow-xs mx-auto mb-3">
                {getAvatarLetter(activeAdmin?.fullName, activeAdmin?.email)}
              </div>
              <h3 className="font-bold text-slate-800 text-base">
                {activeAdmin?.fullName || 'Quản trị viên'}
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5 break-all">
                {activeAdmin?.email || 'admin@ecommerce.com'}
              </p>
            </div>

            <div className="mt-4 p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2.5 text-xs text-slate-600">
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-500">Vai trò hệ thống:</span>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-700 border border-purple-200 uppercase">
                  <ShieldCheck className="w-3 h-3" /> {activeAdmin?.role || 'ADMIN'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-500">Trạng thái tài khoản:</span>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700 border border-emerald-200">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  Hoạt động
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-500">Phiên bảo mật:</span>
                <span className="font-mono text-emerald-700 font-bold text-[11px]">Đã xác thực</span>
              </div>
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-slate-200 flex flex-col gap-2">
            <Link
              to="/admin/users"
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-sm font-semibold shadow-sm transition-all cursor-pointer"
            >
              <Users className="w-4 h-4" />
              <span>Chuyển tới Quản lý người dùng</span>
            </Link>

            <Link
              to="/profile"
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-2 bg-white border border-slate-300 text-slate-700 hover:bg-slate-50 hover:text-orange-600 rounded-xl text-xs font-semibold shadow-sm transition-all cursor-pointer"
            >
              <User className="w-3.5 h-3.5 text-slate-400" />
              <span>Xem hồ sơ cá nhân</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 5. Khối Lộ trình phân hệ quản trị (Module Roadmap Grid) */}
      <div className="mt-6 bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6">
        <div className="flex items-center justify-between pb-4 mb-5 border-b border-slate-200">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center shrink-0">
              <Boxes className="w-5 h-5 stroke-[1.75]" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-800">Lộ trình Triển khai Phân hệ Quản trị</h2>
              <p className="text-xs text-slate-500">Kế hoạch bàn giao các tính năng E-commerce theo từng Sprint</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {moduleRoadmap.map((mod, idx) => {
            const ModIcon = mod.icon
            return (
              <div
                key={idx}
                className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 hover:bg-slate-100/60 transition-colors flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="w-10 h-10 rounded-lg bg-white border border-slate-200 text-slate-700 flex items-center justify-center shadow-xs">
                      <ModIcon className="w-5 h-5 text-slate-700" />
                    </div>
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${mod.badgeClass}`}>
                      {mod.badgeText}
                    </span>
                  </div>
                  <h4 className="text-sm font-bold text-slate-800">{mod.name}</h4>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">{mod.desc}</p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-200/80 flex items-center justify-between">
                  <span className="text-[11px] font-semibold text-slate-400">{mod.sprint}</span>
                  {mod.isReady ? (
                    <Link
                      to={mod.link}
                      className="inline-flex items-center gap-1 text-xs font-bold text-orange-600 hover:text-orange-700 transition-colors"
                    >
                      <span>Truy cập</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  ) : (
                    <span className="text-[11px] text-slate-400">Đang phát triển</span>
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
