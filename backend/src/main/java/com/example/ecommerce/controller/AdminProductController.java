package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản trị sản phẩm (Admin Product Management API).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 *
 * Tất cả endpoint yêu cầu quyền ADMIN:
 * - Bảo vệ ở cấp Filter: /api/v1/admin/** (hasRole('ADMIN'))
 * - Bảo vệ ở cấp Method: @PreAuthorize("hasRole('ADMIN')")
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * Lấy danh sách sản phẩm phục vụ quản trị (hỗ trợ tìm kiếm, lọc theo danh mục, trạng thái active/inactive và phân trang).
     * GET /api/v1/admin/products?keyword=ao&categoryId=1&active=true&page=0&size=12&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<ProductResponse> products = productService.getAdminProducts(
                keyword, categoryId, active, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Lấy thông tin chi tiết sản phẩm dành cho Admin theo ID (bao gồm cả sản phẩm inactive).
     * GET /api/v1/admin/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getAdminProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Thêm mới sản phẩm.
     * POST /api/v1/admin/products
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse createdProduct = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới sản phẩm thành công", createdProduct));
    }

    /**
     * Cập nhật thông tin sản phẩm.
     * PUT /api/v1/admin/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse updatedProduct = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", updatedProduct));
    }

    /**
     * Xóa mềm sản phẩm (chuyển active = false, không xóa vật lý).
     * DELETE /api/v1/admin/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mềm sản phẩm thành công", null));
    }
}

