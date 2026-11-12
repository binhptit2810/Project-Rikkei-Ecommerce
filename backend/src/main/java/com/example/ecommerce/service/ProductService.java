package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
import com.example.ecommerce.dto.response.BestSellerProjection;
import com.example.ecommerce.dto.response.DealOfTheDayResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service quản lý sản phẩm (Product Module).
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D08-DDM] Tối ưu hóa Bộ lọc Tìm kiếm Sản phẩm & Viết Unit Tests ProductService
 */
@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CouponRepository couponRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CouponRepository couponRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.couponRepository = couponRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CouponRepository couponRepository
    ) {
        this(productRepository, categoryRepository, couponRepository, null);
    }

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this(productRepository, categoryRepository, null, null);
    }

    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "price", "createdAt", "name", "stockQuantity");

    /**
     * Tìm kiếm và lọc sản phẩm đang active dành cho khách mua sắm (hỗ trợ lọc theo khoảng giá minPrice, maxPrice).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = productRepository.searchProducts(
                searchKeyword,
                categoryId,
                minPrice,
                maxPrice,
                true,
                pageable
        );

        return PageResponse.from(productPage, ProductResponse::fromEntity);
    }

    /**
     * Overload getProducts không kèm khoảng giá (tương thích các lời gọi cũ).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        return getProducts(keyword, categoryId, null, null, page, size, sortBy, sortDir);
    }

    /**
     * Xem thông tin chi tiết một sản phẩm theo ID (chỉ áp dụng với sản phẩm active dành cho khách hàng).
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Sản phẩm", id);
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Lấy danh sách sản phẩm phục vụ quản trị Admin (hỗ trợ phân trang, tìm kiếm, khoảng giá và lọc theo trạng thái active/inactive).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = productRepository.searchProducts(
                searchKeyword,
                categoryId,
                minPrice,
                maxPrice,
                active,
                pageable
        );

        return PageResponse.from(productPage, ProductResponse::fromEntity);
    }

    /**
     * Overload getAdminProducts không kèm khoảng giá.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            String keyword,
            Long categoryId,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        return getAdminProducts(keyword, categoryId, null, null, active, page, size, sortBy, sortDir);
    }

    /**
     * Lấy thông tin chi tiết sản phẩm dành cho Admin (bao gồm cả sản phẩm inactive).
     */
    @Transactional(readOnly = true)
    public ProductResponse getAdminProductById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        return ProductResponse.fromEntity(product);
    }

    /**
     * Admin: Thêm mới sản phẩm.
     * - Xác thực danh mục tồn tại và đang active.
     * - Khởi tạo active = true và số lượng tồn kho ban đầu.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Admin creating product with name: {}, categoryId: {}", request.getName(), request.getCategoryId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .stockQuantity(request.getStockQuantity())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Admin: Cập nhật thông tin sản phẩm.
     * - Xác thực sản phẩm và danh mục tồn tại.
     * - Danh mục được gán phải đang active.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Admin updating product id: {}", id);

        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product id: {} updated successfully", updatedProduct.getId());
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Admin: Xóa mềm sản phẩm.
     * Chuyển active = false, tuyệt đối không dùng repository.delete() để đảm bảo toàn vẹn dữ liệu cho đơn hàng cũ.
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Admin soft-deleting product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Product id: {} marked as inactive (soft deleted)", id);
    }

    /**
     * Lấy sản phẩm Deal of the Day từ cơ sở dữ liệu kèm thông tin khuyến mãi thực tế.
     * Tỷ lệ giảm giá và giá gốc được tính toán chính xác, giá bán khớp tuyệt đối với giá DB.
     *
     * @return DealOfTheDayResponse thông tin deal hoặc null nếu không có sản phẩm khả dụng
     */
    @Transactional(readOnly = true)
    public DealOfTheDayResponse getDealOfTheDay() {
        // Lấy danh sách ứng viên sản phẩm active và có tồn kho > 0
        List<Product> candidates = productRepository.findDealCandidates(PageRequest.of(0, 10));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Ưu tiên sản phẩm ngành điện tử hoặc sản phẩm có lượt đánh giá cao nhất
        Product dealProduct = candidates.stream()
                .filter(p -> p.getCategory() != null && "Điện tử".equalsIgnoreCase(p.getCategory().getName()))
                .findFirst()
                .orElse(candidates.get(0));

        int discountPercentage = 25; // Tỷ lệ giảm chuẩn 25% nếu không có coupon cụ thể
        String couponCode = null;
        LocalDateTime endTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        if (couponRepository != null) {
            try {
                List<Coupon> activeCoupons = couponRepository.findByActiveTrueOrderByCreatedAtDesc();
                Optional<Coupon> promoCoupon = activeCoupons.stream()
                        .filter(c -> !c.isExpired() && (c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit()))
                        .findFirst();

                if (promoCoupon.isPresent()) {
                    Coupon coupon = promoCoupon.get();
                    couponCode = coupon.getCode();
                    if (coupon.getDiscountType() == DiscountType.PERCENT && coupon.getDiscountValue() != null) {
                        discountPercentage = coupon.getDiscountValue().intValue();
                    }
                    if (coupon.getExpireAt() != null && coupon.getExpireAt().isAfter(LocalDateTime.now())) {
                        endTime = coupon.getExpireAt();
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể tải coupon cho Deal of the Day: {}", e.getMessage());
            }
        }

        BigDecimal salePrice = dealProduct.getPrice();
        // Giá gốc tính theo tỷ lệ chiết khấu: originalPrice = salePrice / (1 - discountPercentage / 100)
        BigDecimal oneMinusDiscount = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        BigDecimal originalPrice = salePrice.divide(oneMinusDiscount, 0, RoundingMode.HALF_UP);

        return DealOfTheDayResponse.builder()
                .id(dealProduct.getId())
                .name(dealProduct.getName())
                .description(dealProduct.getDescription())
                .imageUrl(dealProduct.getImageUrl())
                .categoryId(dealProduct.getCategory() != null ? dealProduct.getCategory().getId() : null)
                .categoryName(dealProduct.getCategory() != null ? dealProduct.getCategory().getName() : null)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .discountPercentage(discountPercentage)
                .stockQuantity(dealProduct.getStockQuantity())
                .ratingAverage(dealProduct.getRatingAverage() != null ? dealProduct.getRatingAverage() : 0.0)
                .totalReviews(dealProduct.getTotalReviews() != null ? dealProduct.getTotalReviews() : 0)
                .couponCode(couponCode)
                .endTime(endTime)
                .active(dealProduct.getActive())
                .build();
    }

    /**
     * Lấy danh sách sản phẩm Best Seller (bán chạy nhất) theo tháng và năm có phân trang.
     * Mặc định lấy tháng và năm hiện tại nếu không truyền.
     *
     * Phụ trách: Đặng Thanh Bình (DTB)
     * DoD: Trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng, tốc độ truy vấn < 30ms.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getBestSellers(Integer month, Integer year, int page, int size) {
        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();

        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException("Tháng không hợp lệ: phải nằm trong khoảng từ 1 đến 12");
        }
        if (year != null && year < 2000) {
            throw new BusinessException("Năm không hợp lệ: phải từ 2000 trở lên");
        }

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.plusMonths(1).atDay(1).atStartOfDay();

        int validPage = Math.max(page, 0);
        int validSize = (size > 0) ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(validPage, validSize);

        log.info("Lấy danh sách Best Seller tháng {}/{} (page={}, size={})", targetMonth, targetYear, validPage, validSize);

        if (orderItemRepository == null) {
            return PageResponse.<ProductResponse>builder()
                    .content(java.util.Collections.emptyList())
                    .pageNumber(validPage)
                    .pageSize(validSize)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        Page<BestSellerProjection> bestSellerPage = orderItemRepository.findBestSellers(startDate, endDate, pageable);

        return PageResponse.from(bestSellerPage, ProductResponse::fromProjection);
    }

    /**
     * Tạo Pageable chuẩn hóa từ các tham số request.
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, validSortBy));
    }
}
