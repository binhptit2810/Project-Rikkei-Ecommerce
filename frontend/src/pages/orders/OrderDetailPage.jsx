import React, { useState, useEffect } from 'react'
import { useParams, Link, useSearchParams, useNavigate } from 'react-router-dom'
import orderService from '../../services/orderService'

export const OrderDetailPage = () => {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
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
            Đã hoàn thành
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

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 py-12 px-4 flex items-center justify-center">
        <div className="text-center">
          <div className="w-10 h-10 border-4 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <p className="text-slate-500 text-sm">Đang tải thông tin chi tiết đơn hàng...</p>
        </div>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-slate-50 py-12 px-4">
        <div className="max-w-xl mx-auto bg-white rounded-3xl border border-red-200 p-10 text-center shadow-sm">
          <div className="text-4xl mb-3">⚠️</div>
          <h2 className="text-lg font-bold text-slate-900 mb-2">Không tìm thấy đơn hàng</h2>
          <p className="text-slate-500 text-sm mb-6">{error || 'Đơn hàng không tồn tại hoặc bạn không có quyền xem.'}</p>
          <Link
            to="/orders"
            className="inline-flex items-center gap-2 px-6 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-sm rounded-xl shadow-xs"
          >
            <span>&larr;</span> Quay lại danh sách đơn
          </Link>
        </div>
      </div>
    )
  }

  const canCancel = order.orderStatus === 'PENDING' || order.orderStatus === 'CONFIRMED'

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
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
          <span className="font-semibold text-slate-800">{order.orderCode}</span>
        </nav>

        {/* Celebration Banner khi vừa tạo đơn */}
        {isNewlyCreated && (
          <div className="p-6 bg-gradient-to-r from-emerald-500 to-teal-600 rounded-3xl text-white shadow-lg shadow-emerald-500/20 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-4 text-center sm:text-left">
              <span className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-xs flex items-center justify-center text-3xl flex-shrink-0">
                🎉
              </span>
              <div>
                <h2 className="text-xl font-black">Đặt hàng thành công!</h2>
                <p className="text-sm text-emerald-50 mt-0.5">
                  Cảm ơn bạn đã tin tưởng mua sắm. Đơn hàng của bạn đang được xử lý nhanh chóng.
                </p>
              </div>
            </div>

            <Link
              to="/products"
              className="px-5 py-2.5 bg-white text-emerald-800 font-bold text-xs rounded-xl shadow-xs hover:bg-emerald-50 transition-colors flex-shrink-0"
            >
              Tiếp tục mua sắm &rarr;
            </Link>
          </div>
        )}

        {/* Success Toast */}
        {successToast && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between text-sm shadow-sm">
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

        {/* Header Thông tin chung */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-7 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                  Đơn hàng {order.orderCode}
                </h1>
                {getStatusBadge(order.orderStatus)}
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Thời gian đặt: <span className="font-semibold text-slate-700">{formatDate(order.createdAt)}</span>
              </p>
            </div>

            <div className="flex items-center gap-3">
              {canCancel && (
                <button
                  type="button"
                  onClick={() => setShowCancelModal(true)}
                  className="px-4 py-2.5 text-xs font-bold text-red-600 hover:bg-red-50 border border-red-200 rounded-xl transition-colors cursor-pointer"
                >
                  Hủy đơn hàng này
                </button>
              )}
            </div>
          </div>

          {/* Grid thông tin người nhận & thanh toán */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-6 text-sm">
            {/* Thông tin giao hàng */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
                <span>📍</span>
                <span>Thông tin nhận hàng</span>
              </div>
              <div className="bg-slate-50 rounded-2xl p-4 space-y-1.5 border border-slate-100">
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

            {/* Thông tin thanh toán */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
                <span>💳</span>
                <span>Thanh toán</span>
              </div>
              <div className="bg-slate-50 rounded-2xl p-4 space-y-2 border border-slate-100">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-500">Phương thức:</span>
                  <span className="font-bold text-slate-800">{order.paymentMethodDescription || order.paymentMethod}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-500">Trạng thái:</span>
                  <span className="font-bold text-slate-800">
                    {order.paymentStatus === 'PAID' ? (
                      <span className="text-emerald-600">✓ Đã thanh toán</span>
                    ) : order.paymentStatus === 'REFUNDED' ? (
                      <span className="text-purple-600">Đã hoàn tiền</span>
                    ) : (
                      <span className="text-amber-600">Chưa thanh toán</span>
                    )}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-200/60">
                  <span className="text-slate-500">Tổng thanh toán:</span>
                  <span className="text-base font-black text-orange-600">{formatPrice(order.totalAmount)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Danh sách sản phẩm trong đơn */}
        <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="p-5 bg-slate-50/70 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-base font-bold text-slate-900">Chi tiết sản phẩm ({order.items?.length || 0})</h2>
          </div>

          <div className="p-5 divide-y divide-slate-100">
            {order.items?.map((item) => (
              <div key={item.id} className="py-4 first:pt-0 last:pb-0 flex items-center justify-between gap-4">
                <div className="flex items-center gap-4 min-w-0 flex-1">
                  <Link
                    to={`/products/${item.productId}`}
                    className="w-16 h-16 rounded-xl overflow-hidden bg-slate-100 border border-slate-200 flex-shrink-0"
                  >
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.productName}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-slate-400 text-xl">
                        🛍️
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
                    <div className="text-xs text-slate-500 mt-1">
                      {formatPrice(item.unitPrice)} × {item.quantity}
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-sm font-black text-slate-900">
                    {formatPrice(item.subtotal)}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Tổng tiền tóm tắt footer */}
          <div className="p-5 bg-slate-50/50 border-t border-slate-100 flex flex-col items-end space-y-2 text-sm">
            <div className="w-full sm:w-72 space-y-2">
              <div className="flex justify-between text-slate-600 text-xs">
                <span>Tạm tính</span>
                <span className="font-semibold text-slate-800">{formatPrice(order.totalAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-600 text-xs">
                <span>Phí vận chuyển</span>
                <span className="font-semibold text-emerald-600">Miễn phí</span>
              </div>
              <div className="pt-2 border-t border-slate-200 flex justify-between items-baseline">
                <span className="font-bold text-slate-900">Tổng thanh toán:</span>
                <span className="text-xl font-black text-orange-600">{formatPrice(order.totalAmount)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Actions */}
        <div className="flex justify-between items-center pt-2">
          <Link
            to="/orders"
            className="text-xs font-bold text-slate-600 hover:text-orange-600 inline-flex items-center gap-1.5"
          >
            <span>&larr;</span> Quay lại danh sách đơn hàng
          </Link>
          <Link
            to="/products"
            className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors"
          >
            Tiếp tục mua hàng
          </Link>
        </div>

        {/* Modal xác nhận hủy đơn hàng */}
        {showCancelModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
            <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-scale-up">
              <div className="flex items-center gap-3 mb-4">
                <span className="w-10 h-10 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-xl">
                  ⚠️
                </span>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Xác nhận hủy đơn hàng</h3>
                  <p className="text-xs text-slate-500">Mã đơn: {order.orderCode}</p>
                </div>
              </div>

              <p className="text-sm text-slate-600 mb-4">
                Bạn có chắc chắn muốn hủy đơn hàng này? Toàn bộ số lượng sản phẩm sẽ được tự động hoàn lại vào kho.
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
                  onClick={() => setShowCancelModal(false)}
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

export default OrderDetailPage
