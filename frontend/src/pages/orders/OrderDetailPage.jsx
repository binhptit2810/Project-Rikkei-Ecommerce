import React, { useState, useEffect, useCallback } from 'react'
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom'
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
  RefreshCw,
  Clock,
  Package,
  X,
  QrCode,
  Banknote,
} from 'lucide-react'
import orderService from '../../services/orderService'
import paymentService from '../../services/paymentService'
import { useCartStore } from '../../store/useCartStore'
import { useToastStore } from '../../store/useToastStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import CancelOrderModal from './CancelOrderModal'
import SimulatePaymentModal from './SimulatePaymentModal'
import VNPayQRModal from './VNPayQRModal'
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

export const OrderDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const addToCart = useCartStore((state) => state.addToCart)
  const clearCart = useCartStore((state) => state.clearCart)

  const [order, setOrder] = useState(null)
  const [paymentInfo, setPaymentInfo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Thông báo chúc mừng khi vừa đặt hàng thành công từ Checkout
  const [justCreated, setJustCreated] = useState(
    Boolean(location.state?.orderSuccess || location.state?.justCreated)
  )

  // Thông báo lỗi thanh toán mô phỏng nếu chuyển từ checkout
  const [paymentFailedNotice, setPaymentFailedNotice] = useState(
    Boolean(location.state?.paymentFailed)
  )

  // Copy mã đơn
  const [copiedCode, setCopiedCode] = useState(false)

  // Cancel order modal
  const [cancelModalOpen, setCancelModalOpen] = useState(false)

  // Simulate payment modal
  const [paymentModalOpen, setPaymentModalOpen] = useState(false)

  // VNPay QR modal
  const [vnpayQRModalOpen, setVnpayQRModalOpen] = useState(false)
  const [vnpayQRData, setVnpayQRData] = useState(null)
  const [loadingVNPayQR, setLoadingVNPayQR] = useState(false)

  const handleOpenVNPayQR = async () => {
    if (!order?.id) return
    setLoadingVNPayQR(true)
    try {
      const res = await paymentService.createVNPayPayment(
        order.id,
        'NCB',
        `Thanh toan don hang #${order.orderCode || order.id}`
      )
      const data = res?.data || res
      setVnpayQRData(data)
      setVnpayQRModalOpen(true)
    } catch (err) {
      const fallbackQR = {
        orderId: order.id,
        orderCode: order.orderCode,
        amount: order.totalAmount,
        bankCode: 'NCB',
        bankName: 'Ngân hàng Quốc Dân (NCB)',
        accountNumber: '9704198526191432198',
        accountName: 'CONG TY TNHH E-COMMERCE RIKKEI',
        vnpTxnRef: order.transactionCode || `PAY-${order.orderCode || order.id}`,
        qrCodeUrl: `https://img.vietqr.io/image/NCB-9704198526191432198-compact2.png?amount=${Math.round(
          order.totalAmount || 0
        )}&addInfo=${encodeURIComponent(order.orderCode || order.id)}&accountName=CONG%20TY%20TNHH%20E-COMMERCE%20RIKKEI`,
      }
      setVnpayQRData(fallbackQR)
      setVnpayQRModalOpen(true)
    } finally {
      setLoadingVNPayQR(false)
    }
  }

  // Tự động mở modal mã QR VNPAY nếu được điều hướng từ CheckoutPage với cờ openVNPayQR (chỉ cho đơn thanh toán online)
  useEffect(() => {
    if (location.state?.openVNPayQR && order?.paymentMethod !== 'COD') {
      if (location.state?.vnpayPaymentData) {
        setVnpayQRData(location.state.vnpayPaymentData)
      }
      setVnpayQRModalOpen(true)
    }
  }, [location.state, order?.paymentMethod])

  // Khởi tạo dữ liệu mã QR hiển thị sẵn trên trang nếu đơn chưa thanh toán VÀ là phương thức thanh toán trực tuyến
  useEffect(() => {
    const isOnline =
      order?.paymentMethod === 'VNPAY' || order?.paymentMethod === 'SIMULATED'
    if (order?.id && order?.paymentStatus === 'UNPAID' && isOnline && !vnpayQRData) {
      const defaultQR = {
        orderId: order.id,
        orderCode: order.orderCode,
        amount: order.totalAmount,
        bankCode: 'NCB',
        bankName: 'Ngân hàng Quốc Dân (NCB)',
        accountNumber: '9704198526191432198',
        accountName: 'CONG TY TNHH E-COMMERCE RIKKEI',
        vnpTxnRef: order.transactionCode || `PAY-${order.orderCode || order.id}`,
        qrCodeUrl: `https://img.vietqr.io/image/NCB-9704198526191432198-compact2.png?amount=${Math.round(
          order.totalAmount || 0
        )}&addInfo=${encodeURIComponent(order.orderCode || order.id)}&accountName=CONG%20TY%20TNHH%20E-COMMERCE%20RIKKEI`,
      }
      setVnpayQRData(defaultQR)
    }
  }, [order?.id, order?.paymentStatus, order?.paymentMethod, vnpayQRData])

  // Reorder loading
  const [reordering, setReordering] = useState(false)

  // Toast kích hoạt ô thông báo góc phải tự động ẩn sau 2s
  const showToast = (message, type = 'success') => {
    useToastStore.getState().showToast({
      title: type === 'error' ? 'Có lỗi xảy ra' : 'Thao tác thành công',
      message,
      type: type === 'error' ? 'error' : 'success',
      duration: 2000,
    })
  }

  // Tự động kích hoạt toast chúc mừng nếu được điều hướng từ CheckoutPage (biến mất sau 2s)
  useEffect(() => {
    if (location.state?.orderSuccess || location.state?.justCreated) {
      setJustCreated(true)
      if (location.state?.paymentFailed) {
        useToastStore.getState().showToast({
          title: 'Thanh toán chưa hoàn tất',
          message: `Đơn hàng #${location.state.orderCode || ''} đã được tạo. Vui lòng thanh toán lại.`,
          type: 'error',
          duration: 2000,
        })
      } else {
        useToastStore.getState().showToast({
          title: 'Đặt hàng thành công!',
          message: `Mã đơn hàng: #${location.state.orderCode || ''} • Cảm ơn bạn đã mua hàng!`,
          type: 'success',
          duration: 2000,
        })
      }
    }
  }, [location.state])

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

  // Mở modal hủy đơn
  const handleOpenCancelModal = () => {
    setCancelModalOpen(true)
  }

  // Xử lý khi hủy đơn hàng thành công từ CancelOrderModal (dùng chung logic với OrderHistoryPage)
  const handleCancelSuccess = (updatedOrder, finalReason) => {
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
      `Đã hủy đơn hàng #${order?.orderCode || order?.id} thành công! Số lượng sản phẩm đã được tự động hoàn lại vào kho.`,
      'success'
    )
    setCancelModalOpen(false)
  }

  // Xử lý khi hủy đơn hàng thất bại từ CancelOrderModal
  const handleCancelError = (errorMsg) => {
    showToast(errorMsg, 'error')
    if (errorMsg && errorMsg.includes('trạng thái')) {
      fetchOrderDetail()
    }
  }

  // Xử lý khi thanh toán mô phỏng thành công
  const handlePaymentSuccess = (paymentData) => {
    setOrder((prev) => ({
      ...prev,
      paymentStatus: 'PAID',
      paymentStatusDescription: 'Đã thanh toán',
      orderStatus: prev?.orderStatus === 'PENDING' ? 'CONFIRMED' : prev?.orderStatus,
      orderStatusDescription: prev?.orderStatus === 'PENDING' ? 'Đã xác nhận' : prev?.orderStatusDescription,
      transactionCode: paymentData?.transactionCode || prev?.transactionCode,
    }))
    setPaymentInfo(paymentData)
    clearCart()
    setPaymentFailedNotice(false)
    showToast(
      `🎉 Thanh toán thành công cho đơn #${order?.orderCode || order?.id}! Mã giao dịch: ${paymentData?.transactionCode || ''}`,
      'success'
    )
    fetchOrderDetail()
  }

  // Xử lý khi thanh toán mô phỏng thất bại
  const handlePaymentError = (errorMsg) => {
    showToast(errorMsg || 'Thanh toán mô phỏng thất bại.', 'error')
    fetchOrderDetail()
  }

  const [confirmingPayment, setConfirmingPayment] = useState(false)

  // Xử lý xác nhận chuyển khoản trực tiếp từ Banner QR trên trang
  const handleInlineConfirmPaid = async () => {
    if (!order?.id || confirmingPayment) return
    setConfirmingPayment(true)
    try {
      const note = `Xác nhận thanh toán thành công qua VNPAY-QR (${vnpayQRData?.bankCode || 'NCB'})`
      const res = await paymentService.simulatePayment(order.id, true, note)
      const data = res?.data || res
      if (data?.status === 'PAID') {
        handlePaymentSuccess(data)
      } else {
        showToast('Giao dịch chưa được ghi nhận thành công.', 'error')
      }
    } catch (err) {
      showToast(err.response?.data?.message || err.message || 'Lỗi khi xác nhận giao dịch.', 'error')
    } finally {
      setConfirmingPayment(false)
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
      {/* Modal Hủy đơn hàng dùng chung với OrderHistoryPage */}
      <CancelOrderModal
        isOpen={cancelModalOpen}
        order={order}
        onClose={() => setCancelModalOpen(false)}
        onSuccess={handleCancelSuccess}
        onError={handleCancelError}
      />

      {/* Modal Mô phỏng thanh toán trực tuyến */}
      <SimulatePaymentModal
        isOpen={paymentModalOpen}
        order={order}
        onClose={() => setPaymentModalOpen(false)}
        onSuccess={handlePaymentSuccess}
        onError={handlePaymentError}
      />

      {/* Modal Quét mã VNPAY-QR */}
      <VNPayQRModal
        isOpen={vnpayQRModalOpen}
        order={order}
        paymentData={vnpayQRData}
        onClose={() => setVnpayQRModalOpen(false)}
        onSuccess={handlePaymentSuccess}
        onError={handlePaymentError}
      />

      <div className="max-w-7xl mx-auto">
        {/* Thông báo chúc mừng khi vừa đặt hàng thành công */}
        {justCreated && !paymentFailedNotice && (
          <div className="mb-6 p-4 sm:p-5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-950 shadow-sm flex items-start justify-between gap-4 animate-in fade-in slide-in-from-top-2 duration-300">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-sm shadow-emerald-200">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm sm:text-base font-bold text-emerald-950">
                  🎉 Chúc mừng bạn đã đặt hàng thành công! Rikkei Store chân thành cảm ơn bạn.
                </h3>
                <p className="text-xs text-emerald-800 mt-1 leading-relaxed">
                  Mã đơn hàng của bạn là <strong className="font-mono underline text-emerald-950">#{order?.orderCode || location.state?.orderCode || id}</strong>.
                  Đơn hàng đã được ghi nhận vào hệ thống và đang ở trạng thái <strong>{order?.orderStatusDescription || 'Chờ xác nhận'}</strong>.
                </p>
              </div>
            </div>
            <button
              onClick={() => setJustCreated(false)}
              className="text-emerald-700 hover:text-emerald-950 p-1.5 rounded-lg hover:bg-emerald-100 transition-colors cursor-pointer shrink-0"
              title="Đóng thông báo"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Banner thông báo khi thanh toán mô phỏng thất bại */}
        {paymentFailedNotice && (
          <div className="mb-6 p-4 sm:p-5 rounded-2xl bg-amber-50 border border-amber-200 text-amber-950 shadow-sm flex items-start justify-between gap-4 animate-in fade-in duration-300">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-sm">
                <AlertCircle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm sm:text-base font-bold text-amber-950">
                  ⚠️ Đơn hàng #{order?.orderCode || location.state?.orderCode || id} chưa hoàn tất thanh toán
                </h3>
                <p className="text-xs text-amber-800 mt-1 leading-relaxed">
                  Giao dịch thanh toán mô phỏng chưa thành công. Giỏ hàng của bạn đã được giữ nguyên và đơn hàng vẫn đang ở trạng thái Chờ xác nhận. Bạn có thể nhấn nút bên dưới để thanh toán lại.
                </p>
                <div className="flex flex-wrap items-center gap-2 mt-3">
                  <button
                    type="button"
                    onClick={handleOpenVNPayQR}
                    disabled={loadingVNPayQR}
                    className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 transition-colors shadow-md shadow-blue-500/20 cursor-pointer disabled:opacity-50"
                  >
                    <QrCode className="w-3.5 h-3.5" />
                    <span>{loadingVNPayQR ? 'Đang tạo QR...' : 'Quét mã VNPAY-QR'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setPaymentModalOpen(true)}
                    className="inline-flex items-center gap-2 px-4 py-2.5 rounded-full text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 transition-colors cursor-pointer"
                  >
                    <CreditCard className="w-3.5 h-3.5" />
                    <span>Mô phỏng tức thì</span>
                  </button>
                </div>
              </div>
            </div>
            <button
              onClick={() => setPaymentFailedNotice(false)}
              className="text-amber-700 hover:text-amber-950 p-1.5 rounded-lg hover:bg-amber-100 transition-colors cursor-pointer shrink-0"
              title="Đóng thông báo"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

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
                                  ? 'bg-blue-600 text-white ring-4 ring-blue-100 shadow-md'
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

            {/* 1. Nếu là đơn hàng COD: Hiển thị Card thông tin thanh toán tiền mặt khi nhận hàng */}
            {order.paymentMethod === 'COD' && order.orderStatus !== 'CANCELLED' && (
              <div className="bg-gradient-to-br from-emerald-50/90 via-white to-teal-50/90 rounded-3xl border-2 border-emerald-200 shadow-md p-6 sm:p-7 flex flex-col md:flex-row items-center gap-6 animate-in fade-in duration-300">
                <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-emerald-100 text-emerald-700 flex flex-col items-center justify-center shrink-0 shadow-inner text-center p-2">
                  <Banknote className="w-10 h-10 mb-1" />
                  <span className="text-[10px] font-black uppercase tracking-wider">Tiền mặt</span>
                </div>

                <div className="flex-1 space-y-3 w-full">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-emerald-600 text-white tracking-wider shadow-xs">
                      Thanh toán khi nhận hàng (COD)
                    </span>
                    <h3 className="text-base sm:text-lg font-black text-slate-900 tracking-tight">
                      Đơn hàng #{order.orderCode} sẽ thanh toán bằng tiền mặt
                    </h3>
                  </div>

                  <p className="text-xs text-slate-600 leading-relaxed">
                    Quý khách <span className="font-bold text-emerald-700">không cần chuyển khoản trước</span>. Nhân viên giao hàng (Shipper) sẽ liên hệ qua số điện thoại <span className="font-bold text-slate-900">{order.recipientPhone || 'của bạn'}</span> và thu tiền mặt trực tiếp khi bạn nhận hàng.
                  </p>

                  <div className="bg-white/95 rounded-2xl p-4 border border-emerald-200/90 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs shadow-2xs">
                    <div>
                      <span className="text-slate-400 block text-[11px]">Số tiền cần thanh toán cho shipper:</span>
                      <span className="font-black text-base text-emerald-600">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[11px]">Địa chỉ giao hàng:</span>
                      <span className="font-semibold text-slate-800 line-clamp-1">
                        {order.shippingAddress || 'Theo thông tin đơn hàng'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 text-[11px] text-slate-500 pt-1">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Được kiểm tra hàng trước khi thanh toán • Vui lòng chuẩn bị sẵn tiền mặt khi shipper giao tới</span>
                  </div>
                </div>
              </div>
            )}

            {/* 2. Banner/Card hiển thị Mã QR thanh toán VNPAY (Chỉ hiển thị khi chọn thanh toán online VNPAY / SIMULATED) */}
            {(order.paymentMethod === 'VNPAY' || order.paymentMethod === 'SIMULATED') &&
              order.paymentStatus === 'UNPAID' &&
              order.orderStatus !== 'CANCELLED' && (
              <div className="bg-gradient-to-br from-blue-50/90 via-white to-indigo-50/90 rounded-3xl border-2 border-blue-200 shadow-md p-6 sm:p-7 flex flex-col md:flex-row items-center gap-6 animate-in fade-in duration-300">
                <div className="bg-white p-3 rounded-2xl border border-blue-200 shadow-sm shrink-0 text-center">
                  <img
                    src={
                      vnpayQRData?.qrCodeUrl ||
                      `https://img.vietqr.io/image/NCB-9704198526191432198-compact2.png?amount=${Math.round(
                        order.totalAmount || 0
                      )}&addInfo=${encodeURIComponent(
                        order.transactionCode || order.orderCode || `ORD-${order.id}`
                      )}&accountName=CONG%20TY%20TNHH%20E-COMMERCE%20RIKKEI`
                    }
                    alt="Mã QR thanh toán VNPAY"
                    className="w-48 h-48 sm:w-56 sm:h-56 object-contain rounded-xl mx-auto shadow-xs"
                    onError={(e) => {
                      e.target.onerror = null
                      e.target.src =
                        'https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=' +
                        encodeURIComponent(
                          `VNPAY-ORDER:${order.orderCode || order.id}|AMOUNT:${order.totalAmount}|ACC:9704198526191432198`
                        )
                    }}
                  />
                  <span className="text-[10px] font-extrabold text-blue-700 uppercase tracking-wider block mt-2">
                    Quét bằng App Ngân hàng
                  </span>
                </div>

                <div className="flex-1 space-y-3.5 w-full">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase bg-gradient-to-r from-blue-600 to-indigo-600 text-white tracking-wider shadow-xs">
                      VNPAY-QR (Mô phỏng)
                    </span>
                    <h3 className="text-base sm:text-lg font-black text-slate-900 tracking-tight">
                      Mã QR thanh toán đơn hàng #{order.orderCode}
                    </h3>
                  </div>

                  <p className="text-xs text-slate-600 leading-relaxed">
                    Vui lòng mở ứng dụng ngân hàng hoặc ví VNPAY trên điện thoại, quét mã QR bên cạnh để chuyển khoản chính xác số tiền:{' '}
                    <span className="font-extrabold text-blue-700 text-sm">
                      {formatPrice(order.totalAmount)}
                    </span>
                  </p>

                  <div className="bg-white/95 rounded-2xl p-3.5 border border-slate-200/90 grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs shadow-2xs">
                    <div>
                      <span className="text-slate-400 block text-[11px]">Ngân hàng thụ hưởng:</span>
                      <span className="font-bold text-slate-900">Quốc Dân (NCB)</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[11px]">Số tài khoản:</span>
                      <span className="font-mono font-bold text-slate-900">9704198526191432198</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[11px]">Chủ tài khoản:</span>
                      <span className="font-bold text-slate-900">CONG TY TNHH E-COMMERCE RIKKEI</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[11px]">Nội dung chuyển khoản:</span>
                      <span className="font-mono font-bold text-blue-700">
                        {order.transactionCode || order.orderCode}
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 pt-1">
                    <button
                      type="button"
                      disabled={confirmingPayment}
                      onClick={handleInlineConfirmPaid}
                      className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 active:scale-[0.98] transition-all shadow-md shadow-emerald-500/20 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                      {confirmingPayment ? (
                        <LoadingSpinner size="sm" />
                      ) : (
                        <Check className="w-4 h-4" />
                      )}
                      <span>✓ Tôi đã hoàn tất chuyển khoản</span>
                    </button>
                    <button
                      type="button"
                      onClick={handleOpenVNPayQR}
                      className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold text-blue-700 bg-blue-100/80 hover:bg-blue-100 transition-colors cursor-pointer"
                    >
                      <QrCode className="w-3.5 h-3.5" />
                      <span>Xem mã phóng to</span>
                    </button>
                  </div>
                </div>
              </div>
            )}

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
                              {item.selectedSize && (
                                <div className="mt-1">
                                  <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                                    Size: {item.selectedSize}
                                  </span>
                                </div>
                              )}
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
                        paymentMethod={order.paymentMethod}
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

                    {/* Nút hành động thanh toán mô phỏng (Chỉ áp dụng khi chọn thanh toán online VNPAY / SIMULATED) */}
                    {(order.paymentMethod === 'VNPAY' || order.paymentMethod === 'SIMULATED') &&
                      order.paymentStatus !== 'PAID' &&
                      order.orderStatus !== 'CANCELLED' && (
                      <div className="pt-3 border-t border-slate-100 space-y-2">
                        <button
                          type="button"
                          onClick={handleOpenVNPayQR}
                          disabled={loadingVNPayQR}
                          className="w-full py-2.5 px-3 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 transition-colors flex items-center justify-center gap-1.5 cursor-pointer shadow-md shadow-blue-500/20 disabled:opacity-50"
                        >
                          <QrCode className="w-3.5 h-3.5" />
                          <span>{loadingVNPayQR ? 'Đang tạo QR...' : 'Quét mã VNPAY-QR'}</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setPaymentModalOpen(true)}
                          className="w-full py-2.5 px-3 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                        >
                          <CreditCard className="w-3.5 h-3.5" />
                          <span>Mô phỏng tức thì</span>
                        </button>
                      </div>
                    )}

                    {/* Thông báo hướng dẫn thanh toán cho đơn COD */}
                    {order.paymentMethod === 'COD' && (
                      <div className="pt-3 border-t border-slate-100">
                        <div className="bg-emerald-50/80 border border-emerald-200/80 rounded-xl p-3 text-xs text-emerald-800 space-y-1">
                          <div className="flex items-center gap-1.5 font-bold">
                            <Banknote className="w-4 h-4 text-emerald-600 shrink-0" />
                            <span>Thu tiền mặt khi nhận hàng</span>
                          </div>
                          <p className="text-[11px] text-emerald-700 leading-relaxed">
                            Quý khách vui lòng chuẩn bị <strong>{formatPrice(order.totalAmount)}</strong> để gửi nhân viên giao hàng khi nhận kiện hàng.
                          </p>
                        </div>
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
                      className="w-full py-3 px-5 rounded-full text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 active:scale-[0.99] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-md shadow-blue-500/20 disabled:opacity-50"
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
                        className="w-full py-2.5 px-4 rounded-full text-xs font-bold text-red-600 hover:bg-red-50 hover:border-red-300 border border-red-200 transition-all cursor-pointer flex items-center justify-center gap-2"
                      >
                        <XCircle className="w-4 h-4 text-red-500" />
                        <span>Hủy đơn hàng</span>
                      </button>
                    )}

                    {/* Nút quay lại danh sách */}
                    <Link
                      to="/orders"
                      className="w-full py-2.5 px-4 rounded-full text-xs font-bold text-slate-700 hover:bg-slate-100 border border-slate-200 transition-colors flex items-center justify-center gap-1.5"
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
