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
  AlertCircle,
  Info,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'

export const CartPage = () => {
  const navigate = useNavigate()
  const { items, updateQuantity, removeFromCart, clearCart, getTotalPrice, getTotalCount } =
    useCartStore()
  const { isAuthenticated } = useAuthStore()

  const [checkoutNotice, setCheckoutNotice] = useState(null)

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleCheckout = () => {
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
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
          <Link to="/" className="hover:text-orange-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/products" className="hover:text-orange-600 transition-colors">
            Sản phẩm
          </Link>
          <span>/</span>
          <span className="font-semibold text-slate-900">Giỏ hàng ({totalCount})</span>
        </nav>

        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-3">
              <span>Giỏ hàng của bạn</span>
              {totalCount > 0 && (
                <span className="text-xs font-bold bg-orange-50 text-orange-700 px-3 py-1 rounded-full border border-orange-200/60 tabular-nums">
                  {totalCount} sản phẩm
                </span>
              )}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Kiểm tra các sản phẩm đã chọn trước khi hoàn tất thủ tục thanh toán.
            </p>
          </div>

          {items.length > 0 && (
            <button
              onClick={() => {
                if (window.confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?')) {
                  clearCart()
                }
              }}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-red-600 hover:bg-red-50 px-3 py-2 rounded-xl border border-slate-200 hover:border-red-200 transition-colors cursor-pointer self-start sm:self-auto bg-white"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Xóa tất cả</span>
            </button>
          )}
        </div>

        {/* Notice Banner */}
        {checkoutNotice && (
          <div className="mb-6 p-4 bg-amber-50 border border-amber-200 text-amber-900 rounded-2xl flex items-center justify-between text-sm shadow-2xs">
            <div className="flex items-center gap-2.5">
              <Info className="w-5 h-5 text-amber-600 shrink-0" />
              <span>{checkoutNotice}</span>
            </div>
            <button
              onClick={() => setCheckoutNotice(null)}
              className="text-amber-700 hover:text-amber-950 font-bold text-xs cursor-pointer px-2 py-1 rounded"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Cart Contents */}
        {items.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-slate-200 p-12 text-center max-w-xl mx-auto my-12 shadow-2xs">
            <div className="w-18 h-18 bg-orange-50 text-orange-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <ShoppingCart className="w-9 h-9 stroke-[1.5]" />
            </div>
            <h2 className="text-xl font-bold text-slate-900 mb-2">Giỏ hàng của bạn đang trống</h2>
            <p className="text-slate-500 text-sm mb-6 max-w-sm mx-auto leading-relaxed">
              Bạn chưa thêm sản phẩm nào vào giỏ. Hãy khám phá bộ sưu tập sản phẩm phong phú của chúng tôi!
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-3 bg-orange-600 hover:bg-orange-700 active:scale-[0.99] text-white font-bold text-sm rounded-xl shadow-2xs transition-all cursor-pointer"
            >
              <span>Khám phá sản phẩm ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Products List (8/12) */}
            <div className="lg:col-span-8 space-y-4">
              <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden divide-y divide-slate-100">
                {items.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 hover:bg-slate-50/50 transition-colors"
                  >
                    {/* Item Details */}
                    <div className="flex items-center gap-4 min-w-0 flex-1">
                      <Link
                        to={`/products/${item.id}`}
                        className="w-20 h-20 rounded-xl overflow-hidden bg-slate-100 flex-shrink-0 border border-slate-200/80"
                      >
                        {item.imageUrl ? (
                          <img
                            src={item.imageUrl}
                            alt={item.name}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.currentTarget.onerror = null
                              e.currentTarget.src =
                                'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=200&auto=format&fit=crop&q=80'
                            }}
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-slate-400">
                            <ShoppingBag className="w-8 h-8 stroke-1 text-slate-300" />
                          </div>
                        )}
                      </Link>

                      <div className="min-w-0 flex-1">
                        <Link
                          to={`/products/${item.id}`}
                          className="font-bold text-slate-900 text-sm sm:text-base hover:text-orange-600 line-clamp-1 transition-colors"
                        >
                          {item.name}
                        </Link>
                        <div className="text-orange-600 font-extrabold text-sm mt-0.5 tabular-nums">
                          {formatPrice(item.price)}
                        </div>
                        {item.stockQuantity !== undefined && (
                          <div className="text-xs text-slate-400 mt-1">
                            Còn lại trong kho: {item.stockQuantity}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Quantity & Controls */}
                    <div className="flex items-center justify-between sm:justify-end w-full sm:w-auto gap-4">
                      {/* Quantity Selector */}
                      <div className="flex items-center border border-slate-200 rounded-xl bg-white overflow-hidden shadow-2xs">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity - 1)}
                          className="p-2 text-slate-600 hover:bg-slate-50 transition-colors cursor-pointer"
                          aria-label="Giảm số lượng"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="px-3 py-1.5 text-xs font-bold text-slate-900 min-w-[32px] text-center select-none tabular-nums">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          disabled={item.stockQuantity && item.quantity >= item.stockQuantity}
                          className="p-2 text-slate-600 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                          aria-label="Tăng số lượng"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* Line Item Total */}
                      <div className="text-right min-w-[100px]">
                        <div className="text-sm font-extrabold text-slate-900 tabular-nums">
                          {formatPrice((item.price || 0) * item.quantity)}
                        </div>
                      </div>

                      {/* Remove Button */}
                      <button
                        type="button"
                        onClick={() => removeFromCart(item.id)}
                        className="text-slate-400 hover:text-red-600 p-2 rounded-xl hover:bg-red-50 transition-colors cursor-pointer"
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
                  className="text-xs font-semibold text-slate-600 hover:text-orange-600 inline-flex items-center gap-1.5 transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Tiếp tục chọn thêm sản phẩm khác</span>
                </Link>
              </div>
            </div>

            {/* Order Summary Sidebar (4/12) */}
            <div className="lg:col-span-4 space-y-6">
              <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs p-6 space-y-6 sticky top-24">
                <h2 className="text-base font-bold text-slate-900 pb-4 border-b border-slate-100">
                  Tóm tắt đơn hàng
                </h2>

                <div className="space-y-3.5 text-sm">
                  <div className="flex justify-between text-slate-600">
                    <span>Tạm tính ({totalCount} sản phẩm)</span>
                    <span className="font-semibold text-slate-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-semibold text-emerald-600">Miễn phí</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>Ưu đãi</span>
                    <span className="font-semibold text-slate-900">0 ₫</span>
                  </div>

                  <div className="pt-4 border-t border-slate-100 flex justify-between items-baseline">
                    <span className="text-base font-bold text-slate-900">Tổng cộng</span>
                    <div className="text-right">
                      <span className="text-2xl font-black text-orange-600 tracking-tight tabular-nums">
                        {formatPrice(totalPrice)}
                      </span>
                      <div className="text-[11px] text-slate-400 mt-0.5">(Đã bao gồm thuế GTGT)</div>
                    </div>
                  </div>
                </div>

                {/* Checkout Button */}
                <button
                  type="button"
                  onClick={handleCheckout}
                  className="w-full py-3.5 px-6 bg-orange-600 hover:bg-orange-700 active:scale-[0.99] text-white font-bold text-sm rounded-xl shadow-2xs transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Tiến hành đặt hàng</span>
                </button>

                {/* Trust Badges */}
                <div className="pt-4 border-t border-slate-100 space-y-2.5 text-xs text-slate-500">
                  <div className="flex items-center gap-2">
                    <Truck className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Miễn phí giao hàng cho mọi đơn từ 500k</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <RotateCcw className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Đổi trả thuận tiện trong vòng 7 ngày</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Bảo mật giao dịch tuyệt đối 100%</span>
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
