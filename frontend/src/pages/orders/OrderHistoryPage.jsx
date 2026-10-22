import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
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
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
            Chờ xác nhận
          </span>
        )
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
            Đã xác nhận
          </span>
        )
      case 'PROCESSING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
            <span className="w-1.5 h-1.5 rounded-full bg-indigo-500"></span>
            Đang đóng gói
          </span>
        )
      case 'SHIPPING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-purple-100 text-purple-800 border border-purple-200">
            <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
            Đang giao hàng
          </span>
        )
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            Hoàn thành
          </span>
        )
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
            <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span>
            Đã hủy
          </span>
        )
      default:
        return (
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-100 text-slate-700">
            {status}
          </span>
        )
    }
  }

  const getPaymentBadge = (status) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
            ✓ Đã thanh toán
          </span>
        )
      case 'UNPAID':
        return (
          <span className="text-[11px] font-semibold text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
            Chưa thanh toán
          </span>
        )
      case 'REFUNDED':
        return (
          <span className="text-[11px] font-semibold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-md border border-purple-200">
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
        // Cập nhật lại đơn hàng trong state danh sách
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
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-orange-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-800">Đơn hàng của tôi</span>
        </nav>

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Đơn hàng của tôi
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Theo dõi và quản lý lịch sử đơn hàng của bạn.
            </p>
          </div>

          <Link
            to="/products"
            className="inline-flex items-center gap-2 px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors self-start sm:self-auto"
          >
            <span>🛍️</span>
            <span>Khám phá thêm sản phẩm</span>
          </Link>
        </div>

        {/* Success Toast */}
        {successToast && (
          <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between text-sm shadow-sm animate-fade-in">
            <div className="flex items-center gap-2.5">
              <span>✅</span>
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
              className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                selectedFilter === tab.id
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
              }`}
            >
              {tab.label}
              {tab.id !== 'ALL' && (
                <span className="ml-1.5 opacity-70">
                  ({orders.filter((o) => o.orderStatus === tab.id).length})
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Content */}
        {loading ? (
          <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center my-8 shadow-sm">
            <div className="w-10 h-10 border-4 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-slate-500 text-sm">Đang tải danh sách đơn hàng...</p>
          </div>
        ) : error ? (
          <div className="bg-white rounded-3xl border border-red-200 p-10 text-center my-8 shadow-sm">
            <div className="text-4xl mb-3">⚠️</div>
            <h3 className="text-base font-bold text-slate-800 mb-1">Không thể tải đơn hàng</h3>
            <p className="text-slate-500 text-sm mb-4">{error}</p>
            <button
              onClick={fetchOrders}
              className="px-4 py-2 bg-orange-600 text-white font-bold text-xs rounded-xl shadow-xs"
            >
              Thử lại
            </button>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-slate-300 p-12 text-center my-8 shadow-sm">
            <div className="w-16 h-16 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center text-3xl mx-auto mb-3">
              📦
            </div>
            <h3 className="text-lg font-bold text-slate-800 mb-1">
              {selectedFilter === 'ALL'
                ? 'Bạn chưa có đơn hàng nào'
                : 'Không có đơn hàng nào trong mục này'}
            </h3>
            <p className="text-slate-500 text-sm mb-6 max-w-sm mx-auto">
              Khi bạn mua sản phẩm, các đơn hàng sẽ được hiển thị tại đây để bạn tiện theo dõi trạng thái.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-sm rounded-xl shadow-md shadow-orange-600/20 transition-all"
            >
              <span>Mua sắm ngay</span>
              <span>&rarr;</span>
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
                  className="bg-white rounded-3xl border border-slate-200 shadow-xs hover:shadow-md transition-shadow overflow-hidden"
                >
                  {/* Card Header */}
                  <div className="p-4 sm:p-5 bg-slate-50/70 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-3">
                      <span className="font-black text-slate-900 text-sm tracking-tight">
                        {order.orderCode}
                      </span>
                      <span className="text-slate-400">|</span>
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
                        className="py-3 first:pt-0 last:pb-0 flex items-center justify-between gap-4"
                      >
                        <div className="flex items-center gap-3.5 min-w-0">
                          <div className="w-14 h-14 rounded-xl overflow-hidden bg-slate-100 border border-slate-200 flex-shrink-0">
                            {item.imageUrl ? (
                              <img
                                src={item.imageUrl}
                                alt={item.productName}
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              <div className="w-full h-full flex items-center justify-center text-slate-400 text-lg">
                                🛍️
                              </div>
                            )}
                          </div>
                          <div className="min-w-0">
                            <h4 className="font-bold text-slate-800 text-sm line-clamp-1">
                              {item.productName}
                            </h4>
                            <div className="text-xs text-slate-500 mt-0.5">
                              Số lượng: {item.quantity} × {formatPrice(item.unitPrice)}
                            </div>
                          </div>
                        </div>

                        <div className="text-right flex-shrink-0">
                          <div className="text-sm font-bold text-slate-900">
                            {formatPrice(item.subtotal)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Card Footer: Summary & Actions */}
                  <div className="p-4 sm:p-5 bg-slate-50/40 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="flex items-baseline gap-2">
                      <span className="text-xs text-slate-500">Tổng thanh toán:</span>
                      <span className="text-lg font-black text-orange-600">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-auto">
                      {canCancel && (
                        <button
                          type="button"
                          onClick={() => handleOpenCancelModal(order)}
                          className="px-3.5 py-2 text-xs font-bold text-slate-600 hover:text-red-600 hover:bg-red-50 border border-slate-200 hover:border-red-200 rounded-xl transition-colors cursor-pointer"
                        >
                          Hủy đơn hàng
                        </button>
                      )}

                      <Link
                        to={`/orders/${order.id}`}
                        className="px-4 py-2 text-xs font-bold bg-white hover:bg-slate-50 text-slate-800 border border-slate-300 rounded-xl transition-colors flex items-center gap-1.5 shadow-2xs"
                      >
                        <span>Chi tiết đơn</span>
                        <span>&rarr;</span>
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
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
            <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-scale-up">
              <div className="flex items-center gap-3 mb-4">
                <span className="w-10 h-10 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-xl">
                  ⚠️
                </span>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Xác nhận hủy đơn hàng</h3>
                  <p className="text-xs text-slate-500">Mã đơn: {cancellingOrder.orderCode}</p>
                </div>
              </div>

              <p className="text-sm text-slate-600 mb-4">
                Bạn có chắc chắn muốn hủy đơn hàng này? Khi hủy, số lượng sản phẩm sẽ được tự động hoàn lại vào kho.
              </p>

              {cancelError && (
                <div className="mb-4 p-3 bg-red-50 text-red-700 text-xs rounded-xl border border-red-200">
                  {cancelError}
                </div>
              )}

              <div className="mb-5">
                <label className="block text-xs font-bold text-slate-700 mb-1.5">
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
                  className="px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  disabled={isSubmittingCancel}
                  onClick={handleConfirmCancel}
                  className="px-4 py-2 text-xs font-bold bg-red-600 hover:bg-red-700 text-white rounded-xl shadow-xs transition-colors cursor-pointer flex items-center gap-1.5"
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
