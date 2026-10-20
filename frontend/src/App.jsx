import React, { useState, useEffect } from 'react'
import apiClient from './services/apiClient'

function App() {
  const [healthStatus, setHealthStatus] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const checkBackendHealth = async () => {
    setLoading(true)
    setError(null)
    try {
      // Thử gọi /health hoặc /api/health
      const response = await apiClient.get('/health')
      setHealthStatus(response.data)
    } catch (err) {
      try {
        // Fallback kiểm tra /api/v1/health
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
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col justify-between">
      {/* Navigation Header */}
      <header className="bg-white border-b border-slate-200 shadow-sm sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl">🛒</span>
            <span className="font-bold text-xl text-orange-600 tracking-tight">E-commerce Platform</span>
          </div>
          <div className="text-sm font-medium bg-orange-50 text-orange-700 px-3 py-1 rounded-full border border-orange-200">
            Base Project v1.0
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-6xl mx-auto px-4 py-10 flex-1 w-full">
        {/* Hero Section */}
        <div className="bg-gradient-to-br from-orange-500 via-orange-600 to-amber-600 text-white rounded-2xl p-8 sm:p-12 shadow-lg mb-10">
          <div className="max-w-3xl">
            <span className="inline-block bg-white/20 text-white text-xs font-semibold px-3 py-1 rounded-full uppercase tracking-wider mb-4 backdrop-blur-sm">
              Đồ án môn học
            </span>
            <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight mb-4">
              E-commerce Platform
            </h1>
            <p className="text-orange-100 text-base sm:text-lg leading-relaxed mb-6">
              Bộ khung dự án cơ bản (React + Spring Boot + MySQL) dành cho nhóm sinh viên. 
              Các thành viên có thể bắt đầu phát triển các module chức năng độc lập theo phân công.
            </p>
            <div className="flex flex-wrap gap-4 items-center">
              <button
                onClick={checkBackendHealth}
                disabled={loading}
                className="bg-white text-orange-600 font-semibold px-5 py-2.5 rounded-xl shadow hover:bg-orange-50 active:scale-95 transition-all text-sm flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                {loading ? 'Đang kiểm tra...' : '🔄 Kiểm tra Backend API'}
              </button>
              <span className="text-xs text-orange-100">
                Backend Endpoint: <code className="bg-black/20 px-2 py-0.5 rounded">GET /api/health</code>
              </span>
            </div>
          </div>
        </div>

        {/* Backend Status Card */}
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 mb-8">
          <h2 className="text-lg font-bold text-slate-800 mb-3 flex items-center gap-2">
            <span>🔌</span> Trạng thái kết nối Backend
          </h2>
          {loading && <p className="text-slate-500 text-sm">Đang gửi yêu cầu kiểm tra tới backend...</p>}
          {error && (
            <div className="bg-amber-50 border border-amber-200 text-amber-800 text-sm rounded-lg p-4 flex items-start gap-3">
              <span className="text-amber-500 text-lg">⚠️</span>
              <div>
                <p className="font-semibold">Chưa kết nối được với Backend:</p>
                <p className="text-xs mt-1 text-amber-700">{error}</p>
                <p className="text-xs mt-2 text-slate-600">
                  (Hãy đảm bảo Spring Boot backend đang chạy tại cổng <code>http://localhost:8080</code>)
                </p>
              </div>
            </div>
          )}
          {healthStatus && (
            <div className="bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm rounded-lg p-4">
              <div className="flex items-center gap-2 font-semibold text-emerald-700 mb-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                Backend đang hoạt động tốt (Status: UP)
              </div>
              <pre className="text-xs bg-white/70 p-3 rounded border border-emerald-100 overflow-x-auto text-slate-700 font-mono">
                {JSON.stringify(healthStatus, null, 2)}
              </pre>
            </div>
          )}
        </section>

        {/* Tech Stack Overview */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="text-2xl mb-2">⚛️</div>
            <h3 className="font-bold text-slate-800 text-base mb-1">Frontend</h3>
            <p className="text-xs text-slate-500 mb-3">React 18 + Vite + Tailwind CSS</p>
            <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
              <li>SPA gọn nhẹ, tải trang nhanh</li>
              <li>Cấu hình proxy gọi API qua Vite</li>
              <li>Axios client cấu hình sẵn</li>
            </ul>
          </div>
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="text-2xl mb-2">🍃</div>
            <h3 className="font-bold text-slate-800 text-base mb-1">Backend</h3>
            <p className="text-xs text-slate-500 mb-3">Java 21 + Spring Boot 3.3.5 + Gradle</p>
            <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
              <li>API GET <code>/api/health</code> kiểm tra trạng thái</li>
              <li>CORS cấu hình cho Vite frontend</li>
              <li>Global Exception Handler chuẩn</li>
            </ul>
          </div>
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="text-2xl mb-2">🐬</div>
            <h3 className="font-bold text-slate-800 text-base mb-1">Database & DevOps</h3>
            <p className="text-xs text-slate-500 mb-3">MySQL 8.0 + Docker + CI/CD</p>
            <ul className="text-xs text-slate-600 space-y-1 list-disc list-inside">
              <li>File SQL khởi tạo database <code>01-schema.sql</code></li>
              <li>Docker Compose đồng bộ môi trường</li>
              <li>Tài liệu SRS, TEAM-WORK, README đầy đủ</li>
            </ul>
          </div>
        </section>

        {/* Team Work Allocation Table */}
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
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
          <p className="text-xs text-slate-400 mt-4 italic">
            * Chi tiết mô tả nhiệm vụ, checklist tiêu chí hoàn thành xem tại file <code>docs/TEAM-WORK.md</code>.
          </p>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        <p>E-commerce Platform — Base Project for Students • React + Spring Boot + MySQL</p>
      </footer>
    </div>
  )
}

export default App
