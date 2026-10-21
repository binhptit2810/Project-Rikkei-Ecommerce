package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Lấy danh sách tất cả danh mục đang hoạt động (active = true).
     * Dành cho khách vãng lai và khách hàng trên trang mua sắm.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tất cả danh mục (bao gồm cả active và inactive).
     * Dành riêng cho trang quản trị Admin.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Xem thông tin chi tiết một danh mục theo ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));
        return CategoryResponse.fromEntity(category);
    }

    /**
     * Tạo danh mục mới (dành cho Admin).
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessException("Tên danh mục đã tồn tại: " + trimmedName);
        }

        Category category = Category.builder()
                .name(trimmedName)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Admin created category id={}, name='{}'", saved.getId(), saved.getName());
        return CategoryResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin danh mục (dành cho Admin).
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));

        String trimmedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new BusinessException("Tên danh mục đã tồn tại: " + trimmedName);
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updated = categoryRepository.save(category);
        log.info("Admin updated category id={}, name='{}'", updated.getId(), updated.getName());
        return CategoryResponse.fromEntity(updated);
    }

    /**
     * Xóa danh mục (dành cho Admin).
     * Nếu danh mục đã có sản phẩm liên kết, chuyển trạng thái sang inactive để bảo toàn toàn vẹn dữ liệu.
     * Nếu chưa có sản phẩm liên kết, xóa trực tiếp khỏi database.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));

        boolean hasProducts = productRepository.existsByCategoryId(id);
        if (hasProducts) {
            log.info("Category id={} has associated products; setting active=false (soft-delete)", id);
            category.setActive(false);
            categoryRepository.save(category);
        } else {
            log.info("Category id={} has no associated products; deleting permanently", id);
            categoryRepository.delete(category);
        }
    }
}
