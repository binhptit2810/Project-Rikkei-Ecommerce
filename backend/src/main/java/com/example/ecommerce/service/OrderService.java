package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Phụ trách: Vũ Đức Huy Hoàng (Thành viên 3 - VDHH)
 *
 * Chức năng:
 * - Tạo đơn hàng từ giỏ hàng (createOrder):
 *   + Tính toán đơn giá và tổng tiền tuyệt đối tại Server dựa trên giá sản phẩm trong DB (chống gian lận client).
 *   + Lưu OrderItem snapshot (tên sản phẩm, đơn giá, số lượng, subtotal tại thời điểm mua).
 *   + Khóa bi quan và trừ tồn kho an toàn thông qua InventoryService.
 *   + Khởi tạo bản ghi thanh toán thông qua PaymentService.
 *   + Toàn bộ bọc trong một giao dịch @Transactional duy nhất để đảm bảo tính nhất quán ACID.
 * - Tra cứu lịch sử đơn hàng của người dùng (getMyOrders).
 * - Tra cứu chi tiết đơn hàng (getOrderById, getOrderByOrderCode).
 * - Hủy đơn hàng và hoàn lại tồn kho (cancelOrder).
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

        // 5. Trừ kho an toàn có khóa bi quan (PESSIMISTIC_WRITE) qua InventoryService
        // Nếu bất kỳ sản phẩm nào không đủ tồn kho, ngoại lệ BusinessException sẽ được ném
        // và toàn bộ giao dịch @Transactional được rollback
        inventoryService.deductStockForOrder(savedOrder);

        // 6. Khởi tạo bản ghi thanh toán ban đầu (COD hoặc SIMULATED)
        Payment payment = paymentService.createInitialPayment(savedOrder, request.getPaymentMethod());
        savedOrder.setPayment(payment);

        log.info("Tạo đơn hàng thành công! Mã đơn: {}, Tổng tiền: {} VNĐ, Trạng thái: {}",
                savedOrder.getOrderCode(), savedOrder.getTotalAmount(), savedOrder.getOrderStatus());

        return mapToOrderResponse(savedOrder);
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của người dùng đang đăng nhập.
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
     * Xem chi tiết đơn hàng theo ID (kiểm tra phân quyền sở hữu).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, String userEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền truy cập thông tin đơn hàng này");
        }

        return mapToOrderResponse(order);
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
            throw new BusinessException("Bạn không có quyền truy cập thông tin đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    /**
     * Hủy đơn hàng và hoàn lại số lượng tồn kho.
     * Chỉ cho phép hủy khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(Long id, String userEmail) {
        log.info("Yêu cầu hủy đơn hàng ID: {} từ người dùng: {}", id, userEmail);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email: " + userEmail));

        if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(String.format("Không thể hủy đơn hàng ở trạng thái '%s'",
                    order.getOrderStatus().getDescription()));
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Hoàn lại tồn kho và ghi nhật ký ORDER_CANCELLED
        inventoryService.restoreStockForOrder(order);

        // Hoàn tiền nếu đơn hàng đã được thanh toán
        if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PAID) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Hủy đơn hàng thành công: mã {}, kho đã được hoàn lại", updatedOrder.getOrderCode());

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
     * Chuyển đổi Order entity sang OrderResponse DTO.
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

