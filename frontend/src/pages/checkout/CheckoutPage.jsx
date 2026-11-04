import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Banknote,
  CreditCard,
  ShieldCheck,
  ShoppingBag,
  AlertCircle,
  User,
  Phone,
  MapPin,
  FileText,
  Lock,
  CheckCircle2,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'
import orderService from '../../services/orderService'

export const CheckoutPage = () => {
  const navigate = useNavigate()
  const { items, getTotalPrice, getTotalCount, clearCart } = useCartStore()
  const { user } = useAuthStore()

  const [formData, setFormData] = useState({
    recipientName: '',
    recipientPhone: '',
    shippingAddress: '',
    paymentMethod: 'COD',
    note: '',
  })

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)
  const [successInfo, setSuccessInfo] = useState(null)

  // Tự động điền thông tin có sẵn từ tài khoản người dùng
  useEffect(() => {
    if (user) {
      setFormData((prev) => ({
        ...prev,
        recipientName: prev.recipientName || user.fullName || '',
        recipientPhone: prev.recipientPhone || user.phone || '',
        shippingAddress: prev.shippingAddress || user.address || '',
      }))
    }
  }, [user])

  // Nếu giỏ hàng rỗng, điều hướng về trang giỏ hàng
  useEffect(() => {
    if (items.length === 0 && !successInfo) {
      navigate('/cart')
    }
  }, [items, successInfo, navigate])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errorMessage) setErrorMessage(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!formData.recipientName.trim()) {
      setErrorMessage('Vui lòng nhập họ tên người nhận hàng')
      return
    }

    if (formData.recipientName.trim().length > 100) {
      setErrorMessage('Họ tên người nhận không được vượt quá 100 ký tự')
      return
    }

    const phoneRegex = /^(0|\+84)[0-9]{9}$/
    if (!formData.recipientPhone.trim()) {
      setErrorMessage('Vui lòng nhập số điện thoại người nhận hàng')
      return
    }

    if (!phoneRegex.test(formData.recipientPhone.trim())) {
      setErrorMessage('Số điện thoại không hợp lệ (yêu cầu 10 chữ số, ví dụ: 0912345678 hoặc +84912345678)')
      return
    }

    if (!formData.shippingAddress.trim()) {
      setErrorMessage('Vui lòng nhập địa chỉ nhận hàng chi tiết')
      return
    }

    if (formData.shippingAddress.trim().length < 5) {
      setErrorMessage('Địa chỉ nhận hàng quá ngắn, vui lòng nhập chi tiết số nhà, tên đường, phường/xã')
      return
    }

    if (!formData.paymentMethod) {
      setErrorMessage('Vui lòng chọn phương thức thanh toán (COD hoặc SIMULATED)')
      return
    }

    // Validate tồn kho: 1 <= quantity <= stockQuantity
    for (const item of items) {
      if (!item.quantity || item.quantity < 1) {
        setErrorMessage(`Số lượng đặt của sản phẩm "${item.name}" không hợp lệ (phải >= 1)`)
        return
      }
      if (item.stockQuantity !== undefined && item.stockQuantity !== null && item.quantity > item.stockQuantity) {
        setErrorMessage(
          `Sản phẩm "${item.name}" chỉ còn ${item.stockQuantity} món trong kho (bạn đang chọn ${item.quantity}). Vui lòng điều chỉnh lại.`
        )
        return
      }
    }

    const orderPayload = {
      recipientName: formData.recipientName.trim(),
      recipientPhone: formData.recipientPhone.trim(),
      shippingAddress: formData.shippingAddress.trim(),
      paymentMethod: formData.paymentMethod,
      note: formData.note.trim() || null,
      items: items.map((item) => ({
        productId: item.id,
        quantity: item.quantity,
      })),
    }

    setErrorMessage(null)
    setIsSubmitting(true)
    try {
      const response = await orderService.createOrder(orderPayload)
      const createdOrder = response?.data || response
      clearCart()
      setSuccessInfo({
        message: 'Đặt hàng thành công! Đơn hàng của bạn đã được ghi nhận vào hệ thống.',
        order: createdOrder,
        payload: orderPayload,
      })
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Không thể tạo đơn hàng. Vui lòng thử lại.'
      setErrorMessage(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const totalPrice = getTotalPrice()
  const totalCount = getTotalCount()

  if (items.length === 0 && !successInfo) {
    return null
  }

  return (
    <div className="min-h-[calc(100vh-140px)] bg-zinc-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs text-zinc-500 mb-6">
          <Link to="/" className="hover:text-zinc-950 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/cart" className="hover:text-zinc-950 transition-colors">
            Giỏ hàng
          </Link>
          <span>/</span>
          <span className="font-medium text-zinc-900">Thanh toán</span>
        </nav>

        {/* Header */}
        <div className="mb-6 pb-4 border-b border-zinc-200">
          <div className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wider text-zinc-500 mb-1">
            <ShieldCheck className="w-3.5 h-3.5 text-zinc-700" />
            <span>Thanh toán bảo mật</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-semibold text-zinc-950 tracking-tight">
            Xác nhận thông tin & Đặt hàng
          </h1>
          <p className="text-zinc-500 text-sm mt-1">
            Kiểm tra địa chỉ giao nhận và phương thức thanh toán trước khi xác nhận đặt mua.
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-6 p-3.5 bg-red-50 border border-red-200 text-red-800 rounded-md flex items-center justify-between text-xs sm:text-sm shadow-xs">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span className="font-medium">{errorMessage}</span>
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-700 hover:text-red-950 font-medium text-xs px-2 py-0.5 rounded cursor-pointer"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Success Notice with PAY- transactionCode */}
        {successInfo && (
          <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-md shadow-xs space-y-2">
            <div className="flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-sm">{successInfo.message}</p>
                {successInfo.order && (
                  <div className="mt-1 space-y-1">
                    <p className="text-xs text-emerald-800 font-medium">
                      Mã đơn hàng: <span className="underline font-semibold">{successInfo.order.orderCode}</span> — Tổng thanh toán: {formatPrice(successInfo.order.totalAmount)}
                    </p>
                    {successInfo.order.transactionCode && (
                      <p className="text-xs text-emerald-800 font-medium">
                        Mã giao dịch: <span className="font-mono bg-emerald-100 px-1.5 py-0.5 rounded text-emerald-900 font-bold">{successInfo.order.transactionCode}</span> ({successInfo.order.paymentMethodDescription || successInfo.order.paymentMethod})
                      </p>
                    )}
                  </div>
                )}
                <p className="text-xs text-emerald-700 mt-1">
                  Người nhận: <strong>{successInfo.payload?.recipientName}</strong> ({successInfo.payload?.recipientPhone}) - Địa chỉ: <strong>{successInfo.payload?.shippingAddress}</strong>
                </p>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Recipient info & Payment methods (7/12) */}
            <div className="lg:col-span-7 space-y-6">
              {/* Card thông tin giao hàng */}
              <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 space-y-4">
                <div className="flex items-center gap-2.5 pb-3 border-b border-zinc-100">
                  <span className="w-6 h-6 rounded bg-zinc-900 text-zinc-50 flex items-center justify-center font-medium text-xs">
                    1
                  </span>
                  <h2 className="text-sm font-semibold text-zinc-950">Thông tin người nhận</h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-zinc-900 block">
                      Họ và tên người nhận <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <User className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        name="recipientName"
                        value={formData.recipientName}
                        onChange={handleChange}
                        placeholder="Nguyễn Văn A"
                        required
                        className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-medium text-zinc-900 block">
                      Số điện thoại <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="tel"
                        name="recipientPhone"
                        value={formData.recipientPhone}
                        onChange={handleChange}
                        placeholder="0912345678"
                        required
                        className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950"
                      />
                    </div>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-zinc-900 block">
                    Địa chỉ nhận hàng chi tiết <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
                    <textarea
                      name="shippingAddress"
                      value={formData.shippingAddress}
                      onChange={handleChange}
                      rows="2"
                      placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                      required
                      className="flex min-h-[60px] w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-2 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 resize-none"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-zinc-900 block">
                    Ghi chú đơn hàng (Tùy chọn)
                  </label>
                  <div className="relative">
                    <FileText className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      name="note"
                      value={formData.note}
                      onChange={handleChange}
                      placeholder="Giao giờ hành chính, gọi trước khi giao..."
                      className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950"
                    />
                  </div>
                </div>
              </div>

              {/* Card phương thức thanh toán */}
              <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 space-y-4">
                <div className="flex items-center gap-2.5 pb-3 border-b border-zinc-100">
                  <span className="w-6 h-6 rounded bg-zinc-900 text-zinc-50 flex items-center justify-center font-medium text-xs">
                    2
                  </span>
                  <h2 className="text-sm font-semibold text-zinc-950">Phương thức thanh toán</h2>
                </div>

                <div className="space-y-3">
                  <label
                    className={`flex items-start gap-3 p-3.5 rounded-md border transition-colors cursor-pointer ${
                      formData.paymentMethod === 'COD'
                        ? 'border-zinc-900 bg-zinc-50 shadow-xs'
                        : 'border-zinc-200 hover:border-zinc-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={formData.paymentMethod === 'COD'}
                      onChange={handleChange}
                      className="mt-0.5 text-zinc-900 focus:ring-zinc-900 cursor-pointer"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <Banknote className="w-4 h-4 text-zinc-800" />
                        <span className="font-medium text-sm text-zinc-900">Thanh toán khi nhận hàng (COD)</span>
                      </div>
                      <p className="text-xs text-zinc-500 mt-0.5">
                        Thanh toán tiền mặt cho nhân viên giao hàng khi nhận được kiện hàng.
                      </p>
                    </div>
                  </label>

                  <label
                    className={`flex items-start gap-3 p-3.5 rounded-md border transition-colors cursor-pointer ${
                      formData.paymentMethod === 'SIMULATED'
                        ? 'border-zinc-900 bg-zinc-50 shadow-xs'
                        : 'border-zinc-200 hover:border-zinc-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="SIMULATED"
                      checked={formData.paymentMethod === 'SIMULATED'}
                      onChange={handleChange}
                      className="mt-0.5 text-zinc-900 focus:ring-zinc-900 cursor-pointer"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <CreditCard className="w-4 h-4 text-zinc-800" />
                        <span className="font-medium text-sm text-zinc-900">Thanh toán trực tuyến mô phỏng</span>
                      </div>
                      <p className="text-xs text-zinc-500 mt-0.5">
                        Cổng thanh toán giả lập (sinh mã định danh PAY- tự động).
                      </p>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            {/* Right Column: Order summary (5/12) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 space-y-5 sticky top-20">
                <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
                  <div className="flex items-center gap-2">
                    <ShoppingBag className="w-4 h-4 text-zinc-700" />
                    <h2 className="text-sm font-semibold text-zinc-950">Đơn hàng của bạn</h2>
                  </div>
                  <span className="text-xs font-medium px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200">
                    {totalCount} sản phẩm
                  </span>
                </div>

                {/* Danh sách sản phẩm mua */}
                <div className="max-h-64 overflow-y-auto divide-y divide-zinc-100 pr-1">
                  {items.map((item) => (
                    <div key={item.id} className="py-2.5 flex items-center justify-between gap-3">
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-10 h-10 rounded-md bg-zinc-50 shrink-0 overflow-hidden border border-zinc-200">
                          {item.imageUrl ? (
                            <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-zinc-400 text-xs">
                              SP
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-medium text-zinc-900 truncate">{item.name}</p>
                          <p className="text-[11px] text-zinc-500">
                            {formatPrice(item.price)} x {item.quantity}
                          </p>
                        </div>
                      </div>
                      <div className="text-xs font-semibold text-zinc-950 shrink-0 tabular-nums">
                        {formatPrice(item.price * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Chi phí */}
                <div className="pt-3 border-t border-zinc-100 space-y-2 text-xs">
                  <div className="flex justify-between text-zinc-600">
                    <span>Tạm tính</span>
                    <span className="font-medium text-zinc-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  <div className="flex justify-between text-zinc-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-medium text-emerald-600">Miễn phí</span>
                  </div>
                  <div className="pt-3 border-t border-zinc-100 flex justify-between text-sm">
                    <span className="font-semibold text-zinc-950">Tổng thanh toán</span>
                    <span className="font-semibold text-zinc-950 text-base tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                </div>

                {/* Submit button */}
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none text-zinc-50 font-medium text-sm rounded-md shadow-sm transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <Lock className="w-4 h-4" />
                  <span>Xác nhận đặt hàng</span>
                </button>

                <div className="text-center text-[11px] text-zinc-400 leading-relaxed">
                  Bằng việc hoàn tất đặt hàng, bạn xác nhận đồng ý với điều khoản mua sắm và chính sách bảo mật của Rikkei Store.
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  )
}

export default CheckoutPage
