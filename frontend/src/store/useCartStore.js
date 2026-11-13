import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import cartService from '../services/cartService'
import { useAuthStore } from './useAuthStore'
import { useToastStore } from './useToastStore'

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
       * Hỗ trợ lưu trữ biến thể kích cỡ selectedSize riêng biệt [TSK-D14-VDHH]
       */
      addToCart: async (product, quantity = 1, selectedSize = null) => {
        const { isAuthenticated, user } = useAuthStore.getState()
        if (!isAuthenticated) {
          return {
            success: false,
            requireLogin: true,
            message: 'Vui lòng đăng nhập hoặc đăng ký tài khoản để thêm sản phẩm vào giỏ hàng.',
          }
        }

        if (user?.role === 'ADMIN') {
          return {
            success: false,
            isAdmin: true,
            message: 'Tài khoản Quản trị viên (ADMIN) không được phép mua hàng.',
          }
        }

        // Xác định số lượng tồn kho theo biến thể kích cỡ nếu có
        let maxStock = 9999
        if (selectedSize && Array.isArray(product?.variants) && product.variants.length > 0) {
          const matchedVariant = product.variants.find((v) => v.size === selectedSize)
          if (matchedVariant) {
            maxStock = matchedVariant.stockQuantity !== undefined ? Number(matchedVariant.stockQuantity) : 0
          }
        } else if (product?.stockQuantity !== undefined && product?.stockQuantity !== null) {
          maxStock = Number(product.stockQuantity)
        }

        if (!product || maxStock <= 0) {
          return {
            success: false,
            message: selectedSize ? 'Kích cỡ này hiện đã hết hàng.' : 'Sản phẩm đã hết hàng',
          }
        }

        const normalizedSize = selectedSize ? String(selectedSize).trim() : null
        const currentItems = get().items

        // Định danh duy nhất theo Product ID và Kích cỡ được chọn
        const existingIndex = currentItems.findIndex(
          (item) => item.id === product.id && (item.selectedSize || null) === normalizedSize
        )
        let newItems = [...currentItems]
        const desiredQty = Math.max(1, Number(quantity) || 1)

        if (existingIndex > -1) {
          const current = newItems[existingIndex]
          const updatedQuantity = Math.min(
            maxStock,
            (Number(current.quantity) || 1) + desiredQty
          )
          newItems[existingIndex] = {
            ...current,
            stockQuantity: maxStock,
            quantity: updatedQuantity,
          }
        } else {
          newItems.push({
            id: product.id,
            selectedSize: normalizedSize,
            name: product.name,
            price: Number(product.price) || 0,
            imageUrl: product.imageUrl || '',
            stockQuantity: maxStock,
            quantity: Math.min(maxStock, desiredQty),
          })
        }

        set({ items: newItems })

        // Gửi request API lên backend
        cartService.addToCart(product.id, desiredQty)

        // Hiển thị ô thông báo ở góc phải tự động ẩn sau 2s
        useToastStore.getState().showToast({
          type: 'cart',
          title: 'Đã thêm vào giỏ hàng!',
          message: `Đã thêm "${product.name}"${normalizedSize ? ` (Size ${normalizedSize})` : ''} vào giỏ hàng.`,
          link: { to: '/cart', label: 'Xem giỏ hàng →' },
          duration: 2000,
        })

        return { success: true }
      },

      /**
       * Cập nhật số lượng sản phẩm trong giỏ (hỗ trợ phân biệt theo selectedSize)
       */
      updateQuantity: (productId, quantity, selectedSize = null) => {
        const isAuthenticated = useAuthStore.getState().isAuthenticated
        if (!isAuthenticated) return

        const numQty = Number(quantity)
        const normalizedSize = selectedSize !== null && selectedSize !== undefined ? String(selectedSize).trim() : null

        if (numQty <= 0) {
          get().removeFromCart(productId, selectedSize)
          return
        }

        set({
          items: get().items.map((item) => {
            const isMatch =
              item.id === productId &&
              (normalizedSize === null || (item.selectedSize || null) === normalizedSize)
            if (isMatch) {
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
       * Xóa một sản phẩm khỏi giỏ hàng (hỗ trợ phân biệt theo selectedSize)
       */
      removeFromCart: (productId, selectedSize = null) => {
        const normalizedSize = selectedSize !== null && selectedSize !== undefined ? String(selectedSize).trim() : null
        set({
          items: get().items.filter((item) => {
            if (item.id !== productId) return true
            if (normalizedSize !== null) {
              return (item.selectedSize || null) !== normalizedSize
            }
            return false
          }),
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
       * Lấy tổng số lượng sản phẩm trong giỏ (Trả về 0 nếu chưa đăng nhập hoặc là Admin)
       */
      getTotalCount: () => {
        const { isAuthenticated, user } = useAuthStore.getState()
        if (!isAuthenticated || user?.role === 'ADMIN') return 0
        return get().items.reduce((total, item) => total + (item.quantity || 0), 0)
      },

      /**
       * Tính tổng tiền giỏ hàng (VND) (Trả về 0 nếu chưa đăng nhập hoặc là Admin)
       */
      getTotalPrice: () => {
        const { isAuthenticated, user } = useAuthStore.getState()
        if (!isAuthenticated || user?.role === 'ADMIN') return 0
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
