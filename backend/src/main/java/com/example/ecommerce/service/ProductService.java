package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Product;
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
 * Service quản lý sản phẩm (Product Module - Sprint 1: Read-only).
 * Admin CRUD sản phẩm sẽ được hiện thực trong Sprint 2 (TSK-D06-DDM / TSK-D07-DDM).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "createdAt", "name");

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
     * Xem thông tin chi tiết một sản phẩm theo ID (chỉ áp dụng với sản phẩm active).
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
