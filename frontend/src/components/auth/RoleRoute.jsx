import React from 'react'
import { Navigate, useLocation, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard yêu cầu quyền cụ thể (ví dụ: ADMIN).
 * Nếu chưa đăng nhập -> chuyển sang /login.
 * Nếu đã đăng nhập nhưng không đủ quyền -> chuyển sang /forbidden (403).
 */
export const RoleRoute = ({ requiredRole = 'ADMIN', children }) => {
  const { isAuthenticated, user } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (user?.role !== requiredRole) {
    return <Navigate to="/forbidden" replace />
  }

  return children ? children : <Outlet />
}

export default RoleRoute
