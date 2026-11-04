import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  ClipboardList,
  Search,
  RefreshCw,
  Eye,
  CheckCircle2,
  XCircle,
  Clock,
  Package,
  Truck,
  AlertCircle,
  Copy,
  Check,
  ChevronLeft,
  ChevronRight,
  User,
  Phone,
  MapPin,
  FileText,
  AlertTriangle,
  X,
} from 'lucide-react'
import orderService from '../../services/orderService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  OrderStatusBadge,
  PaymentStatusBadge,
  formatPrice,
  formatDateTime,
  FALLBACK_IMAGE,
} from '../orders/OrderBadges'

// Định nghĩa các trạng thái đơn hàng và nhãn hiển thị
const ORDER_STATUS_CONFIG = {
  PENDING: {
    label: 'Chờ xác nhận',
    color: 'bg-amber-50 text-amber-800 border-amber-200',
    dotColor: 'bg-amber-500',
    icon: Clock,
  },
  CONFIRMED: {
    label: 'Đã xác nhận',
    color: 'bg-blue-50 text-blue-800 border-blue-200',
    dotColor: 'bg-blue-500',
    icon: CheckCircle2,
  },
  PROCESSING: {
    label: 'Đang đóng gói',
    color: 'bg-indigo-50 text-indigo-800 border-indigo-200',
    dotColor: 'bg-indigo-500',
    icon: Package,
  },
  SHIPPING: {
    label: 'Đang giao hàng',
    color: 'bg-purple-50 text-purple-800 border-purple-200',
    dotColor: 'bg-purple-500',
    icon: Truck,
  },
  COMPLETED: {
    label: 'Hoàn thành',
    color: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    dotColor: 'bg-emerald-500',
    icon: CheckCircle2,
  },
  CANCELLED: {
    label: 'Đã hủy',
    color: 'bg-rose-50 text-rose-800 border-rose-200',
    dotColor: 'bg-rose-500',
    icon: XCircle,
  },
}

// State Machine: Tính toán các trạng thái chuyển đổi hợp lệ tiếp theo
const getNextAvailableStatuses = (currentStatus) => {
  switch (currentStatus) {
    case 'PENDING':
      return [
        { value: 'CONFIRMED', label: 'Duyệt đơn (Đã xác nhận)', icon: CheckCircle2, color: 'text-blue-600' },
        { value: 'CANCELLED', label: 'Hủy đơn hàng', icon: XCircle, color: 'text-rose-600' },
      ]
    case 'CONFIRMED':
      return [
        { value: 'PROCESSING', label: 'Bắt đầu chuẩn bị hàng', icon: Package, color: 'text-indigo-600' },
        { value: 'CANCELLED', label: 'Hủy đơn hàng', icon: XCircle, color: 'text-rose-600' },
      ]
    case 'PROCESSING':
      return [
        { value: 'SHIPPING', label: 'Bàn giao vận chuyển (Đang giao)', icon: Truck, color: 'text-purple-600' },
      ]
    case 'SHIPPING':
      return [
        { value: 'COMPLETED', label: 'Giao hàng thành công (Hoàn thành)', icon: CheckCircle2, color: 'text-emerald-600' },
      ]
    case 'COMPLETED':
    case 'CANCELLED':
    default:
      return []
  }
}

export const AdminOrdersPage = () => {
  // --- Danh sách đơn hàng & Phân trang ---
  const [orders, setOrders] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  // --- Bộ lọc & Tìm kiếm ---
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  // --- Trạng thái giao diện ---
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)
  const [copiedCode, setCopiedCode] = useState(null)

  // --- Modal Chi tiết đơn hàng ---
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)

  // --- Modal xác nhận thay đổi trạng thái ---
  const [statusConfirmModal, setStatusConfirmModal] = useState({
    isOpen: false,
    order: null,
    newStatus: null,
    newStatusLabel: '',
  })
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false)

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 4000)
  }

  // Tải danh sách đơn hàng từ API
  const fetchOrders = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      const params = {
        page,
        size: pageSize,
        sortBy: 'createdAt',
        sortDir: 'desc',
      }

      if (statusFilter !== 'ALL') {
        params.status = statusFilter
      }

      if (searchQuery.trim()) {
        params.search = searchQuery.trim()
      }

      const res = await orderService.getAdminOrders(params)
      const data = res?.data || res

      if (data) {
        if (Array.isArray(data)) {
          // Backend trả về dạng mảng
          setOrders(data)
          setTotalElements(data.length)
          setTotalPages(Math.ceil(data.length / pageSize) || 1)
        } else if (data.content && Array.isArray(data.content)) {
          // Backend trả về dạng PageResponse
          setOrders(data.content)
          setTotalElements(data.totalElements ?? data.content.length)
          setTotalPages(data.totalPages ?? 1)
        } else {
          setOrders([])
          setTotalElements(0)
          setTotalPages(1)
        }
      } else {
        setOrders([])
        setTotalElements(0)
        setTotalPages(1)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách đơn hàng:', err)
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách đơn hàng. Vui lòng thử lại.'
      setError(msg)
      showToast(msg, 'error')
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, statusFilter, searchQuery])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  // Xử lý tìm kiếm
  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setPage(0)
    setSearchQuery(searchInput)
  }

  const handleClearSearch = () => {
    setSearchInput('')
    setSearchQuery('')
    setPage(0)
  }

  // Copy mã đơn hàng
  const handleCopyCode = (code, e) => {
    e?.stopPropagation()
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(code)
      setCopiedCode(code)
      setTimeout(() => setCopiedCode(null), 2000)
    }
  }

  // Mở modal xác nhận cập nhật trạng thái
  const handleInitiateStatusChange = (order, newStatus) => {
    const config = ORDER_STATUS_CONFIG[newStatus]
    setStatusConfirmModal({
      isOpen: true,
      order,
      newStatus,
      newStatusLabel: config ? config.label : newStatus,
    })
  }

  // Thực thi cập nhật trạng thái đơn hàng
  const handleConfirmStatusChange = async () => {
    const { order, newStatus } = statusConfirmModal
    if (!order || !newStatus) return

    setIsUpdatingStatus(true)
    try {
      const res = await orderService.updateAdminOrderStatus(order.id, newStatus)
      const updatedOrder = res?.data || res

      // Cập nhật state đơn hàng trong bảng
      setOrders((prev) =>
        prev.map((o) => (o.id === order.id ? { ...o, ...updatedOrder } : o))
      )

      // Cập nhật selectedOrder nếu modal chi tiết đang mở
      if (selectedOrder && selectedOrder.id === order.id) {
        setSelectedOrder((prev) => ({ ...prev, ...updatedOrder }))
      }

      showToast(
        `Đã chuyển trạng thái đơn hàng #${order.orderCode} sang "${
          ORDER_STATUS_CONFIG[newStatus]?.label || newStatus
        }" thành công!`,
        'success'
      )
      setStatusConfirmModal({ isOpen: false, order: null, newStatus: null, newStatusLabel: '' })
    } catch (err) {
      console.error('Lỗi khi cập nhật trạng thái đơn hàng:', err)
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể cập nhật trạng thái đơn hàng. Vui lòng thử lại.'
      showToast(msg, 'error')
    } finally {
      setIsUpdatingStatus(false)
    }
  }

  // Xem chi tiết đơn hàng
  const handleOpenDetailModal = async (order) => {
    setSelectedOrder(order)
    setDetailLoading(true)
    try {
      const res = await orderService.getAdminOrderById(order.id)
      const fullOrder = res?.data || res
      if (fullOrder && fullOrder.id) {
        setSelectedOrder(fullOrder)
      }
    } catch {
      // Nếu API getById lỗi, giữ nguyên dữ liệu tóm tắt từ bảng
    } finally {
      setDetailLoading(false)
    }
  }

  // Thống kê nhanh từ danh sách hiện tại
  const stats = useMemo(() => {
    const total = totalElements || orders.length
    let pending = 0
    let inProgress = 0
    let completed = 0
    let cancelled = 0
    let totalRevenue = 0

    orders.forEach((o) => {
      const st = o.orderStatus
      if (st === 'PENDING') pending += 1
      else if (st === 'CONFIRMED' || st === 'PROCESSING' || st === 'SHIPPING') inProgress += 1
      else if (st === 'COMPLETED') {
        completed += 1
        totalRevenue += Number(o.totalAmount || 0)
      } else if (st === 'CANCELLED') cancelled += 1
    })

    return { total, pending, inProgress, completed, cancelled, totalRevenue }
  }, [orders, totalElements])

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 max-w-md w-full animate-in fade-in slide-in-from-bottom-3 duration-200">
          <div className="bg-white rounded-2xl shadow-xl border border-slate-200/90 p-4 flex items-start gap-3 backdrop-blur-md">
            {toast.type === 'error' ? (
              <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
            ) : (
              <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />
            )}
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-slate-900 leading-snug">
                {toast.message}
              </p>
            </div>
            <button
              onClick={() => setToast(null)}
              className="text-slate-400 hover:text-slate-600 p-0.5 cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Header trang */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2.5">
            <ClipboardList className="w-7 h-7 text-zinc-900" />
            <span>Quản lý đơn hàng</span>
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Theo dõi, tra cứu và xử lý toàn bộ đơn hàng trên hệ thống thương mại điện tử
          </p>
        </div>

        <button
          onClick={fetchOrders}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-700 hover:bg-slate-50 active:scale-[0.98] transition-all shadow-2xs cursor-pointer self-start sm:self-auto disabled:opacity-60"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-zinc-900' : 'text-slate-500'}`} />
          <span>{isLoading ? 'Đang tải...' : 'Làm mới dữ liệu'}</span>
        </button>
      </div>

      {/* KPI Cards: Thống kê nhanh */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3.5">
        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-[11px] font-semibold uppercase tracking-wider">Tổng đơn hàng</span>
            <ClipboardList className="w-4 h-4 text-slate-400" />
          </div>
          <div className="text-2xl font-black text-slate-900 tabular-nums">
            {stats.total}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-amber-200/80 bg-amber-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-amber-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Chờ xác nhận</span>
            <Clock className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl font-black text-amber-900 tabular-nums">
            {stats.pending}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-indigo-200/80 bg-indigo-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-indigo-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Đang xử lý / Giao</span>
            <Truck className="w-4 h-4 text-indigo-500" />
          </div>
          <div className="text-2xl font-black text-indigo-900 tabular-nums">
            {stats.inProgress}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-emerald-200/80 bg-emerald-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-emerald-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Hoàn thành</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-black text-emerald-900 tabular-nums">
            {stats.completed}
          </div>
        </div>

        <div className="col-span-2 lg:col-span-1 bg-white rounded-2xl border border-rose-200/80 bg-rose-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-rose-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Đã hủy</span>
            <XCircle className="w-4 h-4 text-rose-500" />
          </div>
          <div className="text-2xl font-black text-rose-900 tabular-nums">
            {stats.cancelled}
          </div>
        </div>
      </div>

      {/* Toolbar: Tìm kiếm, Bộ lọc trạng thái & Phân trang */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-2xs space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3.5">
          {/* Ô tìm kiếm */}
          <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm theo mã đơn (ORD-...), tên người nhận, SĐT..."
              className="w-full pl-9 pr-9 py-2.5 rounded-xl border border-slate-200 bg-slate-50/60 text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-zinc-900 focus:border-transparent transition-all"
            />
            {searchInput && (
              <button
                type="button"
                onClick={handleClearSearch}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </form>

          {/* Chọn số dòng mỗi trang */}
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span>Hiển thị:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value))
                setPage(0)
              }}
              className="px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white text-xs font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-zinc-900 cursor-pointer"
            >
              <option value={10}>10 đơn / trang</option>
              <option value={20}>20 đơn / trang</option>
              <option value={50}>50 đơn / trang</option>
            </select>
          </div>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-1 border-t border-slate-100 scrollbar-none">
          {[
            { key: 'ALL', label: 'Tất cả' },
            { key: 'PENDING', label: 'Chờ xác nhận' },
            { key: 'CONFIRMED', label: 'Đã xác nhận' },
            { key: 'PROCESSING', label: 'Đang đóng gói' },
            { key: 'SHIPPING', label: 'Đang giao' },
            { key: 'COMPLETED', label: 'Hoàn thành' },
            { key: 'CANCELLED', label: 'Đã hủy' },
          ].map((tab) => {
            const isActive = statusFilter === tab.key
            return (
              <button
                key={tab.key}
                onClick={() => {
                  setStatusFilter(tab.key)
                  setPage(0)
                }}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  isActive
                    ? 'bg-zinc-900 text-white shadow-2xs'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            )
          })}
        </div>
      </div>

      {/* Bảng danh sách đơn hàng */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="py-24 flex flex-col items-center justify-center space-y-3">
            <LoadingSpinner size="lg" className="text-zinc-900" />
            <p className="text-xs text-slate-500 font-medium">Đang tải danh sách đơn hàng...</p>
          </div>
        ) : error ? (
          <div className="py-16 px-4 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-slate-900">Không thể tải dữ liệu đơn hàng</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">{error}</p>
            <button
              onClick={fetchOrders}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 transition-colors cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : orders.length === 0 ? (
          <div className="py-20 px-4 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
              <ClipboardList className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-slate-900">Không tìm thấy đơn hàng nào</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {searchQuery || statusFilter !== 'ALL'
                ? 'Không có đơn hàng nào khớp với điều kiện tìm kiếm hoặc bộ lọc hiện tại.'
                : 'Chưa có đơn hàng nào được tạo trong hệ thống.'}
            </p>
            {(searchQuery || statusFilter !== 'ALL') && (
              <button
                onClick={() => {
                  setStatusFilter('ALL')
                  setSearchInput('')
                  setSearchQuery('')
                  setPage(0)
                }}
                className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
              >
                Xóa bộ lọc
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4">Mã đơn hàng</th>
                  <th className="py-3.5 px-4">Người nhận</th>
                  <th className="py-3.5 px-4">Địa chỉ giao hàng</th>
                  <th className="py-3.5 px-4">Ngày tạo</th>
                  <th className="py-3.5 px-4 text-right">Tổng tiền</th>
                  <th className="py-3.5 px-4 text-center">Thanh toán</th>
                  <th className="py-3.5 px-4">Trạng thái đơn & Cập nhật</th>
                  <th className="py-3.5 px-4 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((order) => {
                  const currentSt = order.orderStatus
                  const availableNext = getNextAvailableStatuses(currentSt)
                  const isTerminal = currentSt === 'COMPLETED' || currentSt === 'CANCELLED'

                  return (
                    <tr
                      key={order.id}
                      className="hover:bg-slate-50/60 transition-colors group"
                    >
                      {/* Mã đơn */}
                      <td className="py-3.5 px-4">
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-1.5">
                            <span className="font-mono font-bold text-slate-900 group-hover:text-zinc-950">
                              #{order.orderCode}
                            </span>
                            <button
                              onClick={(e) => handleCopyCode(order.orderCode, e)}
                              className="p-1 text-slate-400 hover:text-zinc-950 hover:bg-slate-200 rounded transition-colors cursor-pointer"
                              title="Sao chép mã"
                            >
                              {copiedCode === order.orderCode ? (
                                <Check className="w-3 h-3 text-emerald-600" />
                              ) : (
                                <Copy className="w-3 h-3" />
                              )}
                            </button>
                          </div>
                          <span className="text-[10px] text-slate-400 block">
                            {order.itemCount || order.items?.length || 1} sản phẩm
                          </span>
                        </div>
                      </td>

                      {/* Người nhận */}
                      <td className="py-3.5 px-4">
                        <div className="space-y-0.5 max-w-[160px]">
                          <p className="font-bold text-slate-900 truncate">
                            {order.recipientName}
                          </p>
                          <p className="text-[11px] text-slate-500 font-mono">
                            {order.recipientPhone}
                          </p>
                          {order.userEmail && (
                            <p className="text-[10px] text-slate-400 truncate">
                              {order.userEmail}
                            </p>
                          )}
                        </div>
                      </td>

                      {/* Địa chỉ giao hàng */}
                      <td className="py-3.5 px-4">
                        <p
                          className="max-w-[200px] text-slate-600 line-clamp-2 leading-relaxed"
                          title={order.shippingAddress}
                        >
                          {order.shippingAddress}
                        </p>
                      </td>

                      {/* Ngày tạo */}
                      <td className="py-3.5 px-4 whitespace-nowrap text-slate-500">
                        {formatDateTime(order.createdAt)}
                      </td>

                      {/* Tổng tiền */}
                      <td className="py-3.5 px-4 text-right">
                        <span className="font-extrabold text-slate-900 tabular-nums">
                          {formatPrice(order.totalAmount)}
                        </span>
                      </td>

                      {/* Trạng thái thanh toán */}
                      <td className="py-3.5 px-4 text-center">
                        <div className="flex flex-col items-center gap-1">
                          <PaymentStatusBadge
                            status={order.paymentStatus}
                            description={order.paymentStatusDescription}
                          />
                          <span className="text-[10px] font-semibold text-slate-400">
                            {order.paymentMethodDescription || order.paymentMethod || 'COD'}
                          </span>
                        </div>
                      </td>

                      {/* Dropdown cập nhật trạng thái đơn hàng */}
                      <td className="py-3.5 px-4">
                        <div className="space-y-1.5">
                          {/* Badge trạng thái hiện tại */}
                          <div className="flex items-center gap-1.5">
                            <OrderStatusBadge
                              status={currentSt}
                              description={order.orderStatusDescription}
                            />
                          </div>

                          {/* Dropdown chuyển trạng thái tiếp theo */}
                          {!isTerminal && availableNext.length > 0 && (
                            <div className="relative inline-block text-left">
                              <select
                                value=""
                                onChange={(e) => {
                                  if (e.target.value) {
                                    handleInitiateStatusChange(order, e.target.value)
                                  }
                                }}
                                className="text-[11px] font-semibold py-1 px-2.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-zinc-900 cursor-pointer shadow-2xs"
                              >
                                <option value="" disabled>
                                  Chuyển trạng thái...
                                </option>
                                {availableNext.map((st) => (
                                  <option key={st.value} value={st.value}>
                                    → {st.label}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                        </div>
                      </td>

                      {/* Thao tác: Xem chi tiết */}
                      <td className="py-3.5 px-4 text-center">
                        <button
                          onClick={() => handleOpenDetailModal(order)}
                          className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-zinc-900 hover:text-white text-slate-700 font-semibold text-[11px] transition-all cursor-pointer shadow-2xs"
                          title="Xem chi tiết đơn hàng"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Chi tiết</span>
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
            <div>
              Hiển thị <strong>{page * pageSize + 1}</strong> -{' '}
              <strong>{Math.min((page + 1) * pageSize, totalElements)}</strong> trên tổng số{' '}
              <strong>{totalElements}</strong> đơn hàng
            </div>

            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <span className="px-3 py-1 font-bold text-slate-900">
                Trang {page + 1} / {totalPages}
              </span>

              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal xác nhận cập nhật trạng thái đơn hàng */}
      {statusConfirmModal.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-md animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3">
              <div
                className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 ${
                  statusConfirmModal.newStatus === 'CANCELLED'
                    ? 'bg-rose-50 text-rose-600 border border-rose-200'
                    : 'bg-zinc-100 text-zinc-900 border border-zinc-200'
                }`}
              >
                {statusConfirmModal.newStatus === 'CANCELLED' ? (
                  <AlertTriangle className="w-5 h-5" />
                ) : (
                  <CheckCircle2 className="w-5 h-5" />
                )}
              </div>
              <div>
                <h3 className="text-base font-extrabold text-slate-900">
                  Xác nhận chuyển trạng thái
                </h3>
                <p className="text-xs text-slate-500 font-mono mt-0.5">
                  Đơn hàng #{statusConfirmModal.order?.orderCode}
                </p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn có chắc chắn muốn chuyển đơn hàng này sang trạng thái{' '}
              <strong className="text-slate-900">
                "{statusConfirmModal.newStatusLabel}"
              </strong>
              ?
              {statusConfirmModal.newStatus === 'CANCELLED' && (
                <span className="block mt-2 text-rose-700 bg-rose-50 p-2.5 rounded-xl border border-rose-200">
                  Lưu ý: Sau khi hủy, toàn bộ số lượng sản phẩm sẽ được tự động hoàn lại vào kho.
                </span>
              )}
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() =>
                  setStatusConfirmModal({
                    isOpen: false,
                    order: null,
                    newStatus: null,
                    newStatusLabel: '',
                  })
                }
                disabled={isUpdatingStatus}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmStatusChange}
                disabled={isUpdatingStatus}
                className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold text-white transition-all cursor-pointer shadow-2xs ${
                  statusConfirmModal.newStatus === 'CANCELLED'
                    ? 'bg-rose-600 hover:bg-rose-700'
                    : 'bg-zinc-900 hover:bg-zinc-800'
                }`}
              >
                {isUpdatingStatus ? (
                  <>
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Đang cập nhật...</span>
                  </>
                ) : (
                  <span>Xác nhận cập nhật</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Chi tiết đơn hàng */}
      {selectedOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-md animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-2xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="p-5 sm:p-6 border-b border-slate-100 flex items-center justify-between gap-4 shrink-0">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-black text-slate-900">
                    Đơn hàng #{selectedOrder.orderCode}
                  </h3>
                  <button
                    onClick={(e) => handleCopyCode(selectedOrder.orderCode, e)}
                    className="p-1 text-slate-400 hover:text-zinc-950 rounded transition-colors"
                  >
                    {copiedCode === selectedOrder.orderCode ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Ngày đặt: {formatDateTime(selectedOrder.createdAt)}
                </p>
              </div>

              <div className="flex items-center gap-2">
                <OrderStatusBadge
                  status={selectedOrder.orderStatus}
                  description={selectedOrder.orderStatusDescription}
                />
                <button
                  onClick={() => setSelectedOrder(null)}
                  className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-5 sm:p-6 overflow-y-auto space-y-5">
              {detailLoading && (
                <div className="py-4 text-center">
                  <LoadingSpinner size="sm" className="text-zinc-900 inline-block mr-2" />
                  <span className="text-xs text-slate-500">Đang đồng bộ chi tiết mới nhất...</span>
                </div>
              )}

              {/* Thông tin khách hàng & Giao nhận */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs bg-slate-50/70 p-4 rounded-2xl border border-slate-100">
                <div className="space-y-1">
                  <span className="text-slate-400 font-medium">Người nhận</span>
                  <div className="flex items-center gap-1.5 font-bold text-slate-900">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span>{selectedOrder.recipientName}</span>
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-slate-400 font-medium">Số điện thoại</span>
                  <div className="flex items-center gap-1.5 font-bold text-slate-900 font-mono">
                    <Phone className="w-3.5 h-3.5 text-slate-400" />
                    <span>{selectedOrder.recipientPhone}</span>
                  </div>
                </div>

                <div className="sm:col-span-2 space-y-1">
                  <span className="text-slate-400 font-medium">Địa chỉ nhận hàng</span>
                  <div className="flex items-start gap-1.5 font-semibold text-slate-800">
                    <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                    <span>{selectedOrder.shippingAddress}</span>
                  </div>
                </div>

                {selectedOrder.note && (
                  <div className="sm:col-span-2 space-y-1 pt-2 border-t border-slate-200/60">
                    <span className="text-slate-400 font-medium">Ghi chú</span>
                    <div className="flex items-start gap-1.5 text-slate-700 italic">
                      <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                      <span>{selectedOrder.note}</span>
                    </div>
                  </div>
                )}
              </div>

              {/* Thông tin thanh toán */}
              <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-100 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Phương thức thanh toán</span>
                  <span className="font-bold text-slate-900">
                    {selectedOrder.paymentMethodDescription || selectedOrder.paymentMethod || 'COD'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Trạng thái thanh toán</span>
                  <PaymentStatusBadge
                    status={selectedOrder.paymentStatus}
                    description={selectedOrder.paymentStatusDescription}
                  />
                </div>
                {selectedOrder.transactionCode && (
                  <div className="flex items-center justify-between pt-1 border-t border-slate-200/60">
                    <span className="text-slate-500">Mã giao dịch</span>
                    <span className="font-mono font-bold text-slate-800">
                      {selectedOrder.transactionCode}
                    </span>
                  </div>
                )}
              </div>

              {/* Danh sách sản phẩm */}
              <div>
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2.5">
                  Danh sách sản phẩm ({selectedOrder.items?.length || 0})
                </h4>

                <div className="border border-slate-200 rounded-2xl overflow-hidden divide-y divide-slate-100">
                  {Array.isArray(selectedOrder.items) && selectedOrder.items.length > 0 ? (
                    selectedOrder.items.map((item) => (
                      <div
                        key={item.id || item.productId}
                        className="p-3.5 flex items-center justify-between gap-3 text-xs"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <img
                            src={item.imageUrl || FALLBACK_IMAGE}
                            alt={item.productName}
                            onError={(e) => {
                              e.target.onerror = null
                              e.target.src = FALLBACK_IMAGE
                            }}
                            className="w-12 h-12 rounded-xl object-cover bg-slate-100 border border-slate-100 shrink-0"
                          />
                          <div className="min-w-0">
                            <p className="font-bold text-slate-900 truncate">
                              {item.productName}
                            </p>
                            <p className="text-[11px] text-slate-500 mt-0.5">
                              {formatPrice(item.unitPrice)} × {item.quantity}
                            </p>
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <span className="font-bold text-slate-900 tabular-nums">
                            {formatPrice(item.subtotal || item.unitPrice * item.quantity)}
                          </span>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="p-4 text-center text-xs text-slate-400">
                      Không có thông tin chi tiết món hàng
                    </div>
                  )}
                </div>
              </div>

              {/* Tổng cộng */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="font-bold text-slate-700">Tổng thanh toán:</span>
                <span className="text-lg font-black text-zinc-900 tabular-nums">
                  {formatPrice(selectedOrder.totalAmount)}
                </span>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 sm:p-5 border-t border-slate-100 bg-slate-50/60 flex items-center justify-between gap-3 shrink-0">
              <div className="text-xs text-slate-500">
                Trạng thái: <strong>{selectedOrder.orderStatusDescription || selectedOrder.orderStatus}</strong>
              </div>
              <button
                type="button"
                onClick={() => setSelectedOrder(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminOrdersPage
