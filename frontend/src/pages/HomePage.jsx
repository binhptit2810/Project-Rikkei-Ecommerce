import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { 
  ArrowRight, 
  ShoppingBag, 
  Sparkles, 
  CheckCircle2, 
  Layers, 
  Server, 
  Database, 
  Users, 
  Activity, 
  Plus, 
  ShieldCheck, 
  Truck, 
  RotateCcw,
  ChevronRight
} from 'lucide-react'
import productService from '../services/productService'
import categoryService from '../services/categoryService'
import apiClient from '../services/apiClient'
import { useAuthStore } from '../store/useAuthStore'
import { useCartStore } from '../store/useCartStore'

export const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore()
  const addToCart = useCartStore((state) => state.addToCart)

  const [categories, setCategories] = useState([])
  const [featuredProducts, setFeaturedProducts] = useState([])
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [healthStatus, setHealthStatus] = useState(null)
  const [healthLoading, setHealthLoading] = useState(false)
  const [healthError, setHealthError] = useState(null)
  const [addedToast, setAddedToast] = useState(null)

  // Tải danh mục và sản phẩm nổi bật từ API thật
  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        const catRes = await categoryService.getCategories()
        if (catRes.success && catRes.data) {
          setCategories(catRes.data.slice(0, 5))
        }
      } catch (err) {
        console.error('Không thể tải danh mục trang chủ:', err)
      }

      try {
        setLoadingProducts(true)
        const prodRes = await productService.getProducts({ size: 8, sortBy: 'createdAt', sortDir: 'desc' })
        if (prodRes.success && prodRes.data?.content) {
          setFeaturedProducts(prodRes.data.content)
        }
      } catch (err) {
        console.error('Không thể tải sản phẩm trang chủ:', err)
      } finally {
        setLoadingProducts(false)
      }
    }

    fetchHomeData()
  }, [])

  const checkBackendHealth = async () => {
    setHealthLoading(true)
    setHealthError(null)
    try {
      const response = await apiClient.get('/health')
      setHealthStatus(response.data)
    } catch (err) {
      try {
        const fallbackRes = await apiClient.get('/v1/health')
        setHealthStatus(fallbackRes.data)
      } catch (fallbackErr) {
        setHealthError(fallbackErr.message || 'Không thể kết nối đến Backend')
      }
    } finally {
      setHealthLoading(false)
    }
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleQuickAdd = async (e, product) => {
    e.preventDefault()
    e.stopPropagation()
    if (!product || product.stockQuantity <= 0) return

    const res = await addToCart(product, 1)
    if (res?.success) {
      setAddedToast(`Đã thêm "${product.name}" vào giỏ hàng`)
      setTimeout(() => setAddedToast(null), 3000)
    }
  }

  const teamMembers = [
    { stt: 1, name: 'Cao Chí Thiện', role: 'Team Lead / Backend Core', task: 'Auth, User Profile, Security & Tích hợp' },
    { stt: 2, name: 'Đỗ Đức Mạnh', role: 'Backend + Frontend', task: 'Product, Category, Search' },
    { stt: 3, name: 'Vũ Đức Huy Hoàng', role: 'Backend + Frontend', task: 'Cart, Order, Checkout & Trừ kho' },
    { stt: 4, name: 'Đặng Thanh Bình', role: 'Backend + Frontend', task: 'Inventory, Payment, Admin UI' },
    { stt: 5, name: 'Đỗ Hoàng Anh', role: 'Frontend + DevOps', task: 'Layout, Routing, Docker & CI/CD' },
  ]

  return (
    <div className="flex-1 w-full bg-slate-50/50">
      {/* Toast thông báo thêm giỏ hàng thành công */}
      {addedToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-900 text-white text-xs font-medium px-4 py-3 rounded-xl shadow-lg border border-slate-800 flex items-center gap-2.5 animate-in slide-in-from-bottom-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{addedToast}</span>
        </div>
      )}

      {/* Hero Section: Editorial & Clean Commerce */}
      <section className="relative overflow-hidden bg-white border-b border-slate-200/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-20 lg:py-24">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            {/* Cột trái: Văn bản & CTA (7 cols) */}
            <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-orange-50 border border-orange-200/60 text-primary-700 text-xs font-semibold">
                <Sparkles className="w-3.5 h-3.5" />
                <span>Nền tảng mua sắm trực tuyến chọn lọc</span>
              </div>

              <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-[1.15]">
                Mua sắm hiện đại, <br />
                <span className="text-primary-600">chất lượng vượt trội.</span>
              </h1>

              <p className="text-sm sm:text-base text-slate-600 max-w-xl mx-auto lg:mx-0 leading-relaxed">
                Khám phá hệ sinh thái sản phẩm phong phú từ thời trang, công nghệ đến gia dụng và đời sống. Cam kết 100% chính hãng và bảo hành tin cậy.
              </p>

              <div className="flex flex-wrap items-center justify-center lg:justify-start gap-3 pt-2">
                <Link
                  to="/products"
                  className="btn-primary px-6 py-3 text-sm font-semibold shadow-md shadow-primary-600/20"
                >
                  <span>Khám phá sản phẩm</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>

                {isAuthenticated ? (
                  <Link
                    to="/orders"
                    className="btn-secondary px-5 py-3 text-sm font-medium"
                  >
                    <span>Xem đơn hàng của bạn</span>
                  </Link>
                ) : (
                  <Link
                    to="/register"
                    className="btn-secondary px-5 py-3 text-sm font-medium"
                  >
                    <span>Tạo tài khoản mới</span>
                  </Link>
                )}
              </div>

              {/* Cam kết nhanh */}
              <div className="pt-6 border-t border-slate-100 flex flex-wrap items-center justify-center lg:justify-start gap-6 text-xs text-slate-500">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Miễn phí ship từ 500k</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Đổi trả trong 7 ngày</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Hỗ trợ khách hàng 24/7</span>
                </div>
              </div>
            </div>

            {/* Cột phải: Visual Showcase Banner (5 cols) */}
            <div className="lg:col-span-5 relative">
              <div className="relative mx-auto max-w-md lg:max-w-none">
                <div className="rounded-3xl overflow-hidden shadow-2xl border border-slate-200/80 bg-slate-100 aspect-[4/5] relative group">
                  <img
                    src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&auto=format&fit=crop&q=80"
                    alt="Store Collection"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-slate-950/20 to-transparent flex flex-col justify-end p-6 sm:p-8 text-white">
                    <span className="text-xs uppercase font-bold tracking-widest text-primary-400">
                      Bộ sưu tập 2026
                    </span>
                    <h3 className="text-xl font-bold mt-1 text-white">
                      Thời trang & Thiết bị công nghệ
                    </h3>
                    <p className="text-xs text-slate-300 mt-1 line-clamp-2">
                      Sản phẩm chuẩn hóa chất lượng, đa dạng mẫu mã và giá trị bền bỉ.
                    </p>
                  </div>
                </div>

                {/* Floating pill badge */}
                <div className="absolute -bottom-4 -left-4 sm:bottom-6 sm:-left-6 bg-white/95 backdrop-blur-md p-3.5 rounded-2xl shadow-dropdown border border-slate-200/80 flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-primary-100 text-primary-700 flex items-center justify-center shrink-0">
                    <ShoppingBag className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900">Sẵn sàng giao ngay</div>
                    <div className="text-[10px] text-slate-500">Hàng trăm mẫu mã cập nhật</div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Featured Categories Grid */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="flex items-end justify-between mb-8">
          <div>
            <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Danh mục nổi bật</h2>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">Khám phá các nhóm sản phẩm được quan tâm nhiều nhất</p>
          </div>
          <Link
            to="/products"
            className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 group"
          >
            <span>Xem tất cả</span>
            <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-6">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/products?categoryId=${cat.id}`}
              className="group bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-subtle hover:shadow-card hover:-translate-y-1 transition-all flex flex-col"
            >
              <div className="aspect-square bg-slate-100 overflow-hidden relative">
                {cat.imageUrl ? (
                  <img
                    src={cat.imageUrl}
                    alt={cat.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-slate-400">
                    <ShoppingBag className="w-8 h-8" />
                  </div>
                )}
              </div>
              <div className="p-4 text-center">
                <h3 className="font-bold text-sm text-slate-900 group-hover:text-primary-600 transition-colors">
                  {cat.name}
                </h3>
                <p className="text-[11px] text-slate-500 line-clamp-1 mt-0.5">
                  {cat.description || 'Xem sản phẩm'}
                </p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* Featured Products Showcase */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
        <div className="flex items-end justify-between mb-8">
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-primary-600">Gợi ý hôm nay</div>
            <h2 className="text-2xl font-bold text-slate-900 tracking-tight mt-0.5">Sản phẩm mới nhất</h2>
          </div>
          <Link
            to="/products"
            className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 group"
          >
            <span>Tất cả sản phẩm</span>
            <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        {loadingProducts ? (
          <div className="py-16 text-center">
            <div className="w-8 h-8 border-3 border-primary-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-xs text-slate-500">Đang tải sản phẩm nổi bật...</p>
          </div>
        ) : featuredProducts.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
            <p className="text-xs text-slate-500">Chưa có sản phẩm nào được hiển thị.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {featuredProducts.map((prod) => {
              const isOutOfStock = prod.stockQuantity <= 0

              return (
                <div
                  key={prod.id}
                  className="group bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-subtle hover:shadow-card hover:-translate-y-1 transition-all flex flex-col justify-between"
                >
                  {/* Image & Badges */}
                  <div className="relative aspect-square bg-slate-100 overflow-hidden">
                    <Link to={`/products/${prod.id}`} className="block w-full h-full">
                      {prod.imageUrl ? (
                        <img
                          src={prod.imageUrl}
                          alt={prod.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-300">
                          <ShoppingBag className="w-12 h-12" />
                        </div>
                      )}
                    </Link>

                    {/* Category Tag */}
                    {prod.categoryName && (
                      <span className="absolute top-2.5 left-2.5 bg-white/90 backdrop-blur-xs text-slate-800 text-[10px] font-semibold px-2 py-0.5 rounded-md shadow-2xs">
                        {prod.categoryName}
                      </span>
                    )}

                    {/* Stock Tag if Out of Stock */}
                    {isOutOfStock && (
                      <span className="absolute top-2.5 right-2.5 bg-slate-900/80 text-white text-[10px] font-semibold px-2 py-0.5 rounded-md">
                        Hết hàng
                      </span>
                    )}
                  </div>

                  {/* Info & Action */}
                  <div className="p-4 flex-1 flex flex-col justify-between">
                    <div>
                      <Link
                        to={`/products/${prod.id}`}
                        className="font-semibold text-xs sm:text-sm text-slate-900 hover:text-primary-600 line-clamp-2 transition-colors"
                      >
                        {prod.name}
                      </Link>
                      <div className="mt-2 text-sm sm:text-base font-bold text-slate-900 tabular-nums">
                        {formatPrice(prod.price)}
                      </div>
                    </div>

                    <div className="pt-3 mt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                      <span className="text-[11px] text-slate-400">
                        {isOutOfStock ? 'Hết hàng' : `Còn: ${prod.stockQuantity}`}
                      </span>

                      <button
                        type="button"
                        onClick={(e) => handleQuickAdd(e, prod)}
                        disabled={isOutOfStock}
                        className="p-2 rounded-xl bg-slate-100 hover:bg-primary-600 text-slate-700 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                        title={isOutOfStock ? 'Hết hàng' : 'Thêm nhanh vào giỏ'}
                      >
                        <Plus className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </section>

      {/* Promotional Banner */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="rounded-3xl bg-slate-900 text-white p-8 sm:p-12 relative overflow-hidden flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="max-w-xl space-y-3 text-center md:text-left z-10">
            <span className="text-xs font-bold uppercase tracking-wider text-primary-400">
              Ưu đãi đặc biệt
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Giao hàng hỏa tốc trong 24 giờ
            </h2>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              Áp dụng miễn phí vận chuyển cho tất cả đơn hàng từ 500.000 ₫. Đổi trả miễn phí không cần lý do trong vòng 7 ngày.
            </p>
          </div>
          <Link
            to="/products"
            className="btn-primary px-6 py-3 text-sm font-semibold shrink-0 z-10"
          >
            Mua sắm ngay
          </Link>
        </div>
      </section>

      {/* Project Academic Info & Backend Diagnostics (Refined clean section) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-slate-200/80">
        <div className="bg-white rounded-2xl border border-slate-200/80 p-6 sm:p-8 shadow-subtle space-y-8">
          
          {/* Header of Project section */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-primary-600"></span>
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Thông tin dự án môn học</span>
              </div>
              <h3 className="text-lg font-bold text-slate-900 mt-1">
                E-Commerce Platform Architecture & Team Assignment
              </h3>
            </div>

            <button
              type="button"
              onClick={checkBackendHealth}
              disabled={healthLoading}
              className="btn-secondary text-xs self-start sm:self-auto"
            >
              <Activity className="w-3.5 h-3.5 text-primary-600" />
              <span>{healthLoading ? 'Đang kiểm tra...' : 'Kiểm tra trạng thái Backend API'}</span>
            </button>
          </div>

          {/* Backend Diagnostics Result */}
          {healthError && (
            <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs">
              <span className="font-bold">Chưa kết nối Backend:</span> {healthError}
              <div className="text-slate-500 mt-1">Đảm bảo backend Spring Boot đang chạy tại cổng 8080.</div>
            </div>
          )}

          {healthStatus && (
            <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs">
              <div className="font-bold text-emerald-800 flex items-center gap-1.5 mb-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Backend đang hoạt động bình thường (Status: UP)</span>
              </div>
              <pre className="p-2.5 bg-white/90 rounded-lg border border-emerald-100 font-mono text-[11px] overflow-x-auto text-slate-700">
                {JSON.stringify(healthStatus, null, 2)}
              </pre>
            </div>
          )}

          {/* Architecture 3 pillars */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-xs">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/60 space-y-2">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Layers className="w-4 h-4 text-primary-600" />
                <span>Frontend Layer</span>
              </div>
              <p className="text-slate-600">React 18 + Vite + Tailwind CSS + Zustand</p>
              <div className="text-slate-500 text-[11px]">Route guards, token refresh interceptor, client-side cart storage.</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/60 space-y-2">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Server className="w-4 h-4 text-primary-600" />
                <span>Backend Core</span>
              </div>
              <p className="text-slate-600">Spring Boot 3.3.5 + Java 21 + Spring Security</p>
              <div className="text-slate-500 text-[11px]">Stateless JWT JJWT 0.12.6, transaction isolation, global error handler.</div>
            </div>

            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/60 space-y-2">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Database className="w-4 h-4 text-primary-600" />
                <span>Database & Cache</span>
              </div>
              <p className="text-slate-600">MySQL 8.0 & Redis 7 (Docker Compose)</p>
              <div className="text-slate-500 text-[11px]">Khóa ngoại an toàn, kiểm tra tồn kho, lưu trữ phiên đăng nhập.</div>
            </div>
          </div>

          {/* Team Work Allocation Table */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-2">
              <Users className="w-4 h-4 text-slate-600" />
              <span>Phân chia công việc thành viên nhóm</span>
            </h4>
            <div className="overflow-x-auto rounded-xl border border-slate-200/80">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-700 font-bold uppercase border-b border-slate-200/80">
                  <tr>
                    <th className="py-2.5 px-3 w-12 text-center">STT</th>
                    <th className="py-2.5 px-3">Thành viên</th>
                    <th className="py-2.5 px-3">Vai trò</th>
                    <th className="py-2.5 px-3">Phụ trách Module</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {teamMembers.map((m) => (
                    <tr key={m.stt} className="hover:bg-slate-50/60">
                      <td className="py-2.5 px-3 text-center text-slate-400 font-medium">{m.stt}</td>
                      <td className="py-2.5 px-3 font-semibold text-slate-900">{m.name}</td>
                      <td className="py-2.5 px-3 text-slate-600">{m.role}</td>
                      <td className="py-2.5 px-3 text-primary-700 font-medium">{m.task}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

        </div>
      </section>
    </div>
  )
}

export default HomePage
