import React from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, ShieldCheck, Truck, RotateCcw, Headphones, ArrowRight } from 'lucide-react'

export const Footer = () => {
  return (
    <footer className="bg-zinc-950 text-zinc-400 text-xs border-t border-zinc-800 mt-auto">
      {/* 4 Pillars Trust Bar */}
      <div className="border-b border-zinc-800/80 bg-zinc-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-md bg-zinc-900 text-zinc-100 flex items-center justify-center shrink-0 border border-zinc-800">
                <Truck className="w-4 h-4" />
              </div>
              <div>
                <div className="font-medium text-zinc-200">Giao hàng toàn quốc</div>
                <div className="text-[11px] text-zinc-500">Miễn phí từ 500.000₫</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-md bg-zinc-900 text-zinc-100 flex items-center justify-center shrink-0 border border-zinc-800">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <div>
                <div className="font-medium text-zinc-200">Chính hãng 100%</div>
                <div className="text-[11px] text-zinc-500">Bảo đảm nguồn gốc</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-md bg-zinc-900 text-zinc-100 flex items-center justify-center shrink-0 border border-zinc-800">
                <RotateCcw className="w-4 h-4" />
              </div>
              <div>
                <div className="font-medium text-zinc-200">Đổi trả 30 ngày</div>
                <div className="text-[11px] text-zinc-500">Thủ tục đơn giản</div>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-md bg-zinc-900 text-zinc-100 flex items-center justify-center shrink-0 border border-zinc-800">
                <Headphones className="w-4 h-4" />
              </div>
              <div>
                <div className="font-medium text-zinc-200">Hỗ trợ 24/7</div>
                <div className="text-[11px] text-zinc-500">Hotline 1900 xxxx</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-6 h-6 rounded-md bg-zinc-100 text-zinc-950 flex items-center justify-center font-bold">
                <ShoppingBag className="w-3.5 h-3.5 text-zinc-900" />
              </div>
              <span className="font-semibold text-sm text-zinc-100 tracking-tight">Rikkei Store</span>
            </div>
            <p className="text-zinc-400 text-xs leading-relaxed">
              Dự án E-commerce Platform xây dựng bởi DevOps Team. Ứng dụng kiến trúc Microservices hiện đại, CI/CD và Cloud Native.
            </p>
          </div>

          <div>
            <h4 className="font-medium text-zinc-100 text-xs uppercase tracking-wider mb-3">Về chúng tôi</h4>
            <ul className="space-y-2 text-zinc-400">
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Giới thiệu công ty
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Tuyển dụng DevOps
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Điều khoản dịch vụ
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Chính sách bảo mật
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-medium text-zinc-100 text-xs uppercase tracking-wider mb-3">Hỗ trợ khách hàng</h4>
            <ul className="space-y-2 text-zinc-400">
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Hướng dẫn mua hàng
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Phương thức thanh toán
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Chính sách vận chuyển
                </Link>
              </li>
              <li>
                <Link to="/" className="hover:text-zinc-100 transition-colors">
                  Chính sách đổi trả
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="font-medium text-zinc-100 text-xs uppercase tracking-wider mb-3">Cổng Quản trị</h4>
            <p className="text-zinc-400 text-xs mb-3">
              Dành riêng cho Quản trị viên và Vận hành hệ thống.
            </p>
            <Link
              to="/admin/login"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-zinc-900 hover:bg-zinc-800 text-zinc-200 hover:text-white font-medium transition-colors border border-zinc-800"
            >
              <span>Đăng nhập Admin</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-zinc-800 mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-zinc-500">
          <div>
            © {new Date().getFullYear()} Rikkei E-commerce Platform. All rights reserved.
          </div>
          <div className="flex items-center gap-4">
            <span>React 18 · Vite · Tailwind CSS · shadcn/ui</span>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
