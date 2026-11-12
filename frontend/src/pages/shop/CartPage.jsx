import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ShoppingCart,
  ShoppingBag,
  CreditCard,
  Trash2,
  Minus,
  Plus,
  ArrowRight,
  ArrowLeft,
  ShieldCheck,
  Truck,
  RotateCcw,
  Info,
  Zap,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'

export const CartPage = () => {
  const navigate = useNavigate()
  const { items, updateQuantity, removeFromCart, clearCart, getTotalPrice, getTotalCount } =
    useCartStore()
  const { isAuthenticated, user } = useAuthStore()

  const [checkoutNotice, setCheckoutNotice] = useState(null)

  React.useEffect(() => {
    // Chặn Quản trị viên truy cập hoặc đặt hàng tại giỏ hàng
    if (user?.role === 'ADMIN') {
      navigate('/admin/dashboard', { replace: true })
      return
    }

    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: '/cart' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để xem và quản lý giỏ hàng.',
        },
      })
    }
  }, [isAuthenticated, user, navigate])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleCheckout = () => {
    // Chặn Quản trị viên đặt hàng
    if (user?.role === 'ADMIN') {
      setCheckoutNotice('Tài khoản Quản trị viên (ADMIN) không được phép đặt hàng.')
      return
    }

    // Validate mọi mặt hàng: 1 <= quantity <= stockQuantity
    for (const item of items) {
      if (!item.quantity || item.quantity <= 0) {
        setCheckoutNotice(`Sản phẩm "${item.name}" có số lượng không hợp lệ. Vui lòng cập nhật lại.`)
        return
      }
      if (item.stockQuantity !== undefined && item.stockQuantity !== null && item.quantity > item.stockQuantity) {
        setCheckoutNotice(
          `Sản phẩm "${item.name}" chỉ còn ${item.stockQuantity} sản phẩm trong kho. Vui lòng giảm số lượng đặt hàng.`
        )
        return
      }
    }

    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: '/checkout' } } })
      return
    }

    navigate('/checkout')
  }

  const totalCount = getTotalCount()
  const totalPrice = getTotalPrice()

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/60 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-8">
          <Link to="/" className="hover:text-blue-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/products" className="hover:text-blue-600 transition-colors">
            Sản phẩm
          </Link>
          <span>/</span>
          <span className="font-bold text-slate-900">Giỏ hàng ({totalCount})</span>
        </nav>

        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 pb-5 border-b border-slate-200">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-3">
              <span>Giỏ hàng của bạn</span>
              {totalCount > 0 && (
                <span className="text-xs font-bold bg-blue-50 text-blue-700 px-3 py-1 rounded-full border border-blue-200 tabular-nums">
                  {totalCount} sản phẩm
                </span>
              )}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Kiểm tra các sản phẩm công nghệ đã chọn trước khi thanh toán.
            </p>
          </div>

          {items.length > 0 && (
            <button
              onClick={() => {
                if (window.confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?')) {
                  clearCart()
                }
              }}
              className="inline-flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-red-600 hover:bg-red-50 px-4 py-2 rounded-full border border-slate-200 hover:border-red-200 transition-colors cursor-pointer self-start sm:self-auto bg-white shadow-xs"
            >
              <Trash2 className="w-4 h-4" />
              <span>Xóa toàn bộ giỏ</span>
            </button>
          )}
        </div>

        {/* Notice Banner */}
        {checkoutNotice && (
          <div className="mb-6 p-4 bg-amber-50 border border-amber-200 text-amber-900 rounded-2xl flex items-center justify-between text-xs sm:text-sm shadow-xs">
            <div className="flex items-center gap-2.5">
              <Info className="w-5 h-5 text-amber-600 shrink-0" />
              <span className="font-medium">{checkoutNotice}</span>
            </div>
            <button
              onClick={() => setCheckoutNotice(null)}
              className="text-amber-700 hover:text-amber-950 font-bold text-xs cursor-pointer px-3 py-1 rounded-full hover:bg-amber-100"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Cart Contents */}
        {items.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-slate-200 p-16 text-center max-w-xl mx-auto my-12 shadow-xs">
            <div className="w-16 h-16 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <ShoppingCart className="w-8 h-8 stroke-[1.6]" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-1">Giỏ hàng của bạn đang trống</h2>
            <p className="text-slate-500 text-xs sm:text-sm mb-6 max-w-sm mx-auto leading-relaxed">
              Bạn chưa thêm thiết bị công nghệ nào vào giỏ. Hãy khám phá kho sản phẩm chính hãng Rikkei Store ngay hôm nay!
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-full shadow-md shadow-blue-500/20 transition-all hover:scale-105 active:scale-95 cursor-pointer"
            >
              <span>Khám phá sản phẩm ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Products List (8/12) */}
            <div className="lg:col-span-8 space-y-4">
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden divide-y divide-slate-100">
                {items.map((item) => (
                  <div
                    key={`${item.id}-${item.selectedSize || 'default'}`}
                    className="p-5 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-5 hover:bg-slate-50/60 transition-colors"
                  >
                    {/* Item Details */}
                    <div className="flex items-center gap-4 min-w-0 flex-1">
                      <Link
                        to={`/products/${item.id}`}
                        className="w-18 h-18 sm:w-22 sm:h-22 rounded-2xl overflow-hidden bg-slate-50 shrink-0 border border-slate-200 p-2 flex items-center justify-center"
                      >
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.name}
                            className="w-full h-full object-contain mix-blend-multiply"
                            onError={(e) => {
                              e.currentTarget.onerror = null
                              e.currentTarget.src =
                                'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200&auto=format&fit=crop&q=80'
                            }}
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-slate-300">
                            <ShoppingBag className="w-7 h-7 stroke-[1.25]" />
                          </div>
                        )}
                      </Link>

                      <div className="min-w-0 flex-1 space-y-1">
                        <Link
                          to={`/products/${item.id}`}
                          className="font-bold text-slate-900 text-sm hover:text-blue-600 line-clamp-1 transition-colors"
                        >
                          {item.name}
                        </Link>
                        {item.selectedSize && (
                          <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-blue-50 border border-blue-200 text-blue-700 text-xs font-semibold">
                            <span>Kích cỡ:</span>
                            <span className="font-extrabold">{item.selectedSize}</span>
                          </div>
                        )}
                        <div className="text-slate-900 font-extrabold text-sm tabular-nums">
                          {formatPrice(item.price)}
                        </div>
                        {item.stockQuantity !== undefined && (
                          <div className="text-[11px] text-slate-400">
                            Tồn kho khả dụng: <span className="font-semibold text-slate-600">{item.stockQuantity}</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Quantity & Controls */}
                    <div className="flex items-center justify-between sm:justify-end w-full sm:w-auto gap-5">
                      {/* Quantity Selector */}
                      <div className="flex items-center border border-slate-200 rounded-full bg-slate-50 overflow-hidden shadow-xs">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity - 1, item.selectedSize)}
                          className="p-2 text-slate-600 hover:bg-white transition-colors cursor-pointer"
                          aria-label="Giảm số lượng"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="px-3 py-1 text-xs font-bold text-slate-900 min-w-[32px] text-center select-none tabular-nums">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity + 1, item.selectedSize)}
                          disabled={item.stockQuantity && item.quantity >= item.stockQuantity}
                          className="p-2 text-slate-600 hover:bg-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                          aria-label="Tăng số lượng"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* Line Item Total */}
                      <div className="text-right min-w-[110px]">
                        <div className="text-base font-extrabold text-slate-900 tabular-nums">
                          {formatPrice((item.price || 0) * item.quantity)}
                        </div>
                      </div>

                      {/* Remove Button */}
                      <button
                        type="button"
                        onClick={() => removeFromCart(item.id, item.selectedSize)}
                        className="text-slate-400 hover:text-red-600 p-2 rounded-full hover:bg-red-50 transition-colors cursor-pointer"
                        title="Xóa khỏi giỏ hàng"
                        aria-label="Xóa món hàng"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Navigation Back */}
              <div className="flex justify-between items-center pt-2">
                <Link
                  to="/products"
                  className="text-xs font-bold text-slate-600 hover:text-blue-600 inline-flex items-center gap-1.5 transition-colors"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Tiếp tục chọn thêm sản phẩm khác</span>
                </Link>
              </div>
            </div>

            {/* Order Summary Sidebar (4/12) */}
            <div className="lg:col-span-4 space-y-6">
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 space-y-5 sticky top-24">
                <h2 className="text-lg font-bold text-slate-900 pb-3 border-b border-slate-100 flex items-center justify-between">
                  <span>Tóm tắt đơn hàng</span>
                  <Zap className="w-4 h-4 text-orange-500 fill-current" />
                </h2>

                <div className="space-y-3.5 text-xs sm:text-sm">
                  <div className="flex justify-between text-slate-600">
                    <span>Tạm tính ({totalCount} món)</span>
                    <span className="font-bold text-slate-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-bold text-emerald-600">Miễn phí toàn quốc</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Voucher áp dụng</span>
                    <span className="font-bold text-slate-900">0 ₫</span>
                  </div>

                  <div className="pt-4 border-t border-slate-100 flex justify-between items-baseline">
                    <span className="text-sm font-bold text-slate-900">Tổng thanh toán</span>
                    <div className="text-right">
                      <span className="text-2xl font-extrabold text-slate-900 tabular-nums">
                        {formatPrice(totalPrice)}
                      </span>
                      <div className="text-[11px] text-slate-400 mt-0.5">(Đã bao gồm thuế GTGT)</div>
                    </div>
                  </div>
                </div>

                {/* Checkout Button: Royal Blue Pill */}
                <button
                  type="button"
                  onClick={handleCheckout}
                  className="w-full py-3.5 px-6 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-full shadow-md shadow-blue-500/20 transition-all hover:scale-[1.02] active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Tiến hành đặt hàng</span>
                </button>

                {/* Trust Badges */}
                <div className="pt-4 border-t border-slate-100 space-y-2.5 text-xs text-slate-500">
                  <div className="flex items-center gap-2.5">
                    <Truck className="w-4 h-4 text-blue-600 shrink-0" />
                    <span>Miễn phí giao hàng hỏa tốc trong 48h</span>
                  </div>
                  <div className="flex items-center gap-2.5">
                    <RotateCcw className="w-4 h-4 text-blue-600 shrink-0" />
                    <span>Đổi trả thuận tiện 7 ngày tận nhà</span>
                  </div>
                  <div className="flex items-center gap-2.5">
                    <ShieldCheck className="w-4 h-4 text-blue-600 shrink-0" />
                    <span>Thanh toán an toàn với xác thực mã OTP</span>
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

export default CartPage
