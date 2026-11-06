import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import cartService from '../services/cartService'
import { useAuthStore } from './useAuthStore'

/**
 * Quản lý trạng thái giỏ hàng (Cart Store).
 * - Yêu cầu xác thực tài khoản: Người dùng chưa đăng nhập KHÔNG được phép thêm vào giỏ hàng
 *   hoặc hiển thị số lượng sản phẩm giỏ hàng.
 * - Lưu trữ offline an toàn & đồng bộ vào localStorage khi đã đăng nhập.
 * - Tự động phát request API lên server thông qua cartService.
 */
export const useCartStore = create(
  persist(
    (set, get) => ({
      items: [], // Danh sách sản phẩm: [{ id, name, price, imageUrl, stockQuantity, quantity }]

      /**
       * Thêm sản phẩm vào giỏ hàng (Chỉ cho phép khi người dùng đã đăng nhập)
       */
      addToCart: async (product, quantity = 1) => {
        const isAuthenticated = useAuthStore.getState().isAuthenticated
        if (!isAuthenticated) {
          return {
            success: false,
            requireLogin: true,
            message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
          }
        }

        if (!product || (product.stockQuantity !== undefined && product.stockQuantity !== null && product.stockQuantity <= 0)) {
          return { success: false, message: 'Sản phẩm đã hết hàng' }
        }

        const currentItems = get().items
        const existingIndex = currentItems.findIndex((item) => item.id === product.id)
        let newItems = [...currentItems]
        const maxStock = (product.stockQuantity !== undefined && product.stockQuantity !== null) ? Number(product.stockQuantity) : 9999
        const desiredQty = Math.max(1, Number(quantity) || 1)

        if (existingIndex > -1) {
          const current = newItems[existingIndex]
          const updatedQuantity = Math.min(
            maxStock,
            (Number(current.quantity) || 1) + desiredQty
          )
          newItems[existingIndex] = {
            ...current,
            quantity: updatedQuantity,
          }
        } else {
          newItems.push({
            id: product.id,
            name: product.name,
            price: Number(product.price) || 0,
            imageUrl: product.imageUrl || '',
            stockQuantity: product.stockQuantity,
            quantity: Math.min(maxStock, desiredQty),
          })
        }

        set({ items: newItems })

        // Gửi request API lên backend
        cartService.addToCart(product.id, desiredQty)

        return { success: true }
      },

      /**
       * Cập nhật số lượng sản phẩm trong giỏ
       */
      updateQuantity: (productId, quantity) => {
        const isAuthenticated = useAuthStore.getState().isAuthenticated
        if (!isAuthenticated) return

        const numQty = Number(quantity)
        if (numQty <= 0) {
          get().removeFromCart(productId)
          return
        }

        set({
          items: get().items.map((item) => {
            if (item.id === productId) {
              const maxStock = (item.stockQuantity !== undefined && item.stockQuantity !== null) ? Number(item.stockQuantity) : 9999
              const clamped = Math.min(maxStock, Math.max(1, numQty))
              return { ...item, quantity: clamped }
            }
            return item
          }),
        })

        cartService.updateCartItem(productId, numQty)
      },

      /**
       * Xóa một sản phẩm khỏi giỏ hàng
       */
      removeFromCart: (productId) => {
        set({
          items: get().items.filter((item) => item.id !== productId),
        })

        cartService.removeFromCart(productId)
      },

      /**
       * Xóa sạch giỏ hàng (trong state, localStorage và gọi backend)
       */
      clearCart: () => {
        set({ items: [] })
        try {
          localStorage.removeItem('shopping_cart_storage')
        } catch (_) {}
        cartService.clearCart()
      },

      /**
       * Lấy tổng số lượng sản phẩm trong giỏ (Trả về 0 nếu chưa đăng nhập)
       */
      getTotalCount: () => {
        const isAuthenticated = useAuthStore.getState().isAuthenticated
        if (!isAuthenticated) return 0
        return get().items.reduce((total, item) => total + (item.quantity || 0), 0)
      },

      /**
       * Tính tổng tiền giỏ hàng (VND) (Trả về 0 nếu chưa đăng nhập)
       */
      getTotalPrice: () => {
        const isAuthenticated = useAuthStore.getState().isAuthenticated
        if (!isAuthenticated) return 0
        return get().items.reduce(
          (total, item) => total + (Number(item.price) || 0) * (item.quantity || 0),
          0
        )
      },
    }),
    {
      name: 'shopping_cart_storage', // Key lưu trong localStorage
      storage: createJSONStorage(() => localStorage),
    }
  )
)

export default useCartStore
