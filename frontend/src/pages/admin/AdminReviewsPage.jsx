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
  SlidersHorizontal,
} from 'lucide-react'
import reviewService from '../../services/reviewService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import Pagination from '../../components/common/Pagination'

/**
 * Trang Quản trị Đánh giá & Nhận xét sản phẩm (AdminReviewsPage).
 * Tuyến đường (Route): /admin/reviews
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 *
 * Tính năng chính & Quy chuẩn giao diện:
 * 1. Đồng bộ 100% với Design System của hệ thống Admin (shadcn/ui Neutral/Zinc Palette, nền sáng đồng nhất).
 * 2. Hiển thị danh sách đánh giá sản phẩm có phân trang từ API GET /api/v1/admin/reviews.
 * 3. Bộ lọc trạng thái hiển thị: Tất cả, Đang hiển thị (isVisible = true), Đã ẩn (isVisible = false).
 * 4. Lọc theo điểm số sao đánh giá (1 - 5 sao) và tìm kiếm tức thì theo mã ID, khách hàng, sản phẩm, bình luận.
 * 5. Chuyển đổi trạng thái hiển thị công khai (Ẩn / Hiện) qua PUT /api/v1/admin/reviews/{id}/toggle-status.
 * 6. Xóa vĩnh viễn đánh giá và dọn dẹp các tệp ảnh khỏi storage qua DELETE /api/v1/admin/reviews/{id} kèm Modal xác nhận chuẩn Admin.
 * 7. Lightbox modal phóng to hình ảnh đính kèm của đánh giá.
 * 8. Tái sử dụng component Pagination dùng chung với theme="zinc" và tùy chọn thay đổi kích thước trang (10, 20, 50).
 */
export const AdminReviewsPage = () => {
  // --- Danh sách đánh giá & Phân trang ---
  const [reviews, setReviews] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
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
  const fetchReviews = useCallback(
    async (targetPage = 0, currentSize = pageSize) => {
      setIsLoading(true)
      try {
        let isVisibleParam = null
        if (visibilityFilter === 'VISIBLE') isVisibleParam = true
        if (visibilityFilter === 'HIDDEN') isVisibleParam = false

        const res = await reviewService.getAdminReviews(targetPage, currentSize, isVisibleParam)
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
    },
    [pageSize, visibilityFilter]
  )

  useEffect(() => {
    fetchReviews(0, pageSize)
  }, [fetchReviews, pageSize])

  const handleRefresh = () => {
    setIsRefreshing(true)
    fetchReviews(page, pageSize)
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
        if (reviews.length === 1 && page > 0) {
          fetchReviews(page - 1, pageSize)
        } else {
          fetchReviews(page, pageSize)
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

  // Thống kê nhanh trên trang hiện tại
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
    <div className="space-y-6 max-w-7xl mx-auto w-full animate-in fade-in duration-200">
      {/* Toast Notification */}
      {toastMessage && (
        <div
          className={`fixed top-20 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg border text-sm font-semibold transition-all animate-in slide-in-from-top-2 ${
            toastMessage.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          )}
          <span className="leading-snug">{toastMessage.text}</span>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="p-1 hover:bg-slate-200/50 rounded-lg text-slate-500 cursor-pointer ml-2"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 1. Header Trang & Thao tác chính */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2.5">
              <Star className="w-7 h-7 text-amber-500 fill-amber-500" />
              <span>Quản lý đánh giá</span>
            </h1>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200 uppercase tracking-wider">
              <MessageSquare className="w-3.5 h-3.5" /> {totalElements} đánh giá
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Kiểm duyệt bình luận, ẩn/hiển thị công khai và xóa vĩnh viễn đánh giá vi phạm tiêu chuẩn cộng đồng.
          </p>
        </div>

        {/* Nút hành động */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={handleRefresh}
            disabled={isLoading || isRefreshing}
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:text-zinc-950 shadow-2xs transition-all cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách đánh giá"
          >
            <RefreshCw
              className={`w-4 h-4 ${isRefreshing || isLoading ? 'animate-spin text-zinc-900' : 'text-slate-500'}`}
            />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* 2. Thống kê nhanh (KPI Cards) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5">
        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Tổng trên hệ thống</span>
            <MessageSquare className="w-4 h-4 text-slate-400" />
          </div>
          <div className="text-2xl font-black text-slate-900 tabular-nums">
            {totalElements}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-emerald-200/80 bg-emerald-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-emerald-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Đang hiển thị (Trang này)</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-black text-emerald-900 tabular-nums">
            {stats.visibleCount}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-amber-200/80 bg-amber-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-amber-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Đã bị ẩn (Trang này)</span>
            <EyeOff className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl font-black text-amber-900 tabular-nums">
            {stats.hiddenCount}
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-blue-200/80 bg-blue-50/20 p-4 shadow-2xs">
          <div className="flex items-center justify-between text-blue-700 mb-2">
            <span className="text-[11px] font-bold uppercase tracking-wider">Có ảnh đính kèm (Trang này)</span>
            <ImageIcon className="w-4 h-4 text-blue-500" />
          </div>
          <div className="text-2xl font-black text-blue-900 tabular-nums">
            {stats.withImagesCount}
          </div>
        </div>
      </div>

      {/* 3. Thanh tìm kiếm & bộ lọc */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-2xs space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3.5">
          {/* Ô tìm kiếm */}
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo ID, khách hàng, sản phẩm, bình luận..."
              className="w-full pl-9 pr-9 py-2.5 rounded-xl border border-slate-200 bg-slate-50/60 text-xs text-slate-900 placeholder:text-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-zinc-900 focus:border-transparent transition-all"
            />
            {searchTerm && (
              <button
                type="button"
                onClick={() => setSearchTerm('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Lọc theo số sao */}
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <span className="font-semibold text-slate-600 hidden sm:inline">Mức sao:</span>
              <select
                value={selectedRating}
                onChange={(e) => setSelectedRating(e.target.value)}
                className="px-3 py-2 rounded-xl border border-slate-200 bg-slate-50/60 text-xs font-semibold text-slate-800 focus:outline-none focus:bg-white focus:ring-2 focus:ring-zinc-900 cursor-pointer"
              >
                <option value="ALL">Tất cả số sao</option>
                <option value="5">5 sao ⭐⭐⭐⭐⭐</option>
                <option value="4">4 sao ⭐⭐⭐⭐</option>
                <option value="3">3 sao ⭐⭐⭐</option>
                <option value="2">2 sao ⭐⭐</option>
                <option value="1">1 sao ⭐</option>
              </select>
            </div>

            {/* Chọn số dòng mỗi trang */}
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <span className="font-semibold text-slate-600 hidden sm:inline">Hiển thị:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  const newSize = Number(e.target.value)
                  setPageSize(newSize)
                  setPage(0)
                  fetchReviews(0, newSize)
                }}
                className="px-3 py-2 rounded-xl border border-slate-200 bg-slate-50/60 text-xs font-semibold text-slate-800 focus:outline-none focus:bg-white focus:ring-2 focus:ring-zinc-900 cursor-pointer"
              >
                <option value={10}>10 đánh giá / trang</option>
                <option value={20}>20 đánh giá / trang</option>
                <option value={50}>50 đánh giá / trang</option>
              </select>
            </div>
          </div>
        </div>

        {/* Tab phân loại trạng thái hiển thị */}
        <div className="flex items-center gap-2 pt-2 border-t border-slate-100 overflow-x-auto pb-1">
          <span className="text-xs font-bold text-slate-500 mr-1 flex items-center gap-1.5 shrink-0">
            <SlidersHorizontal className="w-3.5 h-3.5" />
            Trạng thái:
          </span>
          <button
            type="button"
            onClick={() => setVisibilityFilter('ALL')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              visibilityFilter === 'ALL'
                ? 'bg-zinc-900 text-white shadow-2xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Tất cả đánh giá
          </button>
          <button
            type="button"
            onClick={() => setVisibilityFilter('VISIBLE')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              visibilityFilter === 'VISIBLE'
                ? 'bg-zinc-900 text-white shadow-2xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Đang hiển thị
          </button>
          <button
            type="button"
            onClick={() => setVisibilityFilter('HIDDEN')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
              visibilityFilter === 'HIDDEN'
                ? 'bg-zinc-900 text-white shadow-2xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            Đã bị ẩn
          </button>
        </div>
      </div>

      {/* 4. Bảng danh sách đánh giá */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="py-24 flex flex-col items-center justify-center space-y-3">
            <LoadingSpinner size="lg" className="text-zinc-900" />
            <p className="text-xs text-slate-500 font-medium">Đang tải danh sách đánh giá...</p>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="py-20 px-4 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
              <MessageSquare className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-slate-900">Không tìm thấy đánh giá nào</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {searchTerm || selectedRating !== 'ALL' || visibilityFilter !== 'ALL'
                ? 'Không có đánh giá nào khớp với điều kiện tìm kiếm hoặc bộ lọc hiện tại.'
                : 'Chưa có đánh giá nào được gửi trên hệ thống.'}
            </p>
            {(searchTerm || selectedRating !== 'ALL' || visibilityFilter !== 'ALL') && (
              <button
                type="button"
                onClick={() => {
                  setVisibilityFilter('ALL')
                  setSelectedRating('ALL')
                  setSearchTerm('')
                }}
                className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors cursor-pointer"
              >
                Xóa bộ lọc
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 px-4 w-16">Mã ID</th>
                  <th className="py-3.5 px-4 min-w-[200px]">Sản phẩm</th>
                  <th className="py-3.5 px-4 min-w-[170px]">Khách hàng</th>
                  <th className="py-3.5 px-4 min-w-[300px]">Nội dung &amp; Ảnh đính kèm</th>
                  <th className="py-3.5 px-4 w-32 text-center">Trạng thái</th>
                  <th className="py-3.5 px-4 w-36 text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredReviews.map((r) => {
                  const isToggling = togglingId === r.id

                  return (
                    <tr
                      key={r.id}
                      className={`hover:bg-slate-50/60 transition-colors ${
                        !r.isVisible ? 'bg-amber-50/20' : ''
                      }`}
                    >
                      {/* Cột 1: ID & Ngày tạo */}
                      <td className="py-4 px-4 align-top">
                        <span className="font-mono font-bold text-slate-800">#{r.id}</span>
                        <div className="text-[11px] text-slate-400 mt-1 whitespace-nowrap">
                          {formatDate(r.createdAt)}
                        </div>
                      </td>

                      {/* Cột 2: Sản phẩm */}
                      <td className="py-4 px-4 align-top">
                        <div className="flex items-start gap-3">
                          {r.productImage ? (
                            <img
                              src={r.productImage}
                              alt={r.productName || 'Product'}
                              className="w-10 h-10 rounded-lg object-cover bg-slate-100 shrink-0 border border-slate-200"
                              onError={(e) => {
                                e.currentTarget.style.display = 'none'
                              }}
                            />
                          ) : (
                            <div className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center shrink-0 text-slate-400">
                              <Package className="w-4 h-4" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <span className="font-semibold text-slate-900 block truncate leading-tight">
                              {r.productName || `Sản phẩm #${r.productId}`}
                            </span>
                            <span className="text-[11px] text-slate-400 mt-0.5 block">
                              Mã SP: #{r.productId}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Cột 3: Khách hàng */}
                      <td className="py-4 px-4 align-top">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-700 font-bold text-xs shrink-0">
                            {(r.userFullName || r.userName || 'U').charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <span className="font-semibold text-slate-800 block truncate">
                              {r.userFullName || r.userName || 'Khách hàng ẩn danh'}
                            </span>
                            <span className="text-[11px] text-slate-400">User ID: #{r.userId}</span>
                          </div>
                        </div>
                      </td>

                      {/* Cột 4: Đánh giá & Bình luận & Ảnh */}
                      <td className="py-4 px-4 align-top space-y-2">
                        {/* Số sao */}
                        <div className="flex items-center gap-1">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <Star
                              key={star}
                              className={`w-3.5 h-3.5 ${
                                star <= r.rating
                                  ? 'text-amber-400 fill-amber-400'
                                  : 'text-slate-200 fill-slate-200'
                              }`}
                            />
                          ))}
                          <span className="text-xs font-bold text-amber-500 ml-1">
                            {r.rating}.0
                          </span>
                        </div>

                        {/* Nội dung nhận xét */}
                        <p className="text-xs text-slate-700 whitespace-pre-line leading-relaxed line-clamp-3">
                          {r.comment || <span className="italic text-slate-400">Không có nhận xét</span>}
                        </p>

                        {/* Danh sách ảnh đính kèm */}
                        {r.images && r.images.length > 0 && (
                          <div className="flex items-center gap-2 pt-1">
                            <span className="text-[11px] text-slate-500 font-medium flex items-center gap-1">
                              <ImageIcon className="w-3.5 h-3.5 text-blue-500" />
                              <span>{r.images.length} ảnh:</span>
                            </span>
                            <div className="flex items-center gap-1.5 flex-wrap">
                              {r.images.map((img) => (
                                <button
                                  key={img.id}
                                  type="button"
                                  onClick={() => setActiveLightboxImage(img.imageUrl)}
                                  className="group relative w-10 h-10 rounded-lg overflow-hidden border border-slate-200 hover:border-zinc-900 transition-all cursor-pointer shadow-2xs"
                                  title="Bấm để xem ảnh phóng to"
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
                                    <ZoomIn className="w-3.5 h-3.5 text-white" />
                                  </div>
                                </button>
                              ))}
                            </div>
                          </div>
                        )}
                      </td>

                      {/* Cột 5: Trạng thái hiển thị */}
                      <td className="py-4 px-4 align-top text-center">
                        {r.isVisible ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                            Hiển thị
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
                            <EyeOff className="w-3 h-3" />
                            Đã bị ẩn
                          </span>
                        )}
                      </td>

                      {/* Cột 6: Thao tác */}
                      <td className="py-4 px-4 align-top text-center">
                        <div className="inline-flex items-center gap-1.5">
                          {/* Nút Chuyển đổi Ẩn / Hiện */}
                          <button
                            type="button"
                            onClick={() => handleToggleStatus(r)}
                            disabled={isToggling}
                            className={`p-2 rounded-xl border transition-colors cursor-pointer ${
                              r.isVisible
                                ? 'bg-white border-slate-200 text-slate-600 hover:text-amber-700 hover:bg-amber-50 hover:border-amber-200'
                                : 'bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100'
                            } disabled:opacity-50`}
                            title={r.isVisible ? 'Ẩn đánh giá khỏi trang sản phẩm' : 'Hiển thị lại đánh giá'}
                          >
                            {isToggling ? (
                              <LoadingSpinner size="sm" className="text-zinc-900" />
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
                            className="p-2 rounded-xl bg-white border border-slate-200 text-slate-600 hover:text-red-600 hover:bg-red-50 hover:border-red-200 transition-colors cursor-pointer"
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

        {/* 5. Phân trang chuẩn hóa với component Pagination */}
        <div className="p-4 bg-slate-50/60 border-t border-slate-200">
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(newPage) => fetchReviews(newPage, pageSize)}
            onPageSizeChange={(newSize) => {
              setPageSize(newSize)
              setPage(0)
              fetchReviews(0, newSize)
            }}
            pageSizeOptions={[10, 20, 50]}
            theme="zinc"
            scrollToTop={false}
          />
        </div>
      </div>

      {/* 6. Modal Xác nhận Xóa vĩnh viễn Review */}
      {deletingReview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto">
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
            onClick={() => !isDeleting && setDeletingReview(null)}
          />

          <div className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-md p-6 z-10 animate-in fade-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-4">
              <AlertTriangle className="w-6 h-6" />
            </div>

            <h3 className="text-lg font-bold text-slate-900 text-center">
              Xác nhận xóa vĩnh viễn đánh giá
            </h3>
            <p className="text-sm text-slate-500 text-center mt-2 leading-relaxed">
              Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá{' '}
              <strong className="text-slate-800 font-semibold">
                #{deletingReview.id}
              </strong>{' '}
              của khách hàng{' '}
              <strong className="text-slate-800 font-semibold">
                "{deletingReview.userFullName || deletingReview.userName || 'Khách hàng'}"
              </strong>?
            </p>

            <div className="mt-4 p-3.5 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1.5 text-slate-600">
              <div className="flex items-center gap-1.5">
                <span className="font-semibold text-slate-700">Điểm đánh giá:</span>
                <span className="font-bold text-amber-500">{deletingReview.rating} sao</span>
              </div>
              <div>
                <span className="font-semibold text-slate-700">Tệp ảnh đính kèm:</span>{' '}
                <span className="text-slate-800 font-medium">
                  {deletingReview.images?.length || 0} tệp ảnh (sẽ bị dọn sạch khỏi ổ đĩa)
                </span>
              </div>
              {deletingReview.comment && (
                <div>
                  <span className="font-semibold text-slate-700">Nội dung nhận xét:</span>
                  <p className="italic text-slate-500 mt-0.5 line-clamp-2">
                    &ldquo;{deletingReview.comment}&rdquo;
                  </p>
                </div>
              )}
            </div>

            <p className="text-xs text-rose-600 bg-rose-50 border border-rose-200 rounded-lg p-2.5 mt-3 text-center font-medium">
              Lưu ý: Hành động này không thể hoàn tác. Hệ thống sẽ tự động tính toán lại điểm rating trung bình của sản phẩm sau khi xóa.
            </p>

            <div className="mt-6 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setDeletingReview(null)}
                disabled={isDeleting}
                className="px-4 py-2 border border-slate-300 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Hủy thao tác
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="inline-flex items-center gap-2 px-5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold shadow-xs transition-all cursor-pointer disabled:opacity-50"
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
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-xs animate-in fade-in duration-200"
          onClick={() => setActiveLightboxImage(null)}
        >
          <div
            className="relative max-w-3xl max-h-[85vh] bg-white rounded-2xl shadow-2xl p-2 border border-slate-200 overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setActiveLightboxImage(null)}
              className="absolute top-4 right-4 z-10 p-2 rounded-full bg-slate-900/80 hover:bg-slate-950 text-white transition-colors cursor-pointer shadow-md"
              title="Đóng cửa sổ ảnh"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={activeLightboxImage}
              alt="Hình ảnh đánh giá"
              className="max-h-[80vh] w-auto mx-auto object-contain rounded-xl"
            />
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminReviewsPage
