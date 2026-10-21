import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import CustomerLayout from './components/common/CustomerLayout'
import AdminLayout from './components/admin/AdminLayout'
import ProtectedRoute from './components/auth/ProtectedRoute'
import GuestRoute from './components/auth/GuestRoute'
import RoleRoute from './components/auth/RoleRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ProfilePage from './pages/user/ProfilePage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import UserManagementPage from './pages/admin/UserManagementPage'
import ForbiddenPage from './pages/ForbiddenPage'
import NotFoundPage from './pages/NotFoundPage'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 1. Admin Area Routes (Bảo vệ bởi RoleRoute requiredRole="ADMIN" & dùng AdminLayout riêng) */}
        <Route
          path="/admin"
          element={
            <RoleRoute requiredRole="ADMIN">
              <AdminLayout />
            </RoleRoute>
          }
        >
          {/* Mặc định chuyển hướng /admin sang /admin/dashboard */}
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="users" element={<UserManagementPage />} />
        </Route>

        {/* 2. Customer Area Routes (Sử dụng CustomerLayout riêng với Navbar và Footer) */}
        <Route element={<CustomerLayout />}>
          {/* Public Home Page */}
          <Route path="/" element={<HomePage />} />

          {/* Guest Only Routes (chuyển hướng sang /profile nếu đã đăng nhập) */}
          <Route
            path="/login"
            element={
              <GuestRoute>
                <LoginPage />
              </GuestRoute>
            }
          />
          <Route
            path="/register"
            element={
              <GuestRoute>
                <RegisterPage />
              </GuestRoute>
            }
          />

          {/* Protected Routes (yêu cầu đăng nhập) */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />

          {/* Error & Fallback Routes */}
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
