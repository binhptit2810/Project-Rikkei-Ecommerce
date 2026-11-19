import React, { useState, useEffect } from 'react'
import {
  MapPin,
  Phone,
  User,
  Plus,
  Trash2,
  CheckCircle2,
  X,
  Star,
  Check,
} from 'lucide-react'
import {
  getSavedAddresses,
  deleteAddress,
  setDefaultAddress,
} from '../../utils/addressStorage'

/**
 * Modal Chọn và Quản lý Sổ địa chỉ giao hàng đã lưu.
 */
export const SavedAddressModal = ({
  isOpen,
  onClose,
  onSelectAddress,
  userId,
  currentAddress = '',
}) => {
  const [addresses, setAddresses] = useState([])

  const loadAddresses = () => {
    const list = getSavedAddresses(userId)
    setAddresses(list)
  }

  useEffect(() => {
    if (isOpen) {
      loadAddresses()
    }
  }, [isOpen, userId])

  const handleDelete = (e, id) => {
    e.stopPropagation()
    if (window.confirm('Bạn có chắc chắn muốn xóa địa chỉ này khỏi sổ địa chỉ?')) {
      const res = deleteAddress(userId, id)
      if (res.success) {
        setAddresses(res.list)
      }
    }
  }

  const handleSetDefault = (e, id) => {
    e.stopPropagation()
    const res = setDefaultAddress(userId, id)
    if (res.success) {
      setAddresses(res.list)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-xl rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-slate-900 text-base">
                Sổ địa chỉ nhận hàng đã lưu
              </h3>
              <p className="text-xs text-slate-500">
                Chọn địa chỉ giao hàng nhanh cho đơn hàng của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-200/70 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Danh sách địa chỉ */}
        <div className="p-4 sm:p-5 overflow-y-auto divide-y divide-slate-100 space-y-3">
          {addresses.length === 0 ? (
            <div className="text-center py-10 text-slate-400 space-y-2">
              <MapPin className="w-10 h-10 mx-auto text-slate-300 stroke-1" />
              <p className="text-xs font-semibold text-slate-600">
                Chưa có địa chỉ nào được lưu trong sổ địa chỉ
              </p>
              <p className="text-[11px] text-slate-400 max-w-xs mx-auto">
                Khi bạn nhập thông tin đặt hàng và tích chọn "Lưu thông tin", địa chỉ sẽ tự động xuất hiện ở đây.
              </p>
            </div>
          ) : (
            addresses.map((addr) => {
              const isSelected =
                currentAddress &&
                addr.shippingAddress &&
                currentAddress.trim().toLowerCase() === addr.shippingAddress.trim().toLowerCase()

              return (
                <div
                  key={addr.id}
                  onClick={() => {
                    onSelectAddress(addr)
                    onClose()
                  }}
                  className={`pt-3 first:pt-0 p-3.5 rounded-2xl border transition-all cursor-pointer group ${
                    isSelected
                      ? 'border-orange-500 bg-orange-50/40 shadow-xs'
                      : 'border-slate-200/90 hover:border-orange-300 hover:bg-slate-50/70'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1.5 flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-black text-slate-900 text-sm">
                          {addr.recipientName || 'Người nhận'}
                        </span>
                        <span className="text-xs font-semibold text-slate-500">
                          ({addr.recipientPhone})
                        </span>
                        {addr.isDefault && (
                          <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-700 text-[10px] font-black uppercase tracking-wider">
                            Mặc định
                          </span>
                        )}
                        {isSelected && (
                          <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-black uppercase tracking-wider flex items-center gap-1">
                            <Check className="w-3 h-3" /> Đang chọn
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-slate-700 line-clamp-2 leading-relaxed flex items-start gap-1.5">
                        <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                        <span>{addr.shippingAddress}</span>
                      </p>
                    </div>

                    {/* Thao tác xóa hoặc đặt làm mặc định */}
                    <div className="flex items-center gap-1 shrink-0 pt-0.5">
                      {!addr.isDefault && (
                        <button
                          type="button"
                          onClick={(e) => handleSetDefault(e, addr.id)}
                          title="Đặt làm địa chỉ mặc định"
                          className="p-1.5 text-slate-400 hover:text-orange-600 rounded-lg hover:bg-orange-50 transition-colors cursor-pointer text-xs flex items-center gap-1"
                        >
                          <Star className="w-3.5 h-3.5" />
                          <span className="hidden sm:inline text-[11px]">Mặc định</span>
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={(e) => handleDelete(e, addr.id)}
                        title="Xóa địa chỉ này"
                        className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/70 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 text-xs font-bold transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  )
}

export default SavedAddressModal
