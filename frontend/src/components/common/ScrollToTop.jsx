import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Component ScrollToTop: Tự động cuộn lên đầu trang (top = 0, left = 0)
 * giải quyết triệt để lỗi khi người dùng click vào sản phẩm, danh mục, phân trang,
 * hoặc chuyển route mà bị hiển thị ở giữa hoặc cuối trang.
 *
 * Hỗ trợ:
 * 1. Thay đổi route (pathname) ví dụ: Click vào sản phẩm /products/:id, /cart, /checkout...
 * 2. Thay đổi tham số truy vấn (search) ví dụ: Click vào danh mục ?categoryId=..., tìm kiếm, phân trang
 * 3. Tắt cơ chế scrollRestoration tự động của trình duyệt tránh bị khôi phục vị trí cuộn cũ
 * 4. Tương thích với animation chuyển trang (Framer Motion) và vùng cuộn riêng của Admin Layout
 */
export const ScrollToTop = () => {
  const { pathname, search } = useLocation()

  useEffect(() => {
    // 1. Tắt cơ chế tự động khôi phục vị trí cuộn cũ của trình duyệt
    if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
      window.history.scrollRestoration = 'manual'
    }

    const performScrollToTop = () => {
      // Cuộn cửa sổ trình duyệt lên đầu trang ngay lập tức
      window.scrollTo({
        top: 0,
        left: 0,
        behavior: 'instant',
      })

      // Đảm bảo cả documentElement và body đều về 0
      if (document.documentElement) {
        document.documentElement.scrollTop = 0
      }
      if (document.body) {
        document.body.scrollTop = 0
      }

      // Xử lý vùng cuộn riêng của Admin Layout nếu đang ở trang quản trị
      const adminMain = document.querySelector('main.overflow-y-auto')
      if (adminMain) {
        adminMain.scrollTop = 0
      }
    }

    // Thực hiện ngay lập tức
    performScrollToTop()

    // Thực hiện ở frame tiếp theo (sau khi DOM mount)
    const rafId = typeof window !== 'undefined' && window.requestAnimationFrame
      ? window.requestAnimationFrame(performScrollToTop)
      : null

    // Thực hiện sau khi kết thúc animation chuyển trang của Framer Motion (thường từ 150ms - 220ms)
    const timer1 = setTimeout(performScrollToTop, 60)
    const timer2 = setTimeout(performScrollToTop, 220)

    return () => {
      if (rafId && typeof window !== 'undefined' && window.cancelAnimationFrame) {
        window.cancelAnimationFrame(rafId)
      }
      clearTimeout(timer1)
      clearTimeout(timer2)
    }
  }, [pathname, search])

  return null
}

export default ScrollToTop

