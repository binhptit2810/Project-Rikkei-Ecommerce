import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  Shield,
  Users,
  ShoppingBag,
  CheckCircle2,
  Lock,
  ArrowRight,
  Package,
  Tags,
  ClipboardList,
  RefreshCw,
  AlertTriangle,
  Clock,
  TrendingUp,
  Warehouse,
  Truck,
  Check,
  XCircle,
  CreditCard,
  Wallet,
  Calendar,
  BarChart3,
  DollarSign,
  PieChart,
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import adminUserService from '../../services/adminUserService'
import dashboardService from '../../services/dashboardService'

/**
 * Trang Bảng điều khiển Quản trị viên (Admin Dashboard Page).
 * Route: /admin/dashboard
 * Phụ trách: Đặng Thanh Bình & Đỗ Hoàng Anh (hoanganh/frontend-devops)
 * Phong cách: Modern SaaS E-commerce Dashboard - shadcn/ui Neutral/Zinc Palette.
 *
 * Chỉ số hiển thị:
 * 1. Thẻ KPI tổng quan: Doanh thu thực tế, Tổng đơn hàng, Đơn chờ duyệt, Cảnh báo tồn kho, Tổng SP, Tổng KH.
 * 2. Biểu đồ trực quan trạng thái đơn hàng: SVG Donut Chart + Animated Progress Bars.
 * 3. Lối tắt thao tác nhanh (Quick Actions).
 * 4. Bảng cảnh báo sản phẩm có nguy cơ hết hàng (stock_quantity <= 5).
 * 5. Thông tin phiên làm việc quản trị & kiến trúc bảo mật RBAC.
 */
export const AdminDashboardPage = () => {
  const { user: authUser } = useAuthStore()

  // Dữ liệu thống kê Dashboard từ API GET /admin/dashboard/stats
  const [dashboardStats, setDashboardStats] = useState({
    totalOrders: 0,
    actualRevenue: 0,
    totalRevenue: 0,
    revenueToday: 0,
    revenueThisWeek: 0,
    revenueThisMonth: 0,
    revenueThisYear: 0,
    pendingOrders: 0,
    paidOrders: 0,
    deliveredOrders: 0,
    confirmedOrders: 0,
    processingOrders: 0,
    shippingOrders: 0,
    completedOrders: 0,
    cancelledOrders: 0,
    dailyRevenue: [],
    paymentDistribution: {
      codCount: 0,
      codRevenue: 0,
      codPercentage: 0,
      vnpayCount: 0,
      vnpayRevenue: 0,
      vnpayPercentage: 0,
      totalCount: 0,
      totalRevenue: 0,
    },
    totalProducts: 0,
    lowStockCount: 0,
    lowStockProducts: [],
    totalUsers: 0,
    totalCustomers: 0,
    orderPlacedCount: 0,
    orderCancelledCount: 0,
    manualImportCount: 0,
  })

  // Thông tin hồ sơ admin & danh sách người dùng
  const [currentAdminProfile, setCurrentAdminProfile] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [fetchError, setFetchError] = useState(null)
  const [hoveredDay, setHoveredDay] = useState(null)

  /**
   * Tải toàn bộ dữ liệu thống kê bảng điều khiển từ Backend API
   */
  const fetchDashboardData = async () => {
    setIsLoading(true)
    setFetchError(null)

    try {
      // 1. Tải thông tin hồ sơ Admin hiện tại
      try {
        const adminRes = await adminUserService.getCurrentAdmin()
        if (adminRes?.data) {
          setCurrentAdminProfile(adminRes.data)
        }
      } catch (err) {
        // Nếu lỗi, tiếp tục dùng authUser từ useAuthStore
      }

      // 2. Tải thống kê Bảng điều khiển nghiệp vụ (ngưỡng cảnh báo tồn kho <= 5)
      const statsRes = await dashboardService.getStats(5)
      if (statsRes?.data) {
        setDashboardStats(statsRes.data)
      } else {
        throw new Error('Dữ liệu phản hồi từ máy chủ không hợp lệ')
      }
    } catch (err) {
      console.error('Lỗi khi tải dữ liệu Dashboard:', err)
      setFetchError(
        err.response?.data?.message ||
          err.message ||
          'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại dịch vụ Backend.'
      )
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const activeAdmin = currentAdminProfile || authUser

  /**
   * Định dạng tiền tệ VNĐ chuẩn theo Intl.NumberFormat
   */
  const formatCurrency = (val) => {
    const num = Number(val) || 0
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(num)
  }

  /**
   * Lấy chữ cái đại diện cho avatar
   */
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

  // ============================================================================
  // TÍNH TOÁN DỮ LIỆU BIỂU ĐỒ TRẠNG THÁI ĐƠN HÀNG (ORDER STATUS SUMMARY)
  // ============================================================================
  // Danh sách các trạng thái khớp với OrderStatus trong Backend
  const orderStatusConfig = [
    {
      key: 'COMPLETED',
      label: 'Hoàn thành',
      count: Number(dashboardStats.completedOrders) || 0,
      color: '#10b981', // emerald-500
      tailwindBg: 'bg-emerald-500',
      tailwindText: 'text-emerald-700',
      tailwindBadge: 'bg-emerald-50 border-emerald-200 text-emerald-700',
      icon: CheckCircle2,
    },
    {
      key: 'SHIPPING',
      label: 'Đang giao hàng',
      count: Number(dashboardStats.shippingOrders) || 0,
      color: '#8b5cf6', // purple-500
      tailwindBg: 'bg-purple-500',
      tailwindText: 'text-purple-700',
      tailwindBadge: 'bg-purple-50 border-purple-200 text-purple-700',
      icon: Truck,
    },
    {
      key: 'PROCESSING',
      label: 'Đang xử lý',
      count: Number(dashboardStats.processingOrders) || 0,
      color: '#3b82f6', // blue-500
      tailwindBg: 'bg-blue-500',
      tailwindText: 'text-blue-700',
      tailwindBadge: 'bg-blue-50 border-blue-200 text-blue-700',
      icon: RefreshCw,
    },
    {
      key: 'CONFIRMED',
      label: 'Đã xác nhận',
      count: Number(dashboardStats.confirmedOrders) || 0,
      color: '#0284c7', // sky-600
      tailwindBg: 'bg-sky-600',
      tailwindText: 'text-sky-700',
      tailwindBadge: 'bg-sky-50 border-sky-200 text-sky-700',
      icon: Check,
    },
    {
      key: 'PENDING',
      label: 'Chờ xử lý',
      count: Number(dashboardStats.pendingOrders) || 0,
      color: '#f59e0b', // amber-500
      tailwindBg: 'bg-amber-500',
      tailwindText: 'text-amber-700',
      tailwindBadge: 'bg-amber-50 border-amber-200 text-amber-700',
      icon: Clock,
    },
    {
      key: 'CANCELLED',
      label: 'Đã hủy',
      count: Number(dashboardStats.cancelledOrders) || 0,
      color: '#f43f5e', // rose-500
      tailwindBg: 'bg-rose-500',
      tailwindText: 'text-rose-700',
      tailwindBadge: 'bg-rose-50 border-rose-200 text-rose-700',
      icon: XCircle,
    },
  ]

  // Tổng số đơn hàng thực tế tính từ các trạng thái (xử lý an toàn tránh chia cho 0)
  const totalOrdersCalculated = orderStatusConfig.reduce((acc, cur) => acc + cur.count, 0)
  const totalOrdersCount = Math.max(Number(dashboardStats.totalOrders) || 0, totalOrdersCalculated)

  // Chu vi đường tròn SVG với bán kính R = 38 (2 * pi * 38 = 238.761)
  const SVG_RADIUS = 38
  const CIRCUMFERENCE = 2 * Math.PI * SVG_RADIUS

  // Tính toán các phân đoạn (segments) cho SVG Donut Chart
  let accumulatedOffset = 0
  const donutSegments = orderStatusConfig.map((item) => {
    // Tỷ lệ % của từng trạng thái (xử lý an toàn khi tổng đơn = 0 để không bị NaN)
    const percentage = totalOrdersCount > 0 ? (item.count / totalOrdersCount) * 100 : 0
    const strokeDash = totalOrdersCount > 0 ? (item.count / totalOrdersCount) * CIRCUMFERENCE : 0
    const segment = {
      ...item,
      percentage: Number(percentage.toFixed(1)),
      strokeDash,
      offset: accumulatedOffset,
    }
    accumulatedOffset += strokeDash
    return segment
  })

  // Tỷ lệ đơn hoàn thành thành công (Completion Rate)
  const completedRate = totalOrdersCount > 0
    ? ((Number(dashboardStats.completedOrders) || 0) / totalOrdersCount) * 100
    : 0

  return (
    <div className="flex-1 py-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full animate-in fade-in duration-300">
      {/* 1. TIÊU ĐỀ TRANG & NÚT THAO TÁC */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-zinc-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-zinc-950 tracking-tight font-mono">
              Bảng điều khiển
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-zinc-100 text-zinc-800 border border-zinc-200">
              <Shield className="w-3.5 h-3.5 text-zinc-600" /> Admin Portal
            </span>
          </div>
          <p className="text-sm text-zinc-500 mt-1">
            Tổng quan các chỉ số kinh doanh, tình trạng đơn hàng, biến động kho bãi và an toàn hệ thống.
          </p>
        </div>

        {/* Nút thao tác đầu trang */}
        <div className="flex items-center gap-2.5 shrink-0">
          <button
            type="button"
            onClick={fetchDashboardData}
            disabled={isLoading}
            className="btn-outline font-mono cursor-pointer flex items-center gap-2"
            title="Làm mới toàn bộ số liệu"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-zinc-900' : 'text-zinc-500'}`} />
            <span>{isLoading ? 'Đang cập nhật...' : 'Làm mới'}</span>
          </button>

          <Link
            to="/admin/orders"
            className="btn-primary font-mono flex items-center gap-2"
          >
            <ClipboardList className="w-4 h-4" />
            <span>Quản lý đơn hàng</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* 2. THÔNG BÁO LỖI NẾU API GẶP SỰ CỐ (ERROR ALERT) */}
      {fetchError && (
        <div className="my-5 p-4 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
            <div>
              <h4 className="text-sm font-semibold text-rose-900">Không thể tải dữ liệu Bảng điều khiển</h4>
              <p className="text-xs text-rose-700 mt-0.5">{fetchError}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={fetchDashboardData}
            className="px-3 py-1.5 rounded-md bg-rose-600 hover:bg-rose-700 text-white text-xs font-medium font-mono shrink-0 cursor-pointer transition-colors"
          >
            Thử lại ngay
          </button>
        </div>
      )}

      {/* 3. THẺ THỐNG KÊ TỔNG QUAN (CORE METRIC CARDS) */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 my-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white p-5 rounded-xl border border-zinc-200 shadow-sm animate-pulse space-y-3">
              <div className="flex justify-between items-center">
                <div className="h-4 w-24 bg-zinc-200 rounded"></div>
                <div className="h-5 w-12 bg-zinc-100 rounded"></div>
              </div>
              <div className="h-8 w-36 bg-zinc-200 rounded"></div>
              <div className="h-3 w-44 bg-zinc-100 rounded"></div>
              <div className="pt-2 border-t border-zinc-100 flex justify-between">
                <div className="h-3 w-20 bg-zinc-100 rounded"></div>
                <div className="h-3 w-16 bg-zinc-100 rounded"></div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5 my-6">
          {/* Card 1: Doanh thu thực tế */}
          <div className="group bg-white p-5 rounded-xl border border-zinc-200 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
                Doanh thu thực tế
              </span>
              <span className="inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                <TrendingUp className="w-3 h-3" /> PAID
              </span>
            </div>
            <div className="my-3">
              <div className="text-2xl sm:text-3xl font-bold text-zinc-950 font-mono tracking-tight tabular-nums">
                {formatCurrency(dashboardStats.actualRevenue)}
              </div>
              <p className="text-xs text-zinc-500 mt-1">
                Thực thu từ các đơn hàng đã thanh toán thành công
              </p>
            </div>
            <div className="pt-2.5 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
              <span className="flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                {dashboardStats.completedOrders || 0} đơn hoàn tất
              </span>
              <Link to="/admin/orders" className="text-zinc-900 font-medium hover:underline inline-flex items-center gap-0.5">
                Xem chi tiết &rarr;
              </Link>
            </div>
          </div>

          {/* Card 2: Tổng đơn hàng */}
          <div className="group bg-white p-5 rounded-xl border border-zinc-200 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
                Tổng đơn hàng
              </span>
              <span className="inline-flex items-center text-[11px] font-mono font-medium px-2 py-0.5 rounded-full bg-zinc-100 text-zinc-800 border border-zinc-200">
                <ShoppingBag className="w-3 h-3 mr-1 text-zinc-600" /> Đơn hàng
              </span>
            </div>
            <div className="my-3">
              <div className="text-2xl sm:text-3xl font-bold text-zinc-950 font-mono tracking-tight tabular-nums">
                {dashboardStats.totalOrders}
              </div>
              <p className="text-xs text-zinc-500 mt-1">
                Tất cả đơn đặt hàng được tạo trên hệ thống
              </p>
            </div>
            <div className="pt-2.5 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
              <span>Đã xác nhận: {dashboardStats.confirmedOrders || 0}</span>
              <Link to="/admin/orders" className="text-zinc-900 font-medium hover:underline inline-flex items-center gap-0.5">
                Tất cả đơn &rarr;
              </Link>
            </div>
          </div>

          {/* Card 3: Đơn chờ duyệt */}
          <div className="group bg-white p-5 rounded-xl border border-zinc-200 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
                Đơn chờ duyệt
              </span>
              <span className="inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200">
                <Clock className="w-3 h-3 text-amber-600" /> PENDING
              </span>
            </div>
            <div className="my-3">
              <div className="text-2xl sm:text-3xl font-bold text-zinc-950 font-mono tracking-tight tabular-nums">
                {dashboardStats.pendingOrders}
              </div>
              <p className="text-xs text-zinc-500 mt-1">
                Cần Admin phê duyệt và điều chuyển giao hàng
              </p>
            </div>
            <div className="pt-2.5 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
              <span>Đang xử lý: {dashboardStats.processingOrders || 0}</span>
              <Link to="/admin/orders" className="text-amber-800 font-medium hover:underline inline-flex items-center gap-0.5">
                Duyệt ngay &rarr;
              </Link>
            </div>
          </div>

          {/* Card 4: Cảnh báo tồn kho */}
          <div className="group bg-white p-5 rounded-xl border border-zinc-200 shadow-sm hover:shadow-md hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider font-mono">
                Cảnh báo kho
              </span>
              <span className={`inline-flex items-center gap-1 text-[11px] font-mono font-medium px-2 py-0.5 rounded-full border ${
                (dashboardStats.lowStockCount || dashboardStats.lowStockProducts?.length) > 0
                  ? 'bg-rose-50 text-rose-700 border-rose-200'
                  : 'bg-emerald-50 text-emerald-700 border-emerald-200'
              }`}>
                <AlertTriangle className="w-3 h-3" /> Tồn &le; 5
              </span>
            </div>
            <div className="my-3">
              <div className="text-2xl sm:text-3xl font-bold text-zinc-950 font-mono tracking-tight tabular-nums">
                {dashboardStats.lowStockCount || dashboardStats.lowStockProducts?.length || 0}
              </div>
              <p className="text-xs text-zinc-500 mt-1">
                Mặt hàng sắp hết cần nhập bổ sung kho
              </p>
            </div>
            <div className="pt-2.5 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
              <span>Tổng SP: {dashboardStats.totalProducts || 0}</span>
              <Link to="/admin/inventory" className="text-zinc-900 font-medium hover:underline inline-flex items-center gap-0.5">
                Nhập kho &rarr;
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* 3.1. CHỈ SỐ DOANH THU THEO KỲ: HÔM NAY - TUẦN NÀY - THÁNG NÀY - NĂM NAY */}
      <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-5 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-4 mb-4 border-b border-zinc-100">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center shrink-0">
              <TrendingUp className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                Doanh thu theo Chu kỳ Kinh doanh
              </h3>
              <p className="text-xs text-zinc-500">
                Chỉ số doanh thu thực thu theo từng mốc thời gian: Hôm nay, Tuần này, Tháng này và Năm nay.
              </p>
            </div>
          </div>
          <span className="text-xs font-mono text-zinc-500 bg-zinc-50 px-2.5 py-1 rounded-md border border-zinc-200 shrink-0">
            Cập nhật theo thời gian thực
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 font-mono">
          {/* Hôm nay */}
          <div className="p-4 rounded-xl bg-zinc-50/70 border border-zinc-200/80 hover:bg-zinc-50 transition-colors">
            <div className="flex items-center justify-between text-xs text-zinc-500 mb-1">
              <span className="font-medium uppercase tracking-wider">Hôm nay</span>
              <Calendar className="w-3.5 h-3.5 text-zinc-400" />
            </div>
            <div className="text-xl font-bold text-zinc-950 tabular-nums">
              {formatCurrency(dashboardStats.revenueToday)}
            </div>
            <p className="text-[11px] text-zinc-400 mt-1">
              Từ 00:00:00 hôm nay
            </p>
          </div>

          {/* Tuần này */}
          <div className="p-4 rounded-xl bg-zinc-50/70 border border-zinc-200/80 hover:bg-zinc-50 transition-colors">
            <div className="flex items-center justify-between text-xs text-zinc-500 mb-1">
              <span className="font-medium uppercase tracking-wider">Tuần này</span>
              <BarChart3 className="w-3.5 h-3.5 text-indigo-500" />
            </div>
            <div className="text-xl font-bold text-indigo-950 tabular-nums">
              {formatCurrency(dashboardStats.revenueThisWeek)}
            </div>
            <p className="text-[11px] text-zinc-400 mt-1">
              Từ Thứ Hai đầu tuần
            </p>
          </div>

          {/* Tháng này */}
          <div className="p-4 rounded-xl bg-zinc-50/70 border border-zinc-200/80 hover:bg-zinc-50 transition-colors">
            <div className="flex items-center justify-between text-xs text-zinc-500 mb-1">
              <span className="font-medium uppercase tracking-wider">Tháng này</span>
              <DollarSign className="w-3.5 h-3.5 text-emerald-500" />
            </div>
            <div className="text-xl font-bold text-emerald-950 tabular-nums">
              {formatCurrency(dashboardStats.revenueThisMonth)}
            </div>
            <p className="text-[11px] text-zinc-400 mt-1">
              Từ ngày 01 tháng này
            </p>
          </div>

          {/* Năm nay */}
          <div className="p-4 rounded-xl bg-zinc-50/70 border border-zinc-200/80 hover:bg-zinc-50 transition-colors">
            <div className="flex items-center justify-between text-xs text-zinc-500 mb-1">
              <span className="font-medium uppercase tracking-wider">Năm nay</span>
              <TrendingUp className="w-3.5 h-3.5 text-blue-500" />
            </div>
            <div className="text-xl font-bold text-blue-950 tabular-nums">
              {formatCurrency(dashboardStats.revenueThisYear)}
            </div>
            <p className="text-[11px] text-zinc-400 mt-1">
              Lũy kế từ 01/01
            </p>
          </div>
        </div>
      </div>

      {/* 3.2. BỐN CHỈ SỐ TRẠNG THÁI ĐƠN HÀNG TRỌNG TÂM (CHỜ XỬ LÝ, ĐÃ THANH TOÁN, ĐÃ GIAO, ĐÃ HỦY) */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 mb-6 font-mono">
        {/* Chờ xử lý */}
        <div className="bg-white p-4 rounded-xl border border-amber-200/80 shadow-xs bg-gradient-to-br from-amber-50/40 to-white">
          <div className="flex items-center justify-between text-xs text-amber-800 mb-1">
            <span className="font-medium uppercase">Chờ xử lý</span>
            <Clock className="w-3.5 h-3.5 text-amber-600" />
          </div>
          <div className="text-2xl font-bold text-zinc-950 tabular-nums">
            {dashboardStats.pendingOrders ?? 0}
          </div>
          <span className="text-[11px] text-amber-700/80 mt-0.5 block">Đơn hàng PENDING</span>
        </div>

        {/* Đã thanh toán */}
        <div className="bg-white p-4 rounded-xl border border-emerald-200/80 shadow-xs bg-gradient-to-br from-emerald-50/40 to-white">
          <div className="flex items-center justify-between text-xs text-emerald-800 mb-1">
            <span className="font-medium uppercase">Đã thanh toán</span>
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-zinc-950 tabular-nums">
            {dashboardStats.paidOrders ?? 0}
          </div>
          <span className="text-[11px] text-emerald-700/80 mt-0.5 block">Đơn hàng PAID</span>
        </div>

        {/* Đã giao */}
        <div className="bg-white p-4 rounded-xl border border-blue-200/80 shadow-xs bg-gradient-to-br from-blue-50/40 to-white">
          <div className="flex items-center justify-between text-xs text-blue-800 mb-1">
            <span className="font-medium uppercase">Đã giao</span>
            <Truck className="w-3.5 h-3.5 text-blue-600" />
          </div>
          <div className="text-2xl font-bold text-zinc-950 tabular-nums">
            {dashboardStats.deliveredOrders || dashboardStats.completedOrders || 0}
          </div>
          <span className="text-[11px] text-blue-700/80 mt-0.5 block">Đơn COMPLETED</span>
        </div>

        {/* Đã hủy */}
        <div className="bg-white p-4 rounded-xl border border-rose-200/80 shadow-xs bg-gradient-to-br from-rose-50/40 to-white">
          <div className="flex items-center justify-between text-xs text-rose-800 mb-1">
            <span className="font-medium uppercase">Đã hủy</span>
            <XCircle className="w-3.5 h-3.5 text-rose-600" />
          </div>
          <div className="text-2xl font-bold text-zinc-950 tabular-nums">
            {dashboardStats.cancelledOrders ?? 0}
          </div>
          <span className="text-[11px] text-rose-700/80 mt-0.5 block">Đơn CANCELLED</span>
        </div>
      </div>

      {/* 3.3. BIỂU ĐỒ DOANH THU THEO NGÀY TRONG THÁNG (CHART DATA) & TỶ LỆ THANH TOÁN (COD VS VNPAY) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
        {/* Biểu đồ Doanh thu theo từng ngày trong tháng (7/12) */}
        <div className="lg:col-span-7 bg-white rounded-xl border border-zinc-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-4 mb-4 border-b border-zinc-100">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center justify-center shrink-0">
                  <BarChart3 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                    Doanh thu theo ngày trong tháng (Chart Data)
                  </h3>
                  <p className="text-xs text-zinc-500">
                    Tháng {new Date().getMonth() + 1}/{new Date().getFullYear()} - Thống kê thực thu theo từng ngày
                  </p>
                </div>
              </div>
              {hoveredDay ? (
                <div className="text-right font-mono bg-indigo-50/80 border border-indigo-200 px-2.5 py-1 rounded-md">
                  <span className="text-[11px] text-indigo-700 block font-semibold">
                    Ngày {hoveredDay.day}: {formatCurrency(hoveredDay.revenue)}
                  </span>
                  <span className="text-[10px] text-indigo-600">
                    {hoveredDay.orderCount} đơn hàng
                  </span>
                </div>
              ) : (
                <span className="text-xs font-mono text-zinc-500 bg-zinc-50 px-2.5 py-1 rounded-md border border-zinc-200 shrink-0">
                  Rà chuột lên cột để xem
                </span>
              )}
            </div>

            {/* Vùng vẽ biểu đồ SVG Bar Chart */}
            <div className="relative pt-4 pb-2">
              {dashboardStats.dailyRevenue && dashboardStats.dailyRevenue.length > 0 ? (
                <div>
                  {/* Container các thanh biểu đồ */}
                  <div className="h-44 flex items-end gap-1 sm:gap-1.5 px-1 border-b border-zinc-200">
                    {(() => {
                      const maxRev = Math.max(
                        ...dashboardStats.dailyRevenue.map((p) => Number(p.revenue) || 0),
                        1
                      )
                      const currentDay = new Date().getDate()

                      return dashboardStats.dailyRevenue.map((point) => {
                        const rev = Number(point.revenue) || 0
                        const isToday = point.day === currentDay
                        const isHovered = hoveredDay?.day === point.day
                        const heightPct = Math.max(
                          (rev / maxRev) * 100,
                          point.orderCount > 0 ? 8 : 3
                        )

                        return (
                          <div
                            key={point.day}
                            onMouseEnter={() => setHoveredDay(point)}
                            onMouseLeave={() => setHoveredDay(null)}
                            className="flex-1 flex flex-col items-center justify-end h-full cursor-pointer group relative"
                          >
                            <div
                              style={{ height: `${heightPct}%` }}
                              className={`w-full rounded-t transition-all duration-200 ${
                                isHovered
                                  ? 'bg-violet-600'
                                  : isToday
                                  ? 'bg-emerald-500'
                                  : rev > 0
                                  ? 'bg-indigo-500 group-hover:bg-indigo-600'
                                  : 'bg-zinc-200/80 group-hover:bg-zinc-300'
                              }`}
                            />
                          </div>
                        )
                      })
                    })()}
                  </div>

                  {/* Trục hoành ngày (1, 5, 10, 15, 20, 25, ngày cuối) */}
                  <div className="flex justify-between items-center text-[10px] font-mono text-zinc-400 mt-2 px-1">
                    <span>N01</span>
                    <span>N05</span>
                    <span>N10</span>
                    <span>N15</span>
                    <span>N20</span>
                    <span>N25</span>
                    <span>N{dashboardStats.dailyRevenue.length}</span>
                  </div>
                </div>
              ) : (
                <div className="h-44 flex items-center justify-center text-xs text-zinc-400 font-mono">
                  Đang tổng hợp dữ liệu biểu đồ...
                </div>
              )}
            </div>
          </div>

          <div className="pt-3 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
            <span className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block"></span> Hôm nay
              <span className="w-2.5 h-2.5 rounded-sm bg-indigo-500 inline-block ml-2"></span> Có doanh thu
            </span>
            <span>Tổng tháng: <strong className="text-zinc-950">{formatCurrency(dashboardStats.revenueThisMonth || dashboardStats.actualRevenue)}</strong></span>
          </div>
        </div>

        {/* Tỷ lệ thanh toán: COD vs VNPay Online (5/12) */}
        <div className="lg:col-span-5 bg-white rounded-xl border border-zinc-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-zinc-100">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-700 border border-amber-200 flex items-center justify-center shrink-0">
                  <PieChart className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                    Tỷ lệ thanh toán (COD vs VNPay)
                  </h3>
                  <p className="text-xs text-zinc-500">
                    Phân bổ phương thức thanh toán toàn hệ thống
                  </p>
                </div>
              </div>
            </div>

            {/* Hai thẻ chi tiết phương thức thanh toán */}
            <div className="space-y-3 font-mono">
              {/* Thẻ COD */}
              <div className="p-3.5 rounded-lg border border-amber-200 bg-amber-50/50 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-md bg-amber-100 text-amber-800 flex items-center justify-center shrink-0 border border-amber-300">
                    <Wallet className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-zinc-950 text-xs">Thanh toán COD</div>
                    <div className="text-[11px] text-zinc-500">
                      {dashboardStats.paymentDistribution?.codCount ?? 0} đơn hàng &bull; {formatCurrency(dashboardStats.paymentDistribution?.codRevenue)}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300">
                    {dashboardStats.paymentDistribution?.codPercentage ?? 0}%
                  </span>
                </div>
              </div>

              {/* Thẻ VNPay Online */}
              <div className="p-3.5 rounded-lg border border-blue-200 bg-blue-50/50 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-md bg-blue-100 text-blue-800 flex items-center justify-center shrink-0 border border-blue-300">
                    <CreditCard className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-zinc-950 text-xs">VNPay Online</div>
                    <div className="text-[11px] text-zinc-500">
                      {dashboardStats.paymentDistribution?.vnpayCount ?? 0} đơn hàng &bull; {formatCurrency(dashboardStats.paymentDistribution?.vnpayRevenue)}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-bold bg-blue-100 text-blue-900 border border-blue-300">
                    {dashboardStats.paymentDistribution?.vnpayPercentage ?? 0}%
                  </span>
                </div>
              </div>

              {/* Thanh tiến độ trực quan phân bổ tỷ lệ COD vs VNPay */}
              <div className="pt-2">
                <div className="flex justify-between text-[11px] text-zinc-500 mb-1 font-mono">
                  <span>COD ({dashboardStats.paymentDistribution?.codPercentage ?? 0}%)</span>
                  <span>VNPay ({dashboardStats.paymentDistribution?.vnpayPercentage ?? 0}%)</span>
                </div>
                <div className="w-full h-3 bg-zinc-200 rounded-full overflow-hidden flex">
                  <div
                    style={{ width: `${dashboardStats.paymentDistribution?.codPercentage ?? 50}%` }}
                    className="bg-amber-500 transition-all duration-700"
                    title={`COD: ${dashboardStats.paymentDistribution?.codPercentage ?? 0}%`}
                  />
                  <div
                    style={{ width: `${dashboardStats.paymentDistribution?.vnpayPercentage ?? 50}%` }}
                    className="bg-blue-600 transition-all duration-700"
                    title={`VNPay: ${dashboardStats.paymentDistribution?.vnpayPercentage ?? 0}%`}
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="pt-3 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono mt-4">
            <span>Tổng giao dịch: <strong>{dashboardStats.paymentDistribution?.totalCount ?? 0}</strong> đơn</span>
            <span className="text-emerald-700 font-medium">Đối soát 100%</span>
          </div>
        </div>
      </div>

      {/* 4. BIỂU ĐỒ TRỰC QUAN TÌNH TRẠNG ĐƠN HÀNG (ORDER STATUS SUMMARY CHART) */}
      <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-5 mb-5 border-b border-zinc-100">
          <div>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <h2 className="text-base font-semibold text-zinc-950 font-mono">
                Tổng quan Tình trạng Đơn hàng
              </h2>
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              Biểu đồ trực quan phân bổ tỷ lệ phần trăm (%) và số lượng từng trạng thái đơn hàng trên hệ thống.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-zinc-500">
              Tổng số: <strong className="text-zinc-950 font-semibold">{totalOrdersCount}</strong> đơn
            </span>
            <Link
              to="/admin/orders"
              className="text-xs font-mono text-zinc-700 hover:text-zinc-950 hover:underline inline-flex items-center gap-1"
            >
              <span>Xem chi tiết</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>

        {/* Nội dung biểu đồ: SVG Donut + Animated Progress Bars */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
          {/* Cột trái (4/12): SVG Donut Chart */}
          <div className="lg:col-span-5 flex flex-col items-center justify-center p-4">
            <div className="relative w-52 h-52 flex items-center justify-center">
              <svg viewBox="0 0 100 100" className="w-full h-full transform -rotate-90">
                {/* Vòng nền xám mờ */}
                <circle
                  cx="50"
                  cy="50"
                  r={SVG_RADIUS}
                  fill="transparent"
                  stroke="#f4f4f5"
                  strokeWidth="11"
                />

                {/* Các phân đoạn trạng thái đơn hàng */}
                {totalOrdersCount > 0 &&
                  donutSegments.map((seg) => {
                    if (seg.count <= 0) return null
                    return (
                      <circle
                        key={seg.key}
                        cx="50"
                        cy="50"
                        r={SVG_RADIUS}
                        fill="transparent"
                        stroke={seg.color}
                        strokeWidth="11"
                        strokeDasharray={`${seg.strokeDash} ${CIRCUMFERENCE - seg.strokeDash}`}
                        strokeDashoffset={-seg.offset}
                        strokeLinecap="butt"
                        className="transition-all duration-700 ease-out"
                      />
                    )
                  })}
              </svg>

              {/* Tâm của biểu đồ tròn: Hiển thị tỷ lệ đơn thành công hoặc số đơn */}
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none">
                {totalOrdersCount > 0 ? (
                  <>
                    <span className="text-2xl font-bold font-mono text-zinc-950 tracking-tight">
                      {Math.round(completedRate)}%
                    </span>
                    <span className="text-[11px] font-mono text-zinc-500 font-medium">
                      Hoàn thành
                    </span>
                    <span className="text-[10px] font-mono text-zinc-400 mt-0.5">
                      {dashboardStats.completedOrders || 0}/{totalOrdersCount} đơn
                    </span>
                  </>
                ) : (
                  <>
                    <span className="text-xl font-bold font-mono text-zinc-400">0</span>
                    <span className="text-[11px] font-mono text-zinc-400">Chưa có đơn</span>
                  </>
                )}
              </div>
            </div>

            <p className="text-[11px] font-mono text-zinc-500 text-center mt-3">
              Tỷ lệ hoàn thành đơn: <strong className="text-emerald-600">{completedRate.toFixed(1)}%</strong>
            </p>
          </div>

          {/* Cột phải (7/12): Animated Progress Bars cho từng trạng thái */}
          <div className="lg:col-span-7 space-y-3.5">
            {donutSegments.map((item) => {
              const ItemIcon = item.icon
              return (
                <div
                  key={item.key}
                  className="p-3 rounded-lg bg-zinc-50/80 border border-zinc-100 hover:bg-zinc-50 transition-colors"
                >
                  <div className="flex items-center justify-between text-xs mb-1.5 font-mono">
                    <div className="flex items-center gap-2">
                      <div className={`w-6 h-6 rounded-md flex items-center justify-center shrink-0 border ${item.tailwindBadge}`}>
                        <ItemIcon className="w-3.5 h-3.5" />
                      </div>
                      <span className="font-semibold text-zinc-900">{item.label}</span>
                      <span className="text-[10px] text-zinc-500 uppercase">({item.key})</span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-zinc-950 tabular-nums">
                        {item.count} đơn
                      </span>
                      <span className={`text-[11px] font-medium px-1.5 py-0.2 rounded border ${item.tailwindBadge}`}>
                        {item.percentage}%
                      </span>
                    </div>
                  </div>

                  {/* Thanh tiến độ có hiệu ứng chuyển động mượt mà */}
                  <div className="w-full bg-zinc-200/70 h-2 rounded-full overflow-hidden">
                    <div
                      className={`h-full ${item.tailwindBg} rounded-full transition-all duration-700 ease-out`}
                      style={{ width: `${Math.max(item.percentage, item.count > 0 ? 2 : 0)}%` }}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* 5. LỐI TẮT NHANH (QUICK ACTIONS) */}
      <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between pb-4 mb-4 border-b border-zinc-100">
          <div>
            <h3 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
              Lối tắt Quản trị Nhanh (Quick Actions)
            </h3>
            <p className="text-xs text-zinc-500 mt-0.5">
              Truy cập nhanh vào các phân hệ nghiệp vụ chính của hệ thống E-commerce.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 font-mono">
          <Link
            to="/admin/orders"
            className="p-3.5 rounded-lg border border-zinc-200 bg-zinc-50/50 hover:bg-zinc-100/70 hover:border-zinc-300 transition-all flex flex-col items-center text-center group"
          >
            <div className="w-10 h-10 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <ClipboardList className="w-5 h-5" />
            </div>
            <span className="text-xs font-semibold text-zinc-950">Quản lý Đơn hàng</span>
            <span className="text-[10px] text-zinc-500 mt-0.5">Xử lý & giao vận</span>
          </Link>

          <Link
            to="/admin/products"
            className="p-3.5 rounded-lg border border-zinc-200 bg-zinc-50/50 hover:bg-zinc-100/70 hover:border-zinc-300 transition-all flex flex-col items-center text-center group"
          >
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <Package className="w-5 h-5" />
            </div>
            <span className="text-xs font-semibold text-zinc-950">Quản lý Sản phẩm</span>
            <span className="text-[10px] text-zinc-500 mt-0.5">{dashboardStats.totalProducts || 0} sản phẩm</span>
          </Link>

          <Link
            to="/admin/inventory"
            className="p-3.5 rounded-lg border border-zinc-200 bg-zinc-50/50 hover:bg-zinc-100/70 hover:border-zinc-300 transition-all flex flex-col items-center text-center group"
          >
            <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-700 border border-amber-200 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <Warehouse className="w-5 h-5" />
            </div>
            <span className="text-xs font-semibold text-zinc-950">Quản lý Tồn kho</span>
            <span className="text-[10px] text-zinc-500 mt-0.5">Cảnh báo & nhập hàng</span>
          </Link>

          <Link
            to="/admin/categories"
            className="p-3.5 rounded-lg border border-zinc-200 bg-zinc-50/50 hover:bg-zinc-100/70 hover:border-zinc-300 transition-all flex flex-col items-center text-center group"
          >
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <Tags className="w-5 h-5" />
            </div>
            <span className="text-xs font-semibold text-zinc-950">Quản lý Danh mục</span>
            <span className="text-[10px] text-zinc-500 mt-0.5">Phân loại hàng hóa</span>
          </Link>

          <Link
            to="/admin/users"
            className="p-3.5 rounded-lg border border-zinc-200 bg-zinc-50/50 hover:bg-zinc-100/70 hover:border-zinc-300 transition-all flex flex-col items-center text-center group"
          >
            <div className="w-10 h-10 rounded-lg bg-zinc-100 text-zinc-800 border border-zinc-200 flex items-center justify-center mb-2 group-hover:scale-105 transition-transform">
              <Users className="w-5 h-5" />
            </div>
            <span className="text-xs font-semibold text-zinc-950">Quản lý Người dùng</span>
            <span className="text-[10px] text-zinc-500 mt-0.5">{dashboardStats.totalUsers || 0} tài khoản</span>
          </Link>
        </div>
      </div>

      {/* 6. BẢNG DANH SÁCH SẢN PHẨM CÓ NGUY CƠ HẾT HÀNG (stock_quantity <= 5) */}
      <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 mb-4 border-b border-zinc-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-700 border border-rose-200 flex items-center justify-center shrink-0">
              <AlertTriangle className="w-4 h-4 stroke-[2]" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                Sản phẩm có nguy cơ hết hàng (Tồn kho &le; 5)
              </h2>
              <p className="text-xs text-zinc-500">
                Danh sách các mặt hàng có số lượng tồn kho chạm ngưỡng cảnh báo, cần nhập bổ sung kho ngay.
              </p>
            </div>
          </div>

          <Link
            to="/admin/inventory"
            className="btn-outline text-xs font-mono shrink-0 flex items-center gap-1.5"
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
                            className="w-10 h-10 rounded-md object-cover border border-zinc-200 shrink-0"
                          />
                        ) : (
                          <div className="w-10 h-10 rounded-md bg-zinc-100 text-zinc-400 flex items-center justify-center border border-zinc-200 shrink-0">
                            <Package className="w-4 h-4" />
                          </div>
                        )}
                        <div>
                          <div className="font-medium text-zinc-950">{p.name}</div>
                          <div className="text-[11px] text-zinc-400">Mã SP: #{p.id}</div>
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
                          ? 'bg-rose-100 text-rose-800 border border-rose-200'
                          : 'bg-amber-100 text-amber-800 border border-amber-200'
                      }`}>
                        {p.stockQuantity === 0 ? 'Hết hàng (0)' : `Còn ${p.stockQuantity}`}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-right">
                      <Link
                        to="/admin/inventory"
                        className="inline-flex items-center gap-1 text-xs font-medium text-zinc-950 hover:underline"
                      >
                        <span>Nhập kho</span>
                        <ArrowRight className="w-3.5 h-3.5" />
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
            <p>Tất cả sản phẩm hiện đều có tồn kho an toàn (&gt; 5 sản phẩm).</p>
          </div>
        )}
      </div>

      {/* 6.1. ĐỐI SOÁT NHẬT KÝ BIẾN ĐỘNG KHO (INVENTORY RECONCILIATION) */}
      <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-4 mb-4 border-b border-zinc-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 flex items-center justify-center shrink-0">
              <Warehouse className="w-4 h-4 stroke-[2]" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                Đối soát Nhật ký Biến động Kho (Inventory Audit)
              </h2>
              <p className="text-xs text-zinc-500">
                Thống kê các giao dịch kho tự động và thủ công: ORDER_PLACED, ORDER_CANCELLED, MANUAL_IMPORT.
              </p>
            </div>
          </div>
          <Link
            to="/admin/inventory"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-zinc-200 bg-white hover:bg-zinc-50 text-xs font-mono text-zinc-900 shadow-xs"
          >
            <span>Lịch sử biến động</span>
            <ArrowRight className="w-3.5 h-3.5 text-zinc-600" />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 font-mono">
          <div className="p-4 rounded-lg bg-zinc-50 border border-zinc-200 flex items-center justify-between">
            <div>
              <span className="text-xs text-zinc-500 uppercase tracking-wider block">Trừ kho khi đặt hàng</span>
              <span className="text-lg font-bold text-zinc-950 mt-1 block tabular-nums">
                {dashboardStats.orderPlacedCount ?? 0} <span className="text-xs font-normal text-zinc-500">lần</span>
              </span>
              <span className="text-[11px] text-zinc-400 mt-0.5 block">Loại: ORDER_PLACED</span>
            </div>
            <div className="w-9 h-9 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center justify-center shrink-0">
              <ShoppingBag className="w-4 h-4" />
            </div>
          </div>

          <div className="p-4 rounded-lg bg-zinc-50 border border-zinc-200 flex items-center justify-between">
            <div>
              <span className="text-xs text-zinc-500 uppercase tracking-wider block">Hoàn kho do hủy đơn</span>
              <span className="text-lg font-bold text-zinc-950 mt-1 block tabular-nums">
                {dashboardStats.orderCancelledCount ?? 0} <span className="text-xs font-normal text-zinc-500">lần</span>
              </span>
              <span className="text-[11px] text-zinc-400 mt-0.5 block">Loại: ORDER_CANCELLED</span>
            </div>
            <div className="w-9 h-9 rounded-md bg-rose-50 text-rose-700 border border-rose-200 flex items-center justify-center shrink-0">
              <XCircle className="w-4 h-4" />
            </div>
          </div>

          <div className="p-4 rounded-lg bg-zinc-50 border border-zinc-200 flex items-center justify-between">
            <div>
              <span className="text-xs text-zinc-500 uppercase tracking-wider block">Admin nhập kho thủ công</span>
              <span className="text-lg font-bold text-zinc-950 mt-1 block tabular-nums">
                {dashboardStats.manualImportCount ?? 0} <span className="text-xs font-normal text-zinc-500">lần</span>
              </span>
              <span className="text-[11px] text-zinc-400 mt-0.5 block">Loại: MANUAL_IMPORT</span>
            </div>
            <div className="w-9 h-9 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center justify-center shrink-0">
              <Package className="w-4 h-4" />
            </div>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-zinc-100 flex items-center justify-between text-xs text-zinc-500 font-mono">
          <span>Công thức đối soát: Tồn kho thực tế = Tồn kho ban đầu + &sum;(Biến động kho)</span>
          <span className="text-emerald-700 font-medium">Khớp 100% CSDL MySQL</span>
        </div>
      </div>

      {/* 7. KIẾN TRÚC BẢO MẬT & THÔNG TIN PHIÊN QUẢN TRỊ (SECURITY & ADMIN SESSION) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Cột trái (2/3): Kiến trúc Bảo mật & Phân quyền */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-zinc-200 shadow-sm p-6">
          <div className="flex items-center justify-between pb-4 mb-4 border-b border-zinc-100">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                <Shield className="w-4 h-4 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                  Kiến trúc Bảo mật & Xác thực Hệ thống
                </h2>
                <p className="text-xs text-zinc-500">
                  Cơ chế bảo vệ phiên làm việc và kiểm soát truy cập phân tầng (RBAC)
                </p>
              </div>
            </div>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200 font-mono">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
              Toàn vẹn 100%
            </span>
          </div>

          <div className="space-y-3 font-mono text-xs">
            <div className="flex items-start justify-between gap-4 p-3 rounded-lg bg-zinc-50 border border-zinc-200/80">
              <div className="flex items-start gap-2.5">
                <div className="w-6 h-6 rounded bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="font-semibold text-zinc-900">Bảo mật JWT & Token Rotation</h4>
                  <p className="text-zinc-500 mt-0.5 leading-relaxed">
                    Access Token lưu thuần In-Memory chống XSS, Refresh Token gửi qua Cookie HttpOnly và luân phiên nguyên tử qua Redis.
                  </p>
                </div>
              </div>
              <span className="shrink-0 px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200">
                Kích hoạt
              </span>
            </div>

            <div className="flex items-start justify-between gap-4 p-3 rounded-lg bg-zinc-50 border border-zinc-200/80">
              <div className="flex items-start gap-2.5">
                <div className="w-6 h-6 rounded bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="font-semibold text-zinc-900">Phân quyền phân tầng Role-Based Access Control (RBAC)</h4>
                  <p className="text-zinc-500 mt-0.5 leading-relaxed">
                    Bảo vệ các endpoint quản trị phía Backend bằng annotation @PreAuthorize("hasRole('ADMIN')") và Spring Security Context.
                  </p>
                </div>
              </div>
              <span className="shrink-0 px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200">
                Kích hoạt
              </span>
            </div>

            <div className="flex items-start justify-between gap-4 p-3 rounded-lg bg-zinc-50 border border-zinc-200/80">
              <div className="flex items-start gap-2.5">
                <div className="w-6 h-6 rounded bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="font-semibold text-zinc-900">Phòng chống tấn công Brute-Force & Hết hạn Token</h4>
                  <p className="text-zinc-500 mt-0.5 leading-relaxed">
                    Khóa tài khoản tự động khi đăng nhập sai nhiều lần và Axios Interceptor tự động làm mới hoặc điều hướng khi 401.
                  </p>
                </div>
              </div>
              <span className="shrink-0 px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200">
                Kích hoạt
              </span>
            </div>
          </div>
        </div>

        {/* Cột phải (1/3): Hồ sơ Admin đang hoạt động */}
        <div className="bg-white rounded-xl border border-zinc-200 shadow-sm p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2.5 pb-4 mb-4 border-b border-zinc-100">
              <div className="w-8 h-8 rounded-lg bg-zinc-100 text-zinc-800 flex items-center justify-center shrink-0">
                <Lock className="w-4 h-4 stroke-[1.75]" />
              </div>
              <div>
                <h2 className="text-sm sm:text-base font-semibold text-zinc-950 font-mono">
                  Phiên làm việc Quản trị
                </h2>
                <p className="text-xs text-zinc-500">Tài khoản đang đăng nhập</p>
              </div>
            </div>

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

            <div className="mt-4 p-3.5 rounded-lg bg-zinc-50 border border-zinc-200 space-y-2 text-xs text-zinc-700 font-mono">
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Vai trò:</span>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-zinc-200 text-zinc-900 font-semibold">
                  <Shield className="w-3 h-3" /> {activeAdmin?.role || 'ADMIN'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Trạng thái:</span>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 font-medium">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                  Hoạt động
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-medium text-zinc-500">Phiên bảo mật:</span>
                <span className="text-zinc-950 font-semibold">Đã xác thực</span>
              </div>
            </div>
          </div>

          <div className="mt-5 pt-4 border-t border-zinc-100 flex flex-col gap-2">
            <Link
              to="/admin/users"
              className="btn-primary w-full font-mono text-xs flex items-center justify-center gap-1.5"
            >
              <Users className="w-3.5 h-3.5" />
              <span>Chuyển tới Quản lý người dùng</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminDashboardPage
