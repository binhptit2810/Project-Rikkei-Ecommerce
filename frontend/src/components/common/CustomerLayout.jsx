import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { motion, AnimatePresence, useReducedMotion } from 'framer-motion'
import Navbar from './Navbar'
import Footer from './Footer'
import ChatWidget from './ChatWidget'

/**
 * Layout dành riêng cho khu vực khách hàng (Customer Layout).
 * Tích hợp Motion for React hỗ trợ chuyển trang mượt mà (smooth page transitions).
 * Tôn trọng prefers-reduced-motion, không gây giật lag hoặc layout shift.
 */
export const CustomerLayout = () => {
  const location = useLocation()
  const shouldReduceMotion = useReducedMotion()

  return (
    <div className="min-h-screen bg-slate-50/60 text-slate-900 flex flex-col justify-between antialiased selection:bg-blue-600 selection:text-white font-sans">
      <Navbar />
      <main className="flex-1 flex flex-col">
        <AnimatePresence mode="wait">
          <motion.div
            key={location.pathname}
            initial={shouldReduceMotion ? { opacity: 1 } : { opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={shouldReduceMotion ? { opacity: 1 } : { opacity: 0 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="flex-1 flex flex-col"
          >
            <Outlet />
          </motion.div>
        </AnimatePresence>
      </main>
      <Footer />
      <ChatWidget />
    </div>
  )
}

export default CustomerLayout
