package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewEligibilityResponse;
import com.example.ecommerce.dto.response.ReviewImageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.ReviewImage;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewImageRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho ReviewService bao gồm Upload ảnh và Quản lý Admin.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM] & [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
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

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private User otherUser;
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

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .password("encoded_password")
                .fullName("Trần Văn B")
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
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.666666);
        when(reviewRepository.countByProductId(10L)).thenReturn(3);

        ReviewResponse response = reviewService.createReview(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo(request.getComment());
        assertThat(response.getUserFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.getIsVisible()).isTrue();

        assertThat(testProduct.getRatingAverage()).isEqualTo(4.7);
        assertThat(testProduct.getTotalReviews()).isEqualTo(3);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Test 2: Ném lỗi BadRequestException khi khách hàng chưa từng mua sản phẩm hoặc đơn chưa hoàn thành")
    void createReview_notPurchased_throwsBadRequestException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(4)
                .comment("Chưa mua nhưng thích comment")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Test 3: Ném lỗi DuplicateResourceException khi khách hàng cố tình đánh giá lần thứ 2 trên cùng sản phẩm")
    void createReview_duplicateReview_throwsDuplicateResourceException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Đánh giá lần 2")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(10L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Bạn đã đánh giá sản phẩm này rồi");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Test 4: Lấy danh sách review hiển thị công khai phân trang và summary tính đúng điểm trung bình")
    void getProductReviewsAndSummary_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.existsById(10L)).thenReturn(true);

        Review r1 = Review.builder()
                .id(1L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Tuyệt vời")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Review> page = new PageImpl<>(List.of(r1), pageable, 1);
        when(reviewRepository.findByProductIdAndIsVisibleTrue(eq(10L), any(Pageable.class))).thenReturn(page);

        PageResponse<ReviewResponse> reviewsResponse = reviewService.getProductReviews(10L, pageable);

        assertThat(reviewsResponse).isNotNull();
        assertThat(reviewsResponse.getTotalElements()).isEqualTo(1);
        assertThat(reviewsResponse.getContent()).hasSize(1);
        assertThat(reviewsResponse.getContent().get(0).getComment()).isEqualTo("Tuyệt vời");

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.82);
        when(reviewRepository.countByProductId(10L)).thenReturn(10);

        List<Object[]> groupedRows = new ArrayList<>();
        groupedRows.add(new Object[]{5, 8L});
        groupedRows.add(new Object[]{4, 2L});
        when(reviewRepository.countRatingsGrouped(10L)).thenReturn(groupedRows);

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(10L);

        assertThat(summary).isNotNull();
        assertThat(summary.getRatingAverage()).isEqualTo(4.8);
        assertThat(summary.getTotalReviews()).isEqualTo(10);
        assertThat(summary.getRatingCounts().get(5)).isEqualTo(8L);
        assertThat(summary.getRatingCounts().get(4)).isEqualTo(2L);
        assertThat(summary.getRatingCounts().get(3)).isEqualTo(0L);
    }

    // =========================================================================
    // TESTS CHO UPLOAD ẢNH ĐÁNH GIÁ (TSK-D14-CCT)
    // =========================================================================

    @Test
    @DisplayName("Test 5: Khách hàng upload 2 ảnh cho đánh giá của mình thành công")
    void uploadReviewImages_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Review review = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .rating(5)
                .isVisible(true)
                .images(new ArrayList<>())
                .build();

        when(reviewRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(100L)).thenReturn(0);

        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.png", "image/png", new byte[]{4, 5, 6});
        List<MultipartFile> files = List.of(file1, file2);

        when(fileStorageService.validateReviewImage(file1)).thenReturn(new byte[]{1, 2, 3});
        when(fileStorageService.validateReviewImage(file2)).thenReturn(new byte[]{4, 5, 6});

        FileUploadResponse upload1 = FileUploadResponse.builder()
                .url("/api/v1/uploads/reviews/uuid1.jpg")
                .fileName("reviews/uuid1.jpg")
                .build();
        FileUploadResponse upload2 = FileUploadResponse.builder()
                .url("/api/v1/uploads/reviews/uuid2.png")
                .fileName("reviews/uuid2.png")
                .build();

        when(fileStorageService.storeReviewImage(eq(file1), any())).thenReturn(upload1);
        when(fileStorageService.storeReviewImage(eq(file2), any())).thenReturn(upload2);

        ReviewImage savedImg1 = ReviewImage.builder().id(1L).review(review).imageUrl(upload1.getUrl()).imagePath(upload1.getFileName()).build();
        ReviewImage savedImg2 = ReviewImage.builder().id(2L).review(review).imageUrl(upload2.getUrl()).imagePath(upload2.getFileName()).build();
        when(reviewImageRepository.saveAll(anyList())).thenReturn(List.of(savedImg1, savedImg2));

        List<ReviewImageResponse> response = reviewService.uploadReviewImages(100L, files);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getImageUrl()).isEqualTo("/api/v1/uploads/reviews/uuid1.jpg");
        assertThat(response.get(1).getImageUrl()).isEqualTo("/api/v1/uploads/reviews/uuid2.png");
        verify(reviewImageRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Test 6: Chặn IDOR - Người dùng upload ảnh vào review của người khác -> 403 AccessDeniedException")
    void uploadReviewImages_notOwner_throwsAccessDeniedException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Review reviewOfOther = Review.builder()
                .id(100L)
                .user(otherUser) // Chủ sở hữu là user khác
                .product(testProduct)
                .build();

        when(reviewRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(reviewOfOther));

        MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", new byte[]{1, 2});

        assertThatThrownBy(() -> reviewService.uploadReviewImages(100L, List.of(file)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền đăng tải hình ảnh cho đánh giá của người khác");

        verify(fileStorageService, never()).storeReviewImage(any(), any());
    }

    @Test
    @DisplayName("Test 7: Vượt quá giới hạn 3 ảnh trên 1 đánh giá -> 400 BadRequestException")
    void uploadReviewImages_exceedsMaxImages_throwsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Review review = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .build();

        when(reviewRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(100L)).thenReturn(2); // Đã có 2 ảnh

        // Tải thêm 2 ảnh -> tổng 4 ảnh (> 3)
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", new byte[]{2});

        assertThatThrownBy(() -> reviewService.uploadReviewImages(100L, List.of(file1, file2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tối đa 3 ảnh");

        verify(fileStorageService, never()).storeReviewImage(any(), any());
    }

    @Test
    @DisplayName("Test 8: Database lưu ảnh thất bại -> Rollback xóa sạch các file ảnh đã lưu trên đĩa")
    void uploadReviewImages_databaseFailure_cleanupsStoredFiles() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Review review = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .build();

        when(reviewRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.countByReviewId(100L)).thenReturn(0);

        MockMultipartFile file = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[]{1, 2});
        when(fileStorageService.validateReviewImage(file)).thenReturn(new byte[]{1, 2});

        FileUploadResponse upload = FileUploadResponse.builder()
                .url("/api/v1/uploads/reviews/saved.jpg")
                .fileName("reviews/saved.jpg")
                .build();
        when(fileStorageService.storeReviewImage(eq(file), any())).thenReturn(upload);

        // Mô phỏng DB bị sập hoặc vi phạm ràng buộc
        when(reviewImageRepository.saveAll(anyList())).thenThrow(new RuntimeException("Database error!"));

        assertThatThrownBy(() -> reviewService.uploadReviewImages(100L, List.of(file)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error!");

        // Xác nhận đã gọi deleteFile để xóa file rác rollback
        verify(fileStorageService, times(1)).deleteFile("reviews/saved.jpg");
    }

    // =========================================================================
    // TESTS CHO ADMIN REVIEW MANAGEMENT (TSK-D14-CCT)
    // =========================================================================

    @Test
    @DisplayName("Test 9: Admin toggle ẩn review -> isVisible chuyển false và tự động tính lại rating sản phẩm")
    void toggleReviewVisibility_success() {
        Review review = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .rating(5)
                .isVisible(true)
                .build();

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mô phỏng tính lại điểm: sau khi ẩn review 5 sao, điểm trung bình còn 4.0 và 2 review
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(2);

        ReviewResponse response = reviewService.toggleReviewVisibility(100L);

        assertThat(response).isNotNull();
        assertThat(response.getIsVisible()).isFalse();
        assertThat(testProduct.getRatingAverage()).isEqualTo(4.0);
        assertThat(testProduct.getTotalReviews()).isEqualTo(2);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Test 10: Admin xóa vĩnh viễn review -> xóa DB, tính lại rating sản phẩm và dọn file ảnh")
    void deleteReviewByAdmin_success() {
        ReviewImage img1 = ReviewImage.builder().id(1L).imagePath("reviews/img1.jpg").imageUrl("/api/v1/uploads/reviews/img1.jpg").build();
        Review review = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .rating(5)
                .isVisible(true)
                .images(List.of(img1))
                .build();

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        reviewService.deleteReviewByAdmin(100L);

        verify(reviewRepository, times(1)).delete(review);
        verify(productRepository, times(1)).save(testProduct);
        // Xác nhận fileStorageService.deleteFile được gọi để dọn dẹp ảnh
        verify(fileStorageService, times(1)).deleteFile("reviews/img1.jpg");
    }

    @Test
    @DisplayName("Test 11: Admin toggle hoặc xóa review không tồn tại -> 404 ResourceNotFoundException")
    void adminReview_notFound_throwsResourceNotFoundException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.toggleReviewVisibility(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Đánh giá không tồn tại");

        assertThatThrownBy(() -> reviewService.deleteReviewByAdmin(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Đánh giá không tồn tại");
    }

    @Test
    @DisplayName("Test 12: Không cho phép upload ảnh vào review đã bị ẩn -> 400 BadRequestException")
    void uploadReviewImages_hiddenReview_throwsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Review hiddenReview = Review.builder()
                .id(100L)
                .user(testUser)
                .product(testProduct)
                .rating(5)
                .isVisible(false) // Review đã bị Admin ẩn
                .build();

        when(reviewRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(hiddenReview));

        MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", new byte[]{1, 2});

        assertThatThrownBy(() -> reviewService.uploadReviewImages(100L, List.of(file)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể tải lên hình ảnh cho đánh giá đã bị ẩn");

        verify(fileStorageService, never()).storeReviewImage(any(), any());
    }

    @Test
    @DisplayName("Test 13: Lấy danh sách đánh giá cho Admin có phân trang thành công")
    void getAllReviewsForAdmin_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Review r1 = Review.builder()
                .id(1L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Hài lòng")
                .isVisible(true)
                .build();

        Page<Review> page = new PageImpl<>(List.of(r1), pageable, 1);
        when(reviewRepository.findAllForAdmin(eq(true), eq(pageable))).thenReturn(page);

        PageResponse<ReviewResponse> response = reviewService.getAllReviewsForAdmin(true, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(response.getContent().get(0).getProductName()).isEqualTo("Áo thun nam basic Cotton");
    }

    @Test
    @DisplayName("Test 14: checkReviewEligibility trả về canReview = true khi user đã mua sản phẩm (COMPLETED) và chưa đánh giá")
    void checkReviewEligibility_eligible_returnsCanReviewTrue() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        ReviewEligibilityResponse response = reviewService.checkReviewEligibility(10L);

        assertThat(response).isNotNull();
        assertThat(response.isCanReview()).isTrue();
        assertThat(response.isHasPurchased()).isTrue();
        assertThat(response.isAlreadyReviewed()).isFalse();
        assertThat(response.getMyReviewId()).isNull();
        assertThat(response.getMyReview()).isNull();
    }

    @Test
    @DisplayName("Test 15: checkReviewEligibility trả về canReview = false khi user chưa từng mua sản phẩm hoặc đơn hàng chưa giao thành công")
    void checkReviewEligibility_notPurchased_returnsCanReviewFalse() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(false);
        when(reviewRepository.findByProductIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        ReviewEligibilityResponse response = reviewService.checkReviewEligibility(10L);

        assertThat(response).isNotNull();
        assertThat(response.isCanReview()).isFalse();
        assertThat(response.isHasPurchased()).isFalse();
        assertThat(response.isAlreadyReviewed()).isFalse();
        assertThat(response.getReason()).contains("mua và nhận hàng thành công");
    }

    @Test
    @DisplayName("Test 16: checkReviewEligibility trả về alreadyReviewed = true và myReviewId khi user đã từng đánh giá")
    void checkReviewEligibility_alreadyReviewed_returnsCanReviewFalseAndMyReview() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderItemRepository.hasUserPurchasedProductWithStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(true);

        Review existingReview = Review.builder()
                .id(99L)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .comment("Đã đánh giá trước đó")
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(reviewRepository.findByProductIdAndUserId(10L, 1L)).thenReturn(Optional.of(existingReview));

        ReviewEligibilityResponse response = reviewService.checkReviewEligibility(10L);

        assertThat(response).isNotNull();
        assertThat(response.isCanReview()).isFalse();
        assertThat(response.isHasPurchased()).isTrue();
        assertThat(response.isAlreadyReviewed()).isTrue();
        assertThat(response.getMyReviewId()).isEqualTo(99L);
        assertThat(response.getMyReview()).isNotNull();
        assertThat(response.getMyReview().getComment()).isEqualTo("Đã đánh giá trước đó");
    }
}
