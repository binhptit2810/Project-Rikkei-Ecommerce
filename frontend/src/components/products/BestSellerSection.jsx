import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import {
  Crown,
  Medal,
  Award,
  Flame,
  ArrowRight,
  Eye,
  RefreshCw,
  AlertCircle,
  ShoppingBag,
  Star,
  Sparkles
} from 'lucide-react'
import productService from '../../services/productService'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

// Định dạng giá tiền chuẩn VND
const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(val || 0)
}

/**
 * Component hiển thị huy hiệu xếp hạng Top 1, Top 2, Top 3
 */
const RankBadge = ({ rank }) => {
  if (rank === 1) {
    return (
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-gradient-to-r from-amber-400 via-yellow-300 to-amber-500 text-amber-950 shadow-md shadow-amber-500/25 border border-yellow-200/80 ring-2 ring-amber-300/40 animate-pulse">
        <Crown className="w-3.5 h-3.5 fill-amber-950" />
        <span>TOP 1 BÁN CHẠY</span>
      </div>
    )
  }

  if (rank === 2) {
    return (
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-gradient-to-r from-slate-200 via-gray-100 to-slate-300 text-slate-800 shadow-sm border border-slate-300/80">
        <Medal className="w-3.5 h-3.5 text-slate-700" />
        <span>TOP 2 BÁN CHẠY</span>
      </div>
    )
  }

  if (rank === 3) {
    return (
      <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-gradient-to-r from-amber-700 via-orange-600 to-amber-800 text-amber-50 shadow-sm border border-amber-900/40">
        <Award className="w-3.5 h-3.5 text-amber-100" />
        <span>TOP 3 BÁN CHẠY</span>
      </div>
    )
  }

  if (rank && rank > 3) {
    return (
      <div className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
        <span>#{rank}</span>
      </div>
    )
  }

  return null
}

/**
 * Thẻ khung xương (Skeleton Card) cho trạng thái Loading
 */
const BestSellerSkeleton = () => (
  <div className="rounded-2xl border border-slate-200/80 bg-white p-4 flex flex-col justify-between animate-pulse">
    <div>
      <div className="aspect-square w-full rounded-xl bg-slate-200 mb-4" />
      <div className="h-4 w-24 bg-slate-200 rounded-full mb-3" />
      <div className="h-5 w-3/4 bg-slate-200 rounded mb-2" />
      <div className="h-5 w-1/2 bg-slate-200 rounded mb-4" />
    </div>
    <div className="space-y-3 pt-3 border-t border-slate-100">
      <div className="h-3.5 w-28 bg-slate-200 rounded" />
      <div className="h-2 w-full bg-slate-200 rounded-full" />
      <div className="h-9 w-full bg-slate-200 rounded-xl" />
    </div>
  </div>
)

/**
 * Component BestSellerCard: hiển thị sản phẩm bán chạy với thanh tiến trình doanh số
 */
const BestSellerCard = ({ product, rank, maxSold, shouldReduceMotion }) => {
  const [imgError, setImgError] = useState(false)
  const navigate = useNavigate()

  const sold = Number(product.soldQuantity ?? product.totalSold ?? 0)
  const productId = product.productId ?? product.id
  const productName = product.productName ?? product.name ?? 'Sản phẩm'
  const price = product.price ?? 0
  const imageUrl = product.imageUrl || FALLBACK_IMAGE

  // Tính phần trăm tiến trình tương đối so với sản phẩm bán chạy nhất (Top 1 = 100%)
  const percentage = useMemo(() => {
    if (!maxSold || maxSold <= 0 || sold <= 0) return 0
    return Math.min(100, Math.round((sold / maxSold) * 100))
  }, [sold, maxSold])

  const handleCardClick = () => {
    navigate(`/products/${productId}`)
  }

  const handleImageError = () => {
    if (!imgError) {
      setImgError(true)
    }
  }

  return (
    <motion.div
      whileHover={shouldReduceMotion ? {} : { y: -6, transition: { duration: 0.25, ease: 'easeOut' } }}
      onClick={handleCardClick}
      className="group relative flex flex-col justify-between h-full rounded-2xl border border-slate-200/80 bg-white hover:border-blue-300 shadow-xs hover:shadow-xl transition-all duration-300 overflow-hidden cursor-pointer"
    >
      <div>
        {/* Khung ảnh & Huy hiệu xếp hạng */}
        <div className="relative aspect-square w-full bg-gradient-to-br from-slate-50 via-white to-slate-100/60 overflow-hidden flex items-center justify-center p-4">
          {/* Badge xếp hạng gắn trên góc trái ảnh */}
          <div className="absolute top-3 left-3 z-10">
            <RankBadge rank={rank} />
          </div>

          <img
            src={imgError ? FALLBACK_IMAGE : imageUrl}
            alt={productName}
            onError={handleImageError}
            loading="lazy"
            className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500"
          />
        </div>

        {/* Thông tin sản phẩm */}
        <div className="p-4 sm:p-5">
          {/* Tên danh mục nếu có */}
          {product.categoryName && (
            <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1 line-clamp-1">
              {product.categoryName}
            </p>
          )}

          {/* Tên sản phẩm */}
          <h3
            title={productName}
            className="font-bold text-slate-800 text-sm sm:text-base group-hover:text-blue-600 transition-colors line-clamp-2 min-h-[2.5rem]"
          >
            {productName}
          </h3>

          {/* Đánh giá sao nếu có */}
          <div className="flex items-center gap-1.5 mt-2">
            <div className="flex items-center text-amber-400">
              <Star className="w-3.5 h-3.5 fill-amber-400" />
            </div>
            <span className="text-xs font-semibold text-slate-700">
              {Number(product.ratingAverage || 5.0).toFixed(1)}
            </span>
            <span className="text-xs text-slate-400">
              ({product.totalReviews || 0})
            </span>
          </div>

          {/* Giá tiền */}
          <div className="mt-2.5 flex items-baseline gap-2">
            <span className="text-base sm:text-lg font-black text-blue-600">
              {formatCurrency(price)}
            </span>
          </div>
        </div>
      </div>

      {/* Footer Card: Tiến trình số lượng đã bán & nút Xem chi tiết */}
      <div className="p-4 sm:p-5 pt-0">
        <div className="pt-3 border-t border-slate-100">
          {/* Số lượng đã bán */}
          <div className="flex items-center justify-between text-xs mb-1.5">
            <span className="inline-flex items-center gap-1 font-bold text-orange-600">
              <Flame className="w-3.5 h-3.5 fill-orange-500 animate-bounce" />
              <span>Đã bán {sold.toLocaleString('vi-VN')} sản phẩm</span>
            </span>
            <span className="text-[11px] text-slate-400 font-medium" title="Tỷ lệ tương đối so với sản phẩm bán chạy nhất">
              {percentage}%
            </span>
          </div>

          {/* Thanh tiến trình doanh số */}
          <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden mb-4">
            <motion.div
              initial={shouldReduceMotion ? { width: `${percentage}%` } : { width: 0 }}
              animate={{ width: `${percentage}%` }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
              className="h-full rounded-full bg-gradient-to-r from-amber-400 via-orange-500 to-rose-500"
            />
          </div>

          {/* Nút Xem chi tiết */}
          <Link
            to={`/products/${productId}`}
            onClick={(e) => e.stopPropagation()}
            className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-blue-700 bg-blue-50 hover:bg-blue-600 hover:text-white transition-all duration-200 group/btn"
          >
            <Eye className="w-4 h-4" />
            <span>Xem chi tiết</span>
            <ArrowRight className="w-3.5 h-3.5 group-hover/btn:translate-x-1 transition-transform" />
          </Link>
        </div>
      </div>
    </motion.div>
  )
}

/**
 * Section Sản Phẩm Bán Chạy Nhất Tháng (Best Seller Section)
 */
export const BestSellerSection = ({ limit = 8, month, year }) => {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const shouldReduceMotion = useReducedMotion()

  const now = useMemo(() => new Date(), [])
  const currentMonth = month ?? (now.getMonth() + 1)
  const currentYear = year ?? now.getFullYear()

  const fetchBestSellers = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await productService.getBestSellers({
        month: currentMonth,
        year: currentYear,
        limit,
        size: limit,
      })

      const items = res.data?.content || res.data?.products || (Array.isArray(res.data) ? res.data : [])
      setProducts(items)
    } catch (err) {
      console.error('Lỗi tải danh sách Best Seller:', err)
      setError('Không thể tải danh sách sản phẩm bán chạy. Vui lòng thử lại sau.')
    } finally {
      setLoading(false)
    }
  }, [currentMonth, currentYear, limit])

  useEffect(() => {
    fetchBestSellers()
  }, [fetchBestSellers])

  // Tính số lượng bán cao nhất để làm mốc 100% cho thanh tiến trình
  const maxSold = useMemo(() => {
    if (!products || products.length === 0) return 0
    return Math.max(...products.map((p) => Number(p.soldQuantity ?? p.totalSold ?? 0)), 0)
  }, [products])

  return (
    <section className="py-12 sm:py-16 bg-gradient-to-b from-white via-slate-50/40 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8 sm:mb-10">
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/60 text-amber-700 text-xs font-bold uppercase tracking-wider mb-2.5 shadow-2xs">
              <Sparkles className="w-3.5 h-3.5 text-amber-500 fill-amber-400" />
              <span>THÁNG {currentMonth}/{currentYear}</span>
            </div>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-slate-900 tracking-tight">
              SẢN PHẨM BÁN CHẠY NHẤT THÁNG
            </h2>
            <p className="text-sm sm:text-base text-slate-500 mt-1.5">
              Những sản phẩm được khách hàng yêu thích và lựa chọn nhiều nhất
            </p>
          </div>

          <Link
            to="/products"
            className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-bold text-blue-600 hover:text-blue-700 group transition-colors self-start sm:self-end"
          >
            <span>Khám phá thêm</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Link>
        </div>

        {/* Nội dung chính theo trạng thái: Loading, Error, Empty, Success */}
        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {Array.from({ length: limit }).map((_, idx) => (
              <BestSellerSkeleton key={`skeleton-${idx}`} />
            ))}
          </div>
        ) : error ? (
          <div className="p-8 text-center rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 max-w-lg mx-auto">
            <AlertCircle className="w-8 h-8 mx-auto mb-3 text-rose-500" />
            <p className="text-sm font-semibold mb-4">{error}</p>
            <button
              type="button"
              onClick={fetchBestSellers}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 transition-colors shadow-sm cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : products.length === 0 ? (
          <div className="p-12 text-center rounded-2xl bg-slate-50 border border-slate-200 max-w-md mx-auto">
            <ShoppingBag className="w-12 h-12 mx-auto mb-3 text-slate-300" />
            <h3 className="text-base font-bold text-slate-800 mb-1">
              Chưa có sản phẩm bán chạy trong tháng {currentMonth}/{currentYear}
            </h3>
            <p className="text-xs text-slate-500 mb-5">
              Hãy quay lại sau hoặc khám phá toàn bộ bộ sưu tập mới nhất của chúng tôi.
            </p>
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 transition-colors shadow-sm"
            >
              <span>Xem tất cả sản phẩm</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {products.map((prod, index) => {
              const rank = prod.rank ?? (index + 1)
              return (
                <BestSellerCard
                  key={prod.productId ?? prod.id ?? `best-seller-${index}`}
                  product={prod}
                  rank={rank}
                  maxSold={maxSold}
                  shouldReduceMotion={shouldReduceMotion}
                />
              )
            })}
          </div>
        )}
      </div>
    </section>
  )
}

export default BestSellerSection

