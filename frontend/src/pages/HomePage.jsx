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
  ChevronRight
} from 'lucide-react'
import productService from '../services/productService'
import categoryService from '../services/categoryService'
import apiClient from '../services/apiClient'
import { useAuthStore } from '../store/useAuthStore'
import { useCartStore } from '../store/useCartStore'

export const HomePage = () => {
  const { isAuthenticated } = useAuthStore()
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
    <div className="flex-1 w-full bg-white">
      {/* Toast thông báo thêm giỏ hàng thành công */}
      {addedToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-zinc-900 text-zinc-50 text-xs font-medium px-4 py-2.5 rounded-md shadow-md border border-zinc-800 flex items-center gap-2 animate-in slide-in-from-bottom-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{addedToast}</span>
        </div>
      )}

      {/* Hero Section: Editorial & Minimalist E-Commerce */}
      <section className="border-b border-zinc-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-20 lg:py-24">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            {/* Cột trái: Văn bản & CTA (7 cols) */}
            <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-zinc-100 border border-zinc-200 text-zinc-800 text-xs font-medium">
                <Sparkles className="w-3.5 h-3.5 text-zinc-600" />
                <span>Nền tảng mua sắm trực tuyến chọn lọc</span>
              </div>

              <h1 className="text-3xl sm:text-5xl lg:text-6xl font-semibold text-zinc-950 tracking-tight leading-[1.12]">
                Mua sắm hiện đại, <br />
                <span className="text-zinc-500 font-normal">chất lượng vượt trội.</span>
              </h1>

              <p className="text-sm sm:text-base text-zinc-600 max-w-xl mx-auto lg:mx-0 leading-relaxed">
                Khám phá hệ sinh thái sản phẩm phong phú từ thời trang, công nghệ đến gia dụng và đời sống. Cam kết 100% chính hãng và bảo hành tin cậy.
              </p>

              <div className="flex flex-wrap items-center justify-center lg:justify-start gap-3 pt-2">
                <Link
                  to="/products"
                  className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-md text-sm font-medium text-zinc-50 bg-zinc-900 hover:bg-zinc-800 transition-colors shadow-sm"
                >
                  <span>Khám phá sản phẩm</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>

                {isAuthenticated ? (
                  <Link
                    to="/orders"
                    className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-md text-sm font-medium text-zinc-900 bg-zinc-100 hover:bg-zinc-200/80 transition-colors"
                  >
                    <span>Xem đơn hàng của bạn</span>
                  </Link>
                ) : (
                  <Link
                    to="/register"
                    className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-md text-sm font-medium text-zinc-900 bg-zinc-100 hover:bg-zinc-200/80 transition-colors"
                  >
                    <span>Tạo tài khoản mới</span>
                  </Link>
                )}
              </div>

              {/* Cam kết nhanh */}
              <div className="pt-6 border-t border-zinc-100 flex flex-wrap items-center justify-center lg:justify-start gap-6 text-xs text-zinc-500">
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Miễn phí ship từ 500k</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Đổi trả trong 7 ngày</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Hỗ trợ khách hàng 24/7</span>
                </div>
              </div>
            </div>

            {/* Cột phải: Visual Showcase Banner (5 cols) */}
            <div className="lg:col-span-5 relative">
              <div className="relative mx-auto max-w-md lg:max-w-none">
                <div className="rounded-lg overflow-hidden border border-zinc-200 bg-zinc-100 aspect-[4/5] relative group shadow-sm">
                  <img
                    src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&auto=format&fit=crop&q=80"
                    alt="Store Collection"
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-zinc-950/80 via-zinc-950/20 to-transparent flex flex-col justify-end p-6 text-white">
                    <span className="text-xs uppercase font-medium tracking-wider text-zinc-300">
                      Bộ sưu tập 2026
                    </span>
                    <h3 className="text-lg font-semibold mt-1 text-white">
                      Thời trang & Thiết bị công nghệ
                    </h3>
                    <p className="text-xs text-zinc-300 mt-1 line-clamp-2">
                      Sản phẩm chuẩn hóa chất lượng, đa dạng mẫu mã và giá trị bền bỉ.
                    </p>
                  </div>
                </div>

                {/* Floating badge */}
                <div className="absolute -bottom-3 -left-3 sm:bottom-4 sm:-left-4 bg-white p-3 rounded-lg shadow-sm border border-zinc-200 flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-md bg-zinc-100 text-zinc-900 flex items-center justify-center shrink-0 border border-zinc-200">
                    <ShoppingBag className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-zinc-900">Sẵn sàng giao ngay</div>
                    <div className="text-[10px] text-zinc-500">Hàng trăm mẫu mã cập nhật</div>
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
            <h2 className="text-xl sm:text-2xl font-semibold text-zinc-950 tracking-tight">Danh mục nổi bật</h2>
            <p className="text-xs sm:text-sm text-zinc-500 mt-1">Khám phá các nhóm sản phẩm được quan tâm nhiều nhất</p>
          </div>
          <Link
            to="/products"
            className="text-xs font-medium text-zinc-700 hover:text-zinc-950 flex items-center gap-1 group transition-colors"
          >
            <span>Xem tất cả</span>
            <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/products?categoryId=${cat.id}`}
              className="group bg-white rounded-lg border border-zinc-200 overflow-hidden shadow-sm hover:border-zinc-300 hover:shadow-md transition-all flex flex-col"
            >
              <div className="aspect-square bg-zinc-50 overflow-hidden relative border-b border-zinc-100">
                {cat.imageUrl ? (
                  <img
                    src={cat.imageUrl}
                    alt={cat.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-zinc-300">
                    <ShoppingBag className="w-8 h-8" />
                  </div>
                )}
              </div>
              <div className="p-3.5 text-center">
                <h3 className="font-semibold text-sm text-zinc-900 group-hover:underline transition-colors">
                  {cat.name}
                </h3>
                <p className="text-xs text-zinc-500 line-clamp-1 mt-0.5">
                  {cat.description || 'Xem sản phẩm'}
                </p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* Featured Products Showcase */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 border-t border-zinc-100">
        <div className="flex items-end justify-between mb-8">
          <div>
            <div className="text-xs font-medium uppercase tracking-wider text-zinc-500">Gợi ý hôm nay</div>
            <h2 className="text-xl sm:text-2xl font-semibold text-zinc-950 tracking-tight mt-0.5">Sản phẩm mới nhất</h2>
          </div>
          <Link
            to="/products"
            className="text-xs font-medium text-zinc-700 hover:text-zinc-950 flex items-center gap-1 group transition-colors"
          >
            <span>Tất cả sản phẩm</span>
            <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
          </Link>
        </div>

        {loadingProducts ? (
          <div className="py-16 text-center">
            <div className="w-6 h-6 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-xs text-zinc-500">Đang tải sản phẩm nổi bật...</p>
          </div>
        ) : featuredProducts.length === 0 ? (
          <div className="bg-white rounded-lg border border-zinc-200 p-12 text-center">
            <p className="text-xs text-zinc-500">Chưa có sản phẩm nào được hiển thị.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
            {featuredProducts.map((prod) => {
              const isOutOfStock = prod.stockQuantity <= 0

              return (
                <div
                  key={prod.id}
                  className="group bg-white rounded-lg border border-zinc-200 overflow-hidden shadow-sm hover:border-zinc-300 hover:shadow-md transition-all flex flex-col justify-between"
                >
                  {/* Image & Badges */}
                  <div className="relative aspect-square bg-zinc-50 overflow-hidden border-b border-zinc-100">
                    <Link to={`/products/${prod.id}`} className="block w-full h-full">
                      {prod.imageUrl ? (
                        <img
                          src={prod.imageUrl}
                          alt={prod.name}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-zinc-300">
                          <ShoppingBag className="w-10 h-10" />
                        </div>
                      )}
                    </Link>

                    {/* Category Tag */}
                    {prod.categoryName && (
                      <span className="absolute top-2.5 left-2.5 bg-white/95 text-zinc-700 text-[10px] font-medium px-2 py-0.5 rounded-md border border-zinc-200 shadow-2xs">
                        {prod.categoryName}
                      </span>
                    )}

                    {/* Stock Tag if Out of Stock */}
                    {isOutOfStock && (
                      <span className="absolute top-2.5 right-2.5 bg-zinc-900 text-zinc-50 text-[10px] font-medium px-2 py-0.5 rounded-md uppercase tracking-wider">
                        Hết hàng
                      </span>
                    )}
                  </div>

                  {/* Info & Action */}
                  <div className="p-4 flex-1 flex flex-col justify-between">
                    <div>
                      <Link
                        to={`/products/${prod.id}`}
                        className="font-semibold text-xs sm:text-sm text-zinc-900 group-hover:underline line-clamp-2 transition-colors"
                      >
                        {prod.name}
                      </Link>
                      <div className="mt-1 text-sm sm:text-base font-semibold text-zinc-950 tabular-nums">
                        {formatPrice(prod.price)}
                      </div>
                    </div>

                    <div className="pt-3 mt-3 border-t border-zinc-100 flex items-center justify-between gap-2">
                      <span className="text-[11px] text-zinc-400">
                        {isOutOfStock ? 'Hết hàng' : `Còn: ${prod.stockQuantity}`}
                      </span>

                      <button
                        type="button"
                        onClick={(e) => handleQuickAdd(e, prod)}
                        disabled={isOutOfStock}
                        className="p-1.5 rounded-md bg-zinc-100 hover:bg-zinc-900 text-zinc-700 hover:text-zinc-50 disabled:opacity-30 disabled:pointer-events-none transition-colors cursor-pointer border border-zinc-200 hover:border-zinc-900"
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
        <div className="rounded-lg bg-zinc-900 text-zinc-50 p-8 sm:p-10 relative overflow-hidden flex flex-col md:flex-row items-center justify-between gap-6 border border-zinc-800">
          <div className="max-w-xl space-y-2 text-center md:text-left z-10">
            <span className="text-xs font-medium uppercase tracking-wider text-zinc-400">
              Ưu đãi vận chuyển
            </span>
            <h2 className="text-2xl sm:text-3xl font-semibold tracking-tight text-white">
              Giao hàng hỏa tốc toàn quốc
            </h2>
            <p className="text-xs sm:text-sm text-zinc-400 leading-relaxed">
              Áp dụng miễn phí vận chuyển cho tất cả đơn hàng từ 500.000 ₫. Đổi trả miễn phí không cần lý do trong vòng 7 ngày.
            </p>
          </div>
          <Link
            to="/products"
            className="inline-flex items-center justify-center px-5 py-2.5 rounded-md text-sm font-medium bg-zinc-100 text-zinc-900 hover:bg-white transition-colors shrink-0 z-10"
          >
            Mua sắm ngay
          </Link>
        </div>
      </section>

      {/* Project Academic Info & Backend Diagnostics */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 border-t border-zinc-200">
        <div className="bg-white rounded-lg border border-zinc-200 p-6 sm:p-8 shadow-sm space-y-8">
          
          {/* Header of Project section */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-zinc-100">
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-zinc-900"></span>
                <span className="text-xs font-medium text-zinc-500 uppercase tracking-wider">Thông tin dự án</span>
              </div>
              <h3 className="text-lg font-semibold text-zinc-950 mt-1">
                E-Commerce Platform Architecture & Team Assignment
              </h3>
            </div>

            <button
              type="button"
              onClick={checkBackendHealth}
              disabled={healthLoading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-zinc-100 hover:bg-zinc-200/80 text-zinc-900 border border-zinc-200 transition-colors cursor-pointer self-start sm:self-auto"
            >
              <Activity className="w-3.5 h-3.5 text-zinc-700" />
              <span>{healthLoading ? 'Đang kiểm tra...' : 'Kiểm tra trạng thái Backend'}</span>
            </button>
          </div>

          {/* Backend Diagnostics Result */}
          {healthError && (
            <div className="p-3 rounded-md bg-zinc-100 border border-zinc-200 text-zinc-800 text-xs">
              <span className="font-semibold">Chưa kết nối Backend:</span> {healthError}
              <div className="text-zinc-500 mt-1">Đảm bảo backend Spring Boot đang chạy tại cổng 8080.</div>
            </div>
          )}

          {healthStatus && (
            <div className="p-3 rounded-md bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs">
              <div className="font-semibold text-emerald-800 flex items-center gap-1.5 mb-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Backend đang hoạt động bình thường (Status: UP)</span>
              </div>
              <pre className="p-2.5 bg-white rounded border border-emerald-200 font-mono text-[11px] overflow-x-auto text-zinc-700">
                {JSON.stringify(healthStatus, null, 2)}
              </pre>
            </div>
          )}

          {/* Architecture 3 pillars */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
            <div className="p-4 rounded-md bg-zinc-50 border border-zinc-200 space-y-1.5">
              <div className="flex items-center gap-2 font-semibold text-zinc-950 text-sm">
                <Layers className="w-4 h-4 text-zinc-700" />
                <span>Frontend Layer</span>
              </div>
              <p className="text-zinc-600">React 18 + Vite + Tailwind CSS + Zustand</p>
              <div className="text-zinc-500 text-[11px]">Route guards, token refresh interceptor, client-side cart storage.</div>
            </div>

            <div className="p-4 rounded-md bg-zinc-50 border border-zinc-200 space-y-1.5">
              <div className="flex items-center gap-2 font-semibold text-zinc-950 text-sm">
                <Server className="w-4 h-4 text-zinc-700" />
                <span>Backend Core</span>
              </div>
              <p className="text-zinc-600">Spring Boot 3.3.5 + Java 21 + Spring Security</p>
              <div className="text-zinc-500 text-[11px]">Stateless JWT JJWT 0.12.6, transaction isolation, global error handler.</div>
            </div>

            <div className="p-4 rounded-md bg-zinc-50 border border-zinc-200 space-y-1.5">
              <div className="flex items-center gap-2 font-semibold text-zinc-950 text-sm">
                <Database className="w-4 h-4 text-zinc-700" />
                <span>Database & Cache</span>
              </div>
              <p className="text-zinc-600">MySQL 8.0 & Redis 7 (Docker Compose)</p>
              <div className="text-zinc-500 text-[11px]">Khóa ngoại an toàn, kiểm tra tồn kho, lưu trữ phiên đăng nhập.</div>
            </div>
          </div>

          {/* Team Work Allocation Table */}
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-zinc-500 mb-3 flex items-center gap-2">
              <Users className="w-3.5 h-3.5 text-zinc-700" />
              <span>Phân chia công việc thành viên nhóm</span>
            </h4>
            <div className="overflow-x-auto rounded-md border border-zinc-200">
              <table className="w-full text-left text-xs">
                <thead className="bg-zinc-50 text-zinc-700 font-semibold uppercase border-b border-zinc-200">
                  <tr>
                    <th className="py-2.5 px-3 w-12 text-center">STT</th>
                    <th className="py-2.5 px-3">Thành viên</th>
                    <th className="py-2.5 px-3">Vai trò</th>
                    <th className="py-2.5 px-3">Phụ trách Module</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100">
                  {teamMembers.map((m) => (
                    <tr key={m.stt} className="hover:bg-zinc-50/60">
                      <td className="py-2.5 px-3 text-center text-zinc-400 font-medium">{m.stt}</td>
                      <td className="py-2.5 px-3 font-semibold text-zinc-950">{m.name}</td>
                      <td className="py-2.5 px-3 text-zinc-600">{m.role}</td>
                      <td className="py-2.5 px-3 text-zinc-900 font-medium">{m.task}</td>
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
