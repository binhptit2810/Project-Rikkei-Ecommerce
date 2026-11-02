import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, ShoppingCart } from 'lucide-react'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * Component hiển thị thẻ sản phẩm (ProductCard)
 * Hoàn thành thẻ [TSK-D04-DHA]: Xây dựng Component ProductCard & Tích hợp Trang Shop với API thật
 *
 * Props:
 * - product: Thông tin sản phẩm { id, name, description, price, imageUrl, categoryName, stockQuantity }
 * - onAddToCart: Callback khi bấm nút thêm vào giỏ hàng
 */
export const ProductCard = ({ product, onAddToCart }) => {
  const [imgError, setImgError] = useState(false)

  if (!product) return null

  const isOutOfStock =
    product.stockQuantity === undefined ||
    product.stockQuantity === null ||
    Number(product.stockQuantity) <= 0
  const stockCount = Number(product.stockQuantity) || 0

  // Định dạng giá chuẩn VNĐ theo yêu cầu
  const formattedPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(product.price || 0)

  // Xử lý sự kiện click Thêm vào giỏ hàng
  const handleAddToCartClick = (e) => {
    e.preventDefault()
    e.stopPropagation()
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
      className="group bg-white rounded-2xl border border-slate-200/80 shadow-2xs hover:shadow-card hover:border-orange-300 transition-all duration-200 overflow-hidden flex flex-col justify-between"
    >
      <div>
        {/* Khung ảnh sản phẩm & các Badge */}
        <div className="aspect-square w-full bg-slate-50 relative overflow-hidden flex items-center justify-center">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name || 'Sản phẩm'}
              onError={handleImageError}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          ) : (
            <div className="flex flex-col items-center justify-center text-slate-300 w-full h-full">
              <ShoppingBag className="w-12 h-12 stroke-[1.25] text-slate-300 mb-1" />
              <span className="text-xs text-slate-400 font-medium">Hình ảnh sản phẩm</span>
            </div>
          )}

          {/* Badge tên danh mục */}
          {product.categoryName && (
            <span className="absolute top-3 left-3 bg-white/90 text-slate-800 text-[11px] font-semibold px-2.5 py-1 rounded-lg backdrop-blur-xs border border-slate-200/60 shadow-2xs">
              {product.categoryName}
            </span>
          )}

          {/* Badge trạng thái tồn kho */}
          {isOutOfStock ? (
            <span className="absolute top-3 right-3 bg-red-600 text-white text-[10px] font-bold px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-2xs">
              Hết hàng
            </span>
          ) : (
            <span className="absolute top-3 right-3 bg-emerald-600 text-white text-[10px] font-semibold px-2.5 py-1 rounded-lg shadow-2xs">
              Còn {stockCount}
            </span>
          )}
        </div>

        {/* Thông tin sản phẩm: Tên & Mô tả ngắn */}
        <div className="p-4">
          <h3 className="font-bold text-slate-900 text-sm group-hover:text-orange-600 transition-colors line-clamp-2 mb-1.5 leading-snug">
            {product.name}
          </h3>
          <p className="text-slate-500 text-xs line-clamp-2 leading-relaxed">
            {product.description || 'Sản phẩm chính hãng với tiêu chuẩn chất lượng cao.'}
          </p>
        </div>
      </div>

      {/* Footer: Giá bán & Nút Thêm vào giỏ hàng */}
      <div className="p-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2 mt-auto">
        <div className="min-w-0">
          <span className="text-[11px] text-slate-400 block font-medium">Giá bán</span>
          <span className="text-base font-extrabold text-orange-600 tabular-nums truncate block">
            {formattedPrice}
          </span>
        </div>

        <button
          type="button"
          onClick={handleAddToCartClick}
          disabled={isOutOfStock}
          className={`inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
            isOutOfStock
              ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200'
              : 'bg-orange-600 hover:bg-orange-700 text-white shadow-2xs hover:shadow active:scale-95'
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
