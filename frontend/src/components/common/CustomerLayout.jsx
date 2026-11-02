import React from 'react'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * Cho phép cả khách vãng lai, khách hàng và quản trị viên xem giao diện mua sắm.
 */
export const CustomerLayout = () => {
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
