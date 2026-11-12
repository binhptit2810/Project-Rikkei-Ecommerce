package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng `products` trong MySQL.
 *
 * Schema tham chiếu (01-schema.sql):
 *   id, name, description, price (DECIMAL 15,2), image_url, category_id,
 *   stock_quantity (INT >= 0), active, created_at, updated_at
 *
 * Tối ưu hóa hiệu năng truy vấn phân trang & tìm kiếm đa tiêu chí (< 200ms):
 *   - Đánh Index trên: category_id, name, active, price, created_at
 *   - Composite index: (active, category_id, price)
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_active", columnList = "active"),
        @Index(name = "idx_products_price", columnList = "price"),
        @Index(name = "idx_products_created_at", columnList = "created_at"),
        @Index(name = "idx_products_active_category_price", columnList = "active, category_id, price")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Điểm đánh giá trung bình từ 1.0 đến 5.0 (làm tròn 1 chữ số thập phân). */
    @Column(name = "rating_average", nullable = false)
    @Builder.Default
    private Double ratingAverage = 0.0;

    /** Tổng số lượt đánh giá của sản phẩm. */
    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    /** Danh sách đánh giá của sản phẩm. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews = new ArrayList<>();

    /** Danh sách các biến thể kích cỡ của sản phẩm [TSK-D14-VDHH]. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductVariant> variants = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        if (variants == null) {
            variants = new ArrayList<>();
        }
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        if (variants != null) {
            variants.remove(variant);
            variant.setProduct(null);
        }
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
        if (this.stockQuantity == null) {
            this.stockQuantity = 0;
        }
        if (this.ratingAverage == null) {
            this.ratingAverage = 0.0;
        }
        if (this.totalReviews == null) {
            this.totalReviews = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
