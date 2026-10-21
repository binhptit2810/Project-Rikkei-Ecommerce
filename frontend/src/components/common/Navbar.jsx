import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

export const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const getInitial = (name) => {
    if (!name) return 'U'
    const parts = name.trim().split(' ')
    return parts[parts.length - 1].charAt(0).toUpperCase()
  }

  const isActive = (path) => location.pathname === path

  return (
    <header className="bg-white border-b border-slate-200 shadow-sm sticky top-0 z-30">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        {/* Brand Logo */}
        <Link to="/" className="flex items-center gap-3 group">
          <span className="text-2xl transform group-hover:scale-110 transition-transform">🛒</span>
          <div className="flex flex-col">
            <span className="font-bold text-xl text-orange-600 tracking-tight">
              E-commerce Platform
            </span>
          </div>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-6">
          <Link
            to="/"
            className={`text-sm font-medium transition-colors ${
              isActive('/')
                ? 'text-orange-600 font-semibold'
                : 'text-slate-600 hover:text-orange-600'
            }`}
          >
            Trang chủ
          </Link>

          {isAuthenticated && (
            <Link
              to="/profile"
              className={`text-sm font-medium transition-colors ${
                isActive('/profile')
                  ? 'text-orange-600 font-semibold'
                  : 'text-slate-600 hover:text-orange-600'
              }`}
            >
              Hồ sơ của tôi
            </Link>
          )}
        </nav>

        {/* User Auth Section */}
        <div className="hidden md:flex items-center gap-3">
          {isAuthenticated && user ? (
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200">
              <Link
                to="/profile"
                className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-slate-50 transition-colors"
                title="Xem hồ sơ cá nhân"
              >
                <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-orange-500 to-amber-400 text-white font-bold text-sm flex items-center justify-center shadow-sm">
                  {getInitial(user.fullName)}
                </div>
                <div className="text-left">
                  <div className="text-xs font-bold text-slate-800 leading-tight max-w-[130px] truncate">
                    {user.fullName || user.email}
                  </div>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <span
                      className={`text-[10px] font-semibold px-1.5 py-0.2 rounded-full uppercase tracking-wider ${
                        user.role === 'ADMIN'
                          ? 'bg-purple-100 text-purple-700 border border-purple-200'
                          : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                      }`}
                    >
                      {user.role}
                    </span>
                  </div>
                </div>
              </Link>

              <button
                onClick={handleLogout}
                className="text-xs font-semibold text-slate-500 hover:text-red-600 hover:bg-red-50 p-2 rounded-lg transition-colors flex items-center gap-1.5 cursor-pointer"
                title="Đăng xuất khỏi hệ thống"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                <span>Đăng xuất</span>
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="text-sm font-semibold text-slate-700 hover:text-orange-600 px-3.5 py-2 rounded-lg hover:bg-slate-100 transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                to="/register"
                className="text-sm font-semibold bg-orange-600 hover:bg-orange-700 text-white px-4 py-2 rounded-xl shadow-sm hover:shadow transition-all"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>

        {/* Mobile menu button */}
        <div className="md:hidden flex items-center">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 focus:outline-none"
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {mobileMenuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>
      </div>

      {/* Mobile menu dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 pt-2 pb-4 space-y-2">
          <Link
            to="/"
            onClick={() => setMobileMenuOpen(false)}
            className="block py-2 text-sm font-medium text-slate-700 hover:text-orange-600"
          >
            Trang chủ
          </Link>
          {isAuthenticated && (
            <Link
              to="/profile"
              onClick={() => setMobileMenuOpen(false)}
              className="block py-2 text-sm font-medium text-slate-700 hover:text-orange-600"
            >
              Hồ sơ của tôi ({user?.fullName})
            </Link>
          )}

          <div className="pt-2 border-t border-slate-100">
            {isAuthenticated ? (
              <button
                onClick={() => {
                  setMobileMenuOpen(false)
                  handleLogout()
                }}
                className="w-full text-left py-2 text-sm font-semibold text-red-600 flex items-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                </svg>
                Đăng xuất ({user?.email})
              </button>
            ) : (
              <div className="grid grid-cols-2 gap-2 pt-1">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-center py-2 text-sm font-semibold text-slate-700 border border-slate-300 rounded-lg"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-center py-2 text-sm font-semibold bg-orange-600 text-white rounded-lg"
                >
                  Đăng ký
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  )
}

export default Navbar
