import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  Star,
  CheckCircle2,
  AlertCircle,
  MessageSquare,
  MessageSquareHeart,
  Send,
  LogIn,
  ChevronLeft,
  ChevronRight,
  X,
  Sparkles,
} from 'lucide-react'
import reviewService from '../../services/reviewService'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../common/LoadingSpinner'

const RATING_LABELS = {
  1: 'Rất thất vọng',
  2: 'Chưa hài lòng',
  3: 'Bình thường',
  4: 'Hài lòng',
  5: 'Tuyệt vời',
}

/**
 * Component Hiển thị & Gửi Đánh Giá Sản Phẩm (ProductReviewsSection).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Phối hợp: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D12-DDM] Xây Dựng Giao Diện Đánh Giá Sản Phẩm & Thẻ Sản Phẩm Trong Chat
 */
export const ProductReviewsSection = ({ productId }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuthStore()

  // --- Dữ liệu Reviews & Thống kê ---
  const [summary, setSummary] = useState({
    ratingAverage: 0,
    totalReviews: 0,
    ratingCounts: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
  })
  const [reviews, setReviews] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loadingSummary, setLoadingSummary] = useState(false)
  const [loadingReviews, setLoadingReviews] = useState(false)

  // --- Bộ lọc nhanh theo số sao (null: Tất cả, 1-5: Lọc cụ thể) ---
  const [selectedRatingFilter, setSelectedRatingFilter] = useState(null)

  // --- State Form Đánh Giá ---
  const [userRating, setUserRating] = useState(5)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // --- Toast Notification ---
  const [toast, setToast] = useState(null) // { type: 'success' | 'error', message: string }

  // Tự động đóng toast sau 4 giây
  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [toast])

  // Lấy dữ liệu thống kê số sao
  const fetchSummary = useCallback(async () => {
    if (!productId) return
    setLoadingSummary(true)
    try {
      const res = await reviewService.getProductRatingSummary(productId)
      if (res?.success && res.data) {
        setSummary({
          ratingAverage: res.data.ratingAverage ?? 0,
          totalReviews: res.data.totalReviews ?? 0,
          ratingCounts: res.data.ratingCounts ?? { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
        })
      }
    } catch (err) {
      console.warn('Lỗi tải thống kê đánh giá:', err)
    } finally {
      setLoadingSummary(false)
    }
  }, [productId])

  // Lấy danh sách đánh giá có phân trang
  const fetchReviews = useCallback(async (targetPage = 0) => {
    if (!productId) return
    setLoadingReviews(true)
    try {
      const res = await reviewService.getProductReviews(productId, targetPage, 5)
      if (res?.success && res.data) {
        setReviews(res.data.content || [])
        setPage(res.data.pageNumber || 0)
        setTotalPages(res.data.totalPages || 0)
        setTotalElements(res.data.totalElements || 0)
      } else {
        setReviews([])
        setTotalPages(0)
        setTotalElements(0)
      }
    } catch (err) {
      console.warn('Lỗi tải danh sách đánh giá:', err)
      setReviews([])
    } finally {
      setLoadingReviews(false)
    }
  }, [productId])

  useEffect(() => {
    fetchSummary()
    fetchReviews(0)
  }, [fetchSummary, fetchReviews])

  // Lọc đánh giá theo mức sao đã chọn
  const filteredReviews = useMemo(() => {
    if (selectedRatingFilter === null) return reviews
    return reviews.filter((r) => r.rating === selectedRatingFilter)
  }, [reviews, selectedRatingFilter])

  // Xử lý gửi đánh giá
  const handleSubmitReview = async (e) => {
    e.preventDefault()

    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: location,
          message: 'Vui lòng đăng nhập để gửi đánh giá sản phẩm.',
        },
      })
      return
    }

    if (user?.role === 'ADMIN') {
      setToast({
        type: 'error',
        message: 'Tài khoản Quản trị viên (ADMIN) không được thực hiện đánh giá sản phẩm.',
      })
      return
    }

    const trimmedComment = comment.trim()
    if (!trimmedComment) {
      setToast({
        type: 'error',
        message: 'Vui lòng nhập nội dung đánh giá của bạn.',
      })
      return
    }

    if (userRating < 1 || userRating > 5) {
      setToast({
        type: 'error',
        message: 'Vui lòng chọn số sao đánh giá từ 1 đến 5 sao.',
      })
      return
    }

    setSubmitting(true)
    try {
      const res = await reviewService.createReview(productId, {
        rating: userRating,
        comment: trimmedComment,
      })

      if (res?.success) {
        setToast({
          type: 'success',
          message: 'Đánh giá sản phẩm thành công! Cảm ơn nhận xét quý giá của bạn.',
        })
        setComment('')
        setUserRating(5)
        setHoverRating(0)

        // Reload lại cả summary và danh sách đánh giá mới nhất
        fetchSummary()
        fetchReviews(0)
      } else {
        setToast({
          type: 'error',
          message: res?.message || 'Không thể gửi đánh giá. Vui lòng thử lại.',
        })
      }
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Không thể gửi đánh giá. Vui lòng thử lại sau.'
      setToast({
        type: 'error',
        message: errorMsg,
      })
    } finally {
      setSubmitting(false)
    }
  }

  // Format ngày tháng thân thiện (DD/MM/YYYY lúc HH:mm)
  const formatDateTime = (dateString) => {
    if (!dateString) return ''
    try {
      const d = new Date(dateString)
      const day = String(d.getDate()).padStart(2, '0')
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const year = d.getFullYear()
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      return `${day}/${month}/${year} lúc ${hours}:${minutes}`
    } catch {
      return dateString
    }
  }

  return (
    <section className="mt-12 pt-10 border-t border-slate-200 animate-in fade-in duration-300">
      {/* Toast Notification */}
      {toast && (
        <div
          className={`fixed top-20 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-2xl shadow-xl border text-sm font-semibold transition-all animate-in slide-in-from-top-2 ${
            toast.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {toast.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          )}
          <span>{toast.message}</span>
          <button
            type="button"
            onClick={() => setToast(null)}
            className="p-1 hover:bg-slate-200/50 rounded-lg text-slate-500 cursor-pointer ml-2"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Header Tiêu đề Section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2.5">
            <MessageSquare className="w-6 h-6 text-blue-600" />
            <span>Đánh giá từ khách hàng</span>
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Tổng hợp trải nghiệm và cảm nhận thực tế từ những người đã mua sản phẩm này
          </p>
        </div>
      </div>

      {/* KHU VỰC 1: TỔNG QUAN SỐ SAO & BẢNG PHÂN BỐ */}
      <div className="bg-slate-50/80 rounded-3xl p-6 sm:p-8 border border-slate-200/80 mb-10 shadow-xs">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Cột trái: Điểm trung bình to nổi bật */}
          <div className="lg:col-span-4 flex flex-col items-center justify-center text-center lg:border-r lg:border-slate-200 lg:pr-8">
            <div className="text-5xl sm:text-6xl font-black text-slate-900 tracking-tight tabular-nums">
              {summary.ratingAverage.toFixed(1)}
              <span className="text-xl sm:text-2xl font-semibold text-slate-400 ml-1">/ 5</span>
            </div>

            {/* Hàng sao vàng */}
            <div className="flex items-center gap-1.5 mt-3">
              {[1, 2, 3, 4, 5].map((star) => {
                const filled = star <= Math.round(summary.ratingAverage)
                return (
                  <Star
                    key={star}
                    className={`w-6 h-6 ${
                      filled
                        ? 'text-amber-400 fill-amber-400 drop-shadow-xs'
                        : 'text-slate-300'
                    }`}
                  />
                )
              })}
            </div>

            <p className="text-sm font-medium text-slate-600 mt-2">
              Dựa trên{' '}
              <span className="font-bold text-slate-900 tabular-nums">
                {summary.totalReviews}
              </span>{' '}
              lượt đánh giá
            </p>
          </div>

          {/* Cột phải: Bảng tiến trình phân bố số sao (5 sao -> 1 sao) */}
          <div className="lg:col-span-8 space-y-2.5">
            {[5, 4, 3, 2, 1].map((star) => {
              const count = summary.ratingCounts?.[star] || 0
              const percent =
                summary.totalReviews > 0
                  ? Math.round((count / summary.totalReviews) * 100)
                  : 0
              const isSelected = selectedRatingFilter === star

              return (
                <div
                  key={star}
                  onClick={() =>
                    setSelectedRatingFilter(isSelected ? null : star)
                  }
                  className={`flex items-center gap-3 text-xs sm:text-sm p-1.5 rounded-xl cursor-pointer transition-colors ${
                    isSelected
                      ? 'bg-amber-100/70 font-bold'
                      : 'hover:bg-slate-100 text-slate-700'
                  }`}
                  title={`Bấm để lọc các đánh giá ${star} sao`}
                >
                  <span className="w-12 font-semibold flex items-center gap-1 shrink-0">
                    <span>{star} sao</span>
                    <Star className="w-3.5 h-3.5 text-amber-400 fill-amber-400 inline" />
                  </span>

                  {/* Thanh Progress Bar */}
                  <div className="flex-1 h-3 bg-slate-200 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-amber-400 transition-all duration-500 rounded-full"
                      style={{ width: `${percent}%` }}
                    />
                  </div>

                  <span className="w-20 text-right text-xs text-slate-500 shrink-0 tabular-nums">
                    {count} ({percent}%)
                  </span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Các nút Filter nhanh */}
        <div className="mt-6 pt-6 border-t border-slate-200 flex flex-wrap items-center gap-2">
          <span className="text-xs font-bold text-slate-600 mr-1">Bộ lọc:</span>
          <button
            type="button"
            onClick={() => setSelectedRatingFilter(null)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
              selectedRatingFilter === null
                ? 'bg-blue-600 text-white shadow-xs'
                : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-100'
            }`}
          >
            Tất cả ({summary.totalReviews})
          </button>
          {[5, 4, 3, 2, 1].map((star) => {
            const count = summary.ratingCounts?.[star] || 0
            const isSelected = selectedRatingFilter === star
            return (
              <button
                key={star}
                type="button"
                onClick={() => setSelectedRatingFilter(isSelected ? null : star)}
                className={`px-3 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-amber-500 text-white shadow-xs'
                    : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-100'
                }`}
              >
                <span>{star} sao</span>
                <span className="opacity-80">({count})</span>
              </button>
            )
          })}
        </div>
      </div>

      {/* KHU VỰC 2: FORM GỬI ĐÁNH GIÁ (REVIEW FORM) */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/90 shadow-sm mb-12">
        <h3 className="text-lg font-bold text-slate-900 mb-2 flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-amber-500" />
          <span>Gửi nhận xét của bạn</span>
        </h3>
        <p className="text-xs sm:text-sm text-slate-500 mb-6">
          Đánh giá chỉ áp dụng cho khách hàng đã mua sản phẩm và nhận hàng thành công tại gian hàng Rikkei Commerce.
        </p>

        {!isAuthenticated ? (
          /* Trạng thái chưa đăng nhập: Nút mời đăng nhập */
          <div className="rounded-2xl border border-dashed border-blue-200 bg-blue-50/60 p-6 text-center">
            <p className="text-sm font-semibold text-blue-900 mb-3">
              Bạn đã mua và trải nghiệm sản phẩm này? Đăng nhập ngay để chia sẻ cảm nhận nhé!
            </p>
            <button
              type="button"
              onClick={() =>
                navigate('/login', {
                  state: {
                    from: location,
                    message: 'Vui lòng đăng nhập để gửi đánh giá sản phẩm.',
                  },
                })
              }
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-full shadow-md shadow-blue-500/20 transition-all active:scale-95 cursor-pointer"
            >
              <LogIn className="w-4 h-4" />
              <span>Đăng nhập để đánh giá</span>
            </button>
          </div>
        ) : (
          /* Form Đánh Giá dành cho người dùng đã đăng nhập */
          <form onSubmit={handleSubmitReview} className="space-y-6">
            {/* Chọn số sao tương tác */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Chất lượng sản phẩm
              </label>
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center gap-1.5 bg-slate-50 px-3 py-2 rounded-2xl border border-slate-200">
                  {[1, 2, 3, 4, 5].map((star) => {
                    const isHovered = hoverRating >= star
                    const isSelected = !hoverRating && userRating >= star

                    return (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setUserRating(star)}
                        onMouseEnter={() => setHoverRating(star)}
                        onMouseLeave={() => setHoverRating(0)}
                        className="p-1 cursor-pointer transition-transform hover:scale-125 focus:outline-hidden"
                        aria-label={`${star} sao`}
                      >
                        <Star
                          className={`w-7 h-7 transition-colors ${
                            isHovered || isSelected
                              ? 'text-amber-400 fill-amber-400 drop-shadow-xs'
                              : 'text-slate-300'
                          }`}
                        />
                      </button>
                    )
                  })}
                </div>

                <span className="text-sm font-bold text-amber-600">
                  {RATING_LABELS[hoverRating || userRating]}
                </span>
              </div>
            </div>

            {/* Textarea nhập nhận xét */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label
                  htmlFor="review-comment"
                  className="text-xs font-bold text-slate-700 uppercase tracking-wider"
                >
                  Nhận xét chi tiết
                </label>
                <span className="text-xs font-medium text-slate-400 tabular-nums">
                  {comment.length} / 1000 ký tự
                </span>
              </div>
              <textarea
                id="review-comment"
                rows={4}
                maxLength={1000}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Hãy chia sẻ cảm nhận chân thật của bạn về chất lượng sản phẩm, đóng gói, thời gian giao hàng..."
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm text-slate-800 placeholder-slate-400 focus:bg-white focus:outline-hidden focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all resize-none"
              />
            </div>

            {/* Nút gửi đánh giá */}
            <div className="flex justify-end">
              <button
                type="submit"
                disabled={submitting || !comment.trim()}
                className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-full shadow-md shadow-blue-500/20 disabled:opacity-50 disabled:pointer-events-none transition-all active:scale-98 cursor-pointer"
              >
                {submitting ? (
                  <>
                    <LoadingSpinner size="sm" className="text-white" />
                    <span>Đang gửi đánh giá...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>Gửi đánh giá</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* KHU VỰC 3: DANH SÁCH ĐÁNH GIÁ (REVIEW LIST) */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-slate-900">
            {selectedRatingFilter !== null
              ? `Đánh giá ${selectedRatingFilter} sao (${filteredReviews.length})`
              : `Tất cả đánh giá (${totalElements})`}
          </h3>
        </div>

        {loadingReviews ? (
          <div className="py-12 flex flex-col items-center justify-center text-slate-500">
            <LoadingSpinner size="lg" className="text-blue-600 mb-3" />
            <span className="text-xs">Đang tải đánh giá từ khách hàng...</span>
          </div>
        ) : filteredReviews.length === 0 ? (
          /* Empty State */
          <div className="py-16 px-4 bg-slate-50/60 rounded-3xl border border-dashed border-slate-200 text-center">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400 mb-3 shadow-inner">
              <MessageSquareHeart className="w-7 h-7 text-slate-400" />
            </div>
            <h4 className="text-sm font-bold text-slate-700 mb-1">
              Chưa có đánh giá nào cho mức này
            </h4>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Hãy là người đầu tiên trải nghiệm sản phẩm và để lại đánh giá hữu ích cho cộng đồng!
            </p>
          </div>
        ) : (
          /* Danh sách các Reviews */
          <div className="space-y-4">
            {filteredReviews.map((review) => {
              const initialLetter =
                review.userFullName?.trim()?.[0]?.toUpperCase() || 'K'

              return (
                <div
                  key={review.id}
                  className="bg-white rounded-2xl p-5 sm:p-6 border border-slate-200/90 shadow-2xs hover:shadow-xs transition-shadow"
                >
                  <div className="flex items-start justify-between gap-4">
                    {/* User info */}
                    <div className="flex items-center gap-3">
                      {review.userAvatar ? (
                        <img
                          src={review.userAvatar}
                          alt={review.userFullName}
                          className="w-10 h-10 rounded-full object-cover border border-slate-200"
                        />
                      ) : (
                        <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-bold text-sm flex items-center justify-center shadow-xs">
                          {initialLetter}
                        </div>
                      )}

                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-bold text-sm text-slate-900">
                            {review.userFullName || 'Khách hàng Rikkei'}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600 shrink-0" />
                            <span>Đã mua hàng</span>
                          </span>
                        </div>
                        <span className="text-xs text-slate-400 mt-0.5 block">
                          {formatDateTime(review.createdAt)}
                        </span>
                      </div>
                    </div>

                    {/* Số sao của review */}
                    <div className="flex items-center gap-1 bg-amber-50 px-2.5 py-1 rounded-xl border border-amber-200/60 shrink-0">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          className={`w-3.5 h-3.5 ${
                            s <= review.rating
                              ? 'text-amber-400 fill-amber-400'
                              : 'text-slate-300'
                          }`}
                        />
                      ))}
                    </div>
                  </div>

                  {/* Nội dung Comment */}
                  <div className="mt-3.5 pl-13 text-sm text-slate-700 whitespace-pre-line leading-relaxed">
                    {review.comment}
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* Phân trang (Pagination) */}
        {totalPages > 1 && selectedRatingFilter === null && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <button
              type="button"
              disabled={page <= 0 || loadingReviews}
              onClick={() => fetchReviews(page - 1)}
              className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
              aria-label="Trang trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>

            {Array.from({ length: totalPages }).map((_, idx) => (
              <button
                key={idx}
                type="button"
                disabled={loadingReviews}
                onClick={() => fetchReviews(idx)}
                className={`w-9 h-9 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  page === idx
                    ? 'bg-blue-600 text-white shadow-xs'
                    : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'
                }`}
              >
                {idx + 1}
              </button>
            ))}

            <button
              type="button"
              disabled={page >= totalPages - 1 || loadingReviews}
              onClick={() => fetchReviews(page + 1)}
              className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer"
              aria-label="Trang sau"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </section>
  )
}

export default ProductReviewsSection

