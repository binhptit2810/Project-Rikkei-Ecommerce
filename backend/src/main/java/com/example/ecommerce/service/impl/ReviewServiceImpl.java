package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
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

        // 4. Tự động tính toán lại ratingAverage và totalReviews, cập nhật vào Product
        Double avg = reviewRepository.calculateAverageRating(productId);
        double newAverage = (avg != null)
                ? Math.round(avg * 10.0) / 10.0
                : (double) request.getRating();

        Integer count = reviewRepository.countByProductId(productId);
        int newTotal = (count != null) ? count : 1;

        product.setRatingAverage(newAverage);
        product.setTotalReviews(newTotal);
        productRepository.save(product);

        log.info("Created review #{} for product #{}: rating={}, newAvg={}, totalReviews={}",
                savedReview.getId(), productId, request.getRating(), newAverage, newTotal);

        return ReviewResponse.fromEntity(savedReview);
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

