import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Star,
  Eye,
  EyeOff,
  Trash2,
  RefreshCw,
  Search,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  X,
  MessageSquare,
  ZoomIn,
  Package,
  Image as ImageIcon,
} from 'lucide-react'
import reviewService from '../../services/reviewService'
import LoadingSpinner from '../../components/common/LoadingSpinner'

/**
 * Trang Quản trị Đánh giá & Nhận xét sản phẩm (AdminReviewsPage).
 * Tuyến đường (Route): /admin/reviews
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 *
 * Tính năng chính:
 * 1. Hiển thị danh sách tất cả đánh giá sản phẩm có phân trang từ API GET /api/v1/admin/reviews.
 * 2. Lọc nhanh theo trạng thái hiển thị: Tất cả, Đang hiển thị (isVisible = true), Đã ẩn (isVisible = false).
 * 3. Tìm kiếm theo tên khách hàng, mã hoặc tên sản phẩm, hoặc nội dung nhận xét.
 * 4. Chuyển đổi trạng thái hiển thị công khai (Ẩn / Hiện) qua PUT /api/v1/admin/reviews/{id}/toggle-status.
 * 5. Xóa vĩnh viễn đánh giá và dọn dẹp các tệp ảnh khỏi storage qua DELETE /api/v1/admin/reviews/{id} kèm Modal xác nhận an toàn.
 * 6. Xem ảnh thực tế đính kèm của đánh giá qua Lightbox modal phóng to ảnh.
 */
export const AdminReviewsPage = () => {
  // --- Danh sách đánh giá & Phân trang ---
  const [reviews, setReviews] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  // --- Bộ lọc & Tìm kiếm ---
  const [visibilityFilter, setVisibilityFilter] = useState('ALL') // 'ALL' | 'VISIBLE' | 'HIDDEN'
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedRating, setSelectedRating] = useState('ALL') // 'ALL' | '1' | '2' | '3' | '4' | '5'

  // --- Trạng thái giao diện ---
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // --- Thao tác trên từng dòng ---
  const [togglingId, setTogglingId] = useState(null)
  const [deletingReview, setDeletingReview] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // --- Lightbox xem ảnh lớn ---
  const [activeLightboxImage, setActiveLightboxImage] = useState(null)

  // Tự động đóng toast sau 4 giây
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [toastMessage])

  // Lấy dữ liệu từ Backend API
  const fetchReviews = useCallback(async (targetPage = 0) => {
    setIsLoading(true)
    try {
      let isVisibleParam = null
      if (visibilityFilter === 'VISIBLE') isVisibleParam = true
      if (visibilityFilter === 'HIDDEN') isVisibleParam = false

      const res = await reviewService.getAdminReviews(targetPage, pageSize, isVisibleParam)
      if (res?.success && res.data) {
        setReviews(res.data.content || [])
        setPage(res.data.pageNumber || 0)
        setTotalPages(res.data.totalPages || 1)
        setTotalElements(res.data.totalElements || 0)
      } else {
        setReviews([])
        setTotalPages(1)
        setTotalElements(0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách đánh giá:', err)
      setToastMessage({
        type: 'error',
        text: err.response?.data?.message || 'Không thể tải danh sách đánh giá từ máy chủ.',
      })
      setReviews([])
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }, [pageSize, visibilityFilter])

  useEffect(() => {
    fetchReviews(0)
  }, [fetchReviews])

  const handleRefresh = () => {
    setIsRefreshing(true)
    fetchReviews(page)
  }

  // Chuyển đổi trạng thái Ẩn / Hiện Review
  const handleToggleStatus = async (review) => {
    setTogglingId(review.id)
    try {
      const res = await reviewService.toggleReviewVisibility(review.id)
      if (res?.success && res.data) {
        const updatedIsVisible = res.data.isVisible
        setReviews((prev) =>
          prev.map((r) => (r.id === review.id ? { ...r, isVisible: updatedIsVisible } : r))
        )
        setToastMessage({
          type: 'success',
          text: updatedIsVisible
            ? `Đã hiển thị công khai đánh giá #${review.id}`
            : `Đã ẩn đánh giá #${review.id} khỏi trang sản phẩm`,
        })
      }
    } catch (err) {
      console.error('Lỗi khi thay đổi trạng thái đánh giá:', err)
      setToastMessage({
        type: 'error',
        text: err.response?.data?.message || 'Không thể thay đổi trạng thái đánh giá.',
      })
    } finally {
      setTogglingId(null)
    }
  }

  // Xóa vĩnh viễn Review
  const handleConfirmDelete = async () => {
    if (!deletingReview) return
    setIsDeleting(true)
    try {
      const res = await reviewService.deleteReviewByAdmin(deletingReview.id)
      if (res?.success) {
        setToastMessage({
          type: 'success',
          text: `Đã xóa vĩnh viễn đánh giá #${deletingReview.id} và dọn sạch các tệp ảnh liên quan.`,
        })
        setDeletingReview(null)
        // Refresh lại trang hiện tại hoặc trang trước nếu là phần tử cuối
        if (reviews.length === 1 && page > 0) {
          fetchReviews(page - 1)
        } else {
          fetchReviews(page)
        }
      }
    } catch (err) {
      console.error('Lỗi khi xóa đánh giá:', err)
      setToastMessage({
        type: 'error',
        text: err.response?.data?.message || 'Không thể xóa đánh giá.',
      })
    } finally {
      setIsDeleting(false)
    }
  }

  // Client-side filter theo từ khóa và số sao trên trang dữ liệu
  const filteredReviews = useMemo(() => {
    return reviews.filter((r) => {
      // Lọc theo rating
      if (selectedRating !== 'ALL' && r.rating !== parseInt(selectedRating, 10)) {
        return false
      }

      // Lọc theo search term
      if (searchTerm.trim()) {
        const query = searchTerm.toLowerCase().trim()
        const userMatch =
          r.userName?.toLowerCase().includes(query) ||
          r.userFullName?.toLowerCase().includes(query) ||
          String(r.userId).includes(query)
        const prodMatch =
          r.productName?.toLowerCase().includes(query) ||
          String(r.productId).includes(query)
        const commentMatch = r.comment?.toLowerCase().includes(query)
        const idMatch = String(r.id).includes(query)

        return userMatch || prodMatch || commentMatch || idMatch
      }

      return true
    })
  }, [reviews, selectedRating, searchTerm])

  // Thống kê nhanh trên trang
  const stats = useMemo(() => {
    const visibleCount = reviews.filter((r) => r.isVisible).length
    const hiddenCount = reviews.filter((r) => !r.isVisible).length
    const withImagesCount = reviews.filter((r) => r.images && r.images.length > 0).length
    return { visibleCount, hiddenCount, withImagesCount }
  }, [reviews])

  const formatDate = (isoString) => {
    if (!isoString) return '—'
    const date = new Date(isoString)
    return date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 p-6 lg:p-8 space-y-6">
      {/* 1. Header & Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-zinc-800/80">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-zinc-100">
              Quản lý đánh giá sản phẩm
            </h1>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
              Admin Moderation
            </span>
          </div>
          <p className="text-xs sm:text-sm text-zinc-400 mt-1">
            Duyệt, ẩn/hiện và xóa đánh giá vi phạm, kiểm soát hình ảnh đính kèm và điểm sao sản phẩm.
          </p>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          disabled={isLoading || isRefreshing}
          className="inline-flex items-center gap-2 px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-zinc-200 text-xs font-semibold rounded-xl border border-zinc-800 transition-colors cursor-pointer self-start sm:self-auto disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Toast Notification */}
      {toastMessage && (
        <div
          className={`p-4 rounded-2xl flex items-center gap-3 border text-sm font-medium transition-all ${
            toastMessage.type === 'success'
              ? 'bg-emerald-950/60 border-emerald-800 text-emerald-200'
              : 'bg-red-950/60 border-red-800 text-red-200'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-400 shrink-0" />
          )}
          <span className="flex-1">{toastMessage.text}</span>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="p-1 hover:bg-white/10 rounded-lg cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 2. Thống kê tổng quan */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800/80">
          <span className="text-xs text-zinc-400">Tổng đánh giá trên hệ thống</span>
          <div className="text-2xl font-bold text-zinc-100 mt-1 tabular-nums">
            {totalElements}
          </div>
        </div>
        <div className="p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800/80">
          <span className="text-xs text-emerald-400">Đang hiển thị (Trang này)</span>
          <div className="text-2xl font-bold text-emerald-400 mt-1 tabular-nums">
            {stats.visibleCount}
          </div>
        </div>
        <div className="p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800/80">
          <span className="text-xs text-amber-400">Đang bị ẩn (Trang này)</span>
          <div className="text-2xl font-bold text-amber-400 mt-1 tabular-nums">
            {stats.hiddenCount}
          </div>
        </div>
        <div className="p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800/80">
          <span className="text-xs text-blue-400">Có ảnh đính kèm (Trang này)</span>
          <div className="text-2xl font-bold text-blue-400 mt-1 tabular-nums">
            {stats.withImagesCount}
          </div>
        </div>
      </div>

      {/* 3. Bộ lọc & Tìm kiếm */}
      <div className="p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800/80 space-y-3">
        <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
          {/* Ô tìm kiếm */}
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo mã ID, tên khách hàng, sản phẩm, bình luận..."
              className="w-full pl-10 pr-4 py-2 bg-zinc-950 border border-zinc-800 rounded-xl text-xs text-zinc-200 placeholder-zinc-500 focus:outline-hidden focus:border-blue-500"
            />
          </div>

          {/* Lọc trạng thái hiển thị */}
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-zinc-500 font-medium">Trạng thái:</span>
            <button
              type="button"
              onClick={() => setVisibilityFilter('ALL')}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                visibilityFilter === 'ALL'
                  ? 'bg-blue-600 text-white'
                  : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => setVisibilityFilter('VISIBLE')}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                visibilityFilter === 'VISIBLE'
                  ? 'bg-emerald-600 text-white'
                  : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              Đang hiển thị
            </button>
            <button
              type="button"
              onClick={() => setVisibilityFilter('HIDDEN')}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-colors cursor-pointer ${
                visibilityFilter === 'HIDDEN'
                  ? 'bg-amber-600 text-white'
                  : 'bg-zinc-800/60 text-zinc-400 hover:text-zinc-200'
              }`}
            >
              Đã bị ẩn
            </button>
          </div>

          {/* Lọc số sao */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-zinc-500 font-medium">Số sao:</span>
            <select
              value={selectedRating}
              onChange={(e) => setSelectedRating(e.target.value)}
              className="px-3 py-1.5 bg-zinc-950 border border-zinc-800 rounded-xl text-xs text-zinc-300 focus:outline-hidden focus:border-blue-500 cursor-pointer"
            >
              <option value="ALL">Tất cả sao</option>
              <option value="5">5 sao</option>
              <option value="4">4 sao</option>
              <option value="3">3 sao</option>
              <option value="2">2 sao</option>
              <option value="1">1 sao</option>
            </select>
          </div>
        </div>
      </div>

      {/* 4. Bảng danh sách đánh giá */}
      <div className="rounded-2xl border border-zinc-800/80 bg-zinc-900/50 overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-zinc-400">
            <LoadingSpinner size="lg" className="text-blue-500 mb-3" />
            <p className="text-sm font-medium">Đang tải danh sách đánh giá...</p>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="py-16 text-center text-zinc-500 space-y-2">
            <MessageSquare className="w-10 h-10 mx-auto text-zinc-600 opacity-60" />
            <p className="text-sm font-medium">Không tìm thấy đánh giá nào phù hợp với bộ lọc.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-900/90 text-zinc-400 uppercase tracking-wider font-semibold">
                  <th className="py-3.5 px-4 w-16">ID</th>
                  <th className="py-3.5 px-4 min-w-[200px]">Sản phẩm</th>
                  <th className="py-3.5 px-4 min-w-[150px]">Khách hàng</th>
                  <th className="py-3.5 px-4 min-w-[280px]">Đánh giá &amp; Nội dung</th>
                  <th className="py-3.5 px-4 w-32 text-center">Trạng thái</th>
                  <th className="py-3.5 px-4 w-36 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60">
                {filteredReviews.map((r) => {
                  const isToggling = togglingId === r.id

                  return (
                    <tr
                      key={r.id}
                      className={`hover:bg-zinc-800/30 transition-colors ${
                        !r.isVisible ? 'bg-amber-950/10' : ''
                      }`}
                    >
                      {/* ID & Ngày */}
                      <td className="py-4 px-4 align-top">
                        <span className="font-mono font-bold text-zinc-300">#{r.id}</span>
                        <div className="text-[10px] text-zinc-500 mt-1 whitespace-nowrap">
                          {formatDate(r.createdAt)}
                        </div>
                      </td>

                      {/* Thông tin sản phẩm */}
                      <td className="py-4 px-4 align-top">
                        <div className="flex items-start gap-2.5">
                          {r.productImage ? (
                            <img
                              src={r.productImage}
                              alt={r.productName || 'Product'}
                              className="w-10 h-10 rounded-lg object-cover bg-zinc-800 shrink-0 border border-zinc-800"
                              onError={(e) => {
                                e.currentTarget.style.display = 'none'
                              }}
                            />
                          ) : (
                            <div className="w-10 h-10 rounded-lg bg-zinc-800 border border-zinc-700 flex items-center justify-center shrink-0 text-zinc-500">
                              <Package className="w-4 h-4" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <span className="font-semibold text-zinc-200 block truncate leading-tight">
                              {r.productName || `Sản phẩm #${r.productId}`}
                            </span>
                            <span className="text-[10px] text-zinc-500">ID: {r.productId}</span>
                          </div>
                        </div>
                      </td>

                      {/* Khách hàng */}
                      <td className="py-4 px-4 align-top">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-blue-900/40 border border-blue-700/50 flex items-center justify-center text-blue-300 font-bold text-xs shrink-0">
                            {(r.userFullName || r.userName || 'U').charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <span className="font-medium text-zinc-200 block truncate">
                              {r.userFullName || r.userName || 'Khách hàng ẩn danh'}
                            </span>
                            <span className="text-[10px] text-zinc-500">User #{r.userId}</span>
                          </div>
                        </div>
                      </td>

                      {/* Đánh giá & Nội dung nhận xét */}
                      <td className="py-4 px-4 align-top space-y-2">
                        {/* Số sao */}
                        <div className="flex items-center gap-1">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <Star
                              key={star}
                              className={`w-3.5 h-3.5 ${
                                star <= r.rating
                                  ? 'text-amber-400 fill-amber-400'
                                  : 'text-zinc-700'
                              }`}
                            />
                          ))}
                          <span className="text-[11px] font-bold text-amber-400 ml-1">
                            {r.rating}.0
                          </span>
                        </div>

                        {/* Comment text */}
                        <p className="text-zinc-300 whitespace-pre-line leading-relaxed line-clamp-3">
                          {r.comment || <span className="italic text-zinc-500">Không có nhận xét</span>}
                        </p>

                        {/* Thumbnail ảnh đính kèm (nếu có) */}
                        {r.images && r.images.length > 0 && (
                          <div className="flex items-center gap-2 pt-1">
                            <span className="text-[10px] text-zinc-500 flex items-center gap-1">
                              <ImageIcon className="w-3 h-3 text-blue-400" />
                              <span>{r.images.length} ảnh:</span>
                            </span>
                            {r.images.map((img) => (
                              <button
                                key={img.id}
                                type="button"
                                onClick={() => setActiveLightboxImage(img.imageUrl)}
                                className="group relative w-9 h-9 rounded-lg overflow-hidden border border-zinc-700 hover:border-blue-400 cursor-pointer focus:outline-hidden"
                                title="Bấm để xem ảnh lớn"
                              >
                                <img
                                  src={img.imageUrl}
                                  alt="Ảnh đánh giá"
                                  className="w-full h-full object-cover group-hover:scale-110 transition-transform"
                                  onError={(e) => {
                                    e.currentTarget.style.display = 'none'
                                  }}
                                />
                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                  <ZoomIn className="w-3 h-3 text-white" />
                                </div>
                              </button>
                            ))}
                          </div>
                        )}
                      </td>

                      {/* Trạng thái hiển thị */}
                      <td className="py-4 px-4 align-top text-center">
                        {r.isVisible ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-950 text-emerald-400 border border-emerald-800">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                            Hiển thị
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-950/80 text-amber-400 border border-amber-800">
                            <EyeOff className="w-3 h-3" />
                            Đã bị ẩn
                          </span>
                        )}
                      </td>

                      {/* Thao tác (Ẩn/Hiện, Xóa vĩnh viễn) */}
                      <td className="py-4 px-4 align-top text-center">
                        <div className="inline-flex items-center gap-1.5">
                          {/* Nút Toggle Ẩn / Hiện */}
                          <button
                            type="button"
                            onClick={() => handleToggleStatus(r)}
                            disabled={isToggling}
                            className={`p-2 rounded-xl border transition-colors cursor-pointer ${
                              r.isVisible
                                ? 'bg-zinc-800/80 hover:bg-amber-900/40 text-amber-400 border-zinc-700 hover:border-amber-700'
                                : 'bg-emerald-900/40 hover:bg-emerald-900/60 text-emerald-300 border-emerald-700'
                            } disabled:opacity-50`}
                            title={r.isVisible ? 'Ẩn đánh giá khỏi sản phẩm' : 'Hiển thị lại đánh giá'}
                          >
                            {isToggling ? (
                              <LoadingSpinner size="sm" className="text-zinc-200" />
                            ) : r.isVisible ? (
                              <EyeOff className="w-4 h-4" />
                            ) : (
                              <Eye className="w-4 h-4" />
                            )}
                          </button>

                          {/* Nút Xóa vĩnh viễn */}
                          <button
                            type="button"
                            onClick={() => setDeletingReview(r)}
                            className="p-2 rounded-xl bg-zinc-800/80 hover:bg-red-950/60 text-red-400 border border-zinc-700 hover:border-red-700 transition-colors cursor-pointer"
                            title="Xóa vĩnh viễn đánh giá và dọn file ảnh"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* 5. Phân trang */}
        {!isLoading && totalPages > 1 && (
          <div className="p-4 border-t border-zinc-800 flex items-center justify-between text-xs text-zinc-400">
            <span>
              Trang {page + 1} / {totalPages} (Tổng cộng {totalElements} đánh giá)
            </span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => fetchReviews(page - 1)}
                className="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 hover:bg-zinc-700 disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
              >
                Trang trước
              </button>
              {Array.from({ length: totalPages }).map((_, idx) => {
                // Hiển thị tối đa 5 nút trang
                if (Math.abs(page - idx) <= 2 || idx === 0 || idx === totalPages - 1) {
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => fetchReviews(idx)}
                      className={`w-8 h-8 rounded-lg font-semibold transition-colors cursor-pointer ${
                        page === idx
                          ? 'bg-blue-600 text-white'
                          : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
                      }`}
                    >
                      {idx + 1}
                    </button>
                  )
                }
                return null
              })}
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => fetchReviews(page + 1)}
                className="px-3 py-1.5 rounded-lg bg-zinc-800 text-zinc-200 hover:bg-zinc-700 disabled:opacity-30 disabled:pointer-events-none cursor-pointer"
              >
                Trang sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 6. Modal Xác nhận Xóa vĩnh viễn Review */}
      {deletingReview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-red-950/80 border border-red-800 flex items-center justify-center mx-auto text-red-400">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <div className="text-center space-y-2">
              <h3 className="text-lg font-bold text-zinc-100">
                Xác nhận xóa vĩnh viễn đánh giá #{deletingReview.id}
              </h3>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Hành động này sẽ xóa vĩnh viễn bản ghi đánh giá khỏi cơ sở dữ liệu, dọn sạch toàn bộ{' '}
                <strong className="text-zinc-200">
                  {deletingReview.images?.length || 0} tệp ảnh đính kèm
                </strong>{' '}
                trên máy chủ lưu trữ và tự động tính toán lại điểm rating trung bình của sản phẩm.
              </p>
            </div>

            <div className="p-3 bg-zinc-950 rounded-xl border border-zinc-800 text-xs space-y-1">
              <div>
                <span className="text-zinc-500">Khách hàng: </span>
                <span className="text-zinc-300 font-semibold">
                  {deletingReview.userFullName || deletingReview.userName}
                </span>
              </div>
              <div>
                <span className="text-zinc-500">Nội dung: </span>
                <span className="text-zinc-300 italic line-clamp-2">
                  &ldquo;{deletingReview.comment}&rdquo;
                </span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeletingReview(null)}
                disabled={isDeleting}
                className="px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-semibold rounded-xl cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-2 px-5 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-md shadow-red-900/30 cursor-pointer disabled:opacity-50"
              >
                {isDeleting && <LoadingSpinner size="sm" className="text-white" />}
                <span>{isDeleting ? 'Đang xóa...' : 'Xác nhận xóa vĩnh viễn'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 7. Modal Lightbox Xem Ảnh Lớn */}
      {activeLightboxImage && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-xs animate-in fade-in duration-200"
          onClick={() => setActiveLightboxImage(null)}
        >
          <div
            className="relative max-w-3xl max-h-[85vh] bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-2xl p-2"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setActiveLightboxImage(null)}
              className="absolute top-4 right-4 z-10 p-2 rounded-full bg-zinc-950/80 hover:bg-zinc-950 text-white transition-colors cursor-pointer"
              title="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={activeLightboxImage}
              alt="Hình ảnh đánh giá"
              className="max-h-[80vh] w-auto mx-auto object-contain rounded-2xl"
            />
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminReviewsPage
