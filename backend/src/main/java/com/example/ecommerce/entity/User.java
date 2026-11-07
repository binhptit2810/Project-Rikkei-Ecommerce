package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `users` trong MySQL.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Schema tham chiếu (01-schema.sql):
 *   id, email (UNIQUE), password (BCrypt), full_name, phone, address,
 *   role ENUM('CUSTOMER','ADMIN'), active BOOLEAN, created_at, updated_at
 *
 * Lưu ý bảo mật:
 *   - Trường password không được trả về trong response (dùng UserResponse DTO).
 *   - Không truyền entity này trực tiếp vào ResponseEntity.
 */
// [TSK-D02-CCT]
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email là username duy nhất — dùng để đăng nhập. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt hash — KHÔNG BAO GIỜ lưu plaintext. */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 15)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    /** CUSTOMER hoặc ADMIN — khớp ENUM trong schema. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Cờ xác thực email: true nếu đã xác thực, false nếu chưa xác thực (CUSTOMER mới). */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /** Thời điểm hoàn tất xác thực email. */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /** Lý do khóa tài khoản nếu active = false. */
    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    /** Thời điểm khóa tài khoản nếu active = false. */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

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
