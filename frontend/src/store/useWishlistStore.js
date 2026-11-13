import { create } from 'zustand'
import wishlistService from '../services/wishlistService'
import { useAuthStore } from './useAuthStore'
import { toast } from './useToastStore'

/**
 * Global Wishlist Store (Zustand).
 * Quản lý danh sách sản phẩm yêu thích của người dùng:
 * - Lưu trữ danh sách Product IDs đã thả tim để tô màu đỏ tức thì trên mọi thẻ sản phẩm.
 * - Hỗ trợ Optimistic UI (cập nhật giao diện lập tức trước khi server phản hồi).
 * - Tự động rollback nếu xảy ra lỗi mạng / lỗi server.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
export const useWishlistStore = create((set, get) => ({
  wishlistIds: [],
  items: [],
  loading: false,
  error: null,

  /**
   * Tải danh sách Product IDs yêu thích (gọi 1 lần khi user đã đăng nhập).
   */
  fetchWishlistIds: async () => {
    const { isAuthenticated, user } = useAuthStore.getState()
    if (!isAuthenticated || user?.role === 'ADMIN') {
      set({ wishlistIds: [] })
      return
    }

    try {
      const ids = await wishlistService.getWishlistProductIds()
      set({ wishlistIds: Array.isArray(ids) ? ids.map(Number) : [] })
    } catch (err) {
      console.error('[WishlistStore] Lỗi tải wishlist IDs:', err)
    }
  },

  /**
   * Tải danh sách đầy đủ sản phẩm yêu thích (dùng cho trang WishlistPage).
   */
  fetchWishlist: async () => {
    const { isAuthenticated, user } = useAuthStore.getState()
    if (!isAuthenticated || user?.role === 'ADMIN') {
      set({ items: [], wishlistIds: [], loading: false })
      return
    }

    set({ loading: true, error: null })
    try {
      const data = await wishlistService.getWishlist()
      const list = Array.isArray(data) ? data : []
      const ids = list.map((item) => Number(item.id))
      set({ items: list, wishlistIds: ids, loading: false })
    } catch (err) {
      console.error('[WishlistStore] Lỗi tải wishlist:', err)
      set({
        error: err.response?.data?.message || 'Không thể tải danh sách yêu thích',
        loading: false,
      })
    }
  },

  /**
   * Bật/Tắt yêu thích một sản phẩm với Optimistic UI.
   * @param {number|string} productId
   * @param {Object} [productObj] - Thông tin sản phẩm (nếu có để chèn vào items ngay)
   * @returns {Promise<{ liked: boolean, message: string }>}
   */
  toggleWishlist: async (productId, productObj = null) => {
    const numId = Number(productId)
    const prevIds = [...get().wishlistIds]
    const prevItems = [...get().items]
    const isCurrentlyLiked = prevIds.includes(numId)

    // 1. Optimistic Update (Cập nhật UI ngay lập tức)
    if (isCurrentlyLiked) {
      // Xóa khỏi wishlist
      const newIds = prevIds.filter((id) => id !== numId)
      const newItems = prevItems.filter((item) => Number(item.id) !== numId)
      set({ wishlistIds: newIds, items: newItems })
    } else {
      // Thêm vào wishlist
      const newIds = [numId, ...prevIds]
      let newItems = [...prevItems]
      if (productObj) {
        newItems = [
          {
            id: numId,
            name: productObj.name,
            price: productObj.price,
            discountPrice: productObj.discountPrice || productObj.price,
            imageUrl: productObj.imageUrl,
            ratingAverage: productObj.ratingAverage || 0,
            stockQuantity: productObj.stockQuantity || 0,
            addedAt: new Date().toISOString(),
          },
          ...prevItems,
        ]
      }
      set({ wishlistIds: newIds, items: newItems })
    }

    // 2. Gửi request lên server
    try {
      const result = await wishlistService.toggleWishlist(numId)
      const liked = result?.liked ?? !isCurrentlyLiked
      const message = result?.message || (liked ? 'Đã thêm vào danh sách yêu thích' : 'Đã xóa khỏi danh sách yêu thích')

      // Đồng bộ lại trạng thái từ server nếu có chênh lệch
      if (liked && !get().wishlistIds.includes(numId)) {
        set({ wishlistIds: [numId, ...get().wishlistIds] })
      } else if (!liked && get().wishlistIds.includes(numId)) {
        set({
          wishlistIds: get().wishlistIds.filter((id) => id !== numId),
          items: get().items.filter((item) => Number(item.id) !== numId),
        })
      }

      toast.success(liked ? 'Yêu thích' : 'Bỏ yêu thích', message)
      return { liked, message }
    } catch (err) {
      // Rollback nếu thất bại
      set({ wishlistIds: prevIds, items: prevItems })
      const errorMsg = err.response?.data?.message || 'Có lỗi xảy ra khi cập nhật danh sách yêu thích'
      toast.error('Lỗi', errorMsg)
      throw err
    }
  },

  /**
   * Kiểm tra sản phẩm đã nằm trong Wishlist hay chưa
   */
  isInWishlist: (productId) => {
    return get().wishlistIds.includes(Number(productId))
  },

  /**
   * Lấy tổng số lượng sản phẩm trong Wishlist
   */
  getCount: () => {
    return get().wishlistIds.length
  },

  /**
   * Reset store (khi đăng xuất)
   */
  resetWishlist: () => {
    set({ wishlistIds: [], items: [], loading: false, error: null })
  },
}))

export default useWishlistStore

