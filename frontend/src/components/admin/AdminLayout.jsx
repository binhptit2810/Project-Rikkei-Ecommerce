import React, { useState } from 'react'
import { Outlet } from 'react-router-dom'
import AdminSidebar from './AdminSidebar'
import AdminHeader from './AdminHeader'

/**
 * Layout gốc dành riêng cho khu vực quản trị Admin (Admin Layout).
 * Thiết kế theo chuẩn shadcn/ui New York Style (Neutral/Zinc Palette).
 */
export const AdminLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-zinc-50/50 text-zinc-950 antialiased flex flex-col">
      {/* 1. Sidebar Desktop (Cố định bên trái) */}
      <div className="hidden lg:block fixed inset-y-0 left-0 z-30 w-64">
        <AdminSidebar />
      </div>

      {/* 2. Sidebar Mobile/Tablet (Drawer trượt với lớp phủ làm mờ) */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          {/* Lớp phủ mờ (Backdrop) */}
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-xs transition-opacity duration-200"
            onClick={() => setSidebarOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer Sidebar */}
          <div className="fixed inset-y-0 left-0 z-50 w-64 max-w-full shadow-lg animate-in slide-in-from-left duration-200">
            <AdminSidebar onClose={() => setSidebarOpen(false)} />
          </div>
        </div>
      )}

      {/* 3. Khu vực bên phải (Header + Main Content) */}
      <div className="flex-1 flex flex-col lg:pl-64 min-w-0">
        {/* Header Quản trị */}
        <AdminHeader onToggleSidebar={() => setSidebarOpen(true)} />

        {/* Nội dung trang quản trị (Render qua Outlet) */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
