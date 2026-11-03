package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service quản lý đơn hàng (Order Module).
 * Người phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 * Người phối hợp: Đặng Thanh Bình [DTB] (Inventory & Payment)
 *
 * Chức năng:
 * - Tạo đơn hàng từ giỏ hàng (createOrder) bọc trong 1 @Transactional duy nhất.
 * - Lấy lịch sử đơn hàng của tôi có phân trang (getMyOrders).
 * - Lấy chi tiết đơn hàng kèm danh sách món hàng OrderItem (getOrderDetail).
 * - Hủy đơn hàng và hoàn lại tồn kho qua restoreStockForOrder() (cancelOrder).
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
    private static final int MAX_PAGE_SIZE = 50;

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
     *
     * @param userEmail Email người dùng đăng nhập
     * @param page Số thứ tự trang (0-based)
     * @param size Số phần tử mỗi trang
     * @param sortBy Trường sắp xếp (mặc định createdAt)
     * @param sortDir Hướng sắp xếp (asc/desc)
     * @return PageResponse<OrderResponse>
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
     * Phụ trách: Vũ Đức Huy Hoàng (VDHH)
     *
     * @param id ID của đơn hàng
     * @param userEmail Email người dùng đang đăng nhập
     * @return OrderResponse thông tin chi tiết đơn hàng kèm OrderItem
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
     * Hủy đơn hàng và hoàn lại số lượng tồn kho.
     * Phụ trách: Vũ Đức Huy Hoàng (VDHH)
     * Phối hợp: Đặng Thanh Bình [DTB] - gọi InventoryService.restoreStockForOrder()
     *
     * Điều kiện: Chỉ cho phép hủy khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED.
     *
     * @param id ID của đơn hàng cần hủy
     * @param userEmail Email người dùng yêu cầu hủy
     * @return OrderResponse thông tin đơn hàng sau khi cập nhật trạng thái CANCELLED
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(Long id, String userEmail) {
        log.info("Yêu cầu hủy đơn hàng ID: {} từ người dùng: {}", id, userEmail);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseGet(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id)));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }

        // Kiểm tra điều kiện: chỉ cho phép hủy khi ở trạng thái PENDING hoặc CONFIRMED
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            log.warn("Không thể hủy đơn hàng #{} do đang ở trạng thái {}", id, order.getOrderStatus());
            throw new BusinessException(String.format("Không thể hủy đơn hàng ở trạng thái '%s'. Chỉ được hủy khi đơn ở trạng thái Chờ xác nhận hoặc Đã xác nhận.",
                    order.getOrderStatus().getDescription()));
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Gọi Đặng Thanh Bình [DTB] - restoreStockForOrder() hoàn lại tồn kho và ghi log ORDER_CANCELLED
        inventoryService.restoreStockForOrder(order);

        // Hoàn tiền nếu đơn hàng đã được thanh toán
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Hủy đơn hàng thành công: mã {}, kho đã được hoàn lại đầy đủ", updatedOrder.getOrderCode());

        return mapToOrderResponse(updatedOrder);
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
