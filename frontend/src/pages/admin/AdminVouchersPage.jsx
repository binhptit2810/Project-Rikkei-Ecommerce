import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Ticket,
  Plus,
  RefreshCw,
  Search,
  Edit2,
  Trash2,
  AlertCircle,
  CheckCircle2,
  X,
  Copy,
  Check,
  Percent,
  Coins,
  Clock,
  ShieldCheck,
  AlertTriangle,
  ToggleLeft,
  ToggleRight,
  Filter,
} from 'lucide-react'
import couponService from '../../services/couponService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import VoucherModal from '../../components/admin/VoucherModal'

/**
 * Trang Quản lý Mã giảm giá dành cho Admin (AdminVouchersPage).
 * Tuyến đường (Route): /admin/vouchers (và alias /admin/coupons)
 * Phụ trách: Đặng Thanh Bình (DTB)
 * Phối hợp: Đỗ Đức Mạnh (DDM)
 *
 * Tiêu chí nghiệm thu (DoD):
 * Trang quản trị voucher cho phép Admin thêm/sửa/xóa mã nhanh chóng.
 */
export const AdminVouchersPage = () => {
  const [vouchers, setVouchers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // Search & Filter state
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL') // ALL, ACTIVE, INACTIVE, EXPIRED, MAXED
  const [typeFilter, setTypeFilter] = useState('ALL') // ALL, PERCENT, FIXED

  // Modal Thêm / Sửa Voucher
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingVoucher, setEditingVoucher] = useState(null)

  // Modal Xóa Voucher
  const [deletingVoucher, setDeletingVoucher] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Toggle status loading
  const [togglingId, setTogglingId] = useState(null)

  // Copy code feedback
  const [copiedCode, setCopiedCode] = useState(null)

  const showToast = (text, type = 'success') => {
    setToastMessage({ text, type })
    setTimeout(() => setToastMessage(null), 4000)
  }

  // Tải danh sách voucher từ server
  const fetchVouchers = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      let res
      try {
        res = await couponService.getAdminCoupons()
      } catch (adminErr) {
        // Fallback sang API active nếu chưa có dữ liệu admin
        console.warn('Fallback sang active coupons:', adminErr)
        res = await couponService.getActiveCoupons()
      }

      const data = res?.data || res
      if (Array.isArray(data)) {
        setVouchers(data)
      } else {
        setVouchers([])
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách mã giảm giá từ hệ thống.'
      setError(msg)
      setVouchers([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchVouchers()
  }, [fetchVouchers])

  // Xử lý bật/tắt nhanh trạng thái active
  const handleToggleStatus = async (voucher) => {
    if (!voucher?.id || togglingId) return

    setTogglingId(voucher.id)
    try {
      const res = await couponService.toggleCouponStatus(voucher.id)
      const updated = res?.data || res
      setVouchers((prev) =>
        prev.map((item) =>
          item.id === voucher.id ? { ...item, active: updated.active } : item
        )
      )
      showToast(
        `Đã ${updated.active ? 'kích hoạt' : 'tạm ngưng'} mã giảm giá #${voucher.code} thành công!`
      )
    } catch (err) {
      showToast(
        err.response?.data?.message || 'Không thể thay đổi trạng thái mã giảm giá.',
        'error'
      )
    } finally {
      setTogglingId(null)
    }
  }

  // Xử lý xác nhận xóa voucher
  const handleConfirmDelete = async () => {
    if (!deletingVoucher?.id || isDeleting) return

    setIsDeleting(true)
    try {
      await couponService.deleteCoupon(deletingVoucher.id)
      setVouchers((prev) => prev.filter((item) => item.id !== deletingVoucher.id))
      showToast(`Đã xóa vĩnh viễn mã giảm giá #${deletingVoucher.code}!`)
      setDeletingVoucher(null)
    } catch (err) {
      showToast(
        err.response?.data?.message || 'Lỗi khi xóa mã giảm giá. Vui lòng thử lại.',
        'error'
      )
    } finally {
      setIsDeleting(false)
    }
  }

  // Copy mã vào clipboard
  const handleCopyCode = (code) => {
    if (!code) return
    navigator.clipboard.writeText(code)
    setCopiedCode(code)
    setTimeout(() => setCopiedCode(null), 2000)
  }

  // Format tiền tệ VND
  const formatPrice = (val) => {
    const num = Number(val)
    if (isNaN(num)) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num)
  }

  // Format ngày giờ
  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'Không thời hạn'
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    const pad = (n) => String(n).padStart(2, '0')
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(
      d.getHours()
    )}:${pad(d.getMinutes())}`
  }

  // Kiểm tra trạng thái voucher
  const isVoucherExpired = (v) => {
    if (v.isExpired !== undefined) return Boolean(v.isExpired)
    return v.expireAt && new Date(v.expireAt).getTime() < Date.now()
  }

  const isVoucherMaxed = (v) => {
    if (v.isUsageLimitReached !== undefined) return Boolean(v.isUsageLimitReached)
    return v.usageLimit != null && v.usedCount != null && v.usedCount >= v.usageLimit
  }

  // Thống kê số lượng
  const stats = useMemo(() => {
    const total = vouchers.length
    const active = vouchers.filter(
      (v) => v.active && !isVoucherExpired(v) && !isVoucherMaxed(v)
    ).length
    const maxed = vouchers.filter((v) => isVoucherMaxed(v)).length
    const expired = vouchers.filter((v) => isVoucherExpired(v)).length
    return { total, active, maxed, expired }
  }, [vouchers])

  // Lọc danh sách voucher theo từ khóa & filter dropdown
  const filteredVouchers = useMemo(() => {
    return vouchers.filter((v) => {
      // 1. Tìm kiếm theo mã code
      if (searchTerm.trim()) {
        const term = searchTerm.trim().toUpperCase()
        if (!v.code?.toUpperCase().includes(term)) return false
      }

      // 2. Lọc theo trạng thái
      if (statusFilter === 'ACTIVE') {
        if (!v.active || isVoucherExpired(v) || isVoucherMaxed(v)) return false
      } else if (statusFilter === 'INACTIVE') {
        if (v.active) return false
      } else if (statusFilter === 'EXPIRED') {
        if (!isVoucherExpired(v)) return false
      } else if (statusFilter === 'MAXED') {
        if (!isVoucherMaxed(v)) return false
      }

      // 3. Lọc theo loại giảm giá
      if (typeFilter !== 'ALL') {
        if (v.discountType !== typeFilter) return false
      }

      return true
    })
  }, [vouchers, searchTerm, statusFilter, typeFilter])

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in fade-in slide-in-from-bottom-4 duration-200">
          <div
            className={`px-4 py-3 rounded-2xl shadow-xl border flex items-center gap-2.5 text-xs font-semibold backdrop-blur-md ${
              toastMessage.type === 'error'
                ? 'bg-red-500 text-white border-red-600'
                : 'bg-zinc-900 text-white border-zinc-800'
            }`}
          >
            {toastMessage.type === 'error' ? (
              <AlertCircle className="w-4 h-4 shrink-0 text-red-200" />
            ) : (
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
            )}
            <span>{toastMessage.text}</span>
            <button
              onClick={() => setToastMessage(null)}
              className="p-1 hover:opacity-80 cursor-pointer ml-2"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Header & Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-zinc-200/80">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-zinc-900 text-white flex items-center justify-center shadow-xs">
              <Ticket className="w-5 h-5 text-amber-400" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-zinc-900 tracking-tight">
                Quản lý mã giảm giá (Vouchers)
              </h1>
              <p className="text-xs text-zinc-500 font-sans">
                Tạo mới, chỉnh sửa và quản lý các chương trình khuyến mãi ưu đãi cho shop
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={fetchVouchers}
            disabled={isLoading}
            className="p-2.5 rounded-xl border border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 transition-colors shadow-2xs cursor-pointer disabled:opacity-50"
            title="Làm mới danh sách"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => {
              setEditingVoucher(null)
              setIsModalOpen(true)
            }}
            className="px-4 py-2.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 text-white text-xs font-bold transition-all shadow-xs flex items-center gap-2 active:scale-[0.98] cursor-pointer"
          >
            <Plus className="w-4 h-4 text-emerald-400" />
            <span>Thêm voucher mới</span>
          </button>
        </div>
      </div>

      {/* Overview Metric Badges */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 font-sans">
        <div className="p-4 rounded-2xl bg-white border border-zinc-200/80 shadow-2xs">
          <div className="flex items-center justify-between text-zinc-400 text-xs mb-1">
            <span>Tổng số mã</span>
            <Ticket className="w-4 h-4 text-zinc-500" />
          </div>
          <div className="text-2xl font-black text-zinc-900 font-mono">{stats.total}</div>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-zinc-200/80 shadow-2xs">
          <div className="flex items-center justify-between text-zinc-400 text-xs mb-1">
            <span>Đang hoạt động</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-black text-emerald-600 font-mono">{stats.active}</div>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-zinc-200/80 shadow-2xs">
          <div className="flex items-center justify-between text-zinc-400 text-xs mb-1">
            <span>Hết lượt dùng</span>
            <AlertTriangle className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl font-black text-amber-600 font-mono">{stats.maxed}</div>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-zinc-200/80 shadow-2xs">
          <div className="flex items-center justify-between text-zinc-400 text-xs mb-1">
            <span>Đã hết hạn</span>
            <Clock className="w-4 h-4 text-red-500" />
          </div>
          <div className="text-2xl font-black text-red-600 font-mono">{stats.expired}</div>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="bg-white p-3.5 sm:p-4 rounded-2xl border border-zinc-200/80 shadow-2xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 font-sans">
        {/* Search Input */}
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-400" />
          <input
            type="text"
            placeholder="Tìm theo mã giảm giá (VD: SALE10, FREESHIP...)"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-3.5 py-2 rounded-xl border border-zinc-200 text-xs font-medium text-zinc-900 placeholder:text-zinc-400 focus:outline-hidden focus:border-zinc-900 focus:ring-1 focus:ring-zinc-900 transition-all font-mono"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 p-0.5 cursor-pointer"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Dropdown Filters */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex items-center gap-1.5 text-xs text-zinc-500">
            <Filter className="w-3.5 h-3.5 text-zinc-400" />
            <span>Lọc:</span>
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 rounded-xl border border-zinc-200 bg-white text-xs font-semibold text-zinc-700 focus:outline-hidden focus:border-zinc-900 cursor-pointer"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="INACTIVE">Tạm ngưng</option>
            <option value="MAXED">Hết lượt dùng</option>
            <option value="EXPIRED">Đã hết hạn</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-3 py-2 rounded-xl border border-zinc-200 bg-white text-xs font-semibold text-zinc-700 focus:outline-hidden focus:border-zinc-900 cursor-pointer"
          >
            <option value="ALL">Tất cả loại ưu đãi</option>
            <option value="PERCENT">Phần trăm (%)</option>
            <option value="FIXED">Số tiền cố định (₫)</option>
          </select>
        </div>
      </div>

      {/* Table Data */}
      <div className="bg-white rounded-3xl border border-zinc-200/80 shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="py-20 flex flex-col items-center justify-center text-center">
            <LoadingSpinner size="md" />
            <p className="mt-3 text-xs font-semibold text-zinc-500 font-sans">
              Đang tải danh sách mã giảm giá từ hệ thống...
            </p>
          </div>
        ) : error ? (
          <div className="py-16 px-4 flex flex-col items-center justify-center text-center">
            <AlertCircle className="w-10 h-10 text-red-500 mb-2" />
            <p className="text-xs font-bold text-red-600 max-w-md font-sans">{error}</p>
            <button
              onClick={fetchVouchers}
              className="mt-3 px-4 py-2 rounded-xl bg-zinc-900 text-white text-xs font-bold hover:bg-zinc-800 transition-colors cursor-pointer"
            >
              Thử lại
            </button>
          </div>
        ) : filteredVouchers.length === 0 ? (
          <div className="py-16 px-4 flex flex-col items-center justify-center text-center font-sans">
            <Ticket className="w-12 h-12 text-zinc-300 mb-2" />
            <h3 className="text-sm font-bold text-zinc-800">Không tìm thấy mã giảm giá nào</h3>
            <p className="text-xs text-zinc-400 mt-1 max-w-sm">
              {searchTerm || statusFilter !== 'ALL' || typeFilter !== 'ALL'
                ? 'Không có voucher nào phù hợp với bộ lọc hiện tại. Vui lòng thử tìm kiếm khác.'
                : 'Cửa hàng chưa có voucher nào. Hãy bấm "Thêm voucher mới" để bắt đầu!'}
            </p>
            {(searchTerm || statusFilter !== 'ALL' || typeFilter !== 'ALL') && (
              <button
                onClick={() => {
                  setSearchTerm('')
                  setStatusFilter('ALL')
                  setTypeFilter('ALL')
                }}
                className="mt-3 px-3.5 py-1.5 rounded-xl border border-zinc-200 text-xs font-semibold text-zinc-600 hover:bg-zinc-50 cursor-pointer"
              >
                Xóa bộ lọc
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-100 bg-zinc-50/60 text-[11px] font-black text-zinc-500 uppercase tracking-wider font-sans">
                  <th className="py-3 px-4 sm:px-6">Mã Voucher</th>
                  <th className="py-3 px-4">Mức giảm & Loại</th>
                  <th className="py-3 px-4">Đơn tối thiểu</th>
                  <th className="py-3 px-4">Lượt sử dụng</th>
                  <th className="py-3 px-4">Hạn sử dụng</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4 sm:px-6 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 text-xs">
                {filteredVouchers.map((voucher) => {
                  const expired = isVoucherExpired(voucher)
                  const maxed = isVoucherMaxed(voucher)

                  return (
                    <tr
                      key={voucher.id}
                      className="hover:bg-zinc-50/70 transition-colors group"
                    >
                      {/* Mã Voucher */}
                      <td className="py-4 px-4 sm:px-6 font-mono">
                        <div className="flex items-center gap-2">
                          <span className="px-2.5 py-1 rounded-lg bg-zinc-900 text-white font-bold text-xs tracking-wider shadow-2xs group-hover:bg-amber-500 transition-colors">
                            {voucher.code}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleCopyCode(voucher.code)}
                            title="Sao chép mã"
                            className="p-1 rounded-md text-zinc-400 hover:text-zinc-700 hover:bg-zinc-200/60 transition-colors cursor-pointer"
                          >
                            {copiedCode === voucher.code ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </div>
                      </td>

                      {/* Mức giảm & Loại */}
                      <td className="py-4 px-4">
                        <div className="space-y-0.5">
                          <div className="font-extrabold text-zinc-900 flex items-center gap-1.5 font-sans">
                            {voucher.discountType === 'PERCENT' ? (
                              <>
                                <span className="text-amber-600">Giảm {voucher.discountValue}%</span>
                                {voucher.maxDiscount && (
                                  <span className="text-[11px] text-zinc-500 font-normal">
                                    (Tối đa {formatPrice(voucher.maxDiscount)})
                                  </span>
                                )}
                              </>
                            ) : (
                              <span className="text-emerald-600">
                                Giảm {formatPrice(voucher.discountValue)}
                              </span>
                            )}
                          </div>
                          <span className="text-[11px] text-zinc-400 block font-sans">
                            {voucher.discountType === 'PERCENT'
                              ? 'Chiết khấu theo phần trăm'
                              : 'Giảm trực tiếp vào tổng tiền'}
                          </span>
                        </div>
                      </td>

                      {/* Đơn tối thiểu */}
                      <td className="py-4 px-4 font-sans font-semibold text-zinc-700">
                        {Number(voucher.minOrderValue) > 0 ? (
                          formatPrice(voucher.minOrderValue)
                        ) : (
                          <span className="text-zinc-400 font-normal">Mọi đơn</span>
                        )}
                      </td>

                      {/* Lượt sử dụng */}
                      <td className="py-4 px-4 font-sans">
                        <div className="space-y-1 max-w-[130px]">
                          <div className="flex justify-between items-center text-[11px]">
                            <span className="font-mono font-bold text-zinc-900">
                              {voucher.usedCount || 0}
                            </span>
                            <span className="text-zinc-400">
                              / {voucher.usageLimit ? voucher.usageLimit : '∞'}
                            </span>
                          </div>
                          {voucher.usageLimit ? (
                            <div className="w-full bg-zinc-100 rounded-full h-1.5 overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all duration-300 ${
                                  maxed ? 'bg-red-500' : 'bg-emerald-500'
                                }`}
                                style={{
                                  width: `${Math.min(
                                    100,
                                    ((voucher.usedCount || 0) / voucher.usageLimit) * 100
                                  )}%`,
                                }}
                              />
                            </div>
                          ) : (
                            <span className="text-[10px] text-emerald-600 font-medium">
                              Không giới hạn
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Hạn sử dụng */}
                      <td className="py-4 px-4 font-sans">
                        <div className="space-y-0.5">
                          <span className="text-xs text-zinc-700 font-medium block">
                            {formatDateTime(voucher.expireAt)}
                          </span>
                          {expired ? (
                            <span className="inline-block px-2 py-0.2 rounded-full text-[10px] font-bold bg-red-100 text-red-700">
                              Đã hết hạn
                            </span>
                          ) : voucher.expireAt ? (
                            <span className="inline-block px-2 py-0.2 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">
                              Còn hiệu lực
                            </span>
                          ) : (
                            <span className="inline-block px-2 py-0.2 rounded-full text-[10px] font-bold bg-zinc-100 text-zinc-600">
                              Vĩnh viễn
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Toggle Active / Inactive */}
                      <td className="py-4 px-4 text-center font-sans">
                        <button
                          type="button"
                          onClick={() => handleToggleStatus(voucher)}
                          disabled={togglingId === voucher.id}
                          className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold transition-all cursor-pointer disabled:opacity-50"
                          title="Nhấp để bật/tắt kích hoạt"
                        >
                          {togglingId === voucher.id ? (
                            <LoadingSpinner size="sm" />
                          ) : voucher.active ? (
                            <span className="flex items-center gap-1 text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                              Hoạt động
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 text-zinc-600 bg-zinc-100 px-2 py-0.5 rounded-full border border-zinc-200">
                              <span className="w-1.5 h-1.5 rounded-full bg-zinc-400"></span>
                              Tạm tắt
                            </span>
                          )}
                        </button>
                      </td>

                      {/* Actions: Sửa & Xóa */}
                      <td className="py-4 px-4 sm:px-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => {
                              setEditingVoucher(voucher)
                              setIsModalOpen(true)
                            }}
                            className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition-colors cursor-pointer"
                            title="Chỉnh sửa mã"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingVoucher(voucher)}
                            className="p-1.5 rounded-lg text-zinc-400 hover:text-red-600 hover:bg-red-50 transition-colors cursor-pointer"
                            title="Xóa mã giảm giá"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
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
      </div>

      {/* Modal Thêm mới / Chỉnh sửa Voucher */}
      <VoucherModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false)
          setEditingVoucher(null)
        }}
        voucher={editingVoucher}
        onSuccess={(msg) => {
          showToast(msg)
          fetchVouchers()
        }}
        onError={(err) => showToast(err, 'error')}
      />

      {/* Modal Xác nhận Xóa Voucher */}
      {deletingVoucher && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl border border-zinc-200 shadow-2xl max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-200">
            <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto shadow-inner">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center space-y-1.5 font-sans">
              <h3 className="text-base font-black text-zinc-900">
                Xác nhận xóa mã giảm giá?
              </h3>
              <p className="text-xs text-zinc-500 leading-relaxed">
                Bạn có chắc chắn muốn xóa vĩnh viễn voucher{' '}
                <span className="font-mono font-bold text-red-600">
                  #{deletingVoucher.code}
                </span>{' '}
                khỏi hệ thống? Hành động này không thể hoàn tác.
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2 font-sans">
              <button
                type="button"
                onClick={() => setDeletingVoucher(null)}
                disabled={isDeleting}
                className="flex-1 py-2.5 rounded-xl border border-zinc-200 text-xs font-bold text-zinc-600 hover:bg-zinc-100 transition-colors cursor-pointer disabled:opacity-50"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 transition-all flex items-center justify-center gap-1.5 shadow-xs cursor-pointer disabled:opacity-50"
              >
                {isDeleting ? (
                  <>
                    <LoadingSpinner size="sm" />
                    <span>Đang xóa...</span>
                  </>
                ) : (
                  <span>Xóa vĩnh viễn</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminVouchersPage
