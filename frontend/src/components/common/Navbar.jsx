import React, { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { 
  ShoppingCart, 
  User as UserIcon,
  LogOut, 
  Menu, 
  X,
  Package,
  Search,
  Zap,
  Truck,
  Headphones,
  Info,
  ChevronDown,
  Heart
} from 'lucide-react'
import { useAuthStore } from '../../store/useAuthStore'
import { useCartStore } from '../../store/useCartStore'

export const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuthStore()
  const totalCartCount = useCartStore((state) => state.getTotalCount())
  const navigate = useNavigate()
  const location = useLocation()
  
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [userDropdownOpen, setUserDropdownOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const dropdownRef = useRef(null)

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setUserDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Đóng mobile menu khi chuyển trang
  useEffect(() => {
    setMobileMenuOpen(false)
    setUserDropdownOpen(false)
  }, [location.pathname])

  const handleLogout = async () => {
    await logout()
    setUserDropdownOpen(false)
    navigate('/login')
  }

  const handleCartClick = (e) => {
    if (!isAuthenticated) {
      e.preventDefault()
      navigate('/login', {
        state: {
          from: { pathname: '/cart' },
          message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để xem giỏ hàng.',
        },
      })
      return
    }
    if (user?.role === 'ADMIN') {
      e.preventDefault()
      alert('Tài khoản Quản trị viên (ADMIN) không được phép sử dụng giỏ hàng hoặc mua sắm.')
      return
    }
  }

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      navigate(`/products?keyword=${encodeURIComponent(searchQuery.trim())}`)
    } else {
      navigate('/products')
    }
  }

  const getInitials = (name) => {
    if (!name) return 'U'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/'
    return location.pathname.startsWith(path)
  }

  return (
    <header className="sticky top-0 z-40 w-full bg-white shadow-xs">
      {/* 1. Top Utility Notice Bar (TechNova Style) */}
      <div className="bg-[#070d1e] text-slate-300 text-xs py-2 px-4 border-b border-slate-800/80">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-slate-300 font-normal">
            <Truck className="w-3.5 h-3.5 text-orange-400 shrink-0" />
            <span>Miễn phí giao hàng cho đơn trên 500.000₫ • Đổi trả nhanh 7 ngày</span>
          </div>
          <div className="flex items-center gap-5 text-slate-400 text-[11px]">
            <Link to="/products" className="hover:text-slate-200 transition-colors flex items-center gap-1">
              <Headphones className="w-3 h-3 text-slate-400" />
              <span>Hỗ trợ 24/7</span>
            </Link>
            <Link to="/products" className="hover:text-slate-200 transition-colors flex items-center gap-1">
              <Info className="w-3 h-3 text-slate-400" />
              <span>Về Rikkei Store</span>
            </Link>
            <Link 
              to={isAuthenticated && user?.role !== 'ADMIN' ? "/orders" : "/login"} 
              className="hover:text-slate-200 transition-colors flex items-center gap-1"
            >
              <Package className="w-3 h-3 text-slate-400" />
              <span>Tra cứu đơn hàng</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 2. Main Navigation Bar (Spacious, Modern & High-Class Tech Style) */}
      <div className="border-b border-slate-200/90 bg-white/95 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 sm:py-4 min-h-[78px] flex items-center justify-between gap-3 sm:gap-6">
          
          {/* Brand Logo - Enlarged & Polished */}
          <Link 
            to="/" 
            className="flex items-center gap-3 shrink-0 group hover:opacity-95 transition-opacity"
          >
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center shadow-md shadow-blue-500/25 group-hover:scale-105 group-hover:shadow-blue-500/40 transition-all duration-300">
              <Zap className="w-6 h-6 fill-current text-white" />
            </div>
            <div className="flex flex-col">
              <span className="font-black text-slate-900 tracking-tight text-xl sm:text-2xl leading-none">
                Rikkei Store
              </span>
              <span className="text-[10px] uppercase font-extrabold tracking-widest text-blue-600 mt-1">
                CHÍNH HÃNG
              </span>
            </div>
          </Link>

          {/* Desktop Navigation Links - Modern Pill Indicator */}
          <nav className="hidden lg:flex items-center gap-1.5 text-[15px] font-bold text-slate-600">
            <Link
              to="/"
              className={`px-4 py-2 rounded-full transition-all duration-200 flex items-center gap-1.5 ${
                isActive('/') && location.pathname === '/'
                  ? 'text-blue-700 bg-blue-50 font-black shadow-2xs'
                  : 'text-slate-600 hover:text-slate-950 hover:bg-slate-100'
              }`}
            >
              <span>Trang chủ</span>
            </Link>

            <Link
              to="/products"
              className={`px-4 py-2 rounded-full transition-all duration-200 flex items-center gap-1.5 ${
                isActive('/products')
                  ? 'text-blue-700 bg-blue-50 font-black shadow-2xs'
                  : 'text-slate-600 hover:text-slate-950 hover:bg-slate-100'
              }`}
            >
              <span>Sản phẩm</span>
            </Link>

            {isAuthenticated && user?.role !== 'ADMIN' && (
              <Link
                to="/orders"
                className={`px-4 py-2 rounded-full transition-all duration-200 flex items-center gap-1.5 ${
                  isActive('/orders')
                    ? 'text-blue-700 bg-blue-50 font-black shadow-2xs'
                    : 'text-slate-600 hover:text-slate-950 hover:bg-slate-100'
                }`}
              >
                <span>Đơn hàng</span>
              </Link>
            )}
          </nav>

          {/* Search Pill Input Bar - Expanded, Roomy & Elegant */}
          <form 
            onSubmit={handleSearchSubmit} 
            className="hidden md:flex items-center flex-1 max-w-md lg:max-w-lg mx-2 lg:mx-4 relative"
          >
            <div className="relative w-full flex items-center">
              <input
                type="text"
                placeholder="Tìm laptop, điện thoại, phụ kiện..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-100/90 hover:bg-slate-100 focus:bg-white text-slate-900 placeholder:text-slate-400 text-sm rounded-full pl-5 pr-12 py-2.5 border border-slate-200 focus:border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-500/15 shadow-inner transition-all"
              />
              <button
                type="submit"
                className="absolute right-1.5 w-8.5 h-8.5 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white flex items-center justify-center transition-all shadow-sm shadow-blue-500/25 active:scale-95 cursor-pointer"
                title="Tìm kiếm"
              >
                <Search className="w-4 h-4" />
              </button>
            </div>
          </form>

          {/* Right Action Icons & User Menu */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Wishlist Link */}
            <Link
              to="/products"
              className="w-10 h-10 text-slate-600 hover:text-red-500 hover:bg-slate-100 rounded-full transition-colors hidden sm:flex items-center justify-center cursor-pointer"
              title="Yêu thích"
            >
              <Heart className="w-5 h-5 stroke-[1.8]" />
            </Link>

            {/* Cart Button */}
            <Link
              to="/cart"
              onClick={handleCartClick}
              className="relative w-10 h-10 text-slate-700 hover:text-blue-600 hover:bg-slate-100 rounded-full transition-colors flex items-center justify-center cursor-pointer"
              title="Giỏ hàng"
            >
              <ShoppingCart className="w-5 h-5 stroke-[1.8]" />
              {isAuthenticated && user?.role !== 'ADMIN' && totalCartCount > 0 && (
                <span className="absolute 0 top-0.5 right-0.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white font-extrabold text-[10px] rounded-full min-w-[19px] h-[19px] px-1 flex items-center justify-center border-2 border-white shadow-xs">
                  {totalCartCount > 99 ? '99+' : totalCartCount}
                </span>
              )}
            </Link>

            {/* User Auth Section */}
            {isAuthenticated && user ? (
              <div className="relative" ref={dropdownRef}>
                <button
                  type="button"
                  onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                  className="flex items-center gap-2.5 p-1.5 pl-2 pr-3 rounded-full border border-slate-200 bg-slate-50/80 hover:bg-white hover:border-slate-300 text-slate-800 transition-all cursor-pointer shadow-2xs"
                >
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs">
                    {getInitials(user.fullName || user.email)}
                  </div>
                  <span className="text-sm font-bold max-w-[110px] truncate hidden sm:inline text-slate-800">
                    {user.fullName || user.email.split('@')[0]}
                  </span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400 hidden sm:inline mr-0.5" />
                </button>

                {/* User Dropdown */}
                {userDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-64 rounded-2xl border border-slate-200 bg-white shadow-2xl py-2 z-50 text-xs animate-in fade-in zoom-in-95 duration-100">
                    <div className="px-4 py-3 border-b border-slate-100 bg-slate-50/70 rounded-t-2xl">
                      <p className="font-bold text-slate-950 truncate text-sm">{user.fullName || 'Người dùng'}</p>
                      <p className="text-slate-500 text-[11px] font-mono truncate">{user.email}</p>
                      <div className="mt-1.5">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200/60">
                          {user.role}
                        </span>
                      </div>
                    </div>

                    <div className="py-1.5">
                      {user.role !== 'ADMIN' && (
                        <>
                          <Link
                            to="/profile"
                            className="flex items-center gap-3 px-4 py-2 text-xs font-medium text-slate-700 hover:text-slate-950 hover:bg-slate-50 transition-colors"
                          >
                            <UserIcon className="w-4 h-4 text-slate-400" />
                            <span>Hồ sơ tài khoản</span>
                          </Link>

                          <Link
                            to="/orders"
                            className="flex items-center gap-3 px-4 py-2 text-xs font-medium text-slate-700 hover:text-slate-950 hover:bg-slate-50 transition-colors"
                          >
                            <Package className="w-4 h-4 text-slate-400" />
                            <span>Đơn hàng của tôi</span>
                          </Link>
                        </>
                      )}
                    </div>

                    <div className="pt-1.5 border-t border-slate-100">
                      <button
                        type="button"
                        onClick={handleLogout}
                        className="w-full flex items-center gap-3 px-4 py-2 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left cursor-pointer"
                      >
                        <LogOut className="w-4 h-4 text-red-500" />
                        <span>Đăng xuất</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="text-xs sm:text-sm font-bold text-slate-700 hover:text-slate-950 px-3.5 py-2 rounded-full hover:bg-slate-100 transition-colors"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  className="text-xs sm:text-sm font-bold bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white px-4 sm:px-5 py-2 rounded-full shadow-sm shadow-blue-500/25 hover:shadow-md transition-all active:scale-[0.98]"
                >
                  Đăng ký
                </Link>
              </div>
            )}

            {/* Mobile menu toggle */}
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="lg:hidden p-2 rounded-lg text-slate-600 hover:text-slate-950 hover:bg-slate-100 transition-colors cursor-pointer"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile Search Bar in sub-header */}
        <div className="md:hidden px-4 pb-3.5 pt-1">
          <form onSubmit={handleSearchSubmit} className="relative w-full flex items-center">
            <input
              type="text"
              placeholder="Tìm laptop, điện thoại, phụ kiện..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-100 text-slate-900 placeholder:text-slate-400 text-sm rounded-full pl-4 pr-11 py-2.5 border border-slate-200 focus:outline-none focus:border-blue-500"
            />
            <button
              type="submit"
              className="absolute right-1.5 w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center shadow-xs"
            >
              <Search className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-t border-slate-200 bg-white px-4 py-3 space-y-1 shadow-md animate-in slide-in-from-top-1">
          <Link
            to="/"
            className={`block px-3 py-2 rounded-xl text-sm font-semibold ${
              isActive('/') && location.pathname === '/' ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-50'
            }`}
          >
            Trang chủ
          </Link>
          <Link
            to="/products"
            className={`block px-3 py-2 rounded-xl text-sm font-semibold ${
              isActive('/products') ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-50'
            }`}
          >
            Sản phẩm
          </Link>

          {isAuthenticated ? (
            <>
              {user?.role !== 'ADMIN' && (
                <>
                  <Link
                    to="/profile"
                    className={`block px-3 py-2 rounded-xl text-sm font-semibold ${
                      isActive('/profile') ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Hồ sơ tài khoản
                  </Link>
                  <Link
                    to="/orders"
                    className={`block px-3 py-2 rounded-xl text-sm font-semibold ${
                      isActive('/orders') ? 'bg-blue-50 text-blue-700' : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Đơn hàng của tôi
                  </Link>
                </>
              )}
              <button
                type="button"
                onClick={handleLogout}
                className="w-full text-left px-3 py-2 rounded-xl text-sm font-semibold text-red-600 hover:bg-red-50 flex items-center gap-2 pt-2 border-t border-slate-100 mt-2"
              >
                <LogOut className="w-4 h-4" />
                <span>Đăng xuất</span>
              </button>
            </>
          ) : (
            <div className="pt-2 border-t border-slate-100 flex flex-col gap-2">
              <Link
                to="/login"
                className="w-full text-center py-2.5 text-sm font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-full transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="w-full text-center py-2.5 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-full transition-colors shadow-sm"
              >
                Đăng ký tài khoản
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  )
}

export default Navbar
