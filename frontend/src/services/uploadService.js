import apiClient from './apiClient'

/**
 * Service xử lý tải tệp ảnh từ máy tính lên backend.
 */
export const uploadService = {
  /**
   * Tải tệp ảnh từ máy tính.
   * @param {File} file - Đối tượng File từ input type="file"
   * @returns {Promise<{ url: string, fileName: string, size: number }>}
   */
  async uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post('/admin/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data // ApiResponse<UploadResponse>
  },
}

export default uploadService

