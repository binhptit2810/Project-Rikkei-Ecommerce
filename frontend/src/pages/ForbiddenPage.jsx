import React from 'react'
import { Link } from 'react-router-dom'

export const ForbiddenPage = () => {
  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 text-center">
      <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10">
        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto mb-4 text-3xl">
          🚫
        </div>
        <h1 className="text-3xl font-extrabold text-slate-800 tracking-tight mb-2">
          403 — Không có quyền truy cập
        </h1>
        <p className="text-sm text-slate-600 mb-6 leading-relaxed">
          Tài khoản của bạn không có đủ thẩm quyền để truy cập trang này. Vui lòng liên hệ quản trị viên hệ thống nếu bạn nghĩ đây là sự nhầm lẫn.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/"
            className="bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow transition-all text-sm"
          >
            Về trang chủ
          </Link>
          <Link
            to="/profile"
            className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold py-2.5 px-6 rounded-xl transition-all text-sm"
          >
            Hồ sơ cá nhân
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ForbiddenPage
