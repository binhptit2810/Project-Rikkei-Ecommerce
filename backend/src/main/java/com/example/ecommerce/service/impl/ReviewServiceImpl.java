package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] & [TSK-D14-DDM] Xây Dựng API Chỉnh Sửa, Xóa Đánh Giá & Admin Phản Hồi Đánh Giá
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId);
        }

        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        Page<Review> reviewPage = reviewRepository.findByProductId(productId, sortedPageable);
        return PageResponse.from(reviewPage, ReviewResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        Double avg = reviewRepository.calculateAverageRating(productId);
        double ratingAverage = (avg != null)
                ? Math.round(avg * 10.0) / 10.0
                : (product.getRatingAverage() != null ? product.getRatingAverage() : 0.0);

        Integer total = reviewRepository.countByProductId(productId);
        int totalReviews = (total != null)
                ? total
                : (product.getTotalReviews() != null ? product.getTotalReviews() : 0);

        Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) {
            ratingCounts.put(star, 0L);
        }

        List<Object[]> grouped = reviewRepository.countRatingsGrouped(productId);
        if (grouped != null) {
            for (Object[] row : grouped) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    int star = ((Number) row[0]).intValue();
                    long count = ((Number) row[1]).longValue();
                    ratingCounts.put(star, count);
                }
            }
        }

        return ProductRatingSummaryResponse.builder()
                .ratingAverage(ratingAverage)
                .totalReviews(totalReviews)
                .ratingCounts(ratingCounts)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long productId, ReviewCreateRequest request) {
        User currentUser = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        // 1. Kiểm tra khách hàng đã mua sản phẩm và đơn hàng ở trạng thái COMPLETED
        boolean hasPurchased = orderItemRepository.hasUserPurchasedProductWithStatus(
                currentUser.getId(), productId, OrderStatus.COMPLETED
        );
        if (!hasPurchased) {
            log.warn("User {} attempted to review unpurchased/incomplete product {}", currentUser.getId(), productId);
            throw new BadRequestException("Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công");
        }

        // 2. Kiểm tra nếu đã đánh giá rồi (mỗi user chỉ 1 đánh giá cho mỗi sản phẩm)
        boolean alreadyReviewed = reviewRepository.existsByProductIdAndUserId(productId, currentUser.getId());
        if (alreadyReviewed) {
            log.warn("User {} duplicate review attempt for product {}", currentUser.getId(), productId);
            throw new DuplicateResourceException("Bạn đã đánh giá sản phẩm này rồi");
        }

        // 3. Lưu đánh giá mới vào CSDL
        Review review = Review.builder()
                .product(product)
                .user(currentUser)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .build();
        Review savedReview = reviewRepository.save(review);
        reviewRepository.flush();

        // 4. Tự động tính toán lại ratingAverage và totalReviews, cập nhật vào Product
        recalculateProductRating(product);

        log.info("Created review #{} for product #{}: rating={}, newAvg={}, totalReviews={}",
                savedReview.getId(), productId, request.getRating(), product.getRatingAverage(), product.getTotalReviews());

        return ReviewResponse.fromEntity(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long productId, Long reviewId, ReviewRequest request) {
        User currentUser = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        if (!review.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Đánh giá #" + reviewId + " không thuộc về sản phẩm #" + productId);
        }

        // Chỉ đúng user đã tạo đánh giá này mới được sửa
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Số sao đánh giá phải từ 1 đến 5");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new BadRequestException("Nội dung đánh giá không được để trống");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        review.setUpdatedAt(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);
        reviewRepository.flush();

        // Bắt buộc tính lại rating_average của sản phẩm
        recalculateProductRating(product);

        log.info("User #{} updated review #{} for product #{}: newRating={}",
                currentUser.getId(), reviewId, productId, request.getRating());

        return ReviewResponse.fromEntity(savedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long productId, Long reviewId) {
        User currentUser = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        if (!review.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Đánh giá #" + reviewId + " không thuộc về sản phẩm #" + productId);
        }

        // Cho phép chính chủ tạo review HOẶC người dùng có role ADMIN thực hiện xóa
        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);
        reviewRepository.flush();

        // Tính lại rating_average và total_reviews của sản phẩm
        recalculateProductRating(product);

        log.info("User #{} (role={}) deleted review #{} for product #{}",
                currentUser.getId(), currentUser.getRole(), reviewId, productId);
    }

    @Override
    @Transactional
    public ReviewResponse replyReview(Long reviewId, AdminReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        if (request == null || request.getReply() == null || request.getReply().trim().isEmpty()) {
            throw new BadRequestException("Nội dung phản hồi không được để trống");
        }

        review.setAdminReply(request.getReply().trim());
        review.setAdminReplyAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        log.info("Admin replied to review #{}: {}", reviewId, request.getReply());
        return ReviewResponse.fromEntity(saved);
    }

    /**
     * Tính toán lại điểm trung bình rating_average và tổng số lượt đánh giá total_reviews.
     */
    private void recalculateProductRating(Product product) {
        Long productId = product.getId();
        Double avg = reviewRepository.calculateAverageRating(productId);
        Integer count = reviewRepository.countByProductId(productId);
        int total = (count != null) ? count : 0;
        double newAverage = (total > 0 && avg != null)
                ? Math.round(avg * 10.0) / 10.0
                : 0.0;

        product.setRatingAverage(newAverage);
        product.setTotalReviews(total);
        productRepository.save(product);
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập từ SecurityContext.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("Vui lòng đăng nhập để thực hiện đánh giá sản phẩm");
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

