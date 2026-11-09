import React from 'react'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'
import ChatWidget from './ChatWidget'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * Tuân thủ tiêu chuẩn Design System shadcn/ui New York.
 */
export const CustomerLayout = () => {
  return (
    <div className="min-h-screen bg-slate-50/60 text-slate-900 flex flex-col justify-between antialiased selection:bg-blue-600 selection:text-white font-sans">
      <Navbar />
      <main className="flex-1 flex flex-col">
        <Outlet />
      </main>
      <Footer />
      <ChatWidget />
    </div>
  )
}

export default CustomerLayout
