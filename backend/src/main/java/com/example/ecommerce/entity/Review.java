package com.example.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `reviews` trong MySQL.
 * Quản lý đánh giá và nhận xét sản phẩm của khách hàng.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_product_user", columnNames = {"product_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_reviews_product", columnList = "product_id"),
        @Index(name = "idx_reviews_user", columnList = "user_id"),
        @Index(name = "idx_reviews_rating", columnList = "rating"),
        @Index(name = "idx_reviews_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"product", "user"})
@EqualsAndHashCode(of = "id")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT", length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

