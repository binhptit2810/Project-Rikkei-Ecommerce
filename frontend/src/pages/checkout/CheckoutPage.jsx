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
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'
import orderService from '../../services/orderService'
import paymentService from '../../services/paymentService'
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
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

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
    if (!isSubmitting && items.length === 0) {
      navigate('/cart')
    }
  }, [items, isSubmitting, navigate])

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

    // Chống submit trùng lặp khi đang xử lý
    if (isSubmitting) return

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
      setErrorMessage('Vui lòng chọn phương thức thanh toán (COD hoặc SIMULATED)')
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
      if (formData.paymentMethod === 'SIMULATED') {
        try {
          const payRes = await paymentService.simulatePayment(
            createdOrder.id,
            simulateSuccessOption,
            simulateSuccessOption
              ? 'Thanh toán trực tuyến mô phỏng thành công (PAID)'
              : 'Thanh toán trực tuyến mô phỏng thất bại (FAILED)'
          )
          const paymentData = payRes?.data || payRes

          if (paymentData?.status === 'PAID') {
            // Thanh toán mô phỏng thành công -> Xóa sạch giỏ hàng
            clearCart()
            navigate(`/orders/${createdOrder.id}`, {
              state: {
                orderSuccess: true,
                paymentSuccess: true,
                justCreated: true,
                orderCode: createdOrder.orderCode,
                totalAmount: createdOrder.totalAmount,
              },
              replace: true,
            })
            return
          } else {
            // Thanh toán mô phỏng thất bại -> KHÔNG XÓA GIỎ HÀNG (DoD requirement)
            navigate(`/orders/${createdOrder.id}`, {
              state: {
                orderSuccess: true,
                paymentFailed: true,
                justCreated: true,
                orderCode: createdOrder.orderCode,
                totalAmount: createdOrder.totalAmount,
                paymentErrorMsg: 'Thanh toán mô phỏng thất bại (FAILED). Giỏ hàng của bạn đã được giữ nguyên và bạn có thể thử lại thanh toán bất kỳ lúc nào.',
              },
              replace: true,
            })
            return
          }
        } catch (payErr) {
          // Lỗi gọi API thanh toán -> KHÔNG XÓA GIỎ HÀNG
          navigate(`/orders/${createdOrder.id}`, {
            state: {
              orderSuccess: true,
              paymentFailed: true,
              justCreated: true,
              orderCode: createdOrder.orderCode,
              totalAmount: createdOrder.totalAmount,
              paymentErrorMsg: payErr?.response?.data?.message || 'Có lỗi khi xử lý thanh toán mô phỏng.',
            },
            replace: true,
          })
          return
        }
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
          // Lỗi validation Bean Validation từ backend (trả về map lỗi các trường)
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

  if (items.length === 0) {
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
          <div className="mb-6 p-3.5 bg-red-50 border border-red-200 text-red-800 rounded-md flex items-center justify-between text-xs sm:text-sm shadow-xs animate-in fade-in duration-200">
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
                        disabled={isSubmitting}
                        placeholder="Nguyễn Văn A"
                        required
                        className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 disabled:bg-zinc-100 disabled:opacity-70"
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
                        disabled={isSubmitting}
                        placeholder="0912345678"
                        required
                        className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 disabled:bg-zinc-100 disabled:opacity-70"
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
                      disabled={isSubmitting}
                      rows="2"
                      placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                      required
                      className="flex min-h-[60px] w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-2 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 resize-none disabled:bg-zinc-100 disabled:opacity-70"
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
                      disabled={isSubmitting}
                      placeholder="Giao giờ hành chính, gọi trước khi giao..."
                      className="flex h-9 w-full rounded-md border border-zinc-200 bg-white pl-9 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-zinc-400 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-zinc-950 disabled:bg-zinc-100 disabled:opacity-70"
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
                    } ${isSubmitting ? 'pointer-events-none opacity-70' : ''}`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={formData.paymentMethod === 'COD'}
                      onChange={handleChange}
                      disabled={isSubmitting}
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

                  <div
                    className={`rounded-md border transition-colors ${
                      formData.paymentMethod === 'SIMULATED'
                        ? 'border-zinc-900 bg-zinc-50 shadow-xs'
                        : 'border-zinc-200 hover:border-zinc-300'
                    }`}
                  >
                    <label
                      className={`flex items-start gap-3 p-3.5 cursor-pointer ${
                        isSubmitting ? 'pointer-events-none opacity-70' : ''
                      }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="SIMULATED"
                        checked={formData.paymentMethod === 'SIMULATED'}
                        onChange={handleChange}
                        disabled={isSubmitting}
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

                    {formData.paymentMethod === 'SIMULATED' && (
                      <div className="px-3.5 pb-3.5 pt-1 border-t border-zinc-200/80 pl-10 space-y-2">
                        <span className="text-[11px] font-semibold text-zinc-700 block uppercase tracking-wider">
                          Kịch bản mô phỏng kiểm thử:
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                          <label className="flex items-center gap-2 text-xs text-zinc-800 cursor-pointer p-2 rounded-md border border-zinc-200 bg-white hover:bg-zinc-100 transition-colors">
                            <input
                              type="radio"
                              name="simulateSuccessOption"
                              checked={simulateSuccessOption === true}
                              onChange={() => setSimulateSuccessOption(true)}
                              disabled={isSubmitting}
                              className="text-zinc-900"
                            />
                            <span className="font-semibold text-emerald-700">✓ Thành công (PAID)</span>
                          </label>
                          <label className="flex items-center gap-2 text-xs text-zinc-800 cursor-pointer p-2 rounded-md border border-zinc-200 bg-white hover:bg-zinc-100 transition-colors">
                            <input
                              type="radio"
                              name="simulateSuccessOption"
                              checked={simulateSuccessOption === false}
                              onChange={() => setSimulateSuccessOption(false)}
                              disabled={isSubmitting}
                              className="text-zinc-900"
                            />
                            <span className="font-semibold text-red-600">✗ Thất bại (FAILED)</span>
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

                {/* Submit button with Loading Spinner and Duplicate Submission Prevention */}
                <button
                  type="submit"
                  disabled={isSubmitting || items.length === 0}
                  className="w-full py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none text-zinc-50 font-medium text-sm rounded-md shadow-sm transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <LoadingSpinner size="sm" className="text-zinc-50" />
                      <span>Đang xử lý giao dịch...</span>
                    </>
                  ) : (
                    <>
                      <Lock className="w-4 h-4" />
                      <span>Xác nhận đặt hàng</span>
                    </>
                  )}
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
