import React, { useState } from 'react'
import { Outlet } from 'react-router-dom'
import AdminSidebar from './AdminSidebar'
import AdminHeader from './AdminHeader'

/**
 * Layout gốc dành riêng cho khu vực quản trị Admin (Admin Layout).
 * 
 * Đặc điểm:
 * 1. Tách biệt hoàn toàn với layout của Customer (không có Navbar & Footer chung).
 * 2. Cố định Sidebar bên trái (chiều rộng 256px trên Desktop).
 * 3. Hỗ trợ Drawer Sidebar đóng/mở mượt mà trên Mobile & Tablet.
 * 4. Header nằm trên cùng của khu vực nội dung bên phải.
 * 5. Khu vực nội dung chính hiển thị các trang con thông qua React Router Outlet.
 */
export const AdminLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-slate-100/70 text-slate-800 antialiased flex flex-col">
      {/* 1. Sidebar Desktop (Cố định bên trái) */}
      <div className="hidden lg:block fixed inset-y-0 left-0 z-30 w-64">
        <AdminSidebar />
      </div>

      {/* 2. Sidebar Mobile/Tablet (Drawer trượt với lớp phủ làm mờ) */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          {/* Lớp phủ mờ (Backdrop) */}
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity duration-300"
            onClick={() => setSidebarOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer Sidebar */}
          <div className="fixed inset-y-0 left-0 z-50 w-64 max-w-full shadow-2xl animate-in slide-in-from-left duration-300">
            <AdminSidebar onClose={() => setSidebarOpen(false)} />
          </div>
        </div>
      )}

      {/* 3. Khu vực bên phải (Header + Main Content) */}
      <div className="flex-1 flex flex-col lg:pl-64 min-w-0">
        {/* Header Quản trị */}
        <AdminHeader onToggleSidebar={() => setSidebarOpen(true)} />

        {/* Nội dung trang quản trị (Render qua Outlet) */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
