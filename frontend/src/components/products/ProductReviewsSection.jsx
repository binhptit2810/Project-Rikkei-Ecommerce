import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react'
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
  Edit2,
  Trash2,
  AlertTriangle,
  Check,
  Camera,
  ZoomIn,
  Store,
  ShieldCheck,
  UploadCloud,
} from 'lucide-react'
import reviewService from '../../services/reviewService'
import { useAuthStore } from '../../store/useAuthStore'
import LoadingSpinner from '../common/LoadingSpinner'
import ReviewImageLightbox, { resolveImageUrl } from '../reviews/ReviewImageLightbox'

const RATING_LABELS = {
  1: 'Rất thất vọng',
  2: 'Chưa hài lòng',
  3: 'Bình thường',
  4: 'Hài lòng',
  5: 'Tuyệt vời',
}

const MAX_IMAGES_COUNT = 3
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5MB

/**
 * Component Hiển thị & Gửi Đánh Giá Sản Phẩm (ProductReviewsSection).
 * Phụ trách: Đỗ Đức Mạnh (DDM) & Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 *
 * Đáp ứng đầy đủ Checklist:
 * [1] Thêm nút chọn ảnh và khung xem trước (preview thumbnail) trong form gửi review (tối đa 3 ảnh).
 * [2] Tích hợp lightbox phóng to khi click vào ảnh đánh giá của khách.
 * [3] Thiết kế khung "Phản hồi từ Người bán" bo góc màu xám nhạt dưới review.
 */
export const ProductReviewsSection = ({ productId }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuthStore()

  const fileInputRef = useRef(null)
  const editFileInputRef = useRef(null)

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

  // --- State Form Đánh Giá Mới ---
  const [userRating, setUserRating] = useState(5)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')
  const [selectedImages, setSelectedImages] = useState([]) // [{ file, previewUrl, id }]
  const [submitting, setSubmitting] = useState(false)

  // --- Toast Notification ---
  const [toast, setToast] = useState(null) // { type: 'success' | 'error', message: string }

  // --- State Chỉnh sửa Đánh giá (Inline Edit) ---
  const [editingReviewId, setEditingReviewId] = useState(null)
  const [editRating, setEditRating] = useState(5)
  const [editHoverRating, setEditHoverRating] = useState(0)
  const [editComment, setEditComment] = useState('')
  const [editImages, setEditImages] = useState([]) // [{ file?, previewUrl, id, isExisting?: boolean }]
  const [editSubmitting, setEditSubmitting] = useState(false)

  // --- State Modal Xác nhận Xóa ---
  const [deletingReview, setDeletingReview] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // --- State Lightbox phóng to ảnh ---
  const [lightboxState, setLightboxState] = useState({
    isOpen: false,
    images: [],
    currentIndex: 0,
  })

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

  // =========================================================================
  // XỬ LÝ CHỌN VÀ VALIDATE ẢNH (FORM GỬI MỚI & SỬA)
  // =========================================================================

  const handleSelectFiles = (e, isEdit = false) => {
    const rawFiles = Array.from(e.target.files || [])
    if (rawFiles.length === 0) return

    const currentList = isEdit ? editImages : selectedImages
    const remainingSlots = MAX_IMAGES_COUNT - currentList.length

    if (remainingSlots <= 0) {
      setToast({
        type: 'error',
        message: `Bạn chỉ có thể đính kèm tối đa ${MAX_IMAGES_COUNT} ảnh cho mỗi đánh giá.`,
      })
      if (e.target) e.target.value = ''
      return
    }

    let filesToAdd = rawFiles
    if (rawFiles.length > remainingSlots) {
      filesToAdd = rawFiles.slice(0, remainingSlots)
      setToast({
        type: 'error',
        message: `Đã chọn vượt quá số lượng. Hệ thống chỉ lấy ${remainingSlots} ảnh đầu tiên để không quá ${MAX_IMAGES_COUNT} ảnh.`,
      })
    }

    const validNewImages = []
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

    for (const file of filesToAdd) {
      if (!allowedTypes.includes(file.type)) {
        setToast({
          type: 'error',
          message: `Tệp "${file.name}" không đúng định dạng. Chỉ hỗ trợ ảnh JPG, PNG, WEBP, GIF.`,
        })
        continue
      }

      if (file.size > MAX_IMAGE_SIZE_BYTES) {
        setToast({
          type: 'error',
          message: `Ảnh "${file.name}" vượt quá dung lượng tối đa 5MB. Vui lòng chọn ảnh nhỏ hơn.`,
        })
        continue
      }

      const previewUrl = URL.createObjectURL(file)
      validNewImages.push({
        id: `${Date.now()}-${Math.random()}`,
        file,
        previewUrl,
        isExisting: false,
      })
    }

    if (validNewImages.length > 0) {
      if (isEdit) {
        setEditImages((prev) => [...prev, ...validNewImages].slice(0, MAX_IMAGES_COUNT))
      } else {
        setSelectedImages((prev) => [...prev, ...validNewImages].slice(0, MAX_IMAGES_COUNT))
      }
    }

    if (e.target) e.target.value = ''
  }

  const handleRemoveImage = (indexToRemove, isEdit = false) => {
    if (isEdit) {
      setEditImages((prev) => {
        const target = prev[indexToRemove]
        if (target && !target.isExisting && target.previewUrl?.startsWith('blob:')) {
          URL.revokeObjectURL(target.previewUrl)
        }
        return prev.filter((_, idx) => idx !== indexToRemove)
      })
    } else {
      setSelectedImages((prev) => {
        const target = prev[indexToRemove]
        if (target && target.previewUrl?.startsWith('blob:')) {
          URL.revokeObjectURL(target.previewUrl)
        }
        return prev.filter((_, idx) => idx !== indexToRemove)
      })
    }
  }

  // =========================================================================
  // XỬ LÝ LIGHTBOX PHÓNG TO ẢNH
  // =========================================================================

  const openLightbox = (imagesList, startIdx = 0) => {
    if (!imagesList || imagesList.length === 0) return
    setLightboxState({
      isOpen: true,
      images: imagesList,
      currentIndex: startIdx,
    })
  }

  const closeLightbox = () => {
    setLightboxState((prev) => ({ ...prev, isOpen: false }))
  }

  const handleLightboxNavigate = (newIdx) => {
    setLightboxState((prev) => ({ ...prev, currentIndex: newIdx }))
  }

  // =========================================================================
  // GỬI ĐÁNH GIÁ MỚI
  // =========================================================================

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
      // 1. Tải ảnh lên máy chủ nếu có
      let uploadedUrls = []
      if (selectedImages.length > 0) {
        const filesToUpload = selectedImages.map((img) => img.file).filter(Boolean)
        if (filesToUpload.length > 0) {
          uploadedUrls = await reviewService.uploadReviewImages(filesToUpload)
        }
      }

      // 2. Gửi request tạo review
      const res = await reviewService.createReview(productId, {
        rating: userRating,
        comment: trimmedComment,
        images: uploadedUrls,
      })

      if (res?.success) {
        setToast({
          type: 'success',
          message: 'Đánh giá sản phẩm thành công! Cảm ơn nhận xét quý giá của bạn.',
        })
        setComment('')
        setUserRating(5)
        setHoverRating(0)

        // Dọn dẹp object URLs preview
        selectedImages.forEach((img) => {
          if (img.previewUrl?.startsWith('blob:')) {
            URL.revokeObjectURL(img.previewUrl)
          }
        })
        setSelectedImages([])

        // Tải lại summary và danh sách đánh giá mới nhất
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

  // =========================================================================
  // XỬ LÝ CHỈNH SỬA ĐÁNH GIÁ (INLINE EDIT)
  // =========================================================================

  const handleStartEdit = (review) => {
    setEditingReviewId(review.id)
    setEditRating(review.rating || 5)
    setEditHoverRating(0)
    setEditComment(review.comment || '')

    const rawImages = review.images || []
    const existingImgs = rawImages.map((img, idx) => {
      const url = typeof img === 'object' && img !== null ? (img.imageUrl || img.url || '') : String(img)
      return {
        id: `existing-${idx}-${url}`,
        previewUrl: url,
        isExisting: true,
      }
    })
    setEditImages(existingImgs)
  }

  const handleCancelEdit = () => {
    editImages.forEach((img) => {
      if (!img.isExisting && img.previewUrl?.startsWith('blob:')) {
        URL.revokeObjectURL(img.previewUrl)
      }
    })
    setEditingReviewId(null)
    setEditRating(5)
    setEditHoverRating(0)
    setEditComment('')
    setEditImages([])
  }

  const handleSaveEdit = async (reviewId) => {
    const trimmed = editComment.trim()
    if (!trimmed) {
      setToast({
        type: 'error',
        message: 'Nội dung đánh giá không được để trống.',
      })
      return
    }

    if (editRating < 1 || editRating > 5) {
      setToast({
        type: 'error',
        message: 'Vui lòng chọn số sao đánh giá từ 1 đến 5 sao.',
      })
      return
    }

    setEditSubmitting(true)
    try {
      // 1. Tải ảnh mới lên nếu có
      const existingUrls = editImages.filter((img) => img.isExisting).map((img) => img.previewUrl)
      const newFiles = editImages.filter((img) => !img.isExisting && img.file).map((img) => img.file)

      let newlyUploadedUrls = []
      if (newFiles.length > 0) {
        newlyUploadedUrls = await reviewService.uploadReviewImages(newFiles)
      }
      const finalImages = [...existingUrls, ...newlyUploadedUrls].slice(0, MAX_IMAGES_COUNT)

      // 2. Gửi request cập nhật review
      const res = await reviewService.updateReview(productId, reviewId, {
        rating: editRating,
        comment: trimmed,
        images: finalImages,
      })

      if (res?.success) {
        setToast({
          type: 'success',
          message: 'Cập nhật đánh giá thành công!',
        })
        handleCancelEdit()
        fetchSummary()
        fetchReviews(page)
      } else {
        setToast({
          type: 'error',
          message: res?.message || 'Không thể cập nhật đánh giá.',
        })
      }
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi cập nhật đánh giá.'
      setToast({
        type: 'error',
        message: errorMsg,
      })
    } finally {
      setEditSubmitting(false)
    }
  }

  // =========================================================================
  // XỬ LÝ XÓA ĐÁNH GIÁ
  // =========================================================================

  const handleConfirmDelete = async () => {
    if (!deletingReview) return

    setIsDeleting(true)
    try {
      const res = await reviewService.deleteReview(productId, deletingReview.id)
      if (res?.success !== false) {
        setToast({
          type: 'success',
          message: 'Đã xóa đánh giá thành công!',
        })
        setDeletingReview(null)
        fetchSummary()
        fetchReviews(0)
      } else {
        setToast({
          type: 'error',
          message: res?.message || 'Không thể xóa đánh giá.',
        })
      }
    } catch (err) {
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'Có lỗi xảy ra khi xóa đánh giá.'
      setToast({
        type: 'error',
        message: errorMsg,
      })
    } finally {
      setIsDeleting(false)
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
      <div className="bg-slate-50/80 rounded-3xl p-6 sm:p-8 border border-slate-200/80 mb-10 shadow-xs relative">
        {loadingSummary && (
          <div className="absolute top-4 right-4 text-xs font-medium text-slate-400 flex items-center gap-1.5">
            <LoadingSpinner size="sm" className="text-blue-500" />
            <span>Đang cập nhật...</span>
          </div>
        )}

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
          Đánh giá chỉ áp dụng cho khách hàng đã mua sản phẩm và nhận hàng thành công tại gian hàng Rikkei Store.
        </p>

        {!isAuthenticated ? (
          /* Trạng thái chưa đăng nhập */
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

            {/* CHECKLIST [1]: NÚT CHỌN ẢNH & KHUNG XEM TRƯỚC (PREVIEW THUMBNAIL) */}
            <div>
              <div className="flex items-center justify-between mb-2.5">
                <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                  <Camera className="w-4 h-4 text-blue-600" />
                  <span>Hình ảnh thực tế đính kèm (Tối đa 3 ảnh)</span>
                </label>
                <span className="text-xs text-slate-400 font-medium">
                  {selectedImages.length} / {MAX_IMAGES_COUNT} ảnh (tối đa 5MB/ảnh)
                </span>
              </div>

              {/* Hidden file input */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                multiple
                onChange={(e) => handleSelectFiles(e, false)}
                className="hidden"
              />

              {/* Lưới xem trước Thumbnail ảnh & Nút thêm ảnh */}
              <div className="flex flex-wrap items-center gap-3">
                {selectedImages.map((imgItem, idx) => (
                  <div
                    key={imgItem.id || idx}
                    className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-2xl overflow-hidden border-2 border-slate-200 shadow-xs group bg-slate-100"
                  >
                    <img
                      src={imgItem.previewUrl}
                      alt={`Xem trước ảnh ${idx + 1}`}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />

                    {/* Overlay Zoom In khi hover */}
                    <div
                      onClick={() =>
                        openLightbox(
                          selectedImages.map((i) => i.previewUrl),
                          idx
                        )
                      }
                      className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center cursor-pointer"
                      title="Xem phóng to ảnh này"
                    >
                      <ZoomIn className="w-5 h-5 text-white" />
                    </div>

                    {/* Nút xóa ảnh (icon X đỏ) */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleRemoveImage(idx, false)
                      }}
                      className="absolute top-1 right-1 p-1 bg-red-600 hover:bg-red-700 text-white rounded-full shadow-md transition-transform hover:scale-110 active:scale-90 cursor-pointer z-10"
                      title="Xóa ảnh này"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}

                {/* Nút Chọn Thêm Ảnh (Disabled khi đã đủ 3 ảnh) */}
                {selectedImages.length < MAX_IMAGES_COUNT ? (
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl border-2 border-dashed border-blue-300 hover:border-blue-500 bg-blue-50/50 hover:bg-blue-50 text-blue-600 flex flex-col items-center justify-center gap-1 transition-all cursor-pointer group hover:scale-[1.02]"
                    title="Chọn ảnh từ thiết bị của bạn"
                  >
                    <UploadCloud className="w-5 h-5 sm:w-6 sm:h-6 group-hover:-translate-y-0.5 transition-transform" />
                    <span className="text-[11px] font-bold">Thêm ảnh</span>
                    <span className="text-[9px] text-blue-500/80">
                      ({selectedImages.length}/{MAX_IMAGES_COUNT})
                    </span>
                  </button>
                ) : (
                  <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl border border-slate-200 bg-slate-50 text-slate-400 flex flex-col items-center justify-center text-center p-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-500 mb-0.5" />
                    <span className="text-[10px] font-bold text-slate-600 leading-tight">
                      Đã đạt tối đa
                    </span>
                    <span className="text-[9px] text-slate-400">3/3 ảnh</span>
                  </div>
                )}
              </div>
            </div>

            {/* Nút gửi đánh giá */}
            <div className="flex justify-end pt-2">
              <button
                type="submit"
                disabled={submitting || !comment.trim()}
                className="inline-flex items-center gap-2 px-7 py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-full shadow-md shadow-blue-500/20 disabled:opacity-50 disabled:pointer-events-none transition-all active:scale-98 cursor-pointer"
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
                review.userFullName?.trim()?.[0]?.toUpperCase() ||
                review.userName?.trim()?.[0]?.toUpperCase() ||
                'K'
              const isOwner =
                user &&
                (user.id === review.userId ||
                  (user.id && review.userId && String(user.id) === String(review.userId)))
              const canDelete = isOwner || user?.role === 'ADMIN'
              const isEditing = editingReviewId === review.id
              const reviewImages = review.images || []

              return (
                <div
                  key={review.id}
                  className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/90 shadow-2xs hover:shadow-xs transition-shadow"
                >
                  <div className="flex items-start justify-between gap-4">
                    {/* User info */}
                    <div className="flex items-center gap-3">
                      {review.userAvatar ? (
                        <img
                          src={resolveImageUrl(review.userAvatar)}
                          alt={review.userFullName || review.userName}
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
                            {review.userFullName || review.userName || 'Khách hàng Rikkei'}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600 shrink-0" />
                            <span>Đã mua hàng</span>
                          </span>
                        </div>
                        <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                          <span>{formatDateTime(review.createdAt)}</span>
                          {review.updatedAt && review.updatedAt !== review.createdAt && (
                            <span className="text-[11px] text-slate-400 italic">
                              (Đã chỉnh sửa {formatDateTime(review.updatedAt)})
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Số sao & Nút Thao tác */}
                    <div className="flex items-center gap-3">
                      {!isEditing && (
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
                      )}

                      {/* Nút Sửa & Xóa khi thuộc về chính chủ hoặc Admin */}
                      {!isEditing && (
                        <div className="flex items-center gap-1">
                          {isOwner && (
                            <button
                              type="button"
                              onClick={() => handleStartEdit(review)}
                              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                              title="Chỉnh sửa đánh giá"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                              <span>Sửa</span>
                            </button>
                          )}
                          {canDelete && (
                            <button
                              type="button"
                              onClick={() => setDeletingReview(review)}
                              className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                              title="Xóa đánh giá"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              <span>Xóa</span>
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Khi đang ở chế độ chỉnh sửa (Inline Edit) */}
                  {isEditing ? (
                    <div className="mt-4 pl-0 sm:pl-13 space-y-4">
                      {/* Đổi số sao */}
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-600">Đổi số sao:</span>
                        <div className="flex items-center gap-1 bg-slate-50 px-2.5 py-1 rounded-xl border border-slate-200">
                          {[1, 2, 3, 4, 5].map((star) => {
                            const isHovered = editHoverRating >= star
                            const isSelected = !editHoverRating && editRating >= star
                            return (
                              <button
                                key={star}
                                type="button"
                                onClick={() => setEditRating(star)}
                                onMouseEnter={() => setEditHoverRating(star)}
                                onMouseLeave={() => setEditHoverRating(0)}
                                className="p-0.5 cursor-pointer transition-transform hover:scale-125 focus:outline-hidden"
                              >
                                <Star
                                  className={`w-5 h-5 transition-colors ${
                                    isHovered || isSelected
                                      ? 'text-amber-400 fill-amber-400'
                                      : 'text-slate-300'
                                  }`}
                                />
                              </button>
                            )
                          })}
                        </div>
                        <span className="text-xs font-bold text-amber-600">
                          {RATING_LABELS[editHoverRating || editRating]}
                        </span>
                      </div>

                      {/* Textarea sửa nhận xét */}
                      <textarea
                        rows={3}
                        value={editComment}
                        onChange={(e) => setEditComment(e.target.value)}
                        placeholder="Cập nhật nội dung đánh giá của bạn..."
                        className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 text-sm text-slate-800 placeholder-slate-400 resize-none outline-hidden"
                      />

                      {/* Sửa ảnh đính kèm (Xem lại ảnh cũ, Thêm ảnh mới, Xóa từng ảnh) */}
                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-xs text-slate-500">
                          <span className="font-bold text-slate-700 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                            <Camera className="w-3.5 h-3.5 text-blue-600" />
                            Hình ảnh đính kèm ({editImages.length}/{MAX_IMAGES_COUNT})
                          </span>
                          <span className="text-[11px] text-slate-400">
                            Hỗ trợ JPG, PNG, WEBP, GIF &lt; 5MB/ảnh
                          </span>
                        </div>

                        <input
                          ref={editFileInputRef}
                          type="file"
                          accept="image/jpeg,image/png,image/webp,image/gif"
                          multiple
                          onChange={(e) => handleSelectFiles(e, true)}
                          className="hidden"
                        />

                        <div className="flex flex-wrap items-center gap-3">
                          {editImages.map((imgItem, idx) => (
                            <div
                              key={imgItem.id || idx}
                              className="relative group w-20 h-20 rounded-2xl overflow-hidden border-2 border-slate-200 bg-slate-100 shadow-2xs hover:border-blue-400 transition-all"
                            >
                              <img
                                src={resolveImageUrl(imgItem.previewUrl)}
                                alt={`Ảnh đính kèm ${idx + 1}`}
                                className="w-full h-full object-cover"
                              />

                              {/* Badge phân biệt ảnh cũ vs ảnh mới */}
                              <span
                                className={`absolute bottom-1 left-1 text-[9px] font-bold px-1.5 py-0.5 rounded-md shadow-xs pointer-events-none ${
                                  imgItem.isExisting
                                    ? 'bg-slate-900/80 text-white'
                                    : 'bg-emerald-600 text-white'
                                }`}
                              >
                                {imgItem.isExisting ? 'Ảnh hiện tại' : 'Ảnh mới'}
                              </span>

                              {/* Nút xem phóng to ảnh */}
                              <button
                                type="button"
                                onClick={() => {
                                  const allUrls = editImages.map((it) => it.previewUrl)
                                  openLightbox(allUrls, idx)
                                }}
                                className="absolute inset-0 bg-black/35 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity text-white cursor-pointer"
                                title="Xem ảnh phóng to"
                              >
                                <div className="p-1 rounded-full bg-white/20 backdrop-blur-xs">
                                  <ZoomIn className="w-3.5 h-3.5" />
                                </div>
                              </button>

                              {/* Nút xóa ảnh riêng */}
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  handleRemoveImage(idx, true)
                                }}
                                className="absolute top-1 right-1 z-10 w-5 h-5 bg-red-600 hover:bg-red-700 text-white rounded-full flex items-center justify-center cursor-pointer shadow-md transition-transform hover:scale-110"
                                title={imgItem.isExisting ? 'Xóa ảnh cũ này khỏi đánh giá' : 'Bỏ ảnh mới này'}
                              >
                                <X className="w-3 h-3" />
                              </button>
                            </div>
                          ))}

                          {editImages.length < MAX_IMAGES_COUNT && (
                            <button
                              type="button"
                              onClick={() => editFileInputRef.current?.click()}
                              className="w-20 h-20 rounded-2xl border-2 border-dashed border-blue-300 hover:border-blue-500 bg-blue-50/40 hover:bg-blue-50 text-blue-600 flex flex-col items-center justify-center gap-1 transition-all cursor-pointer text-[10px] font-bold shadow-2xs group"
                              title={`Thêm ảnh mới (Còn ${MAX_IMAGES_COUNT - editImages.length} slot)`}
                            >
                              <UploadCloud className="w-5 h-5 group-hover:scale-110 transition-transform" />
                              <span>+ Thêm ảnh</span>
                              <span className="text-[9px] text-blue-400 font-normal">
                                ({editImages.length}/{MAX_IMAGES_COUNT})
                              </span>
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Action buttons */}
                      <div className="flex items-center gap-2 justify-end">
                        <button
                          type="button"
                          onClick={handleCancelEdit}
                          disabled={editSubmitting}
                          className="px-3 py-1.5 rounded-xl border border-slate-300 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                        >
                          Hủy
                        </button>
                        <button
                          type="button"
                          onClick={() => handleSaveEdit(review.id)}
                          disabled={editSubmitting || !editComment.trim()}
                          className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs cursor-pointer disabled:opacity-50"
                        >
                          {editSubmitting ? (
                            <>
                              <LoadingSpinner size="sm" className="text-white" />
                              <span>Đang lưu...</span>
                            </>
                          ) : (
                            <>
                              <Check className="w-3.5 h-3.5" />
                              <span>Lưu thay đổi</span>
                            </>
                          )}
                        </button>
                      </div>
                    </div>
                  ) : (
                    /* Nội dung Comment thông thường */
                    <div className="mt-3.5 pl-0 sm:pl-13 text-sm text-slate-700 whitespace-pre-line leading-relaxed">
                      {review.comment}
                    </div>
                  )}

                  {/* CHECKLIST [2]: DANH SÁCH ẢNH ĐÍNH KÈM CỦA KHÁCH & LIGHTBOX TRIGGER */}
                  {!isEditing && reviewImages.length > 0 && (
                    <div className="mt-3.5 pl-0 sm:pl-13">
                      <div className="flex flex-wrap items-center gap-2.5">
                        {reviewImages.map((imgUrl, imgIdx) => (
                          <div
                            key={imgIdx}
                            onClick={() => openLightbox(reviewImages, imgIdx)}
                            className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-2xl overflow-hidden border border-slate-200/90 shadow-2xs group cursor-pointer bg-slate-50 hover:shadow-md transition-all duration-200"
                            title="Bấm để xem ảnh phóng to sắc nét"
                          >
                            <img
                              src={resolveImageUrl(imgUrl)}
                              alt={`Hình ảnh đánh giá ${imgIdx + 1}`}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                              loading="lazy"
                            />
                            {/* Hover Overlay Zoom Indicator */}
                            <div className="absolute inset-0 bg-black/35 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                              <div className="p-1.5 rounded-full bg-white/20 backdrop-blur-xs text-white">
                                <ZoomIn className="w-4 h-4" />
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* CHECKLIST [3]: KHUNG "PHẢN HỒI TỪ NGƯỜI BÁN" BO GÓC MÀU XÁM NHẠT */}
                  {review.adminReply && (
                    <div className="mt-4 pl-0 sm:pl-13">
                      <div className="bg-slate-50/90 border border-slate-200/80 border-l-4 border-indigo-600 rounded-2xl p-4 shadow-2xs">
                        <div className="flex items-center justify-between gap-2 mb-1.5">
                          <div className="flex items-center gap-1.5 font-bold text-xs text-indigo-900">
                            <Store className="w-3.5 h-3.5 text-indigo-600" />
                            <span>Phản hồi từ Người bán</span>
                            <ShieldCheck className="w-3.5 h-3.5 text-blue-600 ml-0.5" />
                          </div>
                          {review.adminReplyAt && (
                            <span className="text-[11px] text-slate-400">
                              {formatDateTime(review.adminReplyAt)}
                            </span>
                          )}
                        </div>
                        <p className="text-xs sm:text-sm text-slate-700 whitespace-pre-line leading-relaxed font-normal">
                          {review.adminReply}
                        </p>
                      </div>
                    </div>
                  )}
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

      {/* Modal xác nhận xóa đánh giá */}
      {deletingReview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-slate-100 animate-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-red-50 border border-red-100 flex items-center justify-center mx-auto text-red-600 mb-4">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-slate-900 text-center">
              Xác nhận xóa đánh giá
            </h3>
            <p className="text-sm text-slate-500 text-center mt-2 leading-relaxed">
              Bạn có chắc chắn muốn xóa đánh giá này không? Thao tác này sẽ tính toán lại điểm sao trung bình của sản phẩm và không thể hoàn tác.
            </p>

            <div className="mt-6 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setDeletingReview(null)}
                disabled={isDeleting}
                className="px-4 py-2.5 border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Hủy thao tác
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50"
              >
                {isDeleting && <LoadingSpinner size="sm" className="text-white" />}
                <span>{isDeleting ? 'Đang xóa...' : 'Xác nhận xóa'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox phóng to ảnh đánh giá */}
      <ReviewImageLightbox
        isOpen={lightboxState.isOpen}
        images={lightboxState.images}
        currentIndex={lightboxState.currentIndex}
        onClose={closeLightbox}
        onNavigate={handleLightboxNavigate}
      />
    </section>
  )
}

export default ProductReviewsSection
