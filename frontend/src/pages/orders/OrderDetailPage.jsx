import React, { useState, useEffect, useCallback } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Copy,
  Check,
  Calendar,
  User,
  Phone,
  MapPin,
  FileText,
  CreditCard,
  Truck,
  RotateCcw,
  CheckCircle2,
  XCircle,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  Clock,
  Package,
  X,
  HelpCircle,
} from 'lucide-react'
import orderService from '../../services/orderService'
import paymentService from '../../services/paymentService'
import { useCartStore } from '../../store/useCartStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import {
  OrderStatusBadge,
  PaymentStatusBadge,
  formatPrice,
  formatDateTime,
  FALLBACK_IMAGE,
} from './OrderBadges'

// 5 mốc tiến trình chuẩn của đơn hàng
const PROGRESS_STEPS = [
  {
    key: 'PENDING',
    label: 'Đặt hàng thành công',
    desc: 'Chờ hệ thống duyệt',
    icon: Clock,
  },
  {
    key: 'CONFIRMED',
    label: 'Đã xác nhận',
    desc: 'Đơn hàng hợp lệ',
    icon: CheckCircle2,
  },
  {
    key: 'PROCESSING',
    label: 'Đang đóng gói',
    desc: 'Chuẩn bị sản phẩm',
    icon: Package,
  },
  {
    key: 'SHIPPING',
    label: 'Đang giao hàng',
    desc: 'Bàn giao vận chuyển',
    icon: Truck,
  },
  {
    key: 'COMPLETED',
    label: 'Hoàn thành',
    desc: 'Giao hàng thành công',
    icon: CheckCircle2,
  },
]

const STATUS_TO_STEP_INDEX = {
  PENDING: 0,
  CONFIRMED: 1,
  PROCESSING: 2,
  SHIPPING: 3,
  COMPLETED: 4,
}

// Các lý do hủy đơn hàng gợi ý sẵn
const PRESET_CANCEL_REASONS = [
  'Thay đổi địa chỉ nhận hàng',
  'Muốn thay đổi sản phẩm / số lượng',
  'Tìm thấy giá tốt hơn ở nơi khác',
  'Thay đổi phương thức thanh toán',
  'Đổi ý, không còn nhu cầu mua nữa',
  'Lý do khác',
]

export const OrderDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const addToCart = useCartStore((state) => state.addToCart)

  const [order, setOrder] = useState(null)
  const [paymentInfo, setPaymentInfo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Copy mã đơn
  const [copiedCode, setCopiedCode] = useState(false)

  // Cancel order modal & reason states
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [cancelReasonPreset, setCancelReasonPreset] = useState('')
  const [cancelReasonText, setCancelReasonText] = useState('')
  const [cancelReasonError, setCancelReasonError] = useState('')
  const [cancelling, setCancelling] = useState(false)

  // Reorder loading
  const [reordering, setReordering] = useState(false)

  // Toast
  const [toast, setToast] = useState(null)

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 4000)
  }

  // Tải chi tiết đơn hàng và tra cứu thanh toán tương ứng
  const fetchOrderDetail = useCallback(async () => {
    if (!id) return
    setLoading(true)
    setError(null)

    try {
      const res = await orderService.getOrderById(id)
      const orderData = res?.data || res
      setOrder(orderData)

      // Tra cứu thêm chi tiết thanh toán giao dịch nếu có
      try {
        const payRes = await paymentService.getPaymentByOrderId(id)
        if (payRes?.data || payRes) {
          setPaymentInfo(payRes.data || payRes)
        }
      } catch {
        // Tra cứu thanh toán thất bại (chưa tạo bản ghi hoặc đơn COD chờ thu) không làm crash trang chi tiết
        setPaymentInfo(null)
      }
    } catch (err) {
      if (err.response?.status === 401) {
        showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'error')
        navigate('/login')
        return
      }
      if (err.response?.status === 403) {
        setError('Bạn không có quyền truy cập thông tin đơn hàng này.')
        return
      }
      if (err.response?.status === 404) {
        setError('Đơn hàng không tồn tại hoặc đã bị xóa.')
        return
      }
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không tìm thấy thông tin đơn hàng hoặc bạn không có quyền xem đơn hàng này.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [id, navigate])

  useEffect(() => {
    fetchOrderDetail()
  }, [fetchOrderDetail])

  // Copy mã đơn hàng vào clipboard
  const handleCopyOrderCode = () => {
    if (!order?.orderCode) return
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(order.orderCode)
      setCopiedCode(true)
      setTimeout(() => setCopiedCode(false), 2000)
    }
  }

  // Mở modal hủy đơn và reset form
  const handleOpenCancelModal = () => {
    setCancelReasonPreset('')
    setCancelReasonText('')
    setCancelReasonError('')
    setCancelModalOpen(true)
  }

  // Chọn lý do gợi ý
  const handleSelectPresetReason = (reason) => {
    setCancelReasonPreset(reason)
    setCancelReasonError('')
    if (reason === 'Lý do khác') {
      if (PRESET_CANCEL_REASONS.slice(0, -1).includes(cancelReasonText)) {
        setCancelReasonText('')
      }
    } else {
      setCancelReasonText(reason)
    }
  }

  // Xử lý hủy đơn hàng kèm gửi lý do lên backend
  const handleConfirmCancel = async () => {
    if (!order?.id) return

    const finalReason = (cancelReasonText || cancelReasonPreset || '').trim()
    if (!finalReason) {
      setCancelReasonError('Vui lòng chọn hoặc nhập lý do hủy đơn hàng.')
      return
    }

    setCancelling(true)
    setCancelReasonError('')

    try {
      const res = await orderService.cancelOrder(order.id, finalReason)
      const updatedOrder = res?.data || res

      setOrder((prev) => ({
        ...prev,
        ...updatedOrder,
        orderStatus: updatedOrder.orderStatus || 'CANCELLED',
        orderStatusDescription:
          updatedOrder.orderStatusDescription || 'Đã hủy',
        paymentStatus: updatedOrder.paymentStatus || prev?.paymentStatus,
        paymentStatusDescription:
          updatedOrder.paymentStatusDescription || prev?.paymentStatusDescription,
        note:
          updatedOrder.note ||
          (prev?.note
            ? `${prev.note} [Lý do hủy: ${finalReason}]`
            : `Lý do hủy: ${finalReason}`),
        updatedAt: updatedOrder.updatedAt || new Date().toISOString(),
      }))

      showToast(
        `Đã hủy đơn hàng #${order.orderCode} thành công! Số lượng sản phẩm đã được tự động hoàn lại vào kho.`,
        'success'
      )
      setCancelModalOpen(false)
    } catch (err) {
      if (err.response?.status === 409) {
        showToast(
          err.response?.data?.message ||
            'Không thể hủy đơn hàng do trạng thái đơn hàng đã thay đổi.',
          'error'
        )
        fetchOrderDetail()
        setCancelModalOpen(false)
      } else if (err.response?.status === 403) {
        showToast('Bạn không có quyền hủy đơn hàng này.', 'error')
      } else if (err.response?.status === 404) {
        showToast('Đơn hàng không tồn tại.', 'error')
      } else if (err.response?.status === 401) {
        showToast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 'error')
        navigate('/login')
      } else {
        const msg =
          err.response?.data?.message ||
          err.message ||
          'Không thể hủy đơn hàng. Vui lòng thử lại.'
        showToast(msg, 'error')
      }
    } finally {
      setCancelling(false)
    }
  }

  // Xử lý mua lại (thêm toàn bộ sản phẩm trong đơn vào giỏ hàng rồi chuyển sang /cart)
  const handleBuyAgain = async () => {
    if (!order?.items || order.items.length === 0) return
    setReordering(true)
    try {
      for (const item of order.items) {
        await addToCart(
          {
            id: item.productId,
            name: item.productName,
            price: item.unitPrice,
            imageUrl: item.imageUrl,
          },
          item.quantity || 1
        )
      }
      navigate('/cart')
    } catch (err) {
      const msg =
        err.message || 'Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.'
      showToast(msg, 'error')
    } finally {
      setReordering(false)
    }
  }

  const currentStatus = order?.orderStatus || order?.status
  const isCancelled = currentStatus === 'CANCELLED'
  // Nút hủy chỉ hiển thị khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED
  const canCancel = currentStatus === 'PENDING' || currentStatus === 'CONFIRMED'
  const currentStepIndex =
    STATUS_TO_STEP_INDEX[currentStatus] !== undefined
      ? STATUS_TO_STEP_INDEX[currentStatus]
      : -1

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
              className="text-slate-400 hover:text-slate-600 p-0.5 cursor-pointer transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Cancel Order Modal (Tailwind CSS, Backdrop blur, Animation mượt mà) */}
      {cancelModalOpen && order && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-md transition-all duration-300 animate-in fade-in">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-2xl max-w-lg w-full p-6 sm:p-7 space-y-5 animate-in zoom-in-95 duration-200">
            {/* Header Modal */}
            <div className="flex items-start justify-between gap-3 pb-3 border-b border-slate-100">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-100 shadow-2xs">
                  <AlertTriangle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-slate-900">
                    Xác nhận hủy đơn hàng
                  </h3>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">
                    Mã đơn: #{order.orderCode}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setCancelModalOpen(false)}
                disabled={cancelling}
                className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Warning notice */}
            <div className="p-3.5 rounded-2xl bg-amber-50/80 border border-amber-200/80 text-xs text-amber-900 flex items-start gap-2.5 leading-relaxed">
              <HelpCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <span>
                Sau khi xác nhận hủy, đơn hàng sẽ chuyển sang trạng thái <strong>Đã hủy</strong>.
                Toàn bộ số lượng sản phẩm sẽ được <strong>tự động hoàn lại vào kho</strong>.
              </span>
            </div>

            {/* Lý do hủy: Chọn nhanh hoặc Tự nhập */}
            <div className="space-y-3">
              <label className="block text-xs font-bold text-slate-800">
                Lý do hủy đơn hàng <span className="text-red-500">*</span>
              </label>

              {/* Chips chọn nhanh */}
              <div className="flex flex-wrap gap-2">
                {PRESET_CANCEL_REASONS.map((reason) => {
                  const isSelected =
                    cancelReasonPreset === reason || cancelReasonText === reason
                  return (
                    <button
                      key={reason}
                      type="button"
                      onClick={() => handleSelectPresetReason(reason)}
                      className={`text-xs px-3 py-1.5 rounded-xl border transition-all text-left cursor-pointer ${
                        isSelected
                          ? 'bg-zinc-900 text-white border-zinc-900 shadow-2xs font-semibold'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      {reason}
                    </button>
                  )
                })}
              </div>

              {/* Textarea nhập chi tiết */}
              <div className="relative">
                <textarea
                  rows={3}
                  value={cancelReasonText}
                  onChange={(e) => {
                    setCancelReasonText(e.target.value)
                    if (cancelReasonError) setCancelReasonError('')
                  }}
                  placeholder="Vui lòng cung cấp thêm chi tiết lý do hủy đơn của bạn..."
                  className="w-full text-xs p-3 rounded-2xl border border-slate-200 bg-slate-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-900 focus:border-transparent transition-all placeholder:text-slate-400 resize-none"
                />
              </div>

              {cancelReasonError && (
                <p className="text-[11px] font-semibold text-red-600 flex items-center gap-1.5">
                  <AlertCircle className="w-3.5 h-3.5" />
                  <span>{cancelReasonError}</span>
                </p>
              )}
            </div>

            {/* Footer Buttons */}
            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setCancelModalOpen(false)}
                disabled={cancelling}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer"
              >
                Giữ lại đơn hàng
              </button>
              <button
                type="button"
                onClick={handleConfirmCancel}
                disabled={cancelling}
                className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 active:scale-[0.98] transition-all disabled:opacity-50 cursor-pointer shadow-sm shadow-red-200"
              >
                {cancelling ? (
                  <>
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Đang xử lý hủy...</span>
                  </>
                ) : (
                  <>
                    <XCircle className="w-4 h-4" />
                    <span>Xác nhận hủy đơn</span>
                  </>
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
          <Link to="/orders" className="hover:text-zinc-950 transition-colors">
            Đơn hàng của tôi
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-900">
            {order ? `#${order.orderCode}` : 'Chi tiết đơn hàng'}
          </span>
        </nav>

        {loading ? (
          <div className="py-24 flex flex-col items-center justify-center bg-white rounded-2xl border border-slate-200/80 shadow-2xs">
            <LoadingSpinner size="lg" className="text-zinc-900 mb-3" />
            <p className="text-slate-500 text-sm font-medium">
              Đang tải chi tiết đơn hàng #{id}...
            </p>
          </div>
        ) : error || !order ? (
          <div className="p-8 text-center bg-white rounded-2xl border border-red-200 shadow-2xs space-y-4">
            <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Không thể tải đơn hàng
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                {error || 'Đơn hàng không tồn tại hoặc bạn không có quyền truy cập.'}
              </p>
            </div>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={fetchOrderDetail}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 transition-colors cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Thử lại</span>
              </button>
              <Link
                to="/orders"
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Về danh sách đơn</span>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Header đơn hàng: Mã đơn, Ngày đặt, Badges & Nút quay lại */}
            <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <Link
                    to="/orders"
                    className="p-1.5 -ml-1 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                    title="Quay lại danh sách"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </Link>
                  <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
                    <span>Đơn hàng #{order.orderCode}</span>
                    <button
                      type="button"
                      onClick={handleCopyOrderCode}
                      className="p-1.5 text-slate-400 hover:text-zinc-950 hover:bg-zinc-100 rounded-lg transition-colors cursor-pointer"
                      title="Sao chép mã đơn"
                    >
                      {copiedCode ? (
                        <Check className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <Copy className="w-4 h-4" />
                      )}
                    </button>
                  </h1>
                </div>

                <div className="flex flex-wrap items-center gap-4 text-xs text-slate-500 pl-8">
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>Ngày đặt: {formatDateTime(order.createdAt)}</span>
                  </div>
                  {order.updatedAt && (
                    <div className="flex items-center gap-1.5 text-slate-400">
                      <span>• Cập nhật: {formatDateTime(order.updatedAt)}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Badges trạng thái */}
              <div className="flex flex-wrap items-center gap-2 self-start md:self-auto pl-8 md:pl-0">
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

            {/* Stepper / Timeline Lộ trình đơn hàng */}
            <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-6 sm:p-8">
              {isCancelled ? (
                /* Banner khi đơn hàng bị hủy */
                <div className="bg-red-50/80 border border-red-200/90 rounded-2xl p-5 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center shrink-0">
                      <XCircle className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-red-900">
                        Đơn hàng này đã bị hủy
                      </h3>
                      <p className="text-xs text-red-700 mt-1 leading-relaxed">
                        Đơn hàng #{order.orderCode} đã bị hủy vào{' '}
                        {formatDateTime(order.updatedAt || order.createdAt)}. Toàn bộ số
                        lượng sản phẩm trong đơn đã được tự động hoàn lại vào kho hàng.
                      </p>
                      {order.note && order.note.includes('Lý do hủy:') && (
                        <p className="text-xs font-semibold text-red-800 mt-2 bg-red-100/60 px-3 py-1.5 rounded-xl border border-red-200/60 inline-block">
                          {order.note}
                        </p>
                      )}
                    </div>
                  </div>
                  <OrderStatusBadge
                    status="CANCELLED"
                    description={order.orderStatusDescription}
                  />
                </div>
              ) : (
                /* Stepper 5 bước bình thường */
                <div>
                  <div className="mb-6">
                    <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                      Lộ trình đơn hàng
                    </h2>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Theo dõi tiến trình xử lý và vận chuyển kiện hàng của bạn
                    </p>
                  </div>

                  <div className="relative">
                    {/* Stepper Grid */}
                    <div className="grid grid-cols-5 gap-2 sm:gap-4 relative z-10">
                      {PROGRESS_STEPS.map((step, idx) => {
                        const isCompleted = currentStepIndex > idx
                        const isCurrent = currentStepIndex === idx
                        const StepIcon = step.icon

                        return (
                          <div
                            key={step.key}
                            className="flex flex-col items-center text-center group"
                          >
                            {/* Step Circle Icon */}
                            <div
                              className={`w-10 h-10 sm:w-12 sm:h-12 rounded-2xl flex items-center justify-center transition-all ${
                                isCompleted
                                  ? 'bg-emerald-600 text-white shadow-2xs'
                                  : isCurrent
                                  ? 'bg-zinc-900 text-zinc-50 ring-4 ring-zinc-200 shadow-xs'
                                  : 'bg-slate-100 text-slate-400 border border-slate-200'
                              }`}
                            >
                              {isCompleted ? (
                                <Check className="w-5 h-5 sm:w-6 sm:h-6 stroke-[2.5]" />
                              ) : (
                                <StepIcon className="w-5 h-5 sm:w-6 sm:h-6" />
                              )}
                            </div>

                            {/* Step Labels */}
                            <div className="mt-3 space-y-0.5">
                              <p
                                className={`text-[11px] sm:text-xs font-bold leading-tight ${
                                  isCompleted
                                    ? 'text-emerald-700'
                                    : isCurrent
                                    ? 'text-zinc-950 font-bold'
                                    : 'text-slate-400 font-medium'
                                }`}
                              >
                                {step.label}
                              </p>
                              <p className="text-[10px] text-slate-400 hidden sm:block">
                                {step.desc}
                              </p>
                            </div>
                          </div>
                        )
                      })}
                    </div>

                    {/* Connecting line behind circles */}
                    <div className="absolute top-5 sm:top-6 left-[10%] right-[10%] h-1 bg-slate-200 -z-0 -translate-y-1/2">
                      <div
                        className="h-full bg-emerald-600 transition-all duration-500"
                        style={{
                          width: `${
                            currentStepIndex >= 0
                              ? Math.min(100, (currentStepIndex / 4) * 100)
                              : 0
                          }%`,
                        }}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* 2 Cột chi tiết: Left: Sản phẩm & Người nhận / Right: Thanh toán & Chi phí & Thao tác */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              {/* Left Column (8/12): Danh sách sản phẩm + Thông tin giao hàng */}
              <div className="lg:col-span-8 space-y-6">
                {/* Danh sách sản phẩm */}
                <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
                  <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                    <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                      <Package className="w-4 h-4 text-zinc-900" />
                      <span>Sản phẩm trong đơn hàng</span>
                    </h2>
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                      {order.items?.length || 0} sản phẩm
                    </span>
                  </div>

                  <div className="divide-y divide-slate-100">
                    {Array.isArray(order.items) &&
                      order.items.map((item) => (
                        <div
                          key={item.id || item.productId}
                          className="p-4 sm:p-5 flex items-center justify-between gap-4"
                        >
                          <div className="flex items-center gap-3.5 min-w-0">
                            <Link
                              to={`/products/${item.productId}`}
                              className="w-16 h-16 rounded-xl bg-slate-100 shrink-0 overflow-hidden border border-slate-100 group block"
                            >
                              <img
                                src={item.imageUrl || FALLBACK_IMAGE}
                                alt={item.productName || 'Sản phẩm'}
                                onError={(e) => {
                                  e.target.onerror = null
                                  e.target.src = FALLBACK_IMAGE
                                }}
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                              />
                            </Link>
                            <div className="min-w-0">
                              <Link
                                to={`/products/${item.productId}`}
                                className="text-xs sm:text-sm font-bold text-slate-900 hover:text-zinc-950 transition-colors line-clamp-2 leading-snug"
                              >
                                {item.productName}
                              </Link>
                              <p className="text-xs text-slate-500 mt-1">
                                {formatPrice(item.unitPrice)} × {item.quantity}
                              </p>
                            </div>
                          </div>

                          <div className="text-xs sm:text-sm font-bold text-slate-900 tabular-nums shrink-0">
                            {formatPrice(
                              item.subtotal || item.unitPrice * item.quantity
                            )}
                          </div>
                        </div>
                      ))}
                  </div>
                </div>

                {/* Thông tin giao nhận hàng */}
                <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                    <MapPin className="w-4 h-4 text-zinc-900" />
                    <h2 className="text-base font-bold text-slate-900">
                      Địa chỉ nhận hàng
                    </h2>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                    <div className="space-y-1">
                      <span className="text-slate-400 block font-medium">
                        Họ và tên người nhận
                      </span>
                      <div className="flex items-center gap-2 font-bold text-slate-900">
                        <User className="w-3.5 h-3.5 text-slate-400" />
                        <span>{order.recipientName}</span>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <span className="text-slate-400 block font-medium">
                        Số điện thoại liên hệ
                      </span>
                      <div className="flex items-center gap-2 font-bold text-slate-900">
                        <Phone className="w-3.5 h-3.5 text-slate-400" />
                        <span>{order.recipientPhone}</span>
                      </div>
                    </div>

                    <div className="sm:col-span-2 space-y-1">
                      <span className="text-slate-400 block font-medium">
                        Địa chỉ chi tiết
                      </span>
                      <div className="flex items-start gap-2 font-semibold text-slate-800 leading-relaxed">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                        <span>{order.shippingAddress}</span>
                      </div>
                    </div>

                    {order.note && (
                      <div className="sm:col-span-2 space-y-1 pt-2 border-t border-slate-100">
                        <span className="text-slate-400 block font-medium">
                          Ghi chú từ khách hàng
                        </span>
                        <div className="flex items-start gap-2 text-slate-600 italic">
                          <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                          <span>"{order.note}"</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Column (4/12): Thông tin thanh toán + Tổng kết chi phí + Nút thao tác */}
              <div className="lg:col-span-4 space-y-6">
                {/* Thông tin thanh toán */}
                <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 space-y-4">
                  <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                    <CreditCard className="w-4 h-4 text-zinc-900" />
                    <h2 className="text-base font-bold text-slate-900">
                      Thông tin thanh toán
                    </h2>
                  </div>

                  <div className="space-y-3 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Phương thức</span>
                      <span className="font-bold text-slate-900">
                        {order.paymentMethodDescription ||
                          order.paymentMethod ||
                          'COD'}
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-slate-500">Trạng thái</span>
                      <PaymentStatusBadge
                        status={order.paymentStatus}
                        description={order.paymentStatusDescription}
                      />
                    </div>

                    {(order.transactionCode || paymentInfo?.transactionCode) && (
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                        <span className="text-slate-500">Mã giao dịch</span>
                        <span className="font-mono font-bold text-slate-800 bg-slate-100 px-2 py-0.5 rounded-md text-[11px]">
                          {paymentInfo?.transactionCode || order.transactionCode}
                        </span>
                      </div>
                    )}

                    {paymentInfo?.paidAt && (
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Thời gian thanh toán</span>
                        <span className="font-semibold text-slate-700">
                          {formatDateTime(paymentInfo.paidAt)}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Tổng kết chi phí */}
                <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-5 sm:p-6 space-y-4">
                  <h2 className="text-base font-bold text-slate-900 pb-3 border-b border-slate-100">
                    Tóm tắt chi phí
                  </h2>

                  <div className="space-y-2.5 text-xs">
                    <div className="flex justify-between text-slate-600">
                      <span>Tạm tính</span>
                      <span className="font-semibold text-slate-900 tabular-nums">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>

                    <div className="flex justify-between text-slate-600">
                      <span>Phí vận chuyển</span>
                      <span className="font-semibold text-emerald-600">
                        Miễn phí
                      </span>
                    </div>

                    <div className="pt-3 border-t border-slate-100 flex justify-between items-baseline">
                      <span className="font-bold text-slate-900 text-sm">
                        Tổng thanh toán
                      </span>
                      <span className="font-extrabold text-zinc-900 text-xl tabular-nums">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>
                  </div>

                  {/* Nút thao tác */}
                  <div className="pt-3 space-y-2.5">
                    {/* Nút Mua lại */}
                    <button
                      type="button"
                      onClick={handleBuyAgain}
                      disabled={reordering}
                      className="w-full py-3 px-4 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-2xs disabled:opacity-50"
                    >
                      {reordering ? (
                        <>
                          <LoadingSpinner size="sm" className="text-white" />
                          <span>Đang thêm vào giỏ...</span>
                        </>
                      ) : (
                        <>
                          <RotateCcw className="w-3.5 h-3.5" />
                          <span>Mua lại đơn hàng này</span>
                        </>
                      )}
                    </button>

                    {/* Nút Hủy đơn có điều kiện: chỉ hiển thị khi PENDING hoặc CONFIRMED */}
                    {canCancel && (
                      <button
                        type="button"
                        onClick={handleOpenCancelModal}
                        className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold text-red-600 hover:bg-red-50 hover:border-red-300 border border-red-200 transition-all cursor-pointer flex items-center justify-center gap-2"
                      >
                        <XCircle className="w-4 h-4 text-red-500" />
                        <span>Hủy đơn hàng</span>
                      </button>
                    )}

                    {/* Nút quay lại danh sách */}
                    <Link
                      to="/orders"
                      className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-100 border border-slate-200 transition-colors flex items-center justify-center gap-1.5"
                    >
                      <ArrowLeft className="w-3.5 h-3.5" />
                      <span>Quay lại danh sách đơn hàng</span>
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default OrderDetailPage
