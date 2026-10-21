import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard dành cho khách vãng lai (chưa đăng nhập).
 * Nếu đã đăng nhập, tự động chuyển hướng về /profile.
 */
export const GuestRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore()

  if (isAuthenticated) {
    if (user?.role === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />
    }
    return <Navigate to="/" replace />
  }

  return children ? children : <Outlet />
}

export default GuestRoute
