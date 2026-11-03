import React from 'react'
import { Link } from 'react-router-dom'
import { SearchX, ArrowLeft } from 'lucide-react'

export const NotFoundPage = () => {
  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 text-center bg-slate-50/50">
      <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200/80 shadow-card p-8 sm:p-10">
        <div className="w-14 h-14 bg-zinc-100 text-zinc-800 rounded-lg flex items-center justify-center mx-auto mb-4 shadow-sm">
          <SearchX className="w-7 h-7 stroke-[1.75]" />
        </div>
        <h1 className="text-4xl font-black text-slate-900 tracking-tight mb-2">
          404
        </h1>
        <h2 className="text-base font-bold text-slate-800 mb-2">
          Không tìm thấy trang yêu cầu
        </h2>
        <p className="text-xs text-slate-500 mb-6 leading-relaxed">
          Đường dẫn bạn vừa truy cập không tồn tại hoặc đã được thay đổi. Vui lòng kiểm tra lại địa chỉ URL.
        </p>
        <Link
          to="/"
          className="inline-flex items-center justify-center gap-2 btn-primary text-xs"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại trang chủ</span>
        </Link>
      </div>
    </div>
  )
}

export default NotFoundPage
