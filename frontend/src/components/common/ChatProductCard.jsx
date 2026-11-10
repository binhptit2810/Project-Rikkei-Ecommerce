import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ExternalLink, PackageCheck, PackageX, ShoppingBag } from 'lucide-react'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * Thẻ hiển thị sản phẩm thu gọn (Product Card) trong giao diện hội thoại Chatbot.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TASK-CHAT-PRODUCT-CARDS]
 */
export const ChatProductCard = ({ product }) => {
  const [imgError, setImgError] = useState(false)

  if (!product || !product.id) return null

  const isOutOfStock =
    product.stockQuantity === undefined ||
    product.stockQuantity === null ||
    Number(product.stockQuantity) <= 0

  const formattedPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(product.price || 0)

  const handleImageError = () => {
    if (!imgError) {
      setImgError(true)
    }
  }

  const imageSrc = (!imgError && product.imageUrl) ? product.imageUrl : FALLBACK_IMAGE

  return (
    <div className="flex-none w-[190px] sm:w-[200px] bg-white rounded-xl border border-slate-200/90 shadow-2xs hover:shadow-md hover:border-indigo-300 transition-all duration-200 overflow-hidden flex flex-col justify-between">
      <div>
        {/* Khung ảnh Thumbnail */}
        <div className="relative aspect-[4/3] w-full bg-slate-50 overflow-hidden flex items-center justify-center p-2 border-b border-slate-100">
          <img
            src={imageSrc}
            alt={product.name || 'Sản phẩm'}
            onError={handleImageError}
            loading="lazy"
            className="w-full h-full object-contain mix-blend-multiply transition-transform duration-200 hover:scale-105"
          />
          {/* Badge Tồn kho */}
          {isOutOfStock ? (
            <span className="absolute top-1.5 left-1.5 inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-rose-100/90 text-rose-700 backdrop-blur-xs">
              <PackageX className="w-2.5 h-2.5" />
              <span>Hết hàng</span>
            </span>
          ) : (
            <span className="absolute top-1.5 left-1.5 inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-100/90 text-emerald-700 backdrop-blur-xs">
              <PackageCheck className="w-2.5 h-2.5" />
              <span>Còn hàng</span>
            </span>
          )}
        </div>

        {/* Thông tin chi tiết */}
        <div className="p-2.5">
          {product.categoryName && (
            <span className="text-[10px] font-medium text-slate-400 uppercase tracking-wider block truncate">
              {product.categoryName}
            </span>
          )}
          <h4
            className="text-xs font-semibold text-slate-800 line-clamp-2 mt-0.5 leading-snug hover:text-indigo-600 transition-colors"
            title={product.name}
          >
            {product.name}
          </h4>
          <div className="mt-1.5 flex items-baseline justify-between gap-1">
            <span className="text-xs font-bold text-indigo-600">
              {formattedPrice}
            </span>
            {product.stockQuantity > 0 && (
              <span className="text-[10px] text-slate-400 shrink-0">
                Kho: {product.stockQuantity}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Nút điều hướng xem chi tiết */}
      <div className="p-2.5 pt-0">
        <Link
          to={`/products/${product.id}`}
          className="w-full inline-flex items-center justify-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-indigo-600 bg-indigo-50/80 hover:bg-indigo-600 hover:text-white rounded-lg transition-colors cursor-pointer"
        >
          <span>Xem chi tiết</span>
          <ExternalLink className="w-3 h-3" />
        </Link>
      </div>
    </div>
  )
}

/**
 * Danh sách hiển thị cuộn ngang các Product Cards dưới câu trả lời của AI.
 */
export const ChatProductList = ({ products }) => {
  if (!products || !Array.isArray(products) || products.length === 0) {
    return null
  }

  return (
    <div className="w-full mt-2.5">
      <div className="text-[11px] font-medium text-slate-500 mb-1.5 flex items-center justify-between">
        <span className="flex items-center gap-1">
          <ShoppingBag className="w-3 h-3 text-indigo-500" />
          <span>Sản phẩm gợi ý ({products.length}):</span>
        </span>
        {products.length > 1 && (
          <span className="text-[10px] text-slate-400 italic">Cuộn ngang &rarr;</span>
        )}
      </div>
      <div className="flex gap-2.5 overflow-x-auto pb-2 pt-0.5 scrollbar-thin scrollbar-thumb-slate-200">
        {products.map((product) => (
          <ChatProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  )
}

export default ChatProductCard

