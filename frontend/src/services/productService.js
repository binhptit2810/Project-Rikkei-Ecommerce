import apiClient from './apiClient'

/**
 * Service quản lý Sản phẩm (Product Service).
 * Tuân theo tài liệu API hệ thống (docs/API.md):
 *   - GET /api/v1/products             — Danh sách sản phẩm, tìm kiếm, lọc danh mục, phân trang
 *   - GET /api/v1/products/{id}        — Chi tiết sản phẩm
 *   - GET /api/v1/admin/products       — Quản lý sản phẩm dành cho Admin
 *   - POST /api/v1/admin/products      — Thêm sản phẩm mới (Admin)
 *   - PUT /api/v1/admin/products/{id}  — Sửa sản phẩm (Admin)
 *   - DELETE /api/v1/admin/products/{id} — Xóa mềm sản phẩm (Admin)
 */
export const productService = {
  /**
   * Lấy danh sách sản phẩm công khai với tìm kiếm, lọc và phân trang.
   * @param {Object} params - { keyword, categoryId, page, size, sortBy, sortDir }
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ProductResponse>
   */
  async getProducts(params = {}) {
    const response = await apiClient.get('/products', { params })
    return response.data
  },

  /**
   * Lấy thông tin chi tiết một sản phẩm theo ID.
   * @param {number|string} id - ID sản phẩm
   * @returns {Promise<Object>} ApiResponse<ProductResponse>
   */
  async getProductById(id) {
    const response = await apiClient.get(`/products/${id}`)
    return response.data
  },

  /**
   * Lấy danh sách sản phẩm dành cho Admin (bao gồm cả active và inactive).
   * @param {Object} params - { keyword, categoryId, active, page, size, sortBy, sortDir }
   * @returns {Promise<Object>} ApiResponse chứa PageResponse<ProductResponse>
   */
  async getAdminProducts(params = {}) {
    const response = await apiClient.get('/admin/products', { params })
    return response.data
  },

  /**
   * Thêm sản phẩm mới (Yêu cầu quyền ADMIN).
   * @param {Object} productData - { name, description, price, imageUrl, categoryId, stockQuantity, active }
   * @returns {Promise<Object>} ApiResponse<ProductResponse>
   */
  async createProduct(productData) {
    const response = await apiClient.post('/admin/products', productData)
    return response.data
  },

  /**
   * Cập nhật thông tin sản phẩm (Yêu cầu quyền ADMIN).
   * @param {number|string} id - ID sản phẩm
   * @param {Object} productData - Dữ liệu cập nhật
   * @returns {Promise<Object>} ApiResponse<ProductResponse>
   */
  async updateProduct(id, productData) {
    const response = await apiClient.put(`/admin/products/${id}`, productData)
    return response.data
  },

  /**
   * Xóa sản phẩm (soft-delete, yêu cầu quyền ADMIN).
   * @param {number|string} id - ID sản phẩm
   * @returns {Promise<Object>} ApiResponse
   */
  async deleteProduct(id) {
    const response = await apiClient.delete(`/admin/products/${id}`)
    return response.data
  },
}

export default productService
