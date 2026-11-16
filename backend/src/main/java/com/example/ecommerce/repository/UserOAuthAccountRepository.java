package com.example.ecommerce.repository;

import com.example.ecommerce.entity.UserOAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác bảng `user_oauth_accounts`.
 * Phụ trách: Cao Chí Thiện
 * Task ID: TSK-D15-CCT - Tích hợp đăng nhập nhanh bằng tài khoản Google
 */
@Repository
public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {

    /**
     * Tìm liên kết OAuth theo nhà cung cấp (provider) và định danh providerId (Google sub).
     */
    Optional<UserOAuthAccount> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Kiểm tra xem định danh provider đã được liên kết với bất kỳ tài khoản nào chưa.
     */
    boolean existsByProviderAndProviderId(String provider, String providerId);

    /**
     * Lấy danh sách các liên kết OAuth của một người dùng.
     */
    List<UserOAuthAccount> findByUserId(Long userId);

    /**
     * Tìm liên kết của một người dùng với một provider cụ thể.
     */
    Optional<UserOAuthAccount> findByUserIdAndProvider(Long userId, String provider);
}

