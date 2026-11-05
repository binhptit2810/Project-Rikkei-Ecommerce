import React, { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { 
  ShoppingCart, 
  User as UserIcon,
  Shield, 
  LogOut, 
  Menu, 
  X,
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
    }
  }

  const getInitials = (name) => {
    if (!name) return 'U'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  const isActive = (path) => location.pathname === path

  return (
    <header className="sticky top-0 z-40 w-full border-b border-zinc-200 bg-white/95 backdrop-blur-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between gap-4">
        {/* Brand Logo */}
        <Link 
          to="/" 
          className="flex items-center gap-2 text-zinc-950 font-semibold tracking-tight text-base hover:opacity-90 transition-opacity"
        >
          <div className="w-7 h-7 rounded-md bg-zinc-900 text-zinc-50 flex items-center justify-center font-bold text-xs shadow-xs">
            R
          </div>
          <span className="hidden sm:inline font-bold">Rikkei E-commerce</span>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-1 text-sm font-medium">
          <Link
            to="/"
            className={`px-3 py-1.5 rounded-md transition-colors ${
              isActive('/') 
                ? 'bg-zinc-100 text-zinc-950 font-medium' 
                : 'text-zinc-600 hover:text-zinc-950 hover:bg-zinc-50'
            }`}
          >
            Trang chủ
          </Link>
          <Link
            to="/products"
            className={`px-3 py-1.5 rounded-md transition-colors ${
              isActive('/products') 
                ? 'bg-zinc-100 text-zinc-950 font-medium' 
                : 'text-zinc-600 hover:text-zinc-950 hover:bg-zinc-50'
            }`}
          >
            Sản phẩm
          </Link>

          {isAuthenticated && (
            <Link
              to="/orders"
              className={`px-3 py-1.5 rounded-md transition-colors ${
                isActive('/orders') 
                  ? 'bg-zinc-100 text-zinc-950 font-medium' 
                  : 'text-zinc-600 hover:text-zinc-950 hover:bg-zinc-50'
              }`}
            >
              Đơn hàng
            </Link>
          )}

          {isAuthenticated && user?.role === 'ADMIN' && (
            <Link
              to="/admin/users"
              className={`px-3 py-1.5 rounded-md transition-colors flex items-center gap-1.5 ${
                location.pathname.startsWith('/admin')
                  ? 'bg-zinc-900 text-zinc-50 font-medium'
                  : 'text-zinc-700 hover:text-zinc-950 hover:bg-zinc-100'
              }`}
            >
              <Shield className="w-3.5 h-3.5" />
              <span>Quản trị</span>
            </Link>
          )}
        </nav>

        {/* Right Action Icons & User Menu */}
        <div className="flex items-center gap-2">
          {/* Cart Button */}
          <Link
            to="/cart"
            onClick={handleCartClick}
            className="relative p-2 text-zinc-700 hover:text-zinc-950 hover:bg-zinc-100 rounded-md transition-colors flex items-center justify-center cursor-pointer"
            title="Giỏ hàng"
          >
            <ShoppingCart className="w-4 h-4" />
            {isAuthenticated && totalCartCount > 0 && (
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
                className="flex items-center gap-2 p-1 pl-2 rounded-md border border-zinc-200 bg-white hover:bg-zinc-50 text-zinc-800 transition-colors cursor-pointer"
              >
                <div className="w-6 h-6 rounded-full bg-zinc-900 text-zinc-50 font-medium text-xs flex items-center justify-center">
                  {getInitials(user.fullName || user.email)}
                </div>
                <span className="text-xs font-medium max-w-[120px] truncate hidden sm:inline">
                  {user.fullName || user.email.split('@')[0]}
                </span>
              </button>

              {/* User Dropdown */}
              {userDropdownOpen && (
                <div className="absolute right-0 mt-1.5 w-56 rounded-md border border-zinc-200 bg-white shadow-md py-1 z-50 text-xs animate-in fade-in zoom-in-95 duration-100">
                  <div className="px-3 py-2 border-b border-zinc-100">
                    <p className="font-semibold text-zinc-950 truncate">{user.fullName || 'Người dùng'}</p>
                    <p className="text-zinc-500 text-[11px] font-mono truncate">{user.email}</p>
                    <div className="mt-1">
                      <span className="badge-neutral text-[10px] py-0 px-1.5">
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
