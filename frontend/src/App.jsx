import React, { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/useAuthStore'
import CustomerLayout from './components/common/CustomerLayout'
import AdminLayout from './components/admin/AdminLayout'
import ProtectedRoute from './components/auth/ProtectedRoute'
import GuestRoute from './components/auth/GuestRoute'
import RoleRoute from './components/auth/RoleRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ProfilePage from './pages/user/ProfilePage'
import ProductListPage from './pages/shop/ProductListPage'
import ProductDetailPage from './pages/shop/ProductDetailPage'
import CartPage from './pages/shop/CartPage'
import CheckoutPage from './pages/checkout/CheckoutPage'
import OrderHistoryPage from './pages/orders/OrderHistoryPage'
import OrderDetailPage from './pages/orders/OrderDetailPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import UserManagementPage from './pages/admin/UserManagementPage'
import AdminProductsPage from './pages/admin/AdminProductsPage'
import AdminCategoriesPage from './pages/admin/AdminCategoriesPage'
import AdminOrdersPage from './pages/admin/AdminOrdersPage'
import ForbiddenPage from './pages/ForbiddenPage'
import NotFoundPage from './pages/NotFoundPage'

function App() {
  const { initAuth, isInitializing } = useAuthStore()
  const initRef = useRef(false)

  // Khôi phục phiên làm việc khi ứng dụng khởi chạy / reload trang (App Bootstrapping)
  useEffect(() => {
    if (!initRef.current) {
      initRef.current = true
      initAuth()
    }
  }, [initAuth])

  // Hiển thị màn hình chờ toàn trang khi đang kiểm tra và làm mới token trong memory
  if (isInitializing) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3"></div>
        <p className="text-gray-500 font-medium text-sm">Đang khôi phục phiên đăng nhập...</p>
      </div>
    )
  }

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
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
        </Route>

        {/* 2. Customer Area Routes (Sử dụng CustomerLayout riêng với Navbar và Footer) */}
        <Route element={<CustomerLayout />}>
          {/* Public Home & Shop Pages */}
          <Route path="/" element={<HomePage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/cart" element={<CartPage />} />

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
          <Route
            path="/checkout"
            element={
              <ProtectedRoute>
                <CheckoutPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrderHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders/:id"
            element={
              <ProtectedRoute>
                <OrderDetailPage />
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
