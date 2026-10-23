import React, { useState, useEffect } from 'react'
import { useParams, Link, useSearchParams } from 'react-router-dom'
import {
  Package,
  CheckCircle2,
  Clock,
  Truck,
  MapPin,
  CreditCard,
  AlertTriangle,
  ArrowLeft,
  Sparkles,
  ShoppingBag,
  XCircle,
  FileText,
} from 'lucide-react'
import orderService from '../../services/orderService'

export const OrderDetailPage = () => {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const isNewlyCreated = searchParams.get('success') === 'true'

  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // State cho modal hủy đơn
  const [showCancelModal, setShowCancelModal] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [isSubmittingCancel, setIsSubmittingCancel] = useState(false)
  const [cancelError, setCancelError] = useState(null)
  const [successToast, setSuccessToast] = useState(null)

  const fetchOrderDetail = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await orderService.getOrderById(id)
      if (res.success) {
        setOrder(res.data)
      } else {
        setError(res.message || 'Không tìm thấy thông tin đơn hàng')
      }
    } catch (err) {
      console.error('Error fetching order detail:', err)
      setError(err.response?.data?.message || 'Lỗi khi tải thông tin chi tiết đơn hàng')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrderDetail()
  }, [id])

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
            Đã hoàn thành
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

  const handleConfirmCancel = async () => {
    setIsSubmittingCancel(true)
    setCancelError(null)

    try {
      const res = await orderService.cancelOrder(order.id, cancelReason)
      if (res.success) {
        setOrder(res.data)
        setSuccessToast(`Đã hủy thành công đơn hàng ${order.orderCode}`)
        setShowCancelModal(false)
        setTimeout(() => setSuccessToast(null), 4000)
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

  // Steps definition for Order Stepper
  const orderSteps = [
    { key: 'PENDING', label: 'Đặt hàng' },
    { key: 'CONFIRMED', label: 'Đã xác nhận' },
    { key: 'PROCESSING', label: 'Đóng gói' },
    { key: 'SHIPPING', label: 'Đang giao' },
    { key: 'COMPLETED', label: 'Hoàn thành' },
  ]

  const getStepIndex = (status) => {
    switch (status) {
      case 'PENDING':
        return 0
      case 'CONFIRMED':
        return 1
      case 'PROCESSING':
        return 2
      case 'SHIPPING':
        return 3
      case 'COMPLETED':
        return 4
      default:
        return -1
    }
  }

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-12 px-4 flex items-center justify-center">
        <div className="text-center">
          <div className="w-8 h-8 border-3 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <p className="text-slate-500 text-xs">Đang tải chi tiết đơn hàng...</p>
        </div>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-12 px-4">
        <div className="max-w-xl mx-auto bg-white rounded-2xl border border-red-200 p-8 text-center shadow-2xs">
          <AlertTriangle className="w-8 h-8 text-red-500 mx-auto mb-2" />
          <h2 className="text-base font-bold text-slate-900 mb-2">Không tìm thấy đơn hàng</h2>
          <p className="text-slate-500 text-xs mb-6">{error || 'Đơn hàng không tồn tại hoặc bạn không có quyền xem.'}</p>
          <Link
            to="/orders"
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-semibold text-xs rounded-xl shadow-2xs"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại danh sách đơn hàng</span>
          </Link>
        </div>
      </div>
    )
  }

  const canCancel = order.orderStatus === 'PENDING' || order.orderStatus === 'CONFIRMED'
  const currentStep = getStepIndex(order.orderStatus)
  const isCancelled = order.orderStatus === 'CANCELLED'

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500">
          <Link to="/" className="hover:text-orange-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/orders" className="hover:text-orange-600 transition-colors">
            Đơn hàng của tôi
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-900">{order.orderCode}</span>
        </nav>

        {/* Celebration Banner for New Orders */}
        {isNewlyCreated && (
          <div className="p-6 bg-slate-900 text-white rounded-2xl shadow-card flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-4 text-center sm:text-left">
              <div className="w-12 h-12 rounded-xl bg-orange-600/20 text-orange-400 flex items-center justify-center shrink-0 border border-orange-500/30">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <h2 className="text-lg font-bold">Đặt hàng thành công!</h2>
                <p className="text-xs text-slate-300 mt-0.5">
                  Cảm ơn bạn đã tin tưởng mua sắm. Đơn hàng của bạn đang được hệ thống xử lý nhanh chóng.
                </p>
              </div>
            </div>

            <Link
              to="/products"
              className="px-4 py-2 bg-white text-slate-900 font-bold text-xs rounded-xl shadow-2xs hover:bg-slate-100 transition-colors shrink-0"
            >
              Tiếp tục mua hàng &rarr;
            </Link>
          </div>
        )}

        {/* Success Toast */}
        {successToast && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between text-sm shadow-2xs">
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

        {/* Order Stepper Progress Tracker */}
        <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-2xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight">
                  Đơn hàng {order.orderCode}
                </h1>
                {getStatusBadge(order.orderStatus)}
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Thời gian đặt: <span className="font-semibold text-slate-800">{formatDate(order.createdAt)}</span>
              </p>
            </div>

            <div className="flex items-center gap-3">
              {canCancel && (
                <button
                  type="button"
                  onClick={() => setShowCancelModal(true)}
                  className="px-3.5 py-2 text-xs font-semibold text-red-600 hover:bg-red-50 border border-red-200 rounded-xl transition-colors cursor-pointer"
                >
                  Hủy đơn hàng này
                </button>
              )}
            </div>
          </div>

          {/* Stepper UI */}
          {!isCancelled ? (
            <div className="pt-6 pb-2">
              <div className="relative flex items-center justify-between max-w-3xl mx-auto px-4">
                {/* Connecting Bar */}
                <div className="absolute left-8 right-8 top-1/2 -translate-y-1/2 h-0.5 bg-slate-200 -z-0">
                  <div
                    className="h-full bg-orange-600 transition-all duration-500"
                    style={{
                      width: `${(Math.max(0, currentStep) / (orderSteps.length - 1)) * 100}%`,
                    }}
                  ></div>
                </div>

                {orderSteps.map((step, idx) => {
                  const isDone = idx <= currentStep
                  const isCurrent = idx === currentStep

                  return (
                    <div key={step.key} className="flex flex-col items-center relative z-10">
                      <div
                        className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                          isDone
                            ? 'bg-orange-600 text-white shadow-2xs ring-4 ring-orange-50'
                            : 'bg-white text-slate-400 border-2 border-slate-200'
                        }`}
                      >
                        {isDone ? <CheckCircle2 className="w-4 h-4" /> : idx + 1}
                      </div>
                      <span
                        className={`text-[11px] font-semibold mt-2 text-center whitespace-nowrap ${
                          isCurrent
                            ? 'text-orange-600 font-bold'
                            : isDone
                            ? 'text-slate-800'
                            : 'text-slate-400'
                        }`}
                      >
                        {step.label}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : (
            <div className="pt-4 pb-2">
              <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-600 flex items-center gap-3">
                <XCircle className="w-5 h-5 text-slate-400 shrink-0" />
                <span>
                  Đơn hàng này đã bị hủy. Toàn bộ số lượng sản phẩm liên quan đã được tự động hoàn lại vào kho hàng.
                </span>
              </div>
            </div>
          )}
        </div>

        {/* 2-Column Info Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
          {/* Recipient Details */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 shadow-2xs space-y-3">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
              <MapPin className="w-3.5 h-3.5 text-orange-600" />
              <span>Thông tin nhận hàng</span>
            </div>
            <div className="bg-slate-50/70 rounded-xl p-4 space-y-1.5 border border-slate-100">
              <div className="font-bold text-slate-900">{order.recipientName}</div>
              <div className="text-slate-600 text-xs">Điện thoại: {order.recipientPhone}</div>
              <div className="text-slate-600 text-xs">Địa chỉ: {order.shippingAddress}</div>
              {order.note && (
                <div className="pt-2 text-xs text-slate-500 italic border-t border-slate-200/60 mt-2">
                  Ghi chú: {order.note}
                </div>
              )}
            </div>
          </div>

          {/* Payment Details */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 shadow-2xs space-y-3">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
              <CreditCard className="w-3.5 h-3.5 text-orange-600" />
              <span>Phương thức thanh toán</span>
            </div>
            <div className="bg-slate-50/70 rounded-xl p-4 space-y-2 border border-slate-100">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500">Hình thức:</span>
                <span className="font-bold text-slate-800">{order.paymentMethodDescription || order.paymentMethod}</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500">Trạng thái:</span>
                <span className="font-bold text-slate-800">
                  {order.paymentStatus === 'PAID' ? (
                    <span className="text-emerald-600">Đã thanh toán</span>
                  ) : order.paymentStatus === 'REFUNDED' ? (
                    <span className="text-purple-600">Đã hoàn tiền</span>
                  ) : (
                    <span className="text-amber-600">Chưa thanh toán</span>
                  )}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-200/60">
                <span className="text-slate-500">Tổng thanh toán:</span>
                <span className="text-base font-extrabold text-orange-600 tabular-nums">
                  {formatPrice(order.totalAmount)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Product Items Table */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
          <div className="p-5 bg-slate-50/60 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-900">Chi tiết sản phẩm ({order.items?.length || 0})</h2>
          </div>

          <div className="p-5 divide-y divide-slate-100">
            {order.items?.map((item) => (
              <div key={item.id} className="py-3.5 first:pt-0 last:pb-0 flex items-center justify-between gap-4">
                <div className="flex items-center gap-4 min-w-0 flex-1">
                  <Link
                    to={`/products/${item.productId}`}
                    className="w-16 h-16 rounded-xl overflow-hidden bg-slate-100 border border-slate-200/80 flex-shrink-0"
                  >
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
                  </Link>
                  <div className="min-w-0 flex-1">
                    <Link
                      to={`/products/${item.productId}`}
                      className="font-bold text-slate-900 text-sm hover:text-orange-600 transition-colors line-clamp-1"
                    >
                      {item.productName}
                    </Link>
                    <div className="text-xs text-slate-500 mt-0.5 tabular-nums">
                      {formatPrice(item.unitPrice)} × {item.quantity}
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-sm font-extrabold text-slate-900 tabular-nums">
                    {formatPrice(item.subtotal)}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Pricing Totals */}
          <div className="p-5 bg-slate-50/40 border-t border-slate-100 flex flex-col items-end space-y-2 text-sm">
            <div className="w-full sm:w-72 space-y-2">
              <div className="flex justify-between text-slate-600 text-xs">
                <span>Tạm tính</span>
                <span className="font-semibold text-slate-900 tabular-nums">{formatPrice(order.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-600 text-xs">
                <span>Phí vận chuyển</span>
                <span className="font-semibold text-emerald-600">Miễn phí</span>
              </div>
              <div className="pt-2 border-t border-slate-200 flex justify-between items-baseline">
                <span className="font-bold text-slate-900">Tổng thanh toán:</span>
                <span className="text-xl font-black text-orange-600 tabular-nums">{formatPrice(order.totalAmount)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Actions */}
        <div className="flex justify-between items-center pt-2">
          <Link
            to="/orders"
            className="text-xs font-semibold text-slate-600 hover:text-orange-600 inline-flex items-center gap-1.5 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Quay lại danh sách đơn hàng</span>
          </Link>
          <Link
            to="/products"
            className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-2xs transition-colors"
          >
            Tiếp tục mua hàng
          </Link>
        </div>

        {/* Modal Cancel Order */}
        {showCancelModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 backdrop-blur-xs p-4">
            <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-dropdown border border-slate-200 animate-scale-up">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-red-50 text-red-600 flex items-center justify-center">
                  <AlertTriangle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Xác nhận hủy đơn hàng</h3>
                  <p className="text-xs text-slate-500">Mã đơn: {order.orderCode}</p>
                </div>
              </div>

              <p className="text-xs text-slate-600 mb-4 leading-relaxed">
                Bạn có chắc chắn muốn hủy đơn hàng này? Toàn bộ số lượng sản phẩm sẽ được tự động hoàn lại vào kho hàng.
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
                  onClick={() => setShowCancelModal(false)}
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

export default OrderDetailPage
