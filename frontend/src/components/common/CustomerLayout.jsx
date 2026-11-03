import React from 'react'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * Tuân thủ tiêu chuẩn Design System shadcn/ui New York.
 */
export const CustomerLayout = () => {
  return (
    <div className="min-h-screen bg-white text-zinc-950 flex flex-col justify-between antialiased selection:bg-zinc-900 selection:text-zinc-50">
      <Navbar />
      <main className="flex-1 flex flex-col">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

export default CustomerLayout
