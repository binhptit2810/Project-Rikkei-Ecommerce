import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  ShoppingCart,
  ExternalLink,
  Check,
  ShoppingBag,
  PackageX,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import LoadingSpinner from '../common/LoadingSpinner'

const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

/**
 * Thẻ hiển thị sản phẩm mini thu gọn trong bong bóng Chatbot AI.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D12-DDM] Xây Dựng Giao Diện Đánh Giá Sản Phẩm & Thẻ Sản Phẩm Trong Chat
 */
export const MiniProductCard = ({ product }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const addToCart = useCartStore((state) => state.addToCart)

  const [imgError, setImgError] = useState(false)
  const [isAdding, setIsAdding] = useState(false)
  const [toastMessage, setToastMessage] = useState(null)

  if (!product || !product.id) return null

  const isOutOfStock =
    product.stockQuantity === undefined ||
    product.stockQuantity === null ||
    Number(product.stockQuantity) <= 0

  const formattedPrice = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(product.price || 0)

  const imageSrc = !imgError && product.imageUrl ? product.imageUrl : FALLBACK_IMAGE

  // Điều hướng xem chi tiết sản phẩm
  const handleViewDetail = () => {
    navigate(`/products/${product.id}`)
  }

  // Thêm nhanh vào giỏ hàng ngay trong bong bóng chat
  const handleQuickAddToCart = async (e) => {
    e.stopPropagation()
    if (isOutOfStock || isAdding) return

    setIsAdding(true)
    try {
      const res = await addToCart(product, 1)

      if (res?.requireLogin) {
        navigate('/login', {
          state: {
            from: location,
            message: 'Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.',
          },
        })
        return
      }

      if (res?.isAdmin) {
        setToastMessage('Admin không thể mua hàng')
        setTimeout(() => setToastMessage(null), 3000)
        return
      }

      if (res?.success) {
        setToastMessage('Đã thêm vào giỏ hàng!')
        setTimeout(() => setToastMessage(null), 3000)
      } else {
        setToastMessage(res?.message || 'Hết hàng')
        setTimeout(() => setToastMessage(null), 3000)
      }
    } catch (err) {
      console.warn('Lỗi thêm giỏ hàng từ chat:', err)
      setToastMessage('Lỗi thêm giỏ hàng')
      setTimeout(() => setToastMessage(null), 3000)
    } finally {
      setIsAdding(false)
    }
  }

  return (
    <div className="relative flex-none w-[230px] sm:w-[250px] rounded-xl border border-gray-150 bg-white p-2.5 shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between group">
      {/* Toast thông báo nhanh nổi bên trong thẻ */}
      {toastMessage && (
        <div className="absolute -top-3 right-2 z-20 inline-flex items-center gap-1 px-2.5 py-1 bg-slate-900 text-white text-[10px] font-bold rounded-full shadow-lg animate-in fade-in slide-in-from-bottom-1">
          <Check className="w-3 h-3 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      <div className="flex gap-2.5">
        {/* Thumbnail ảnh vuông 64x64px */}
        <div
          onClick={handleViewDetail}
          className="relative w-16 h-16 min-w-[64px] min-h-[64px] rounded-lg overflow-hidden bg-slate-50 border border-slate-100 flex items-center justify-center p-1 cursor-pointer shrink-0"
        >
          <img
            src={imageSrc}
            alt={product.name || 'Sản phẩm'}
            onError={() => setImgError(true)}
            loading="lazy"
            className="w-full h-full object-contain mix-blend-multiply transition-transform duration-200 group-hover:scale-105"
          />
          {isOutOfStock && (
            <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-2xs flex items-center justify-center text-[9px] font-bold text-white uppercase text-center px-0.5">
              Hết hàng
            </div>
          )}
        </div>

        {/* Thông tin sản phẩm */}
        <div className="flex-1 min-w-0">
          <h4
            onClick={handleViewDetail}
            className="text-xs font-semibold text-slate-800 line-clamp-2 leading-tight hover:text-blue-600 transition-colors cursor-pointer"
            title={product.name}
          >
            {product.name}
          </h4>
          <div className="mt-1 flex items-baseline gap-1">
            <span className="text-xs font-bold text-blue-600">
              {formattedPrice}
            </span>
          </div>
          {product.categoryName && (
            <span className="text-[10px] text-slate-400 truncate block mt-0.5">
              {product.categoryName}
            </span>
          )}
        </div>
      </div>

      {/* Hành động dưới chân thẻ */}
      <div className="mt-2.5 pt-2 border-t border-slate-100 flex items-center justify-between gap-1.5">
        <button
          type="button"
          onClick={handleViewDetail}
          className="inline-flex items-center gap-1 text-[11px] font-semibold text-slate-600 hover:text-blue-600 transition-colors cursor-pointer"
        >
          <span>Xem chi tiết</span>
          <ExternalLink className="w-3 h-3" />
        </button>

        <button
          type="button"
          disabled={isOutOfStock || isAdding}
          onClick={handleQuickAddToCart}
          className="p-1.5 rounded-lg bg-blue-50 hover:bg-blue-600 text-blue-600 hover:text-white disabled:opacity-40 disabled:pointer-events-none transition-all duration-150 cursor-pointer shadow-2xs"
          title="Thêm nhanh vào giỏ hàng"
          aria-label="Thêm nhanh vào giỏ hàng"
        >
          {isAdding ? (
            <LoadingSpinner size="sm" className="text-blue-600" />
          ) : isOutOfStock ? (
            <PackageX className="w-3.5 h-3.5 text-slate-400" />
          ) : (
            <ShoppingCart className="w-3.5 h-3.5" />
          )}
        </button>
      </div>
    </div>
  )
}

/**
 * Danh sách hiển thị cuộn ngang các Mini Product Cards dưới bong bóng chat của AI.
 */
export const MiniProductList = ({ products }) => {
  if (!products || !Array.isArray(products) || products.length === 0) {
    return null
  }

  return (
    <div className="w-full mt-2.5">
      <div className="text-[11px] font-medium text-slate-500 mb-1.5 flex items-center justify-between">
        <span className="flex items-center gap-1">
          <ShoppingBag className="w-3 h-3 text-blue-500" />
          <span>Sản phẩm gợi ý ({products.length}):</span>
        </span>
        {products.length > 1 && (
          <span className="text-[10px] text-slate-400 italic">Cuộn ngang &rarr;</span>
        )}
      </div>
      <div className="flex gap-3 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-slate-200">
        {products.map((product) => (
          <MiniProductCard key={product.id} product={product} />
        ))}
      </div>
    </div>
  )
}

export default MiniProductCard

