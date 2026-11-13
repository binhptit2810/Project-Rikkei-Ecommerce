package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `wishlists` trong MySQL.
 * Quản lý danh sách sản phẩm yêu thích của người dùng.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishlists_user_product", columnNames = {"user_id", "product_id"})
    },
    indexes = {
        @Index(name = "idx_wishlists_user", columnList = "user_id"),
        @Index(name = "idx_wishlists_product", columnList = "product_id"),
        @Index(name = "idx_wishlists_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "product"})
@EqualsAndHashCode(of = "id")
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

