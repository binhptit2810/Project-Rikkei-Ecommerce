import React, { useState, useEffect } from 'react'
import {
  RefreshCw,
  Search,
  CheckCircle2,
  AlertCircle,
  ClipboardList,
  Filter,
} from 'lucide-react'
import orderService from '../../services/orderService'

export const AdminOrdersPage = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filterStatus, setFilterStatus] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [updatingId, setUpdatingId] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const fetchOrders = async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await orderService.getAdminOrders()
      if (res.success) {
        setOrders(res.data || [])
      } else {
        setError(res.message || 'Không thể tải danh sách đơn hàng')
      }
    } catch (err) {
      console.error('Error fetching admin orders:', err)
      setError(err.response?.data?.message || 'Lỗi khi tải danh sách đơn hàng')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrders()
  }, [])

  const handleStatusChange = async (orderId, newStatus) => {
    setUpdatingId(orderId)
    setSuccessMessage(null)
    try {
      const res = await orderService.updateAdminOrderStatus(orderId, newStatus)
      if (res.success) {
        setOrders((prev) =>
          prev.map((o) => (o.id === orderId ? res.data : o))
        )
        setSuccessMessage(`Đã cập nhật đơn hàng #${orderId} sang trạng thái "${newStatus}"`)
        setTimeout(() => setSuccessMessage(null), 3000)
      } else {
        alert(res.message || 'Cập nhật thất bại')
      }
    } catch (err) {
      console.error('Update status error:', err)
      alert(err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật trạng thái')
    } finally {
      setUpdatingId(null)
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    return d.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  }

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">Chờ xác nhận</span>
      case 'CONFIRMED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-800 border border-blue-200">Đã xác nhận</span>
      case 'PROCESSING':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-800 border border-indigo-200">Đang chuẩn bị</span>
      case 'SHIPPING':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-50 text-purple-800 border border-purple-200">Đang giao</span>
      case 'COMPLETED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">Hoàn thành</span>
      case 'CANCELLED':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">Đã hủy</span>
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">{status}</span>
    }
  }

  const filteredOrders = orders.filter((order) => {
    const matchesStatus = filterStatus === 'ALL' || order.orderStatus === filterStatus
    const matchesSearch =
      searchTerm === '' ||
      order.orderCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.recipientName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.userEmail?.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesStatus && matchesSearch
  })

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Quản lý Đơn hàng</h1>
          <p className="text-slate-500 text-xs sm:text-sm mt-0.5">
            Theo dõi, cập nhật tiến trình và xử lý tất cả các đơn hàng trong hệ thống.
          </p>
        </div>
        <button
          onClick={fetchOrders}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl shadow-2xs self-start sm:self-auto cursor-pointer transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs sm:text-sm flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button onClick={() => setSuccessMessage(null)} className="font-bold text-xs text-emerald-700 hover:text-emerald-950">
            Đóng
          </button>
        </div>
      )}

      {/* Search & Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="w-full md:w-80 relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo mã đơn, người nhận, email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
          />
        </div>

        <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-1 md:pb-0 scrollbar-none">
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'PENDING', label: 'Chờ xác nhận' },
            { id: 'CONFIRMED', label: 'Đã xác nhận' },
            { id: 'PROCESSING', label: 'Đang chuẩn bị' },
            { id: 'SHIPPING', label: 'Đang giao' },
            { id: 'COMPLETED', label: 'Hoàn thành' },
            { id: 'CANCELLED', label: 'Đã hủy' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setFilterStatus(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
                filterStatus === tab.id
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
        {loading ? (
          <div className="p-12 text-center">
            <div className="w-8 h-8 border-3 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
            <p className="text-slate-500 text-xs">Đang tải danh sách đơn hàng...</p>
          </div>
        ) : error ? (
          <div className="p-12 text-center text-red-600 text-xs flex items-center justify-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-500" />
            <span>{error}</span>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            Không tìm thấy đơn hàng nào phù hợp với bộ lọc.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[11px]">
                  <th className="py-3.5 px-4">Mã đơn</th>
                  <th className="py-3.5 px-4">Khách hàng</th>
                  <th className="py-3.5 px-4">Ngày đặt</th>
                  <th className="py-3.5 px-4">Sản phẩm</th>
                  <th className="py-3.5 px-4 text-right">Tổng tiền</th>
                  <th className="py-3.5 px-4">Thanh toán</th>
                  <th className="py-3.5 px-4">Trạng thái</th>
                  <th className="py-3.5 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-3.5 px-4 font-bold text-slate-900">
                      {order.orderCode}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-900">{order.recipientName}</div>
                      <div className="text-[11px] text-slate-500">{order.recipientPhone}</div>
                      <div className="text-[10px] text-slate-400 truncate max-w-[150px]">{order.userEmail}</div>
                    </td>
                    <td className="py-3.5 px-4 text-slate-500">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="font-medium text-slate-700">
                        {order.items?.length || 0} sản phẩm
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right font-extrabold text-orange-600 tabular-nums">
                      {formatPrice(order.totalAmount)}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-800">{order.paymentMethod}</div>
                      <div className="text-[10px] text-slate-500">
                        {order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'REFUNDED' ? 'Đã hoàn tiền' : 'Chưa thanh toán'}
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      {getStatusBadge(order.orderStatus)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <select
                        value={order.orderStatus}
                        disabled={updatingId === order.id}
                        onChange={(e) => handleStatusChange(order.id, e.target.value)}
                        className="px-2.5 py-1.5 rounded-xl border border-slate-300 bg-white font-semibold text-slate-700 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500/20 cursor-pointer shadow-2xs"
                      >
                        <option value="PENDING">Chờ xác nhận</option>
                        <option value="CONFIRMED">Đã xác nhận</option>
                        <option value="PROCESSING">Đang chuẩn bị</option>
                        <option value="SHIPPING">Đang giao hàng</option>
                        <option value="COMPLETED">Hoàn thành</option>
                        <option value="CANCELLED">Hủy đơn</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminOrdersPage
