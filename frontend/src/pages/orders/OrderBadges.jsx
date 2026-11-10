import React from 'react'
import {
  Clock,
  CheckCircle2,
  Package,
  Truck,
  XCircle,
  RotateCcw,
} from 'lucide-react'

export const FALLBACK_IMAGE =
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'

export const ORDER_STATUS_CONFIG = {
  PENDING: {
    label: 'Chờ xác nhận',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
    icon: Clock,
    step: 0,
  },
  CONFIRMED: {
    label: 'Đã xác nhận',
    badgeClass: 'bg-blue-50 text-blue-700 border-blue-200',
    icon: CheckCircle2,
    step: 1,
  },
  PROCESSING: {
    label: 'Đang xử lý',
    badgeClass: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    icon: Package,
    step: 2,
  },
  SHIPPING: {
    label: 'Đang giao hàng',
    badgeClass: 'bg-purple-50 text-purple-700 border-purple-200',
    icon: Truck,
    step: 3,
  },
  COMPLETED: {
    label: 'Hoàn thành',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    icon: CheckCircle2,
    step: 4,
  },
  CANCELLED: {
    label: 'Đã hủy',
    badgeClass: 'bg-red-50 text-red-700 border-red-200',
    icon: XCircle,
    step: -1,
  },
}

export const PAYMENT_STATUS_CONFIG = {
  PAID: {
    label: 'Đã thanh toán',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    icon: CheckCircle2,
  },
  UNPAID: {
    label: 'Chưa thanh toán',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
    icon: Clock,
  },
  FAILED: {
    label: 'Thanh toán lỗi',
    badgeClass: 'bg-red-50 text-red-700 border-red-200',
    icon: XCircle,
  },
  REFUNDED: {
    label: 'Đã hoàn tiền',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-200',
    icon: RotateCcw,
  },
}

export const formatPrice = (price) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(price || 0)
}

export const formatDateTime = (dateString) => {
  if (!dateString) return '—'
  try {
    const d = new Date(dateString)
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(d)
  } catch (err) {
    return dateString
  }
}

export const OrderStatusBadge = ({ status }) => {
  const config = ORDER_STATUS_CONFIG[status] || {
    label: status || 'Không rõ',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-200',
    icon: Clock,
  }
  const Icon = config.icon

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border shadow-2xs ${config.badgeClass}`}
    >
      <Icon className="w-3.5 h-3.5 shrink-0" />
      <span>{config.label}</span>
    </span>
  )
}

export const PaymentStatusBadge = ({ status, paymentMethod, description }) => {
  if (paymentMethod === 'COD' && status === 'UNPAID') {
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border shadow-2xs bg-emerald-50 text-emerald-700 border-emerald-200">
        <Truck className="w-3.5 h-3.5 shrink-0 text-emerald-600" />
        <span>Thu tiền khi nhận hàng (COD)</span>
      </span>
    )
  }

  const config = PAYMENT_STATUS_CONFIG[status] || {
    label: description || status || 'Chưa rõ',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-200',
    icon: Clock,
  }
  const Icon = config.icon

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border shadow-2xs ${config.badgeClass}`}
    >
      <Icon className="w-3.5 h-3.5 shrink-0" />
      <span>{description || config.label}</span>
    </span>
  )
}
