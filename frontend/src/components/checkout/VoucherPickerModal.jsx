import React, { useState, useEffect, useMemo, useCallback } from 'react'
import {
  X,
  Ticket,
  Check,
  AlertCircle,
  Clock,
  Sparkles,
  RefreshCw,
  Info
} from 'lucide-react'
import couponService from '../../services/couponService'
import LoadingSpinner from '../common/LoadingSpinner'

// Định dạng tiền tệ VND
const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(val || 0)
}

// Định dạng ngày hết hạn dd/MM/yyyy
const formatExpiryDate = (isoString) => {
  if (!isoString) return 'Không thời hạn'
  try {
    const d = new Date(isoString)
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  } catch (_e) {
    return 'Không thời hạn'
  }
}

/**
 * Thẻ Voucher chuẩn phong cách Shopee (Shopee Ticket Card)
 */
const VoucherCard = ({ coupon, orderValue, isSelected, onSelect }) => {
  const isPercent = coupon.discountType === 'PERCENT'
  const discountVal = Number(coupon.discountValue) || 0
  const minOrder = Number(coupon.minOrderValue) || 0
  const maxDiscount = coupon.maxDiscount ? Number(coupon.maxDiscount) : null

  // Kiểm tra tính hợp lệ
  const isExpired = coupon.expireAt && new Date(coupon.expireAt).getTime() < Date.now()
  const isLimitReached =
    coupon.usageLimit !== null &&
    coupon.usageLimit !== undefined &&
    Number(coupon.usedCount || 0) >= Number(coupon.usageLimit)
  const isMinOrderMet = orderValue >= minOrder
  const shortfall = minOrder - orderValue

  const canApply = !isExpired && !isLimitReached && isMinOrderMet

  // Ước tính số tiền giảm
  const estimatedDiscount = useMemo(() => {
    if (isPercent) {
      let est = (orderValue * discountVal) / 100
      if (maxDiscount) est = Math.min(est, maxDiscount)
      return est
    }
    return Math.min(orderValue, discountVal)
  }, [isPercent, orderValue, discountVal, maxDiscount])

  // Lý do không đủ điều kiện
  const ineligibleReason = useMemo(() => {
    if (isExpired) return 'Mã giảm giá đã hết hạn sử dụng'
    if (isLimitReached) return 'Mã đã hết lượt sử dụng'
    if (!isMinOrderMet) return `Mua thêm ${formatCurrency(shortfall)} để sử dụng mã này`
    return null
  }, [isExpired, isLimitReached, isMinOrderMet, shortfall])

  return (
    <div
      onClick={() => canApply && onSelect(coupon)}
      className={`relative flex rounded-2xl border transition-all duration-200 overflow-hidden shadow-2xs ${
        canApply
          ? isSelected
            ? 'border-orange-500 bg-orange-50/30 ring-2 ring-orange-400/40 shadow-sm cursor-pointer'
            : 'border-slate-200 bg-white hover:border-orange-300 hover:shadow-xs cursor-pointer'
          : 'border-slate-200/80 bg-slate-50/70 opacity-60 cursor-not-allowed'
      }`}
    >
      {/* 1. Cuống vé bên trái (Shopee Stub) */}
      <div
        className={`w-24 sm:w-28 shrink-0 flex flex-col items-center justify-center p-3 text-center relative select-none ${
          canApply
            ? 'bg-gradient-to-br from-orange-500 via-amber-500 to-orange-600 text-white'
            : 'bg-slate-200 text-slate-500'
        }`}
      >
        <Ticket className="w-6 h-6 mb-1 opacity-90 stroke-[1.75]" />
        <span className="text-[10px] font-black uppercase tracking-wider">
          {isPercent ? 'GIẢM %' : 'GIẢM Đ'}
        </span>
        <span className="text-xs font-black tracking-tight mt-0.5">
          {isPercent ? `${discountVal}%` : formatCurrency(discountVal)}
        </span>

        {/* Răng cưa / Khuyết bán nguyệt Shopee ở mép trên & dưới */}
        <div className="absolute -top-2 -right-2 w-4 h-4 rounded-full bg-slate-50 border border-slate-200 z-10" />
        <div className="absolute -bottom-2 -right-2 w-4 h-4 rounded-full bg-slate-50 border border-slate-200 z-10" />

        {/* Đường viền đứt nét ngăn cách cuống vé */}
        <div className="absolute top-2 bottom-2 right-0 w-[1px] border-r-2 border-dashed border-white/50" />
      </div>

      {/* 2. Thân vé chứa thông tin chi tiết */}
      <div className="flex-1 p-3.5 sm:p-4 min-w-0 flex flex-col justify-between">
        <div>
          {/* Header mã: Code & Badge giảm */}
          <div className="flex items-center justify-between gap-2 mb-1">
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-extrabold text-slate-900 text-xs sm:text-sm tracking-wider uppercase">
                {coupon.code}
              </span>
              {canApply && (
                <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-700 text-[10px] font-black tracking-wide">
                  Ước tính giảm {formatCurrency(estimatedDiscount)}
                </span>
              )}
            </div>

            {/* Radio Button kiểu Shopee */}
            <div className="shrink-0 pl-1">
              {canApply ? (
                <div
                  className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all ${
                    isSelected
                      ? 'border-orange-500 bg-orange-500 text-white shadow-2xs'
                      : 'border-slate-300 bg-white hover:border-orange-400'
                  }`}
                >
                  {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                </div>
              ) : (
                <div className="w-5 h-5 rounded-full border-2 border-slate-200 bg-slate-100" />
              )}
            </div>
          </div>

          {/* Điều kiện áp dụng */}
          <p className="text-xs font-semibold text-slate-700 line-clamp-1">
            {isPercent
              ? `Giảm ${discountVal}%${maxDiscount ? ` (Tối đa ${formatCurrency(maxDiscount)})` : ''}`
              : `Giảm trực tiếp ${formatCurrency(discountVal)}`}
          </p>

          <p className="text-[11px] text-slate-500 mt-0.5">
            Đơn tối thiểu {formatCurrency(minOrder)}
          </p>
        </div>

        {/* Hạn sử dụng & Thông báo điều kiện */}
        <div className="mt-2.5 pt-2 border-t border-slate-100 flex items-center justify-between gap-2 text-[11px]">
          <div className="flex items-center gap-1 text-slate-400">
            <Clock className="w-3 h-3" />
            <span>HSD: {formatExpiryDate(coupon.expireAt)}</span>
          </div>

          {ineligibleReason && (
            <span className="text-[11px] font-bold text-rose-600 line-clamp-1 text-right">
              {ineligibleReason}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * Modal / Bottom Sheet Chọn Voucher kiểu Shopee
 */
export const VoucherPickerModal = ({
  isOpen,
  onClose,
  onSelectVoucher,
  currentCode,
  orderValue = 0,
}) => {
  const [coupons, setCoupons] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // State nhập mã thủ công trong modal
  const [inputCode, setInputCode] = useState('')
  const [validatingManual, setValidatingManual] = useState(false)
  const [manualError, setManualError] = useState(null)

  // Voucher đang được chọn tạm thời trong modal
  const [selectedCoupon, setSelectedCoupon] = useState(null)
  const [isApplyingSelected, setIsApplyingSelected] = useState(false)

  // Tải danh sách voucher hoạt động từ database
  const fetchActiveCoupons = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await couponService.getActiveCoupons()
      const list = Array.isArray(res.data) ? res.data : Array.isArray(res) ? res : []
      setCoupons(list)
    } catch (err) {
      console.error('Không thể tải danh sách voucher:', err)
      setError('Không thể tải danh sách mã giảm giá. Vui lòng thử lại sau.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isOpen) {
      fetchActiveCoupons()
      setInputCode('')
      setManualError(null)
    }
  }, [isOpen, fetchActiveCoupons])

  // Cập nhật selectedCoupon khi danh sách hoặc currentCode thay đổi
  useEffect(() => {
    if (currentCode && coupons.length > 0) {
      const matched = coupons.find(
        (c) => c.code && c.code.toUpperCase() === currentCode.toUpperCase()
      )
      if (matched) {
        setSelectedCoupon(matched)
      }
    }
  }, [currentCode, coupons])

  // Phân nhóm voucher: Thỏa mãn điều kiện vs Chưa đủ điều kiện
  const { eligibleCoupons, ineligibleCoupons } = useMemo(() => {
    const eligible = []
    const ineligible = []

    coupons.forEach((coupon) => {
      const isExpired = coupon.expireAt && new Date(coupon.expireAt).getTime() < Date.now()
      const isLimitReached =
        coupon.usageLimit !== null &&
        coupon.usageLimit !== undefined &&
        Number(coupon.usedCount || 0) >= Number(coupon.usageLimit)
      const isMinOrderMet = orderValue >= (Number(coupon.minOrderValue) || 0)

      if (!isExpired && !isLimitReached && isMinOrderMet) {
        eligible.push(coupon)
      } else {
        ineligible.push(coupon)
      }
    })

    // Sắp xếp voucher đủ điều kiện theo số tiền giảm dự kiến giảm dần
    eligible.sort((a, b) => {
      const getEst = (c) => {
        const val = Number(c.discountValue) || 0
        if (c.discountType === 'PERCENT') {
          let est = (orderValue * val) / 100
          if (c.maxDiscount) est = Math.min(est, Number(c.maxDiscount))
          return est
        }
        return Math.min(orderValue, val)
      }
      return getEst(b) - getEst(a)
    })

    return { eligibleCoupons: eligible, ineligibleCoupons: ineligible }
  }, [coupons, orderValue])

  // Xử lý áp dụng mã nhập tay
  const handleApplyManualCode = async (e) => {
    e?.preventDefault()
    if (!inputCode.trim()) {
      setManualError('Vui lòng nhập mã voucher')
      return
    }

    setValidatingManual(true)
    setManualError(null)
    try {
      const res = await couponService.validateCoupon(inputCode.trim(), orderValue)
      const data = res?.data || res
      if (data && data.valid) {
        onSelectVoucher(data)
        onClose()
      } else {
        setManualError(data?.message || 'Mã giảm giá không hợp lệ')
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Mã giảm giá không tồn tại hoặc không hợp lệ'
      setManualError(msg)
    } finally {
      setValidatingManual(false)
    }
  }

  // Xử lý xác nhận voucher được chọn từ danh sách
  const handleConfirmSelected = async () => {
    if (!selectedCoupon) {
      onClose()
      return
    }

    setIsApplyingSelected(true)
    try {
      const res = await couponService.validateCoupon(selectedCoupon.code, orderValue)
      const data = res?.data || res
      if (data && data.valid) {
        onSelectVoucher(data)
        onClose()
      } else {
        alert(data?.message || 'Không thể áp dụng voucher này')
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Không thể áp dụng voucher này'
      alert(msg)
    } finally {
      setIsApplyingSelected(false)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
      />

      {/* Modal Container: Bottom Sheet trên Mobile, Modal giữa màn hình trên Desktop */}
      <div className="flex min-h-full items-end sm:items-center justify-center p-0 sm:p-4 text-center">
        <div className="relative w-full sm:max-w-xl bg-white rounded-t-3xl sm:rounded-3xl text-left shadow-2xl transition-all max-h-[90vh] sm:max-h-[85vh] flex flex-col overflow-hidden">
          {/* Header */}
          <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between shrink-0 bg-slate-50/60">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
                <Ticket className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-base font-extrabold text-slate-900">
                  Rikkei Store Voucher
                </h3>
                <p className="text-[11px] text-slate-500">
                  Chọn 1 voucher phù hợp cho đơn hàng của bạn
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Ô nhập mã voucher thủ công */}
          <div className="p-4 sm:p-5 border-b border-slate-100 bg-white shrink-0">
            <form onSubmit={handleApplyManualCode} className="space-y-1.5">
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type="text"
                    value={inputCode}
                    onChange={(e) => {
                      setInputCode(e.target.value.toUpperCase())
                      if (manualError) setManualError(null)
                    }}
                    placeholder="Nhập mã voucher (VD: WELCOME10)"
                    className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50/80 px-3.5 text-xs uppercase font-bold tracking-wider placeholder:normal-case placeholder:font-normal placeholder:text-slate-400 focus:outline-none focus:border-orange-500 focus:bg-white transition-all"
                  />
                  {inputCode && (
                    <button
                      type="button"
                      onClick={() => setInputCode('')}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={validatingManual || !inputCode.trim()}
                  className="px-4 py-2 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 active:scale-95 disabled:opacity-40 text-white font-bold text-xs rounded-xl shadow-xs transition-all shrink-0 cursor-pointer flex items-center gap-1.5"
                >
                  {validatingManual ? <LoadingSpinner size="xs" className="text-white" /> : 'Áp dụng'}
                </button>
              </div>

              {manualError && (
                <p className="text-[11px] text-red-600 font-medium flex items-center gap-1 pt-0.5">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{manualError}</span>
                </p>
              )}
            </form>
          </div>

          {/* Danh sách voucher cuộn */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4">
            {loading ? (
              <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-3">
                <LoadingSpinner size="md" className="text-orange-500" />
                <span className="text-xs font-medium">Đang tải danh sách voucher từ hệ thống...</span>
              </div>
            ) : error ? (
              <div className="p-6 text-center rounded-2xl bg-rose-50 border border-rose-200 text-rose-700">
                <AlertCircle className="w-7 h-7 mx-auto mb-2 text-rose-500" />
                <p className="text-xs font-semibold mb-3">{error}</p>
                <button
                  type="button"
                  onClick={fetchActiveCoupons}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 transition-colors shadow-xs"
                >
                  <RefreshCw className="w-3 h-3" />
                  <span>Thử lại</span>
                </button>
              </div>
            ) : coupons.length === 0 ? (
              <div className="py-12 text-center text-slate-400">
                <Ticket className="w-10 h-10 mx-auto mb-2 opacity-30 stroke-[1.25]" />
                <p className="text-xs font-semibold text-slate-600">
                  Hiện chưa có mã giảm giá nào đang áp dụng.
                </p>
                <p className="text-[11px] text-slate-400 mt-1">
                  Hãy theo dõi các chương trình ưu đãi sắp tới tại Rikkei Store!
                </p>
              </div>
            ) : (
              <>
                {/* 1. Nhóm Voucher đủ điều kiện áp dụng */}
                {eligibleCoupons.length > 0 && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-extrabold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-orange-500 fill-orange-400" />
                        <span>Mã giảm giá có thể dùng ({eligibleCoupons.length})</span>
                      </span>
                    </div>

                    <div className="space-y-2.5">
                      {eligibleCoupons.map((coupon) => (
                        <VoucherCard
                          key={coupon.id || coupon.code}
                          coupon={coupon}
                          orderValue={orderValue}
                          isSelected={selectedCoupon?.code === coupon.code}
                          onSelect={(c) => {
                            if (selectedCoupon?.code === c.code) {
                              setSelectedCoupon(null)
                            } else {
                              setSelectedCoupon(c)
                            }
                          }}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* 2. Nhóm Voucher chưa đủ điều kiện */}
                {ineligibleCoupons.length > 0 && (
                  <div className="space-y-3 pt-2">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                      <Info className="w-3.5 h-3.5 text-slate-400" />
                      <span>Chưa đủ điều kiện ({ineligibleCoupons.length})</span>
                    </span>

                    <div className="space-y-2.5">
                      {ineligibleCoupons.map((coupon) => (
                        <VoucherCard
                          key={coupon.id || coupon.code}
                          coupon={coupon}
                          orderValue={orderValue}
                          isSelected={false}
                          onSelect={() => {}}
                        />
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Footer nút hành động */}
          <div className="px-5 py-3.5 border-t border-slate-100 bg-slate-50/80 flex items-center justify-between gap-3 shrink-0">
            <div className="text-xs text-slate-600 min-w-0 flex-1">
              {selectedCoupon ? (
                <div className="flex items-center gap-1.5">
                  <span className="text-slate-500">Đã chọn:</span>
                  <span className="font-extrabold text-orange-600 uppercase tracking-wider">
                    {selectedCoupon.code}
                  </span>
                </div>
              ) : (
                <span className="text-slate-400">Chưa chọn mã nào</span>
              )}
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-200/70 transition-colors cursor-pointer"
              >
                Trở lại
              </button>

              <button
                type="button"
                onClick={handleConfirmSelected}
                disabled={isApplyingSelected || !selectedCoupon}
                className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 disabled:opacity-40 transition-all shadow-xs cursor-pointer flex items-center gap-1.5"
              >
                {isApplyingSelected ? <LoadingSpinner size="xs" className="text-white" /> : 'Đồng ý'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default VoucherPickerModal

