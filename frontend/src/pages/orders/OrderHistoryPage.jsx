import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import {
  Package,
  ShoppingBag,
  Clock,
  CheckCircle2,
  Truck,
  XCircle,
  AlertTriangle,
  ArrowRight,
  RotateCcw,
  PackageOpen,
} from 'lucide-react'
import orderService from '../../services/orderService'

export const OrderHistoryPage = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedFilter, setSelectedFilter] = useState('ALL')

  // State cho modal hủy đơn
  const [cancellingOrder, setCancellingOrder] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [isSubmittingCancel, setIsSubmittingCancel] = useState(false)
  const [cancelError, setCancelError] = useState(null)
  const [successToast, setSuccessToast] = useState(null)

  const fetchOrders = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await orderService.getMyOrders()
      if (res.success) {
        setOrders(res.data || [])
      } else {
        setError(res.message || 'Không thể tải danh sách đơn hàng')
      }
    } catch (err) {
      console.error('Error fetching orders:', err)
      setError('Lỗi kết nối khi tải lịch sử đơn hàng')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrders()
  }, [])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
            Chờ xác nhận
          </span>
        )
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-800 border border-blue-200">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
            Đã xác nhận
          </span>
        )
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-800 border border-indigo-200">
            <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
            Đang đóng gói
          </span>
        )
      case 'SHIPPING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-800 border border-purple-200">
            <Truck className="w-3.5 h-3.5 text-purple-600" />
            Đang giao hàng
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            Hoàn thành
          </span>
        )
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">
            <XCircle className="w-3.5 h-3.5 text-slate-400" />
            Đã hủy
          </span>
        )
      default:
        return (
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
            {status}
          </span>
        )
    }
  }

  const getPaymentBadge = (status) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-md border border-emerald-200">
            Đã thanh toán
          </span>
        )
      case 'UNPAID':
        return (
          <span className="text-[11px] font-semibold text-slate-600 bg-slate-100 px-2.5 py-0.5 rounded-md">
            Chưa thanh toán
          </span>
        )
      case 'REFUNDED':
        return (
          <span className="text-[11px] font-semibold text-purple-700 bg-purple-50 px-2.5 py-0.5 rounded-md border border-purple-200">
            Đã hoàn tiền
          </span>
        )
      default:
        return null
    }
  }

  const handleOpenCancelModal = (order) => {
    setCancellingOrder(order)
    setCancelReason('')
    setCancelError(null)
  }

  const handleConfirmCancel = async () => {
    if (!cancellingOrder) return
    setIsSubmittingCancel(true)
    setCancelError(null)

    try {
      const res = await orderService.cancelOrder(cancellingOrder.id, cancelReason)
      if (res.success) {
        setOrders((prev) =>
          prev.map((o) => (o.id === cancellingOrder.id ? res.data : o))
        )
        setSuccessToast(`Đã hủy thành công đơn hàng ${cancellingOrder.orderCode}`)
        setTimeout(() => setSuccessToast(null), 4000)
        setCancellingOrder(null)
      } else {
        setCancelError(res.message || 'Hủy đơn hàng thất bại')
      }
    } catch (err) {
      console.error('Cancel order error:', err)
      setCancelError(err.response?.data?.message || 'Có lỗi xảy ra khi hủy đơn hàng')
    } finally {
      setIsSubmittingCancel(false)
    }
  }

  const filteredOrders = orders.filter((order) => {
    if (selectedFilter === 'ALL') return true
    return order.orderStatus === selectedFilter
  })

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-orange-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-900">Đơn hàng của tôi</span>
        </nav>

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Đơn hàng của tôi
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Theo dõi lộ trình giao nhận và lịch sử mua sắm của tài khoản.
            </p>
          </div>

          <Link
            to="/products"
            className="inline-flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl shadow-2xs transition-colors self-start sm:self-auto"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>Khám phá thêm sản phẩm</span>
          </Link>
        </div>

        {/* Success Toast */}
        {successToast && (
          <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between text-sm shadow-2xs animate-fade-in">
            <div className="flex items-center gap-2.5">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              <span>{successToast}</span>
            </div>
            <button
              onClick={() => setSuccessToast(null)}
              className="text-emerald-700 hover:text-emerald-950 font-bold text-xs"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Tabs Filter */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 mb-6 scrollbar-none">
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'PENDING', label: 'Chờ xác nhận' },
            { id: 'CONFIRMED', label: 'Đã xác nhận' },
            { id: 'SHIPPING', label: 'Đang giao' },
            { id: 'COMPLETED', label: 'Hoàn thành' },
            { id: 'CANCELLED', label: 'Đã hủy' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedFilter(tab.id)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                selectedFilter === tab.id
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
              }`}
            >
              {tab.label}
              {tab.id !== 'ALL' && (
                <span className="ml-1.5 opacity-80 tabular-nums">
                  ({orders.filter((o) => o.orderStatus === tab.id).length})
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Content */}
        {loading ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center my-8 shadow-2xs">
            <div className="w-8 h-8 border-3 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-slate-500 text-xs">Đang tải lịch sử đơn hàng...</p>
          </div>
        ) : error ? (
          <div className="bg-white rounded-2xl border border-red-200 p-8 text-center my-8 shadow-2xs">
            <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
            <h3 className="text-base font-bold text-slate-900 mb-1">Không thể tải đơn hàng</h3>
            <p className="text-slate-500 text-xs mb-4">{error}</p>
            <button
              onClick={fetchOrders}
              className="px-4 py-2 bg-orange-600 text-white font-semibold text-xs rounded-xl shadow-2xs"
            >
              Thử lại
            </button>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="bg-white rounded-2xl border border-dashed border-slate-200 p-12 text-center my-8 shadow-2xs">
            <div className="w-16 h-16 bg-orange-50 text-orange-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <PackageOpen className="w-8 h-8 stroke-[1.5]" />
            </div>
            <h3 className="text-base font-bold text-slate-900 mb-1">
              {selectedFilter === 'ALL'
                ? 'Bạn chưa có đơn sắm nào'
                : 'Không có đơn hàng nào trong trạng thái này'}
            </h3>
            <p className="text-slate-500 text-xs mb-6 max-w-sm mx-auto">
              Khi bạn mua sản phẩm, các đơn hàng sẽ được hiển thị tại đây để bạn tiện theo dõi trạng thái.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-all"
            >
              <span>Mua sắm ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map((order) => {
              const canCancel =
                order.orderStatus === 'PENDING' || order.orderStatus === 'CONFIRMED'

              return (
                <div
                  key={order.id}
                  className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs hover:shadow-card transition-all overflow-hidden"
                >
                  {/* Card Header */}
                  <div className="p-4 sm:p-5 bg-slate-50/60 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-3">
                      <span className="font-extrabold text-slate-900 text-sm tracking-tight">
                        {order.orderCode}
                      </span>
                      <span className="text-slate-300">|</span>
                      <span className="text-slate-500">{formatDate(order.createdAt)}</span>
                    </div>

                    <div className="flex items-center gap-2.5">
                      {getPaymentBadge(order.paymentStatus)}
                      {getStatusBadge(order.orderStatus)}
                    </div>
                  </div>

                  {/* Card Body: Items Preview */}
                  <div className="p-4 sm:p-5 divide-y divide-slate-100">
                    {order.items?.map((item) => (
                      <div
                        key={item.id}
                        className="py-3.5 first:pt-0 last:pb-0 flex items-center justify-between gap-4"
                      >
                        <div className="flex items-center gap-3.5 min-w-0">
                          <div className="w-14 h-14 rounded-xl overflow-hidden bg-slate-100 border border-slate-200/80 flex-shrink-0">
                            {item.imageUrl ? (
                              <img
                                src={item.imageUrl}
                                alt={item.productName}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-slate-400">
                                <ShoppingBag className="w-6 h-6 stroke-1 text-slate-300" />
                              </div>
                            )}
                          </div>
                          <div className="min-w-0">
                            <h4 className="font-bold text-slate-900 text-sm line-clamp-1">
                              {item.productName}
                            </h4>
                            <div className="text-xs text-slate-500 mt-0.5 tabular-nums">
                              Số lượng: {item.quantity} × {formatPrice(item.unitPrice)}
                            </div>
                          </div>
                        </div>

                        <div className="text-right flex-shrink-0">
                          <div className="text-sm font-extrabold text-slate-900 tabular-nums">
                            {formatPrice(item.subtotal)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Card Footer: Summary & Actions */}
                  <div className="p-4 sm:p-5 bg-slate-50/30 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="flex items-baseline gap-2">
                      <span className="text-xs text-slate-500">Tổng thanh toán:</span>
                      <span className="text-lg font-black text-orange-600 tabular-nums">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-auto">
                      {canCancel && (
                        <button
                          type="button"
                          onClick={() => handleOpenCancelModal(order)}
                          className="px-3.5 py-2 text-xs font-semibold text-slate-600 hover:text-red-600 hover:bg-red-50 border border-slate-200 hover:border-red-200 rounded-xl transition-colors cursor-pointer"
                        >
                          Hủy đơn
                        </button>
                      )}

                      <Link
                        to={`/orders/${order.id}`}
                        className="px-4 py-2 text-xs font-semibold bg-white hover:bg-slate-50 text-slate-800 border border-slate-200 rounded-xl transition-colors flex items-center gap-1.5 shadow-2xs"
                      >
                        <span>Chi tiết đơn</span>
                        <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                      </Link>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* Modal xác nhận hủy đơn hàng */}
        {cancellingOrder && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 backdrop-blur-xs p-4">
            <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-dropdown border border-slate-200 animate-scale-up">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-red-50 text-red-600 flex items-center justify-center">
                  <AlertTriangle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Xác nhận hủy đơn hàng</h3>
                  <p className="text-xs text-slate-500">Mã đơn: {cancellingOrder.orderCode}</p>
                </div>
              </div>

              <p className="text-xs text-slate-600 mb-4 leading-relaxed">
                Bạn có chắc chắn muốn hủy đơn hàng này? Khi hủy, toàn bộ số lượng sản phẩm sẽ được tự động hoàn lại vào kho.
              </p>

              {cancelError && (
                <div className="mb-4 p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                  {cancelError}
                </div>
              )}

              <div className="mb-5">
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Lý do hủy đơn (Tùy chọn)
                </label>
                <textarea
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Ví dụ: Đổi ý, muốn đổi địa chỉ nhận hàng, đặt nhầm số lượng..."
                  rows="2"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  disabled={isSubmittingCancel}
                  onClick={() => setCancellingOrder(null)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  disabled={isSubmittingCancel}
                  onClick={handleConfirmCancel}
                  className="px-4 py-2 text-xs font-bold bg-red-600 hover:bg-red-700 text-white rounded-xl shadow-2xs transition-colors cursor-pointer flex items-center gap-1.5"
                >
                  {isSubmittingCancel ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Đang hủy...</span>
                    </>
                  ) : (
                    <span>Xác nhận hủy đơn</span>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default OrderHistoryPage
