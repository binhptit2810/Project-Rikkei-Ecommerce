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
  Zap,
  Tag,
  X,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'
import orderService from '../../services/orderService'
import paymentService from '../../services/paymentService'
import couponService from '../../services/couponService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const CheckoutPage = () => {
  const navigate = useNavigate()
  const { items, getTotalPrice, getTotalCount, clearCart } = useCartStore()
  const { user, isAuthenticated } = useAuthStore()

  const [formData, setFormData] = useState({
    recipientName: '',
    recipientPhone: '',
    shippingAddress: '',
    paymentMethod: 'COD',
    note: '',
  })

  const [simulateSuccessOption, setSimulateSuccessOption] = useState(true)
  const [vnpayBankCode, setVnpayBankCode] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  // Mã giảm giá / Voucher
  const [couponCodeInput, setCouponCodeInput] = useState('')
  const [appliedCoupon, setAppliedCoupon] = useState(null)
  const [isValidatingCoupon, setIsValidatingCoupon] = useState(false)
  const [couponError, setCouponError] = useState(null)

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

  // Kiểm tra quyền: Tuyệt đối không cho phép Quản trị viên vào trang đặt hàng
  useEffect(() => {
    if (user?.role === 'ADMIN') {
      navigate('/admin/dashboard', { replace: true })
      return
    }
    if (!isSubmitting && items.length === 0) {
      navigate('/cart')
    }
  }, [user, items, isSubmitting, navigate])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errorMessage) setErrorMessage(null)
  }

  const handleApplyCoupon = async (e) => {
    e?.preventDefault()
    if (!couponCodeInput.trim()) {
      setCouponError('Vui lòng nhập mã giảm giá')
      return
    }
    setIsValidatingCoupon(true)
    setCouponError(null)
    try {
      const res = await couponService.validateCoupon(couponCodeInput.trim(), getTotalPrice())
      const data = res?.data || res
      setAppliedCoupon(data)
      setCouponError(null)
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Mã giảm giá không hợp lệ'
      setCouponError(msg)
      setAppliedCoupon(null)
    } finally {
      setIsValidatingCoupon(false)
    }
  }

  const handleRemoveCoupon = () => {
    setAppliedCoupon(null)
    setCouponCodeInput('')
    setCouponError(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Chống submit trùng lặp khi đang xử lý
    if (isSubmitting) return

    // Chặn Quản trị viên đặt hàng
    if (user?.role === 'ADMIN') {
      setErrorMessage('Tài khoản Quản trị viên (ADMIN) không được phép đặt hàng.')
      return
    }

    // Kiểm tra phiên đăng nhập
    if (!isAuthenticated && !user) {
      setErrorMessage('Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại.')
      navigate('/login', { state: { from: '/checkout' } })
      return
    }

    // Kiểm tra giỏ hàng
    if (!items || items.length === 0) {
      setErrorMessage('Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán.')
      navigate('/cart')
      return
    }

    // Validation form
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
      setErrorMessage('Số điện thoại không đúng định dạng (yêu cầu 10 chữ số, ví dụ: 0912345678 hoặc +84912345678)')
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
      setErrorMessage('Vui lòng chọn phương thức thanh toán (COD, SIMULATED hoặc VNPAY)')
      return
    }

    // Validate số lượng từng sản phẩm
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

    // Chuẩn hóa DTO CreateOrderRequest theo đúng cấu trúc Backend
    const orderPayload = {
      recipientName: formData.recipientName.trim(),
      recipientPhone: formData.recipientPhone.trim(),
      shippingAddress: formData.shippingAddress.trim(),
      paymentMethod: formData.paymentMethod,
      note: formData.note.trim() || null,
      couponCode: appliedCoupon?.code || null,
      items: items.map((item) => ({
        productId: Number(item.id),
        quantity: Number(item.quantity),
      })),
    }

    setErrorMessage(null)
    setIsSubmitting(true)

    try {
      // Gửi request API thật lên backend: POST /api/v1/orders
      const response = await orderService.createOrder(orderPayload)
      const createdOrder = response?.data || response

      // Kiểm tra phản hồi hợp lệ từ backend có ID đơn hàng
      if (!createdOrder || !createdOrder.id) {
        throw new Error('Dữ liệu đơn hàng phản hồi từ máy chủ không hợp lệ.')
      }

      // Xử lý theo phương thức thanh toán
      if (formData.paymentMethod === 'VNPAY' || formData.paymentMethod === 'SIMULATED') {
        let vnpData = null
        try {
          const vnpRes = await paymentService.createVNPayPayment(
            createdOrder.id,
            vnpayBankCode,
            `Thanh toan don hang #${createdOrder.orderCode || createdOrder.id}`
          )
          vnpData = vnpRes?.data || vnpRes
        } catch (vnpErr) {
          console.warn('Lỗi khi gọi API tạo URL VNPay:', vnpErr)
        }

        // Xóa giỏ hàng và điều hướng sang trang Chi tiết đơn hàng để quét mã QR thanh toán
        clearCart()
        navigate(`/orders/${createdOrder.id}`, {
          state: {
            orderSuccess: true,
            justCreated: true,
            openVNPayQR: true,
            vnpayPaymentData: vnpData,
            orderCode: createdOrder.orderCode,
            totalAmount: createdOrder.totalAmount,
          },
          replace: true,
        })
        return
      } else {
        // Luồng COD: Đặt hàng thành công -> Xóa sạch giỏ hàng
        clearCart()

        // Chuyển hướng sang trang chi tiết đơn hàng kèm state chúc mừng và mã đơn ORD-...
        navigate(`/orders/${createdOrder.id}`, {
          state: {
            orderSuccess: true,
            justCreated: true,
            orderCode: createdOrder.orderCode,
            totalAmount: createdOrder.totalAmount,
          },
          replace: true,
        })
      }
    } catch (err) {
      // Giữ nguyên giỏ hàng khi thất bại, phân tích chi tiết mã lỗi từ Backend
      let message = 'Không thể tạo đơn hàng. Vui lòng thử lại.'

      if (err.response) {
        const { status, data } = err.response
        if (status === 401) {
          message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
          navigate('/login', { state: { from: '/checkout' } })
        } else if (status === 403) {
          message = 'Bạn không có quyền thực hiện đặt hàng với tài khoản này.'
        } else if (status === 400) {
          if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) {
            const fieldErrors = Object.values(data.data).join('; ')
            message = fieldErrors || data?.message || 'Dữ liệu đơn hàng không hợp lệ.'
          } else {
            message = data?.message || 'Thông tin đơn hàng không hợp lệ. Vui lòng kiểm tra lại.'
          }
        } else if (status === 404) {
          message = data?.message || 'Một hoặc nhiều sản phẩm trong giỏ hàng không còn tồn tại.'
        } else if (status === 409) {
          message = data?.message || 'Xung đột trạng thái đơn hàng hoặc kho hàng. Vui lòng thử lại.'
        } else if (status === 500) {
          message = data?.message || 'Lỗi hệ thống máy chủ khi xử lý đơn hàng. Vui lòng thử lại sau.'
        } else {
          message = data?.message || `Lỗi yêu cầu (HTTP ${status}). Vui lòng thử lại.`
        }
      } else if (err.request) {
        message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.'
      } else if (err.message) {
        message = err.message
      }

      setErrorMessage(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const totalPrice = getTotalPrice()
  const totalCount = getTotalCount()
  const discountAmount = appliedCoupon?.discountAmount || 0
  const finalPrice = Math.max(0, totalPrice - discountAmount)

  if (items.length === 0) {
    return null
  }

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/60 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-8">
          <Link to="/" className="hover:text-blue-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/cart" className="hover:text-blue-600 transition-colors">
            Giỏ hàng
          </Link>
          <span>/</span>
          <span className="font-bold text-slate-900">Thanh toán</span>
        </nav>

        {/* Header */}
        <div className="mb-8 pb-5 border-b border-slate-200">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold uppercase tracking-wider mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>SECURE CHECKOUT</span>
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">
            Xác nhận thông tin & Đặt hàng
          </h1>
          <p className="text-slate-500 text-sm mt-1">
            Kiểm tra thông tin giao nhận và chọn phương thức thanh toán trước khi xác nhận.
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-800 rounded-2xl flex items-center justify-between text-xs sm:text-sm shadow-xs animate-in fade-in duration-200">
            <div className="flex items-center gap-2.5">
              <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
              <span className="font-semibold">{errorMessage}</span>
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-700 hover:text-red-950 font-bold text-xs px-3 py-1 rounded-full hover:bg-red-100 cursor-pointer"
            >
              Đóng
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Recipient info & Payment methods (7/12) */}
            <div className="lg:col-span-7 space-y-6">

              {/* Card thông tin giao hàng */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 space-y-5">
                <div className="flex items-center gap-3 pb-4 border-b border-slate-100">
                  <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                    1
                  </span>
                  <h2 className="text-base font-bold text-slate-900">Thông tin người nhận</h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-900 block">
                      Họ và tên người nhận <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        name="recipientName"
                        value={formData.recipientName}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        placeholder="Nguyễn Văn A"
                        required
                        className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-900 block">
                      Số điện thoại <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="tel"
                        name="recipientPhone"
                        value={formData.recipientPhone}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        placeholder="0912345678"
                        required
                        className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                      />
                    </div>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-900 block">
                    Địa chỉ nhận hàng chi tiết <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                    <textarea
                      name="shippingAddress"
                      value={formData.shippingAddress}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      rows="2"
                      placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                      required
                      className="flex min-h-[70px] w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-2 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 resize-none disabled:bg-slate-100"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-900 block">
                    Ghi chú đơn hàng (Tùy chọn)
                  </label>
                  <div className="relative">
                    <FileText className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      name="note"
                      value={formData.note}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      placeholder="Giao giờ hành chính, gọi trước khi giao..."
                      className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                    />
                  </div>
                </div>
              </div>

              {/* Card phương thức thanh toán */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 space-y-5">
                <div className="flex items-center gap-3 pb-4 border-b border-slate-100">
                  <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                    2
                  </span>
                  <h2 className="text-base font-bold text-slate-900">Phương thức thanh toán</h2>
                </div>

                <div className="space-y-3.5">
                  <label
                    className={`flex items-start gap-3.5 p-4 rounded-2xl border transition-all cursor-pointer ${formData.paymentMethod === 'COD'
                      ? 'border-blue-600 bg-blue-50/50 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300'
                      } ${isSubmitting ? 'pointer-events-none opacity-70' : ''}`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={formData.paymentMethod === 'COD'}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      className="mt-0.5 text-blue-600 focus:ring-blue-500 cursor-pointer"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <Banknote className="w-4 h-4 text-blue-600" />
                        <span className="font-bold text-sm text-slate-900">Thanh toán khi nhận hàng (COD)</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Thanh toán tiền mặt cho nhân viên giao hàng khi nhận được kiện hàng.
                      </p>
                    </div>
                  </label>

                  {/* Cổng thanh toán VNPay Sandbox */}
                  <div
                    className={`rounded-2xl border transition-all ${formData.paymentMethod === 'VNPAY'
                      ? 'border-blue-600 bg-blue-50/50 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300'
                      }`}
                  >
                    <label
                      className={`flex items-start gap-3.5 p-4 cursor-pointer ${isSubmitting ? 'pointer-events-none opacity-70' : ''
                        }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="VNPAY"
                        checked={formData.paymentMethod === 'VNPAY'}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        className="mt-0.5 text-blue-600 focus:ring-blue-500 cursor-pointer"
                      />
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <CreditCard className="w-4 h-4 text-red-600" />
                          <span className="font-bold text-sm text-slate-900">
                            Cổng thanh toán VNPay (Sandbox)
                          </span>
                          <span className="text-[10px] uppercase font-extrabold tracking-wider px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                            Khuyên dùng
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">
                          Thanh toán an toàn qua Cổng VNPay (Thẻ ATM nội địa, Internet Banking, Quét mã QR VNPay).
                        </p>
                      </div>
                    </label>

                    {formData.paymentMethod === 'VNPAY' && (
                      <div className="px-4 pb-4 pt-2 border-t border-blue-200/60 pl-11 space-y-2">
                        <span className="text-[11px] font-bold text-slate-700 block uppercase tracking-wider">
                          Chọn ngân hàng kiểm thử (tùy chọn):
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                          <label
                            className={`flex items-center gap-2 text-xs p-2.5 rounded-xl border cursor-pointer transition-colors ${!vnpayBankCode
                              ? 'border-blue-500 bg-blue-100/70 text-blue-900 font-bold'
                              : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                              }`}
                          >
                            <input
                              type="radio"
                              name="vnpayBank"
                              checked={!vnpayBankCode}
                              onChange={() => setVnpayBankCode('')}
                              disabled={isSubmitting}
                              className="text-blue-600"
                            />
                            <span>Tất cả / Quét QR</span>
                          </label>

                          <label
                            className={`flex items-center gap-2 text-xs p-2.5 rounded-xl border cursor-pointer transition-colors ${vnpayBankCode === 'NCB'
                              ? 'border-blue-500 bg-blue-100/70 text-blue-900 font-bold'
                              : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                              }`}
                          >
                            <input
                              type="radio"
                              name="vnpayBank"
                              checked={vnpayBankCode === 'NCB'}
                              onChange={() => setVnpayBankCode('NCB')}
                              disabled={isSubmitting}
                              className="text-blue-600"
                            />
                            <span>Ngân hàng NCB</span>
                          </label>

                          <label
                            className={`flex items-center gap-2 text-xs p-2.5 rounded-xl border cursor-pointer transition-colors ${vnpayBankCode === 'VNPAYQR'
                              ? 'border-blue-500 bg-blue-100/70 text-blue-900 font-bold'
                              : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                              }`}
                          >
                            <input
                              type="radio"
                              name="vnpayBank"
                              checked={vnpayBankCode === 'VNPAYQR'}
                              onChange={() => setVnpayBankCode('VNPAYQR')}
                              disabled={isSubmitting}
                              className="text-blue-600"
                            />
                            <span>VNPAY QR</span>
                          </label>
                        </div>
                      </div>
                    )}
                  </div>

                  <div
                    className={`rounded-2xl border transition-all ${formData.paymentMethod === 'SIMULATED'
                      ? 'border-blue-600 bg-blue-50/50 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300'
                      }`}
                  >
                    <label
                      className={`flex items-start gap-3.5 p-4 cursor-pointer ${isSubmitting ? 'pointer-events-none opacity-70' : ''
                        }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="SIMULATED"
                        checked={formData.paymentMethod === 'SIMULATED'}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        className="mt-0.5 text-blue-600 focus:ring-blue-500 cursor-pointer"
                      />
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <CreditCard className="w-4 h-4 text-blue-600" />
                          <span className="font-bold text-sm text-slate-900">Thanh toán trực tuyến mô phỏng</span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">
                          Cổng thanh toán giả lập sandbox (sinh mã giao dịch PAY- tự động).
                        </p>
                      </div>
                    </label>

                    {formData.paymentMethod === 'SIMULATED' && (
                      <div className="px-4 pb-4 pt-2 border-t border-blue-200/60 pl-11 space-y-2">
                        <span className="text-[11px] font-bold text-slate-700 block uppercase tracking-wider">
                          Kịch bản thử nghiệm:
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                          <label className="flex items-center gap-2 text-xs text-slate-800 cursor-pointer p-2.5 rounded-xl border border-emerald-200 bg-emerald-50/60 hover:bg-emerald-100/60 transition-colors">
                            <input
                              type="radio"
                              name="simulateSuccessOption"
                              checked={simulateSuccessOption === true}
                              onChange={() => setSimulateSuccessOption(true)}
                              disabled={isSubmitting}
                              className="text-emerald-600"
                            />
                            <span className="font-bold text-emerald-800">✓ Thành công (PAID)</span>
                          </label>
                          <label className="flex items-center gap-2 text-xs text-slate-800 cursor-pointer p-2.5 rounded-xl border border-red-200 bg-red-50/60 hover:bg-red-100/60 transition-colors">
                            <input
                              type="radio"
                              name="simulateSuccessOption"
                              checked={simulateSuccessOption === false}
                              onChange={() => setSimulateSuccessOption(false)}
                              disabled={isSubmitting}
                              className="text-red-600"
                            />
                            <span className="font-bold text-red-700">✗ Thất bại (FAILED)</span>
                          </label>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: Order summary (5/12) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 space-y-5 sticky top-24">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <div className="flex items-center gap-2">
                    <ShoppingBag className="w-4 h-4 text-blue-600" />
                    <h2 className="text-base font-bold text-slate-900">Đơn hàng của bạn</h2>
                  </div>
                  <span className="text-xs font-bold px-3 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                    {totalCount} sản phẩm
                  </span>
                </div>

                {/* Danh sách sản phẩm mua */}
                <div className="max-h-64 overflow-y-auto divide-y divide-slate-100 pr-1">
                  {items.map((item) => (
                    <div key={item.id} className="py-3 flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-11 h-11 rounded-xl bg-slate-50 shrink-0 overflow-hidden border border-slate-200 p-1 flex items-center justify-center">
                          {item.imageUrl ? (
                            <img src={item.imageUrl} alt={item.name} className="w-full h-full object-contain mix-blend-multiply" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs font-bold">
                              SP
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-slate-900 truncate">{item.name}</p>
                          <p className="text-[11px] text-slate-500">
                            {formatPrice(item.price)} × {item.quantity}
                          </p>
                        </div>
                      </div>
                      <div className="text-xs font-extrabold text-slate-900 shrink-0 tabular-nums">
                        {formatPrice(item.price * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Mã giảm giá / Voucher */}
                <div className="pt-3 border-t border-slate-100">
                  <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mb-2">
                    <Tag className="w-3.5 h-3.5 text-blue-600" />
                    <span>Mã giảm giá / Voucher</span>
                  </label>
                  {appliedCoupon ? (
                    <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-extrabold text-emerald-800 tracking-wide">{appliedCoupon.code}</span>
                          <span className="px-2 py-0.5 rounded-full bg-emerald-200 text-emerald-800 text-[10px] font-bold">
                            -{formatPrice(appliedCoupon.discountAmount)}
                          </span>
                        </div>
                        <p className="text-[11px] text-emerald-700 mt-0.5">{appliedCoupon.message}</p>
                      </div>
                      <button
                        type="button"
                        onClick={handleRemoveCoupon}
                        className="text-slate-400 hover:text-red-600 p-1 font-bold text-xs cursor-pointer"
                        title="Gỡ mã giảm giá"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-1.5">
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={couponCodeInput}
                          onChange={(e) => {
                            setCouponCodeInput(e.target.value.toUpperCase())
                            if (couponError) setCouponError(null)
                          }}
                          disabled={isValidatingCoupon || isSubmitting}
                          placeholder="Nhập mã (VD: WELCOME10)"
                          className="flex-1 h-9 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs uppercase font-semibold tracking-wider placeholder:normal-case placeholder:font-normal placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white"
                        />
                        <button
                          type="button"
                          onClick={handleApplyCoupon}
                          disabled={isValidatingCoupon || isSubmitting || !couponCodeInput.trim()}
                          className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 active:scale-95 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-xs transition-all shrink-0 cursor-pointer flex items-center gap-1"
                        >
                          {isValidatingCoupon ? <LoadingSpinner size="xs" className="text-white" /> : 'Áp dụng'}
                        </button>
                      </div>
                      {couponError && (
                        <p className="text-[11px] text-red-600 font-medium flex items-center gap-1">
                          <AlertCircle className="w-3 h-3 shrink-0" />
                          <span>{couponError}</span>
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Chi phí */}
                <div className="pt-3 border-t border-slate-100 space-y-2.5 text-xs">
                  <div className="flex justify-between text-slate-600">
                    <span>Tạm tính</span>
                    <span className="font-bold text-slate-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  {appliedCoupon && (
                    <div className="flex justify-between text-emerald-700">
                      <span>Giảm giá voucher ({appliedCoupon.code})</span>
                      <span className="font-bold tabular-nums">-{formatPrice(appliedCoupon.discountAmount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-slate-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-bold text-emerald-600">Miễn phí toàn quốc</span>
                  </div>
                  <div className="pt-3 border-t border-slate-100 flex justify-between text-sm">
                    <span className="font-bold text-slate-900">Tổng thanh toán</span>
                    <span className="font-extrabold text-slate-900 text-xl tabular-nums">{formatPrice(finalPrice)}</span>
                  </div>
                </div>

                {/* Submit button: Vibrant Orange Pill */}
                <button
                  type="submit"
                  disabled={isSubmitting || items.length === 0}
                  className="w-full py-3.5 px-6 bg-orange-500 hover:bg-orange-600 active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none text-white font-bold text-sm rounded-full shadow-md shadow-orange-500/25 transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>Đang xử lý đặt hàng...</span>
                    </>
                  ) : (
                    <>
                      <Lock className="w-4 h-4" />
                      <span>Xác nhận đặt hàng</span>
                    </>
                  )}
                </button>

                <div className="text-center text-[11px] text-slate-400 leading-relaxed">
                  Bằng việc hoàn tất đơn, bạn xác nhận đồng ý với điều khoản mua sắm và chính sách bảo hành của Rikkei Store.
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
