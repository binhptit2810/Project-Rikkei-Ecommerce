package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác bảng `users`.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-AUTH-00 - Hoàn thiện xác thực JWT, Opaque Token và RBAC
 *
 * Được sử dụng bởi:
 *   - AuthService   — tìm kiếm user khi đăng ký / đăng nhập
 *   - UserService   — lấy thông tin hồ sơ người dùng hiện tại, danh sách khách hàng
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

    /** Lấy danh sách người dùng theo Role có phân trang (phục vụ lọc Customer không lẫn Admin). */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Tìm kiếm người dùng theo Role và từ khóa (họ tên, email, số điện thoại).
     * Tuyệt đối không trả về tài khoản ngoài Role chỉ định.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchByRoleAndKeyword(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}
