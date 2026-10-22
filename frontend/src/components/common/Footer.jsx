import React from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag, ShieldCheck, Truck, RotateCcw, Headphones, Heart } from 'lucide-react'

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
                <div className="font-semibold text-white text-xs">Giao hàng toàn quốc</div>
                <div className="text-[11px] text-slate-500">Miễn phí cho đơn từ 500k</div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-white text-xs">Đổi trả 7 ngày</div>
                <div className="text-[11px] text-slate-500">Thủ tục nhanh chóng, an tâm</div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-white text-xs">100% Chính hãng</div>
                <div className="text-[11px] text-slate-500">Cam kết nguồn gốc minh bạch</div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-800 text-primary-400 flex items-center justify-center shrink-0">
                <Headphones className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-white text-xs">Hỗ trợ 24/7</div>
                <div className="text-[11px] text-slate-500">Hotline và tư vấn trực tuyến</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Info */}
          <div className="space-y-4">
            <Link to="/" className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-primary-600 text-white flex items-center justify-center">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <span className="font-bold text-base text-white tracking-tight">
                Rikkei<span className="text-primary-500">Store</span>
              </span>
            </Link>
            <p className="text-slate-400 text-xs leading-relaxed max-w-xs">
              Nền tảng thương mại điện tử trực tuyến hiện đại, mang đến trải nghiệm mua sắm tinh gọn, tin cậy và tiện lợi cho mọi khách hàng.
            </p>
          </div>

          {/* Quick Categories */}
          <div>
            <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-3">Danh mục nổi bật</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/products?categoryId=1" className="hover:text-white transition-colors">
                  Thời trang nam nữ
                </Link>
              </li>
              <li>
                <Link to="/products?categoryId=2" className="hover:text-white transition-colors">
                  Thiết bị điện tử & Phụ kiện
                </Link>
              </li>
              <li>
                <Link to="/products?categoryId=3" className="hover:text-white transition-colors">
                  Đồ gia dụng & Đời sống
                </Link>
              </li>
              <li>
                <Link to="/products?categoryId=4" className="hover:text-white transition-colors">
                  Sách & Văn phòng phẩm
                </Link>
              </li>
              <li>
                <Link to="/products?categoryId=5" className="hover:text-white transition-colors">
                  Thể thao & Dã ngoại
                </Link>
              </li>
            </ul>
          </div>

          {/* Customer Service */}
          <div>
            <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-3">Hỗ trợ khách hàng</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/products" className="hover:text-white transition-colors">
                  Hướng dẫn đặt hàng
                </Link>
              </li>
              <li>
                <Link to="/orders" className="hover:text-white transition-colors">
                  Tra cứu đơn hàng
                </Link>
              </li>
              <li>
                <span className="text-slate-500">Chính sách bảo mật thanh toán</span>
              </li>
              <li>
                <span className="text-slate-500">Điều khoản & Điều kiện giao dịch</span>
              </li>
            </ul>
          </div>

          {/* Project Details */}
          <div>
            <h4 className="font-semibold text-white text-xs uppercase tracking-wider mb-3">Thông tin dự án</h4>
            <p className="text-xs text-slate-400 leading-relaxed">
              Đồ án môn học Công nghệ Thông tin — E-commerce Platform Architecture.
            </p>
            <div className="mt-3 text-[11px] text-slate-500 space-y-1">
              <div>Backend: Spring Boot 3.3.5 / Java 21</div>
              <div>Frontend: React 18 / Vite / Tailwind CSS</div>
              <div>Database: MySQL 8.0 & Redis 7</div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Copyright Bar */}
      <div className="border-t border-slate-800/80 bg-slate-950/70 py-4">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-[11px] text-slate-500">
          <p>© {new Date().getFullYear()} RikkeiStore E-commerce. Tất cả các quyền được bảo lưu.</p>
          <p className="flex items-center gap-1">
            <span>Thiết kế & phát triển với tiêu chuẩn chất lượng cao</span>
          </p>
        </div>
      </div>
    </footer>
  )
}

export default Footer
