import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import apiClient from '../services/apiClient'
import { useAuthStore } from '../store/useAuthStore'

export const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore()
  const [healthStatus, setHealthStatus] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const checkBackendHealth = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiClient.get('/health')
      setHealthStatus(response.data)
    } catch (err) {
      try {
        const fallbackRes = await apiClient.get('/v1/health')
        setHealthStatus(fallbackRes.data)
      } catch (fallbackErr) {
        setError(fallbackErr.message || 'Không thể kết nối đến Backend')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    checkBackendHealth()
  }, [])

  const teamMembers = [
    { stt: 1, name: 'Cao Chí Thiện', role: 'Team Lead / Backend Core', task: 'Auth, User Profile, Security & Tích hợp' },
    { stt: 2, name: 'Đỗ Đức Mạnh', role: 'Backend + Frontend', task: 'Product, Category, Search' },
    { stt: 3, name: 'Vũ Đức Huy Hoàng', role: 'Backend + Frontend', task: 'Cart, Order, Checkout' },
    { stt: 4, name: 'Đặng Thanh Bình', role: 'Backend + Frontend', task: 'Inventory, Payment, Admin UI' },
    { stt: 5, name: 'Đỗ Hoàng Anh', role: 'Frontend + DevOps', task: 'Layout, Routing, Docker & CI/CD' },
  ]

  return (
    <main className="max-w-6xl mx-auto px-4 py-8 flex-1 w-full">
      {/* Hero Section */}
      <div className="bg-gradient-to-br from-orange-500 via-orange-600 to-amber-600 text-white rounded-3xl p-8 sm:p-12 shadow-xl mb-10 relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-white/10 rounded-full blur-3xl pointer-events-none transform translate-x-1/2 -translate-y-1/2"></div>
        <div className="max-w-3xl relative z-10">
          <div className="flex items-center gap-2 mb-4">
            <span className="bg-white/20 text-white text-xs font-semibold px-3 py-1 rounded-full uppercase tracking-wider backdrop-blur-sm">
              Đồ án môn học
            </span>
            {isAuthenticated && (
              <span className="bg-emerald-500/80 text-white text-xs font-semibold px-3 py-1 rounded-full backdrop-blur-sm flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-white animate-pulse"></span>
                Xin chào, {user?.fullName || user?.email}
              </span>
            )}
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight mb-4">
            E-commerce Platform
          </h1>
          <p className="text-orange-100 text-base sm:text-lg leading-relaxed mb-6">
            Hệ thống thương mại điện tử hoàn chỉnh (React 18 + Spring Boot 3.3.5 + MySQL 8.0).
            Module <strong>Xác thực & Quản lý Người dùng (Auth & User Profile)</strong> đã được hoàn thiện.
          </p>

          <div className="flex flex-wrap gap-3 items-center">
            {isAuthenticated ? (
              <Link
                to="/profile"
                className="bg-white text-orange-600 font-bold px-6 py-3 rounded-xl shadow-md hover:bg-orange-50 active:scale-95 transition-all text-sm inline-flex items-center gap-2"
              >
                <span>👤</span> Xem hồ sơ của bạn
              </Link>
            ) : (
              <>
                <Link
                  to="/login"
                  className="bg-white text-orange-600 font-bold px-6 py-3 rounded-xl shadow-md hover:bg-orange-50 active:scale-95 transition-all text-sm inline-flex items-center gap-2"
                >
                  <span>🔐</span> Đăng nhập ngay
                </Link>
                <Link
                  to="/register"
                  className="bg-orange-700/60 hover:bg-orange-700/80 text-white font-semibold px-6 py-3 rounded-xl border border-white/20 backdrop-blur-sm active:scale-95 transition-all text-sm"
                >
                  Tạo tài khoản mới
                </Link>
              </>
            )}

            <button
              onClick={checkBackendHealth}
              disabled={loading}
              className="bg-black/20 hover:bg-black/30 text-white font-medium px-4 py-3 rounded-xl backdrop-blur-sm active:scale-95 transition-all text-sm flex items-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {loading ? 'Đang kiểm tra...' : '🔄 Kiểm tra Backend API'}
            </button>
          </div>
        </div>
      </div>

      {/* Backend Status Card */}
      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
            <span>🔌</span> Trạng thái kết nối Backend
          </h2>
          <span className="text-xs text-slate-400">
            Endpoint: <code className="bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-mono">GET /api/v1/health</code>
          </span>
        </div>

        {loading && (
          <p className="text-slate-500 text-sm flex items-center gap-2">
            <span className="w-3 h-3 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></span>
            Đang gửi yêu cầu kiểm tra tới backend...
          </p>
        )}

        {error && (
          <div className="bg-amber-50 border border-amber-200 text-amber-900 text-sm rounded-xl p-4 flex items-start gap-3">
            <span className="text-amber-500 text-xl">⚠️</span>
            <div>
              <p className="font-bold">Chưa kết nối được với Backend:</p>
              <p className="text-xs mt-1 text-amber-700">{error}</p>
              <p className="text-xs mt-2 text-slate-600">
                (Hãy đảm bảo Spring Boot backend đang chạy tại cổng <code>http://localhost:8080</code>)
              </p>
            </div>
          </div>
        )}

        {healthStatus && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm rounded-xl p-4">
            <div className="flex items-center gap-2 font-semibold text-emerald-700 mb-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Backend đang hoạt động tốt (Status: UP)
            </div>
            <pre className="text-xs bg-white/80 p-3 rounded-lg border border-emerald-100 overflow-x-auto text-slate-700 font-mono">
              {JSON.stringify(healthStatus, null, 2)}
            </pre>
          </div>
        )}
      </section>

      {/* Auth & Security Status Module Card */}
      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 mb-8">
        <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span>🛡️</span> Module Auth & User Profile (Cao Chí Thiện)
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div className="p-4 rounded-xl bg-orange-50/60 border border-orange-100">
            <div className="text-orange-600 font-bold text-sm mb-1">Đăng ký & Đăng nhập</div>
            <p className="text-xs text-slate-600">
              JWT Bearer (HMAC-SHA256, JJWT 0.12.6), BCrypt 12 hash, validation đầy đủ.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-blue-50/60 border border-blue-100">
            <div className="text-blue-600 font-bold text-sm mb-1">Hồ sơ người dùng</div>
            <p className="text-xs text-slate-600">
              Lấy thông tin và cập nhật hồ sơ (fullName, phone, address), bảo vệ chống IDOR.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-purple-50/60 border border-purple-100">
            <div className="text-purple-600 font-bold text-sm mb-1">Đổi mật khẩu</div>
            <p className="text-xs text-slate-600">
              Xác thực mật khẩu hiện tại, kiểm tra không trùng lặp, mã hóa an toàn.
            </p>
          </div>
          <div className="p-4 rounded-xl bg-emerald-50/60 border border-emerald-100">
            <div className="text-emerald-600 font-bold text-sm mb-1">Phân quyền Route</div>
            <p className="text-xs text-slate-600">
              ProtectedRoute, RoleRoute (ADMIN / CUSTOMER), tự động xử lý khi hết hạn phiên.
            </p>
          </div>
        </div>
      </section>

      {/* Tech Stack Overview */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-3xl mb-3">⚛️</div>
          <h3 className="font-bold text-slate-800 text-base mb-1">Frontend</h3>
          <p className="text-xs text-slate-500 mb-3">React 18 + Vite + Tailwind CSS + Zustand</p>
          <ul className="text-xs text-slate-600 space-y-1.5 list-disc list-inside">
            <li>Quản lý state xác thực bằng Zustand</li>
            <li>Route Guard (Protected & Role based)</li>
            <li>Axios interceptor tự động gắn token</li>
          </ul>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-3xl mb-3">🍃</div>
          <h3 className="font-bold text-slate-800 text-base mb-1">Backend</h3>
          <p className="text-xs text-slate-500 mb-3">Java 21 + Spring Boot 3.3.5 + Spring Security</p>
          <ul className="text-xs text-slate-600 space-y-1.5 list-disc list-inside">
            <li>Stateless JWT Security Filter Chain</li>
            <li>Xử lý tài khoản disabled (403 Forbidden)</li>
            <li>Global Exception Handler chuẩn ApiResponse</li>
          </ul>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-3xl mb-3">🐬</div>
          <h3 className="font-bold text-slate-800 text-base mb-1">Database & DevOps</h3>
          <p className="text-xs text-slate-500 mb-3">MySQL 8.0 + Docker + CI/CD</p>
          <ul className="text-xs text-slate-600 space-y-1.5 list-disc list-inside">
            <li>Bảng users với enum role: CUSTOMER, ADMIN</li>
            <li>Docker Compose đồng bộ môi trường</li>
            <li>Tài liệu SRS, TEAM-WORK, API.md đầy đủ</li>
          </ul>
        </div>
      </section>

      {/* Team Work Allocation Table */}
      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6">
        <h2 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span>👥</span> Phân công công việc nhóm sinh viên
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-700 text-xs uppercase border-b border-slate-200">
              <tr>
                <th className="py-3 px-4 w-12 text-center">STT</th>
                <th className="py-3 px-4">Thành viên</th>
                <th className="py-3 px-4">Vai trò</th>
                <th className="py-3 px-4">Phụ trách Module</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {teamMembers.map((member) => (
                <tr key={member.stt} className="hover:bg-slate-50/50">
                  <td className="py-3 px-4 text-center font-medium text-slate-500">{member.stt}</td>
                  <td className="py-3 px-4 font-semibold text-slate-800">{member.name}</td>
                  <td className="py-3 px-4 text-slate-600">{member.role}</td>
                  <td className="py-3 px-4 text-orange-600 font-medium">{member.task}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}

export default HomePage
