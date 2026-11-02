import React from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, ShieldCheck, Truck, RotateCcw, Headphones } from 'lucide-react'

export const Footer = () => {
  return (
    <footer className="bg-slate-900 text-slate-400 text-xs border-t border-slate-800 mt-auto">
      {/* 4 Pillars Trust Bar */}
      <div className="border-b border-slate-800/80 bg-slate-950/40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <Truck className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-200">Giao hàng toàn quốc</div>
                <div className="text-[11px] text-slate-500">Miễn phí từ 500.000₫</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-200">Chính hãng 100%</div>
                <div className="text-[11px] text-slate-500">Bảo đảm nguồn gốc</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-200">Đổi trả 30 ngày</div>
                <div className="text-[11px] text-slate-500">Thủ tục đơn giản</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <Headphones className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-200">Hỗ trợ 24/7</div>
                <div className="text-[11px] text-slate-500">Hotline 1900 xxxx</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-7 h-7 rounded-lg bg-primary-600 text-white flex items-center justify-center font-bold">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <span className="font-extrabold text-sm text-white">Rikkei Commerce</span>
            </div>
            <p className="text-slate-400 text-xs leading-relaxed">
              Dự án thực tập E-commerce Platform xây dựng bởi DevOps Team. Ứng dụng kiến trúc Microservices hiện đại, CI/CD và Cloud Native.
            </p>
          </div>

          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Về chúng tôi</h4>
            <ul className="space-y-2 text-slate-400">
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Giới thiệu công ty
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Tuyển dụng DevOps
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Điều khoản dịch vụ
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Chính sách bảo mật
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Hỗ trợ khách hàng</h4>
            <ul className="space-y-2 text-slate-400">
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Hướng dẫn mua hàng
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Phương thức thanh toán
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Chính sách vận chuyển
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-primary-400 transition-colors">
                  Chính sách đổi trả
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-white text-xs uppercase tracking-wider mb-3">Cổng Quản trị</h4>
            <p className="text-slate-400 text-xs mb-3">
              Dành riêng cho Quản trị viên và Vận hành hệ thống.
            </p>
            <Link
              to="/admin/login"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-primary-400 hover:text-primary-300 font-semibold transition-colors border border-slate-700/60"
            >
              <span>Đăng nhập Admin</span>
              <span>→</span>
            </Link>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-slate-800/80 mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
          <div>
            © {new Date().getFullYear()} Rikkei E-commerce Platform. Thực tập sinh [IT-209] Devops Fundamentals.
          </div>
          <div className="flex items-center gap-4">
            <span>Powered by React 18, Vite & Tailwind CSS</span>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
