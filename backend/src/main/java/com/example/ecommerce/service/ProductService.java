package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
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

import java.util.Set;

/**
 * Service quản lý sản phẩm (Product Module).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "price", "createdAt", "name", "stockQuantity");

    /**
     * Tìm kiếm và lọc sản phẩm đang active dành cho khách mua sắm.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = productRepository.searchProducts(
                searchKeyword,
                categoryId,
                true,
                pageable
        );

        return PageResponse.from(productPage, ProductResponse::fromEntity);
    }

    /**
     * Xem thông tin chi tiết một sản phẩm theo ID (chỉ áp dụng với sản phẩm active dành cho khách hàng).
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Sản phẩm", id);
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Lấy danh sách sản phẩm phục vụ quản trị Admin (hỗ trợ phân trang, tìm kiếm và lọc theo trạng thái active/inactive).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
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
     * Lấy thông tin chi tiết sản phẩm dành cho Admin (bao gồm cả sản phẩm inactive).
     */
    @Transactional(readOnly = true)
    public ProductResponse getAdminProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        return ProductResponse.fromEntity(product);
    }

    /**
     * Admin: Thêm mới sản phẩm.
     * - Xác thực danh mục tồn tại và đang active.
     * - Khởi tạo active = true và số lượng tồn kho ban đầu.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Admin creating product with name: {}, categoryId: {}", request.getName(), request.getCategoryId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Admin: Cập nhật thông tin sản phẩm.
     * - Xác thực sản phẩm và danh mục tồn tại.
     * - Danh mục được gán phải đang active.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Admin updating product id: {}", id);

        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product id: {} updated successfully", updatedProduct.getId());
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Admin: Xóa mềm sản phẩm.
     * Chuyển active = false, tuyệt đối không dùng repository.delete() để đảm bảo toàn vẹn dữ liệu cho đơn hàng cũ.
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Admin soft-deleting product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Product id: {} marked as inactive (soft deleted)", id);
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
}
