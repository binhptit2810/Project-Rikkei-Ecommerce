import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
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
    if (!isAuthenticated) {
      navigate('/login?redirect=/checkout')
      return
    }

    navigate('/checkout')
  }

  const totalCount = getTotalCount()
  const totalPrice = getTotalPrice()

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
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
          <span className="font-semibold text-slate-800">Giỏ hàng</span>
        </nav>

        {/* Page Header */}
        <div className="flex items-center justify-between mb-8 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <span>Giỏ hàng của bạn</span>
              {totalCount > 0 && (
                <span className="text-sm font-semibold bg-orange-100 text-orange-700 px-3 py-1 rounded-full">
                  {totalCount} sản phẩm
                </span>
              )}
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Xem lại danh sách sản phẩm đã chọn trước khi tiến hành thanh toán.
            </p>
          </div>

          {items.length > 0 && (
            <button
              onClick={() => {
                if (window.confirm('Bạn có chắc chắn muốn xóa toàn bộ sản phẩm trong giỏ hàng?')) {
                  clearCart()
                }
              }}
              className="text-xs font-semibold text-slate-500 hover:text-red-600 hover:bg-red-50 px-3 py-2 rounded-lg transition-colors cursor-pointer"
            >
              Xóa tất cả
            </button>
          )}
        </div>

        {/* Notice */}
        {checkoutNotice && (
          <div className="mb-6 p-4 bg-amber-50 border border-amber-200 text-amber-900 rounded-2xl flex items-center justify-between text-sm shadow-sm">
            <div className="flex items-center gap-2.5">
              <span className="text-xl">ℹ️</span>
              <span>{checkoutNotice}</span>
            </div>
            <button
              onClick={() => setCheckoutNotice(null)}
              className="text-amber-700 hover:text-amber-950 font-bold text-xs cursor-pointer"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Cart Contents */}
        {items.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-slate-300 p-12 text-center max-w-xl mx-auto my-12 shadow-sm">
            <div className="w-20 h-20 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center text-4xl mx-auto mb-4">
              🛒
            </div>
            <h2 className="text-xl font-bold text-slate-800 mb-2">Giỏ hàng của bạn đang trống</h2>
            <p className="text-slate-500 text-sm mb-6 max-w-sm mx-auto">
              Chưa có sản phẩm nào được chọn. Hãy khám phá hàng trăm sản phẩm hấp dẫn của chúng tôi!
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-3 bg-orange-600 hover:bg-orange-700 active:scale-95 text-white font-bold text-sm rounded-xl shadow-md shadow-orange-600/20 transition-all cursor-pointer"
            >
              <span>Mua sắm ngay</span>
              <span>&rarr;</span>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
            {/* Products List */}
            <div className="lg:col-span-2 space-y-4">
              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden divide-y divide-slate-100">
                {items.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 hover:bg-slate-50/50 transition-colors"
                  >
                    {/* Item Details */}
                    <div className="flex items-center gap-4 min-w-0 flex-1">
                      <Link
                        to={`/products/${item.id}`}
                        className="w-18 h-18 sm:w-20 sm:h-20 rounded-xl overflow-hidden bg-slate-100 flex-shrink-0 border border-slate-200"
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
                          <div className="w-full h-full flex items-center justify-center text-slate-400 text-2xl">
                            🛍️
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
                        <div className="text-orange-600 font-bold text-sm mt-0.5">
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
                      <div className="flex items-center border border-slate-200 rounded-xl bg-white overflow-hidden shadow-xs">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity - 1)}
                          className="px-2.5 py-1.5 text-slate-600 hover:bg-slate-100 font-bold text-sm cursor-pointer"
                          aria-label="Giảm số lượng"
                        >
                          -
                        </button>
                        <span className="px-3 py-1.5 text-xs font-bold text-slate-800 min-w-[32px] text-center select-none">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          disabled={item.stockQuantity && item.quantity >= item.stockQuantity}
                          className="px-2.5 py-1.5 text-slate-600 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed font-bold text-sm cursor-pointer"
                          aria-label="Tăng số lượng"
                        >
                          +
                        </button>
                      </div>

                      {/* Line Item Total */}
                      <div className="text-right min-w-[90px]">
                        <div className="text-sm font-black text-slate-900">
                          {formatPrice((item.price || 0) * item.quantity)}
                        </div>
                      </div>

                      {/* Remove Button */}
                      <button
                        type="button"
                        onClick={() => removeFromCart(item.id)}
                        className="text-slate-400 hover:text-red-600 p-2 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                        title="Xóa khỏi giỏ hàng"
                        aria-label="Xóa món hàng"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                          />
                        </svg>
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {/* Navigation Back */}
              <div className="flex justify-between items-center pt-2">
                <Link
                  to="/products"
                  className="text-sm font-semibold text-orange-600 hover:text-orange-700 inline-flex items-center gap-1.5"
                >
                  <span>&larr;</span> Tiếp tục xem thêm sản phẩm
                </Link>
              </div>
            </div>

            {/* Order Summary Sidebar */}
            <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 space-y-6">
              <h2 className="text-lg font-bold text-slate-900 pb-4 border-b border-slate-100">
                Tóm tắt đơn hàng
              </h2>

              <div className="space-y-3 text-sm">
                <div className="flex justify-between text-slate-600">
                  <span>Tạm tính ({totalCount} sản phẩm)</span>
                  <span className="font-semibold text-slate-800">{formatPrice(totalPrice)}</span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Phí vận chuyển</span>
                  <span className="font-semibold text-emerald-600">Miễn phí</span>
                </div>
                <div className="flex justify-between text-slate-600">
                  <span>Giảm giá</span>
                  <span className="font-semibold text-slate-800">0 ₫</span>
                </div>

                <div className="pt-4 border-t border-slate-100 flex justify-between items-baseline">
                  <span className="text-base font-bold text-slate-900">Tổng cộng</span>
                  <div className="text-right">
                    <span className="text-2xl font-black text-orange-600 tracking-tight">
                      {formatPrice(totalPrice)}
                    </span>
                    <div className="text-[11px] text-slate-400 mt-0.5">(Đã bao gồm VAT nếu có)</div>
                  </div>
                </div>
              </div>

              {/* Checkout Button */}
              <button
                type="button"
                onClick={handleCheckout}
                className="w-full py-3.5 px-6 bg-orange-600 hover:bg-orange-700 active:scale-95 text-white font-bold text-base rounded-2xl shadow-lg shadow-orange-600/25 transition-all cursor-pointer flex items-center justify-center gap-2"
              >
                <span>💳</span>
                <span>Tiến hành đặt hàng</span>
              </button>

              {/* Trust Badges */}
              <div className="pt-4 border-t border-slate-100 space-y-2 text-xs text-slate-500">
                <div className="flex items-center gap-2">
                  <span className="text-emerald-500 font-bold">✓</span>
                  <span>Miễn phí giao hàng cho mọi đơn từ 500k</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-emerald-500 font-bold">✓</span>
                  <span>Đổi trả dễ dàng trong vòng 7 ngày</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-emerald-500 font-bold">✓</span>
                  <span>Bảo mật thông tin thanh toán 100%</span>
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
