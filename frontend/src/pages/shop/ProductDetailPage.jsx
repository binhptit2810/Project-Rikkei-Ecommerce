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
  Share2,
} from 'lucide-react'
import productService from '../../services/productService'
import { useCartStore } from '../../store/useCartStore'
import LoadingSpinner from '../../components/common/LoadingSpinner'

export const ProductDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const addToCart = useCartStore((state) => state.addToCart)

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
    if (!product || product.stockQuantity <= 0) return
    await addToCart(product, quantity)
    setCartNotice(`Đã thêm ${quantity} sản phẩm vào giỏ hàng thành công!`)
    setTimeout(() => setCartNotice(null), 5000)
  }

  const handleBuyNow = async () => {
    if (!product || product.stockQuantity <= 0) return
    await addToCart(product, quantity)
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
        <div className="w-16 h-16 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-4">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 mb-2">Không tìm thấy sản phẩm</h2>
        <p className="text-slate-500 mb-6 text-sm">{error || 'Sản phẩm bạn yêu cầu không tồn tại.'}</p>
        <Link
          to="/products"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-semibold hover:bg-orange-700 transition-colors shadow-2xs"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách sản phẩm</span>
        </Link>
      </div>
    )
  }

  const isOutOfStock = product.stockQuantity <= 0

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
        <Link to="/" className="hover:text-orange-600 transition-colors">
          Trang chủ
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
        <Link to="/products" className="hover:text-orange-600 transition-colors">
          Sản phẩm
        </Link>
        {product.categoryName && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
            <Link
              to={`/products?categoryId=${product.categoryId}`}
              className="hover:text-orange-600 transition-colors"
            >
              {product.categoryName}
            </Link>
          </>
        )}
        <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
        <span className="text-slate-900 font-semibold truncate max-w-[200px] sm:max-w-md">
          {product.name}
        </span>
      </nav>

      {/* Cart Feedback Notification */}
      {cartNotice && (
        <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-sm shadow-2xs animate-fade-in">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span className="font-medium">{cartNotice}</span>
            <Link
              to="/cart"
              className="ml-2 font-bold text-orange-600 hover:text-orange-700 underline inline-flex items-center gap-1"
            >
              Xem giỏ hàng &rarr;
            </Link>
          </div>
          <button
            onClick={() => setCartNotice(null)}
            className="text-emerald-700 hover:text-emerald-950 font-bold text-xs cursor-pointer px-2 py-1 rounded hover:bg-emerald-100"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Main Product Card */}
      <div className="bg-white rounded-3xl border border-slate-200/80 shadow-card overflow-hidden p-6 sm:p-10">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
          {/* Product Media Area */}
          <div className="flex flex-col items-center">
            <div className="aspect-square w-full max-w-md bg-slate-50 rounded-2xl border border-slate-200/80 overflow-hidden flex items-center justify-center relative shadow-2xs">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.target.onerror = null
                    e.target.src = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=700&auto=format&fit=crop&q=80'
                  }}
                />
              ) : (
                <div className="flex flex-col items-center justify-center text-slate-300">
                  <ShoppingBag className="w-16 h-16 stroke-[1.25] text-slate-300 mb-2" />
                  <span className="text-xs text-slate-400 font-medium">Hình ảnh sản phẩm</span>
                </div>
              )}

              {/* Status Badge */}
              {isOutOfStock ? (
                <span className="absolute top-4 right-4 bg-red-600 text-white text-xs font-bold px-3 py-1 rounded-xl uppercase tracking-wider shadow-sm">
                  Tạm hết hàng
                </span>
              ) : (
                <span className="absolute top-4 right-4 bg-emerald-600 text-white text-xs font-semibold px-3 py-1 rounded-xl shadow-sm">
                  Còn hàng
                </span>
              )}
            </div>

            {/* Sub-meta details */}
            <div className="mt-4 flex items-center justify-between w-full max-w-md text-xs text-slate-400">
              <span>Mã SP: #{product.id}</span>
              <span className="flex items-center gap-1 text-slate-500">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                Sản phẩm được bảo đảm chính hãng
              </span>
            </div>
          </div>

          {/* Product Information */}
          <div className="flex flex-col justify-between">
            <div>
              {/* Category Pill */}
              {product.categoryName && (
                <div className="mb-3">
                  <span className="inline-block bg-orange-50 text-orange-600 text-xs font-bold px-3 py-1 rounded-lg border border-orange-200/60">
                    {product.categoryName}
                  </span>
                </div>
              )}

              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight mb-4">
                {product.name}
              </h1>

              {/* Price Display */}
              <div className="flex items-baseline gap-3 mb-6 p-4 bg-orange-50/40 rounded-2xl border border-orange-100">
                <span className="text-3xl sm:text-4xl font-extrabold text-orange-600 tabular-nums">
                  {formatPrice(product.price)}
                </span>
                <span className="text-xs text-slate-500">Đã bao gồm thuế GTGT</span>
              </div>

              {/* Stock Quantity Info */}
              <div className="flex items-center gap-2.5 mb-6 text-sm">
                <span className="font-semibold text-slate-700">Tình trạng kho:</span>
                {isOutOfStock ? (
                  <span className="font-bold text-red-600 inline-flex items-center gap-1">
                    <AlertCircle className="w-4 h-4" /> Tạm hết hàng (0 sản phẩm)
                  </span>
                ) : (
                  <span className="font-bold text-emerald-600 inline-flex items-center gap-1">
                    <CheckCircle2 className="w-4 h-4" /> Còn {product.stockQuantity} sản phẩm sẵn sàng giao
                  </span>
                )}
              </div>

              {/* Description */}
              <div className="mb-6">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">
                  Mô tả sản phẩm
                </h3>
                <div className="text-slate-600 text-sm leading-relaxed whitespace-pre-line bg-slate-50/70 p-4 rounded-xl border border-slate-100">
                  {product.description || 'Sản phẩm chính hãng với đầy đủ chế độ bảo hành và hỗ trợ kỹ thuật.'}
                </div>
              </div>

              {/* Commercial Trust Pillars */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8 pt-4 border-t border-slate-100">
                <div className="flex items-center gap-2.5 text-xs text-slate-600">
                  <Truck className="w-4 h-4 text-orange-600 shrink-0" />
                  <span>Giao toàn quốc 2-3 ngày</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-600">
                  <RotateCcw className="w-4 h-4 text-orange-600 shrink-0" />
                  <span>Đổi trả trong 7 ngày</span>
                </div>
                <div className="flex items-center gap-2.5 text-xs text-slate-600">
                  <ShieldCheck className="w-4 h-4 text-orange-600 shrink-0" />
                  <span>100% Chính hãng</span>
                </div>
              </div>
            </div>

            {/* Actions: Quantity & Add to Cart */}
            <div className="pt-6 border-t border-slate-200">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 mb-4">
                {/* Quantity selector */}
                <div className="flex items-center justify-between border border-slate-200 rounded-xl bg-white shadow-2xs">
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                    disabled={isOutOfStock || quantity <= 1}
                    className="p-3 text-slate-600 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                    aria-label="Giảm số lượng"
                  >
                    <Minus className="w-4 h-4" />
                  </button>
                  <span className="px-4 text-sm font-bold text-slate-900 select-none min-w-[40px] text-center tabular-nums">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.min(product.stockQuantity, prev + 1))}
                    disabled={isOutOfStock || quantity >= product.stockQuantity}
                    className="p-3 text-slate-600 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                    aria-label="Tăng số lượng"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>

                {/* Add to Cart button */}
                <button
                  type="button"
                  onClick={handleAddToCart}
                  disabled={isOutOfStock}
                  className="flex-1 px-6 py-3 bg-white hover:bg-slate-50 text-slate-900 border border-slate-300 font-bold text-sm rounded-xl shadow-2xs disabled:opacity-40 disabled:cursor-not-allowed transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <ShoppingCart className="w-4 h-4 text-orange-600" />
                  <span>{isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}</span>
                </button>

                {/* Buy Now button */}
                <button
                  type="button"
                  onClick={handleBuyNow}
                  disabled={isOutOfStock}
                  className="flex-1 px-6 py-3 bg-orange-600 hover:bg-orange-700 active:scale-[0.99] text-white font-bold text-sm rounded-xl shadow-2xs disabled:opacity-40 disabled:cursor-not-allowed transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <ShoppingBag className="w-4 h-4" />
                  <span>Mua ngay</span>
                </button>
              </div>

              <div className="flex items-center justify-between text-xs text-slate-400">
                <Link to="/products" className="hover:text-orange-600 transition-colors inline-flex items-center gap-1 font-medium">
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Xem thêm sản phẩm khác</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
