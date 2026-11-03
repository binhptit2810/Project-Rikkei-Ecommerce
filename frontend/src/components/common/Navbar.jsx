import React, { useState, useRef, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { 
  ShoppingBag, 
  ShoppingCart, 
  User as UserIcon,
  Shield, 
  LogOut, 
  Menu, 
  X,
  ChevronDown,
  Package
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

  // Đóng menu khi chuyển trang
  useEffect(() => {
    setMobileMenuOpen(false)
    setUserDropdownOpen(false)
  }, [location.pathname])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const getInitial = (name) => {
    if (!name) return 'U'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/'
    return location.pathname.startsWith(path)
  }

  return (
    <header className="bg-white/95 backdrop-blur-sm border-b border-zinc-200 sticky top-0 z-40 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-2.5 group shrink-0">
          <div className="w-8 h-8 rounded-md bg-zinc-900 text-zinc-50 flex items-center justify-center transition-colors shadow-sm">
            <ShoppingBag className="w-4 h-4 text-zinc-50" />
          </div>
          <div className="flex flex-col">
            <span className="font-semibold text-base text-zinc-950 tracking-tight leading-none">
              Rikkei<span className="text-zinc-500 font-normal">Store</span>
            </span>
            <span className="text-[10px] font-medium text-zinc-400 tracking-wider uppercase mt-0.5">
              Commerce Platform
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-1">
          <Link
            to="/"
            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
              isActive('/')
                ? 'text-zinc-950 bg-zinc-100 font-medium'
                : 'text-zinc-600 hover:text-zinc-950 hover:bg-zinc-50'
            }`}
          >
            Trang chủ
          </Link>

          <Link
            to="/products"
            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
              isActive('/products')
                ? 'text-zinc-950 bg-zinc-100 font-medium'
                : 'text-zinc-600 hover:text-zinc-950 hover:bg-zinc-50'
            }`}
          >
            Sản phẩm
          </Link>

          {isAuthenticated && user?.role === 'ADMIN' && (
            <Link
              to="/admin/users"
              className="px-3 py-1.5 rounded-md text-sm font-medium text-zinc-900 bg-zinc-100 hover:bg-zinc-200/80 transition-colors flex items-center gap-1.5 border border-zinc-200"
            >
              <Shield className="w-3.5 h-3.5 text-zinc-700" />
              <span>Quản trị</span>
            </Link>
          )}
        </nav>

        {/* Right Action Icons & User Menu */}
        <div className="flex items-center gap-2">
          {/* Cart Button */}
          <Link
            to="/cart"
            className="relative p-2 text-zinc-700 hover:text-zinc-950 hover:bg-zinc-100 rounded-md transition-colors flex items-center justify-center cursor-pointer"
            title="Giỏ hàng"
          >
            <ShoppingCart className="w-4 h-4" />
            {totalCartCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-zinc-900 text-zinc-50 font-semibold text-[10px] rounded-full min-w-[18px] h-[18px] px-1 flex items-center justify-center border-2 border-white shadow-xs">
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
                className="flex items-center gap-2 p-1.5 pr-2 rounded-md hover:bg-zinc-100 transition-colors border border-transparent hover:border-zinc-200 cursor-pointer"
              >
                <div className="w-7 h-7 rounded-md bg-zinc-900 text-zinc-50 font-medium text-xs flex items-center justify-center">
                  {getInitial(user.fullName)}
                </div>
                <span className="text-xs font-medium text-zinc-800 hidden sm:inline max-w-[120px] truncate">
                  {user.fullName || user.email}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-zinc-400" />
              </button>

              {/* User Dropdown Menu */}
              {userDropdownOpen && (
                <div className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-md border border-zinc-200 py-1 z-50 animate-in fade-in slide-in-from-top-1 duration-150">
                  <div className="px-3 py-2 border-b border-zinc-100">
                    <div className="text-xs font-semibold text-zinc-900 truncate">
                      {user.fullName || 'Người dùng'}
                    </div>
                    <div className="text-[11px] text-zinc-500 truncate mt-0.5">
                      {user.email}
                    </div>
                    <div className="mt-1.5">
                      <span className="inline-block text-[10px] font-medium px-2 py-0.5 rounded bg-zinc-100 text-zinc-800 border border-zinc-200 uppercase tracking-wider">
                        {user.role}
                      </span>
                    </div>
                  </div>

                  <div className="py-1">
                    <Link
                      to="/profile"
                      className="flex items-center gap-2.5 px-3 py-1.5 text-xs font-medium text-zinc-700 hover:text-zinc-950 hover:bg-zinc-100 transition-colors"
                    >
                      <UserIcon className="w-4 h-4 text-zinc-400" />
                      <span>Hồ sơ tài khoản</span>
                    </Link>

                    <Link
                      to="/orders"
                      className="flex items-center gap-2.5 px-3 py-1.5 text-xs font-medium text-zinc-700 hover:text-zinc-950 hover:bg-zinc-100 transition-colors"
                    >
                      <Package className="w-4 h-4 text-zinc-400" />
                      <span>Đơn hàng của tôi</span>
                    </Link>

                    {user.role === 'ADMIN' && (
                      <Link
                        to="/admin/users"
                        className="flex items-center gap-2.5 px-3 py-1.5 text-xs font-medium text-zinc-900 hover:bg-zinc-100 transition-colors"
                      >
                        <Shield className="w-4 h-4 text-zinc-700" />
                        <span>Khu vực quản trị Admin</span>
                      </Link>
                    )}
                  </div>

                  <div className="pt-1 border-t border-zinc-100">
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="w-full flex items-center gap-2.5 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors text-left cursor-pointer"
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
                className="text-xs font-medium text-zinc-700 hover:text-zinc-950 px-3 py-2 rounded-md hover:bg-zinc-100 transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="text-xs font-medium bg-zinc-900 hover:bg-zinc-800 text-zinc-50 px-3.5 py-2 rounded-md shadow-sm transition-colors"
              >
                Đăng ký
              </Link>
            </div>
          )}

          {/* Mobile menu toggle */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-md text-zinc-600 hover:text-zinc-950 hover:bg-zinc-100 transition-colors"
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? <X className="w-4 h-4" /> : <Menu className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-zinc-200 bg-white px-4 py-3 space-y-1 shadow-sm animate-in slide-in-from-top-1">
          <Link
            to="/"
            className={`block px-3 py-2 rounded-md text-sm font-medium ${
              isActive('/') ? 'bg-zinc-100 text-zinc-950 font-medium' : 'text-zinc-700 hover:bg-zinc-50'
            }`}
          >
            Trang chủ
          </Link>
          <Link
            to="/products"
            className={`block px-3 py-2 rounded-md text-sm font-medium ${
              isActive('/products') ? 'bg-zinc-100 text-zinc-950 font-medium' : 'text-zinc-700 hover:bg-zinc-50'
            }`}
          >
            Sản phẩm
          </Link>

          {isAuthenticated ? (
            <>
              <Link
                to="/profile"
                className={`block px-3 py-2 rounded-md text-sm font-medium ${
                  isActive('/profile') ? 'bg-zinc-100 text-zinc-950 font-medium' : 'text-zinc-700 hover:bg-zinc-50'
                }`}
              >
                Hồ sơ tài khoản
              </Link>
              <Link
                to="/orders"
                className={`block px-3 py-2 rounded-md text-sm font-medium ${
                  isActive('/orders') ? 'bg-zinc-100 text-zinc-950 font-medium' : 'text-zinc-700 hover:bg-zinc-50'
                }`}
              >
                Đơn hàng của tôi
              </Link>
              {user?.role === 'ADMIN' && (
                <Link
                  to="/admin/users"
                  className="block px-3 py-2 rounded-md text-sm font-medium text-zinc-900 bg-zinc-100 border border-zinc-200"
                >
                  Khu vực quản trị
                </Link>
              )}
              <button
                type="button"
                onClick={handleLogout}
                className="w-full text-left px-3 py-2 rounded-md text-sm font-medium text-red-600 hover:bg-red-50 flex items-center gap-2 pt-2 border-t border-zinc-100 mt-2"
              >
                <LogOut className="w-4 h-4" />
                <span>Đăng xuất</span>
              </button>
            </>
          ) : (
            <div className="pt-2 border-t border-zinc-100 flex flex-col gap-2">
              <Link
                to="/login"
                className="w-full text-center py-2 text-sm font-medium text-zinc-700 bg-zinc-100 hover:bg-zinc-200/80 rounded-md transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="w-full text-center py-2 text-sm font-medium text-zinc-50 bg-zinc-900 hover:bg-zinc-800 rounded-md transition-colors shadow-sm"
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
