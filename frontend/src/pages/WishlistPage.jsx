import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Heart,
  HeartOff,
  Trash2,
  ShoppingCart,
  Star,
  ArrowRight,
  Sparkles,
} from 'lucide-react'
import { motion } from 'framer-motion'
import { useWishlistStore } from '../store/useWishlistStore'
import { useCartStore } from '../store/useCartStore'
import { useAuthStore } from '../store/useAuthStore'
import { toast } from '../store/useToastStore'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * Trang Danh sách sản phẩm yêu thích (WishlistPage).
 * Hiển thị toàn bộ sản phẩm khách hàng đã thả tim với giao diện chuẩn TechNova.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
export const WishlistPage = () => {
  const navigate = useNavigate()
  const { items, loading, error, fetchWishlist, toggleWishlist } = useWishlistStore()
  const { addToCart } = useCartStore()
  const { isAuthenticated, user } = useAuthStore()
  const [imgErrors, setImgErrors] = useState({})

  useEffect(() => {
    fetchWishlist()
  }, [fetchWishlist])

  const handleImageError = (productId) => {
    setImgErrors((prev) => ({ ...prev, [productId]: true }))
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price || 0)
  }

  // Xóa sản phẩm khỏi wishlist
  const handleRemove = async (e, productId) => {
    e.preventDefault()
    e.stopPropagation()
    try {
      await toggleWishlist(productId)
    } catch (err) {
      // Đã xử lý trong store
    }
  }

  // Thêm vào giỏ hàng
  const handleAddToCart = async (e, product) => {
    e.preventDefault()
    e.stopPropagation()

    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: '/wishlist' },
          message: 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }

    if (user?.role === 'ADMIN') {
      alert('Tài khoản Quản trị viên (ADMIN) không được phép mua hàng.')
      return
    }

    const res = await addToCart(product, 1)
    if (res?.success) {
      toast.cart('Đã thêm vào giỏ hàng', product.name)
    } else if (res?.message) {
      toast.error('Thông báo', res.message)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50/60 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-8 border-b border-slate-200">
          <div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-red-50 text-red-500 flex items-center justify-center shadow-xs">
                <Heart className="w-5 h-5 fill-red-500" />
              </div>
              <div>
                <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                  Sản phẩm yêu thích của tôi
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
                  Quản lý và theo dõi các sản phẩm công nghệ bạn đã lưu
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="inline-flex items-center px-3.5 py-1.5 rounded-full text-xs font-bold bg-white text-slate-700 border border-slate-200 shadow-2xs">
              Đã lưu: <span className="text-red-500 font-extrabold ml-1.5">{items.length}</span> sản phẩm
            </span>
            <Link
              to="/products"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-blue-600 hover:text-blue-700 hover:underline"
            >
              <span>Xem tất cả sản phẩm</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>

        {/* Content Section */}
        {loading ? (
          <div className="py-24 flex flex-col items-center justify-center">
            <div className="w-10 h-10 border-4 border-red-500 border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-sm font-medium text-slate-500">Đang tải danh sách yêu thích của bạn...</p>
          </div>
        ) : error ? (
          <div className="py-16 text-center">
            <div className="w-14 h-14 rounded-2xl bg-red-50 text-red-500 mx-auto flex items-center justify-center mb-4">
              <HeartOff className="w-7 h-7" />
            </div>
            <p className="text-base font-bold text-slate-800">{error}</p>
            <button
              onClick={() => fetchWishlist()}
              className="mt-4 px-4 py-2 rounded-full text-xs font-bold bg-slate-900 text-white hover:bg-slate-800 transition-colors"
            >
              Thử lại
            </button>
          </div>
        ) : items.length === 0 ? (
          /* Empty State */
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="py-20 sm:py-28 text-center max-w-md mx-auto"
          >
            <div className="w-20 h-20 rounded-3xl bg-gradient-to-tr from-red-100 to-rose-50 text-red-500 mx-auto flex items-center justify-center mb-6 shadow-inner">
              <HeartOff className="w-10 h-10 stroke-[1.5]" />
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              Chưa có sản phẩm yêu thích nào
            </h2>
            <p className="text-slate-500 text-xs sm:text-sm mt-2 leading-relaxed">
              Hãy nhấn vào biểu tượng trái tim ở bất kỳ sản phẩm nào để lưu lại danh sách những món đồ công nghệ tuyệt vời bạn ưng ý nhất!
            </p>
            <div className="mt-8">
              <Link
                to="/products"
                className="inline-flex items-center gap-2 px-6 py-3 rounded-full text-sm font-bold bg-gradient-to-r from-blue-600 to-indigo-600 text-white hover:from-blue-700 hover:to-indigo-700 shadow-md shadow-blue-500/25 hover:shadow-lg transition-all active:scale-[0.98]"
              >
                <Sparkles className="w-4 h-4" />
                <span>Khám phá sản phẩm ngay</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </motion.div>
        ) : (
          /* Responsive Grid: 4 columns desktop, 2 columns tablet, 1 column mobile */
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 pt-8">
            {items.map((item) => {
              const isOutOfStock =
                item.stockQuantity === undefined ||
                item.stockQuantity === null ||
                Number(item.stockQuantity) <= 0

              const imgSrc = imgErrors[item.id] ? FALLBACK_IMAGE : item.imageUrl || FALLBACK_IMAGE

              return (
                <motion.div
                  key={item.id}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  transition={{ duration: 0.2 }}
                  className="group relative flex flex-col justify-between rounded-2xl border border-slate-200/90 bg-white hover:border-slate-300 shadow-sm hover:shadow-xl transition-all duration-300 overflow-hidden"
                >
                  <Link to={`/products/${item.id}`} className="block">
                    {/* Ảnh sản phẩm */}
                    <div className="aspect-square w-full relative overflow-hidden flex items-center justify-center p-6 bg-slate-50/80 group-hover:bg-slate-100/60 transition-colors">
                      <img
                        src={imgSrc}
                        alt={item.name}
                        onError={() => handleImageError(item.id)}
                        className="w-full h-full object-contain mix-blend-multiply transition-transform duration-300 group-hover:scale-108"
                      />

                      {/* Nút Bỏ thích (Trash / Heart Off) ở góc trên bên phải */}
                      <button
                        type="button"
                        onClick={(e) => handleRemove(e, item.id)}
                        className="absolute top-3 right-3 w-8.5 h-8.5 rounded-full bg-white/90 hover:bg-red-50 text-slate-400 hover:text-red-500 border border-slate-200/60 flex items-center justify-center shadow-xs transition-colors cursor-pointer z-10"
                        title="Bỏ thích sản phẩm này"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>

                      {/* Trạng thái tồn kho */}
                      {isOutOfStock && (
                        <div className="absolute top-3 left-3 z-10">
                          <span className="bg-slate-900 text-white text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase tracking-wider shadow-xs">
                            HẾT HÀNG
                          </span>
                        </div>
                      )}
                    </div>

                    {/* Thông tin sản phẩm */}
                    <div className="p-4 sm:p-5 space-y-2">
                      <h3 className="font-bold text-slate-900 text-sm group-hover:text-blue-600 transition-colors line-clamp-2 leading-snug">
                        {item.name}
                      </h3>

                      {/* Đánh giá sao */}
                      <div className="flex items-center gap-1.5 text-xs text-amber-400">
                        <div className="flex">
                          {[...Array(5)].map((_, i) => (
                            <Star key={i} className="w-3.5 h-3.5 fill-current" />
                          ))}
                        </div>
                        <span className="text-slate-600 font-bold text-[11px] ml-1">
                          {item.ratingAverage ? Number(item.ratingAverage).toFixed(1) : '5.0'}
                        </span>
                      </div>

                      {/* Giá bán */}
                      <div className="pt-1 flex items-baseline gap-2">
                        <span className="text-base font-extrabold text-slate-900">
                          {formatPrice(item.price)}
                        </span>
                        {item.discountPrice && item.discountPrice < item.price && (
                          <span className="text-xs text-slate-400 line-through">
                            {formatPrice(item.price * 1.15)}
                          </span>
                        )}
                      </div>
                    </div>
                  </Link>

                  {/* Action Buttons: Thêm vào giỏ hàng & Bỏ thích */}
                  <div className="p-4 pt-0 sm:p-5 sm:pt-0 mt-auto flex items-center gap-2">
                    <button
                      type="button"
                      onClick={(e) => handleAddToCart(e, item)}
                      disabled={isOutOfStock}
                      className={`flex-1 py-2.5 px-3 rounded-full text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer shadow-xs ${
                        isOutOfStock
                          ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
                          : 'bg-blue-600 hover:bg-blue-700 text-white hover:shadow-md'
                      }`}
                      title={isOutOfStock ? 'Sản phẩm đã hết hàng' : 'Thêm vào giỏ'}
                    >
                      <ShoppingCart className="w-3.5 h-3.5" />
                      <span>{isOutOfStock ? 'Tạm hết' : 'Thêm vào giỏ'}</span>
                    </button>

                    <button
                      type="button"
                      onClick={(e) => handleRemove(e, item.id)}
                      className="w-9 h-9 rounded-full bg-slate-100 hover:bg-red-50 text-slate-500 hover:text-red-500 flex items-center justify-center transition-colors cursor-pointer shrink-0"
                      title="Bỏ thích"
                    >
                      <HeartOff className="w-4 h-4" />
                    </button>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

export default WishlistPage

