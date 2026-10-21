import React from 'react'
import { Link } from 'react-router-dom'

export const NotFoundPage = () => {
  return (
    <div className="flex-1 flex items-center justify-center px-4 py-16 text-center">
      <div className="max-w-md w-full bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-10">
        <div className="text-6xl mb-4">🔍</div>
        <h1 className="text-4xl font-extrabold text-slate-800 tracking-tight mb-2">
          404
        </h1>
        <h2 className="text-lg font-bold text-slate-700 mb-2">
          Không tìm thấy trang yêu cầu
        </h2>
        <p className="text-sm text-slate-500 mb-6 leading-relaxed">
          Đường dẫn bạn vừa truy cập không tồn tại hoặc đã được di chuyển.
        </p>
        <Link
          to="/"
          className="inline-block bg-orange-600 hover:bg-orange-700 text-white font-bold py-2.5 px-6 rounded-xl shadow transition-all text-sm"
        >
          Quay lại trang chủ
        </Link>
      </div>
    </div>
  )
}

export default NotFoundPage
