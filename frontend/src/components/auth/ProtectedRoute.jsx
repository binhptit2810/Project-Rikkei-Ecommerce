import React from 'react'
import { Navigate, useLocation, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard yêu cầu người dùng phải đăng nhập và kiểm tra quyền (nếu có requiredRole).
 * - Nếu chưa đăng nhập: chuyển hướng về /admin/login (nếu requiredRole === 'ADMIN') hoặc /login.
 * - Nếu đã đăng nhập nhưng không có vai trò phù hợp: chuyển hướng về /forbidden (403).
 */
export const ProtectedRoute = ({ requiredRole, children }) => {
  const { isAuthenticated, user } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    const redirectPath = requiredRole === 'ADMIN' ? '/admin/login' : '/login'
    return <Navigate to={redirectPath} state={{ from: location }} replace />
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/forbidden" replace />
  }

  return children ? children : <Outlet />
}

export default ProtectedRoute
