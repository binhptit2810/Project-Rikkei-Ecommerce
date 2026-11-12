package com.example.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng `reviews` trong MySQL.
 * Quản lý đánh giá và nhận xét sản phẩm của khách hàng.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_product_user", columnNames = {"product_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_reviews_product", columnList = "product_id"),
        @Index(name = "idx_reviews_product_created", columnList = "product_id, created_at"),
        @Index(name = "idx_reviews_product_visible", columnList = "product_id, is_visible"),
        @Index(name = "idx_reviews_visible", columnList = "is_visible"),
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
@ToString(exclude = {"product", "user", "images"})
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

    /** Trạng thái ẩn/hiển thị đánh giá do Admin quản lý */
    @Builder.Default
    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    /** Phản hồi chính thức từ Quản trị viên / Người bán */
    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    /** Thời điểm Quản trị viên gửi phản hồi */
    @Column(name = "admin_reply_at")
    private LocalDateTime adminReplyAt;

    /** Trạng thái hiển thị công khai (true: hiển thị, false: bị quản trị viên ẩn) */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;

    /** Danh sách hình ảnh đính kèm của đánh giá (tối đa 3 ảnh) [TSK-D14-CCT] */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isVisible == null) {
            this.isVisible = true;
        }
        if (this.isHidden == null) {
            this.isHidden = !this.isVisible;
        }
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

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
        if (isVisible != null) {
            this.isHidden = !isVisible;
        }
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
        if (isHidden != null) {
            this.isVisible = !isHidden;
        }
    }

    public void addImage(ReviewImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
        image.setReview(this);
    }

    public void removeImage(ReviewImage image) {
        if (this.images != null) {
            this.images.remove(image);
            image.setReview(null);
        }
    }
}
