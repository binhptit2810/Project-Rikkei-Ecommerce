package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu sản phẩm trong CSDL.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D08-DDM] Tối ưu hóa Bộ lọc Tìm kiếm Sản phẩm & Viết Unit Tests ProductService
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Tìm kiếm và lọc sản phẩm đa tiêu chí tối ưu:
     * - keyword: tìm kiếm theo tên hoặc mô tả (không phân biệt hoa thường, chứa từ khóa)
     * - categoryId: lọc theo danh mục
     * - minPrice & maxPrice: lọc theo khoảng giá linh hoạt
     * - active: lọc theo trạng thái hoạt động (true/false, hoặc null để lấy tất cả)
     */
    @Query(value = "SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category c " +
           "WHERE (:active IS NULL OR p.active = :active) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "     OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p " +
           "WHERE (:active IS NULL OR p.active = :active) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "     OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("active") Boolean active,
                                 Pageable pageable);

    /**
     * Overload tìm kiếm không kèm khoảng giá (tương thích ngược).
     */
    default Page<Product> searchProducts(String keyword, Long categoryId, Boolean active, Pageable pageable) {
        return searchProducts(keyword, categoryId, null, null, active, pageable);
    }

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    long countByStockQuantity(Integer stockQuantity);

    long countByStockQuantityLessThanEqual(Integer stockQuantity);

    long countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(Integer max, Integer min);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.stockQuantity <= :threshold ORDER BY p.stockQuantity ASC")
    List<Product> findByStockQuantityLessThanEqualOrderByStockQuantityAsc(@Param("threshold") Integer threshold);
}
