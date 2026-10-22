package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service quản lý đơn hàng (Cart & Order Module).
 * Phụ trách: Vũ Đức Huy Hoàng
 *
 * Chức năng:
 * - Tạo đơn hàng: tính toán totalAmount an toàn tại backend, gọi InventoryService trừ kho trong 1 transaction.
 * - Xem danh sách đơn hàng của người dùng (lịch sử đơn hàng).
 * - Xem chi tiết đơn hàng.
 * - Hủy đơn hàng: chỉ cho phép hủy khi PENDING hoặc CONFIRMED, gọi InventoryService hoàn lại tồn kho.
 * - Quản trị viên cập nhật trạng thái đơn hàng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;

    private static final Random RANDOM = new Random();

    /**
     * Tạo đơn hàng mới từ giỏ hàng.
     * Toàn bộ thao tác (lưu đơn, trừ kho, tạo thanh toán) thực hiện trong 1 transaction an toàn.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {
        log.info("Bắt đầu tạo đơn hàng cho người dùng: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new BusinessException("Tài khoản của bạn đã bị vô hiệu hóa");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng không có sản phẩm nào để đặt hàng");
        }

        // 1. Tạo đối tượng Order ban đầu
        String orderCode = generateUniqueOrderCode();

        PaymentStatus initialPaymentStatus = (request.getPaymentMethod() == PaymentMethod.SIMULATED)
                ? PaymentStatus.PAID
                : PaymentStatus.UNPAID;

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .totalAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(initialPaymentStatus)
                .note(request.getNote() != null ? request.getNote().trim() : null)
                .items(new ArrayList<>())
                .build();

        // 2. Tính toán chi tiết từng sản phẩm và tổng tiền tại Backend (không tin giá từ frontend)
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + itemReq.getProductId()));

            if (Boolean.FALSE.equals(product.getActive())) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' hiện đang ngừng kinh doanh");
            }

            if (itemReq.getQuantity() <= 0) {
                throw new BusinessException("Số lượng đặt hàng của sản phẩm '" + product.getName() + "' phải lớn hơn 0");
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 3. Lưu Order & OrderItems để sinh Order ID
        Order savedOrder = orderRepository.save(order);

        // 4. Trừ kho thông qua InventoryService (đã thống nhất với Bình)
        inventoryService.deductStockForOrder(savedOrder);

        // 5. Tạo bản ghi Payment gắn liền với đơn hàng
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(request.getPaymentMethod())
                .status(initialPaymentStatus)
                .amount(totalAmount)
                .transactionNote(request.getPaymentMethod() == PaymentMethod.SIMULATED
                        ? "Thanh toán mô phỏng thành công qua ví điện tử / thẻ"
                        : "Thanh toán tiền mặt khi nhận hàng (COD)")
                .paidAt(request.getPaymentMethod() == PaymentMethod.SIMULATED ? LocalDateTime.now() : null)
                .build();

        paymentRepository.save(payment);
        savedOrder.setPayment(payment);

        log.info("Tạo đơn hàng thành công: mã đơn {}, tổng tiền: {}", savedOrder.getOrderCode(), savedOrder.getTotalAmount());

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng hiện tại (sắp xếp mới nhất lên đầu).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một đơn hàng theo ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        // Kiểm tra quyền: Người mua chỉ xem được đơn của mình, ADMIN xem được mọi đơn
        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Hủy đơn hàng.
     * Quy tắc:
     * - Chỉ được hủy khi đơn ở trạng thái PENDING hoặc CONFIRMED.
     * - Không thể hủy khi đơn đang PROCESSING, SHIPPING, COMPLETED.
     * - Khi hủy thành công: trạng thái đổi sang CANCELLED, gọi InventoryService hoàn kho.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(String userEmail, Long orderId, String reason) {
        log.info("Yêu cầu hủy đơn hàng {} từ user {}", orderId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        // Kiểm tra quyền sở hữu đơn hàng
        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã được hủy trước đó");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(String.format(
                    "Không thể hủy đơn hàng ở trạng thái '%s'. Chỉ có thể hủy đơn khi 'Chờ xác nhận' hoặc 'Đã xác nhận'.",
                    order.getOrderStatus().getDescription()
            ));
        }

        // Cập nhật trạng thái đơn
        order.setOrderStatus(OrderStatus.CANCELLED);

        // Nếu đã thanh toán trực tuyến, hoàn tiền mô phỏng
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.REFUNDED);
                order.getPayment().setTransactionNote("Đã hoàn tiền vào tài khoản do đơn hàng bị hủy");
                paymentRepository.save(order.getPayment());
            }
        }

        // Thêm ghi chú lý do hủy
        String cancelNote = "[Đã hủy bởi " + (user.getRole() == Role.ADMIN ? "Admin" : "Khách hàng") + "]";
        if (reason != null && !reason.trim().isEmpty()) {
            cancelNote += " Lý do: " + reason.trim();
        }
        order.setNote(order.getNote() != null ? order.getNote() + " | " + cancelNote : cancelNote);

        // Hoàn lại số lượng tồn kho (đã thống nhất với Bình)
        inventoryService.restoreStockForOrder(order);

        Order updatedOrder = orderRepository.save(order);
        log.info("Hủy đơn hàng thành công: mã đơn {}", updatedOrder.getOrderCode());

        return mapToOrderResponse(updatedOrder);
    }

    /**
     * Dành cho Admin: Lấy toàn bộ đơn hàng trong hệ thống.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Dành cho Admin: Cập nhật trạng thái đơn hàng.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

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
                    paymentRepository.save(order.getPayment());
                }
            }
        }

        // Nếu admin xác nhận hoàn thành và đơn COD: cập nhật payment status sang PAID
        if (newStatus == OrderStatus.COMPLETED && order.getPaymentStatus() == PaymentStatus.UNPAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(PaymentStatus.PAID);
                order.getPayment().setPaidAt(LocalDateTime.now());
                paymentRepository.save(order.getPayment());
            }
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Admin cập nhật trạng thái đơn hàng {} từ {} sang {}",
                order.getOrderCode(), currentStatus, newStatus);

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Dành cho Admin: Lấy chi tiết đơn hàng theo ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));
        return mapToOrderResponse(order);
    }

    /**
     * Dành cho Admin: Thống kê số liệu đơn hàng và doanh thu.
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
        long processingOrders = orderRepository.countByOrderStatus(OrderStatus.PROCESSING);
        long shippingOrders = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long completedOrders = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByOrderStatus(OrderStatus.COMPLETED);

        long totalProducts = productRepository.count();
        long outOfStock = productRepository.countByStockQuantity(0);
        long lowStock = productRepository.countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(5, 0);

        return DashboardStatsResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalProducts(totalProducts)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .build();
    }

    /**
     * Xác thực tính hợp lệ của luồng chuyển trạng thái đơn hàng (State Machine).
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
                valid = false;
                break;
        }

        if (!valid) {
            throw new BusinessException(String.format(
                    "Không thể chuyển trạng thái đơn hàng từ '%s' sang '%s'. Vui lòng tuân thủ đúng quy trình xử lý đơn hàng.",
                    current.getDescription(), next.getDescription()
            ));
        }
    }

    /**
     * Sinh mã đơn hàng duy nhất có định dạng: ORD-YYYYMMDD-XXXX (ví dụ: ORD-20261022-8142)
     */
    private String generateUniqueOrderCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomPart = 1000 + RANDOM.nextInt(9000);
            code = "ORD-" + datePart + "-" + randomPart;
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    /**
     * Map Order entity sang OrderResponse DTO.
     */
    private OrderResponse mapToOrderResponse(Order order) {
        PaymentMethod method = (order.getPayment() != null)
                ? order.getPayment().getPaymentMethod()
                : PaymentMethod.COD;

        List<OrderItemResponse> itemResponses = order.getItems().stream().map(item -> {
            String imageUrl = (item.getProduct() != null) ? item.getProduct().getImageUrl() : null;
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .imageUrl(imageUrl)
                    .build();
        }).collect(Collectors.toList());

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
                .paymentMethod(method)
                .paymentMethodDescription(method != null ? method.getDescription() : null)
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
