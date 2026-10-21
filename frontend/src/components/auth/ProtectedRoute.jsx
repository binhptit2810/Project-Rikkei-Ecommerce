import React from 'react'
import { Navigate, useLocation, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../store/useAuthStore'

/**
 * Route guard yêu cầu người dùng phải đăng nhập.
 * Nếu chưa đăng nhập -> chuyển hướng về /login và lưu lại trang đích trong state.
 */
export const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children ? children : <Outlet />
}

export default ProtectedRoute
