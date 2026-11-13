import React, { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/useAuthStore'
import CustomerLayout from './components/common/CustomerLayout'
import AdminLayout from './components/admin/AdminLayout'
import ScrollToTop from './components/common/ScrollToTop'
import GlobalToast from './components/common/GlobalToast'
import ProtectedRoute from './components/auth/ProtectedRoute'
import GuestRoute from './components/auth/GuestRoute'
import AdminGuestRoute from './components/auth/AdminGuestRoute'
import RoleRoute from './components/auth/RoleRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import VerifyEmailPage from './pages/auth/VerifyEmailPage'
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage'
import ResetPasswordPage from './pages/auth/ResetPasswordPage'
import AdminLoginPage from './pages/admin/AdminLoginPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import ProfilePage from './pages/user/ProfilePage'
import ProductListPage from './pages/shop/ProductListPage'
import ProductDetailPage from './pages/shop/ProductDetailPage'
import CartPage from './pages/shop/CartPage'
import CheckoutPage from './pages/checkout/CheckoutPage'
import OrderHistoryPage from './pages/orders/OrderHistoryPage'
import OrderDetailPage from './pages/orders/OrderDetailPage'
import PaymentResultPage from './pages/orders/PaymentResultPage'
import VNPayReturnPage from './pages/orders/VNPayReturnPage'
import UserManagementPage from './pages/admin/UserManagementPage'
import AdminProductsPage from './pages/admin/AdminProductsPage'
import AdminCategoriesPage from './pages/admin/AdminCategoriesPage'
import AdminOrdersPage from './pages/admin/AdminOrdersPage'
import AdminInventoryPage from './pages/admin/AdminInventoryPage'
import AdminVouchersPage from './pages/admin/AdminVouchersPage'
import AdminReviewsPage from './pages/admin/AdminReviewsPage'
import ForbiddenPage from './pages/ForbiddenPage'
import NotFoundPage from './pages/NotFoundPage'
import WishlistPage from './pages/WishlistPage'
import { useWishlistStore } from './store/useWishlistStore'

function App() {
  const { initAuth, isInitializing, isAuthenticated, user } = useAuthStore()
  const fetchWishlistIds = useWishlistStore((state) => state.fetchWishlistIds)
  const resetWishlist = useWishlistStore((state) => state.resetWishlist)
  const initRef = useRef(false)

  // Khôi phục phiên làm việc khi ứng dụng khởi chạy / reload trang (App Bootstrapping)
  useEffect(() => {
    if (!initRef.current) {
      initRef.current = true
      initAuth()
    }
  }, [initAuth])

  // Tải danh sách Wishlist IDs khi người dùng đăng nhập
  useEffect(() => {
    if (isAuthenticated && user?.role !== 'ADMIN') {
      fetchWishlistIds()
    } else {
      resetWishlist()
    }
  }, [isAuthenticated, user, fetchWishlistIds, resetWishlist])

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
      {/* Tự động cuộn lên đầu trang khi click sản phẩm, danh mục hoặc chuyển route */}
      <ScrollToTop />
      {/* Ô thông báo toàn cục bên phải màn hình (tự động biến mất sau 2s) */}
      <GlobalToast />
      <Routes>
        {/* 1. Trang Đăng nhập Quản trị viên (Độc lập, không chứa Navbar/Footer mua sắm) */}
        <Route
          path="/admin/login"
          element={
            <AdminGuestRoute>
              <AdminLoginPage />
            </AdminGuestRoute>
          }
        />

        {/* 2. Khu vực Quản trị Admin (Bảo vệ bởi RoleRoute requiredRole="ADMIN" & dùng AdminLayout riêng) */}
        <Route
          path="/admin"
          element={
            <RoleRoute requiredRole="ADMIN">
              <AdminLayout />
            </RoleRoute>
          }
        >
          {/* Trang tổng quan Dashboard, Quản lý người dùng, Sản phẩm, Danh mục và Đơn hàng */}
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboardPage />} />
          <Route path="users" element={<UserManagementPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="inventory" element={<AdminInventoryPage />} />
          <Route path="vouchers" element={<AdminVouchersPage />} />
          <Route path="coupons" element={<AdminVouchersPage />} />
          <Route path="reviews" element={<AdminReviewsPage />} />
        </Route>

        {/* 3. Customer Area Routes (Sử dụng CustomerLayout riêng với Navbar và Footer) */}
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
          <Route
            path="/verify-email"
            element={
              <GuestRoute>
                <VerifyEmailPage />
              </GuestRoute>
            }
          />
          <Route
            path="/forgot-password"
            element={
              <GuestRoute>
                <ForgotPasswordPage />
              </GuestRoute>
            }
          />
          <Route
            path="/reset-password"
            element={
              <GuestRoute>
                <ResetPasswordPage />
              </GuestRoute>
            }
          />

          {/* Protected Customer Routes (chỉ dành cho khách hàng đăng nhập, chặn hoàn toàn Admin) */}
          <Route
            path="/profile"
            element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <ProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/wishlist"
            element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <WishlistPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/checkout"
            element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <CheckoutPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <OrderHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders/:id"
            element={
              <ProtectedRoute requiredRole="CUSTOMER">
                <OrderDetailPage />
              </ProtectedRoute>
            }
          />
          <Route path="/orders/vnpay-return" element={<VNPayReturnPage />} />
          <Route path="/payment/result" element={<PaymentResultPage />} />
          <Route path="/payment-result" element={<PaymentResultPage />} />

          {/* Error & Fallback Routes */}
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
