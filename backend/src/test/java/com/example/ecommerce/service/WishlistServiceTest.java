package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.WishlistItemResponse;
import com.example.ecommerce.dto.response.WishlistToggleResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.entity.Wishlist;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.repository.WishlistRepository;
import com.example.ecommerce.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho WishlistService.
 * Phụ trách: Đỗ Đức Mạnh (mnhd1)
 * Task ID: [TSK-D15-DDM] Xây Dựng Phân Hệ Sản Phẩm Yêu Thích (Wishlist Backend & Frontend)
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Product testProduct;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .password("encoded_pass")
                .fullName("Đỗ Đức Mạnh")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        testProduct = Product.builder()
                .id(100L)
                .name("Tai nghe Bluetooth Sony WH-1000XM5")
                .price(new BigDecimal("7490000"))
                .imageUrl("https://example.com/sony-xm5.jpg")
                .stockQuantity(25)
                .ratingAverage(4.9)
                .active(true)
                .build();

        testUserDetails = new CustomUserDetails(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getPassword(),
                testUser.getRole(),
                testUser.getActive(),
                testUser.getEmailVerified()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        lenient().when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Toggle sản phẩm chưa thích -> Thêm mới thành công và trả về liked = true")
    void toggleWishlist_whenNotLikedYet_shouldAddAndReturnLikedTrue() {
        Long productId = testProduct.getId();
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUserIdAndProductId(testUser.getId(), productId)).thenReturn(Optional.empty());

        WishlistToggleResponse response = wishlistService.toggleWishlist(productId);

        assertThat(response).isNotNull();
        assertThat(response.isLiked()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Đã thêm vào danh sách yêu thích");
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
        verify(wishlistRepository, never()).deleteByUserIdAndProductId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("2. Toggle sản phẩm đã thích -> Xóa thành công và trả về liked = false")
    void toggleWishlist_whenAlreadyLiked_shouldRemoveAndReturnLikedFalse() {
        Long productId = testProduct.getId();
        Wishlist existingWishlist = Wishlist.builder()
                .id(50L)
                .user(testUser)
                .product(testProduct)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(wishlistRepository.findByUserIdAndProductId(testUser.getId(), productId)).thenReturn(Optional.of(existingWishlist));

        WishlistToggleResponse response = wishlistService.toggleWishlist(productId);

        assertThat(response).isNotNull();
        assertThat(response.isLiked()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Đã xóa khỏi danh sách yêu thích");
        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(testUser.getId(), productId);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("3. Toggle sản phẩm không tồn tại -> Ném ResourceNotFoundException")
    void toggleWishlist_whenProductNotFound_shouldThrowResourceNotFoundException() {
        Long nonExistentProductId = 9999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.toggleWishlist(nonExistentProductId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tồn tại với ID: " + nonExistentProductId);

        verify(wishlistRepository, never()).save(any(Wishlist.class));
        verify(wishlistRepository, never()).deleteByUserIdAndProductId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("4. Lấy danh sách wishlist -> Trả về đúng thứ tự mới nhất trước")
    void getWishlist_shouldReturnItemsOrderedByCreatedAtDesc() {
        Product product2 = Product.builder()
                .id(200L)
                .name("Bàn phím cơ Keychron Q1 Pro")
                .price(new BigDecimal("4290000"))
                .imageUrl("https://example.com/keychron-q1.jpg")
                .stockQuantity(10)
                .ratingAverage(4.8)
                .active(true)
                .build();

        LocalDateTime now = LocalDateTime.now();
        Wishlist w1 = Wishlist.builder()
                .id(1L)
                .user(testUser)
                .product(product2)
                .createdAt(now.plusHours(1)) // Thêm gần đây hơn
                .build();

        Wishlist w2 = Wishlist.builder()
                .id(2L)
                .user(testUser)
                .product(testProduct)
                .createdAt(now) // Thêm trước đó
                .build();

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(w1, w2));

        List<WishlistItemResponse> result = wishlistService.getWishlist();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(200L);
        assertThat(result.get(0).getName()).isEqualTo("Bàn phím cơ Keychron Q1 Pro");
        assertThat(result.get(0).getPrice()).isEqualTo(new BigDecimal("4290000"));
        assertThat(result.get(0).getDiscountPrice()).isEqualTo(new BigDecimal("4290000"));
        assertThat(result.get(0).getAddedAt()).isEqualTo(now.plusHours(1));

        assertThat(result.get(1).getId()).isEqualTo(100L);
        assertThat(result.get(1).getName()).isEqualTo("Tai nghe Bluetooth Sony WH-1000XM5");
        assertThat(result.get(1).getAddedAt()).isEqualTo(now);

        verify(wishlistRepository, times(1)).findByUserIdOrderByCreatedAtDesc(testUser.getId());
    }

    @Test
    @DisplayName("5. Lấy danh sách Product IDs yêu thích -> Trả về danh sách IDs chính xác")
    void getWishlistProductIds_shouldReturnListOfProductIds() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findProductIdsByUserId(testUser.getId())).thenReturn(List.of(100L, 200L, 300L));

        List<Long> productIds = wishlistService.getWishlistProductIds();

        assertThat(productIds).containsExactly(100L, 200L, 300L);
        verify(wishlistRepository, times(1)).findProductIdsByUserId(testUser.getId());
    }

    @Test
    @DisplayName("6. Toggle sản phẩm khi userId không tồn tại -> Ném ResourceNotFoundException")
    void toggleWishlist_whenUserNotFound_shouldThrowResourceNotFoundException() {
        Long productId = testProduct.getId();
        Long nonExistentUserId = 8888L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.toggleWishlist(nonExistentUserId, productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại với ID: " + nonExistentUserId);
    }

    @Test
    @DisplayName("7. getCurrentUser khi chưa đăng nhập (SecurityContext null hoặc anonymous) -> Ném BusinessException")
    void getCurrentUser_whenNotAuthenticated_shouldThrowBusinessException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> wishlistService.getWishlist())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng đăng nhập để thực hiện thao tác");
    }

    @Test
    @DisplayName("8. getCurrentUser khi principal là String email (không phải CustomUserDetails) -> Tra cứu qua findByEmail")
    void getCurrentUser_whenPrincipalIsStringEmail_shouldLookupByEmail() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("customer@example.com", "credentials", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of());

        List<WishlistItemResponse> result = wishlistService.getWishlist();

        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findByEmail("customer@example.com");
    }

    @Test
    @DisplayName("9. getCurrentUser khi user trong DB không tồn tại -> Ném ResourceNotFoundException")
    void getCurrentUser_whenUserNotFoundInDb_shouldThrowResourceNotFoundException() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.getWishlist())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại với ID: " + testUser.getId());
    }
}
