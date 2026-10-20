package com.example.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng `users` trong MySQL.
 *
 * Schema tham chiếu (01-schema.sql):
 *   id, email (UNIQUE), password (BCrypt), full_name, phone, address,
 *   role ENUM('CUSTOMER','ADMIN'), active BOOLEAN, created_at, updated_at
 *
 * Lưu ý bảo mật:
 *   - Trường password không được trả về trong response (dùng UserResponse DTO).
 *   - Không truyền entity này trực tiếp vào ResponseEntity.
 */
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
