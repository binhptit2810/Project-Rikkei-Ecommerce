package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateCategoryRequest;
import com.example.ecommerce.dto.request.UpdateCategoryRequest;
import com.example.ecommerce.dto.response.CategoryResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý danh mục sản phẩm (Category Module).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D06-DDM] Xây dựng API Quản trị Sản phẩm & Danh mục (Admin CRUD)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

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
     * Lấy toàn bộ danh sách danh mục phục vụ trang quản trị Admin (bao gồm cả active và inactive).
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
     * Admin: Thêm mới danh mục.
     * Kiểm tra trùng tên danh mục (không phân biệt hoa thường), khởi tạo active = true.
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String trimmedName = request.getName().trim();
        log.info("Admin creating category with name: {}", trimmedName);

        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessException("Tên danh mục đã tồn tại: " + trimmedName);
        }

        Category category = Category.builder()
                .name(trimmedName)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());
        return CategoryResponse.fromEntity(savedCategory);
    }

    /**
     * Admin: Cập nhật thông tin danh mục.
     * Kiểm tra trùng tên với danh mục khác, cập nhật thông tin và trạng thái active nếu được truyền vào.
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        String trimmedName = request.getName().trim();
        log.info("Admin updating category id: {} with name: {}", id, trimmedName);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new BusinessException("Tên danh mục đã tồn tại: " + trimmedName);
        }

        category.setName(trimmedName);
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category id: {} updated successfully", updatedCategory.getId());
        return CategoryResponse.fromEntity(updatedCategory);
    }

    /**
     * Admin: Xóa mềm danh mục.
     * Chuyển active = false, không xóa vật lý khỏi CSDL để đảm bảo toàn vẹn dữ liệu cho các sản phẩm/đơn hàng cũ.
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Admin soft-deleting category id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", id));

        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category id: {} marked as inactive (soft deleted)", id);
    }
}
