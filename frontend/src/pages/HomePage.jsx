import React, { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { 
  ArrowRight, 
  ShoppingBag,  
  CheckCircle2, 
  Layers, 
  Server, 
  Database, 
  Users, 
  Activity, 
  Zap,
  Truck,
  RotateCcw,
  ShieldCheck,
  Headphones,
  Laptop,
  Smartphone,
  Watch,
  Gamepad2,
  Clock,
  ArrowUp,
  Star,
  RefreshCw,
  AlertCircle,
  PackageOpen
} from 'lucide-react'
import { motion, useReducedMotion, AnimatePresence } from 'framer-motion'
import productService from '../services/productService'
import categoryService from '../services/categoryService'
import apiClient from '../services/apiClient'
import { useAuthStore } from '../store/useAuthStore'
import { useCartStore } from '../store/useCartStore'
import ProductCard from '../components/common/ProductCard'

// Các thương hiệu đối tác nổi tiếng (Brand Ticker)
const BRAND_PARTNERS = [
  'TECHNOVA', 'RAPTOR', 'ORBIT', 'NEBULA', 'BASSKING', 
  'RIKKEI STORE', 'ZENITH', 'PULSE', 'SONICWAVE', 'CHRONOFIT', 'VORTEX'
]

// Bảng màu & biểu tượng thích ứng danh mục theo phong cách TechNova
const CATEGORY_THEMES = [
  { gradient: 'from-blue-600 to-indigo-950', icon: Laptop, defaultDesc: 'Ultrabooks, creator rigs & gaming beasts' },
  { gradient: 'from-emerald-600 to-teal-950', icon: Smartphone, defaultDesc: 'Flagship cameras & all-day 5G battery' },
  { gradient: 'from-amber-700 to-orange-950', icon: Headphones, defaultDesc: 'Studio sound with active noise cancelling' },
  { gradient: 'from-indigo-700 to-purple-950', icon: Watch, defaultDesc: 'Fitness, health & notifications on wrist' },
  { gradient: 'from-rose-700 to-red-950', icon: Gamepad2, defaultDesc: 'Mechanical keys, low-latency gear & RGB' },
  { gradient: 'from-sky-700 to-blue-950', icon: Server, defaultDesc: 'Thiết bị & phụ kiện công nghệ đời mới' }
]

export const HomePage = () => {
  const { isAuthenticated } = useAuthStore()
  const navigate = useNavigate()
  const addToCart = useCartStore((state) => state.addToCart)
  const shouldReduceMotion = useReducedMotion()

  // Dữ liệu từ database MySQL qua API
  const [categories, setCategories] = useState([])
  const [featuredProducts, setFeaturedProducts] = useState([])
  const [bestSellers, setBestSellers] = useState([])
  const [selectedCategoryTab, setSelectedCategoryTab] = useState('ALL')
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [productsError, setProductsError] = useState(null)

  // Deal of the Day state - 100% từ MySQL & Coupon API thực tế
  const [deal, setDeal] = useState(null)
  const [loadingDeal, setLoadingDeal] = useState(true)
  const [dealError, setDealError] = useState(null)

  // Diagnostics & UI states
  const [healthStatus, setHealthStatus] = useState(null)
  const [healthLoading, setHealthLoading] = useState(false)
  const [healthError, setHealthError] = useState(null)
  const [addedToast, setAddedToast] = useState(null)
  const [showScrollTop, setShowScrollTop] = useState(false)

  // Countdown timer cho khối "Deal of the Day" tính theo endTime thực tế
  const [timeLeft, setTimeLeft] = useState({ hours: 0, minutes: 0, seconds: 0, isExpired: false })

  // 1. Tải danh sách danh mục trực tiếp từ database MySQL
  const fetchCategories = async () => {
    try {
      const catRes = await categoryService.getCategories()
      if (catRes.success && Array.isArray(catRes.data)) {
        setCategories(catRes.data)
      }
    } catch (err) {
      console.error('Không thể tải danh mục trang chủ:', err)
    }
  }

  // 2. Tải sản phẩm cho Featured Products theo categoryId (hoặc ALL) từ Spring Boot API
  const fetchFeaturedProducts = useCallback(async (catId) => {
    setLoadingProducts(true)
    setProductsError(null)
    try {
      const params = { size: 8, sortBy: 'createdAt', sortDir: 'desc' }
      if (catId !== 'ALL') {
        params.categoryId = Number(catId)
      }
      const prodRes = await productService.getProducts(params)
      // Đọc cấu trúc response thực tế an toàn: data.content hoặc data.products hoặc mảng data
      const items = prodRes.data?.content || prodRes.data?.products || (Array.isArray(prodRes.data) ? prodRes.data : [])
      setFeaturedProducts(items)
    } catch (err) {
      console.error('Lỗi khi tải sản phẩm cho danh mục:', err)
      setProductsError(err.message || 'Không thể tải danh sách sản phẩm từ hệ thống.')
      setFeaturedProducts([])
    } finally {
      setLoadingProducts(false)
    }
  }, [])

  // 3. Tải danh sách sản phẩm Best Sellers độc lập (4 sản phẩm)
  const fetchBestSellers = async () => {
    try {
      const res = await productService.getBestSellers({ size: 4 })
      const items = res.data?.content || res.data?.products || (Array.isArray(res.data) ? res.data : [])
      setBestSellers(items)
    } catch (err) {
      console.error('Không thể tải sản phẩm Best Sellers:', err)
    }
  }

  // 4. Tải Deal of the Day từ API backend thật
  const fetchDealOfTheDay = async () => {
    setLoadingDeal(true)
    setDealError(null)
    try {
      const res = await productService.getDealOfTheDay()
      if (res.success && res.data) {
        setDeal(res.data)
      } else {
        setDeal(null)
      }
    } catch (err) {
      console.error('Không thể tải Deal of the Day:', err)
      setDealError('Không thể tải ưu đãi hôm nay. Vui lòng thử lại sau.')
      setDeal(null)
    } finally {
      setLoadingDeal(false)
    }
  }

  // Tải danh mục, best sellers và deal khi mở trang
  useEffect(() => {
    fetchCategories()
    fetchBestSellers()
    fetchDealOfTheDay()
  }, [])

  // Tự động tải lại sản phẩm khi người dùng chọn tab All hoặc chọn danh mục khác
  useEffect(() => {
    fetchFeaturedProducts(selectedCategoryTab)
  }, [selectedCategoryTab, fetchFeaturedProducts])

  // Đếm ngược thời gian thực tế dựa trên deal.endTime từ CSDL
  useEffect(() => {
    if (!deal?.endTime) return

    const updateCountdown = () => {
      const targetTime = new Date(deal.endTime).getTime()
      const now = new Date().getTime()
      const difference = targetTime - now

      if (difference <= 0) {
        setTimeLeft({ hours: 0, minutes: 0, seconds: 0, isExpired: true })
        return
      }

      const hours = Math.floor(difference / (1000 * 60 * 60))
      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60))
      const seconds = Math.floor((difference % (1000 * 60)) / 1000)

      setTimeLeft({ hours, minutes, seconds, isExpired: false })
    }

    updateCountdown()
    const interval = setInterval(updateCountdown, 1000)
    return () => clearInterval(interval)
  }, [deal?.endTime])

  // Theo dõi vị trí cuộn chuột để hiển thị nút Back To Top
  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 400) {
        setShowScrollTop(true)
      } else {
        setShowScrollTop(false)
      }
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
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

  const handleAddToCart = async (product) => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          from: { pathname: '/' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }

    if (!product || product.stockQuantity <= 0) return

    const res = await addToCart(product, 1)
    if (res?.requireLogin) {
      navigate('/login', {
        state: {
          from: { pathname: '/' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
        },
      })
      return
    }
    if (res?.success) {
      setAddedToast(`Đã thêm "${product.name}" vào giỏ hàng`)
      setTimeout(() => setAddedToast(null), 3000)
    }
  }

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Định dạng tiền tệ VNĐ chuẩn xác
  const formatCurrency = (val) => {
    if (val === null || val === undefined) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
  }

  // Animation variants cho Stagger effect của danh sách sản phẩm
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: shouldReduceMotion ? 0 : 0.08,
      },
    },
  }

  const itemVariants = {
    hidden: { opacity: 0, y: shouldReduceMotion ? 0 : 24 },
    visible: {
      opacity: 1,
      y: 0,
      transition: { duration: 0.45, ease: 'easeOut' },
    },
  }

  // Nhóm dự án Rikkei E-commerce
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
      <AnimatePresence>
        {addedToast && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="fixed bottom-6 left-6 z-50 bg-slate-900 text-white text-xs font-semibold px-4 py-3 rounded-full shadow-2xl border border-slate-700 flex items-center gap-2.5"
          >
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>{addedToast}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Floating Scroll to Top Button với Motion animation */}
      <AnimatePresence>
        {showScrollTop && (
          <motion.button
            initial={{ opacity: 0, scale: 0.7 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.7 }}
            whileHover={shouldReduceMotion ? {} : { scale: 1.1 }}
            whileTap={shouldReduceMotion ? {} : { scale: 0.9 }}
            type="button"
            onClick={scrollToTop}
            className="fixed bottom-6 right-6 z-40 w-11 h-11 rounded-full bg-blue-600 hover:bg-blue-700 text-white shadow-xl flex items-center justify-center cursor-pointer transition-colors"
            title="Lên đầu trang"
          >
            <ArrowUp className="w-5 h-5 stroke-[2.2]" />
          </motion.button>
        )}
      </AnimatePresence>

      {/* 1. HERO BANNER: Motion Fade-in & Slide-up (TechNova Image 1) */}
      <section className="bg-gradient-to-b from-[#070d1e] via-[#091536] to-[#0c1c45] text-white pt-12 pb-20 relative overflow-hidden">
        {/* Subtle radial glow background */}
        <div className="absolute top-1/4 left-1/3 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-10 right-10 w-[450px] h-[450px] bg-orange-600/10 rounded-full blur-3xl pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            {/* Cột trái: Headline & CTAs (7 cols) với Motion entrance */}
            <motion.div 
              initial={shouldReduceMotion ? {} : { opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: 'easeOut' }}
              className="lg:col-span-7 space-y-6 text-center lg:text-left"
            >
              {/* Sale Tag */}
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-orange-400 text-xs font-semibold">
                <Zap className="w-3.5 h-3.5 fill-current text-orange-400" />
                <span>Republic Sale live — up to 40% off</span>
              </div>

              {/* Headline */}
              <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight leading-[1.08] text-white">
                Premium tech, <br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 via-amber-400 to-orange-500">
                  honest prices.
                </span>
              </h1>

              {/* Subtitle */}
              <p className="text-slate-300 text-sm sm:text-base max-w-xl mx-auto lg:mx-0 leading-relaxed font-normal">
                Từ creator laptops đến smartphone flagship và gaming gear chuẩn thi đấu — 100% sản phẩm chính hãng, bảo hành toàn diện và giao nhanh trong 48 giờ.
              </p>

              {/* CTA Buttons với hover & tap micro-interactions */}
              <div className="flex flex-wrap items-center justify-center lg:justify-start gap-4 pt-2">
                <motion.div
                  whileHover={shouldReduceMotion ? {} : { scale: 1.04 }}
                  whileTap={shouldReduceMotion ? {} : { scale: 0.96 }}
                >
                  <Link
                    to="/products"
                    className="inline-flex items-center justify-center gap-2.5 px-7 py-3 rounded-full text-sm font-bold text-white bg-orange-500 hover:bg-orange-600 shadow-lg shadow-orange-500/30 transition-colors"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    <span>Shop Now</span>
                  </Link>
                </motion.div>

                <motion.div
                  whileHover={shouldReduceMotion ? {} : { scale: 1.04 }}
                  whileTap={shouldReduceMotion ? {} : { scale: 0.96 }}
                >
                  <Link
                    to="/products"
                    className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-full text-sm font-semibold text-white bg-white/10 hover:bg-white/20 border border-white/20 backdrop-blur-sm transition-colors"
                  >
                    <span>Browse Categories</span>
                  </Link>
                </motion.div>
              </div>

              {/* Stats Bar */}
              <div className="pt-8 border-t border-slate-800/80 grid grid-cols-3 gap-4 max-w-lg mx-auto lg:mx-0 text-left">
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-white">
                    {featuredProducts.length > 0 ? `${featuredProducts.length}+` : '25+'}
                  </div>
                  <div className="text-xs text-slate-400 font-medium">Curated products</div>
                </div>
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-white">50K+</div>
                  <div className="text-xs text-slate-400 font-medium">Happy customers</div>
                </div>
                <div>
                  <div className="text-2xl sm:text-3xl font-extrabold text-white flex items-center gap-1">
                    4.8 <Star className="w-4 h-4 text-amber-400 fill-current inline" />
                  </div>
                  <div className="text-xs text-slate-400 font-medium">Average rating</div>
                </div>
              </div>
            </motion.div>

            {/* Cột phải: Device Showcase Mockup & Floating Badges (5 cols) */}
            <motion.div 
              initial={shouldReduceMotion ? {} : { opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.7, delay: 0.1, ease: 'easeOut' }}
              className="lg:col-span-5 relative flex justify-center"
            >
              <div className="relative w-full max-w-md sm:max-w-lg p-6">
                
                {/* Floating Badge 1: 2-Year Warranty */}
                <motion.div 
                  initial={shouldReduceMotion ? {} : { opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4, duration: 0.5 }}
                  className="absolute top-4 left-0 sm:left-4 bg-white/95 backdrop-blur-md text-slate-900 px-3.5 py-2 rounded-2xl shadow-xl border border-white/40 flex items-center gap-2.5 z-20"
                >
                  <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="text-xs font-bold text-slate-900 leading-tight">2-Year</div>
                    <div className="text-[10px] text-slate-500 font-medium">Warranty</div>
                  </div>
                </motion.div>

                {/* Floating Badge 2: 48-Hour Delivery */}
                <motion.div 
                  initial={shouldReduceMotion ? {} : { opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.5, duration: 0.5 }}
                  className="absolute bottom-6 right-0 sm:right-4 bg-white/95 backdrop-blur-md text-slate-900 px-3.5 py-2 rounded-2xl shadow-xl border border-white/40 flex items-center gap-2.5 z-20"
                >
                  <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                    <Truck className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="text-xs font-bold text-slate-900 leading-tight">48-Hour</div>
                    <div className="text-[10px] text-slate-500 font-medium">Delivery</div>
                  </div>
                </motion.div>

                {/* Main Hero Device Mockup Card */}
                <div className="relative rounded-3xl bg-gradient-to-tr from-slate-900/90 via-slate-800/80 to-blue-950/90 border border-slate-700/60 p-8 shadow-2xl backdrop-blur-sm flex flex-col items-center justify-center">
                  <div className="w-full aspect-[4/3] rounded-2xl bg-gradient-to-b from-blue-900/30 to-slate-950/90 border border-white/10 flex flex-col items-center justify-center p-6 relative overflow-hidden group">
                    
                    {/* Simulated Laptop/Phone Tech Mockup Screen */}
                    <div className="w-full h-full rounded-xl bg-slate-950/80 border border-slate-800 p-4 flex flex-col justify-between shadow-inner">
                      <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                        <div className="flex items-center gap-1.5">
                          <span className="w-2.5 h-2.5 rounded-full bg-red-500/80"></span>
                          <span className="w-2.5 h-2.5 rounded-full bg-amber-500/80"></span>
                          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500/80"></span>
                        </div>
                        <div className="text-[11px] font-mono text-slate-400">9:41 AM</div>
                      </div>

                      <div className="my-auto space-y-2 py-4 text-center">
                        <div className="h-3 bg-blue-500/30 rounded-full w-3/4 mx-auto"></div>
                        <div className="h-2.5 bg-slate-700/40 rounded-full w-1/2 mx-auto"></div>
                        <div className="mt-4 flex justify-center">
                          <span className="inline-block px-4 py-1.5 rounded-full bg-orange-500 text-white font-bold text-xs shadow-md">
                            Rikkei Store OS
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center justify-between text-[10px] text-slate-500 pt-2 border-t border-slate-800">
                        <span>High-Speed Wi-Fi 6E</span>
                        <span>100% Genuine</span>
                      </div>
                    </div>

                  </div>
                </div>

              </div>
            </motion.div>

          </div>
        </div>
      </section>

      {/* 2. BRAND PARTNERS MARQUEE SECTION (Độc lập, chuyển động ngang vô tận, không chồng lấn) */}
      <section className="bg-white border-y border-slate-200/80 py-5 sm:py-6 overflow-hidden relative select-none">
        {/* Lớp phủ gradient mờ 2 bên tạo hiệu ứng xuất hiện / ẩn đi tự nhiên */}
        <div className="pointer-events-none absolute left-0 top-0 bottom-0 w-16 sm:w-28 bg-gradient-to-r from-white via-white/80 to-transparent z-10" />
        <div className="pointer-events-none absolute right-0 top-0 bottom-0 w-16 sm:w-28 bg-gradient-to-l from-white via-white/80 to-transparent z-10" />

        <div className="animate-marquee-smooth flex items-center gap-12 sm:gap-16">
          {[...BRAND_PARTNERS, ...BRAND_PARTNERS].map((brand, idx) => (
            <div key={idx} className="flex items-center gap-4 shrink-0">
              <span className="text-xs sm:text-sm font-extrabold tracking-widest text-slate-400 hover:text-slate-900 transition-colors uppercase cursor-default">
                {brand}
              </span>
              <span className="w-1.5 h-1.5 rounded-full bg-slate-300 shrink-0" />
            </div>
          ))}
        </div>
      </section>

      {/* 3. 4 TRUST VALUE PILLARS (Section độc lập: 4 cột desktop, 2 cột tablet, 1 cột mobile) */}
      <motion.section 
        initial={shouldReduceMotion ? {} : { opacity: 0, y: 24 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-50px' }}
        transition={{ duration: 0.5 }}
        className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 relative"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 sm:gap-6">
          <motion.div 
            whileHover={shouldReduceMotion ? {} : { y: -4, transition: { duration: 0.2 } }}
            className="bg-white rounded-2xl p-6 border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow flex items-center gap-4"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
              <Truck className="w-6 h-6 stroke-[1.8]" />
            </div>
            <div>
              <div className="font-bold text-slate-900 text-sm">Free Fast Delivery</div>
              <div className="text-xs text-slate-500 mt-0.5">On every order above 500.000₫</div>
            </div>
          </motion.div>

          <motion.div 
            whileHover={shouldReduceMotion ? {} : { y: -4, transition: { duration: 0.2 } }}
            className="bg-white rounded-2xl p-6 border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow flex items-center gap-4"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
              <RotateCcw className="w-6 h-6 stroke-[1.8]" />
            </div>
            <div>
              <div className="font-bold text-slate-900 text-sm">7-Day Returns</div>
              <div className="text-xs text-slate-500 mt-0.5">No-questions-asked refunds</div>
            </div>
          </motion.div>

          <motion.div 
            whileHover={shouldReduceMotion ? {} : { y: -4, transition: { duration: 0.2 } }}
            className="bg-white rounded-2xl p-6 border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow flex items-center gap-4"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
              <ShieldCheck className="w-6 h-6 stroke-[1.8]" />
            </div>
            <div>
              <div className="font-bold text-slate-900 text-sm">Genuine Warranty</div>
              <div className="text-xs text-slate-500 mt-0.5">Brand-authorised service</div>
            </div>
          </motion.div>

          <motion.div 
            whileHover={shouldReduceMotion ? {} : { y: -4, transition: { duration: 0.2 } }}
            className="bg-white rounded-2xl p-6 border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow flex items-center gap-4"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
              <Headphones className="w-6 h-6 stroke-[1.8]" />
            </div>
            <div>
              <div className="font-bold text-slate-900 text-sm">24×7 Support</div>
              <div className="text-xs text-slate-500 mt-0.5">Real humans, real answers</div>
            </div>
          </motion.div>
        </div>
      </motion.section>

      {/* 4. "SHOP BY CATEGORY" SECTION — ĐỒNG BỘ 100% CSDL MYSQL (Scroll Reveal) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <motion.div 
          initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.5 }}
          className="text-center max-w-2xl mx-auto mb-12 space-y-2"
        >
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-50 border border-orange-200/80 text-orange-600 text-xs font-bold uppercase tracking-wider">
            <span>🏷️ SHOP BY CATEGORY</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Find exactly what you need
          </h2>
          <p className="text-sm text-slate-500">
            Danh mục sản phẩm chính hãng lấy trực tiếp từ hệ thống, chọn lọc chất lượng cao nhất.
          </p>
        </motion.div>

        {/* Dynamic Category Cards from MySQL */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {categories.length > 0 ? (
            categories.slice(0, 5).map((cat, idx) => {
              const theme = CATEGORY_THEMES[idx % CATEGORY_THEMES.length]
              const Icon = theme.icon
              const targetUrl = `/products?categoryId=${cat.id}`

              return (
                <motion.div
                  key={cat.id}
                  initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, margin: '-40px' }}
                  transition={{ duration: 0.45, delay: idx * 0.08 }}
                  whileHover={shouldReduceMotion ? {} : { y: -6, transition: { duration: 0.25 } }}
                >
                  <Link
                    to={targetUrl}
                    className={`group rounded-3xl p-6 text-white bg-gradient-to-b ${theme.gradient} relative overflow-hidden shadow-md hover:shadow-2xl transition-all duration-300 flex flex-col justify-between min-h-[260px] h-full block`}
                  >
                    {/* Item count badge at top right */}
                    <div className="flex justify-end">
                      <span className="bg-white/20 backdrop-blur-md text-white text-[11px] font-bold px-2.5 py-0.5 rounded-full">
                        Danh mục
                      </span>
                    </div>

                    {/* Device Icon in Center */}
                    <div className="my-auto flex justify-center py-4">
                      <div className="w-16 h-16 rounded-2xl bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                        <Icon className="w-9 h-9 text-white stroke-[1.6]" />
                      </div>
                    </div>

                    {/* Name & Desc at bottom */}
                    <div>
                      <h3 className="font-extrabold text-base text-white group-hover:text-amber-200 transition-colors">
                        {cat.name}
                      </h3>
                      <p className="text-[11px] text-white/80 line-clamp-2 mt-1 leading-snug">
                        {cat.description || theme.defaultDesc}
                      </p>
                    </div>
                  </Link>
                </motion.div>
              )
            })
          ) : (
            // Skeleton khi đang nạp danh mục
            [...Array(5)].map((_, i) => (
              <div key={i} className="rounded-3xl p-6 bg-slate-200 animate-pulse min-h-[260px]" />
            ))
          )}
        </div>
      </section>

      {/* 5. "HANDPICKED" FEATURED PRODUCTS WITH STAGGER ANIMATION (Scroll Reveal) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
        <motion.div 
          initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.5 }}
          className="text-center max-w-2xl mx-auto mb-8 space-y-2"
        >
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-50 border border-orange-200/80 text-orange-600 text-xs font-bold uppercase tracking-wider">
            <span>⭐ HANDPICKED</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Featured Products
          </h2>
          <p className="text-sm text-slate-500">
            Tuyển chọn những sản phẩm bán chạy và được đánh giá cao nhất từ cơ sở dữ liệu.
          </p>

          {/* Category Filter Pills (Sử dụng ID danh mục từ MySQL) */}
          <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
            <motion.button
              whileTap={shouldReduceMotion ? {} : { scale: 0.95 }}
              type="button"
              onClick={() => setSelectedCategoryTab('ALL')}
              className={`px-5 py-2 rounded-full text-xs font-bold transition-all cursor-pointer ${
                selectedCategoryTab === 'ALL'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
              }`}
            >
              All
            </motion.button>
            {categories.map((c) => {
              const isSelected = selectedCategoryTab === c.id || String(selectedCategoryTab) === String(c.id)
              return (
                <motion.button
                  key={c.id}
                  whileTap={shouldReduceMotion ? {} : { scale: 0.95 }}
                  type="button"
                  onClick={() => setSelectedCategoryTab(c.id)}
                  className={`px-5 py-2 rounded-full text-xs font-bold transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                  }`}
                >
                  {c.name}
                </motion.button>
              )
            })}
          </div>
        </motion.div>

        {/* Product Grid với AnimatePresence & Fallbacks */}
        {loadingProducts ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 py-6">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="bg-white rounded-2xl border border-slate-200 p-5 animate-pulse space-y-4">
                <div className="aspect-square bg-slate-100 rounded-xl" />
                <div className="h-4 bg-slate-200 rounded w-2/3" />
                <div className="h-4 bg-slate-200 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : productsError ? (
          // Hiển thị lỗi API thực tế, không coi là danh sách rỗng
          <div className="bg-red-50 border border-red-200 text-red-700 p-8 rounded-3xl text-center text-sm my-6">
            <AlertCircle className="w-10 h-10 text-red-500 mx-auto mb-2" />
            <p className="font-semibold mb-1">Không thể tải sản phẩm</p>
            <p className="text-xs text-red-600 mb-4">{productsError}</p>
            <button
              type="button"
              onClick={() => fetchFeaturedProducts(selectedCategoryTab)}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-full bg-red-600 hover:bg-red-700 text-white text-xs font-bold transition-colors cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : featuredProducts.length === 0 ? (
          // Trạng thái danh mục không có sản phẩm
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center my-6">
            <PackageOpen className="w-12 h-12 text-slate-400 mx-auto mb-3" />
            <p className="text-sm font-semibold text-slate-700">Không có sản phẩm trong danh mục này</p>
            <p className="text-xs text-slate-500 mt-1">Vui lòng chọn danh mục khác hoặc quay lại sau.</p>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            <motion.div 
              key={String(selectedCategoryTab)}
              initial={shouldReduceMotion ? { opacity: 1 } : { opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: -10 }}
              transition={{ duration: 0.28, ease: 'easeOut' }}
              className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
            >
              {featuredProducts.slice(0, 8).map((prod) => (
                <ProductCard
                  key={prod.id}
                  product={prod}
                  onAddToCart={handleAddToCart}
                />
              ))}
            </motion.div>
          </AnimatePresence>
        )}

        {/* View All Button */}
        <div className="text-center pt-10">
          <motion.div
            whileHover={shouldReduceMotion ? {} : { scale: 1.04 }}
            whileTap={shouldReduceMotion ? {} : { scale: 0.96 }}
            className="inline-block"
          >
            <Link
              to="/products"
              className="inline-flex items-center gap-2 px-6 py-3 rounded-full border border-blue-600 text-blue-600 hover:bg-blue-50 font-bold text-xs transition-colors"
            >
              <span>View All {featuredProducts.length || ''} Products</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </motion.div>
        </div>
      </section>

      {/* 6. "DEAL OF THE DAY" PROMO BANNER WITH COUNTDOWN TIMER — 100% REAL DATA (TechNova Image 4) */}
      <motion.section 
        initial={shouldReduceMotion ? {} : { opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-50px' }}
        transition={{ duration: 0.6 }}
        className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14"
      >
        {/* Loading Skeleton */}
        {loadingDeal ? (
          <div className="rounded-3xl bg-slate-900 border border-slate-800 p-8 sm:p-12 animate-pulse">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
              <div className="lg:col-span-4 flex justify-center">
                <div className="w-64 h-64 rounded-3xl bg-slate-800" />
              </div>
              <div className="lg:col-span-5 space-y-4">
                <div className="h-6 bg-slate-800 rounded-full w-36" />
                <div className="h-8 bg-slate-800 rounded-lg w-3/4" />
                <div className="h-14 bg-slate-800 rounded-lg w-full" />
                <div className="h-8 bg-slate-800 rounded-lg w-48" />
              </div>
              <div className="lg:col-span-3 flex justify-center gap-2">
                <div className="w-16 h-16 bg-slate-800 rounded-2xl" />
                <div className="w-16 h-16 bg-slate-800 rounded-2xl" />
                <div className="w-16 h-16 bg-slate-800 rounded-2xl" />
              </div>
            </div>
          </div>
        ) : dealError ? (
          // Error State: Hiển thị lỗi văn minh, KHÔNG bao giờ hiển thị dữ liệu giả
          <div className="rounded-3xl bg-slate-900/90 border border-slate-800 p-8 text-center text-slate-300">
            <AlertCircle className="w-10 h-10 text-orange-400 mx-auto mb-2" />
            <p className="text-sm font-semibold">{dealError}</p>
            <button
              type="button"
              onClick={fetchDealOfTheDay}
              className="mt-3 inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full bg-slate-800 hover:bg-slate-700 text-xs text-white cursor-pointer transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Thử lại</span>
            </button>
          </div>
        ) : !deal ? (
          // Empty State: Không có deal hợp lệ, thông báo nhẹ nhàng
          <div className="rounded-3xl bg-slate-900 border border-slate-800 p-8 text-center text-slate-400">
            <Clock className="w-10 h-10 text-slate-500 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-300">Chương trình ưu đãi ngày hôm nay đã kết thúc</p>
            <p className="text-xs text-slate-500 mt-1">Vui lòng quay lại sau để đón chờ những ưu đãi bùng nổ tiếp theo.</p>
          </div>
        ) : (
          // Real Deal State: 100% dữ liệu từ database MySQL
          <div className="rounded-3xl bg-gradient-to-r from-[#070e24] via-[#0b163b] to-[#081230] border border-blue-900/40 p-8 sm:p-12 text-white shadow-2xl relative overflow-hidden">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center relative z-10">
              
              {/* Left: Product Image Glow Box */}
              <div className="lg:col-span-4 flex justify-center">
                <div className="w-64 h-64 rounded-3xl bg-gradient-to-tr from-purple-500/20 via-blue-500/10 to-indigo-500/20 border border-white/10 p-6 flex items-center justify-center relative shadow-lg overflow-hidden group">
                  <div className="w-36 h-36 rounded-full bg-purple-500/20 blur-xl absolute" />
                  {deal.imageUrl ? (
                    <img
                      src={deal.imageUrl}
                      alt={deal.name}
                      className="w-full h-full object-contain relative z-10 transition-transform duration-300 group-hover:scale-108"
                    />
                  ) : (
                    <Headphones className="w-24 h-24 text-white relative z-10 stroke-[1.4]" />
                  )}
                </div>
              </div>

              {/* Middle: Real Product Details */}
              <div className="lg:col-span-5 space-y-4 text-center lg:text-left">
                <div className="flex flex-wrap items-center justify-center lg:justify-start gap-2">
                  <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/20 border border-orange-500/40 text-orange-400 text-xs font-bold uppercase tracking-wider">
                    <Clock className="w-3.5 h-3.5" />
                    <span>DEAL OF THE DAY</span>
                  </div>
                  {deal.couponCode && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full bg-blue-500/20 border border-blue-400/40 text-blue-300 text-[11px] font-bold">
                      Voucher: {deal.couponCode}
                    </span>
                  )}
                  {deal.stockQuantity !== undefined && (
                    <span className="text-[11px] text-slate-400 font-medium">
                      (Còn {deal.stockQuantity} sản phẩm)
                    </span>
                  )}
                </div>

                <h3 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight">
                  {deal.name}
                </h3>

                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed line-clamp-3">
                  {deal.description || 'Sản phẩm công nghệ chính hãng với mức chiết khấu đặc biệt áp dụng duy nhất hôm nay.'}
                </p>

                {/* Price & Discount */}
                <div className="flex items-baseline justify-center lg:justify-start gap-3 pt-1">
                  <span className="text-2xl sm:text-3xl font-extrabold text-white">
                    {formatCurrency(deal.salePrice)}
                  </span>
                  {deal.originalPrice && (
                    <span className="text-sm text-slate-400 line-through">
                      {formatCurrency(deal.originalPrice)}
                    </span>
                  )}
                  {deal.discountPercentage > 0 && (
                    <span className="bg-amber-400 text-slate-950 text-xs font-bold px-2 py-0.5 rounded-md">
                      {deal.discountPercentage}% OFF
                    </span>
                  )}
                </div>

                {/* Grab Deal CTA điều hướng chính xác đến sản phẩm */}
                <div className="pt-2">
                  <motion.div
                    whileHover={shouldReduceMotion ? {} : { scale: 1.05 }}
                    whileTap={shouldReduceMotion ? {} : { scale: 0.95 }}
                    className="inline-block"
                  >
                    <Link
                      to={`/products/${deal.id}`}
                      className="inline-flex items-center justify-center gap-2 px-7 py-3 rounded-full text-sm font-bold text-white bg-orange-500 hover:bg-orange-600 shadow-lg shadow-orange-500/30 transition-colors"
                    >
                      <Zap className="w-4 h-4 fill-current" />
                      <span>Grab This Deal</span>
                    </Link>
                  </motion.div>
                </div>
              </div>

              {/* Right: Dynamic Countdown Timer Blocks */}
              <div className="lg:col-span-3 flex flex-col items-center lg:items-end justify-center">
                <span className="text-xs font-medium text-slate-400 mb-2">
                  {timeLeft.isExpired ? 'Ưu đãi đã kết thúc' : 'Offer ends in'}
                </span>
                <div className="flex items-center gap-2">
                  <div className="w-16 h-16 rounded-2xl bg-slate-900/80 border border-slate-700 flex flex-col items-center justify-center shadow-md">
                    <span className="text-lg font-extrabold text-white font-mono">
                      {String(timeLeft.hours).padStart(2, '0')}
                    </span>
                    <span className="text-[9px] font-bold text-slate-400 uppercase">HOURS</span>
                  </div>
                  <div className="w-16 h-16 rounded-2xl bg-slate-900/80 border border-slate-700 flex flex-col items-center justify-center shadow-md">
                    <span className="text-lg font-extrabold text-white font-mono">
                      {String(timeLeft.minutes).padStart(2, '0')}
                    </span>
                    <span className="text-[9px] font-bold text-slate-400 uppercase">MINS</span>
                  </div>
                  <div className="w-16 h-16 rounded-2xl bg-slate-900/80 border border-slate-700 flex flex-col items-center justify-center shadow-md">
                    <span className="text-lg font-extrabold text-orange-400 font-mono">
                      {String(timeLeft.seconds).padStart(2, '0')}
                    </span>
                    <span className="text-[9px] font-bold text-slate-400 uppercase">SECS</span>
                  </div>
                </div>
              </div>

            </div>
          </div>
        )}
      </motion.section>

      {/* 7. "MOST LOVED" BEST SELLERS (Scroll Reveal) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
        <motion.div 
          initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.5 }}
          className="flex items-end justify-between mb-8"
        >
          <div>
            <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-orange-50 text-orange-600 text-xs font-bold uppercase tracking-wider mb-2">
              <span>🏆 MOST LOVED</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Best Sellers
            </h2>
          </div>
          <Link
            to="/products"
            className="text-xs font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1 group transition-colors"
          >
            <span>See all</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </Link>
        </motion.div>

        {/* Product Grid */}
        <motion.div 
          initial={shouldReduceMotion ? {} : { opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-40px' }}
          transition={{ duration: 0.4 }}
          className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6"
        >
          {(bestSellers.length > 0 ? bestSellers : featuredProducts.slice(0, 4)).map((prod) => (
            <ProductCard
              key={prod.id}
              product={prod}
              onAddToCart={handleAddToCart}
            />
          ))}
        </motion.div>
      </section>

      {/* 8. PROJECT ACADEMIC INFO & BACKEND DIAGNOSTICS (Scroll Reveal) */}
      <motion.section 
        initial={shouldReduceMotion ? {} : { opacity: 0, y: 24 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-50px' }}
        transition={{ duration: 0.5 }}
        className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16 border-t border-slate-200"
      >
        <div className="bg-white rounded-3xl border border-slate-200/90 p-6 sm:p-8 shadow-sm space-y-8">
          
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
            <div>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-blue-600"></span>
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Thông tin dự án & Hệ thống</span>
              </div>
              <h3 className="text-lg font-bold text-slate-900 mt-1">
                E-Commerce Platform Architecture & Team Assignment
              </h3>
            </div>

            <motion.button
              whileTap={shouldReduceMotion ? {} : { scale: 0.96 }}
              type="button"
              onClick={checkBackendHealth}
              disabled={healthLoading}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-800 transition-colors cursor-pointer self-start sm:self-auto"
            >
              <Activity className="w-3.5 h-3.5 text-blue-600" />
              <span>{healthLoading ? 'Đang kiểm tra...' : 'Kiểm tra trạng thái Backend'}</span>
            </motion.button>
          </div>

          {/* Backend Diagnostics Result */}
          {healthError && (
            <div className="p-3.5 rounded-2xl bg-red-50 border border-red-200 text-red-800 text-xs">
              <span className="font-bold">Chưa kết nối Backend:</span> {healthError}
              <div className="text-red-600 mt-1">Đảm bảo backend Spring Boot đang chạy tại cổng 8080.</div>
            </div>
          )}

          {healthStatus && (
            <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs">
              <div className="font-bold text-emerald-800 flex items-center gap-1.5 mb-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Backend đang hoạt động bình thường (Status: UP)</span>
              </div>
              <pre className="p-3 bg-white rounded-xl border border-emerald-200 font-mono text-[11px] overflow-x-auto text-slate-700">
                {JSON.stringify(healthStatus, null, 2)}
              </pre>
            </div>
          )}

          {/* Architecture 3 pillars */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
            <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1.5">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Layers className="w-4 h-4 text-blue-600" />
                <span>Frontend Layer</span>
              </div>
              <p className="text-slate-600 font-medium">React 18 + Vite + Tailwind CSS + Zustand + Motion</p>
              <div className="text-slate-500 text-[11px]">Rikkei Store Design System, Route guards, token refresh interceptor.</div>
            </div>

            <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1.5">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Server className="w-4 h-4 text-blue-600" />
                <span>Backend Core</span>
              </div>
              <p className="text-slate-600 font-medium">Spring Boot 3.3.5 + Java 21 + Spring Security</p>
              <div className="text-slate-500 text-[11px]">Stateless JWT JJWT 0.12.6, transaction isolation, global error handler.</div>
            </div>

            <div className="p-5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1.5">
              <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
                <Database className="w-4 h-4 text-blue-600" />
                <span>Database & Cache</span>
              </div>
              <p className="text-slate-600 font-medium">MySQL 8.0 & Redis 7 (Docker Compose)</p>
              <div className="text-slate-500 text-[11px]">Khóa ngoại an toàn, kiểm tra tồn kho, nhật ký giao dịch kho.</div>
            </div>
          </div>

          {/* Team Work Allocation Table */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-2">
              <Users className="w-3.5 h-3.5 text-blue-600" />
              <span>Phân chia công việc thành viên nhóm</span>
            </h4>
            <div className="overflow-x-auto rounded-2xl border border-slate-200">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-700 font-bold uppercase border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4 w-12 text-center">STT</th>
                    <th className="py-3 px-4">Thành viên</th>
                    <th className="py-3 px-4">Vai trò</th>
                    <th className="py-3 px-4">Phụ trách Module</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {teamMembers.map((m) => (
                    <tr key={m.stt} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 text-center text-slate-400 font-medium">{m.stt}</td>
                      <td className="py-3 px-4 font-bold text-slate-900">{m.name}</td>
                      <td className="py-3 px-4 text-slate-600">{m.role}</td>
                      <td className="py-3 px-4 text-slate-900 font-medium">{m.task}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

        </div>
      </motion.section>

    </div>
  )
}

export default HomePage
