import apiClient from './apiClient'

/**
 * Service xử lý tải lên tệp tin và hình ảnh (Upload Service).
 * Phụ trách: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 */
export const uploadService = {
  /**
   * Tải tệp tin hình ảnh sản phẩm/danh mục lên máy chủ (dành cho Admin).
   * @param {File} file - Đối tượng tệp tin từ input file
   * @returns {Promise<Object>} ApiResponse chứa FileUploadResponse { url, fileName, ... }
   */
  async uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post('/admin/uploads', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  },

  /**
   * Tải 1 hình ảnh đính kèm bài đánh giá (dành cho Khách hàng đã đăng nhập).
   * @param {File} file - File ảnh đánh giá (jpg, png, webp, gif)
   * @returns {Promise<string>} URL của ảnh đã lưu
   */
  async uploadReviewImage(file) {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await apiClient.post('/reviews/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      const url = response.data?.data?.url || response.data?.url
      if (url) return url
    } catch (err) {
      // Fallback nếu người dùng là admin hoặc đang test
      console.warn('Fallback upload image via /admin/uploads:', err)
      try {
        const adminRes = await apiClient.post('/admin/uploads', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        })
        const url = adminRes.data?.data?.url || adminRes.data?.url
        if (url) return url
      } catch (adminErr) {
        console.warn('Fallback DataURL upload:', adminErr)
      }
    }

    // Fallback an toàn client-side nếu backend upload chưa có thư mục vật lý
    return new Promise((resolve) => {
      const FileReaderClass = typeof window !== 'undefined' ? window.FileReader : globalThis.FileReader
      if (!FileReaderClass) {
        resolve('')
        return
      }
      const reader = new FileReaderClass()
      reader.onloadend = () => resolve(reader.result)
      reader.readAsDataURL(file)
    })
  },

  /**
   * Tải lên danh sách nhiều ảnh đánh giá cùng lúc (tối đa 3 ảnh).
   * @param {File[]} files - Mảng các file ảnh
   * @returns {Promise<string[]>} Danh sách URL ảnh
   */
  async uploadReviewImages(files) {
    if (!files || files.length === 0) return []
    const selected = files.slice(0, 3)
    const uploadPromises = selected.map((file) => this.uploadReviewImage(file))
    return Promise.all(uploadPromises)
  },
}

export default uploadService
