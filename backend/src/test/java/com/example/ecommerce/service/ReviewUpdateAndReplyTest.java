package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.ReviewImage;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test bao phủ các kịch bản Chỉnh sửa, Xóa Đánh giá và Admin Phản hồi Đánh giá.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D14-DDM] Xây Dựng API Chỉnh Sửa, Xóa Đánh Giá & Admin Phản Hồi Đánh Giá
 */
@ExtendWith(MockitoExtension.class)
class ReviewUpdateAndReplyTest {

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

    private User ownerUser;
    private User otherUser;
    private User adminUser;
    private Product testProduct;
    private Review testReview;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder()
                .id(1L)
                .email("owner@example.com")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Trần Thị B")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(99L)
                .email("admin@eshop.com")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .active(true)
                .build();

        testProduct = Product.builder()
                .id(10L)
                .name("Áo thun nam basic Cotton")
                .price(new BigDecimal("199000"))
                .ratingAverage(5.0)
                .totalReviews(1)
                .active(true)
                .build();

        testReview = Review.builder()
                .id(100L)
                .product(testProduct)
                .user(ownerUser)
                .rating(5)
                .comment("Sản phẩm rất đẹp và chất lượng")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "password",
                user.getRole(),
                user.getActive() != null ? user.getActive() : true,
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Kịch bản 1: Chính chủ sửa review thành công -> rating_average của Product được tính lại đúng")
    void updateReview_byOwner_success_recalculatesRating() {
        authenticateAs(ownerUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock tính toán lại rating: điểm mới là 4 sao
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        ReviewRequest request = ReviewRequest.builder()
                .rating(4)
                .comment("Đã dùng 1 tuần, vải hơi mỏng hơn kỳ vọng nhưng vẫn ổn")
                .build();

        ReviewResponse response = reviewService.updateReview(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getComment()).isEqualTo("Đã dùng 1 tuần, vải hơi mỏng hơn kỳ vọng nhưng vẫn ổn");
        assertThat(testProduct.getRatingAverage()).isEqualTo(4.0);
        assertThat(testProduct.getTotalReviews()).isEqualTo(1);

        verify(reviewRepository, times(1)).save(testReview);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Kịch bản 2: User khác cố tình gọi API sửa review của người khác -> ném lỗi AccessDeniedException")
    void updateReview_byAnotherUser_throwsAccessDeniedException() {
        authenticateAs(otherUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));

        ReviewRequest request = ReviewRequest.builder()
                .rating(1)
                .comment("Cố tình phá hoại review")
                .build();

        assertThatThrownBy(() -> reviewService.updateReview(10L, 100L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền chỉnh sửa đánh giá này");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Kịch bản 3: Xóa review thành công -> total_reviews giảm đi 1 và rating_average cập nhật đúng (về 0.0 nếu hết review)")
    void deleteReview_byOwnerOrAdmin_success_decrementsTotalAndUpdatesRating() {
        authenticateAs(ownerUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));

        // Sau khi xóa đánh giá duy nhất: không còn review nào
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(null);
        when(reviewRepository.countByProductId(10L)).thenReturn(0);

        reviewService.deleteReview(10L, 100L);

        verify(reviewRepository, times(1)).delete(testReview);
        verify(reviewRepository, times(1)).flush();

        // Product phải được cập nhật lại: totalReviews = 0, ratingAverage = 0.0
        assertThat(testProduct.getTotalReviews()).isEqualTo(0);
        assertThat(testProduct.getRatingAverage()).isEqualTo(0.0);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("Kịch bản 4: Admin gửi phản hồi cho review thành công -> lưu đúng adminReply và adminReplyAt")
    void replyReview_byAdmin_success_savesAdminReplyAndTimestamp() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReplyRequest request = AdminReplyRequest.builder()
                .reply("Cảm ơn bạn đã ủng hộ shop! Chúc bạn có trải nghiệm tuyệt vời với sản phẩm ạ.")
                .build();

        ReviewResponse response = reviewService.replyReview(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAdminReply()).isEqualTo("Cảm ơn bạn đã ủng hộ shop! Chúc bạn có trải nghiệm tuyệt vời với sản phẩm ạ.");
        assertThat(response.getAdminReplyAt()).isNotNull();

        assertThat(testReview.getAdminReply()).isEqualTo("Cảm ơn bạn đã ủng hộ shop! Chúc bạn có trải nghiệm tuyệt vời với sản phẩm ạ.");
        assertThat(testReview.getAdminReplyAt()).isNotNull();
        verify(reviewRepository, times(1)).save(testReview);
    }

    @Test
    @DisplayName("Kịch bản 5: Chỉnh sửa review - Giữ nguyên toàn bộ ảnh cũ")
    void updateReview_keepExistingImages_retainsImagesWithoutDeletion() {
        authenticateAs(ownerUser);

        ReviewImage img1 = ReviewImage.builder()
                .id(1001L)
                .imageUrl("/api/v1/uploads/reviews/img1.jpg")
                .imagePath("reviews/img1.jpg")
                .build();
        ReviewImage img2 = ReviewImage.builder()
                .id(1002L)
                .imageUrl("/api/v1/uploads/reviews/img2.jpg")
                .imagePath("reviews/img2.jpg")
                .build();
        testReview.addImage(img1);
        testReview.addImage(img2);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(5.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Nội dung cập nhật giữ nguyên ảnh")
                .images(List.of("/api/v1/uploads/reviews/img1.jpg", "/api/v1/uploads/reviews/img2.jpg"))
                .build();

        ReviewResponse response = reviewService.updateReview(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getImages()).containsExactly("/api/v1/uploads/reviews/img1.jpg", "/api/v1/uploads/reviews/img2.jpg");
        assertThat(testReview.getImages()).hasSize(2);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("Kịch bản 6: Chỉnh sửa review - Xóa 1 ảnh cũ và giữ lại 1 ảnh cũ, kích hoạt dọn dẹp file vật lý")
    void updateReview_removeOneOldImage_removesImageAndCleansUpFile() {
        authenticateAs(ownerUser);

        ReviewImage img1 = ReviewImage.builder()
                .id(1001L)
                .imageUrl("/api/v1/uploads/reviews/img1.jpg")
                .imagePath("reviews/img1.jpg")
                .build();
        ReviewImage img2 = ReviewImage.builder()
                .id(1002L)
                .imageUrl("/api/v1/uploads/reviews/img2.jpg")
                .imagePath("reviews/img2.jpg")
                .build();
        testReview.addImage(img1);
        testReview.addImage(img2);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(5.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Đã xóa bớt 1 ảnh cũ")
                .images(List.of("/api/v1/uploads/reviews/img1.jpg"))
                .build();

        ReviewResponse response = reviewService.updateReview(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getImages()).containsExactly("/api/v1/uploads/reviews/img1.jpg");
        assertThat(testReview.getImages()).hasSize(1);
        assertThat(testReview.getImages().get(0).getId()).isEqualTo(1001L);
        verify(fileStorageService, times(1)).deleteFile("reviews/img2.jpg");
    }

    @Test
    @DisplayName("Kịch bản 7: Chỉnh sửa review - Kết hợp giữ ảnh cũ và thêm ảnh mới tải lên")
    void updateReview_addNewImageWhileKeepingOld_addsNewImageSuccessfully() {
        authenticateAs(ownerUser);

        ReviewImage img1 = ReviewImage.builder()
                .id(1001L)
                .imageUrl("/api/v1/uploads/reviews/img1.jpg")
                .imagePath("reviews/img1.jpg")
                .build();
        testReview.addImage(img1);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(5.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Thêm ảnh mới kết hợp ảnh cũ")
                .images(List.of("/api/v1/uploads/reviews/img1.jpg", "/api/v1/uploads/reviews/new_photo.jpg"))
                .build();

        ReviewResponse response = reviewService.updateReview(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getImages()).containsExactly("/api/v1/uploads/reviews/img1.jpg", "/api/v1/uploads/reviews/new_photo.jpg");
        assertThat(testReview.getImages()).hasSize(2);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("Kịch bản 8: Chỉnh sửa review - Xóa toàn bộ ảnh đính kèm (gửi mảng rỗng)")
    void updateReview_removeAllImages_removesAllAndCleansUpAllFiles() {
        authenticateAs(ownerUser);

        ReviewImage img1 = ReviewImage.builder()
                .id(1001L)
                .imageUrl("/api/v1/uploads/reviews/img1.jpg")
                .imagePath("reviews/img1.jpg")
                .build();
        ReviewImage img2 = ReviewImage.builder()
                .id(1002L)
                .imageUrl("/api/v1/uploads/reviews/img2.jpg")
                .imagePath("reviews/img2.jpg")
                .build();
        testReview.addImage(img1);
        testReview.addImage(img2);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.calculateAverageRating(10L)).thenReturn(5.0);
        when(reviewRepository.countByProductId(10L)).thenReturn(1);

        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Không còn dùng ảnh nào")
                .images(Collections.emptyList())
                .build();

        ReviewResponse response = reviewService.updateReview(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getImages()).isEmpty();
        assertThat(testReview.getImages()).isEmpty();
        verify(fileStorageService, times(1)).deleteFile("reviews/img1.jpg");
        verify(fileStorageService, times(1)).deleteFile("reviews/img2.jpg");
    }

    @Test
    @DisplayName("Kịch bản 9: Chỉnh sửa review - Gửi vượt quá 3 ảnh -> Ném lỗi BadRequestException")
    void updateReview_moreThanThreeImages_throwsBadRequestException() {
        authenticateAs(ownerUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));

        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("Cố tình gửi 4 ảnh")
                .images(List.of("url1.jpg", "url2.jpg", "url3.jpg", "url4.jpg"))
                .build();

        assertThatThrownBy(() -> reviewService.updateReview(10L, 100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh");

        verify(reviewRepository, never()).save(any(Review.class));
        verify(fileStorageService, never()).deleteFile(anyString());
    }
}
