package com.example.ecommerce.service.impl;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.request.ReviewRequest;
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
import java.util.Optional;

/**
 * Triển khai nghiệp vụ Đánh giá & Nhận xét sản phẩm.
 * Phụ trách: Đỗ Đức Mạnh (DDM), Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D11-DDM], [TSK-D14-DHA] & [TSK-D14-CCT]
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

        // Chỉ trả về các đánh giá hiển thị công khai (isVisible = true)
        Page<Review> reviewPage = reviewRepository.findByProductIdAndIsVisibleTrue(productId, sortedPageable);
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
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkReviewEligibility(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .hasPurchased(false)
                    .alreadyReviewed(false)
                    .reason("Vui lòng đăng nhập để đánh giá sản phẩm.")
                    .build();
        }

        User currentUser = getCurrentUser();

        // 1. Kiểm tra khách hàng đã mua sản phẩm và đơn hàng ở trạng thái COMPLETED
        boolean hasPurchased = orderItemRepository.hasUserPurchasedProductWithStatus(
                currentUser.getId(), productId, OrderStatus.COMPLETED
        );

        // 2. Tìm bản ghi đánh giá của người dùng đối với sản phẩm này nếu có
        Optional<Review> myReviewOpt = reviewRepository.findByProductIdAndUserId(productId, currentUser.getId());
        boolean alreadyReviewed = myReviewOpt.isPresent();

        // Chỉ được đánh giá mới khi: đã mua và giao thành công + chưa từng đánh giá
        boolean canReview = hasPurchased && !alreadyReviewed;

        String reason;
        if (!hasPurchased) {
            reason = "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công.";
        } else if (alreadyReviewed) {
            reason = "Bạn đã gửi đánh giá cho sản phẩm này rồi.";
        } else {
            reason = "Bạn đủ điều kiện để đánh giá sản phẩm này.";
        }

        ReviewResponse myReviewResponse = myReviewOpt.map(ReviewResponse::fromEntity).orElse(null);
        Long myReviewId = myReviewOpt.map(Review::getId).orElse(null);

        return ReviewEligibilityResponse.builder()
                .canReview(canReview)
                .hasPurchased(hasPurchased)
                .alreadyReviewed(alreadyReviewed)
                .reason(reason)
                .myReviewId(myReviewId)
                .myReview(myReviewResponse)
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

        // 3. Lưu đánh giá mới vào CSDL (mặc định isVisible = true, isHidden = false)
        Review review = Review.builder()
                .product(product)
                .user(currentUser)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .isVisible(true)
                .isHidden(false)
                .images(new ArrayList<>())
                .build();

        // 4. Nếu có danh sách ảnh đính kèm ban đầu
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            if (request.getImages().size() > 3) {
                throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh");
            }
            for (String url : request.getImages()) {
                if (url != null && !url.trim().isEmpty()) {
                    ReviewImage ri = ReviewImage.builder()
                            .review(review)
                            .imageUrl(url.trim())
                            .imagePath(url.trim())
                            .build();
                    review.addImage(ri);
                }
            }
        }

        Review savedReview = reviewRepository.save(review);

        // 5. Tính toán lại rating_average và total_reviews của sản phẩm
        recalculateProductRating(product);

        log.info("User {} created review #{} for product {} with rating {}",
                currentUser.getId(), savedReview.getId(), productId, savedReview.getRating());

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
            throw new BadRequestException("Đánh giá #" + reviewId + " không thuộc về sản phẩm #" + productId);
        }

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : "");

        if (request.getImages() != null) {
            if (request.getImages().size() > 3) {
                throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh");
            }

            List<String> requestedUrls = request.getImages().stream()
                    .filter(u -> u != null && !u.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .toList();

            List<ReviewImage> currentImages = review.getImages() != null
                    ? new ArrayList<>(review.getImages())
                    : new ArrayList<>();

            List<ReviewImage> toRemove = new ArrayList<>();
            List<String> toRemovePaths = new ArrayList<>();
            java.util.Set<String> keptUrls = new java.util.HashSet<>();

            for (ReviewImage currentImg : currentImages) {
                if (requestedUrls.contains(currentImg.getImageUrl())) {
                    keptUrls.add(currentImg.getImageUrl());
                } else {
                    toRemove.add(currentImg);
                    if (StringUtils.hasText(currentImg.getImagePath())) {
                        toRemovePaths.add(currentImg.getImagePath());
                    } else if (StringUtils.hasText(currentImg.getImageUrl())) {
                        toRemovePaths.add(currentImg.getImageUrl());
                    }
                }
            }

            // Xóa các ảnh cũ không còn được giữ lại khỏi quan hệ
            for (ReviewImage img : toRemove) {
                review.removeImage(img);
            }

            // Đăng ký dọn dẹp các tệp ảnh vật lý bị xóa sau khi transaction commit
            if (!toRemovePaths.isEmpty()) {
                registerPostCommitImageCleanup(toRemovePaths);
            }

            // Thêm các ảnh mới (chưa có trong review hiện tại)
            for (String url : requestedUrls) {
                if (!keptUrls.contains(url)) {
                    String path = url;
                    if (url.startsWith("/api/v1/uploads/")) {
                        path = url.substring("/api/v1/uploads/".length());
                    }
                    ReviewImage newImg = ReviewImage.builder()
                            .review(review)
                            .imageUrl(url)
                            .imagePath(path)
                            .build();
                    review.addImage(newImg);
                }
            }
        }

        Review updatedReview = reviewRepository.save(review);
        recalculateProductRating(product);

        log.info("User #{} updated review #{} for product #{}: new rating={}",
                currentUser.getId(), reviewId, productId, updatedReview.getRating());

        return ReviewResponse.fromEntity(updatedReview);
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
            throw new BadRequestException("Đánh giá #" + reviewId + " không thuộc về sản phẩm #" + productId);
        }

        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

        List<String> imagePaths = collectReviewImagePaths(review);

        reviewRepository.delete(review);
        reviewRepository.flush();

        registerPostCommitImageCleanup(imagePaths);
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

    @Override
    @Transactional
    public List<ReviewImageResponse> uploadReviewImages(Long reviewId, List<MultipartFile> files) {
        User currentUser = getCurrentUser();

        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        if (Boolean.FALSE.equals(review.getIsVisible()) || Boolean.TRUE.equals(review.getIsHidden())) {
            throw new BadRequestException("Không thể tải lên hình ảnh cho đánh giá đã bị ẩn");
        }

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền đăng tải hình ảnh cho đánh giá của người khác");
        }

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất 1 tệp hình ảnh để tải lên");
        }

        int currentImagesCount = reviewImageRepository.countByReviewId(reviewId);
        int newImagesCount = files.size();

        if (currentImagesCount + newImagesCount > 3) {
            throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh. Bạn đã có "
                    + currentImagesCount + " ảnh, không thể thêm " + newImagesCount + " ảnh nữa.");
        }

        List<byte[]> validatedBytesList = new ArrayList<>();
        for (MultipartFile file : files) {
            byte[] fileBytes = fileStorageService.validateReviewImage(file);
            validatedBytesList.add(fileBytes);
        }

        List<FileUploadResponse> uploadedFiles = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                byte[] bytes = validatedBytesList.get(i);
                FileUploadResponse uploadRes = fileStorageService.storeReviewImage(file, bytes);
                uploadedFiles.add(uploadRes);
            }

            List<ReviewImage> reviewImagesToSave = new ArrayList<>();
            for (FileUploadResponse upload : uploadedFiles) {
                ReviewImage reviewImage = ReviewImage.builder()
                        .review(review)
                        .imageUrl(upload.getUrl())
                        .imagePath(upload.getFileName())
                        .build();
                reviewImagesToSave.add(reviewImage);
            }

            List<ReviewImage> savedImages = reviewImageRepository.saveAll(reviewImagesToSave);
            for (ReviewImage img : savedImages) {
                review.addImage(img);
            }

            log.info("User {} uploaded {} images for review #{}", currentUser.getId(), savedImages.size(), reviewId);

            return savedImages.stream()
                    .map(ReviewImageResponse::fromEntity)
                    .toList();

        } catch (Exception ex) {
            log.error("Lỗi khi lưu ảnh đánh giá vào CSDL, tiến hành dọn sạch {} tệp vừa tải lên...", uploadedFiles.size(), ex);
            for (FileUploadResponse upload : uploadedFiles) {
                try {
                    fileStorageService.deleteFile(upload.getFileName());
                } catch (Exception delEx) {
                    log.warn("Không thể dọn dẹp tệp ảnh mồ côi: {}", upload.getFileName(), delEx);
                }
            }
            if (ex instanceof RuntimeException re) {
                throw re;
            }
            throw new BusinessException("Lỗi trong quá trình lưu trữ hình ảnh đánh giá: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviewsForAdmin(Pageable pageable, Long productId, Integer rating, Boolean isHidden) {
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        Page<Review> reviewPage = reviewRepository.findAllForAdmin(productId, rating, isHidden, sortedPageable);
        return PageResponse.from(reviewPage, ReviewResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllReviewsForAdmin(Boolean isVisible, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findAllForAdmin(isVisible, pageable);
        return PageResponse.from(reviewPage, ReviewResponse::fromEntity);
    }

    @Override
    @Transactional
    public ReviewResponse toggleReviewVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));

        boolean currentVisible = Boolean.TRUE.equals(review.getIsVisible());
        boolean nextVisible = !currentVisible;
        review.setIsVisible(nextVisible);
        review.setIsHidden(!nextVisible);

        Review saved = reviewRepository.save(review);

        if (saved.getProduct() != null) {
            recalculateProductRating(saved.getProduct());
        }

        log.info("Admin {} toggled review #{} visibility: isVisible={}, isHidden={}",
                getAdminIdentifier(), reviewId, saved.getIsVisible(), saved.getIsHidden());

        return ReviewResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá không tồn tại với ID: " + reviewId));
        Product product = review.getProduct();

        List<String> imagePaths = collectReviewImagePaths(review);

        reviewRepository.delete(review);
        reviewRepository.flush();

        registerPostCommitImageCleanup(imagePaths);

        if (product != null) {
            recalculateProductRating(product);
        }

        log.info("Admin {} permanently deleted review #{} and scheduled cleanup for {} images",
                getAdminIdentifier(), reviewId, imagePaths.size());
    }

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
            for (String path : imagePaths) {
                fileStorageService.deleteFile(path);
            }
        }
    }

    private String getAdminIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "ADMIN";
    }

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
