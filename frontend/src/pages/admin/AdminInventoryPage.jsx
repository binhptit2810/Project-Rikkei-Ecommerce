import React, { useState, useEffect, useCallback } from 'react'
import {
  Boxes,
  RefreshCw,
  Search,
  Plus,
  Minus,
  History,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  PackageCheck,
  PackageX,
  X,
  Layers,
  ArrowUpRight,
  ArrowDownLeft,
} from 'lucide-react'
import inventoryService from '../../services/inventoryService'
import categoryService from '../../services/categoryService'

export const AdminInventoryPage = () => {
  const [activeTab, setActiveTab] = useState('inventory') // 'inventory' | 'transactions'

  // Inventory Table States
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [stockFilter, setStockFilter] = useState('ALL') // 'ALL' | 'OUT_OF_STOCK' | 'LOW_STOCK' | 'IN_STOCK'
  const [categories, setCategories] = useState([])

  // Transactions State
  const [transactions, setTransactions] = useState([])
  const [loadingTx, setLoadingTx] = useState(false)

  // Adjust Modal State
  const [adjustModalProduct, setAdjustModalProduct] = useState(null)
  const [adjustType, setAdjustType] = useState('ADD') // 'ADD' | 'SUBTRACT'
  const [adjustQuantity, setAdjustQuantity] = useState('')
  const [adjustNote, setAdjustNote] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [adjustError, setAdjustError] = useState(null)

  // History Modal for a single product
  const [historyProduct, setHistoryProduct] = useState(null)
  const [productHistoryList, setProductHistoryList] = useState([])
  const [loadingHistory, setLoadingHistory] = useState(false)

  // Notification
  const [successMessage, setSuccessMessage] = useState(null)

  // Load categories
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const res = await categoryService.getAdminCategories()
        if (res.success && res.data) {
          setCategories(res.data)
        }
      } catch (err) {
        console.error('Không thể tải danh mục:', err)
      }
    }
    fetchCategories()
  }, [])

  // Load Inventory Data
  const fetchInventory = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        size: 100,
        sortBy: 'stockQuantity',
        sortDir: 'asc',
      }
      if (searchTerm.trim()) params.keyword = searchTerm.trim()
      if (selectedCategory) params.categoryId = selectedCategory

      const res = await inventoryService.getInventory(params)
      if (res.success && res.data) {
        setProducts(res.data.content || [])
      } else {
        setError(res.message || 'Không thể tải danh sách tồn kho')
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách tồn kho:', err)
      setError(err.response?.data?.message || 'Có lỗi xảy ra khi tải dữ liệu tồn kho')
    } finally {
      setLoading(false)
    }
  }, [searchTerm, selectedCategory])

  // Load All Transactions
  const fetchAllTransactions = useCallback(async () => {
    setLoadingTx(true)
    try {
      const res = await inventoryService.getAllTransactions()
      if (res.success && res.data) {
        setTransactions(res.data)
      }
    } catch (err) {
      console.error('Lỗi khi tải lịch sử giao dịch:', err)
    } finally {
      setLoadingTx(false)
    }
  }, [])

  useEffect(() => {
    if (activeTab === 'inventory') {
      fetchInventory()
    } else {
      fetchAllTransactions()
    }
  }, [activeTab, fetchInventory, fetchAllTransactions])

  // Open adjust modal
  const handleOpenAdjustModal = (product) => {
    setAdjustModalProduct(product)
    setAdjustType('ADD')
    setAdjustQuantity('')
    setAdjustNote('')
    setAdjustError(null)
  }

  // Open product transaction history
  const handleOpenProductHistory = async (product) => {
    setHistoryProduct(product)
    setLoadingHistory(true)
    try {
      const res = await inventoryService.getProductTransactions(product.id)
      if (res.success && res.data) {
        setProductHistoryList(res.data)
      }
    } catch (err) {
      console.error('Lỗi tải lịch sử sản phẩm:', err)
    } finally {
      setLoadingHistory(false)
    }
  }

  // Submit Stock Adjustment
  const handleAdjustSubmit = async (e) => {
    e.preventDefault()
    setAdjustError(null)

    const qty = parseInt(adjustQuantity, 10)
    if (isNaN(qty) || qty <= 0) {
      setAdjustError('Số lượng phải là một số nguyên dương lớn hơn 0.')
      return
    }

    const currentStock = adjustModalProduct.stockQuantity || 0
    const finalChange = adjustType === 'ADD' ? qty : -qty

    if (adjustType === 'SUBTRACT' && qty > currentStock) {
      setAdjustError(`Không thể xuất quá số lượng hiện có (${currentStock} sản phẩm).`)
      return
    }

    setIsSubmitting(true)
    try {
      const res = await inventoryService.adjustStock(adjustModalProduct.id, {
        changeQuantity: finalChange,
        note: adjustNote.trim() || undefined,
      })

      if (res.success) {
        setSuccessMessage(
          `Đã ${adjustType === 'ADD' ? 'nhập thêm' : 'xuất'} ${qty} sản phẩm cho "${adjustModalProduct.name}". Tồn kho mới: ${res.data.stockAfter}`
        )
        setTimeout(() => setSuccessMessage(null), 4000)
        setAdjustModalProduct(null)
        fetchInventory()
      } else {
        setAdjustError(res.message || 'Điều chỉnh thất bại')
      }
    } catch (err) {
      console.error('Lỗi khi điều chỉnh tồn kho:', err)
      setAdjustError(err.response?.data?.message || 'Có lỗi xảy ra khi gọi máy chủ')
    } finally {
      setIsSubmitting(false)
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

  // Filter products by stock status
  const filteredProducts = products.filter((p) => {
    const stock = p.stockQuantity || 0
    if (stockFilter === 'OUT_OF_STOCK') return stock === 0
    if (stockFilter === 'LOW_STOCK') return stock > 0 && stock <= 5
    if (stockFilter === 'IN_STOCK') return stock > 5
    return true
  })

  // Stock stats
  const outOfStockCount = products.filter((p) => (p.stockQuantity || 0) === 0).length
  const lowStockCount = products.filter((p) => (p.stockQuantity || 0) > 0 && (p.stockQuantity || 0) <= 5).length
  const totalStockSum = products.reduce((acc, p) => acc + (p.stockQuantity || 0), 0)

  const getTransactionBadge = (type) => {
    switch (type) {
      case 'ORDER_PLACED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <ArrowDownLeft className="w-3 h-3 text-rose-500" />
            <span>Xuất kho (Đặt hàng)</span>
          </span>
        )
      case 'ORDER_CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <ArrowUpRight className="w-3 h-3 text-emerald-500" />
            <span>Hoàn kho (Hủy đơn)</span>
          </span>
        )
      case 'MANUAL_ADJUST':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            <Layers className="w-3 h-3 text-amber-600" />
            <span>Điều chỉnh thủ công</span>
          </span>
        )
      case 'INITIAL_STOCK':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
            <Boxes className="w-3 h-3 text-blue-500" />
            <span>Nhập kho ban đầu</span>
          </span>
        )
      default:
        return <span className="px-2 py-0.5 rounded text-xs bg-slate-100 text-slate-700">{type}</span>
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Quản lý Tồn kho</h1>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-orange-100 text-orange-700 border border-orange-200">
              Real-time Inventory
            </span>
          </div>
          <p className="text-slate-500 text-xs sm:text-sm mt-0.5">
            Theo dõi số lượng hàng hóa, điều chỉnh xuất/nhập kho và giám sát lịch sử biến động.
          </p>
        </div>
        <button
          onClick={activeTab === 'inventory' ? fetchInventory : fetchAllTransactions}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl shadow-2xs self-start sm:self-auto cursor-pointer transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Làm mới</span>
        </button>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs flex items-center justify-between">
          <div>
            <div className="text-xs font-medium text-slate-500">Tổng sản phẩm trong kho</div>
            <div className="text-2xl font-black text-slate-900 mt-1 tabular-nums">{totalStockSum.toLocaleString('vi-VN')}</div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center text-orange-600">
            <Boxes className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs flex items-center justify-between">
          <div>
            <div className="text-xs font-medium text-slate-500">Sản phẩm sắp hết (&le; 5)</div>
            <div className="text-2xl font-black text-amber-600 mt-1 tabular-nums">{lowStockCount}</div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs flex items-center justify-between">
          <div>
            <div className="text-xs font-medium text-slate-500">Sản phẩm đã hết hàng (0)</div>
            <div className="text-2xl font-black text-rose-600 mt-1 tabular-nums">{outOfStockCount}</div>
          </div>
          <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600">
            <PackageX className="w-5 h-5" />
          </div>
        </div>
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

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          onClick={() => setActiveTab('inventory')}
          className={`pb-3 text-xs sm:text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            activeTab === 'inventory'
              ? 'border-orange-600 text-orange-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Boxes className="w-4 h-4" />
          <span>Tồn kho sản phẩm ({products.length})</span>
        </button>
        <button
          onClick={() => setActiveTab('transactions')}
          className={`pb-3 text-xs sm:text-sm font-bold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
            activeTab === 'transactions'
              ? 'border-orange-600 text-orange-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Nhật ký giao dịch kho</span>
        </button>
      </div>

      {/* TAB 1: INVENTORY TABLE */}
      {activeTab === 'inventory' && (
        <div className="space-y-4">
          {/* Search & Filter */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-2xs flex flex-col md:flex-row gap-4 items-center justify-between">
            <div className="w-full md:w-80 relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm theo tên sản phẩm..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
              />
            </div>

            <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
              {/* Category Filter */}
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="px-3 py-2 rounded-xl border border-slate-200 text-xs font-medium text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 cursor-pointer"
              >
                <option value="">Tất cả danh mục</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>

              {/* Stock status filter */}
              {[
                { id: 'ALL', label: 'Tất cả' },
                { id: 'IN_STOCK', label: 'Còn hàng (>5)' },
                { id: 'LOW_STOCK', label: 'Sắp hết (1-5)' },
                { id: 'OUT_OF_STOCK', label: 'Hết hàng (0)' },
              ].map((btn) => (
                <button
                  key={btn.id}
                  onClick={() => setStockFilter(btn.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                    stockFilter === btn.id
                      ? 'bg-orange-600 text-white shadow-2xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {btn.label}
                </button>
              ))}
            </div>
          </div>

          {/* Table */}
          <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
            {loading ? (
              <div className="p-12 text-center">
                <div className="w-8 h-8 border-3 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                <p className="text-slate-500 text-xs">Đang tải dữ liệu tồn kho...</p>
              </div>
            ) : error ? (
              <div className="p-12 text-center text-red-600 text-xs flex items-center justify-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-500" />
                <span>{error}</span>
              </div>
            ) : filteredProducts.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs">
                Không tìm thấy sản phẩm nào theo bộ lọc này.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[11px]">
                      <th className="py-3.5 px-4">Sản phẩm</th>
                      <th className="py-3.5 px-4">Danh mục</th>
                      <th className="py-3.5 px-4 text-right">Đơn giá</th>
                      <th className="py-3.5 px-4 text-center">Số lượng tồn</th>
                      <th className="py-3.5 px-4">Tình trạng kho</th>
                      <th className="py-3.5 px-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredProducts.map((p) => {
                      const stock = p.stockQuantity || 0
                      return (
                        <tr key={p.id} className="hover:bg-slate-50/60 transition-colors">
                          <td className="py-3.5 px-4">
                            <div className="flex items-center gap-3">
                              {p.imageUrl ? (
                                <img
                                  src={p.imageUrl}
                                  alt={p.name}
                                  className="w-10 h-10 rounded-lg object-cover border border-slate-200 shrink-0"
                                />
                              ) : (
                                <div className="w-10 h-10 rounded-lg bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 shrink-0">
                                  <Boxes className="w-5 h-5" />
                                </div>
                              )}
                              <div className="min-w-0">
                                <div className="font-bold text-slate-900 truncate max-w-xs">{p.name}</div>
                                <div className="text-[11px] text-slate-400">ID: #{p.id}</div>
                              </div>
                            </div>
                          </td>
                          <td className="py-3.5 px-4 text-slate-600 font-medium">
                            {p.categoryName || p.category?.name || 'Chưa phân loại'}
                          </td>
                          <td className="py-3.5 px-4 text-right font-bold text-slate-800 tabular-nums">
                            {formatPrice(p.price)}
                          </td>
                          <td className="py-3.5 px-4 text-center">
                            <span className="font-black text-sm text-slate-900 tabular-nums">
                              {stock}
                            </span>
                          </td>
                          <td className="py-3.5 px-4">
                            {stock === 0 ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
                                <PackageX className="w-3 h-3 text-rose-500" />
                                <span>Hết hàng</span>
                              </span>
                            ) : stock <= 5 ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                                <AlertTriangle className="w-3 h-3 text-amber-600" />
                                <span>Sắp hết hàng ({stock})</span>
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                                <PackageCheck className="w-3 h-3 text-emerald-600" />
                                <span>Còn hàng</span>
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-4 text-right space-x-2">
                            <button
                              onClick={() => handleOpenAdjustModal(p)}
                              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white font-semibold text-xs shadow-2xs transition-colors cursor-pointer"
                              title="Điều chỉnh số lượng kho"
                            >
                              <Layers className="w-3.5 h-3.5" />
                              <span>Điều chỉnh</span>
                            </button>
                            <button
                              onClick={() => handleOpenProductHistory(p)}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition-colors cursor-pointer"
                              title="Xem lịch sử biến động"
                            >
                              <History className="w-3.5 h-3.5 text-slate-500" />
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: SYSTEM TRANSACTIONS */}
      {activeTab === 'transactions' && (
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden">
          {loadingTx ? (
            <div className="p-12 text-center">
              <div className="w-8 h-8 border-3 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
              <p className="text-slate-500 text-xs">Đang tải nhật ký giao dịch kho...</p>
            </div>
          ) : transactions.length === 0 ? (
            <div className="p-12 text-center text-slate-500 text-xs">
              Chưa có giao dịch biến động kho nào được ghi nhận.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50/70 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-bold text-[11px]">
                    <th className="py-3.5 px-4">Thời gian</th>
                    <th className="py-3.5 px-4">Sản phẩm</th>
                    <th className="py-3.5 px-4">Loại biến động</th>
                    <th className="py-3.5 px-4 text-center">Thay đổi</th>
                    <th className="py-3.5 px-4 text-center">Tồn sau</th>
                    <th className="py-3.5 px-4">Ghi chú / Đơn hàng</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="py-3.5 px-4 text-slate-500 whitespace-nowrap">
                        {formatDate(tx.createdAt)}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="font-bold text-slate-900">{tx.productName}</span>
                        <span className="text-[11px] text-slate-400 block">Mã SP: #{tx.productId}</span>
                      </td>
                      <td className="py-3.5 px-4">
                        {getTransactionBadge(tx.transactionType)}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <span
                          className={`font-black text-xs tabular-nums px-2 py-0.5 rounded-md ${
                            tx.changeQuantity > 0
                              ? 'bg-emerald-50 text-emerald-700 font-bold'
                              : 'bg-rose-50 text-rose-700 font-bold'
                          }`}
                        >
                          {tx.changeQuantity > 0 ? `+${tx.changeQuantity}` : tx.changeQuantity}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-center font-extrabold text-slate-800 tabular-nums">
                        {tx.stockAfter}
                      </td>
                      <td className="py-3.5 px-4 text-slate-600">
                        <div>{tx.note || '—'}</div>
                        {tx.orderId && (
                          <span className="text-[10px] text-orange-600 font-semibold block">
                            Đơn hàng ID: #{tx.orderId}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* MODAL 1: ADJUST STOCK */}
      {adjustModalProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-md w-full overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70">
              <div className="flex items-center gap-2">
                <Layers className="w-5 h-5 text-orange-600" />
                <h3 className="font-bold text-slate-900 text-sm">Điều chỉnh tồn kho</h3>
              </div>
              <button
                onClick={() => setAdjustModalProduct(null)}
                className="text-slate-400 hover:text-slate-700 p-1 rounded-lg"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleAdjustSubmit} className="p-6 space-y-4">
              {/* Product Info */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 flex items-center gap-3">
                {adjustModalProduct.imageUrl ? (
                  <img
                    src={adjustModalProduct.imageUrl}
                    alt={adjustModalProduct.name}
                    className="w-12 h-12 rounded-lg object-cover border border-slate-200 shrink-0"
                  />
                ) : (
                  <div className="w-12 h-12 rounded-lg bg-slate-200 flex items-center justify-center text-slate-500 shrink-0">
                    <Boxes className="w-6 h-6" />
                  </div>
                )}
                <div className="min-w-0 flex-1">
                  <div className="font-bold text-slate-900 text-xs truncate">{adjustModalProduct.name}</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">
                    Tồn kho hiện tại:{' '}
                    <span className="font-extrabold text-orange-600 tabular-nums">
                      {adjustModalProduct.stockQuantity || 0}
                    </span>{' '}
                    sản phẩm
                  </div>
                </div>
              </div>

              {/* Adjust Type Selection */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Hình thức điều chỉnh
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setAdjustType('ADD')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                      adjustType === 'ADD'
                        ? 'border-emerald-600 bg-emerald-50 text-emerald-800 shadow-2xs'
                        : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    <Plus className="w-4 h-4 text-emerald-600" />
                    <span>Nhập thêm (+)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setAdjustType('SUBTRACT')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                      adjustType === 'SUBTRACT'
                        ? 'border-rose-600 bg-rose-50 text-rose-800 shadow-2xs'
                        : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    <Minus className="w-4 h-4 text-rose-600" />
                    <span>Xuất bớt (-)</span>
                  </button>
                </div>
              </div>

              {/* Quantity Input */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Số lượng {adjustType === 'ADD' ? 'nhập thêm' : 'xuất ra'} <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  min="1"
                  placeholder="Nhập số lượng..."
                  value={adjustQuantity}
                  onChange={(e) => setAdjustQuantity(e.target.value)}
                  required
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
                />
              </div>

              {/* Stock After Preview */}
              {adjustQuantity && !isNaN(parseInt(adjustQuantity, 10)) && parseInt(adjustQuantity, 10) > 0 && (
                <div className="p-3 bg-orange-50/70 border border-orange-200/80 rounded-xl flex items-center justify-between text-xs">
                  <span className="text-slate-600 font-medium">Tồn kho dự kiến sau điều chỉnh:</span>
                  <span
                    className={`font-black text-sm tabular-nums ${
                      adjustType === 'SUBTRACT' && parseInt(adjustQuantity, 10) > (adjustModalProduct.stockQuantity || 0)
                        ? 'text-red-600'
                        : 'text-orange-600'
                    }`}
                  >
                    {adjustType === 'ADD'
                      ? (adjustModalProduct.stockQuantity || 0) + parseInt(adjustQuantity, 10)
                      : (adjustModalProduct.stockQuantity || 0) - parseInt(adjustQuantity, 10)}
                  </span>
                </div>
              )}

              {/* Note Input */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Lý do / Ghi chú điều chỉnh
                </label>
                <textarea
                  rows="2"
                  placeholder="Ví dụ: Nhập hàng đợt mới từ kho tổng, kiểm kê định kỳ..."
                  value={adjustNote}
                  onChange={(e) => setAdjustNote(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500"
                ></textarea>
              </div>

              {/* Error in modal */}
              {adjustError && (
                <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{adjustError}</span>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setAdjustModalProduct(null)}
                  disabled={isSubmitting}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-xs font-semibold hover:bg-slate-50 cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-xs font-bold shadow-2xs transition-colors cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting ? 'Đang cập nhật...' : 'Xác nhận điều chỉnh'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: PRODUCT TRANSACTION HISTORY */}
      {historyProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xl max-w-2xl w-full overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50/70">
              <div className="flex items-center gap-2">
                <History className="w-5 h-5 text-orange-600" />
                <h3 className="font-bold text-slate-900 text-sm">
                  Lịch sử biến động: {historyProduct.name}
                </h3>
              </div>
              <button
                onClick={() => setHistoryProduct(null)}
                className="text-slate-400 hover:text-slate-700 p-1 rounded-lg"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-6 max-h-[70vh] overflow-y-auto">
              {loadingHistory ? (
                <div className="p-8 text-center">
                  <div className="w-6 h-6 border-2 border-orange-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  <p className="text-slate-500 text-xs">Đang tải lịch sử...</p>
                </div>
              ) : productHistoryList.length === 0 ? (
                <div className="p-8 text-center text-slate-500 text-xs">
                  Sản phẩm này chưa phát sinh giao dịch xuất/nhập nào.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase font-bold text-[10px]">
                        <th className="py-2.5 px-3">Thời gian</th>
                        <th className="py-2.5 px-3">Loại giao dịch</th>
                        <th className="py-2.5 px-3 text-center">Thay đổi</th>
                        <th className="py-2.5 px-3 text-center">Tồn sau</th>
                        <th className="py-2.5 px-3">Ghi chú</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {productHistoryList.map((tx) => (
                        <tr key={tx.id} className="hover:bg-slate-50/60">
                          <td className="py-2.5 px-3 text-slate-500 whitespace-nowrap">
                            {formatDate(tx.createdAt)}
                          </td>
                          <td className="py-2.5 px-3">
                            {getTransactionBadge(tx.transactionType)}
                          </td>
                          <td className="py-2.5 px-3 text-center">
                            <span
                              className={`font-black text-xs tabular-nums ${
                                tx.changeQuantity > 0 ? 'text-emerald-600' : 'text-rose-600'
                              }`}
                            >
                              {tx.changeQuantity > 0 ? `+${tx.changeQuantity}` : tx.changeQuantity}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-center font-bold text-slate-800 tabular-nums">
                            {tx.stockAfter}
                          </td>
                          <td className="py-2.5 px-3 text-slate-600">
                            <div>{tx.note || '—'}</div>
                            {tx.orderId && (
                              <span className="text-[10px] text-orange-600">Đơn #{tx.orderId}</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="px-6 py-3 border-t border-slate-200 bg-slate-50 flex justify-end">
              <button
                onClick={() => setHistoryProduct(null)}
                className="px-4 py-1.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold cursor-pointer"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdminInventoryPage
