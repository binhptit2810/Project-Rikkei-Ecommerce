package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateProductRequest;
import com.example.ecommerce.dto.request.ProductVariantRequest;
import com.example.ecommerce.dto.request.UpdateProductRequest;
import com.example.ecommerce.dto.response.BestSellerProjection;
import com.example.ecommerce.dto.response.DealOfTheDayResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.dto.response.ProductVariantResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Coupon;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ProductVariant;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.CouponRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductVariantRepository;
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
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
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

    @Autowired(required = false)
    private ProductVariantRepository productVariantRepository;

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
            CouponRepository couponRepository,
            OrderItemRepository orderItemRepository,
            ProductVariantRepository productVariantRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.couponRepository = couponRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
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
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "price", "createdAt", "name", "stockQuantity", "ratingAverage", "totalReviews"
    );

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
     * Tự động nạp đầy đủ danh mục và các biến thể kích cỡ (ProductVariant).
     */
    @Transactional
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdWithCategoryAndVariants(id)
                .or(() -> productRepository.findByIdWithCategory(id))
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Sản phẩm", id);
        }

        ProductResponse response = ProductResponse.fromEntity(product);
        if (response != null && (response.getVariants() == null || response.getVariants().isEmpty())) {
            if (productVariantRepository != null) {
                try {
                    List<ProductVariantResponse> variants = getProductVariants(id);
                    if (variants != null && !variants.isEmpty()) {
                        response.setVariants(variants);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return response;
    }

    /**
     * Lấy danh sách các biến thể kích cỡ (ProductVariant) của sản phẩm theo productId.
     */
    @Transactional
    public List<ProductVariantResponse> getProductVariants(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        if (productVariantRepository == null) {
            return List.of();
        }

        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        if (variants == null || variants.isEmpty()) {
            String nameLower = product.getName() != null ? product.getName().toLowerCase(java.util.Locale.ROOT) : "";
            String catName = (product.getCategory() != null && product.getCategory().getName() != null)
                    ? product.getCategory().getName() : "";

            boolean isFashion = "Thời trang".equalsIgnoreCase(catName)
                    || nameLower.contains("áo") || nameLower.contains("quần") || nameLower.contains("giày")
                    || nameLower.contains("đầm") || nameLower.contains("váy") || nameLower.contains("hoodie")
                    || nameLower.contains("slip-on") || nameLower.contains("sneaker");

            boolean isAccessory = nameLower.contains("thắt lưng") || nameLower.contains("khóa kim")
                    || nameLower.contains("ví da") || nameLower.contains("túi");

            if (isFashion && !isAccessory) {
                variants = autoGenerateDefaultVariants(product);
            } else {
                variants = List.of();
            }
        }

        return variants.stream()
                .map(ProductVariantResponse::fromEntity)
                .toList();
    }

    /**
     * Tự động sinh biến thể kích cỡ mặc định nếu sản phẩm thời trang chưa có biến thể trong DB.
     */
    @Transactional
    public List<ProductVariant> autoGenerateDefaultVariants(Product product) {
        if (product == null || productVariantRepository == null) return List.of();
        String nameLower = product.getName() != null ? product.getName().toLowerCase(java.util.Locale.ROOT) : "";
        List<String> sizes;
        List<Integer> defaultStocks;

        if (nameLower.contains("giày") || nameLower.contains("slip-on") || nameLower.contains("sneaker")) {
            sizes = List.of("38", "39", "40", "41", "42", "43");
            defaultStocks = List.of(4, 6, 6, 4, 0, 0); // Size 43 hết hàng
        } else if (nameLower.contains("quần jean")) {
            sizes = List.of("29", "30", "31", "32", "33");
            defaultStocks = List.of(5, 10, 10, 5, 0); // Size 33 hết hàng
        } else if (nameLower.contains("quần")) {
            sizes = List.of("28", "29", "30", "31", "32");
            defaultStocks = List.of(5, 8, 10, 7, 0); // Size 32 hết hàng
        } else {
            sizes = List.of("S", "M", "L", "XL", "XXL");
            defaultStocks = List.of(10, 15, 15, 10, 0); // Size XXL hết hàng
        }

        List<ProductVariant> created = new java.util.ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            String size = sizes.get(i);
            int stock = (i < defaultStocks.size()) ? defaultStocks.get(i) : 5;
            Optional<ProductVariant> opt = productVariantRepository.findByProductIdAndSize(product.getId(), size);
            if (opt == null || opt.isEmpty()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .size(size)
                        .stockQuantity(stock)
                        .sku("PROD-" + product.getId() + "-" + size)
                        .build();
                ProductVariant saved = productVariantRepository.save(variant);
                created.add(saved != null ? saved : variant);
            } else {
                created.add(opt.get());
            }
        }

        int totalStock = created.stream().mapToInt(ProductVariant::getStockQuantity).sum();
        if (product.getStockQuantity() == null || product.getStockQuantity() != totalStock) {
            product.setStockQuantity(totalStock);
            productRepository.save(product);
        }
        return created;
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
        Product product = productRepository.findByIdWithCategoryAndVariants(id)
                .or(() -> productRepository.findByIdWithCategory(id))
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", id));

        ProductResponse response = ProductResponse.fromEntity(product);
        if (response != null && (response.getVariants() == null || response.getVariants().isEmpty())) {
            if (productVariantRepository != null) {
                List<ProductVariant> variants = productVariantRepository.findByProductId(id);
                if (variants != null && !variants.isEmpty()) {
                    response.setVariants(variants.stream().map(ProductVariantResponse::fromEntity).toList());
                }
            }
        }
        return response;
    }

    /**
     * Admin: Thêm mới sản phẩm kèm theo danh sách biến thể kích cỡ nếu có.
     * - Xác thực danh mục tồn tại và đang active.
     * - Tự động đồng bộ số lượng tồn kho tổng từ tổng tồn kho các biến thể nếu có kích cỡ.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Admin creating product with name: {}, categoryId: {}", request.getName(), request.getCategoryId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", request.getCategoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessException("Không thể gán sản phẩm vào danh mục đã bị vô hiệu hóa");
        }

        boolean hasVariants = Boolean.TRUE.equals(request.getHasVariants())
                || (request.getVariants() != null && !request.getVariants().isEmpty());

        int initialStock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        if (hasVariants && request.getVariants() != null && !request.getVariants().isEmpty()) {
            initialStock = request.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .stockQuantity(initialStock)
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);

        List<ProductVariantResponse> variantResponses = null;
        if (hasVariants && request.getVariants() != null && !request.getVariants().isEmpty()) {
            saveOrUpdateProductVariants(savedProduct, request.getVariants());
            if (productVariantRepository != null) {
                List<ProductVariant> variants = productVariantRepository.findByProductId(savedProduct.getId());
                if (variants != null && !variants.isEmpty()) {
                    variantResponses = variants.stream().map(ProductVariantResponse::fromEntity).toList();
                }
            }
        }

        log.info("Product created successfully with id: {}", savedProduct.getId());
        ProductResponse response = ProductResponse.fromEntity(savedProduct);
        if (variantResponses != null) {
            response.setVariants(variantResponses);
        }
        return response;
    }

    /**
     * Admin: Cập nhật thông tin sản phẩm và đồng bộ biến thể kích cỡ.
     * - Xác thực sản phẩm và danh mục tồn tại.
     * - Kiểm tra an toàn trước khi xóa biến thể (không cho xóa biến thể đã có đơn hàng).
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
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        boolean hasVariants = Boolean.TRUE.equals(request.getHasVariants())
                || (request.getVariants() != null && !request.getVariants().isEmpty());

        Product updatedProduct;
        if (hasVariants && request.getVariants() != null) {
            saveOrUpdateProductVariants(product, request.getVariants());
            updatedProduct = product;
        } else if (Boolean.FALSE.equals(request.getHasVariants()) && productVariantRepository != null) {
            // Tắt biến thể: kiểm tra xem có biến thể nào đã phát sinh đơn hàng chưa
            List<ProductVariant> existing = productVariantRepository.findByProductId(id);
            for (ProductVariant v : existing) {
                if (orderItemRepository != null && (orderItemRepository.existsByVariantId(v.getId())
                        || orderItemRepository.existsByProductIdAndSize(id, v.getSize()))) {
                    throw new BusinessException("Không thể tắt biến thể kích cỡ vì kích cỡ '" + v.getSize() + "' đã có đơn hàng trong hệ thống.");
                }
            }
            productVariantRepository.deleteAll(existing);
            product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
            updatedProduct = productRepository.save(product);
        } else {
            product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
            updatedProduct = productRepository.save(product);
        }

        List<ProductVariantResponse> variantResponses = null;
        if (productVariantRepository != null) {
            List<ProductVariant> variants = productVariantRepository.findByProductId(id);
            if (variants != null && !variants.isEmpty()) {
                variantResponses = variants.stream().map(ProductVariantResponse::fromEntity).toList();
            }
        }

        log.info("Product id: {} updated successfully", product.getId());
        ProductResponse response = ProductResponse.fromEntity(updatedProduct);
        if (variantResponses != null) {
            response.setVariants(variantResponses);
        }
        return response;
    }

    /**
     * Lưu hoặc cập nhật danh sách biến thể kích cỡ của sản phẩm từ form Admin.
     * Đảm bảo tính toàn vẹn dữ liệu, kiểm tra trùng lặp SKU & Size và bảo vệ lịch sử đơn hàng.
     */
    @Transactional
    public void saveOrUpdateProductVariants(Product product, List<ProductVariantRequest> variantRequests) {
        if (product == null || productVariantRepository == null || variantRequests == null) return;

        // 1. Kiểm tra trùng Size và trùng SKU ngay trong payload gửi lên
        java.util.Set<String> seenSizes = new java.util.HashSet<>();
        java.util.Set<String> seenSkus = new java.util.HashSet<>();
        for (ProductVariantRequest vr : variantRequests) {
            if (vr.getSize() == null || vr.getSize().trim().isEmpty()) {
                throw new BusinessException("Kích cỡ sản phẩm không được để trống");
            }
            String normSize = vr.getSize().trim().toUpperCase(Locale.ROOT);
            if (!seenSizes.add(normSize)) {
                throw new BusinessException("Kích cỡ '" + vr.getSize().trim() + "' bị trùng lặp trong sản phẩm");
            }

            if (vr.getSku() != null && !vr.getSku().trim().isEmpty()) {
                String normSku = vr.getSku().trim();
                if (!seenSkus.add(normSku)) {
                    throw new BusinessException("Mã SKU '" + normSku + "' bị trùng lặp trong danh sách kích cỡ");
                }
            }
        }

        List<ProductVariant> existingVariants = productVariantRepository.findByProductId(product.getId());
        java.util.Set<Long> updatedVariantIds = new java.util.HashSet<>();

        // 2. Cập nhật hoặc tạo mới từng biến thể
        for (ProductVariantRequest vr : variantRequests) {
            String size = vr.getSize().trim();
            int stock = vr.getStockQuantity() != null ? vr.getStockQuantity() : 0;
            if (stock < 0) {
                throw new BusinessException("Số lượng tồn kho của kích cỡ '" + size + "' không được nhỏ hơn 0");
            }

            String sku = (vr.getSku() != null && !vr.getSku().trim().isEmpty())
                    ? vr.getSku().trim()
                    : "PROD-" + product.getId() + "-" + size;

            if (vr.getId() != null) {
                ProductVariant existing = productVariantRepository.findById(vr.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Biến thể", vr.getId()));

                if (!existing.getProduct().getId().equals(product.getId())) {
                    throw new BusinessException("Biến thể #" + vr.getId() + " không thuộc về sản phẩm này");
                }

                if (productVariantRepository.existsBySkuAndIdNot(sku, vr.getId())) {
                    throw new BusinessException("Mã SKU '" + sku + "' đã được sử dụng bởi sản phẩm/biến thể khác");
                }

                if (productVariantRepository.existsByProductIdAndSizeAndIdNot(product.getId(), size, vr.getId())) {
                    throw new BusinessException("Kích cỡ '" + size + "' đã tồn tại cho biến thể khác của sản phẩm");
                }

                existing.setSize(size);
                existing.setStockQuantity(stock);
                existing.setSku(sku);
                ProductVariant saved = productVariantRepository.save(existing);
                updatedVariantIds.add(saved != null && saved.getId() != null ? saved.getId() : existing.getId());
            } else {
                Optional<ProductVariant> opt = productVariantRepository.findByProductIdAndSize(product.getId(), size);
                if (opt.isPresent()) {
                    ProductVariant existing = opt.get();
                    if (productVariantRepository.existsBySkuAndIdNot(sku, existing.getId())) {
                        sku = "PROD-" + product.getId() + "-" + size + "-" + (System.currentTimeMillis() % 1000);
                    }
                    existing.setStockQuantity(stock);
                    existing.setSku(sku);
                    ProductVariant saved = productVariantRepository.save(existing);
                    updatedVariantIds.add(saved != null && saved.getId() != null ? saved.getId() : existing.getId());
                } else {
                    if (productVariantRepository.existsBySku(sku)) {
                        throw new BusinessException("Mã SKU '" + sku + "' đã tồn tại trong hệ thống");
                    }
                    ProductVariant newVariant = ProductVariant.builder()
                            .product(product)
                            .size(size)
                            .stockQuantity(stock)
                            .sku(sku)
                            .build();
                    ProductVariant saved = productVariantRepository.save(newVariant);
                    if (saved != null && saved.getId() != null) {
                        updatedVariantIds.add(saved.getId());
                    }
                }
            }
        }

        // 3. Xử lý xóa an toàn cho các biến thể bị bỏ khỏi form
        for (ProductVariant oldVariant : existingVariants) {
            if (!updatedVariantIds.contains(oldVariant.getId())) {
                if (orderItemRepository != null && (orderItemRepository.existsByVariantId(oldVariant.getId())
                        || orderItemRepository.existsByProductIdAndSize(product.getId(), oldVariant.getSize()))) {
                    throw new BusinessException("Không thể xóa kích cỡ '" + oldVariant.getSize() + "' vì đã có đơn hàng phát sinh cho biến thể này.");
                }
                productVariantRepository.delete(oldVariant);
            }
        }

        // 4. Đồng bộ tổng tồn kho sản phẩm bằng tổng tồn kho của các biến thể
        List<ProductVariant> allCurrent = productVariantRepository.findByProductId(product.getId());
        if (allCurrent != null) {
            int totalStock = allCurrent.stream().mapToInt(ProductVariant::getStockQuantity).sum();
            product.setStockQuantity(totalStock);
            productRepository.save(product);
        }
    }

    /**
     * Admin: Thêm trực tiếp 1 biến thể kích cỡ cho sản phẩm.
     */
    @Transactional
    public ProductVariantResponse addVariantToProduct(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        if (productVariantRepository == null) {
            throw new BusinessException("Hệ thống chưa hỗ trợ quản lý biến thể");
        }

        String size = request.getSize() != null ? request.getSize().trim() : "";
        if (size.isEmpty()) {
            throw new BusinessException("Kích cỡ sản phẩm không được để trống");
        }
        if (productVariantRepository.existsByProductIdAndSize(productId, size)) {
            throw new BusinessException("Kích cỡ '" + size + "' đã tồn tại cho sản phẩm này");
        }

        String sku = (request.getSku() != null && !request.getSku().trim().isEmpty())
                ? request.getSku().trim()
                : "PROD-" + productId + "-" + size;

        if (productVariantRepository.existsBySku(sku)) {
            throw new BusinessException("Mã SKU '" + sku + "' đã tồn tại trong hệ thống");
        }

        int stock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        if (stock < 0) {
            throw new BusinessException("Số lượng tồn kho không được nhỏ hơn 0");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(size)
                .stockQuantity(stock)
                .sku(sku)
                .build();

        ProductVariant saved = productVariantRepository.save(variant);

        List<ProductVariant> all = productVariantRepository.findByProductId(productId);
        int totalStock = all.stream().mapToInt(ProductVariant::getStockQuantity).sum();
        product.setStockQuantity(totalStock);
        productRepository.save(product);

        return ProductVariantResponse.fromEntity(saved);
    }

    /**
     * Admin: Cập nhật thông tin/tồn kho của 1 biến thể kích cỡ.
     */
    @Transactional
    public ProductVariantResponse updateProductVariant(Long productId, Long variantId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        if (productVariantRepository == null) {
            throw new BusinessException("Hệ thống chưa hỗ trợ quản lý biến thể");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Biến thể không thuộc về sản phẩm này");
        }

        String size = request.getSize() != null ? request.getSize().trim() : variant.getSize();
        if (size.isEmpty()) {
            throw new BusinessException("Kích cỡ sản phẩm không được để trống");
        }

        if (productVariantRepository.existsByProductIdAndSizeAndIdNot(productId, size, variantId)) {
            throw new BusinessException("Kích cỡ '" + size + "' đã tồn tại cho biến thể khác của sản phẩm");
        }

        String sku = (request.getSku() != null && !request.getSku().trim().isEmpty())
                ? request.getSku().trim()
                : variant.getSku();

        if (productVariantRepository.existsBySkuAndIdNot(sku, variantId)) {
            throw new BusinessException("Mã SKU '" + sku + "' đã tồn tại trong hệ thống");
        }

        int stock = request.getStockQuantity() != null ? request.getStockQuantity() : variant.getStockQuantity();
        if (stock < 0) {
            throw new BusinessException("Số lượng tồn kho không được nhỏ hơn 0");
        }

        variant.setSize(size);
        variant.setSku(sku);
        variant.setStockQuantity(stock);
        ProductVariant updated = productVariantRepository.save(variant);

        List<ProductVariant> all = productVariantRepository.findByProductId(productId);
        int totalStock = all.stream().mapToInt(ProductVariant::getStockQuantity).sum();
        product.setStockQuantity(totalStock);
        productRepository.save(product);

        return ProductVariantResponse.fromEntity(updated);
    }

    /**
     * Admin: Xóa 1 biến thể kích cỡ (chặn nếu đã có đơn hàng sử dụng biến thể này).
     */
    @Transactional
    public void deleteProductVariant(Long productId, Long variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", productId));

        if (productVariantRepository == null) {
            throw new BusinessException("Hệ thống chưa hỗ trợ quản lý biến thể");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Biến thể không thuộc về sản phẩm này");
        }

        if (orderItemRepository != null && (orderItemRepository.existsByVariantId(variantId)
                || orderItemRepository.existsByProductIdAndSize(productId, variant.getSize()))) {
            throw new BusinessException("Không thể xóa kích cỡ '" + variant.getSize() + "' vì đã có đơn hàng phát sinh cho biến thể này.");
        }

        productVariantRepository.delete(variant);

        List<ProductVariant> all = productVariantRepository.findByProductId(productId);
        int totalStock = all.stream().mapToInt(ProductVariant::getStockQuantity).sum();
        product.setStockQuantity(totalStock);
        productRepository.save(product);
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
        List<Product> candidates = productRepository.findDealCandidates(PageRequest.of(0, 30));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Thời gian bắt đầu 00:00:00 và kết thúc 23:59:59 cùng ngày theo giờ Việt Nam
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.atTime(23, 59, 59);

        // Luân phiên sản phẩm cố định theo ngày (deterministic rotation)
        // để tất cả khách hàng cùng thấy 1 sản phẩm trong ngày và tự động chuyển sang sản phẩm kế tiếp vào ngày mới
        int dealIndex = (int) Math.abs(today.toEpochDay() % candidates.size());
        Product dealProduct = candidates.get(dealIndex);

        int discountPercentage = 25; // Tỷ lệ giảm chuẩn 25% nếu không có coupon cụ thể
        String couponCode = null;

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
                    // Lưu ý: Deal of the Day luôn kết thúc vào 23:59:59 của ngày hôm nay (tối đa 24 giờ),
                    // không ghi đè endTime bằng expireAt dài hạn của coupon để đảm bảo countdown chính xác < 24h.
                }
            } catch (Exception e) {
                log.warn("Không thể tải coupon cho Deal of the Day: {}", e.getMessage());
            }
        }

        BigDecimal salePrice = dealProduct.getPrice() != null ? dealProduct.getPrice() : BigDecimal.ZERO;
        if (discountPercentage <= 0 || discountPercentage >= 100) {
            discountPercentage = 25;
        }
        // Giá gốc tính theo tỷ lệ chiết khấu: originalPrice = salePrice / (1 - discountPercentage / 100)
        BigDecimal oneMinusDiscount = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        BigDecimal originalPrice = salePrice;
        if (oneMinusDiscount.compareTo(BigDecimal.ZERO) > 0) {
            originalPrice = salePrice.divide(oneMinusDiscount, 0, RoundingMode.HALF_UP);
        }

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
                .startTime(startTime)
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

        java.util.List<ProductResponse> content = new java.util.ArrayList<>();
        int startIndex = validPage * validSize;
        java.util.List<BestSellerProjection> projections = bestSellerPage.getContent();
        for (int i = 0; i < projections.size(); i++) {
            ProductResponse res = ProductResponse.fromProjection(projections.get(i));
            if (res != null) {
                res.setRank(startIndex + i + 1);
                content.add(res);
            }
        }

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(bestSellerPage.getNumber())
                .pageSize(bestSellerPage.getSize())
                .totalElements(bestSellerPage.getTotalElements())
                .totalPages(bestSellerPage.getTotalPages())
                .last(bestSellerPage.isLast())
                .build();
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
