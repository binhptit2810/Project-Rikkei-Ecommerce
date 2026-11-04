import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ShoppingBag, ShoppingCart } from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * Component hiển thị thẻ sản phẩm (ProductCard)
 * Phong cách: shadcn/ui New York - Neutral/Zinc Palette
 */
export const ProductCard = ({ product, onAddToCart }) => {
  const [imgError, setImgError] = useState(false)
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()

  if (!product) return null

  const isOutOfStock =
    product.stockQuantity === undefined ||
    product.stockQuantity === null ||
    Number(product.stockQuantity) <= 0
  const stockCount = Number(product.stockQuantity) || 0

  // Định dạng giá chuẩn VNĐ
  const formattedPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(product.price || 0)

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

  return (
    <Link
      to={`/products/${product.id}`}
      className="group relative flex flex-col justify-between overflow-hidden rounded-lg border border-zinc-200 bg-white shadow-sm transition-all hover:border-zinc-300 hover:shadow-md"
    >
      <div>
        {/* Khung ảnh sản phẩm & các Badge */}
        <div className="aspect-square w-full bg-zinc-50 relative overflow-hidden flex items-center justify-center border-b border-zinc-100">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name || 'Sản phẩm'}
              onError={handleImageError}
              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="flex flex-col items-center justify-center text-zinc-300 w-full h-full">
              <ShoppingBag className="w-10 h-10 stroke-[1.25] text-zinc-300 mb-1" />
              <span className="text-xs text-zinc-400 font-medium">Hình ảnh sản phẩm</span>
            </div>
          )}

          {/* Badge tên danh mục */}
          {product.categoryName && (
            <span className="absolute top-2.5 left-2.5 bg-white/95 text-zinc-700 text-[11px] font-medium px-2 py-0.5 rounded-md backdrop-blur-xs border border-zinc-200 shadow-2xs">
              {product.categoryName}
            </span>
          )}

          {/* Badge trạng thái tồn kho */}
          {isOutOfStock ? (
            <span className="absolute top-2.5 right-2.5 bg-zinc-900 text-zinc-50 text-[10px] font-medium px-2 py-0.5 rounded-md uppercase tracking-wider">
              Hết hàng
            </span>
          ) : (
            <span className="absolute top-2.5 right-2.5 bg-zinc-100 text-zinc-700 text-[10px] font-medium px-2 py-0.5 rounded-md border border-zinc-200">
              Còn {stockCount}
            </span>
          )}
        </div>

        {/* Thông tin sản phẩm: Tên & Mô tả ngắn */}
        <div className="p-4 space-y-1">
          <h3 className="font-semibold text-zinc-900 text-sm group-hover:underline line-clamp-2 leading-snug">
            {product.name}
          </h3>
          <p className="text-zinc-500 text-xs line-clamp-2 leading-relaxed">
            {product.description || 'Sản phẩm chính hãng với tiêu chuẩn chất lượng cao.'}
          </p>
        </div>
      </div>

      {/* Footer: Giá bán & Nút Thêm vào giỏ hàng */}
      <div className="p-4 pt-3 border-t border-zinc-100 flex items-center justify-between gap-2 mt-auto">
        <div className="min-w-0">
          <span className="text-[10px] uppercase font-medium tracking-wider text-zinc-400 block">Giá bán</span>
          <span className="text-base font-semibold text-zinc-950 tabular-nums truncate block">
            {formattedPrice}
          </span>
        </div>

        <button
          type="button"
          onClick={handleAddToCartClick}
          disabled={isOutOfStock}
          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors cursor-pointer ${
            isOutOfStock
              ? 'bg-zinc-100 text-zinc-400 cursor-not-allowed border border-zinc-200'
              : 'bg-zinc-900 hover:bg-zinc-800 text-zinc-50 shadow-sm active:scale-[0.98]'
          }`}
          title={isOutOfStock ? 'Sản phẩm đã hết hàng' : 'Thêm vào giỏ hàng'}
        >
          <ShoppingCart className="w-3.5 h-3.5" />
          <span>{isOutOfStock ? 'Hết hàng' : 'Thêm giỏ'}</span>
        </button>
      </div>
    </Link>
  )
}

export default ProductCard
