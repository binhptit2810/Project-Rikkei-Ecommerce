package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProductRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "price", "name");
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Tìm kiếm và phân trang sản phẩm cho khách vãng lai và khách hàng (chỉ lấy active = true).
     *
     * @param keyword    từ khóa tìm kiếm theo tên hoặc mô tả
     * @param categoryId ID danh mục cần lọc
     * @param page       số trang (bắt đầu từ 0)
     * @param size       số lượng sản phẩm trên mỗi trang
     * @param sortBy     trường sắp xếp (createdAt, price, name)
     * @param sortDir    hướng sắp xếp (asc, desc)
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = productRepository.searchProducts(
                searchKeyword,
                categoryId,
                true, // chỉ hiển thị sản phẩm đang kinh doanh
                pageable
        );

        return PageResponse.from(productPage, ProductResponse::fromEntity);
    }

    /**
     * Lấy thông tin chi tiết một sản phẩm công khai theo ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Sản phẩm", id);
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Tìm kiếm và phân trang sản phẩm dành riêng cho Admin (xem cả active và inactive).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = productRepository.searchProducts(
                searchKeyword,
                categoryId,
                active,
                pageable
        );

        return PageResponse.from(productPage, ProductResponse::fromEntity);
    }

    /**
     * Tạo sản phẩm mới (Admin).
     *
     * Lưu ý tích hợp:
     *   - stockQuantity được khởi tạo theo giá trị request hoặc mặc định 0.
     *   - Thống nhất trường tồn kho với module Inventory của Đặng Thanh Bình.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateProductPrice(request.getPrice());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));
        }

        Integer stock = (request.getStockQuantity() != null) ? request.getStockQuantity() : 0;
        Boolean active = (request.getActive() != null) ? request.getActive() : true;

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .stockQuantity(stock)
                .active(active)
                .build();

        Product saved = productRepository.save(product);
        log.info("Admin created product id={}, name='{}', stock={}", saved.getId(), saved.getName(), saved.getStockQuantity());
        return ProductResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin sản phẩm (Admin).
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        validateProductPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());

        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updated = productRepository.save(product);
        log.info("Admin updated product id={}, name='{}'", updated.getId(), updated.getName());
        return ProductResponse.fromEntity(updated);
    }

    /**
     * Xóa sản phẩm (Admin).
     * Áp dụng xóa mềm (soft-delete, active = false) để tránh vi phạm khóa ngoại trong đơn hàng (orders, order_items).
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Admin soft-deleted product id={}", id);
    }

    /**
     * Tạo Pageable chuẩn hóa từ các tham số request.
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, validSortBy));
    }

    private void validateProductPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Giá sản phẩm phải lớn hơn hoặc bằng 0");
        }
    }
}
