package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ApiResponse;
import com.example.ecommerce.dto.response.DealOfTheDayResponse;
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
     * Lấy sản phẩm Deal of the Day với chương trình khuyến mãi thực tế từ CSDL.
     * GET /api/v1/products/deal-of-the-day
     */
    @GetMapping("/deal-of-the-day")
    public ResponseEntity<ApiResponse<DealOfTheDayResponse>> getDealOfTheDay() {
        DealOfTheDayResponse deal = productService.getDealOfTheDay();
        return ResponseEntity.ok(ApiResponse.success(deal));
    }

    /**
     * Lấy danh sách sản phẩm bán chạy nhất (Best Sellers) theo tháng và năm có phân trang.
     * GET /api/v1/products/best-sellers?month=11&year=2026&page=0&size=10
     *
     * Phụ trách: Đặng Thanh Bình (DTB)
     * DoD: Trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng, tốc độ truy vấn < 30ms.
     */
    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getBestSellers(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ProductResponse> bestSellers = productService.getBestSellers(month, year, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Best Seller thành công", bestSellers));
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
