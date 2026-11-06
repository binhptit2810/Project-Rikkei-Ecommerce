import React from 'react'
import { Link } from 'react-router-dom'
import { 
  Zap, 
  ShieldCheck, 
  Truck, 
  RotateCcw, 
  Headphones, 
  Send,
  Mail,
  MapPin,
  Phone
} from 'lucide-react'

export const Footer = () => {
  return (
    <footer className="bg-[#070d1e] text-slate-400 text-xs border-t border-slate-800/80 mt-auto">
      {/* 4 Pillars Trust Bar */}
      <div className="border-b border-slate-800/80 bg-[#091126]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-white/5 border border-white/5">
              <div className="w-11 h-11 rounded-xl bg-blue-600/15 text-blue-400 flex items-center justify-center shrink-0 border border-blue-500/20 shadow-xs">
                <Truck className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-100 text-sm">Giao hàng siêu tốc</div>
                <div className="text-xs text-slate-400">Miễn phí cho đơn từ 500.000₫</div>
              </div>
            </div>

            <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-white/5 border border-white/5">
              <div className="w-11 h-11 rounded-xl bg-orange-600/15 text-orange-400 flex items-center justify-center shrink-0 border border-orange-500/20 shadow-xs">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-100 text-sm">Chính hãng 100%</div>
                <div className="text-xs text-slate-400">Bảo hành 1-2 năm từ hãng</div>
              </div>
            </div>

            <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-white/5 border border-white/5">
              <div className="w-11 h-11 rounded-xl bg-emerald-600/15 text-emerald-400 flex items-center justify-center shrink-0 border border-emerald-500/20 shadow-xs">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-100 text-sm">Đổi trả 7 ngày</div>
                <div className="text-xs text-slate-400">Thủ tục nhanh chóng, an tâm</div>
              </div>
            </div>

            <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-white/5 border border-white/5">
              <div className="w-11 h-11 rounded-xl bg-indigo-600/15 text-indigo-400 flex items-center justify-center shrink-0 border border-indigo-500/20 shadow-xs">
                <Headphones className="w-5 h-5" />
              </div>
              <div>
                <div className="font-semibold text-slate-100 text-sm">Hỗ trợ 24/7</div>
                <div className="text-xs text-slate-400">Chuyên viên tư vấn tận tâm</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10">
          
          {/* Brand Info */}
          <div className="lg:col-span-2 space-y-4">
            <Link to="/" className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-700 via-indigo-600 to-blue-500 text-white flex items-center justify-center shadow-md shadow-blue-500/20">
                <Zap className="w-5 h-5 fill-current text-white" />
              </div>
              <div className="flex flex-col">
                <span className="font-extrabold text-white tracking-tight text-lg leading-tight">
                  Rikkei Store
                </span>
                <span className="text-[10px] uppercase font-bold tracking-widest text-slate-400 -mt-0.5">
                  Official Store
                </span>
              </div>
            </Link>
            
            <p className="text-slate-400 text-xs leading-relaxed max-w-sm">
              Nền tảng thương mại điện tử chuyên cung cấp thiết bị công nghệ, laptop, điện thoại thông minh và phụ kiện chính hãng với mức giá cạnh tranh và chế độ hậu mãi chu đáo nhất.
            </p>

            <div className="space-y-2 text-xs text-slate-400 pt-1">
              <div className="flex items-center gap-2.5">
                <MapPin className="w-4 h-4 text-slate-500 shrink-0" />
                <span>Tòa nhà Sông Đà, Phạm Hùng, Nam Từ Liêm, Hà Nội</span>
              </div>
              <div className="flex items-center gap-2.5">
                <Phone className="w-4 h-4 text-slate-500 shrink-0" />
                <span>Hotline: 1900 6868 (8:00 - 21:30 hàng ngày)</span>
              </div>
              <div className="flex items-center gap-2.5">
                <Mail className="w-4 h-4 text-slate-500 shrink-0" />
                <span>support@rikkeistore.vn</span>
              </div>
            </div>
          </div>

          {/* Categories */}
          <div>
            <h4 className="font-semibold text-slate-100 text-xs uppercase tracking-wider mb-4">
              Danh mục nổi bật
            </h4>
            <ul className="space-y-2.5 text-slate-400">
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Laptop & Máy tính
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Điện thoại thông minh
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Tai nghe & Audio
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Đồng hồ thông minh
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Phụ kiện Gaming
                </Link>
              </li>
            </ul>
          </div>

          {/* Customer Care */}
          <div>
            <h4 className="font-semibold text-slate-100 text-xs uppercase tracking-wider mb-4">
              Chính sách & Hỗ trợ
            </h4>
            <ul className="space-y-2.5 text-slate-400">
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Hướng dẫn mua hàng
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Chính sách bảo hành 2 năm
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Quy định đổi trả 7 ngày
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Phương thức thanh toán
                </Link>
              </li>
              <li>
                <Link to="/products" className="hover:text-blue-400 transition-colors">
                  Bảo mật thông tin khách hàng
                </Link>
              </li>
            </ul>
          </div>

          {/* Newsletter / Admin Access */}
          <div>
            <h4 className="font-semibold text-slate-100 text-xs uppercase tracking-wider mb-4">
              Nhận bản tin công nghệ
            </h4>
            <p className="text-slate-400 text-xs mb-3">
              Đăng ký để nhận voucher 100.000₫ và thông tin sản phẩm mới sớm nhất.
            </p>
            <div className="relative mb-5">
              <input
                type="email"
                placeholder="Nhập email của bạn..."
                className="w-full bg-slate-900/90 text-white placeholder:text-slate-500 text-xs rounded-full pl-4 pr-10 py-2.5 border border-slate-700/80 focus:border-blue-500 focus:outline-none"
              />
              <button
                type="button"
                className="absolute right-1 top-1 w-7 h-7 rounded-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center transition-colors cursor-pointer"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="pt-2 border-t border-slate-800">
              <span className="text-[11px] text-slate-500 block mb-1">Thời gian phục vụ khách hàng:</span>
              <span className="text-xs text-slate-300 font-medium">8:00 - 21:30 (Thứ 2 đến Chủ nhật)</span>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-slate-800/80 mt-12 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
          <div>
            © {new Date().getFullYear()} Rikkei Store. Bản quyền thuộc về Rikkei E-commerce.
          </div>
          <div className="flex items-center gap-4 text-slate-400">
            <span>Rikkei Store Design System</span>
            <span>•</span>
            <span>Chất lượng hàng đầu</span>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
