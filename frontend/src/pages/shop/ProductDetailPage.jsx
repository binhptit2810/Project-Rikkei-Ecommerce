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
        <div className="w-12 h-12 rounded-lg bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-3 border border-red-200">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-semibold text-zinc-950 mb-1">Không tìm thấy sản phẩm</h2>
        <p className="text-zinc-500 mb-6 text-sm">{error || 'Sản phẩm bạn yêu cầu không tồn tại.'}</p>
        <Link
          to="/products"
          className="inline-flex items-center gap-2 px-4 py-2 bg-zinc-900 text-zinc-50 rounded-md text-sm font-medium hover:bg-zinc-800 transition-colors shadow-sm"
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
      <nav className="flex items-center gap-1.5 text-xs text-zinc-500 mb-6">
        <Link to="/" className="hover:text-zinc-950 transition-colors">
          Trang chủ
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-zinc-400" />
        <Link to="/products" className="hover:text-zinc-950 transition-colors">
          Sản phẩm
        </Link>
        {product.categoryName && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-zinc-400" />
            <Link
              to={`/products?categoryId=${product.categoryId}`}
              className="hover:text-zinc-950 transition-colors"
            >
              {product.categoryName}
            </Link>
          </>
        )}
        <ChevronRight className="w-3.5 h-3.5 text-zinc-400" />
        <span className="text-zinc-900 font-medium truncate max-w-[200px] sm:max-w-md">
          {product.name}
        </span>
      </nav>

      {/* Cart Feedback Notification */}
      {cartNotice && (
        <div className="mb-6 p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-md flex flex-wrap items-center justify-between gap-3 text-sm animate-in slide-in-from-top-1">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium text-xs sm:text-sm">{cartNotice}</span>
            <Link
              to="/cart"
              className="ml-2 font-medium text-zinc-950 underline underline-offset-4 hover:text-zinc-700 text-xs sm:text-sm inline-flex items-center gap-1"
            >
              Xem giỏ hàng &rarr;
            </Link>
          </div>
          <button
            onClick={() => setCartNotice(null)}
            className="text-emerald-700 hover:text-emerald-950 font-medium text-xs cursor-pointer px-2 py-0.5 rounded hover:bg-emerald-100"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Main Product Card */}
      <div className="bg-white rounded-lg border border-zinc-200 shadow-sm overflow-hidden p-6 sm:p-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
          {/* Product Media Area */}
          <div className="flex flex-col items-center">
            <div className="aspect-square w-full max-w-md bg-zinc-50 rounded-lg border border-zinc-200 overflow-hidden flex items-center justify-center relative shadow-xs">
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
                <div className="flex flex-col items-center justify-center text-zinc-300">
                  <ShoppingBag className="w-16 h-16 stroke-[1.25] text-zinc-300 mb-2" />
                  <span className="text-xs text-zinc-400 font-medium">Hình ảnh sản phẩm</span>
                </div>
              )}

              {/* Status Badge */}
              {isOutOfStock ? (
                <span className="absolute top-3 right-3 bg-zinc-900 text-zinc-50 text-xs font-medium px-2.5 py-1 rounded-md uppercase tracking-wider">
                  Tạm hết hàng
                </span>
              ) : (
                <span className="absolute top-3 right-3 bg-zinc-100 text-zinc-800 text-xs font-medium px-2.5 py-1 rounded-md border border-zinc-200">
                  Còn hàng
                </span>
              )}
            </div>

            {/* Sub-meta details */}
            <div className="mt-3 flex items-center justify-between w-full max-w-md text-xs text-zinc-500">
              <span>Mã SP: #{product.id}</span>
              <span className="flex items-center gap-1 text-zinc-600">
                <ShieldCheck className="w-3.5 h-3.5 text-zinc-700" />
                Sản phẩm chính hãng
              </span>
            </div>
          </div>

          {/* Product Information */}
          <div className="flex flex-col justify-between">
            <div>
              {/* Category Pill */}
              {product.categoryName && (
                <div className="mb-2.5">
                  <span className="inline-block bg-zinc-100 text-zinc-800 text-xs font-medium px-2.5 py-0.5 rounded-md border border-zinc-200">
                    {product.categoryName}
                  </span>
                </div>
              )}

              <h1 className="text-2xl sm:text-3xl font-semibold text-zinc-950 tracking-tight mb-4">
                {product.name}
              </h1>

              {/* Price Display */}
              <div className="flex items-baseline gap-3 mb-6 p-4 bg-zinc-50 rounded-md border border-zinc-200">
                <span className="text-3xl sm:text-4xl font-semibold text-zinc-950 tabular-nums">
                  {formatPrice(product.price)}
                </span>
                <span className="text-xs text-zinc-500">Đã bao gồm VAT</span>
              </div>

              {/* Stock Quantity Info */}
              <div className="flex items-center gap-2 mb-6 text-sm">
                <span className="font-medium text-zinc-700">Tình trạng kho:</span>
                {isOutOfStock ? (
                  <span className="font-medium text-red-600 inline-flex items-center gap-1">
                    <AlertCircle className="w-4 h-4" /> Tạm hết hàng (0 sản phẩm)
                  </span>
                ) : (
                  <span className="font-medium text-emerald-600 inline-flex items-center gap-1">
                    <CheckCircle2 className="w-4 h-4" /> Còn {product.stockQuantity} sản phẩm trong kho
                  </span>
                )}
              </div>

              {/* Description */}
              <div className="mb-6 space-y-1.5">
                <h3 className="text-xs font-medium uppercase tracking-wider text-zinc-500">
                  Mô tả sản phẩm
                </h3>
                <div className="text-zinc-600 text-sm leading-relaxed whitespace-pre-line bg-zinc-50/50 p-4 rounded-md border border-zinc-200">
                  {product.description || 'Sản phẩm chính hãng với đầy đủ chế độ bảo hành và hỗ trợ kỹ thuật.'}
                </div>
              </div>

              {/* Commercial Trust Pillars */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-8 pt-4 border-t border-zinc-100">
                <div className="flex items-center gap-2 text-xs text-zinc-600">
                  <Truck className="w-4 h-4 text-zinc-700 shrink-0" />
                  <span>Giao toàn quốc 2-3 ngày</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-zinc-600">
                  <RotateCcw className="w-4 h-4 text-zinc-700 shrink-0" />
                  <span>Đổi trả trong 7 ngày</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-zinc-600">
                  <ShieldCheck className="w-4 h-4 text-zinc-700 shrink-0" />
                  <span>100% Chính hãng</span>
                </div>
              </div>
            </div>

            {/* Actions: Quantity & Add to Cart */}
            <div className="pt-6 border-t border-zinc-200">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 mb-4">
                {/* Quantity selector */}
                <div className="flex items-center justify-between border border-zinc-200 rounded-md bg-white shadow-xs">
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                    disabled={isOutOfStock || quantity <= 1}
                    className="p-2.5 text-zinc-600 hover:bg-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Giảm số lượng"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="px-3 text-sm font-medium text-zinc-900 select-none min-w-[36px] text-center tabular-nums">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.min(product.stockQuantity, prev + 1))}
                    disabled={isOutOfStock || quantity >= product.stockQuantity}
                    className="p-2.5 text-zinc-600 hover:bg-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
                    aria-label="Tăng số lượng"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Add to Cart button */}
                <button
                  type="button"
                  onClick={handleAddToCart}
                  disabled={isOutOfStock}
                  className="flex-1 px-4 py-2 bg-white hover:bg-zinc-100 text-zinc-900 border border-zinc-200 font-medium text-sm rounded-md shadow-xs disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <ShoppingCart className="w-4 h-4 text-zinc-700" />
                  <span>{isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}</span>
                </button>

                {/* Buy Now button */}
                <button
                  type="button"
                  onClick={handleBuyNow}
                  disabled={isOutOfStock}
                  className="flex-1 px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-50 font-medium text-sm rounded-md shadow-sm disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer flex items-center justify-center gap-2"
                >
                  <ShoppingBag className="w-4 h-4" />
                  <span>Mua ngay</span>
                </button>
              </div>

              <div className="flex items-center justify-between text-xs text-zinc-500">
                <Link to="/products" className="hover:text-zinc-900 transition-colors inline-flex items-center gap-1 font-medium">
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
