package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.WishlistItemResponse;
import com.example.ecommerce.dto.response.WishlistToggleResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.Wishlist;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.repository.WishlistRepository;
import com.example.ecommerce.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ Danh sách sản phẩm yêu thích (Wishlist).
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách sản phẩm yêu thích của người dùng hiện tại (mới nhất trước).
     */
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist() {
        User currentUser = getCurrentUser();
        return getWishlist(currentUser.getId());
    }

    /**
     * Lấy danh sách sản phẩm yêu thích theo userId (mới nhất trước).
     */
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(Long userId) {
        log.info("[Wishlist] Lấy danh sách sản phẩm yêu thích cho user ID: {}", userId);
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return wishlists.stream()
                .map(WishlistItemResponse::fromEntity)
                .toList();
    }

    /**
     * Lấy danh sách ID các sản phẩm đã thả tim của người dùng hiện tại.
     */
    @Transactional(readOnly = true)
    public List<Long> getWishlistProductIds() {
        User currentUser = getCurrentUser();
        return getWishlistProductIds(currentUser.getId());
    }

    /**
     * Lấy danh sách ID các sản phẩm đã thả tim theo userId.
     */
    @Transactional(readOnly = true)
    public List<Long> getWishlistProductIds(Long userId) {
        return wishlistRepository.findProductIdsByUserId(userId);
    }

    /**
     * Bật/Tắt yêu thích sản phẩm cho người dùng hiện tại (Toggle Wishlist).
     */
    @Transactional
    public WishlistToggleResponse toggleWishlist(Long productId) {
        User currentUser = getCurrentUser();
        return toggleWishlist(currentUser.getId(), productId);
    }

    /**
     * Bật/Tắt yêu thích sản phẩm theo userId và productId.
     */
    @Transactional
    public WishlistToggleResponse toggleWishlist(Long userId, Long productId) {
        log.info("[Wishlist] Toggle wishlist cho user ID: {} với product ID: {}", userId, productId);

        // 1. Kiểm tra productId tồn tại trong DB (ném ResourceNotFoundException nếu không tìm thấy)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        // 2. Tìm người dùng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + userId));

        // 3. Kiểm tra cặp (userId, productId) đã tồn tại trong bảng wishlists chưa
        Optional<Wishlist> existingWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId);

        if (existingWishlist.isPresent()) {
            // Đã tồn tại -> Xóa khỏi bảng
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            log.info("[Wishlist] Đã xóa sản phẩm ID: {} khỏi wishlist của user ID: {}", productId, userId);
            return WishlistToggleResponse.builder()
                    .liked(false)
                    .message("Đã xóa khỏi danh sách yêu thích")
                    .build();
        } else {
            // Chưa tồn tại -> Thêm mới vào bảng
            Wishlist newWishlist = Wishlist.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(newWishlist);
            log.info("[Wishlist] Đã thêm sản phẩm ID: {} vào wishlist của user ID: {}", productId, userId);
            return WishlistToggleResponse.builder()
                    .liked(true)
                    .message("Đã thêm vào danh sách yêu thích")
                    .build();
        }
    }

    /**
     * Lấy thông tin người dùng hiện tại từ Security Context.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("Vui lòng đăng nhập để thực hiện thao tác");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails cud && cud.getUserId() != null) {
            return userRepository.findById(cud.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với ID: " + cud.getUserId()));
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + email));
    }
}

