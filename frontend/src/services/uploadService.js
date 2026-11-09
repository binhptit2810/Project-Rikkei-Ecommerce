import apiClient from './apiClient'

/**
 * Service xử lý tải lên tệp tin và hình ảnh (Upload Service).
 * Dành cho Quản trị viên (Admin) để tải ảnh sản phẩm và danh mục từ máy tính.
 */
export const uploadService = {
  /**
   * Tải tệp tin hình ảnh lên máy chủ.
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
}

export default uploadService
