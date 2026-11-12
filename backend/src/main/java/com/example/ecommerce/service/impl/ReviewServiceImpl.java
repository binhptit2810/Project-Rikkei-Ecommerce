package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
import com.example.ecommerce.dto.response.ReviewImageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.Review;
import com.example.ecommerce.entity.ReviewImage;
import com.example.ecommerce.entity.Role;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ReviewImageRepository;
import com.example.ecommerce.repository.ReviewRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.CustomUserDetails;
import com.example.ecommerce.service.FileStorageService;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Triển khai nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DDM] & [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final FileStorageService fileStorageService;

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

        // [TSK-D14-CCT] Chỉ trả về các đánh giá hiển thị công khai (isVisible = true)
        Page<Review> reviewPage = reviewRepository.findByProductIdAndIsVisibleTrue(productId, sortedPageable);
        return PageResponse.from(reviewPage, ReviewResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

        // Chỉ tính toán dựa trên các review hiển thị (isVisible = true)
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

        // 3. Lưu đánh giá mới vào CSDL (mặc định isVisible = true)
        Review review = Review.builder()
                .product(product)
                .user(currentUser)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .isVisible(true)
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

        // Thu thập đường dẫn ảnh trước khi xóa bản ghi
        List<String> imagePaths = collectReviewImagePaths(review);

        reviewRepository.delete(review);
        reviewRepository.flush();

        // Tính lại rating_average và total_reviews của sản phẩm
        recalculateProductRating(product);

        // Đăng ký hook xóa ảnh sau khi commit transaction thành công
        registerPostCommitImageCleanup(imagePaths);

        log.info("User #{} (role={}) deleted review #{} for product #{}. Cleaned up {} images.",
                currentUser.getId(), currentUser.getRole(), reviewId, productId, imagePaths.size());
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

    @Override
    @Transactional
    public List<ReviewImageResponse> uploadReviewImages(Long reviewId, List<MultipartFile> files) {
        User currentUser = getCurrentUser();

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một tệp tin hình ảnh để tải lên");
        }

        // Khóa bi quan bản ghi Review để bảo đảm không bị race condition vượt quá 3 ảnh
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        // Kiểm tra quyền sở hữu review (Chỉ chính chủ mới được upload ảnh)
        if (!review.getUser().getId().equals(currentUser.getId())) {
            log.warn("User #{} attempted IDOR image upload on review #{} owned by user #{}",
                    currentUser.getId(), reviewId, review.getUser().getId());
            throw new AccessDeniedException("Bạn không có quyền đăng tải hình ảnh cho đánh giá của người khác");
        }

        // Kiểm tra tổng số lượng ảnh hiện có + số lượng ảnh tải lên
        int currentCount = reviewImageRepository.countByReviewId(reviewId);
        if (currentCount + files.size() > 3) {
            throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh. Đánh giá này hiện đã có "
                    + currentCount + " ảnh.");
        }

        // 1. Validation Fail-Fast: Xác thực toàn bộ file trước khi lưu bất kỳ file nào lên đĩa
        List<byte[]> validatedByteList = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            byte[] validatedBytes = fileStorageService.validateReviewImage(file);
            validatedByteList.add(validatedBytes);
        }

        // 2. Lưu file vào Local Storage và theo dõi danh sách để rollback nếu DB fail
        List<FileUploadResponse> uploadedResponses = new ArrayList<>(files.size());
        try {
            for (int i = 0; i < files.size(); i++) {
                FileUploadResponse stored = fileStorageService.storeReviewImage(files.get(i), validatedByteList.get(i));
                uploadedResponses.add(stored);
            }

            List<ReviewImage> imageEntities = new ArrayList<>(uploadedResponses.size());
            for (FileUploadResponse uploaded : uploadedResponses) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(uploaded.getUrl())
                        .imagePath(uploaded.getFileName())
                        .build();
                imageEntities.add(img);
                review.addImage(img);
            }

            List<ReviewImage> savedImages = reviewImageRepository.saveAll(imageEntities);
            reviewRepository.save(review);

            log.info("User #{} uploaded {} images for review #{}: {}",
                    currentUser.getId(), savedImages.size(), reviewId,
                    uploadedResponses.stream().map(FileUploadResponse::getFileName).toList());

            return savedImages.stream()
                    .map(ReviewImageResponse::fromEntity)
                    .toList();

        } catch (Exception e) {
            // Rollback an toàn: Xóa ngay các file vừa lưu vào Local Storage khi DB gặp lỗi
            for (FileUploadResponse uploaded : uploadedResponses) {
                fileStorageService.deleteFile(uploaded.getFileName());
            }
            log.error("Lỗi khi lưu metadata ảnh đánh giá vào CSDL, đã hoàn tác xóa các tệp tin trên đĩa: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public ReviewResponse toggleReviewVisibility(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + id));

        boolean newVisibility = !Boolean.TRUE.equals(review.getIsVisible());
        review.setIsVisible(newVisibility);
        review.setUpdatedAt(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);
        reviewRepository.flush();

        // Bắt buộc tính toán lại điểm rating và số lượng đánh giá công khai của sản phẩm
        recalculateProductRating(review.getProduct());

        String adminIdentity = getAdminIdentifier();
        log.info("Admin [{}] toggled review #{} visibility to {} for product #{}",
                adminIdentity, id, newVisibility, review.getProduct().getId());

        return ReviewResponse.fromEntity(savedReview);
    }

    @Override
    @Transactional
    public void deleteReviewByAdmin(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + id));

        Product product = review.getProduct();
        Long productId = product.getId();

        // Thu thập danh sách đường dẫn ảnh cần xóa trước khi xóa bản ghi
        List<String> imagePathsToDelete = collectReviewImagePaths(review);

        // Xóa review trong database (cascade sẽ xóa review_images)
        reviewRepository.delete(review);
        reviewRepository.flush();

        // Tính lại thống kê sản phẩm
        recalculateProductRating(product);

        // Đăng ký hook xóa file trên Local Storage SAU KHI transaction commit thành công
        registerPostCommitImageCleanup(imagePathsToDelete);

        String adminIdentity = getAdminIdentifier();
        log.info("Admin [{}] deleted review #{} for product #{}. Scheduled cleanup for {} images.",
                adminIdentity, id, productId, imagePathsToDelete.size());
    }

    /**
     * Thu thập danh sách đường dẫn tương đối của các ảnh đính kèm theo review.
     */
    private List<String> collectReviewImagePaths(Review review) {
        List<String> paths = new ArrayList<>();
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            for (ReviewImage img : review.getImages()) {
                if (StringUtils.hasText(img.getImagePath())) {
                    paths.add(img.getImagePath());
                } else if (StringUtils.hasText(img.getImageUrl())) {
                    paths.add(img.getImageUrl());
                }
            }
        } else if (reviewImageRepository != null && review.getId() != null) {
            // Truy vấn CSDL nếu collection lazy chưa nạp
            List<ReviewImage> dbImages = reviewImageRepository.findByReviewIdOrderByCreatedAtAsc(review.getId());
            if (dbImages != null) {
                for (ReviewImage img : dbImages) {
                    if (StringUtils.hasText(img.getImagePath())) {
                        paths.add(img.getImagePath());
                    } else if (StringUtils.hasText(img.getImageUrl())) {
                        paths.add(img.getImageUrl());
                    }
                }
            }
        }
        return paths;
    }

    /**
     * Đăng ký dọn dẹp file Local Storage sau khi giao dịch database commit thành công.
     */
    private void registerPostCommitImageCleanup(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty() || fileStorageService == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String path : imagePaths) {
                        boolean success = fileStorageService.deleteFile(path);
                        if (!success) {
                            log.error("Không thể xóa tệp tin ảnh đánh giá khỏi Local Storage sau commit: {}", path);
                        }
                    }
                }
            });
        } else {
            // Fallback khi chạy ngoài transaction context
            for (String path : imagePaths) {
                fileStorageService.deleteFile(path);
            }
        }
    }

    /**
     * Lấy định danh admin hiện tại để ghi log kiểm toán.
     */
    private String getAdminIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "ADMIN";
    }

    /**
     * Tính toán lại điểm trung bình rating_average và tổng số lượt đánh giá total_reviews
     * (chỉ tính các review có isVisible = true).
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
            throw new BusinessException("Vui lòng đăng nhập để thực hiện thao tác này");
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
