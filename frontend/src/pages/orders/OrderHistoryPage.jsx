import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ShoppingBag,
  Search,
  X,
  ChevronRight,
  AlertCircle,
  PackageOpen,
  Calendar,
  User,
  Phone,
  MapPin,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react'
import orderService from '../../services/orderService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  OrderStatusBadge,
  PaymentStatusBadge,
  formatPrice,
  formatDateTime,
  FALLBACK_IMAGE,
} from './OrderBadges'

const STATUS_TABS = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PROCESSING', label: 'Đang xử lý' },
  { key: 'SHIPPING', label: 'Đang giao hàng' },
  { key: 'COMPLETED', label: 'Hoàn thành' },
  { key: 'CANCELLED', label: 'Đã hủy' },
]

export const OrderHistoryPage = () => {
  const navigate = useNavigate()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Filter & Search
  const [activeTab, setActiveTab] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')

  // Cancel order modal
  const [cancelModalOrder, setCancelModalOrder] = useState(null)
  const [cancelling, setCancelling] = useState(false)

  // Toast notification
  const [toast, setToast] = useState(null)

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 3500)
  }

  // Tải danh sách đơn hàng của người dùng hiện tại
  const fetchOrders = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await orderService.getMyOrders()
      let orderList = []
      if (Array.isArray(res)) {
        orderList = res
      } else if (res?.data && Array.isArray(res.data)) {
        orderList = res.data
      } else if (res?.data?.content && Array.isArray(res.data.content)) {
        orderList = res.data.content
      } else if (res?.content && Array.isArray(res.content)) {
        orderList = res.content
      }
      setOrders(orderList)
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải lịch sử đơn hàng. Vui lòng thử lại.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  // Tính số lượng đơn cho từng tab trạng thái
  const tabCounts = useMemo(() => {
    const counts = { ALL: orders.length }
    STATUS_TABS.forEach((tab) => {
      if (tab.key !== 'ALL') {
        counts[tab.key] = orders.filter(
          (o) => (o.orderStatus || o.status) === tab.key
        ).length
      }
    })
    return counts
  }, [orders])

  // Lọc danh sách theo tab và ô tìm kiếm
  const filteredOrders = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()

    return orders.filter((order) => {
      const currentStatus = order.orderStatus || order.status

      // Lọc theo tab trạng thái
      if (activeTab !== 'ALL' && currentStatus !== activeTab) {
        return false
      }

      // Lọc theo tìm kiếm: orderCode, recipientName, tên sản phẩm
      if (query) {
        const matchCode = (order.orderCode || '').toLowerCase().includes(query)
        const matchRecipient = (order.recipientName || '')
          .toLowerCase()
          .includes(query)
        const matchItems =
          Array.isArray(order.items) &&
          order.items.some((item) =>
            (item.productName || '').toLowerCase().includes(query)
          )

        if (!matchCode && !matchRecipient && !matchItems) {
          return false
        }
      }

      return true
    })
  }, [orders, activeTab, searchQuery])

  // Xử lý hủy đơn hàng
  const handleConfirmCancel = async () => {
    if (!cancelModalOrder) return

    setCancelling(true)
    try {
      const res = await orderService.cancelOrder(cancelModalOrder.id)
      const updatedOrder = res?.data || res

      // Cập nhật state trực tiếp để UI phản hồi ngay lập tức
      setOrders((prev) =>
        prev.map((o) =>
          o.id === cancelModalOrder.id
            ? {
                ...o,
                orderStatus: updatedOrder.orderStatus || 'CANCELLED',
                orderStatusDescription:
                  updatedOrder.orderStatusDescription || 'Đã hủy',
                updatedAt: updatedOrder.updatedAt || new Date().toISOString(),
              }
            : o
        )
      )

      showToast(
        `Đã hủy đơn hàng #${cancelModalOrder.orderCode} thành công! Số lượng sản phẩm đã được hoàn lại vào kho.`,
        'success'
      )
      setCancelModalOrder(null)
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể hủy đơn hàng. Vui lòng thử lại.'
      showToast(msg, 'error')
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-8 px-4 sm:px-6 lg:px-8">
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
              className="text-slate-400 hover:text-slate-600 p-0.5"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {cancelModalOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl border border-slate-200/90 shadow-2xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Xác nhận hủy đơn hàng
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Mã đơn hàng: #{cancelModalOrder.orderCode}
                </p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Bạn có chắc chắn muốn hủy đơn hàng này không? Sau khi xác nhận hủy,
              toàn bộ số lượng sản phẩm trong đơn sẽ được tự động hoàn lại vào kho
              và không thể hoàn tác hành động này.
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setCancelModalOrder(null)}
                disabled={cancelling}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                Không hủy
              </button>
              <button
                type="button"
                onClick={handleConfirmCancel}
                disabled={cancelling}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-red-600 hover:bg-red-700 active:scale-[0.98] transition-all disabled:opacity-50 cursor-pointer shadow-2xs"
              >
                {cancelling ? (
                  <>
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Đang xử lý...</span>
                  </>
                ) : (
                  <span>Xác nhận hủy đơn</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-zinc-950 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-900">Đơn hàng của tôi</span>
        </nav>

        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-3">
              <span>Lịch sử đơn hàng</span>
              {orders.length > 0 && (
                <span className="text-xs font-bold bg-zinc-100 text-zinc-800 px-3 py-1 rounded-full border border-zinc-200 tabular-nums">
                  {orders.length} đơn
                </span>
              )}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Theo dõi lộ trình, trạng thái giao nhận và quản lý các đơn hàng bạn đã đặt.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={fetchOrders}
              disabled={loading}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-zinc-950 bg-white hover:bg-slate-50 px-3.5 py-2 rounded-xl border border-slate-200 transition-colors cursor-pointer shadow-2xs"
              title="Làm mới danh sách"
            >
              <RefreshCw
                className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-zinc-900' : ''}`}
              />
              <span>Làm mới</span>
            </button>
            <Link
              to="/products"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 px-3.5 py-2 rounded-xl transition-all shadow-2xs"
            >
              <ShoppingBag className="w-3.5 h-3.5" />
              <span>Tiếp tục mua sắm</span>
            </Link>
          </div>
        </div>

        {/* Search & Tabs Filter */}
        <div className="space-y-4 mb-6">
          {/* Ô tìm kiếm */}
          <div className="relative max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo mã đơn, người nhận, tên sản phẩm..."
              className="w-full pl-10 pr-9 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 text-xs text-slate-800 bg-white"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Thanh Tabs lọc trạng thái */}
          <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
            {STATUS_TABS.map((tab) => {
              const count = tabCounts[tab.key] || 0
              const isActive = activeTab === tab.key
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                    isActive
                      ? 'bg-zinc-900 text-zinc-50 shadow-sm'
                      : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                  }`}
                >
                  <span>{tab.label}</span>
                  <span
                    className={`text-[10px] px-1.5 py-0.2 rounded-full tabular-nums font-bold ${
                      isActive
                        ? 'bg-white/20 text-white'
                        : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {count}
                  </span>
                </button>
              )
            })}
          </div>
        </div>

        {/* Content Section */}
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center bg-white rounded-2xl border border-slate-200/80 shadow-2xs">
            <LoadingSpinner size="lg" className="text-zinc-900 mb-3" />
            <p className="text-slate-500 text-sm font-medium">
              Đang tải lịch sử đơn hàng của bạn...
            </p>
          </div>
        ) : error ? (
          <div className="p-8 text-center bg-white rounded-2xl border border-red-200 shadow-2xs space-y-4">
            <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Không thể tải đơn hàng
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                {error}
              </p>
            </div>
            <button
              onClick={fetchOrders}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 transition-colors cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : orders.length === 0 ? (
          /* Empty state khi chưa có đơn hàng nào */
          <div className="py-16 px-4 text-center bg-white rounded-2xl border border-slate-200/80 shadow-2xs space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-zinc-100 text-zinc-900 flex items-center justify-center mx-auto">
              <ShoppingBag className="w-8 h-8 stroke-[1.5]" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">
                Bạn chưa có đơn hàng nào
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto leading-relaxed">
                Khám phá hàng ngàn sản phẩm công nghệ và phụ kiện tuyệt vời tại
                Rikkei Store và đặt hàng ngay hôm nay!
              </p>
            </div>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 active:scale-95 transition-all shadow-2xs"
            >
              <span>Khám phá sản phẩm ngay</span>
              <ChevronRight className="w-4 h-4" />
            </Link>
          </div>
        ) : filteredOrders.length === 0 ? (
          /* Empty state khi lọc không có kết quả */
          <div className="py-14 px-4 text-center bg-white rounded-2xl border border-slate-200/80 shadow-2xs space-y-4">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 text-slate-500 flex items-center justify-center mx-auto">
              <PackageOpen className="w-7 h-7 stroke-[1.5]" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Không tìm thấy đơn hàng phù hợp
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                {searchQuery
                  ? `Không có đơn hàng nào khớp với từ khóa "${searchQuery}".`
                  : 'Không có đơn hàng nào trong trạng thái đã chọn.'}
              </p>
            </div>
            <button
              onClick={() => {
                setActiveTab('ALL')
                setSearchQuery('')
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
            >
              <span>Xóa bộ lọc</span>
            </button>
          </div>
        ) : (
          /* Danh sách thẻ đơn hàng */
          <div className="space-y-4">
            {filteredOrders.map((order) => {
              const currentStatus = order.orderStatus || order.status
              const canCancel =
                currentStatus === 'PENDING' || currentStatus === 'CONFIRMED'

              return (
                <div
                  key={order.id}
                  className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs hover:border-slate-300 transition-all overflow-hidden"
                >
                  {/* Header thẻ đơn hàng */}
                  <div className="p-4 sm:p-5 bg-slate-50/70 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-3">
                      <div className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
                        <span className="text-slate-400">#</span>
                        <span>{order.orderCode}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-slate-500">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>{formatDateTime(order.createdAt)}</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <PaymentStatusBadge
                        status={order.paymentStatus}
                        description={order.paymentStatusDescription}
                      />
                      <OrderStatusBadge
                        status={currentStatus}
                        description={order.orderStatusDescription}
                      />
                    </div>
                  </div>

                  {/* Thông tin người nhận hàng vắn tắt */}
                  <div className="px-4 sm:px-5 py-2.5 bg-white border-b border-slate-100 text-xs text-slate-600 flex flex-wrap items-center gap-y-1 gap-x-4">
                    <div className="flex items-center gap-1.5">
                      <User className="w-3.5 h-3.5 text-slate-400" />
                      <span className="font-semibold text-slate-800">
                        {order.recipientName}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      <span>{order.recipientPhone}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-500 truncate max-w-md">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span className="truncate">{order.shippingAddress}</span>
                    </div>
                  </div>

                  {/* Danh sách các sản phẩm trong đơn */}
                  <div className="p-4 sm:p-5 divide-y divide-slate-100">
                    {Array.isArray(order.items) &&
                      order.items.map((item) => (
                        <div
                          key={item.id || item.productId}
                          className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-3"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <div className="w-12 h-12 rounded-xl bg-slate-100 shrink-0 overflow-hidden border border-slate-100">
                              <img
                                src={item.imageUrl || FALLBACK_IMAGE}
                                alt={item.productName || 'Sản phẩm'}
                                onError={(e) => {
                                  e.target.onerror = null
                                  e.target.src = FALLBACK_IMAGE
                                }}
                                className="w-full h-full object-cover"
                              />
                            </div>
                            <div className="min-w-0">
                              <Link
                                to={`/products/${item.productId}`}
                                className="text-xs font-bold text-slate-900 hover:text-zinc-950 transition-colors line-clamp-1"
                              >
                                {item.productName}
                              </Link>
                              <p className="text-[11px] text-slate-500 mt-0.5">
                                {formatPrice(item.unitPrice)} × {item.quantity}
                              </p>
                            </div>
                          </div>

                          <div className="text-xs font-bold text-slate-900 tabular-nums shrink-0">
                            {formatPrice(item.subtotal || item.unitPrice * item.quantity)}
                          </div>
                        </div>
                      ))}
                  </div>

                  {/* Footer thẻ đơn hàng: Tổng tiền & Thao tác */}
                  <div className="p-4 sm:p-5 bg-slate-50/40 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="text-xs text-slate-500">
                      <span>Phương thức: </span>
                      <span className="font-semibold text-slate-700">
                        {order.paymentMethodDescription || order.paymentMethod}
                      </span>
                    </div>

                    <div className="flex items-center justify-between sm:justify-end gap-3 sm:gap-4">
                      <div className="text-left sm:text-right">
                        <span className="text-[11px] text-slate-400 block font-medium">
                          Tổng tiền
                        </span>
                        <span className="text-base sm:text-lg font-extrabold text-zinc-900 tabular-nums">
                          {formatPrice(order.totalAmount)}
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        {canCancel && (
                          <button
                            type="button"
                            onClick={() => setCancelModalOrder(order)}
                            className="px-3.5 py-2 rounded-xl text-xs font-semibold text-red-600 hover:text-red-700 hover:bg-red-50 border border-red-200 transition-colors cursor-pointer"
                          >
                            Hủy đơn
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => navigate(`/orders/${order.id}`)}
                          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 transition-colors cursor-pointer shadow-2xs"
                        >
                          <span>Xem chi tiết</span>
                          <ChevronRight className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

export default OrderHistoryPage
