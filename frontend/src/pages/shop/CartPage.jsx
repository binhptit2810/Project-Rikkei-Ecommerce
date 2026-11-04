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
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'

export const CartPage = () => {
  const navigate = useNavigate()
  const { items, updateQuantity, removeFromCart, clearCart, getTotalPrice, getTotalCount } =
    useCartStore()
  const { isAuthenticated } = useAuthStore()

  const [checkoutNotice, setCheckoutNotice] = useState(null)

  React.useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: '/cart' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để xem và quản lý giỏ hàng.',
        },
      })
    }
  }, [isAuthenticated, navigate])

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
    <div className="min-h-[calc(100vh-140px)] bg-zinc-50/50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs text-zinc-500 mb-6">
          <Link to="/" className="hover:text-zinc-950 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          <Link to="/products" className="hover:text-zinc-950 transition-colors">
            Sản phẩm
          </Link>
          <span>/</span>
          <span className="font-medium text-zinc-900">Giỏ hàng ({totalCount})</span>
        </nav>

        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 pb-4 border-b border-zinc-200">
          <div>
            <h1 className="text-2xl sm:text-3xl font-semibold text-zinc-950 tracking-tight flex items-center gap-3">
              <span>Giỏ hàng của bạn</span>
              {totalCount > 0 && (
                <span className="text-xs font-medium bg-zinc-100 text-zinc-800 px-2.5 py-0.5 rounded-full border border-zinc-200 tabular-nums">
                  {totalCount} sản phẩm
                </span>
              )}
            </h1>
            <p className="text-zinc-500 text-sm mt-1">
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
              className="inline-flex items-center gap-1.5 text-xs font-medium text-zinc-600 hover:text-red-600 hover:bg-red-50 px-3 py-1.5 rounded-md border border-zinc-200 hover:border-red-200 transition-colors cursor-pointer self-start sm:self-auto bg-white shadow-xs"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Xóa tất cả</span>
            </button>
          )}
        </div>

        {/* Notice Banner */}
        {checkoutNotice && (
          <div className="mb-6 p-3.5 bg-zinc-100 border border-zinc-200 text-zinc-800 rounded-md flex items-center justify-between text-xs sm:text-sm shadow-xs">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-zinc-700 shrink-0" />
              <span>{checkoutNotice}</span>
            </div>
            <button
              onClick={() => setCheckoutNotice(null)}
              className="text-zinc-600 hover:text-zinc-950 font-medium text-xs cursor-pointer px-2 py-0.5 rounded"
            >
              Đóng
            </button>
          </div>
        )}

        {/* Cart Contents */}
        {items.length === 0 ? (
          <div className="bg-white rounded-lg border border-dashed border-zinc-200 p-12 text-center max-w-xl mx-auto my-12 shadow-xs">
            <div className="w-14 h-14 bg-zinc-100 text-zinc-700 rounded-lg flex items-center justify-center mx-auto mb-3 border border-zinc-200">
              <ShoppingCart className="w-7 h-7 stroke-[1.5]" />
            </div>
            <h2 className="text-lg font-semibold text-zinc-900 mb-1">Giỏ hàng của bạn đang trống</h2>
            <p className="text-zinc-500 text-xs sm:text-sm mb-6 max-w-sm mx-auto leading-relaxed">
              Bạn chưa thêm sản phẩm nào vào giỏ. Hãy khám phá bộ sưu tập sản phẩm phong phú của chúng tôi!
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 font-medium text-sm rounded-md shadow-sm transition-colors cursor-pointer"
            >
              <span>Khám phá sản phẩm ngay</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Products List (8/12) */}
            <div className="lg:col-span-8 space-y-4">
              <div className="bg-white rounded-lg border border-zinc-200 shadow-sm overflow-hidden divide-y divide-zinc-100">
                {items.map((item) => (
                  <div
                    key={item.id}
                    className="p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 hover:bg-zinc-50/50 transition-colors"
                  >
                    {/* Item Details */}
                    <div className="flex items-center gap-4 min-w-0 flex-1">
                      <Link
                        to={`/products/${item.id}`}
                        className="w-16 h-16 sm:w-20 sm:h-20 rounded-md overflow-hidden bg-zinc-50 shrink-0 border border-zinc-200"
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
                          <div className="w-full h-full flex items-center justify-center text-zinc-400">
                            <ShoppingBag className="w-6 h-6 stroke-1 text-zinc-300" />
                          </div>
                        )}
                      </Link>

                      <div className="min-w-0 flex-1">
                        <Link
                          to={`/products/${item.id}`}
                          className="font-medium text-zinc-900 text-sm hover:underline line-clamp-1 transition-colors"
                        >
                          {item.name}
                        </Link>
                        <div className="text-zinc-950 font-semibold text-sm mt-0.5 tabular-nums">
                          {formatPrice(item.price)}
                        </div>
                        {item.stockQuantity !== undefined && (
                          <div className="text-[11px] text-zinc-400 mt-0.5">
                            Còn lại trong kho: {item.stockQuantity}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Quantity & Controls */}
                    <div className="flex items-center justify-between sm:justify-end w-full sm:w-auto gap-4">
                      {/* Quantity Selector */}
                      <div className="flex items-center border border-zinc-200 rounded-md bg-white overflow-hidden shadow-xs">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity - 1)}
                          className="p-1.5 text-zinc-600 hover:bg-zinc-100 transition-colors cursor-pointer"
                          aria-label="Giảm số lượng"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="px-2.5 py-1 text-xs font-medium text-zinc-900 min-w-[32px] text-center select-none tabular-nums">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          disabled={item.stockQuantity && item.quantity >= item.stockQuantity}
                          className="p-1.5 text-zinc-600 hover:bg-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                          aria-label="Tăng số lượng"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      {/* Line Item Total */}
                      <div className="text-right min-w-[100px]">
                        <div className="text-sm font-semibold text-zinc-950 tabular-nums">
                          {formatPrice((item.price || 0) * item.quantity)}
                        </div>
                      </div>

                      {/* Remove Button */}
                      <button
                        type="button"
                        onClick={() => removeFromCart(item.id)}
                        className="text-zinc-400 hover:text-red-600 p-1.5 rounded-md hover:bg-red-50 transition-colors cursor-pointer"
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
                  className="text-xs font-medium text-zinc-600 hover:text-zinc-950 inline-flex items-center gap-1.5 transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Tiếp tục chọn thêm sản phẩm khác</span>
                </Link>
              </div>
            </div>

            {/* Order Summary Sidebar (4/12) */}
            <div className="lg:col-span-4 space-y-6">
              <div className="bg-white rounded-lg border border-zinc-200 shadow-sm p-6 space-y-5 sticky top-24">
                <h2 className="text-base font-semibold text-zinc-950 pb-3 border-b border-zinc-100">
                  Tóm tắt đơn hàng
                </h2>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between text-zinc-600">
                    <span>Tạm tính ({totalCount} sản phẩm)</span>
                    <span className="font-medium text-zinc-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  <div className="flex justify-between text-zinc-600">
                    <span>Phí vận chuyển</span>
                    <span className="font-medium text-emerald-600">Miễn phí</span>
                  </div>
                  <div className="flex justify-between text-zinc-600">
                    <span>Ưu đãi</span>
                    <span className="font-medium text-zinc-900">0 ₫</span>
                  </div>

                  <div className="pt-3 border-t border-zinc-100 flex justify-between items-baseline">
                    <span className="text-sm font-semibold text-zinc-950">Tổng cộng</span>
                    <div className="text-right">
                      <span className="text-xl font-semibold text-zinc-950 tabular-nums">
                        {formatPrice(totalPrice)}
                      </span>
                      <div className="text-[11px] text-zinc-400 mt-0.5">(Đã bao gồm VAT)</div>
                    </div>
                  </div>
                </div>

                {/* Checkout Button */}
                <button
                  type="button"
                  onClick={handleCheckout}
                  className="w-full py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 font-medium text-sm rounded-md shadow-sm transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Tiến hành đặt hàng</span>
                </button>

                {/* Trust Badges */}
                <div className="pt-3 border-t border-zinc-100 space-y-2 text-xs text-zinc-500">
                  <div className="flex items-center gap-2">
                    <Truck className="w-3.5 h-3.5 text-zinc-700 shrink-0" />
                    <span>Miễn phí giao hàng cho mọi đơn từ 500k</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <RotateCcw className="w-3.5 h-3.5 text-zinc-700 shrink-0" />
                    <span>Đổi trả thuận tiện trong vòng 7 ngày</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-3.5 h-3.5 text-zinc-700 shrink-0" />
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
