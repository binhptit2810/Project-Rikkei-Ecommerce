import React from 'react'
import { Link } from 'react-router-dom'
import {
  Package,
  Clock,
  Truck,
  CheckCircle2,
  XCircle,
  ExternalLink,
  MapPin,
  Calendar,
  CreditCard
} from 'lucide-react'
import { normalizeOrderStatus } from '../../services/chatOrderHelper'

export { normalizeOrderStatus }

/**
 * Component Inline Order Status Card hiển thị trực tiếp trong tin nhắn phản hồi của AI Chatbot.
 * Phụ trách: Đỗ Hoàng Anh (DHA) [TSK-D12-DHA]
 *
 * @param {Object} props
 * @param {Object} props.order Thông tin đơn hàng (real API hoặc simulated demo)
 */
export const ChatOrderStatusCard = ({ order }) => {
  if (!order) return null

  const statusMeta = normalizeOrderStatus(order.status || order.orderStatus || order.statusLabel)
  const StatusIcon = statusMeta.key === 'SHIPPING'
    ? Truck
    : statusMeta.key === 'DELIVERED'
    ? CheckCircle2
    : statusMeta.key === 'CANCELLED'
    ? XCircle
    : Clock

  const currentStep = statusMeta.step
  const isCancelled = currentStep === 0

  const formattedAmount = typeof order.totalAmount === 'number'
    ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.totalAmount)
    : order.totalAmount || '0 ₫'

  const formattedDate = (() => {
    try {
      const dt = order.updatedAt || order.createdAt
      if (!dt) return 'Vừa cập nhật'
      const date = new Date(dt)
      return date.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch (_e) {
      return 'Vừa cập nhật'
    }
  })()

  const orderCodeDisplay = order.orderCode || (order.id ? `ORD-${order.id}` : 'ORD-123')
  const detailLink = order.id ? `/orders/${order.id}` : '/orders'

  // Danh sách 3 bước tiến trình chuẩn theo Trello checklist
  const TRACKER_STEPS = [
    { step: 1, label: 'Đang xử lý', icon: Clock },
    { step: 2, label: 'Đang giao', icon: Truck },
    { step: 3, label: 'Đã giao', icon: CheckCircle2 },
  ]

  return (
    <div className="w-full mt-2.5 bg-white border border-slate-200 rounded-xl shadow-xs overflow-hidden text-slate-800 transition-all hover:border-indigo-300">
      {/* 1. Header Card: Mã đơn & Status Pill */}
      <div className="p-3 bg-gradient-to-r from-slate-50 to-indigo-50/40 border-b border-slate-100 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <div className="w-7 h-7 rounded-lg bg-indigo-600/10 text-indigo-600 flex items-center justify-center shrink-0">
            <Package className="w-4 h-4" />
          </div>
          <div className="truncate">
            <span className="text-[10px] uppercase font-semibold text-slate-400 block tracking-wider">
              Mã đơn hàng
            </span>
            <span className="font-bold text-xs text-slate-800 tracking-tight block truncate">
              #{orderCodeDisplay}
            </span>
          </div>
        </div>

        {/* Status Badge Pill */}
        <span
          className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold border ${statusMeta.badgeBg} ${statusMeta.badgeText} ${statusMeta.badgeBorder} shrink-0 shadow-2xs`}
        >
          <StatusIcon className="w-3.5 h-3.5" />
          <span>{statusMeta.label}</span>
        </span>
      </div>

      {/* 2. Visual 3-Step Progress Tracker */}
      <div className="px-3 pt-3 pb-2.5 bg-slate-50/60 border-b border-slate-100">
        <div className="text-[10px] font-medium text-slate-400 uppercase tracking-wider mb-2">
          Tiến trình đơn hàng
        </div>

        {isCancelled ? (
          <div className="flex items-center gap-2 p-2 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs">
            <XCircle className="w-4 h-4 shrink-0" />
            <span>Đơn hàng đã được hủy. Số tiền sẽ được xử lý hoàn trả theo chính sách.</span>
          </div>
        ) : (
          <div className="relative flex items-center justify-between">
            {/* Background connecting line */}
            <div className="absolute top-3.5 left-4 right-4 h-0.5 bg-slate-200 z-0" />
            {/* Active connecting line fill */}
            <div
              className="absolute top-3.5 left-4 h-0.5 bg-indigo-600 z-0 transition-all duration-500"
              style={{
                width: currentStep === 1 ? '0%' : currentStep === 2 ? '50%' : '100%',
              }}
            />

            {TRACKER_STEPS.map((s) => {
              const StepIcon = s.icon
              const isPast = currentStep > s.step
              const isCurrent = currentStep === s.step

              return (
                <div key={s.step} className="relative z-10 flex flex-col items-center flex-1">
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center transition-all ${
                      isPast
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : isCurrent
                        ? 'bg-indigo-600 text-white ring-4 ring-indigo-100 shadow-md scale-105'
                        : 'bg-slate-100 text-slate-400 border border-slate-200'
                    }`}
                  >
                    {isPast ? (
                      <CheckCircle2 className="w-3.5 h-3.5 stroke-[2.5]" />
                    ) : (
                      <StepIcon className="w-3.5 h-3.5" />
                    )}
                  </div>
                  <span
                    className={`text-[10px] mt-1.5 font-medium tracking-tight text-center ${
                      isCurrent
                        ? 'text-indigo-700 font-bold'
                        : isPast
                        ? 'text-emerald-700'
                        : 'text-slate-400'
                    }`}
                  >
                    {s.label}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* 3. Summary Info Rows */}
      <div className="p-3 space-y-1.5 text-xs">
        {/* Tổng tiền */}
        <div className="flex items-center justify-between text-slate-600">
          <span className="flex items-center gap-1.5 text-slate-500">
            <CreditCard className="w-3.5 h-3.5 text-slate-400" />
            <span>Tổng thanh toán:</span>
          </span>
          <span className="font-bold text-indigo-600">{formattedAmount}</span>
        </div>

        {/* Địa chỉ nhận / Người nhận */}
        {(order.recipientName || order.shippingAddress) && (
          <div className="flex items-start justify-between gap-2 text-slate-600">
            <span className="flex items-center gap-1.5 text-slate-500 shrink-0">
              <MapPin className="w-3.5 h-3.5 text-slate-400" />
              <span>Giao đến:</span>
            </span>
            <span className="text-right truncate max-w-[190px] text-[11px] text-slate-700">
              {order.recipientName ? `${order.recipientName} • ` : ''}
              {order.shippingAddress || 'Địa chỉ mặc định'}
            </span>
          </div>
        )}

        {/* Cập nhật lần cuối */}
        <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-100">
          <span className="flex items-center gap-1">
            <Calendar className="w-3 h-3" />
            <span>Thời gian cập nhật:</span>
          </span>
          <span>{formattedDate}</span>
        </div>
      </div>

      {/* 4. Action Button / Navigation Link */}
      <div className="p-2.5 pt-0">
        <Link
          to={detailLink}
          className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-semibold text-indigo-600 bg-indigo-50/80 hover:bg-indigo-600 hover:text-white rounded-lg transition-all duration-150 cursor-pointer group active:scale-[0.98]"
        >
          <span>Xem chi tiết đơn hàng</span>
          <ExternalLink className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
        </Link>
      </div>
    </div>
  )
}

export default ChatOrderStatusCard
