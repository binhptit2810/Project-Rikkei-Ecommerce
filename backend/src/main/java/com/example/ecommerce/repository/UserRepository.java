package com.example.ecommerce.repository;

import com.example.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác bảng `users`.
 *
 * Được sử dụng bởi:
 *   - AuthService   — tìm kiếm user khi đăng ký / đăng nhập
 *   - UserService   — lấy thông tin hồ sơ người dùng hiện tại
 *   - CustomUserDetailsService — Spring Security load UserDetails từ email
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email (không phân biệt hoa thường nên email được chuẩn hóa
     * thành lowercase trước khi lưu tại tầng Service).
     */
    Optional<User> findByEmail(String email);

    /** Kiểm tra email đã được sử dụng để đăng ký tài khoản chưa. */
    boolean existsByEmail(String email);
}
