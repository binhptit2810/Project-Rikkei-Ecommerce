import React, { useState, useEffect } from 'react'
import {
  Ticket,
  X,
  AlertCircle,
  CheckCircle2,
  Percent,
  Coins,
} from 'lucide-react'
import couponService from '../../services/couponService'
import LoadingSpinner from '../common/LoadingSpinner'

/**
 * Modal Thêm mới & Chỉnh sửa Mã giảm giá (VoucherModal).
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
 *
 * @param {boolean} isOpen - Trạng thái hiển thị modal
 * @param {Function} onClose - Hàm đóng modal
 * @param {Object|null} voucher - Dữ liệu voucher cần sửa (null nếu thêm mới)
 * @param {Function} onSuccess - Callback khi thêm/sửa thành công
 * @param {Function} onError - Callback khi xảy ra lỗi
 */
export const VoucherModal = ({
  isOpen,
  onClose,
  voucher = null,
  onSuccess,
  onError,
}) => {
  const isEditing = Boolean(voucher && voucher.id)

  const [formData, setFormData] = useState({
    code: '',
    discountType: 'PERCENT',
    discountValue: '',
    minOrderValue: '0',
    maxDiscount: '',
    usageLimit: '',
    expireAt: '',
    active: true,
  })

  const [formErrors, setFormErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Khởi tạo hoặc reset form khi modal mở/đổi voucher
  useEffect(() => {
    if (isOpen) {
      setFormErrors({})
      if (voucher) {
        // Format expireAt sang định dạng input datetime-local (YYYY-MM-DDTHH:mm)
        let formattedExpireAt = ''
        if (voucher.expireAt) {
          const d = new Date(voucher.expireAt)
          if (!isNaN(d.getTime())) {
            const pad = (n) => String(n).padStart(2, '0')
            formattedExpireAt = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
          }
        }

        setFormData({
          code: voucher.code || '',
          discountType: voucher.discountType || 'PERCENT',
          discountValue: voucher.discountValue != null ? String(voucher.discountValue) : '',
          minOrderValue: voucher.minOrderValue != null ? String(voucher.minOrderValue) : '0',
          maxDiscount: voucher.maxDiscount != null ? String(voucher.maxDiscount) : '',
          usageLimit: voucher.usageLimit != null ? String(voucher.usageLimit) : '',
          expireAt: formattedExpireAt,
          active: voucher.active !== undefined ? Boolean(voucher.active) : true,
        })
      } else {
        // Mặc định cho tạo mới
        const defaultExpire = new Date()
        defaultExpire.setDate(defaultExpire.getDate() + 30)
        const pad = (n) => String(n).padStart(2, '0')
        const defaultExpireStr = `${defaultExpire.getFullYear()}-${pad(defaultExpire.getMonth() + 1)}-${pad(defaultExpire.getDate())}T23:59`

        setFormData({
          code: '',
          discountType: 'PERCENT',
          discountValue: '10',
          minOrderValue: '100000',
          maxDiscount: '50000',
          usageLimit: '100',
          expireAt: defaultExpireStr,
          active: true,
        })
      }
    }
  }, [isOpen, voucher])

  if (!isOpen) return null

  // Format số tiền VND hiển thị
  const formatVND = (val) => {
    const num = Number(val)
    if (isNaN(num)) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num)
  }

  // Validate form trước khi submit
  const validateForm = () => {
    const errors = {}

    const cleanCode = (formData.code || '').trim().toUpperCase()
    if (!cleanCode) {
      errors.code = 'Mã giảm giá không được để trống.'
    } else if (cleanCode.length < 3 || cleanCode.length > 50) {
      errors.code = 'Mã giảm giá phải từ 3 đến 50 ký tự.'
    } else if (!/^[A-Za-z0-9_-]+$/.test(cleanCode)) {
      errors.code = 'Mã chỉ được chứa chữ cái, chữ số, gạch ngang (-) hoặc gạch dưới (_).'
    }

    const discountVal = Number(formData.discountValue)
    if (formData.discountValue === '' || isNaN(discountVal) || discountVal <= 0) {
      errors.discountValue = 'Giá trị giảm giá phải lớn hơn 0.'
    } else if (formData.discountType === 'PERCENT' && discountVal > 100) {
      errors.discountValue = 'Tỷ lệ giảm phần trăm không được vượt quá 100%.'
    }

    if (formData.minOrderValue !== '') {
      const minVal = Number(formData.minOrderValue)
      if (isNaN(minVal) || minVal < 0) {
        errors.minOrderValue = 'Giá trị đơn tối thiểu không được âm.'
      }
    }

    if (formData.discountType === 'PERCENT' && formData.maxDiscount !== '') {
      const maxVal = Number(formData.maxDiscount)
      if (isNaN(maxVal) || maxVal < 0) {
        errors.maxDiscount = 'Mức giảm tối đa không được âm.'
      }
    }

    if (formData.usageLimit !== '') {
      const limitVal = Number(formData.usageLimit)
      if (isNaN(limitVal) || limitVal <= 0 || !Number.isInteger(limitVal)) {
        errors.usageLimit = 'Giới hạn số lần dùng phải là số nguyên dương.'
      }
    }

    if (formData.expireAt) {
      const expDate = new Date(formData.expireAt)
      if (isNaN(expDate.getTime())) {
        errors.expireAt = 'Thời hạn sử dụng không hợp lệ.'
      } else if (!isEditing && expDate.getTime() <= Date.now()) {
        errors.expireAt = 'Thời hạn sử dụng phải ở tương lai.'
      }
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!validateForm()) return

    setIsSubmitting(true)
    try {
      const payload = {
        code: formData.code.trim().toUpperCase(),
        discountType: formData.discountType,
        discountValue: Number(formData.discountValue),
        minOrderValue: formData.minOrderValue ? Number(formData.minOrderValue) : 0,
        maxDiscount:
          formData.discountType === 'PERCENT' && formData.maxDiscount
            ? Number(formData.maxDiscount)
            : null,
        usageLimit: formData.usageLimit ? Number(formData.usageLimit) : null,
        expireAt: formData.expireAt ? new Date(formData.expireAt).toISOString() : null,
        active: Boolean(formData.active),
      }

      if (isEditing) {
        await couponService.updateCoupon(voucher.id, payload)
      } else {
        await couponService.createCoupon(payload)
      }

      onSuccess(
        isEditing
          ? `Cập nhật mã giảm giá "${payload.code}" thành công!`
          : `Tạo mã giảm giá "${payload.code}" thành công!`
      )
      onClose()
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        (isEditing ? 'Lỗi khi cập nhật mã giảm giá.' : 'Lỗi khi tạo mã giảm giá.')
      setFormErrors((prev) => ({ ...prev, server: msg }))
      if (onError) onError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-zinc-950/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl border border-zinc-200 shadow-2xl max-w-2xl w-full p-6 sm:p-7 max-h-[92vh] overflow-y-auto space-y-6 animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-zinc-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-zinc-900 text-white flex items-center justify-center shadow-xs">
              <Ticket className="w-5 h-5 text-amber-400" />
            </div>
            <div>
              <h2 className="text-lg font-black text-zinc-900 tracking-tight">
                {isEditing ? `Chỉnh sửa mã #${voucher.code}` : 'Tạo mới mã giảm giá (Voucher)'}
              </h2>
              <p className="text-xs text-zinc-500">
                {isEditing
                  ? 'Cập nhật điều kiện và mức ưu đãi của mã giảm giá'
                  : 'Thiết lập mã code, tỷ lệ chiết khấu và điều kiện áp dụng'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-zinc-400 hover:text-zinc-700 hover:bg-zinc-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Server Error Banner */}
        {formErrors.server && (
          <div className="p-3.5 bg-red-50 border border-red-200 rounded-2xl flex items-start gap-2.5 text-xs text-red-700 animate-in fade-in">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            <span>{formErrors.server}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Mã Voucher + Loại chiết khấu */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                Mã giảm giá (Code) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                placeholder="VD: WELCOME10, GIAM50K..."
                value={formData.code}
                onChange={(e) => {
                  setFormData((prev) => ({
                    ...prev,
                    code: e.target.value.toUpperCase().replace(/\s+/g, ''),
                  }))
                  if (formErrors.code) setFormErrors((prev) => ({ ...prev, code: null }))
                }}
                className={`w-full px-3.5 py-2.5 rounded-xl border text-sm font-mono font-bold tracking-wider uppercase transition-all outline-hidden ${
                  formErrors.code
                    ? 'border-red-300 bg-red-50/50 text-red-900 focus:ring-2 focus:ring-red-200'
                    : 'border-zinc-200 bg-zinc-50/50 text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10'
                }`}
              />
              {formErrors.code && (
                <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.code}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                Loại giảm giá <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setFormData((prev) => ({ ...prev, discountType: 'PERCENT' }))}
                  className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                    formData.discountType === 'PERCENT'
                      ? 'border-zinc-900 bg-zinc-900 text-white shadow-xs'
                      : 'border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50'
                  }`}
                >
                  <Percent className="w-3.5 h-3.5" />
                  <span>Phần trăm (%)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setFormData((prev) => ({ ...prev, discountType: 'FIXED' }))}
                  className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                    formData.discountType === 'FIXED'
                      ? 'border-zinc-900 bg-zinc-900 text-white shadow-xs'
                      : 'border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50'
                  }`}
                >
                  <Coins className="w-3.5 h-3.5" />
                  <span>Cố định (₫)</span>
                </button>
              </div>
            </div>
          </div>

          {/* Mức giảm & Giảm tối đa */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                {formData.discountType === 'PERCENT'
                  ? 'Mức giảm (%)'
                  : 'Số tiền giảm (₫)'}{' '}
                <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <input
                  type="number"
                  min="0"
                  step={formData.discountType === 'PERCENT' ? '1' : '1000'}
                  placeholder={formData.discountType === 'PERCENT' ? 'VD: 10, 15, 20...' : 'VD: 30000, 50000...'}
                  value={formData.discountValue}
                  onChange={(e) => {
                    setFormData((prev) => ({ ...prev, discountValue: e.target.value }))
                    if (formErrors.discountValue) setFormErrors((prev) => ({ ...prev, discountValue: null }))
                  }}
                  className={`w-full px-3.5 py-2.5 rounded-xl border text-sm font-semibold transition-all outline-hidden ${
                    formErrors.discountValue
                      ? 'border-red-300 bg-red-50/50 text-red-900 focus:ring-2 focus:ring-red-200'
                      : 'border-zinc-200 bg-zinc-50/50 text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10'
                  }`}
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-zinc-400">
                  {formData.discountType === 'PERCENT' ? '%' : '₫'}
                </span>
              </div>
              {formErrors.discountValue && (
                <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.discountValue}</p>
              )}
            </div>

            {formData.discountType === 'PERCENT' ? (
              <div>
                <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                  Giảm tối đa (₫) <span className="text-zinc-400 font-normal lowercase">(để trống nếu không giới hạn)</span>
                </label>
                <div className="relative">
                  <input
                    type="number"
                    min="0"
                    step="1000"
                    placeholder="VD: 50000, 100000..."
                    value={formData.maxDiscount}
                    onChange={(e) => setFormData((prev) => ({ ...prev, maxDiscount: e.target.value }))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-zinc-50/50 text-sm font-semibold text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10 transition-all outline-hidden"
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-zinc-400">
                    ₫
                  </span>
                </div>
                {formErrors.maxDiscount && (
                  <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.maxDiscount}</p>
                )}
              </div>
            ) : (
              <div>
                <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                  Hình thức áp dụng
                </label>
                <div className="px-3.5 py-2.5 rounded-xl border border-dashed border-zinc-200 bg-zinc-50 text-xs text-zinc-500 font-medium">
                  Trừ thẳng {formatVND(formData.discountValue || 0)} vào tổng tiền thanh toán của đơn hàng.
                </div>
              </div>
            )}
          </div>

          {/* Đơn tối thiểu & Giới hạn lượt dùng */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                Đơn hàng tối thiểu (₫)
              </label>
              <div className="relative">
                <input
                  type="number"
                  min="0"
                  step="1000"
                  placeholder="0 (áp dụng cho mọi đơn)"
                  value={formData.minOrderValue}
                  onChange={(e) => setFormData((prev) => ({ ...prev, minOrderValue: e.target.value }))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-zinc-50/50 text-sm font-semibold text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10 transition-all outline-hidden"
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-zinc-400">
                  ₫
                </span>
              </div>
              {formErrors.minOrderValue && (
                <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.minOrderValue}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                Giới hạn lượt dùng <span className="text-zinc-400 font-normal lowercase">(để trống = vô hạn)</span>
              </label>
              <input
                type="number"
                min="1"
                step="1"
                placeholder="VD: 50, 100, 500..."
                value={formData.usageLimit}
                onChange={(e) => setFormData((prev) => ({ ...prev, usageLimit: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-zinc-50/50 text-sm font-semibold text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10 transition-all outline-hidden"
              />
              {formErrors.usageLimit && (
                <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.usageLimit}</p>
              )}
            </div>
          </div>

          {/* Thời hạn sử dụng & Trạng thái kích hoạt */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-zinc-700 mb-1.5 uppercase tracking-wider">
                Hạn sử dụng <span className="text-zinc-400 font-normal lowercase">(để trống = không hết hạn)</span>
              </label>
              <input
                type="datetime-local"
                value={formData.expireAt}
                onChange={(e) => setFormData((prev) => ({ ...prev, expireAt: e.target.value }))}
                className="w-full px-3.5 py-2.5 rounded-xl border border-zinc-200 bg-zinc-50/50 text-xs font-semibold text-zinc-900 focus:bg-white focus:border-zinc-900 focus:ring-2 focus:ring-zinc-900/10 transition-all outline-hidden"
              />
              {formErrors.expireAt && (
                <p className="mt-1 text-xs text-red-600 font-medium">{formErrors.expireAt}</p>
              )}
            </div>

            <div className="flex items-center gap-3 pt-6">
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.active}
                  onChange={(e) => setFormData((prev) => ({ ...prev, active: e.target.checked }))}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-zinc-200 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-zinc-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600"></div>
                <span className="ml-3 text-xs font-bold text-zinc-800 select-none">
                  {formData.active ? 'Kích hoạt ngay' : 'Tạm ngưng áp dụng'}
                </span>
              </label>
            </div>
          </div>

          {/* Live Preview Card */}
          <div className="pt-2">
            <span className="text-[11px] font-bold text-zinc-400 uppercase tracking-wider block mb-2">
              Xem trước hiển thị voucher cho khách hàng:
            </span>
            <div className="bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent border border-dashed border-amber-300/80 rounded-2xl p-4 flex items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-md bg-amber-500 text-white font-mono font-black text-xs tracking-wider shadow-2xs">
                    {formData.code.trim().toUpperCase() || 'MA_GIAM_GIA'}
                  </span>
                  <span className="text-xs font-bold text-zinc-900">
                    {formData.discountType === 'PERCENT'
                      ? `Giảm ${formData.discountValue || 0}%${
                          formData.maxDiscount ? ` (Tối đa ${formatVND(formData.maxDiscount)})` : ''
                        }`
                      : `Giảm ${formatVND(formData.discountValue || 0)}`}
                  </span>
                </div>
                <p className="text-[11px] text-zinc-500">
                  {Number(formData.minOrderValue) > 0
                    ? `Đơn tối thiểu ${formatVND(formData.minOrderValue)}`
                    : 'Áp dụng cho mọi giá trị đơn hàng'}
                  {formData.usageLimit ? ` • Giới hạn ${formData.usageLimit} lượt` : ''}
                </p>
              </div>
              <div className="shrink-0 text-right">
                <span
                  className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold ${
                    formData.active
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-zinc-200 text-zinc-600'
                  }`}
                >
                  {formData.active ? 'Đang bật' : 'Tạm tắt'}
                </span>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-zinc-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl border border-zinc-200 text-xs font-bold text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 transition-colors cursor-pointer disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-xl text-xs font-bold text-white bg-zinc-900 hover:bg-zinc-800 active:scale-[0.98] transition-all flex items-center gap-2 shadow-xs cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span>{isEditing ? 'Lưu thay đổi' : 'Tạo mã voucher'}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default VoucherModal

