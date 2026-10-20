import React from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard dành cho khách vãng lai (chưa đăng nhập).
 * Nếu đã đăng nhập, tự động chuyển hướng về /profile.
 */
export const GuestRoute = ({ children }) => {
  const { isAuthenticated } = useAuthStore()

  if (isAuthenticated) {
    return <Navigate to="/profile" replace />
  }

  return children ? children : <Outlet />
}

export default GuestRoute
