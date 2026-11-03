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
    badgeClass: 'bg-blue-50 text-blue-700 border-blue-200',
    icon: RotateCcw,
  },
}

export const formatPrice = (price) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(price || 0)
}

export const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

export const OrderStatusBadge = ({ status, description }) => {
  const config = ORDER_STATUS_CONFIG[status] || {
    label: description || status || 'Chưa rõ',
    badgeClass: 'bg-slate-50 text-slate-700 border-slate-200',
    icon: Clock,
  }
  const IconComponent = config.icon

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${config.badgeClass}`}
    >
      <IconComponent className="w-3.5 h-3.5" />
      <span>{description || config.label}</span>
    </span>
  )
}

export const PaymentStatusBadge = ({ status, description }) => {
  const config = PAYMENT_STATUS_CONFIG[status] || {
    label: description || status || 'Chưa thanh toán',
    badgeClass: 'bg-slate-50 text-slate-700 border-slate-200',
    icon: Clock,
  }
  const IconComponent = config.icon

  return (
    <span
      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold border ${config.badgeClass}`}
    >
      <IconComponent className="w-3 h-3" />
      <span>{description || config.label}</span>
    </span>
  )
}
