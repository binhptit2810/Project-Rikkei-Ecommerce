import React, { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
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
    setCartNotice(`Đã thêm ${quantity} "${product.name}" vào giỏ hàng!`)
    setTimeout(() => setCartNotice(null), 5000)
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
        <div className="text-5xl mb-4">🔍</div>
        <h2 className="text-2xl font-bold text-slate-800 mb-2">Không tìm thấy sản phẩm</h2>
        <p className="text-slate-500 mb-6 text-sm">{error || 'Sản phẩm bạn yêu cầu không tồn tại.'}</p>
        <Link
          to="/products"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-orange-600 text-white rounded-xl text-sm font-semibold hover:bg-orange-700 transition-colors"
        >
          &larr; Quay lại danh sách sản phẩm
        </Link>
      </div>
    )
  }

  const isOutOfStock = product.stockQuantity <= 0

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 flex-1 w-full">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-xs text-slate-500 mb-6">
        <Link to="/" className="hover:text-orange-600 transition-colors">
          Trang chủ
        </Link>
        <span>/</span>
        <Link to="/products" className="hover:text-orange-600 transition-colors">
          Sản phẩm
        </Link>
        {product.categoryName && (
          <>
            <span>/</span>
            <Link
              to={`/products?categoryId=${product.categoryId}`}
              className="hover:text-orange-600 transition-colors"
            >
              {product.categoryName}
            </Link>
          </>
        )}
        <span>/</span>
        <span className="text-slate-900 font-medium truncate max-w-[200px]">
          {product.name}
        </span>
      </nav>

      {/* Cart Feedback Notification */}
      {cartNotice && (
        <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-sm shadow-sm animate-fade-in">
          <div className="flex items-center gap-2.5">
            <span className="text-lg">✅</span>
            <span>{cartNotice}</span>
            <Link
              to="/cart"
              className="ml-2 font-bold text-orange-600 hover:text-orange-700 underline inline-flex items-center gap-1"
            >
              Xem giỏ hàng &rarr;
            </Link>
          </div>
          <button
            onClick={() => setCartNotice(null)}
            className="text-emerald-600 hover:text-emerald-900 font-bold text-xs cursor-pointer px-2 py-1 rounded hover:bg-emerald-100"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Main Product Card */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden p-6 sm:p-10">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
          {/* Product Media Area */}
          <div className="flex flex-col items-center">
            <div className="aspect-square w-full max-w-md bg-slate-50 rounded-2xl border border-slate-100 overflow-hidden flex items-center justify-center relative">
              {product.imageUrl ? (
                <img
                  src={product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.target.onerror = null
                    e.target.src = 'https://placehold.co/500x500?text=Sản+Phẩm'
                  }}
                />
              ) : (
                <div className="flex flex-col items-center justify-center text-slate-300">
                  <span className="text-6xl mb-2">🛍️</span>
                  <span className="text-sm text-slate-400 font-medium">Chưa có ảnh mô tả</span>
                </div>
              )}

              {/* Status Badge */}
              {isOutOfStock ? (
                <span className="absolute top-4 right-4 bg-red-600 text-white text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider shadow-md">
                  Hết hàng
                </span>
              ) : (
                <span className="absolute top-4 right-4 bg-emerald-600 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md">
                  Còn hàng
                </span>
              )}
            </div>
          </div>

          {/* Product Information */}
          <div className="flex flex-col justify-between">
            <div>
              {/* Category Pill */}
              {product.categoryName && (
                <div className="mb-2">
                  <span className="inline-block bg-orange-50 text-orange-600 text-xs font-bold px-3 py-1 rounded-full border border-orange-200/60">
                    {product.categoryName}
                  </span>
                </div>
              )}

              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight mb-3">
                {product.name}
              </h1>

              {/* Price Display */}
              <div className="flex items-baseline gap-3 mb-6 p-4 bg-orange-50/50 rounded-2xl border border-orange-100">
                <span className="text-3xl font-black text-orange-600">
                  {formatPrice(product.price)}
                </span>
                <span className="text-xs text-slate-500">Đã bao gồm thuế VAT</span>
              </div>

              {/* Stock Quantity Info (Thống nhất với module Inventory của Đặng Thanh Bình) */}
              <div className="flex items-center gap-3 mb-6 text-sm">
                <span className="font-semibold text-slate-700">Tồn kho hiện tại:</span>
                {isOutOfStock ? (
                  <span className="font-bold text-red-600">Tạm thời hết hàng (0 sản phẩm)</span>
                ) : (
                  <span className="font-bold text-emerald-600">
                    Còn {product.stockQuantity} sản phẩm sẵn sàng giao
                  </span>
                )}
              </div>

              {/* Description */}
              <div className="mb-8">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                  Mô tả sản phẩm
                </h3>
                <div className="text-slate-600 text-sm leading-relaxed whitespace-pre-line bg-slate-50 p-4 rounded-xl border border-slate-100">
                  {product.description || 'Chưa có mô tả chi tiết cho sản phẩm này.'}
                </div>
              </div>
            </div>

            {/* Actions: Quantity & Add to Cart */}
            <div className="pt-6 border-t border-slate-200">
              <div className="flex flex-wrap items-center gap-4 mb-4">
                <div className="flex items-center border border-slate-200 rounded-xl bg-white overflow-hidden shadow-sm">
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.max(1, prev - 1))}
                    disabled={isOutOfStock || quantity <= 1}
                    className="px-3.5 py-2.5 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-bold"
                  >
                    -
                  </button>
                  <span className="px-4 py-2 text-sm font-bold text-slate-800 select-none min-w-[40px] text-center">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((prev) => Math.min(product.stockQuantity, prev + 1))}
                    disabled={isOutOfStock || quantity >= product.stockQuantity}
                    className="px-3.5 py-2.5 text-slate-600 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed font-bold"
                  >
                    +
                  </button>
                </div>

                <button
                  type="button"
                  onClick={handleAddToCart}
                  disabled={isOutOfStock}
                  className="flex-1 px-6 py-3 bg-orange-600 hover:bg-orange-700 active:scale-95 text-white font-bold text-sm rounded-xl shadow-md shadow-orange-600/20 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <span>🛒</span>
                  {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ hàng'}
                </button>
              </div>

              <div className="flex items-center justify-between text-xs text-slate-400">
                <Link to="/products" className="hover:text-orange-600 transition-colors inline-flex items-center gap-1">
                  &larr; Xem sản phẩm khác
                </Link>
                <span>Mã sản phẩm: #{product.id}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProductDetailPage
