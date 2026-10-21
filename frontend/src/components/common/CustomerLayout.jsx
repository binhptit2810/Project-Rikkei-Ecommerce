import React from 'react'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * 
 * Bố cục:
 * 1. Navbar: Thanh điều hướng chung cho khách hàng.
 * 2. Main Content: Khu vực hiển thị các trang công khai, đăng nhập/đăng ký, và hồ sơ cá nhân.
 * 3. Footer: Chân trang thương mại điện tử.
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
