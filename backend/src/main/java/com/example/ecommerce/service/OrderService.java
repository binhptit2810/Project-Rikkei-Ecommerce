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

import java.math.BigDecimal;
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
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

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

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Đơn hàng phải chứa ít nhất một sản phẩm");
        }

        // 1. Gộp các sản phẩm trùng lặp trong request (nếu có)
        Map<Long, Integer> consolidatedItems = new LinkedHashMap<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getProductId() == null) {
                throw new BusinessException("Mã sản phẩm không được để trống");
            }
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BusinessException("Số lượng sản phẩm đặt mua phải lớn hơn hoặc bằng 1");
            }
            consolidatedItems.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
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

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Duyệt từng sản phẩm: lấy thông tin từ DB, tính giá, lưu snapshot
        for (Map.Entry<Long, Integer> entry : consolidatedItems.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + productId));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BusinessException(String.format("Sản phẩm '%s' (mã #%d) hiện đã ngừng kinh doanh",
                        product.getName(), productId));
            }

            // Tuyệt đối lấy giá từ DB, không nhận hay tin tưởng giá từ client
            BigDecimal unitPrice = product.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Đơn giá của sản phẩm không hợp lệ: " + product.getName());
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);

            // Lưu snapshot dữ liệu sản phẩm tại thời điểm mua hàng
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())   // Snapshot tên sản phẩm
                    .unitPrice(unitPrice)             // Snapshot đơn giá từ DB
                    .quantity(quantity)               // Số lượng mua
                    .subtotal(subtotal)               // Snapshot thành tiền
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 4. Lưu đơn hàng vào DB để có ID định danh cho Transaction và Payment
        Order savedOrder = orderRepository.save(order);

        // 5. Trừ kho an toàn có khóa bi quan (PESSIMISTIC_WRITE) qua InventoryService [Đặng Thanh Bình]
        inventoryService.deductStockForOrder(savedOrder);

        // 6. Khởi tạo bản ghi thanh toán ban đầu (COD hoặc SIMULATED)
        Payment payment = paymentService.createInitialPayment(savedOrder, request.getPaymentMethod());
        savedOrder.setPayment(payment);

        log.info("Tạo đơn hàng thành công! Mã đơn: {}, Tổng tiền: {} VNĐ, Trạng thái: {}",
                savedOrder.getOrderCode(), savedOrder.getTotalAmount(), savedOrder.getOrderStatus());

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

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }

        // Kiểm tra điều kiện: chỉ cho phép hủy khi ở trạng thái PENDING hoặc CONFIRMED
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            log.warn("Không thể hủy đơn hàng #{} do đang ở trạng thái {}", id, order.getOrderStatus());
            throw new ConflictException(String.format("Không thể hủy đơn hàng ở trạng thái '%s'. Chỉ được hủy khi đơn ở trạng thái Chờ xác nhận hoặc Đã xác nhận.",
                    order.getOrderStatus().getDescription()));
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Cập nhật lý do hủy vào ghi chú đơn hàng nếu có
        if (reason != null && !reason.trim().isEmpty()) {
            String cleanReason = reason.trim();
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

        Order updatedOrder = orderRepository.save(order);
        log.info("Hủy đơn hàng thành công: mã {}, kho đã được hoàn lại đầy đủ", updatedOrder.getOrderCode());

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
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus == newStatus) {
            return mapToOrderResponse(order);
        }

        // Kiểm tra tính hợp lệ của luồng chuyển trạng thái (State Machine)
        validateStatusTransition(currentStatus, newStatus);

        // Nếu admin chuyển trạng thái sang CANCELLED mà đơn chưa hủy: hoàn tồn kho
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

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Xác thực tính hợp lệ của luồng chuyển trạng thái đơn hàng (State Machine).
     * Luồng chuẩn: PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED
     * Hủy đơn (CANCELLED) chỉ được phép từ PENDING hoặc CONFIRMED.
     */
    public void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
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
            throw new BusinessException(String.format("Không thể chuyển đổi trạng thái từ '%s' sang '%s'. Luồng chuyển đổi chuẩn: PENDING -> CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED (hoặc CANCELLED từ PENDING/CONFIRMED).",
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
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Chuyển đổi OrderItem entity sang OrderItemResponse DTO.
     */
    public OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .imageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                .build();
    }
}
