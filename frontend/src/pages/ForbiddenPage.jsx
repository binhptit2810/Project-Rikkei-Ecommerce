import React from 'react'
import { Link } from 'react-router-dom'
import { ShieldAlert, ArrowLeft, User } from 'lucide-react'

export const ForbiddenPage = () => {
  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 text-center bg-slate-50/50">
      <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200/80 shadow-card p-8 sm:p-10">
        <div className="w-14 h-14 bg-red-50 text-red-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-2xs">
          <ShieldAlert className="w-7 h-7 stroke-[1.75]" />
        </div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight mb-2">
          403 — Không có quyền truy cập
        </h1>
        <p className="text-xs text-slate-500 mb-6 leading-relaxed">
          Tài khoản hiện tại không có đủ quyền để truy cập khu vực này. Nếu bạn cho rằng đây là sự cố kỹ thuật, vui lòng liên hệ quản trị viên hệ thống.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/"
            className="inline-flex items-center justify-center gap-2 bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-5 rounded-xl shadow-2xs transition-all text-xs"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Về trang chủ</span>
          </Link>
          <Link
            to="/profile"
            className="inline-flex items-center justify-center gap-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold py-2.5 px-5 rounded-xl transition-all text-xs"
          >
            <User className="w-4 h-4" />
            <span>Hồ sơ cá nhân</span>
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ForbiddenPage
