import React from 'react'
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react'

/**
 * Component Phân trang chuẩn tái sử dụng (Pagination Component).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D10-DDM] Loại bỏ Mock Data Module Sản phẩm & Kiểm thử Phân trang Dữ liệu lớn
 *
 * @param {number} currentPage - Trang hiện tại (0-indexed: 0, 1, 2...)
 * @param {number} totalPages - Tổng số trang
 * @param {number} totalElements - Tổng số bản ghi
 * @param {number} pageSize - Kích thước mỗi trang
 * @param {Function} onPageChange - Callback khi đổi trang (trả về newPage 0-indexed)
 * @param {Function} [onPageSizeChange] - Callback khi đổi kích thước trang (12, 24, 48...)
 * @param {number[]} [pageSizeOptions=[12, 24, 48]] - Các tùy chọn kích thước trang
 * @param {boolean} [showPageSizeSelector=true] - Có hiển thị dropdown chọn pageSize không
 * @param {boolean} [showInfo=true] - Có hiển thị text thống kê bản ghi không
 * @param {boolean} [scrollToTop=true] - Tự động cuộn mượt lên đầu khi chuyển trang
 * @param {string} [theme='blue'] - Phong cách màu sắc: 'blue' (Khách hàng) hoặc 'zinc' (Admin)
 */
export const Pagination = ({
  currentPage = 0,
  totalPages = 0,
  totalElements = 0,
  pageSize = 12,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [12, 24, 48],
  showPageSizeSelector = true,
  showInfo = true,
  scrollToTop = true,
  theme = 'blue',
}) => {
  if (totalPages <= 0 && totalElements <= 0) return null

  // Chuyển trang kèm tự động cuộn mượt
  const handlePageClick = (pageIdx) => {
    if (pageIdx === currentPage || pageIdx < 0 || pageIdx >= totalPages) return
    if (onPageChange) {
      onPageChange(pageIdx)
    }
    if (scrollToTop && typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  // Thuật toán sinh danh sách số trang thông minh với dấu "..."
  const generatePageNumbers = () => {
    const pages = []
    const delta = 1 // Số trang liền kề 2 bên của currentPage
    const left = currentPage - delta
    const right = currentPage + delta

    let prev = 0

    for (let i = 0; i < totalPages; i++) {
      if (
        i === 0 || // Luôn hiện trang đầu
        i === totalPages - 1 || // Luôn hiện trang cuối
        (i >= left && i <= right) // Các trang xung quanh trang hiện tại
      ) {
        if (prev && i - prev > 1) {
          pages.push('...')
        }
        pages.push(i)
        prev = i
      }
    }

    return pages
  }

  // Kiểu màu theo theme
  const isZinc = theme === 'zinc'
  const activeBgClass = isZinc
    ? 'bg-zinc-900 text-white shadow-sm'
    : 'bg-blue-600 text-white shadow-md'
  const focusRingClass = isZinc
    ? 'focus:ring-zinc-950/20 focus:border-zinc-900'
    : 'focus:ring-blue-500/20 focus:border-blue-500'

  const startRecord = Math.min(currentPage * pageSize + 1, totalElements)
  const endRecord = Math.min((currentPage + 1) * pageSize, totalElements)

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-4 w-full">
      {/* Thông tin số lượng & Page Size Selector */}
      {showInfo && (
        <div className="flex items-center gap-3 text-xs text-slate-500">
          <div>
            Hiển thị{' '}
            <span className="font-bold text-slate-800 tabular-nums">
              {startRecord} - {endRecord}
            </span>{' '}
            trong{' '}
            <span className="font-bold text-slate-800 tabular-nums">
              {totalElements}
            </span>{' '}
            kết quả
          </div>

          {showPageSizeSelector && onPageSizeChange && (
            <div className="flex items-center gap-1.5 pl-3 border-l border-slate-200">
              <span className="text-slate-400">Hiển thị:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  const newSize = Number(e.target.value)
                  onPageSizeChange(newSize)
                  if (scrollToTop && typeof window !== 'undefined') {
                    window.scrollTo({ top: 0, behavior: 'smooth' })
                  }
                }}
                className={`h-8 px-2 py-0.5 text-xs font-semibold bg-white border border-slate-200 rounded-lg text-slate-700 cursor-pointer focus:outline-none focus:ring-2 ${focusRingClass}`}
                aria-label="Chọn số lượng sản phẩm mỗi trang"
              >
                {pageSizeOptions.map((sizeOpt) => (
                  <option key={sizeOpt} value={sizeOpt}>
                    {sizeOpt} / trang
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}

      {/* Điều khiển chuyển trang */}
      {totalPages > 1 && (
        <div className="flex items-center gap-1 sm:gap-1.5 select-none ml-auto">
          {/* Nút về trang đầu (First) */}
          <button
            type="button"
            onClick={() => handlePageClick(0)}
            disabled={currentPage === 0}
            className="p-1.5 sm:px-2.5 sm:py-1.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 disabled:opacity-40 disabled:cursor-not-allowed transition-colors text-xs font-semibold flex items-center gap-1 cursor-pointer"
            title="Trang đầu"
            aria-label="Đến trang đầu"
          >
            <ChevronsLeft className="w-4 h-4" />
            <span className="hidden md:inline">Đầu</span>
          </button>

          {/* Nút lùi 1 trang (Previous) */}
          <button
            type="button"
            onClick={() => handlePageClick(currentPage - 1)}
            disabled={currentPage === 0}
            className="p-1.5 sm:px-2.5 sm:py-1.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 disabled:opacity-40 disabled:cursor-not-allowed transition-colors text-xs font-semibold flex items-center gap-1 cursor-pointer"
            title="Trang trước"
            aria-label="Đến trang trước"
          >
            <ChevronLeft className="w-4 h-4" />
            <span className="hidden sm:inline">Trước</span>
          </button>

          {/* Danh sách các số trang */}
          <div className="flex items-center gap-1 mx-1">
            {generatePageNumbers().map((item, index) => {
              if (item === '...') {
                return (
                  <span
                    key={`ellipsis-${index}`}
                    className="w-8 h-8 flex items-center justify-center text-xs font-bold text-slate-400 select-none"
                  >
                    ...
                  </span>
                )
              }

              const pageIdx = item
              const isCurrent = pageIdx === currentPage

              return (
                <button
                  key={pageIdx}
                  type="button"
                  onClick={() => handlePageClick(pageIdx)}
                  className={`w-8 h-8 sm:w-9 sm:h-9 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-center tabular-nums ${
                    isCurrent
                      ? activeBgClass
                      : 'text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 hover:border-slate-300'
                  }`}
                  aria-current={isCurrent ? 'page' : undefined}
                  aria-label={`Trang ${pageIdx + 1}`}
                >
                  {pageIdx + 1}
                </button>
              )
            })}
          </div>

          {/* Nút tiến 1 trang (Next) */}
          <button
            type="button"
            onClick={() => handlePageClick(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="p-1.5 sm:px-2.5 sm:py-1.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 disabled:opacity-40 disabled:cursor-not-allowed transition-colors text-xs font-semibold flex items-center gap-1 cursor-pointer"
            title="Trang sau"
            aria-label="Đến trang sau"
          >
            <span className="hidden sm:inline">Sau</span>
            <ChevronRight className="w-4 h-4" />
          </button>

          {/* Nút đến trang cuối (Last) */}
          <button
            type="button"
            onClick={() => handlePageClick(totalPages - 1)}
            disabled={currentPage >= totalPages - 1}
            className="p-1.5 sm:px-2.5 sm:py-1.5 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 disabled:opacity-40 disabled:cursor-not-allowed transition-colors text-xs font-semibold flex items-center gap-1 cursor-pointer"
            title="Trang cuối"
            aria-label="Đến trang cuối"
          >
            <span className="hidden md:inline">Cuối</span>
            <ChevronsRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  )
}

export default Pagination

