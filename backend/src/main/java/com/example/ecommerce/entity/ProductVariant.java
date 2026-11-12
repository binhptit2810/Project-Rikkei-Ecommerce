package com.example.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `product_variants` trong MySQL.
 * Quản lý biến thể sản phẩm theo kích cỡ (Size) và tồn kho riêng biệt.
 * [TSK-D14-VDHH]
 */
@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variant_size", columnNames = {"product_id", "size"}),
        @UniqueConstraint(name = "uk_product_variant_sku", columnNames = {"sku"})
    },
    indexes = {
        @Index(name = "idx_product_variants_product", columnList = "product_id"),
        @Index(name = "idx_product_variants_sku", columnList = "sku")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "product")
@EqualsAndHashCode(of = "id")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @NotBlank(message = "Kích cỡ sản phẩm không được để trống")
    @Column(nullable = false, length = 50)
    private String size;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được nhỏ hơn 0")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @NotBlank(message = "Mã SKU không được để trống")
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

