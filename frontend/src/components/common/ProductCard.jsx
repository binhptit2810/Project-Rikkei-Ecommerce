import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShoppingBag, ShoppingCart, Star, Heart, Eye } from 'lucide-react'
import { motion, useReducedMotion } from 'framer-motion'
import { useAuthStore } from '../../store/useAuthStore'
import { useWishlistStore } from '../../store/useWishlistStore'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

// Màu nền tỏa tròn (Radial Glow) theo chuẩn TechNova Electronics
const GLOW_CLASSES = [
  'bg-gradient-to-br from-orange-50/80 via-white to-amber-50/30',
  'bg-gradient-to-br from-blue-50/80 via-white to-indigo-50/30',
  'bg-gradient-to-br from-emerald-50/80 via-white to-teal-50/30',
  'bg-gradient-to-br from-purple-50/80 via-white to-pink-50/30'
]

/**
 * Component hiển thị thẻ sản phẩm (ProductCard)
 * Thiết kế chuẩn phong cách TechNova Electronics với Motion for React:
 * - Khung thẻ bo góc 2xl viền mảnh tinh tế, hover nâng nhẹ & đổ bóng sâu
 * - Nền ảnh với hiệu ứng radial glow đa sắc, ảnh zoom nhẹ mượt mà
 * - Badge "BEST SELLER" cam và discount xanh lá
 * - Tên thương hiệu in hoa, đánh giá sao vàng
 * - Nút "Thêm vào giỏ" viên thuốc màu xanh hoàng gia (Royal Blue) với micro-interaction
 */
export const ProductCard = ({ product, onAddToCart, variants }) => {
  const [imgError, setImgError] = useState(false)
  const { isAuthenticated, user } = useAuthStore()
  const navigate = useNavigate()
  const shouldReduceMotion = useReducedMotion()

  if (!product) return null

  const isOutOfStock =
    product.stockQuantity === undefined ||
    product.stockQuantity === null ||
    Number(product.stockQuantity) <= 0

  const isLiked = useWishlistStore((state) => state.isInWishlist(product.id))
  const toggleWishlist = useWishlistStore((state) => state.toggleWishlist)

  // Xử lý sự kiện click icon Trái tim Yêu thích [TSK-D15-DDM]
  const handleWishlistClick = async (e) => {
    e.preventDefault()
    e.stopPropagation()

    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${product.id}` },
          message: 'Vui lòng đăng nhập để lưu sản phẩm vào danh sách yêu thích.',
        },
      })
      return
    }

    if (user?.role === 'ADMIN') {
      alert('Tài khoản Quản trị viên (ADMIN) không sử dụng phân hệ yêu thích.')
      return
    }

    try {
      await toggleWishlist(product.id, product)
    } catch (err) {
      // Toast thông báo lỗi đã được xử lý trong store
    }
  }

  // Định dạng giá chuẩn VNĐ
  const formattedPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(product.price || 0)

  // Giá gốc tượng trưng để hiển thị mức giảm giá (15%) như thiết kế TechNova
  const originalPrice = Math.round((product.price || 0) * 1.15)
  const formattedOriginalPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(originalPrice)

  // Xử lý sự kiện click Thêm vào giỏ hàng
  const handleAddToCartClick = (e) => {
    e.preventDefault()
    e.stopPropagation()

    // Bắt buộc đăng nhập hoặc đăng ký nếu chưa xác thực
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${product.id}` },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }

    // Chặn Quản trị viên mua hàng
    if (user?.role === 'ADMIN') {
      alert('Tài khoản Quản trị viên (ADMIN) không được phép mua hàng.')
      return
    }

    if (!isOutOfStock && onAddToCart) {
      onAddToCart(product)
    }
  }

  // Xử lý fallback ảnh khi link ảnh bị lỗi
  const handleImageError = (e) => {
    if (!imgError) {
      setImgError(true)
      e.target.onerror = null
      e.target.src = FALLBACK_IMAGE
    }
  }

  // Lựa chọn màu glow theo ID sản phẩm
  const glowClass = GLOW_CLASSES[(Number(product.id) || 0) % GLOW_CLASSES.length]

  return (
    <motion.div
      variants={variants}
      whileHover={shouldReduceMotion ? {} : { y: -6, transition: { duration: 0.25, ease: 'easeOut' } }}
      className="group relative flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white hover:border-slate-300 shadow-sm hover:shadow-xl transition-shadow duration-300 overflow-hidden"
    >
      <Link to={`/products/${product.id}`} className="block">
        {/* Khung ảnh sản phẩm & các Badge TechNova */}
        <div className={`aspect-square w-full relative overflow-hidden flex items-center justify-center p-6 ${glowClass}`}>
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name || 'Sản phẩm'}
              onError={handleImageError}
              className="w-full h-full object-contain mix-blend-multiply transition-transform duration-300 group-hover:scale-108"
            />
          ) : (
            <div className="flex flex-col items-center justify-center text-slate-300 w-full h-full">
              <ShoppingBag className="w-12 h-12 stroke-[1.25] text-slate-300 mb-2" />
              <span className="text-xs text-slate-400 font-medium">Rikkei Store</span>
            </div>
          )}

          {/* Badge Trái: BEST SELLER hoặc HOT & Giảm giá */}
          <div className="absolute top-3 left-3 flex flex-col gap-1 z-10">
            {product.stockQuantity > 5 ? (
              <span className="bg-orange-500 text-white text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase tracking-wider shadow-xs">
                BÁN CHẠY
              </span>
            ) : isOutOfStock ? (
              <span className="bg-slate-900 text-white text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase tracking-wider shadow-xs">
                HẾT HÀNG
              </span>
            ) : (
              <span className="bg-amber-500 text-white text-[10px] font-bold px-2.5 py-0.5 rounded-md uppercase tracking-wider shadow-xs">
                SẮP HẾT
              </span>
            )}
            {!isOutOfStock && (
              <span className="bg-emerald-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow-xs w-fit">
                -15%
              </span>
            )}
          </div>

          {/* Nút Trái tim yêu thích ở góc trên bên phải ảnh sản phẩm [TSK-D15-DDM] */}
          <motion.button
            whileHover={shouldReduceMotion ? {} : { scale: 1.15 }}
            whileTap={shouldReduceMotion ? {} : { scale: 0.9 }}
            type="button"
            onClick={handleWishlistClick}
            className={`absolute top-3 right-3 z-20 w-8.5 h-8.5 rounded-full flex items-center justify-center transition-all shadow-sm cursor-pointer ${
              isLiked
                ? 'bg-red-50 text-red-500 hover:bg-red-100 shadow-red-500/20'
                : 'bg-white/90 hover:bg-white text-slate-400 hover:text-red-500 border border-slate-200/60'
            }`}
            title={isLiked ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'}
          >
            <Heart
              className={`w-4.5 h-4.5 transition-colors ${
                isLiked ? 'text-red-500 fill-red-500 scale-105' : 'text-slate-400 hover:text-red-500'
              }`}
            />
          </motion.button>

          {/* Hover Action Buttons (TechNova Image 5) */}
          <div className="absolute right-3 top-1/2 -translate-y-1/2 flex flex-col gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10">
            <motion.button
              whileHover={shouldReduceMotion ? {} : { scale: 1.15 }}
              whileTap={shouldReduceMotion ? {} : { scale: 0.9 }}
              type="button"
              onClick={handleWishlistClick}
              className={`w-8 h-8 rounded-full bg-white/95 hover:bg-white shadow-md flex items-center justify-center cursor-pointer transition-colors ${
                isLiked ? 'text-red-500' : 'text-slate-700'
              }`}
              title={isLiked ? 'Bỏ yêu thích' : 'Yêu thích'}
            >
              <Heart
                className={`w-4 h-4 transition-colors ${
                  isLiked ? 'text-red-500 fill-red-500' : 'text-slate-600 hover:text-red-500'
                }`}
              />
            </motion.button>
            <motion.button
              whileHover={shouldReduceMotion ? {} : { scale: 1.15 }}
              whileTap={shouldReduceMotion ? {} : { scale: 0.9 }}
              type="button"
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                navigate(`/products/${product.id}`)
              }}
              className="w-8 h-8 rounded-full bg-white/95 hover:bg-white text-slate-700 shadow-md flex items-center justify-center cursor-pointer"
              title="Xem chi tiết"
            >
              <Eye className="w-4 h-4 text-slate-600 hover:text-blue-600 transition-colors" />
            </motion.button>
          </div>
        </div>

        {/* Thông tin sản phẩm */}
        <div className="p-4 sm:p-5 space-y-1.5">
          {/* Tên thương hiệu/danh mục in hoa */}
          <p className="text-[11px] font-bold tracking-wider text-slate-400 uppercase truncate">
            {product.categoryName || 'RIKKEI STORE GEAR'}
          </p>

          {/* Tên sản phẩm */}
          <h3 className="font-bold text-slate-900 text-sm group-hover:text-blue-600 transition-colors line-clamp-1 leading-snug">
            {product.name}
          </h3>

          {/* Đánh giá sao TechNova (4.8 rating) */}
          <div className="flex items-center gap-1.5 pt-0.5 text-xs">
            <div className="flex text-amber-400">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-3.5 h-3.5 fill-current" />
              ))}
            </div>
            <span className="text-slate-500 font-semibold text-[11px]">
              4.8 <span className="text-slate-400 font-normal">({(Number(product.id) * 17) % 300 + 120})</span>
            </span>
          </div>

          {/* Hàng giá tiền: Giá bán + Giá gốc gạch ngang + Tag giảm */}
          <div className="pt-2 flex items-baseline gap-2 flex-wrap">
            <span className="text-base font-extrabold text-slate-900">
              {formattedPrice}
            </span>
            <span className="text-xs text-slate-400 line-through">
              {formattedOriginalPrice}
            </span>
            <span className="text-[11px] font-bold text-emerald-600">
              15% off
            </span>
          </div>
        </div>
      </Link>

      {/* Nút Thêm vào giỏ hàng: Royal Blue Pill (TechNova Image 5) */}
      <div className="p-4 pt-0 sm:p-5 sm:pt-0 mt-auto">
        <motion.button
          whileHover={shouldReduceMotion || isOutOfStock ? {} : { scale: 1.02 }}
          whileTap={shouldReduceMotion || isOutOfStock ? {} : { scale: 0.96 }}
          type="button"
          onClick={handleAddToCartClick}
          disabled={isOutOfStock}
          className={`w-full py-2.5 px-4 rounded-full text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer shadow-xs ${
            isOutOfStock
              ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
              : 'bg-blue-600 hover:bg-blue-700 hover:shadow-md text-white'
          }`}
          title={isOutOfStock ? 'Sản phẩm đã hết hàng' : 'Thêm vào giỏ hàng'}
        >
          <ShoppingCart className="w-4 h-4" />
          <span>{isOutOfStock ? 'Tạm hết hàng' : 'Thêm vào giỏ'}</span>
        </motion.button>
      </div>
    </motion.div>
  )
}

export default ProductCard
