package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho ReviewService.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .password("encoded_password")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(true)
                .build();

        testProduct = Product.builder()
                .id(10L)
                .name("Áo thun nam basic Cotton")
                .price(new BigDecimal("199000"))
                .ratingAverage(0.0)
                .totalReviews(0)
                .stockQuantity(50)
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test 1: Tạo review thành công khi khách đã mua hàng -> Verify tính lại ratingAverage và totalReviews")
    void createReview_success_updatesProductRatingAndTotalReviews() {
        // Arrange
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Sản phẩm chất vải rất thoáng mát, đóng gói đẹp!")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(10L, 1L)).thenReturn(false);

        Review savedReview = Review.builder()
                .id(100L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // Mô phỏng tính toán lại rating: điểm trung bình 4.7, tổng 3 review
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.666666);
        when(reviewRepository.countByProductId(10L)).thenReturn(3);

        // Act
        ReviewResponse response = reviewService.createReview(10L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo(request.getComment());
        assertThat(response.getUserFullName()).isEqualTo("Nguyễn Văn A");

        // Verify product được cập nhật ratingAverage làm tròn 1 chữ số thập phân (4.7) và totalReviews = 3
        assertThat(testProduct.getRatingAverage()).isEqualTo(4.7);
        assertThat(testProduct.getTotalReviews()).isEqualTo(3);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Test 2: Ném lỗi BadRequestException khi khách hàng chưa từng mua sản phẩm hoặc đơn chưa hoàn thành")
    void createReview_notPurchased_throwsBadRequestException() {
        // Arrange
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(4)
                .comment("Chưa mua nhưng thích comment")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Test 3: Ném lỗi DuplicateResourceException khi khách hàng cố tình đánh giá lần thứ 2 trên cùng sản phẩm")
    void createReview_duplicateReview_throwsDuplicateResourceException() {
        // Arrange
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Đánh giá lần 2")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(10L, 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Bạn đã đánh giá sản phẩm này rồi");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Test 4: Lấy danh sách review phân trang và summary tính đúng điểm trung bình")
    void getProductReviewsAndSummary_success() {
        // --- 1. Test getProductReviews ---
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.existsById(10L)).thenReturn(true);

        Review r1 = Review.builder()
                .id(1L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Tuyệt vời")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Review> page = new PageImpl<>(List.of(r1), pageable, 1);
        when(reviewRepository.findByProductId(eq(10L), any(Pageable.class))).thenReturn(page);

        PageResponse<ReviewResponse> reviewsResponse = reviewService.getProductReviews(10L, pageable);

        assertThat(reviewsResponse).isNotNull();
        assertThat(reviewsResponse.getTotalElements()).isEqualTo(1);
        assertThat(reviewsResponse.getContent()).hasSize(1);
        assertThat(reviewsResponse.getContent().get(0).getComment()).isEqualTo("Tuyệt vời");

        // --- 2. Test getProductRatingSummary ---
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.82);
        when(reviewRepository.countByProductId(10L)).thenReturn(10);

        List<Object[]> groupedRows = new ArrayList<>();
        groupedRows.add(new Object[]{5, 8L}); // 8 đánh giá 5 sao
        groupedRows.add(new Object[]{4, 2L}); // 2 đánh giá 4 sao
        when(reviewRepository.countRatingsGrouped(10L)).thenReturn(groupedRows);

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(10L);

        assertThat(summary).isNotNull();
        assertThat(summary.getRatingAverage()).isEqualTo(4.8); // 4.82 làm tròn -> 4.8
        assertThat(summary.getTotalReviews()).isEqualTo(10);
        assertThat(summary.getRatingCounts().get(5)).isEqualTo(8L);
        assertThat(summary.getRatingCounts().get(4)).isEqualTo(2L);
        assertThat(summary.getRatingCounts().get(3)).isEqualTo(0L);
        assertThat(summary.getRatingCounts().get(2)).isEqualTo(0L);
        assertThat(summary.getRatingCounts().get(1)).isEqualTo(0L);
    }
}

