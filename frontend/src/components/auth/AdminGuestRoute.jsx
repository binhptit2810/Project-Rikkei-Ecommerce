import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard dành riêng cho trang đăng nhập Admin (/admin/login).
 * 
 * Quy tắc điều hướng:
 * 1. Khách vãng lai (chưa đăng nhập): Cho phép truy cập trang đăng nhập Admin.
 * 2. Đã đăng nhập với quyền ADMIN: Tự động chuyển hướng vào /admin/dashboard.
 * 3. Đã đăng nhập với quyền CUSTOMER: Từ chối vào khu vực quản trị, chuyển sang /forbidden (403).
 */
export const AdminGuestRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore()

  if (isAuthenticated) {
    if (user?.role === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />
    }
    // CUSTOMER đã đăng nhập không được phép vào trang đăng nhập Admin
    return <Navigate to="/forbidden" replace />
  }

  return children ? children : <Outlet />
}

export default AdminGuestRoute
