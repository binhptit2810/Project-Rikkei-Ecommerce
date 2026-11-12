import React, { useState, useEffect, useCallback } from 'react'
import {
  X,
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  RotateCcw,
  Image as ImageIcon,
} from 'lucide-react'

/**
 * Helper chuẩn hóa URL hình ảnh:
 * Xử lý an toàn cả relative URL (/uploads/...), absolute URL (http/https),
 * blob URL và base64 data URL.
 */
export const resolveImageUrl = (url) => {
  if (!url) return ''
  const actualUrl = typeof url === 'object' && url !== null ? (url.imageUrl || url.url || '') : url
  if (!actualUrl || typeof actualUrl !== 'string') return ''
  if (
    actualUrl.startsWith('http://') ||
    actualUrl.startsWith('https://') ||
    actualUrl.startsWith('blob:') ||
    actualUrl.startsWith('data:')
  ) {
    return actualUrl
  }

  // Nếu là đường dẫn tương đối từ backend server
  const apiBase = import.meta.env.VITE_API_URL || 'http://localhost:8080'
  const cleanBase = apiBase.endsWith('/') ? apiBase.slice(0, -1) : apiBase
  const cleanPath = actualUrl.startsWith('/') ? actualUrl : `/${actualUrl}`

  // Tránh trùng lặp /api/v1 nếu API_URL đã chứa /api/v1
  if (cleanBase.endsWith('/api/v1') && cleanPath.startsWith('/api/v1/')) {
    return `${cleanBase.replace(/\/api\/v1$/, '')}${cleanPath}`
  }
  return `${cleanBase}${cleanPath}`
}

/**
 * Modal Lightbox Phóng To Ảnh Đánh Giá Sắc Nét.
 * Phụ trách: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 *
 * Checklist [2]:
 * - Nền tối mờ sang trọng (fixed inset-0 z-50 bg-black/85 backdrop-blur-sm).
 * - Hiển thị ảnh lớn, sắc nét, hỗ trợ Zoom In / Zoom Out / Reset Zoom.
 * - Điều hướng Prev / Next, bộ đếm "Ảnh X / Y", dải thumbnail chuyển ảnh.
 * - Hỗ trợ phím ESC, mũi tên Trái/Phải, click ngoài nền để đóng.
 */
export const ReviewImageLightbox = ({
  isOpen,
  images = [],
  currentIndex = 0,
  onClose,
  onNavigate,
}) => {
  const [zoomLevel, setZoomLevel] = useState(1)
  const [imageError, setImageError] = useState(false)

  const activeIndex = Math.max(0, Math.min(currentIndex, (images?.length || 1) - 1))
  const currentImage = images?.[activeIndex]
  const total = images?.length || 0

  // Reset zoom và error mỗi khi đổi ảnh hoặc đóng/mở modal
  useEffect(() => {
    setZoomLevel(1)
    setImageError(false)
  }, [activeIndex, isOpen])

  const handlePrev = useCallback(
    (e) => {
      e?.stopPropagation()
      if (total > 1 && onNavigate) {
        const nextIndex = activeIndex > 0 ? activeIndex - 1 : total - 1
        onNavigate(nextIndex)
      }
    },
    [activeIndex, total, onNavigate]
  )

  const handleNext = useCallback(
    (e) => {
      e?.stopPropagation()
      if (total > 1 && onNavigate) {
        const nextIndex = activeIndex < total - 1 ? activeIndex + 1 : 0
        onNavigate(nextIndex)
      }
    },
    [activeIndex, total, onNavigate]
  )

  const handleZoomIn = (e) => {
    e?.stopPropagation()
    setZoomLevel((prev) => Math.min(prev + 0.35, 3))
  }

  const handleZoomOut = (e) => {
    e?.stopPropagation()
    setZoomLevel((prev) => Math.max(prev - 0.35, 0.7))
  }

  const handleResetZoom = (e) => {
    e?.stopPropagation()
    setZoomLevel(1)
  }

  // Bắt sự kiện bàn phím: ESC để đóng, Mũi tên Trái/Phải để chuyển ảnh
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose?.()
      } else if (e.key === 'ArrowLeft') {
        handlePrev()
      } else if (e.key === 'ArrowRight') {
        handleNext()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    // Khóa cuộn body khi lightbox đang mở
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = originalOverflow
    }
  }, [isOpen, onClose, handlePrev, handleNext])

  if (!isOpen || !images || images.length === 0) return null

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Xem ảnh phóng to"
      onClick={onClose}
      className="fixed inset-0 z-50 flex flex-col justify-between bg-black/90 backdrop-blur-md select-none animate-in fade-in duration-200 p-4 sm:p-6"
    >
      {/* 1. Header Toolbar */}
      <div
        className="flex items-center justify-between z-10 w-full max-w-7xl mx-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Bộ đếm ảnh */}
        <div className="flex items-center gap-2 text-white/90 text-sm font-semibold bg-white/10 px-3.5 py-1.5 rounded-full border border-white/15 backdrop-blur-sm">
          <ImageIcon className="w-4 h-4 text-amber-400" />
          <span>
            Ảnh {activeIndex + 1} / {total}
          </span>
        </div>

        {/* Nút công cụ Zoom & Đóng */}
        <div className="flex items-center gap-1.5 sm:gap-2">
          {/* Zoom In */}
          <button
            type="button"
            onClick={handleZoomIn}
            disabled={zoomLevel >= 3}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer border border-white/10"
            title="Phóng to ảnh (+)"
          >
            <ZoomIn className="w-4 h-4" />
          </button>

          {/* Zoom Out */}
          <button
            type="button"
            onClick={handleZoomOut}
            disabled={zoomLevel <= 0.7}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer border border-white/10"
            title="Thu nhỏ ảnh (-)"
          >
            <ZoomOut className="w-4 h-4" />
          </button>

          {/* Reset Zoom */}
          {zoomLevel !== 1 && (
            <button
              type="button"
              onClick={handleResetZoom}
              className="px-2.5 py-1.5 rounded-full bg-white/10 hover:bg-white/20 text-xs font-semibold text-amber-300 transition-colors cursor-pointer flex items-center gap-1 border border-white/10"
              title="Đặt lại kích thước gốc"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>100%</span>
            </button>
          )}

          {/* Nút Đóng (ESC) */}
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-full bg-white/15 hover:bg-red-600 text-white transition-colors cursor-pointer ml-1 border border-white/20 shadow-lg"
            title="Đóng (ESC)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 2. Main Image Canvas & Floating Navigation Buttons */}
      <div className="relative flex-1 flex items-center justify-center my-2 overflow-hidden w-full max-w-7xl mx-auto">
        {/* Nút Prev (Ảnh trước) */}
        {total > 1 && (
          <button
            type="button"
            onClick={handlePrev}
            className="absolute left-2 sm:left-4 z-20 p-2.5 sm:p-3 rounded-full bg-black/50 hover:bg-white/20 text-white border border-white/15 backdrop-blur-sm transition-all hover:scale-110 active:scale-95 cursor-pointer shadow-xl"
            title="Ảnh trước (Mũi tên trái)"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
        )}

        {/* Khung chứa ảnh phóng to */}
        <div
          className="relative max-h-[82vh] max-w-[90vw] flex items-center justify-center transition-transform duration-200"
          style={{ transform: `scale(${zoomLevel})` }}
          onClick={(e) => e.stopPropagation()}
        >
          {imageError ? (
            <div className="p-8 rounded-3xl bg-white/10 border border-white/20 text-center text-white/80 max-w-sm">
              <ImageIcon className="w-12 h-12 mx-auto text-amber-400 mb-3 opacity-60" />
              <p className="text-sm font-semibold">Không thể tải hình ảnh này</p>
              <p className="text-xs text-white/50 mt-1">Đường dẫn tệp có thể đã thay đổi hoặc bị xóa.</p>
            </div>
          ) : (
            <img
              src={resolveImageUrl(currentImage)}
              alt={`Hình ảnh đánh giá ${activeIndex + 1}`}
              onError={() => setImageError(true)}
              className="max-h-[80vh] max-w-[90vw] object-contain rounded-2xl shadow-2xl transition-all select-none"
            />
          )}
        </div>

        {/* Nút Next (Ảnh tiếp theo) */}
        {total > 1 && (
          <button
            type="button"
            onClick={handleNext}
            className="absolute right-2 sm:right-4 z-20 p-2.5 sm:p-3 rounded-full bg-black/50 hover:bg-white/20 text-white border border-white/15 backdrop-blur-sm transition-all hover:scale-110 active:scale-95 cursor-pointer shadow-xl"
            title="Ảnh tiếp theo (Mũi tên phải)"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        )}
      </div>

      {/* 3. Footer Thumbnail Strip (Nếu có nhiều hơn 1 ảnh) */}
      {total > 1 && (
        <div
          className="flex items-center justify-center gap-2.5 z-10 w-full max-w-7xl mx-auto overflow-x-auto py-1"
          onClick={(e) => e.stopPropagation()}
        >
          {images.map((imgUrl, idx) => {
            const isSelected = idx === activeIndex
            return (
              <button
                key={idx}
                type="button"
                onClick={() => onNavigate?.(idx)}
                className={`relative w-14 h-14 sm:w-16 sm:h-16 rounded-xl overflow-hidden border-2 transition-all cursor-pointer shrink-0 ${
                  isSelected
                    ? 'border-amber-400 ring-2 ring-amber-400/50 scale-105'
                    : 'border-white/20 opacity-50 hover:opacity-90 hover:border-white/50'
                }`}
                title={`Chuyển tới ảnh ${idx + 1}`}
              >
                <img
                  src={resolveImageUrl(imgUrl)}
                  alt={`Thumbnail ${idx + 1}`}
                  className="w-full h-full object-cover"
                />
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default ReviewImageLightbox
