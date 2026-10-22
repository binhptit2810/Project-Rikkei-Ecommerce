import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
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
    if (items.length === 0) {
      navigate('/cart')
    }
  }, [items, navigate])

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
      setErrorMessage('Vui lòng nhập họ tên người nhận')
      return
    }

    const phoneRegex = /^(0|\+84)[0-9]{9}$/
    if (!phoneRegex.test(formData.recipientPhone.trim())) {
      setErrorMessage('Số điện thoại không hợp lệ (yêu cầu 10 chữ số, ví dụ: 0912345678)')
      return
    }

    if (!formData.shippingAddress.trim()) {
      setErrorMessage('Vui lòng nhập địa chỉ nhận hàng')
      return
    }

    setIsSubmitting(true)
    setErrorMessage(null)

    try {
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

      const res = await orderService.createOrder(orderPayload)

      if (res.success && res.data) {
        // Xóa giỏ hàng sau khi đặt thành công
        clearCart()
        // Chuyển hướng đến trang chi tiết đơn kèm thông báo thành công
        navigate(`/orders/${res.data.id}?success=true`)
      } else {
        setErrorMessage(res.message || 'Đặt hàng không thành công, vui lòng thử lại!')
      }
    } catch (err) {
      console.error('Checkout error:', err)
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi xử lý đơn hàng. Vui lòng kiểm tra lại số lượng kho.'
      setErrorMessage(msg)
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
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-orange-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/cart" className="hover:text-orange-600 transition-colors">
            Giỏ hàng
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-800">Thanh toán & Đặt hàng</span>
        </nav>

        {/* Header */}
        <div className="mb-8 pb-4 border-b border-slate-200">
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
            <span>Xác nhận thông tin & Đặt hàng</span>
          </h1>
          <p className="text-slate-500 text-sm mt-1">
            Vui lòng kiểm tra kỹ địa chỉ giao hàng và phương thức thanh toán trước khi hoàn tất.
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-800 rounded-2xl flex items-center justify-between text-sm shadow-sm">
            <div className="flex items-center gap-3">
              <span className="text-xl">⚠️</span>
              <span>{errorMessage}</span>
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-700 hover:text-red-950 font-bold text-xs"
            >
              Đóng
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Cột trái: Thông tin giao hàng & Phương thức thanh toán (7/12) */}
            <div className="lg:col-span-7 space-y-6">
              {/* Card thông tin giao hàng */}
              <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-7 space-y-5">
                <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
                  <span className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-sm">
                    1
                  </span>
                  <h2 className="text-lg font-bold text-slate-900">Thông tin người nhận</h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                      Họ và tên người nhận <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      name="recipientName"
                      value={formData.recipientName}
                      onChange={handleChange}
                      placeholder="Ví dụ: Nguyễn Văn A"
                      required
                      className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-sm text-slate-800"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                      Số điện thoại <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="tel"
                      name="recipientPhone"
                      value={formData.recipientPhone}
                      onChange={handleChange}
                      placeholder="Ví dụ: 0912345678"
                      required
                      className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-sm text-slate-800"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Địa chỉ nhận hàng chi tiết <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    name="shippingAddress"
                    value={formData.shippingAddress}
                    onChange={handleChange}
                    rows="2"
                    placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố..."
                    required
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-sm text-slate-800"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Ghi chú đơn hàng (Tùy chọn)
                  </label>
                  <input
                    type="text"
                    name="note"
                    value={formData.note}
                    onChange={handleChange}
                    placeholder="Ví dụ: Giao giờ hành chính, gọi trước khi giao..."
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-sm text-slate-800"
                  />
                </div>
              </div>

              {/* Card phương thức thanh toán */}
              <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-7 space-y-5">
                <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
                  <span className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-sm">
                    2
                  </span>
                  <h2 className="text-lg font-bold text-slate-900">Phương thức thanh toán</h2>
                </div>

                <div className="space-y-3">
                  {/* COD */}
                  <label
                    className={`flex items-start gap-3.5 p-4 rounded-2xl border cursor-pointer transition-all ${
                      formData.paymentMethod === 'COD'
                        ? 'border-orange-500 bg-orange-50/40 shadow-xs'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={formData.paymentMethod === 'COD'}
                      onChange={handleChange}
                      className="mt-1 text-orange-600 focus:ring-orange-500 h-4 w-4"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2 font-bold text-sm text-slate-900">
                        <span>💵</span>
                        <span>Thanh toán khi nhận hàng (COD)</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">
                        Quý khách sẽ thanh toán tiền mặt trực tiếp cho nhân viên giao hàng khi nhận sản phẩm.
                      </p>
                    </div>
                  </label>

                  {/* SIMULATED */}
                  <label
                    className={`flex items-start gap-3.5 p-4 rounded-2xl border cursor-pointer transition-all ${
                      formData.paymentMethod === 'SIMULATED'
                        ? 'border-orange-500 bg-orange-50/40 shadow-xs'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="SIMULATED"
                      checked={formData.paymentMethod === 'SIMULATED'}
                      onChange={handleChange}
                      className="mt-1 text-orange-600 focus:ring-orange-500 h-4 w-4"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2 font-bold text-sm text-slate-900">
                        <span>💳</span>
                        <span>Thanh toán trực tuyến (Mô phỏng tức thì)</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">
                        Hệ thống sẽ mô phỏng giao dịch thanh toán thành công qua Thẻ quốc tế / Ví điện tử. Đơn hàng chuyển sang trạng thái Đã thanh toán.
                      </p>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            {/* Cột phải: Tóm tắt đơn hàng & Nút thanh toán (5/12) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 space-y-6 sticky top-24">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <h2 className="text-lg font-bold text-slate-900">Sản phẩm đặt mua</h2>
                  <span className="text-xs font-semibold bg-orange-100 text-orange-700 px-2.5 py-1 rounded-full">
                    {totalCount} món
                  </span>
                </div>

                {/* Danh sách items tóm tắt */}
                <div className="space-y-3.5 max-h-72 overflow-y-auto pr-1 divide-y divide-slate-100">
                  {items.map((item) => (
                    <div key={item.id} className="pt-3.5 first:pt-0 flex items-center gap-3">
                      <div className="w-14 h-14 rounded-xl overflow-hidden bg-slate-100 border border-slate-200 flex-shrink-0">
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-slate-400 text-lg">
                            🛍️
                          </div>
                        )}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="text-xs font-bold text-slate-800 line-clamp-1">
                          {item.name}
                        </div>
                        <div className="text-xs text-slate-500 mt-0.5">
                          {formatPrice(item.price)} × {item.quantity}
                        </div>
                      </div>
                      <div className="text-xs font-black text-slate-900 text-right">
                        {formatPrice((item.price || 0) * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Chi tiết tính tiền */}
                <div className="pt-4 border-t border-slate-100 space-y-2.5 text-sm">
                  <div className="flex justify-between text-slate-600">
                    <span>Tạm tính</span>
                    <span className="font-semibold text-slate-800">{formatPrice(totalPrice)}</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-semibold text-emerald-600">Miễn phí</span>
                  </div>
                  <div className="pt-3 border-t border-slate-100 flex justify-between items-baseline">
                    <span className="text-base font-bold text-slate-900">Tổng thanh toán</span>
                    <div className="text-right">
                      <span className="text-2xl font-black text-orange-600 tracking-tight">
                        {formatPrice(totalPrice)}
                      </span>
                      <div className="text-[11px] text-slate-400 mt-0.5">(Đã bao gồm VAT)</div>
                    </div>
                  </div>
                </div>

                {/* Nút đặt hàng */}
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full py-4 px-6 bg-orange-600 hover:bg-orange-700 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-base rounded-2xl shadow-lg shadow-orange-600/25 transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Đang xử lý tạo đơn...</span>
                    </>
                  ) : (
                    <>
                      <span>🚀</span>
                      <span>Hoàn tất đặt hàng</span>
                    </>
                  )}
                </button>

                <div className="text-center text-xs text-slate-400">
                  Bằng việc đặt hàng, bạn đồng ý với các điều khoản mua hàng và chính sách bảo mật của chúng tôi.
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
