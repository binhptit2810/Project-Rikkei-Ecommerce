package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller công khai cho sản phẩm (Public Products API).
 * Cho phép xem danh sách, tìm kiếm đa tiêu chí, lọc khoảng giá, phân trang và xem chi tiết sản phẩm mà không cần đăng nhập.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D08-DDM] Tối ưu hóa Bộ lọc Tìm kiếm Sản phẩm & Viết Unit Tests ProductService
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Danh sách sản phẩm với tìm kiếm, lọc danh mục, lọc khoảng giá minPrice/maxPrice và phân trang.
     * GET /api/v1/products?keyword=ao&categoryId=1&minPrice=100000&maxPrice=500000&page=0&size=12&sortBy=price&sortDir=asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<ProductResponse> products = productService.getProducts(
                keyword, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Chi tiết sản phẩm theo ID (chỉ hiển thị nếu active = true).
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
