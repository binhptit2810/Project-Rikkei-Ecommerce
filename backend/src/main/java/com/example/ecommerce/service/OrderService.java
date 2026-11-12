package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service quản lý đơn hàng (Order Module).
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Phối hợp: Đặng Thanh Bình [DTB] (Inventory & Payment)
 *
 * Chức năng:
 * - Tạo đơn hàng từ giỏ hàng (createOrder) bọc trong 1 @Transactional duy nhất.
 * - Lấy lịch sử đơn hàng của tôi có phân trang (getMyOrders).
 * - Lấy chi tiết đơn hàng kèm danh sách món hàng OrderItem (getOrderDetail).
 * - Hủy đơn hàng và hoàn lại tồn kho qua restoreStockForOrder() (cancelOrder).
 * - Quản trị viên lấy danh sách đơn hàng hệ thống và cập nhật trạng thái (updateOrderStatus).
 */
@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final CouponService couponService;
    private final com.example.ecommerce.repository.CouponRepository couponRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryService inventoryService,
            PaymentService paymentService,
            EmailService emailService,
            CouponService couponService,
            com.example.ecommerce.repository.CouponRepository couponRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
    }

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryService inventoryService,
            PaymentService paymentService,
            EmailService emailService
    ) {
        this(orderRepository, productRepository, userRepository, inventoryService, paymentService, emailService, null, null);
    }

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Tạo đơn hàng mới từ giỏ hàng.
     * Toàn bộ quy trình chạy trong 1 giao dịch @Transactional.
     *
     * @param request Dữ liệu đặt hàng từ client (chỉ chứa ID sản phẩm và số lượng, KHÔNG nhận đơn giá hay tổng tiền)
     * @param userEmail Email của người dùng đang đăng nhập
     * @return OrderResponse thông tin đơn hàng vừa được tạo thành công
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request, String userEmail) {
        log.info("Bắt đầu quy trình tạo đơn hàng cho người dùng: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        Long userId = user.getId();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}",
                    userId, "Đơn hàng phải chứa ít nhất một sản phẩm");
            throw new BusinessException("Đơn hàng phải chứa ít nhất một sản phẩm");
        }

        // 1. Gộp các sản phẩm trùng lặp trong request theo (productId, selectedSize) [TSK-D14-VDHH]
        record OrderItemKey(Long productId, String selectedSize) {}
        Map<OrderItemKey, Integer> consolidatedItems = new LinkedHashMap<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null) {
                log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}",
                        userId, "Mã sản phẩm không được để trống");
                throw new BusinessException("Mã sản phẩm không được để trống");
            }
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}",
                        userId, "Số lượng sản phẩm đặt mua phải lớn hơn hoặc bằng 1");
                throw new BusinessException("Số lượng sản phẩm đặt mua phải lớn hơn hoặc bằng 1");
            }
            String rawSize = itemReq.getSelectedSize() != null && !itemReq.getSelectedSize().isBlank()
                    ? itemReq.getSelectedSize().trim() : null;
            consolidatedItems.merge(new OrderItemKey(itemReq.getProductId(), rawSize), itemReq.getQuantity(), Integer::sum);
        }

        // 2. Khởi tạo đối tượng đơn hàng với mã duy nhất
        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 3. Duyệt từng sản phẩm: lấy thông tin từ DB, tính giá, kiểm tra size, lưu snapshot
        for (Map.Entry<OrderItemKey, Integer> entry : consolidatedItems.entrySet()) {
            Long productId = entry.getKey().productId();
            String selectedSize = entry.getKey().selectedSize();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}",
                                userId, "Sản phẩm không tồn tại với ID: " + productId);
                        return new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId);
                    });

            if (!Boolean.TRUE.equals(product.getActive())) {
                String reason = String.format("Sản phẩm '%s' (mã #%d) hiện đã ngừng kinh doanh",
                        product.getName(), productId);
                log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                throw new BusinessException(reason);
            }

            // Kiểm tra ràng buộc chọn kích cỡ nếu sản phẩm có biến thể kích cỡ [TSK-D14-VDHH]
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                if (selectedSize == null || selectedSize.isBlank()) {
                    String reason = "Vui lòng chọn kích cỡ trước khi tiếp tục cho sản phẩm: " + product.getName();
                    log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                    throw new BusinessException(reason);
                }

                ProductVariant matchedVariant = product.getVariants().stream()
                        .filter(v -> v.getSize().equalsIgnoreCase(selectedSize))
                        .findFirst()
                        .orElseThrow(() -> {
                            String reason = String.format("Kích cỡ '%s' không tồn tại cho sản phẩm '%s'", selectedSize, product.getName());
                            log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                            return new BusinessException(reason);
                        });

                if (matchedVariant.getStockQuantity() == null || matchedVariant.getStockQuantity() <= 0) {
                    String reason = "Kích cỡ này hiện đã hết hàng.";
                    log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                    throw new BusinessException(reason);
                }

                if (quantity > matchedVariant.getStockQuantity()) {
                    String reason = "Số lượng sản phẩm vượt quá tồn kho hiện có.";
                    log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                    throw new BusinessException(reason);
                }
            }

            // Tuyệt đối lấy giá từ DB, không nhận hay tin tưởng giá từ client
            BigDecimal unitPrice = product.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                String reason = "Đơn giá của sản phẩm không hợp lệ: " + product.getName();
                log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | reason={}", userId, reason);
                throw new BusinessException(reason);
            }
            unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal).setScale(2, RoundingMode.HALF_UP);

            // Lưu snapshot dữ liệu sản phẩm tại thời điểm mua hàng
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())   // Snapshot tên sản phẩm
                    .selectedSize(selectedSize)       // Snapshot kích cỡ đã chọn [TSK-D14-VDHH]
                    .unitPrice(unitPrice)             // Snapshot đơn giá từ DB (chuẩn hóa scale = 2)
                    .quantity(quantity)               // Số lượng mua
                    .subtotal(subtotal)               // Snapshot thành tiền (chuẩn hóa scale = 2)
                    .build();

            order.addItem(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty() && couponService != null) {
            discountAmount = couponService.applyCouponForOrder(request.getCouponCode().trim(), totalAmount);
            order.setCouponCode(request.getCouponCode().trim().toUpperCase());
            order.setDiscountAmount(discountAmount);
            totalAmount = totalAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        order.setTotalAmount(totalAmount);

        // 4. Lưu đơn hàng vào DB để có ID định danh cho Transaction và Payment
        Order savedOrder = orderRepository.save(order);

        // 5. Trừ kho an toàn có khóa bi quan (PESSIMISTIC_WRITE) qua InventoryService [Đặng Thanh Bình]
        try {
            inventoryService.deductStockForOrder(savedOrder);
        } catch (BusinessException e) {
            log.warn("userId={} | action=CREATE_ORDER | result=FAILURE | orderId={} | reason={}",
                    userId, savedOrder.getId(), sanitize(e.getMessage()));
            throw e;
        }

        // 6. Khởi tạo bản ghi thanh toán ban đầu (COD hoặc SIMULATED)
        Payment payment = paymentService.createInitialPayment(savedOrder, request.getPaymentMethod());
        savedOrder.setPayment(payment);

        Long savedOrderId = savedOrder.getId();
        BigDecimal finalTotalAmount = savedOrder.getTotalAmount();

        logAfterCommitOrNow(() -> {
            log.info("userId={} | action=CREATE_ORDER | result=SUCCESS | orderId={} | totalAmount={}",
                    userId, savedOrderId, finalTotalAmount);
            // Gửi email xác nhận đặt hàng thành công tới khách hàng
            try {
                emailService.sendOrderConfirmationEmail(user.getEmail(), savedOrder.getRecipientName(), savedOrder);
            } catch (Exception mailEx) {
                log.warn("Không thể gửi email xác nhận đặt hàng cho đơn #{}: {}", savedOrder.getOrderCode(), mailEx.getMessage());
            }
        });

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng đang đăng nhập có phân trang.
     * Phụ trách: Vũ Đức Huy Hoàng (VDHH)
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userEmail, int page, int size, String sortBy, String sortDir) {
        log.info("Lấy lịch sử đơn hàng phân trang cho user: {}, page: {}, size: {}", userEmail, page, size);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String validSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, validSortBy));

        Page<Order> orderPage = orderRepository.findByUserId(user.getId(), pageable);
        return PageResponse.from(orderPage, this::mapToOrderResponse);
    }

    /**
     * Lấy toàn bộ danh sách lịch sử đơn hàng của người dùng đang đăng nhập (không phân trang).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    /**
     * Lấy chi tiết đơn hàng theo ID kèm danh sách món hàng (OrderItem) và thông tin thanh toán.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long id, String userEmail) {
        log.info("Lấy chi tiết đơn hàng ID: {} cho user: {}", id, userEmail);
        Order order = orderRepository.findByIdWithItems(id)
                .orElseGet(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id)));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thông tin đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Alias cho getOrderDetail (tương thích ngược).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String userEmail) {
        return getOrderDetail(id, userEmail);
    }

    /**
     * Xem chi tiết đơn hàng theo mã OrderCode.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderCode(String orderCode, String userEmail) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderCode));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập thông tin đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Hủy đơn hàng và hoàn lại số lượng tồn kho (không kèm lý do).
     */
    public OrderResponse cancelOrder(Long id, String userEmail) {
        return cancelOrder(id, userEmail, null);
    }

    /**
     * Hủy đơn hàng và hoàn lại số lượng tồn kho kèm lý do hủy.
     * Phụ trách: Vũ Đức Huy Hoàng (VDHH) & Hoàng Anh [DHA]
     * Phối hợp: Đặng Thanh Bình [DTB] - gọi InventoryService.restoreStockForOrder()
     *
     * Điều kiện: Chỉ cho phép hủy khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED.
     * Nếu đơn ở trạng thái khác (PROCESSING, SHIPPING, COMPLETED, CANCELLED): ném ConflictException (HTTP 409).
     * Áp dụng khóa bi quan PESSIMISTIC_WRITE để chống Race Condition khi có 2 request đồng thời hủy cùng 1 đơn.
     * Tự động hoàn tiền (chuyển sang REFUNDED) nếu đơn trước đó đã thanh toán thành công (PAID).
     *
     * @param id ID của đơn hàng cần hủy
     * @param userEmail Email người dùng yêu cầu hủy
     * @param reason Lý do hủy đơn hàng từ người dùng
     * @return OrderResponse thông tin đơn hàng sau khi cập nhật trạng thái CANCELLED
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(Long id, String userEmail, String reason) {
        log.info("Yêu cầu hủy đơn hàng ID: {} từ người dùng: {}, lý do: {}", id, userEmail, reason);

        // Khóa bi quan chống Race Condition khi có 2 request đồng thời hủy cùng đơn
        Order order = orderRepository.findByIdForUpdate(id)
                .or(() -> orderRepository.findByIdWithItems(id))
                .or(() -> orderRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        Long userId = user.getId();
        Role actorRole = user.getRole();
        String cleanReason = (reason != null && !reason.trim().isEmpty()) ? sanitize(reason.trim()) : "N/A";

        if (actorRole != Role.ADMIN && !order.getUser().getId().equals(userId)) {
            log.warn("userId={} | action=CANCEL_ORDER | actorRole={} | result=FAILURE | orderId={} | reason={}",
                    userId, actorRole, id, "Bạn không có quyền hủy đơn hàng này");
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }

        // Kiểm tra điều kiện: chỉ cho phép hủy khi ở trạng thái PENDING hoặc CONFIRMED
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            String statusDesc = order.getOrderStatus() != null ? order.getOrderStatus().getDescription() : "Không xác định";
            String statusMsg = String.format("Không thể hủy đơn hàng ở trạng thái '%s'. Chỉ được hủy khi đơn ở trạng thái Chờ xác nhận hoặc Đã xác nhận.",
                    statusDesc);
            log.warn("userId={} | action=CANCEL_ORDER | actorRole={} | result=FAILURE | orderId={} | reason={}",
                    userId, actorRole, id, sanitize(statusMsg));
            throw new ConflictException(statusMsg);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Cập nhật lý do hủy vào ghi chú đơn hàng nếu có
        if (reason != null && !reason.trim().isEmpty()) {
            String existingNote = order.getNote();
            if (existingNote != null && !existingNote.isEmpty()) {
                order.setNote(existingNote + " [Lý do hủy: " + cleanReason + "]");
            } else {
                order.setNote("Lý do hủy: " + cleanReason);
            }
        }

        // Gọi Đặng Thanh Bình [DTB] - restoreStockForOrder() hoàn lại tồn kho và ghi log ORDER_CANCELLED
        inventoryService.restoreStockForOrder(order);

        // Tự động chuyển trạng thái thanh toán sang REFUNDED nếu đơn hàng trước đó đã thanh toán
        if (order.getPaymentStatus() == PaymentStatus.PAID ||
                (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID)) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.REFUNDED);
                order.getPayment().setTransactionNote(
                        (order.getPayment().getTransactionNote() != null ? order.getPayment().getTransactionNote() + " - " : "")
                                + "Đã hoàn tiền do hủy đơn hàng " + order.getOrderCode()
                );
            }
            log.info("Đã tự động chuyển trạng thái thanh toán đơn #{} sang REFUNDED", order.getOrderCode());
        }

        // Hoàn lại 1 lượt sử dụng cho mã giảm giá nếu có
        if (order.getCouponCode() != null && !order.getCouponCode().trim().isEmpty() && couponRepository != null) {
            try {
                couponRepository.findByCodeForUpdate(order.getCouponCode()).ifPresent(c -> {
                    if (c.getUsedCount() != null && c.getUsedCount() > 0) {
                        c.setUsedCount(c.getUsedCount() - 1);
                        couponRepository.save(c);
                        log.info("Đã hoàn lại 1 lượt sử dụng cho mã giảm giá '{}'", c.getCode());
                    }
                });
            } catch (Exception e) {
                log.warn("Không thể hoàn lượt sử dụng mã coupon: {}", order.getCouponCode(), e);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        Long orderId = updatedOrder.getId();

        logAfterCommitOrNow(() -> {
            log.warn("userId={} | action=CANCEL_ORDER | actorRole={} | result=SUCCESS | orderId={} | reason={}",
                    userId, actorRole, orderId, cleanReason);
            // Gửi email thông báo hủy đơn hàng tới khách hàng
            try {
                emailService.sendOrderStatusChangedEmail(
                        user.getEmail(),
                        updatedOrder.getRecipientName(),
                        updatedOrder.getOrderCode(),
                        "PENDING / CONFIRMED",
                        "ĐÃ HỦY (CANCELLED)",
                        cleanReason
                );
            } catch (Exception mailEx) {
                log.warn("Không thể gửi email thông báo hủy đơn hàng cho đơn #{}: {}", updatedOrder.getOrderCode(), mailEx.getMessage());
            }
        });

        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Dành cho Admin: Lấy danh sách toàn bộ đơn hàng trong hệ thống có phân trang, tìm kiếm và lọc.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAdminOrders(int page, int size, OrderStatus status, String search, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String validSortBy = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, validSortBy));

        String searchKeyword = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Order> orderPage = orderRepository.searchOrdersAdmin(status, searchKeyword, pageable);
        return PageResponse.from(orderPage, this::mapToOrderResponse);
    }

    /**
     * Dành cho Admin: Lấy toàn bộ đơn hàng trong hệ thống (không phân trang).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    /**
     * Dành cho Admin: Lấy chi tiết đơn hàng theo ID (không kiểm tra quyền sở hữu user).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseGet(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id)));
        return mapToOrderResponse(order);
    }

    /**
     * Dành cho Admin: Cập nhật trạng thái đơn hàng (tuân thủ State Machine).
     */
    /**
     * Quy tắc nghiệp vụ BR-ORDER-03: Dành cho Admin: Cập nhật trạng thái đơn hàng (tuân thủ State Machine).
     * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
     * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
     *
     * Quy trình bắt buộc tuần tự: PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED
     * Chặn tuyệt đối nhảy cóc trạng thái hoặc quay lui trạng thái.
     * Cho phép hủy (CANCELLED) khi đơn đang ở PENDING hoặc CONFIRMED; tự động hoàn tồn kho và hoàn tiền nếu đã thanh toán.
     * Tự động cập nhật Payment sang PAID khi đơn COD được chuyển sang COMPLETED.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        if (orderId == null) {
            throw new BusinessException("Mã đơn hàng không được để trống.");
        }
        if (newStatus == null) {
            throw new BusinessException("Trạng thái đơn hàng mới không được để trống.");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .or(() -> orderRepository.findById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();

        // Kiểm tra tính hợp lệ của luồng chuyển trạng thái (State Machine BR-ORDER-03)
        validateStatusTransition(currentStatus, newStatus);

        // Nếu admin chuyển trạng thái sang CANCELLED mà đơn chưa hủy: hoàn tồn kho và hoàn tiền
        if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
            inventoryService.restoreStockForOrder(order);
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                if (order.getPayment() != null) {
                    order.getPayment().setStatus(PaymentStatus.REFUNDED);
                    order.getPayment().setTransactionNote(
                            (order.getPayment().getTransactionNote() != null ? order.getPayment().getTransactionNote() + " - " : "")
                                    + "Đã hoàn tiền do hủy đơn hàng " + order.getOrderCode()
                    );
                }
            }
        }

        // Nếu admin xác nhận hoàn thành và đơn COD: cập nhật payment status sang PAID
        if (newStatus == OrderStatus.COMPLETED && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.PAID);
                order.getPayment().setPaidAt(LocalDateTime.now());
            }
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Admin cập nhật trạng thái đơn hàng {} từ {} sang {}",
                order.getOrderCode(), currentStatus, newStatus);

        // Gửi email cập nhật tiến trình đơn hàng cho khách hàng
        try {
            if (savedOrder.getUser() != null && savedOrder.getUser().getEmail() != null) {
                emailService.sendOrderStatusChangedEmail(
                        savedOrder.getUser().getEmail(),
                        savedOrder.getRecipientName(),
                        savedOrder.getOrderCode(),
                        currentStatus.name(),
                        newStatus.name(),
                        "Trạng thái đơn hàng của bạn đã được cập nhật bởi hệ thống Rikkei Store."
                );
            }
        } catch (Exception mailEx) {
            log.warn("Không thể gửi email cập nhật trạng thái cho đơn #{}: {}", savedOrder.getOrderCode(), mailEx.getMessage());
        }

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Xác thực tính hợp lệ của luồng chuyển trạng thái đơn hàng (State Machine BR-ORDER-03).
     * Quy tắc nghiệp vụ BR-ORDER-03:
     * - Luồng chuẩn tuần tự: PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED
     * - Chặn tuyệt đối nhảy cóc trạng thái (ví dụ: PENDING -> PROCESSING, PENDING -> SHIPPING, PENDING -> COMPLETED, CONFIRMED -> SHIPPING, etc.)
     * - Chặn tuyệt đối quay lui trạng thái (ví dụ: SHIPPING -> CONFIRMED, CONFIRMED -> PENDING, etc.)
     * - Hủy đơn (CANCELLED) chỉ được phép từ PENDING hoặc CONFIRMED; không thể hủy khi đã PROCESSING, SHIPPING, COMPLETED.
     * - Khi đơn đã COMPLETED hoặc CANCELLED thì là trạng thái kết thúc (Terminal State), không thể thay đổi nữa.
     * - Không thể cập nhật trạng thái trùng lặp với trạng thái hiện tại.
     */
    public void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (next == null) {
            throw new BusinessException("Trạng thái mới không được để trống.");
        }

        if (current == null) {
            throw new BusinessException("Trạng thái hiện tại của đơn hàng không hợp lệ.");
        }

        if (current == next) {
            throw new BusinessException(String.format(
                    "Đơn hàng hiện tại đã ở trạng thái '%s', không thể cập nhật lại trạng thái này.",
                    current.getDescription()));
        }

        if (current == OrderStatus.COMPLETED) {
            throw new BusinessException("Đơn hàng đã hoàn thành, không thể thay đổi trạng thái.");
        }

        if (current == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng đã bị hủy, không thể thay đổi trạng thái.");
        }

        boolean valid = false;
        switch (current) {
            case PENDING:
                valid = (next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED);
                break;
            case CONFIRMED:
                valid = (next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED);
                break;
            case PROCESSING:
                valid = (next == OrderStatus.SHIPPING);
                break;
            case SHIPPING:
                valid = (next == OrderStatus.COMPLETED);
                break;
            default:
                break;
        }

        if (!valid) {
            throw new BusinessException(String.format(
                    "Vi phạm quy tắc nghiệp vụ BR-ORDER-03: Không thể chuyển đổi trạng thái từ '%s' sang '%s'. " +
                    "Quy trình chuyển đổi bắt buộc: PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED " +
                    "(chỉ cho phép hủy từ PENDING hoặc CONFIRMED). Chặn mọi thao tác nhảy cóc hoặc quay lui trạng thái.",
                    current.getDescription(), next.getDescription()));
        }
    }

    /**
     * Sinh mã đơn hàng duy nhất không trùng lặp có định dạng ORD-YYYYMMDD-XXXXXX (tối đa 20 ký tự).
     */
    private String generateUniqueOrderCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            code = "ORD-" + datePart + "-" + randomPart;
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    /**
     * Chuyển đổi Order entity sang OrderResponse DTO (bao gồm danh sách OrderItem snapshot).
     */
    public OrderResponse mapToOrderResponse(Order order) {
        Payment payment = order.getPayment();

        PaymentMethod paymentMethod = null;
        String paymentMethodDesc = null;
        String transactionCode = null;

        if (payment != null) {
            paymentMethod = payment.getPaymentMethod();
            paymentMethodDesc = paymentMethod != null ? paymentMethod.getDescription() : null;
            transactionCode = payment.getTransactionCode();
        }

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::mapToOrderItemResponse).toList()
                : List.of();

        int itemCount = itemResponses != null
                ? itemResponses.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum()
                : 0;

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderStatusDescription(order.getOrderStatus() != null ? order.getOrderStatus().getDescription() : null)
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusDescription(order.getPaymentStatus() != null ? order.getPaymentStatus().getDescription() : null)
                .paymentMethod(paymentMethod)
                .paymentMethodDescription(paymentMethodDesc)
                .transactionCode(transactionCode)
                .note(order.getNote())
                .couponCode(order.getCouponCode())
                .discountAmount(order.getDiscountAmount())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Chuyển đổi OrderItem entity sang OrderItemResponse DTO.
     */
    private void logAfterCommitOrNow(Runnable logAction) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    logAction.run();
                }
            });
        } else {
            logAction.run();
        }
    }

    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\\r\\n]+", " ").trim();
    }

    public OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .selectedSize(item.getSelectedSize())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .imageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                .build();
    }
}
