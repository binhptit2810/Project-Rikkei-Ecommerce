package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller công khai cho sản phẩm (Public Products API).
 * Cho phép xem danh sách, tìm kiếm, phân trang và xem chi tiết sản phẩm mà không cần đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Danh sách sản phẩm với tìm kiếm, lọc và phân trang.
     * GET /api/v1/products?keyword=ao&categoryId=1&page=0&size=12&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<ProductResponse> products = productService.getProducts(
                keyword, categoryId, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Chi tiết sản phẩm theo ID.
     * GET /api/v1/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
