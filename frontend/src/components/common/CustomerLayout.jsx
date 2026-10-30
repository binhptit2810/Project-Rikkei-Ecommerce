import React from 'react'
import { Outlet, Navigate } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'
import Navbar from './Navbar'
import Footer from './Footer'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * Nếu tài khoản đăng nhập là ADMIN, chặn truy cập khu vực mua sắm và chuyển hướng về /admin/dashboard
 */
export const CustomerLayout = () => {
  const { user } = useAuthStore()

  if (user?.role === 'ADMIN') {
    return <Navigate to="/admin/dashboard" replace />
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col justify-between antialiased">
      <Navbar />
      <main className="flex-1 flex flex-col">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

export default CustomerLayout
