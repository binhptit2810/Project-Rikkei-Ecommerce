import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Boxes,
  Package,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  AlertOctagon,
  RefreshCw,
  Search,
  Filter,
  ArrowUpDown,
  History,
  ArrowLeftRight,
  ChevronLeft,
  ChevronRight,
  Clock,
  X,
  RotateCcw,
} from 'lucide-react'
import inventoryService from '../../services/inventoryService'
import categoryService from '../../services/categoryService'
import AdjustStockModal from '../../components/admin/AdjustStockModal'

const DEFAULT_FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="%23a1a1aa" stroke-width="1.5"><rect width="18" height="18" x="3" y="3" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>'

/**
 * Format tiền tệ VNĐ chuẩn phong cách Việt Nam
 */
const formatVND = (amount) => {
  if (amount === null || amount === undefined || isNaN(amount)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount)
}

/**
 * Format ngày giờ dd/MM/yyyy HH:mm:ss
 */
const formatDateTime = (dateString) => {
  if (!dateString) return '-'
  try {
    const d = new Date(dateString)
    if (isNaN(d.getTime())) return dateString
    const pad = (n) => String(n).padStart(2, '0')
    const day = pad(d.getDate())
    const month = pad(d.getMonth() + 1)
    const year = d.getFullYear()
    const hours = pad(d.getHours())
    const minutes = pad(d.getMinutes())
    const seconds = pad(d.getSeconds())
    return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`
  } catch {
    return dateString
  }
}

/**
 * Cấu hình hiển thị Badge cho các loại giao dịch biến động kho
 */
const TRANSACTION_TYPE_CONFIG = {
  MANUAL_IMPORT: {
    label: 'Admin nhập kho',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    dotClass: 'bg-emerald-500',
  },
  MANUAL_EXPORT: {
    label: 'Admin xuất kho',
    badgeClass: 'bg-rose-50 text-rose-800 border-rose-200',
    dotClass: 'bg-rose-500',
  },
  MANUAL_ADJUST: {
    label: 'Điều chỉnh thủ công',
    badgeClass: 'bg-zinc-100 text-zinc-800 border-zinc-200',
    dotClass: 'bg-zinc-500',
  },
  ORDER_PLACED: {
    label: 'Khách đặt hàng',
    badgeClass: 'bg-indigo-50 text-indigo-800 border-indigo-200',
    dotClass: 'bg-indigo-500',
  },
  ORDER_CANCELLED: {
    label: 'Hủy đơn hoàn kho',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-200',
    dotClass: 'bg-amber-500',
  },
  INITIAL_STOCK: {
    label: 'Khởi tạo kho ban đầu',
    badgeClass: 'bg-blue-50 text-blue-800 border-blue-200',
    dotClass: 'bg-blue-500',
  },
}

/**
 * Trang Quản trị Kho hàng (AdminInventoryPage).
 * Route: /admin/inventory
 * Phụ trách: Đỗ Hoàng Anh (Senior Frontend Engineer)
 * Task ID: [TSK-D09-DHA] Xây dựng Giao diện Quản trị Kho hàng
 *
 * Đáp ứng đầy đủ 4 Checklist Trello:
 *  1. [x] Bảng hiển thị tồn kho hiện tại từng sản phẩm
 *  2. [x] Đánh dấu màu đỏ cảnh báo sản phẩm có tồn kho <= 5
 *  3. [x] Thiết kế modal Nhập/Xuất kho gửi lý do
 *  4. [x] Tạo tab hiển thị bảng lịch sử biến động kho từ API
 */
export const AdminInventoryPage = () => {
  // --- Quản lý Tab chính ---
  const [activeTab, setActiveTab] = useState('inventory') // 'inventory' | 'transactions'

  // --- Dữ liệu Sản phẩm Tồn kho (Tab 1) ---
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [totalProducts, setTotalProducts] = useState(0)
  const [productTotalPages, setProductTotalPages] = useState(1)
  const [productPage, setProductPage] = useState(0)
  const [productPageSize] = useState(10)

  // --- Bộ lọc Sản phẩm (Tab 1) ---
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('')
  const [stockFilter, setStockFilter] = useState('ALL') // 'ALL' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'IN_STOCK'
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortDir, setSortDir] = useState('desc')

  // --- Thống kê Kho hàng tổng quan (KPI Cards) ---
  const [inventoryStats, setInventoryStats] = useState({
    totalProducts: 0,
    safeStockCount: 0, // stock > 5
    lowStockCount: 0, // 1 <= stock <= 5
    outOfStockCount: 0, // stock === 0
  })

  // --- Dữ liệu Lịch sử Biến động Kho (Tab 2) ---
  const [transactions, setTransactions] = useState([])
  const [totalTransactions, setTotalTransactions] = useState(0)
  const [txTotalPages, setTxTotalPages] = useState(1)
  const [txPage, setTxPage] = useState(0)
  const [txPageSize] = useState(10)
  const [txTypeFilter, setTxTypeFilter] = useState('') // '' | 'MANUAL_IMPORT' | ...
  const [txProductFilter, setTxProductFilter] = useState(null) // { id, name } hoặc null

  // --- Trạng thái Giao diện (Loading & Toast) ---
  const [isProductsLoading, setIsProductsLoading] = useState(true)
  const [isTxLoading, setIsTxLoading] = useState(false)
  const [toastMessage, setToastMessage] = useState(null) // { type: 'success' | 'error', text: '' }

  // --- Trạng thái Modal Điều chỉnh Kho (AdjustStockModal) ---
  const [selectedAdjustProduct, setSelectedAdjustProduct] = useState(null)
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false)

  // Tự động đóng Toast sau 4.5 giây
  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(null), 4500)
      return () => clearTimeout(timer)
    }
  }, [toastMessage])

  // Tải danh sách Danh mục cho dropdown bộ lọc
  const fetchCategories = useCallback(async () => {
    try {
      let res
      try {
        res = await categoryService.getAdminCategories()
      } catch {
        res = await categoryService.getCategories()
      }
      if (res?.data) {
        if (Array.isArray(res.data)) {
          setCategories(res.data)
        } else if (Array.isArray(res.data.content)) {
          setCategories(res.data.content)
        }
      }
    } catch (err) {
      console.warn('AdminInventoryPage: Không thể tải danh mục:', err)
    }
  }, [])

  // Tải dữ liệu thống kê KPI kho hàng toàn bộ hệ thống
  const fetchInventoryStats = useCallback(async () => {
    try {
      // Tải mẫu kích thước đủ lớn để tính toán chính xác KPI
      const res = await inventoryService.getInventoryProducts({
        page: 0,
        size: 200,
      })
      const items = res?.data?.content || res?.data || []
      if (Array.isArray(items)) {
        let safe = 0
        let low = 0
        let out = 0
        items.forEach((p) => {
          const qty = Number(p.stockQuantity ?? 0)
          if (qty === 0) out++
          else if (qty <= 5) low++
          else safe++
        })
        const total = res?.data?.totalElements ?? items.length
        setInventoryStats({
          totalProducts: total,
          safeStockCount: safe,
          lowStockCount: low,
          outOfStockCount: out,
        })
      }
    } catch (err) {
      console.warn('AdminInventoryPage: Không thể tính KPI kho hàng:', err)
    }
  }, [])

  // Tải danh sách sản phẩm tồn kho (Tab 1)
  const fetchProducts = useCallback(async () => {
    setIsProductsLoading(true)
    try {
      const params = {
        page: productPage,
        size: productPageSize,
        keyword: searchKeyword.trim() || undefined,
        categoryId: selectedCategory || undefined,
        sortBy,
        sortDir,
      }

      const res = await inventoryService.getInventoryProducts(params)
      if (res?.data) {
        if (Array.isArray(res.data)) {
          setProducts(res.data)
          setTotalProducts(res.data.length)
          setProductTotalPages(Math.ceil(res.data.length / productPageSize) || 1)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setProducts(res.data.content)
          setTotalProducts(res.data.totalElements ?? res.data.content.length)
          setProductTotalPages(res.data.totalPages ?? 1)
        } else {
          setProducts([])
          setTotalProducts(0)
          setProductTotalPages(1)
        }
      } else {
        setProducts([])
        setTotalProducts(0)
        setProductTotalPages(1)
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải danh sách tồn kho sản phẩm.'
      setToastMessage({ type: 'error', text: msg })
      setProducts([])
      setTotalProducts(0)
    } finally {
      setIsProductsLoading(false)
    }
  }, [productPage, productPageSize, searchKeyword, selectedCategory, sortBy, sortDir])

  // Tải danh sách lịch sử biến động kho (Tab 2)
  const fetchTransactions = useCallback(async () => {
    setIsTxLoading(true)
    try {
      const params = {
        page: txPage,
        size: txPageSize,
        productId: txProductFilter?.id || undefined,
        type: txTypeFilter || undefined,
        sortBy: 'createdAt',
        sortDir: 'desc',
      }

      const res = await inventoryService.getTransactions(params)
      if (res?.data) {
        if (Array.isArray(res.data)) {
          setTransactions(res.data)
          setTotalTransactions(res.data.length)
          setTxTotalPages(Math.ceil(res.data.length / txPageSize) || 1)
        } else if (res.data.content && Array.isArray(res.data.content)) {
          setTransactions(res.data.content)
          setTotalTransactions(res.data.totalElements ?? res.data.content.length)
          setTxTotalPages(res.data.totalPages ?? 1)
        } else {
          setTransactions([])
          setTotalTransactions(0)
          setTxTotalPages(1)
        }
      } else {
        setTransactions([])
        setTotalTransactions(0)
        setTxTotalPages(1)
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Không thể tải lịch sử biến động kho.'
      setToastMessage({ type: 'error', text: msg })
      setTransactions([])
      setTotalTransactions(0)
    } finally {
      setIsTxLoading(false)
    }
  }, [txPage, txPageSize, txProductFilter, txTypeFilter])

  // Khởi tạo dữ liệu
  useEffect(() => {
    fetchCategories()
    fetchInventoryStats()
  }, [fetchCategories, fetchInventoryStats])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  useEffect(() => {
    if (activeTab === 'transactions') {
      fetchTransactions()
    }
  }, [activeTab, fetchTransactions])

  // Map ID Danh mục sang Tên hiển thị
  const categoryMap = useMemo(() => {
    const map = new Map()
    categories.forEach((cat) => map.set(cat.id, cat.name))
    return map
  }, [categories])

  // Lọc sản phẩm ở client theo trạng thái tồn kho nếu có
  const filteredProducts = useMemo(() => {
    if (stockFilter === 'ALL') return products
    return products.filter((p) => {
      const qty = Number(p.stockQuantity ?? 0)
      if (stockFilter === 'OUT_OF_STOCK') return qty === 0
      if (stockFilter === 'LOW_STOCK') return qty >= 0 && qty <= 5
      if (stockFilter === 'IN_STOCK') return qty > 5
      return true
    })
  }, [products, stockFilter])

  // Mở modal điều chỉnh kho cho sản phẩm
  const handleOpenAdjustModal = (product) => {
    setSelectedAdjustProduct(product)
    setIsAdjustModalOpen(true)
  }

  // Chuyển sang Tab lịch sử và lọc riêng cho sản phẩm đó
  const handleViewProductHistory = (product) => {
    setTxProductFilter({ id: product.id, name: product.name })
    setTxPage(0)
    setActiveTab('transactions')
  }

  // Xóa lọc theo sản phẩm ở Tab lịch sử
  const handleClearTxProductFilter = () => {
    setTxProductFilter(null)
    setTxPage(0)
  }

  // Callback thành công từ AdjustStockModal
  const handleAdjustSuccess = (msg) => {
    setToastMessage({ type: 'success', text: msg })
    fetchProducts()
    fetchInventoryStats()
    if (activeTab === 'transactions') {
      fetchTransactions()
    }
  }

  // Đặt lại toàn bộ bộ lọc Tab tồn kho
  const handleResetFilters = () => {
    setSearchKeyword('')
    setSelectedCategory('')
    setStockFilter('ALL')
    setSortBy('createdAt')
    setSortDir('desc')
    setProductPage(0)
  }

  return (
    <div className="flex-1 py-8 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full animate-in fade-in duration-200">
      {/* Toast thông báo nổi góc phải */}
      {toastMessage && (
        <div
          className={`fixed top-20 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg border text-xs sm:text-sm font-semibold transition-all animate-in slide-in-from-top-2 ${
            toastMessage.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertOctagon className="w-5 h-5 text-rose-600 shrink-0" />
          )}
          <span>{toastMessage.text}</span>
          <button
            type="button"
            onClick={() => setToastMessage(null)}
            className="p-1 hover:bg-zinc-200/50 rounded-lg text-zinc-500 cursor-pointer ml-2"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 1. Header Trang & Nút Làm Mới */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-zinc-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-zinc-950 tracking-tight">
              Quản lý kho hàng
            </h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold bg-zinc-100 text-zinc-800 border border-zinc-200 uppercase tracking-wider">
              <Boxes className="w-3.5 h-3.5 text-zinc-700" />
              Inventory Central
            </span>
          </div>
          <p className="text-xs sm:text-sm text-zinc-500 mt-1">
            Theo dõi số lượng tồn kho theo thời gian thực, chủ động phát hiện hàng sắp hết và quản trị lịch sử xuất/nhập kho.
          </p>
        </div>

        {/* Nút hành động header */}
        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => {
              if (activeTab === 'inventory') {
                fetchProducts()
                fetchInventoryStats()
              } else {
                fetchTransactions()
              }
            }}
            disabled={isProductsLoading || isTxLoading}
            className="inline-flex items-center gap-2 px-3.5 py-2 bg-white border border-zinc-300 rounded-xl text-xs sm:text-sm font-semibold text-zinc-700 hover:bg-zinc-50 hover:text-zinc-950 shadow-xs transition-all cursor-pointer disabled:opacity-50"
            title="Làm mới dữ liệu từ máy chủ"
          >
            <RefreshCw
              className={`w-4 h-4 ${
                isProductsLoading || isTxLoading
                  ? 'animate-spin text-zinc-900'
                  : 'text-zinc-500'
              }`}
            />
            <span>Làm mới dữ liệu</span>
          </button>
        </div>
      </div>

      {/* 2. Hệ thống Thẻ KPI Thống kê Nhanh (Summary Cards) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 my-6">
        {/* Thẻ 1: Tổng sản phẩm quản lý */}
        <div
          onClick={() => {
            setActiveTab('inventory')
            setStockFilter('ALL')
          }}
          className={`p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer hover:shadow-md ${
            stockFilter === 'ALL' && activeTab === 'inventory'
              ? 'bg-zinc-900 text-white border-zinc-900 ring-2 ring-zinc-900/10'
              : 'bg-white border-zinc-200 text-zinc-900 hover:border-zinc-300'
          }`}
          title="Bấm để xem tất cả sản phẩm"
        >
          <div className="flex items-center justify-between">
            <span
              className={`text-xs font-semibold uppercase tracking-wider ${
                stockFilter === 'ALL' && activeTab === 'inventory'
                  ? 'text-zinc-400'
                  : 'text-zinc-500'
              }`}
            >
              Tổng sản phẩm
            </span>
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                stockFilter === 'ALL' && activeTab === 'inventory'
                  ? 'bg-zinc-800 text-zinc-100'
                  : 'bg-zinc-100 text-zinc-700'
              }`}
            >
              <Package className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold mt-2 tabular-nums">
            {inventoryStats.totalProducts || totalProducts}
          </div>
          <p
            className={`text-[11px] mt-1 ${
              stockFilter === 'ALL' && activeTab === 'inventory'
                ? 'text-zinc-400'
                : 'text-zinc-500'
            }`}
          >
            Mã hàng đang lưu thông
          </p>
        </div>

        {/* Thẻ 2: Tồn kho an toàn (> 5) */}
        <div
          onClick={() => {
            setActiveTab('inventory')
            setStockFilter('IN_STOCK')
          }}
          className={`p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer hover:shadow-md ${
            stockFilter === 'IN_STOCK' && activeTab === 'inventory'
              ? 'bg-emerald-600 text-white border-emerald-600 ring-2 ring-emerald-500/20'
              : 'bg-white border-zinc-200 text-zinc-900 hover:border-emerald-200'
          }`}
          title="Bấm để lọc các sản phẩm có tồn kho an toàn"
        >
          <div className="flex items-center justify-between">
            <span
              className={`text-xs font-semibold uppercase tracking-wider ${
                stockFilter === 'IN_STOCK' && activeTab === 'inventory'
                  ? 'text-emerald-100'
                  : 'text-zinc-500'
              }`}
            >
              Tồn kho an toàn (&gt;5)
            </span>
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                stockFilter === 'IN_STOCK' && activeTab === 'inventory'
                  ? 'bg-emerald-700 text-white'
                  : 'bg-emerald-50 text-emerald-600'
              }`}
            >
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold mt-2 tabular-nums">
            {inventoryStats.safeStockCount}
          </div>
          <p
            className={`text-[11px] mt-1 ${
              stockFilter === 'IN_STOCK' && activeTab === 'inventory'
                ? 'text-emerald-100'
                : 'text-emerald-600 font-medium'
            }`}
          >
            Đủ hàng đáp ứng đơn mới
          </p>
        </div>

        {/* Thẻ 3: Sắp hết hàng (1 <= stock <= 5) */}
        <div
          onClick={() => {
            setActiveTab('inventory')
            setStockFilter('LOW_STOCK')
          }}
          className={`p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer hover:shadow-md ${
            stockFilter === 'LOW_STOCK' && activeTab === 'inventory'
              ? 'bg-amber-600 text-white border-amber-600 ring-2 ring-amber-500/20'
              : 'bg-amber-50/50 border-amber-200 text-zinc-900 hover:border-amber-300'
          }`}
          title="Bấm để lọc các sản phẩm sắp hết hàng"
        >
          <div className="flex items-center justify-between">
            <span
              className={`text-xs font-semibold uppercase tracking-wider ${
                stockFilter === 'LOW_STOCK' && activeTab === 'inventory'
                  ? 'text-amber-100'
                  : 'text-amber-800'
              }`}
            >
              Sắp hết hàng (≤ 5)
            </span>
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                stockFilter === 'LOW_STOCK' && activeTab === 'inventory'
                  ? 'bg-amber-700 text-white'
                  : 'bg-amber-100 text-amber-700'
              }`}
            >
              <AlertTriangle className="w-4 h-4 animate-bounce" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold mt-2 tabular-nums text-amber-700">
            {inventoryStats.lowStockCount}
          </div>
          <p
            className={`text-[11px] mt-1 ${
              stockFilter === 'LOW_STOCK' && activeTab === 'inventory'
                ? 'text-amber-100'
                : 'text-amber-700 font-semibold'
            }`}
          >
            Cần lên kế hoạch nhập kho
          </p>
        </div>

        {/* Thẻ 4: Đã hết hàng (stock === 0) */}
        <div
          onClick={() => {
            setActiveTab('inventory')
            setStockFilter('OUT_OF_STOCK')
          }}
          className={`p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer hover:shadow-md ${
            stockFilter === 'OUT_OF_STOCK' && activeTab === 'inventory'
              ? 'bg-rose-600 text-white border-rose-600 ring-2 ring-rose-500/20'
              : 'bg-rose-50/50 border-rose-200 text-zinc-900 hover:border-rose-300'
          }`}
          title="Bấm để lọc các sản phẩm đã cạn kho"
        >
          <div className="flex items-center justify-between">
            <span
              className={`text-xs font-semibold uppercase tracking-wider ${
                stockFilter === 'OUT_OF_STOCK' && activeTab === 'inventory'
                  ? 'text-rose-100'
                  : 'text-rose-800'
              }`}
            >
              Đã hết hàng (0)
            </span>
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                stockFilter === 'OUT_OF_STOCK' && activeTab === 'inventory'
                  ? 'bg-rose-700 text-white'
                  : 'bg-rose-100 text-rose-700'
              }`}
            >
              <XCircle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold mt-2 tabular-nums text-rose-700">
            {inventoryStats.outOfStockCount}
          </div>
          <p
            className={`text-[11px] mt-1 ${
              stockFilter === 'OUT_OF_STOCK' && activeTab === 'inventory'
                ? 'text-rose-100'
                : 'text-rose-700 font-semibold'
            }`}
          >
            Ngừng nhận đơn do hết hàng
          </p>
        </div>
      </div>

      {/* 3. Thanh Chuyển Tab (2 Tabs Chính) */}
      <div className="flex items-center gap-2 border-b border-zinc-200 mb-6">
        <button
          type="button"
          onClick={() => setActiveTab('inventory')}
          className={`flex items-center gap-2 pb-3 pt-1 px-3 text-xs sm:text-sm font-semibold border-b-2 transition-all cursor-pointer ${
            activeTab === 'inventory'
              ? 'border-zinc-900 text-zinc-950 font-bold'
              : 'border-transparent text-zinc-500 hover:text-zinc-800 hover:border-zinc-300'
          }`}
        >
          <Boxes className="w-4 h-4" />
          <span>Quản lý Tồn kho Sản phẩm</span>
          <span className="px-1.5 py-0.2 rounded-full text-[11px] font-mono bg-zinc-100 text-zinc-700 border border-zinc-200">
            {totalProducts}
          </span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('transactions')}
          className={`flex items-center gap-2 pb-3 pt-1 px-3 text-xs sm:text-sm font-semibold border-b-2 transition-all cursor-pointer ${
            activeTab === 'transactions'
              ? 'border-zinc-900 text-zinc-950 font-bold'
              : 'border-transparent text-zinc-500 hover:text-zinc-800 hover:border-zinc-300'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Lịch sử Biến động Kho</span>
          {txProductFilter ? (
            <span className="px-2 py-0.2 rounded-full text-[11px] font-bold bg-amber-100 text-amber-800 border border-amber-200 animate-pulse">
              Lọc SP #{txProductFilter.id}
            </span>
          ) : (
            <span className="px-1.5 py-0.2 rounded-full text-[11px] font-mono bg-zinc-100 text-zinc-700 border border-zinc-200">
              {totalTransactions}
            </span>
          )}
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: QUẢN LÝ TỒN KHO SẢN PHẨM (Current Inventory Table)                  */}
      {/* ========================================================================= */}
      {activeTab === 'inventory' && (
        <div className="space-y-5 animate-in fade-in duration-150">
          {/* Thanh công cụ (Toolbar Bộ lọc) */}
          <div className="bg-white p-4 rounded-2xl border border-zinc-200 shadow-xs grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
            {/* Tìm kiếm tên hoặc mã SP */}
            <div className="lg:col-span-4 relative">
              <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm tên hoặc mã sản phẩm..."
                value={searchKeyword}
                onChange={(e) => {
                  setSearchKeyword(e.target.value)
                  setProductPage(0)
                }}
                className="w-full pl-9 pr-3.5 py-2 text-xs sm:text-sm bg-zinc-50 border border-zinc-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-zinc-900"
              />
            </div>

            {/* Lọc theo danh mục */}
            <div className="lg:col-span-3 relative">
              <Filter className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <select
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value)
                  setProductPage(0)
                }}
                className="w-full pl-9 pr-3.5 py-2 text-xs sm:text-sm bg-zinc-50 border border-zinc-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-zinc-900 cursor-pointer appearance-none"
              >
                <option value="">Tất cả danh mục</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Lọc nhanh trạng thái tồn kho */}
            <div className="lg:col-span-3 relative">
              <Boxes className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <select
                value={stockFilter}
                onChange={(e) => setStockFilter(e.target.value)}
                className="w-full pl-9 pr-3.5 py-2 text-xs sm:text-sm bg-zinc-50 border border-zinc-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-zinc-900 cursor-pointer appearance-none"
              >
                <option value="ALL">Tất cả mức tồn kho</option>
                <option value="LOW_STOCK">⚠️ Cảnh báo tồn thấp (≤ 5)</option>
                <option value="OUT_OF_STOCK">⛔ Đã hết hàng (0)</option>
                <option value="IN_STOCK">✅ Tồn kho an toàn (&gt; 5)</option>
              </select>
            </div>

            {/* Sắp xếp */}
            <div className="lg:col-span-2 relative">
              <ArrowUpDown className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <select
                value={`${sortBy}-${sortDir}`}
                onChange={(e) => {
                  const [sb, sd] = e.target.value.split('-')
                  setSortBy(sb)
                  setSortDir(sd)
                  setProductPage(0)
                }}
                className="w-full pl-8 pr-2 py-2 text-xs sm:text-sm bg-zinc-50 border border-zinc-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-zinc-900 cursor-pointer appearance-none"
              >
                <option value="createdAt-desc">Mới nhất</option>
                <option value="stockQuantity-asc">Tồn kho: Thấp ➔ Cao</option>
                <option value="stockQuantity-desc">Tồn kho: Cao ➔ Thấp</option>
                <option value="price-desc">Giá: Cao ➔ Thấp</option>
                <option value="price-asc">Giá: Thấp ➔ Cao</option>
              </select>
            </div>
          </div>

          {/* Dòng tóm tắt bộ lọc đang áp dụng */}
          {(searchKeyword || selectedCategory || stockFilter !== 'ALL') && (
            <div className="flex items-center justify-between text-xs text-zinc-600 px-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-zinc-400">Bộ lọc đang áp dụng:</span>
                {searchKeyword && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-zinc-100 text-zinc-800 border border-zinc-200">
                    Từ khóa: &ldquo;{searchKeyword}&rdquo;
                  </span>
                )}
                {selectedCategory && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-zinc-100 text-zinc-800 border border-zinc-200">
                    Danh mục: {categoryMap.get(Number(selectedCategory)) || selectedCategory}
                  </span>
                )}
                {stockFilter !== 'ALL' && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-zinc-100 text-zinc-800 border border-zinc-200">
                    Trạng thái:{' '}
                    {stockFilter === 'LOW_STOCK'
                      ? 'Tồn thấp (≤ 5)'
                      : stockFilter === 'OUT_OF_STOCK'
                      ? 'Hết hàng (0)'
                      : 'Tồn an toàn (> 5)'}
                  </span>
                )}
              </div>
              <button
                type="button"
                onClick={handleResetFilters}
                className="inline-flex items-center gap-1 text-xs font-semibold text-zinc-600 hover:text-zinc-950 cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Đặt lại</span>
              </button>
            </div>
          )}

          {/* Bảng Quản lý Tồn kho (Checklist #1 & #2) */}
          <div className="bg-white rounded-2xl border border-zinc-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-zinc-50/80 border-b border-zinc-200 text-[11px] font-bold text-zinc-500 uppercase tracking-wider">
                    <th className="py-3.5 px-4 w-16">Mã SP</th>
                    <th className="py-3.5 px-4 min-w-[240px]">Sản phẩm</th>
                    <th className="py-3.5 px-4 hidden md:table-cell">Danh mục</th>
                    <th className="py-3.5 px-4 text-right">Giá bán</th>
                    <th className="py-3.5 px-4 text-center">Tồn kho</th>
                    <th className="py-3.5 px-4 min-w-[190px]">Trạng thái kho</th>
                    <th className="py-3.5 px-4 text-right min-w-[180px]">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-200/70 text-xs sm:text-sm">
                  {isProductsLoading ? (
                    // Skeleton Loading
                    Array.from({ length: 5 }).map((_, idx) => (
                      <tr key={idx} className="animate-pulse">
                        <td className="py-4 px-4">
                          <div className="h-4 bg-zinc-200 rounded w-8"></div>
                        </td>
                        <td className="py-4 px-4">
                          <div className="flex items-center gap-3">
                            <div className="w-11 h-11 bg-zinc-200 rounded-lg"></div>
                            <div className="space-y-1.5 flex-1">
                              <div className="h-4 bg-zinc-200 rounded w-3/4"></div>
                              <div className="h-3 bg-zinc-100 rounded w-1/3"></div>
                            </div>
                          </div>
                        </td>
                        <td className="py-4 px-4 hidden md:table-cell">
                          <div className="h-4 bg-zinc-200 rounded w-20"></div>
                        </td>
                        <td className="py-4 px-4 text-right">
                          <div className="h-4 bg-zinc-200 rounded w-16 ml-auto"></div>
                        </td>
                        <td className="py-4 px-4">
                          <div className="h-5 bg-zinc-200 rounded w-10 mx-auto"></div>
                        </td>
                        <td className="py-4 px-4">
                          <div className="h-6 bg-zinc-200 rounded w-28"></div>
                        </td>
                        <td className="py-4 px-4 text-right">
                          <div className="h-8 bg-zinc-200 rounded w-24 ml-auto"></div>
                        </td>
                      </tr>
                    ))
                  ) : filteredProducts.length === 0 ? (
                    // Empty State
                    <tr>
                      <td colSpan={7} className="py-12 text-center text-zinc-500">
                        <div className="flex flex-col items-center justify-center gap-2">
                          <Boxes className="w-10 h-10 text-zinc-300 stroke-[1.25]" />
                          <p className="text-sm font-semibold text-zinc-800">
                            Không tìm thấy sản phẩm nào
                          </p>
                          <p className="text-xs text-zinc-400">
                            Hãy thử thay đổi từ khóa hoặc bộ lọc trạng thái tồn kho
                          </p>
                          <button
                            type="button"
                            onClick={handleResetFilters}
                            className="mt-2 px-3 py-1.5 text-xs font-semibold rounded-lg bg-zinc-100 hover:bg-zinc-200 text-zinc-800 transition-colors cursor-pointer"
                          >
                            Xóa bộ lọc
                          </button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    // Danh sách sản phẩm thực tế
                    filteredProducts.map((p) => {
                      const stock = Number(p.stockQuantity ?? 0)
                      const isOutOfStock = stock === 0
                      const isLowStock = stock > 0 && stock <= 5
                      const isWarning = stock <= 5

                      return (
                        <tr
                          key={p.id}
                          className={`transition-colors ${
                            isOutOfStock
                              ? 'border-l-4 border-l-rose-600 bg-rose-50/40 hover:bg-rose-50/70'
                              : isLowStock
                              ? 'border-l-4 border-l-amber-500 bg-amber-50/30 hover:bg-amber-50/60'
                              : 'hover:bg-zinc-50/80'
                          }`}
                        >
                          {/* Cột 1: Mã SP */}
                          <td className="py-3.5 px-4 font-mono text-xs font-semibold text-zinc-400">
                            #{p.id}
                          </td>

                          {/* Cột 2: Ảnh & Tên Sản phẩm */}
                          <td className="py-3.5 px-4">
                            <div className="flex items-center gap-3">
                              <img
                                src={p.imageUrl || DEFAULT_FALLBACK_IMAGE}
                                alt={p.name}
                                onError={(e) => {
                                  e.currentTarget.onerror = null
                                  e.currentTarget.src = DEFAULT_FALLBACK_IMAGE
                                }}
                                className="w-11 h-11 rounded-lg object-cover bg-zinc-100 border border-zinc-200 shrink-0"
                              />
                              <div className="min-w-0">
                                <span className="font-semibold text-zinc-900 block truncate hover:text-zinc-700">
                                  {p.name}
                                </span>
                                <div className="flex items-center gap-2 mt-0.5">
                                  <span
                                    className={`inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-medium ${
                                      p.active !== false
                                        ? 'bg-zinc-100 text-zinc-700'
                                        : 'bg-zinc-200 text-zinc-500 line-through'
                                    }`}
                                  >
                                    {p.active !== false ? 'Đang bán' : 'Tạm ẩn'}
                                  </span>
                                  <span className="text-[11px] text-zinc-400 md:hidden">
                                    • {categoryMap.get(p.categoryId) || 'Chung'}
                                  </span>
                                </div>
                              </div>
                            </div>
                          </td>

                          {/* Cột 3: Danh mục */}
                          <td className="py-3.5 px-4 text-zinc-600 hidden md:table-cell text-xs">
                            {categoryMap.get(p.categoryId) ||
                              p.categoryName ||
                              'Chưa phân loại'}
                          </td>

                          {/* Cột 4: Giá bán */}
                          <td className="py-3.5 px-4 text-right font-medium tabular-nums text-zinc-900">
                            {formatVND(p.price)}
                          </td>

                          {/* Cột 5: Số lượng tồn kho hiện tại */}
                          <td className="py-3.5 px-4 text-center">
                            <span
                              className={`text-sm sm:text-base font-bold tabular-nums font-mono ${
                                isOutOfStock
                                  ? 'text-rose-700'
                                  : isLowStock
                                  ? 'text-amber-700'
                                  : 'text-zinc-900'
                              }`}
                            >
                              {stock}
                            </span>
                          </td>

                          {/* Cột 6: Trạng thái kho - Huy hiệu đỏ cảnh báo tồn kho <= 5 (Checklist #2) */}
                          <td className="py-3.5 px-4">
                            {isOutOfStock ? (
                              <div className="inline-flex flex-col gap-1">
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200 shadow-xs ring-1 ring-rose-500/20">
                                  <span className="relative flex h-2 w-2">
                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                                    <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-600"></span>
                                  </span>
                                  <XCircle className="w-3.5 h-3.5" />
                                  <span>Hết hàng (0)</span>
                                </span>
                              </div>
                            ) : isLowStock ? (
                              <div className="flex flex-col gap-1 max-w-[160px]">
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 shadow-xs ring-1 ring-amber-500/20">
                                  <span className="relative flex h-2 w-2">
                                    <span className="animate-pulse absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                                    <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-600"></span>
                                  </span>
                                  <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                                  <span>Sắp hết (Còn {stock})</span>
                                </span>
                                {/* Progress bar cảnh báo tồn kho nguy hiểm */}
                                <div className="w-full bg-zinc-200 h-1.5 rounded-full overflow-hidden">
                                  <div
                                    className="bg-amber-500 h-full rounded-full transition-all"
                                    style={{
                                      width: `${Math.min(
                                        100,
                                        Math.max(10, (stock / 5) * 100)
                                      )}%`,
                                    }}
                                  ></div>
                                </div>
                              </div>
                            ) : (
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                <span>Còn hàng ({stock})</span>
                              </span>
                            )}
                          </td>

                          {/* Cột 7: Thao tác (Nhập / Xuất kho & Xem lịch sử) */}
                          <td className="py-3.5 px-4 text-right">
                            <div className="inline-flex items-center gap-1.5">
                              {/* Nút Nhập / Xuất kho (Mở modal) */}
                              <button
                                type="button"
                                onClick={() => handleOpenAdjustModal(p)}
                                className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold shadow-xs transition-all cursor-pointer ${
                                  isWarning
                                    ? 'bg-zinc-900 text-white hover:bg-zinc-800'
                                    : 'bg-white text-zinc-800 border border-zinc-200 hover:bg-zinc-50'
                                }`}
                                title="Điều chỉnh tăng hoặc giảm tồn kho kèm lý do"
                              >
                                <ArrowLeftRight className="w-3.5 h-3.5" />
                                <span>Nhập/Xuất kho</span>
                              </button>

                              {/* Nút Xem lịch sử biến động */}
                              <button
                                type="button"
                                onClick={() => handleViewProductHistory(p)}
                                className="p-1.5 rounded-lg text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100 transition-colors cursor-pointer"
                                title="Xem lịch sử biến động kho của sản phẩm này"
                              >
                                <History className="w-4 h-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>

            {/* Phân trang Bảng Tồn kho */}
            {productTotalPages > 1 && (
              <div className="px-4 py-3.5 border-t border-zinc-200 bg-zinc-50/50 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-zinc-500">
                <div>
                  Hiển thị{' '}
                  <strong className="text-zinc-900 font-mono">
                    {productPage * productPageSize + 1}
                  </strong>{' '}
                  -{' '}
                  <strong className="text-zinc-900 font-mono">
                    {Math.min((productPage + 1) * productPageSize, totalProducts)}
                  </strong>{' '}
                  trong tổng số{' '}
                  <strong className="text-zinc-900 font-mono">{totalProducts}</strong>{' '}
                  sản phẩm
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setProductPage((p) => Math.max(0, p - 1))}
                    disabled={productPage === 0 || isProductsLoading}
                    className="p-1.5 rounded-lg border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-xs transition-colors"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <span className="px-2 font-mono font-medium text-zinc-700">
                    Trang {productPage + 1} / {productTotalPages}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setProductPage((p) => Math.min(productTotalPages - 1, p + 1))
                    }
                    disabled={
                      productPage >= productTotalPages - 1 || isProductsLoading
                    }
                    className="p-1.5 rounded-lg border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-xs transition-colors"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: LỊCH SỬ BIẾN ĐỘNG KHO TỪ API (Checklist #4 - Inventory Transactions) */}
      {/* ========================================================================= */}
      {activeTab === 'transactions' && (
        <div className="space-y-5 animate-in fade-in duration-150">
          {/* Thanh công cụ Bộ lọc Lịch sử */}
          <div className="bg-white p-4 rounded-2xl border border-zinc-200 shadow-xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3 flex-wrap flex-1">
              {/* Lọc theo loại giao dịch */}
              <div className="relative min-w-[200px]">
                <Filter className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <select
                  value={txTypeFilter}
                  onChange={(e) => {
                    setTxTypeFilter(e.target.value)
                    setTxPage(0)
                  }}
                  className="w-full pl-9 pr-3.5 py-2 text-xs sm:text-sm bg-zinc-50 border border-zinc-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-zinc-950/20 focus:border-zinc-900 transition-all text-zinc-900 cursor-pointer appearance-none"
                >
                  <option value="">Tất cả loại biến động</option>
                  <option value="MANUAL_IMPORT">Admin nhập kho</option>
                  <option value="MANUAL_EXPORT">Admin xuất kho</option>
                  <option value="ORDER_PLACED">Khách đặt hàng</option>
                  <option value="ORDER_CANCELLED">Hủy đơn hoàn kho</option>
                  <option value="MANUAL_ADJUST">Điều chỉnh thủ công</option>
                  <option value="INITIAL_STOCK">Khởi tạo kho ban đầu</option>
                </select>
              </div>

              {/* Tag lọc riêng sản phẩm nếu có */}
              {txProductFilter && (
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs font-semibold animate-in fade-in">
                  <span>
                    Lọc SP: #{txProductFilter.id} &ldquo;{txProductFilter.name}&rdquo;
                  </span>
                  <button
                    type="button"
                    onClick={handleClearTxProductFilter}
                    className="p-0.5 rounded hover:bg-amber-200/60 text-amber-800 transition-colors cursor-pointer"
                    title="Xóa lọc theo sản phẩm để xem toàn bộ"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>

            {/* Nút làm mới lịch sử */}
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={fetchTransactions}
                disabled={isTxLoading}
                className="inline-flex items-center gap-2 px-3.5 py-2 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-xl text-xs sm:text-sm font-semibold transition-colors disabled:opacity-50 cursor-pointer"
              >
                <RefreshCw
                  className={`w-4 h-4 ${isTxLoading ? 'animate-spin' : ''}`}
                />
                <span>Tải lại</span>
              </button>
            </div>
          </div>

          {/* Bảng Nhật ký Biến động Kho */}
          <div className="bg-white rounded-2xl border border-zinc-200 shadow-xs overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-zinc-50/80 border-b border-zinc-200 text-[11px] font-bold text-zinc-500 uppercase tracking-wider">
                    <th className="py-3.5 px-4 w-16">Mã GD</th>
                    <th className="py-3.5 px-4 min-w-[150px]">Thời gian</th>
                    <th className="py-3.5 px-4 min-w-[200px]">Sản phẩm</th>
                    <th className="py-3.5 px-4 min-w-[160px]">Loại biến động</th>
                    <th className="py-3.5 px-4 text-center">Thay đổi</th>
                    <th className="py-3.5 px-4 text-center">Tồn sau GD</th>
                    <th className="py-3.5 px-4 text-center">Mã đơn</th>
                    <th className="py-3.5 px-4 min-w-[220px]">Lý do / Ghi chú</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-200/70 text-xs sm:text-sm">
                  {isTxLoading ? (
                    Array.from({ length: 5 }).map((_, idx) => (
                      <tr key={idx} className="animate-pulse">
                        <td className="py-3.5 px-4">
                          <div className="h-4 bg-zinc-200 rounded w-8"></div>
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="h-4 bg-zinc-200 rounded w-28"></div>
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="h-4 bg-zinc-200 rounded w-36"></div>
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="h-5 bg-zinc-200 rounded w-28"></div>
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          <div className="h-5 bg-zinc-200 rounded w-12 mx-auto"></div>
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          <div className="h-5 bg-zinc-200 rounded w-10 mx-auto"></div>
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          <div className="h-4 bg-zinc-200 rounded w-8 mx-auto"></div>
                        </td>
                        <td className="py-3.5 px-4">
                          <div className="h-4 bg-zinc-200 rounded w-44"></div>
                        </td>
                      </tr>
                    ))
                  ) : transactions.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="py-12 text-center text-zinc-500">
                        <div className="flex flex-col items-center justify-center gap-2">
                          <History className="w-10 h-10 text-zinc-300 stroke-[1.25]" />
                          <p className="text-sm font-semibold text-zinc-800">
                            Chưa có dữ liệu biến động kho nào
                          </p>
                          <p className="text-xs text-zinc-400">
                            Các giao dịch nhập, xuất kho hoặc khách đặt hàng sẽ được lưu tại đây
                          </p>
                          {txProductFilter && (
                            <button
                              type="button"
                              onClick={handleClearTxProductFilter}
                              className="mt-2 px-3 py-1.5 text-xs font-semibold rounded-lg bg-zinc-100 hover:bg-zinc-200 text-zinc-800 transition-colors cursor-pointer"
                            >
                              Xem toàn bộ lịch sử hệ thống
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    transactions.map((tx) => {
                      const typeConfig =
                        TRANSACTION_TYPE_CONFIG[tx.transactionType] || {
                          label: tx.transactionTypeDescription || tx.transactionType,
                          badgeClass: 'bg-zinc-100 text-zinc-800 border-zinc-200',
                          dotClass: 'bg-zinc-500',
                        }

                      const isPositive = (tx.changeQuantity ?? 0) > 0

                      return (
                        <tr key={tx.id} className="hover:bg-zinc-50/80 transition-colors">
                          {/* Mã GD */}
                          <td className="py-3.5 px-4 font-mono text-xs font-semibold text-zinc-400">
                            #{tx.id}
                          </td>

                          {/* Thời gian */}
                          <td className="py-3.5 px-4 text-xs text-zinc-600 font-mono tabular-nums">
                            <div className="flex items-center gap-1.5">
                              <Clock className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                              <span>{formatDateTime(tx.createdAt)}</span>
                            </div>
                          </td>

                          {/* Sản phẩm */}
                          <td className="py-3.5 px-4">
                            <div className="font-semibold text-zinc-900 truncate max-w-[240px]">
                              {tx.productName || `Sản phẩm #${tx.productId}`}
                            </div>
                            <span className="text-[11px] font-mono text-zinc-400">
                              Mã SP: #{tx.productId}
                            </span>
                          </td>

                          {/* Loại biến động */}
                          <td className="py-3.5 px-4">
                            <span
                              className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-xs font-medium border ${typeConfig.badgeClass}`}
                            >
                              <span
                                className={`w-1.5 h-1.5 rounded-full ${typeConfig.dotClass}`}
                              ></span>
                              <span>{typeConfig.label}</span>
                            </span>
                          </td>

                          {/* Số lượng thay đổi */}
                          <td className="py-3.5 px-4 text-center">
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded-md font-mono text-xs font-bold tabular-nums border ${
                                isPositive
                                  ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                  : 'bg-rose-50 text-rose-700 border-rose-200'
                              }`}
                            >
                              {isPositive
                                ? `+${tx.changeQuantity}`
                                : tx.changeQuantity}
                            </span>
                          </td>

                          {/* Tồn sau GD */}
                          <td className="py-3.5 px-4 text-center font-mono font-bold text-zinc-900 tabular-nums">
                            {tx.stockAfter ?? '-'}
                          </td>

                          {/* Mã đơn hàng */}
                          <td className="py-3.5 px-4 text-center font-mono text-xs">
                            {tx.orderId ? (
                              <span className="font-semibold text-indigo-600 bg-indigo-50 border border-indigo-200 px-1.5 py-0.5 rounded">
                                #{tx.orderId}
                              </span>
                            ) : (
                              <span className="text-zinc-400">-</span>
                            )}
                          </td>

                          {/* Ghi chú / Lý do */}
                          <td className="py-3.5 px-4 text-zinc-600 text-xs">
                            <div className="max-w-xs break-words">
                              {tx.note || '-'}
                            </div>
                          </td>
                        </tr>
                      )
                    })
                  )}
                </tbody>
              </table>
            </div>

            {/* Phân trang Bảng Lịch sử */}
            {txTotalPages > 1 && (
              <div className="px-4 py-3.5 border-t border-zinc-200 bg-zinc-50/50 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-zinc-500">
                <div>
                  Hiển thị{' '}
                  <strong className="text-zinc-900 font-mono">
                    {txPage * txPageSize + 1}
                  </strong>{' '}
                  -{' '}
                  <strong className="text-zinc-900 font-mono">
                    {Math.min((txPage + 1) * txPageSize, totalTransactions)}
                  </strong>{' '}
                  trong tổng số{' '}
                  <strong className="text-zinc-900 font-mono">
                    {totalTransactions}
                  </strong>{' '}
                  giao dịch
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setTxPage((p) => Math.max(0, p - 1))}
                    disabled={txPage === 0 || isTxLoading}
                    className="p-1.5 rounded-lg border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-xs transition-colors"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <span className="px-2 font-mono font-medium text-zinc-700">
                    Trang {txPage + 1} / {txTotalPages}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setTxPage((p) => Math.min(txTotalPages - 1, p + 1))
                    }
                    disabled={txPage >= txTotalPages - 1 || isTxLoading}
                    className="p-1.5 rounded-lg border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-xs transition-colors"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 4. Modal Điều chỉnh Tồn kho Thủ công (Checklist #3) */}
      <AdjustStockModal
        isOpen={isAdjustModalOpen}
        onClose={() => {
          setIsAdjustModalOpen(false)
          setSelectedAdjustProduct(null)
        }}
        product={selectedAdjustProduct}
        onSuccess={handleAdjustSuccess}
        onError={(errText) =>
          setToastMessage({ type: 'error', text: errText })
        }
      />
    </div>
  )
}

export default AdminInventoryPage
