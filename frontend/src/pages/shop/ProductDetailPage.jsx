import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import {
  ShoppingBag,
  ShoppingCart,
  CheckCircle2,
  AlertCircle,
  Truck,
  RotateCcw,
  ShieldCheck,
  ChevronRight,
  ArrowLeft,
  Minus,
  Plus,
  Star,
  Zap,
} from 'lucide-react'
import productService from '../../services/productService'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import ProductReviewsSection from '../../components/products/ProductReviewsSection'

export const ProductDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const addToCart = useCartStore((state) => state.addToCart)
  const { isAuthenticated, user } = useAuthStore()

  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [quantity, setQuantity] = useState(1)
  const [cartNotice, setCartNotice] = useState(null)

  useEffect(() => {
    const fetchDetail = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await productService.getProductById(id)
        if (res.success && res.data) {
          setProduct(res.data)
        } else {
          setError('Không tìm thấy thông tin sản phẩm')
        }
      } catch (err) {
        console.error('Lỗi tải chi tiết sản phẩm:', err)
        setError(err.response?.data?.message || 'Sản phẩm không tồn tại hoặc đã bị ẩn')
      } finally {
        setLoading(false)
      }
    }

    if (id) {
      fetchDetail()
    }
  }, [id])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${id}` },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }
    if (user?.role === 'ADMIN') {
      alert('Tài khoản Quản trị viên (ADMIN) không được phép mua hàng.')
      return
    }
    if (!product || product.stockQuantity <= 0) return
    const res = await addToCart(product, quantity)
    if (res?.requireLogin) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${id}` },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }
    if (res?.isAdmin) {
      alert(res.message || 'Tài khoản Quản trị viên không được phép mua hàng.')
      return
    }
    setCartNotice(`Đã thêm ${quantity} sản phẩm vào giỏ hàng thành công!`)
    setTimeout(() => setCartNotice(null), 5000)
  }

  const handleBuyNow = async () => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${id}` },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để mua hàng.',
        },
      })
      return
    }
    if (user?.role === 'ADMIN') {
      alert('Tài khoản Quản trị viên (ADMIN) không được phép mua hàng.')
      return
    }
    if (!product || product.stockQuantity <= 0) return
    const res = await addToCart(product, quantity)
    if (res?.requireLogin) {
      navigate('/login', {
        state: {
          from: { pathname: `/products/${id}` },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để mua hàng.',
        },
      })
      return
    }
    if (res?.isAdmin) {
      alert(res.message || 'Tài khoản Quản trị viên không được phép mua hàng.')
      return
    }
    navigate('/checkout')
  }

  if (loading) {
    return (
      <div className="py-24 flex justify-center flex-1">
        <LoadingSpinner />
      </div>
    )
  }

  if (error || !product) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center flex-1">
        <div className="w-14 h-14 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-3 border border-red-200">
          <AlertCircle className="w-7 h-7" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 mb-1">Không tìm thấy sản phẩm</h2>
        <p className="text-slate-500 mb-6 text-sm">{error || 'Sản phẩm bạn yêu cầu không tồn tại.'}</p>
        <Link
          to="/products"
          className="inline-flex items-center gap-2 px-6 py-2.5 bg-blue-600 text-white rounded-full text-xs font-bold hover:bg-blue-700 transition-colors shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách sản phẩm</span>
        </Link>
      </div>
    )
  }

  const isOutOfStock = product.stockQuantity <= 0
  const originalPrice = Math.round((product.price || 0) * 1.15)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex-1 w-full">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-xs text-slate-500 mb-8 flex-wrap">
        <Link to="/" className="hover:text-blue-600 transition-colors">
          Trang chủ
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
        <Link to="/products" className="hover:text-blue-600 transition-colors">
          Sản phẩm
        </Link>
        {product.categoryName && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <Link
              to={`/products?categoryId=${product.categoryId}`}
              className="hover:text-blue-600 transition-colors"
            >
              {product.categoryName}
            </Link>
          </>
        )}
        <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
        <span className="text-slate-900 font-bold truncate max-w-[200px] sm:max-w-md">
          {product.name}
        </span>
      </nav>

      {/* Cart Feedback Notification */}
      {cartNotice && (
        <div className="mb-8 p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-sm animate-in slide-in-from-top-1 shadow-sm">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="font-bold text-xs sm:text-sm">{cartNotice}</span>
            <Link
              to="/cart"
              className="ml-2 font-bold text-blue-600 underline underline-offset-4 hover:text-blue-700 text-xs sm:text-sm inline-flex items-center gap-1"
            >
              Xem giỏ hàng &rarr;
            </Link>
          </div>
          <button
            onClick={() => setCartNotice(null)}
            className="text-emerald-700 hover:text-emerald-950 font-bold text-xs cursor-pointer px-3 py-1 rounded-full hover:bg-emerald-100"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Main Product Card */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden p-6 sm:p-10">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-14">
          
          {/* Product Media Area (TechNova Glow Showcase) */}
          <div className="flex flex-col items-center">
            <div className="aspect-square w-full max-w-lg bg-gradient-to-tr from-slate-50 via-white to-blue-50/40 rounded-3xl border border-slate-200/80 p-8 flex items-center justify-center relative shadow-sm overflow-hidden group">
              
              {/* Radial glow backdrop */}
              <div className="w-72 h-72 rounded-full bg-blue-500/10 blur-2xl absolute pointer-events-none" />

              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-contain mix-blend-multiply transition-transform duration-300 group-hover:scale-105"
                  onError={(e) => {
                    e.target.onerror = null
                    e.target.src = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=700&auto=format&fit=crop&q=80'
                  }}
                />
              ) : (
                <div className="flex flex-col items-center justify-center text-slate-300">
                  <ShoppingBag className="w-20 h-20 stroke-[1.25] text-slate-300 mb-3" />
                  <span className="text-xs text-slate-400 font-semibold">Rikkei Store</span>
                </div>
              )}

              {/* Status Badge */}
              <div className="absolute top-4 left-4">
                <span className="bg-orange-500 text-white text-[11px] font-bold px-3 py-1 rounded-md uppercase tracking-wider shadow-xs">
                  {isOutOfStock ? 'Hết hàng' : 'Best Seller'}
                </span>
              </div>

              {!isOutOfStock && (
                <div className="absolute top-4 right-4">
                  <span className="bg-emerald-500 text-white text-xs font-bold px-2.5 py-1 rounded-md shadow-xs">
                    -15% OFF
                  </span>
                </div>
              )}
            </div>

            {/* Sub-meta details */}
            <div className="mt-4 flex items-center justify-between w-full max-w-lg text-xs text-slate-500 px-2">
              <span className="font-mono">Mã SKU: #TN-{product.id}</span>
              <span className="flex items-center gap-1.5 text-slate-700 font-medium">
                <ShieldCheck className="w-4 h-4 text-blue-600" />
                100% Chính hãng ủy quyền
              </span>
            </div>
          </div>

          {/* Product Information */}
          <div className="flex flex-col justify-between">
            <div className="space-y-4">
              
              {/* Category Pill */}
              <div>
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200/70 uppercase tracking-wider">
                  <Zap className="w-3.5 h-3.5" />
                  <span>{product.categoryName || 'Tech Gear'}</span>
                </span>
              </div>

              {/* Product Title */}
              <h1 className="text-2xl sm:text-4xl font-extrabold text-slate-900 tracking-tight leading-snug">
                {product.name}
              </h1>

              {/* Rating row */}
              <div className="flex items-center gap-3 text-xs">
                <div className="flex text-amber-400">
                  {[...Array(5)].map((_, i) => (
                    <Star key={i} className="w-4 h-4 fill-current" />
                  ))}
                </div>
                <span className="font-bold text-slate-800">4.8</span>
                <span className="text-slate-400">•</span>
                <span className="text-slate-500 underline cursor-pointer">
                  {(product.id * 23) % 400 + 150} Đánh giá đã xác thực
                </span>
              </div>

              {/* Price Display */}
              <div className="p-5 bg-slate-50/80 rounded-2xl border border-slate-200/90 flex items-baseline gap-4 flex-wrap">
                <span className="text-3xl sm:text-4xl font-extrabold text-slate-900 tabular-nums">
                  {formatPrice(product.price)}
                </span>
                <span className="text-base text-slate-400 line-through">
                  {formatPrice(originalPrice)}
                </span>
                <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                  Tiết kiệm 15%
                </span>
              </div>

              {/* Stock Quantity Info */}
              <div className="flex items-center gap-2 text-sm pt-1">
                <span className="font-medium text-slate-600">Tình trạng:</span>
                {isOutOfStock ? (
                  <span className="font-bold text-red-600 inline-flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                    Tạm hết hàng (0 sản phẩm)
                  </span>
                ) : (
                  <span className="font-bold text-emerald-600 inline-flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                    Còn hàng ({product.stockQuantity} sản phẩm sẵn sàng giao)
                  </span>
                )}
              </div>

              {/* Description */}
              <div className="space-y-2 pt-2">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Thông tin & Đặc tính nổi bật
                </h3>
                <div className="text-slate-600 text-sm leading-relaxed whitespace-pre-line bg-slate-50/50 p-4 rounded-2xl border border-slate-200/70">
                  {product.description || 'Sản phẩm công nghệ chính hãng tiêu chuẩn cao cấp, hỗ trợ kiểm tra trước khi nhận hàng và bảo hành toàn diện.'}
                </div>
              </div>

              {/* Commercial Trust Pillars */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-100">
                <div className="flex items-center gap-2 text-xs text-slate-600">
                  <Truck className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Giao nhanh 48h</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-600">
                  <RotateCcw className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Đổi trả 7 ngày</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-600">
                  <ShieldCheck className="w-4 h-4 text-blue-600 shrink-0" />
                  <span>Bảo hành 2 năm</span>
                </div>
              </div>
            </div>

            {/* Actions: Quantity & Add to Cart & Buy Now */}
            <div className="pt-8 border-t border-slate-200 mt-6">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 mb-4">
                
                {/* Quantity selector */}
                <div className="flex items-center justify-between border border-slate-200 rounded-full bg-slate-50 px-2 py-1 shadow-xs">
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                    disabled={isOutOfStock || quantity <= 1}
                    className="w-8 h-8 rounded-full flex items-center justify-center text-slate-600 hover:bg-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Giảm số lượng"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="px-4 text-sm font-bold text-slate-900 select-none min-w-[36px] text-center tabular-nums">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.min(product.stockQuantity, prev + 1))}
                    disabled={isOutOfStock || quantity >= product.stockQuantity}
                    className="w-8 h-8 rounded-full flex items-center justify-center text-slate-600 hover:bg-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Tăng số lượng"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Add to Cart button: Royal Blue Pill */}
                <button
                  type="button"
                  onClick={handleAddToCart}
                  disabled={isOutOfStock}
                  className="flex-1 px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs sm:text-sm rounded-full shadow-md shadow-blue-500/20 disabled:opacity-40 disabled:pointer-events-none transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2"
                >
                  <ShoppingCart className="w-4 h-4" />
                  <span>{isOutOfStock ? 'Tạm hết hàng' : 'Thêm vào giỏ'}</span>
                </button>

                {/* Buy Now button: Vibrant Orange Pill */}
                <button
                  type="button"
                  onClick={handleBuyNow}
                  disabled={isOutOfStock}
                  className="flex-1 px-6 py-3 bg-orange-500 hover:bg-orange-600 text-white font-bold text-xs sm:text-sm rounded-full shadow-md shadow-orange-500/25 disabled:opacity-40 disabled:pointer-events-none transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2"
                >
                  <Zap className="w-4 h-4 fill-current" />
                  <span>Mua ngay</span>
                </button>
              </div>

              <div className="flex items-center justify-between text-xs text-slate-500 pt-2">
                <Link to="/products" className="hover:text-blue-600 transition-colors inline-flex items-center gap-1 font-semibold">
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Khám phá thêm sản phẩm Rikkei Store</span>
                </Link>
              </div>
            </div>

          </div>
        </div>

        {/* Phân hệ Đánh giá & Nhận xét sản phẩm [TSK-D12-DDM] */}
        {product && product.id && (
          <ProductReviewsSection productId={product.id} />
        )}
      </div>
    </div>
  )
}

export default ProductDetailPage
