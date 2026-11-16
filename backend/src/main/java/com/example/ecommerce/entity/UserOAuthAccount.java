package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `user_oauth_accounts` — lưu định danh OAuth2 (Google, etc.).
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Entity
@Table(
        name = "user_oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_oauth_provider_id", columnNames = {"provider", "provider_id"})
        },
        indexes = {
                @Index(name = "idx_user_oauth_user_id", columnList = "user_id"),
                @Index(name = "idx_user_oauth_provider_id", columnList = "provider, provider_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    /** Tên nhà cung cấp danh tính, ví dụ: "GOOGLE". */
    @Column(nullable = false, length = 50)
    private String provider;

    /** Định danh duy nhất từ nhà cung cấp (với Google là Subject - `sub`). */
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    /** Email nhận được từ nhà cung cấp OAuth2 tại thời điểm liên kết. */
    @Column(length = 100)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

