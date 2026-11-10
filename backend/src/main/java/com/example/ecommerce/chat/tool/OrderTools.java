package com.example.ecommerce.chat.tool;

import com.example.ecommerce.chat.tool.dto.MyOrdersRequest;
import com.example.ecommerce.chat.tool.dto.OrderDetailRequest;
import com.example.ecommerce.dto.response.OrderItemResponse;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.User;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tool tra cứu đơn hàng cá nhân dành riêng cho người dùng đã đăng nhập.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT] Spring AI Tool Calling
 *
 * Ràng buộc an toàn:
 * 1. Tuyệt đối không nhận userId từ tham số do LLM truyền vào.
 * 2. Luôn trích xuất danh tính người dùng an toàn từ ToolContext ở phía server.
 * 3. Khách vãng lai (Guest) tuyệt đối không được phép truy xuất dữ liệu đơn hàng.
 * 4. Luôn kiểm tra quyền sở hữu đơn hàng (Ownership Check) trước khi trả về kết quả.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTools {

    private final OrderService orderService;
    private final UserRepository userRepository;

    private static final int MAX_ORDER_RESULTS = 5;

    /**
     * Tra cứu danh sách các đơn hàng gần đây của khách hàng đang đăng nhập.
     */
    public String getMyOrders(MyOrdersRequest request, ToolContext toolContext) {
        log.info("Tool getMyOrders called with request: {}", request);

        User currentUser = resolveUserFromContext(toolContext);
        if (currentUser == null) {
            return "Bạn hiện đang ở chế độ khách vãng lai. Vui lòng đăng nhập tài khoản Rikkei Ecommerce để tra cứu thông tin đơn hàng cá nhân của bạn.";
        }

        try {
            int limit = (request != null && request.getLimit() != null && request.getLimit() > 0)
                    ? Math.min(request.getLimit(), MAX_ORDER_RESULTS)
                    : MAX_ORDER_RESULTS;

            PageResponse<OrderResponse> ordersPage = orderService.getMyOrders(
                    currentUser.getEmail(),
                    0,
                    limit,
                    "createdAt",
                    "desc"
            );

            List<OrderResponse> orders = ordersPage.getContent();
            if (orders == null || orders.isEmpty()) {
                return "Bạn hiện chưa có đơn hàng nào trên hệ thống Rikkei Ecommerce.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Danh sách %d đơn hàng gần nhất của bạn (Tài khoản: %s):\n",
                    orders.size(), currentUser.getEmail()));

            for (OrderResponse o : orders) {
                sb.append(String.format("- Mã đơn: %s | Ngày đặt: %s | Trạng thái: %s | Tổng tiền: %,.0f đ | Người nhận: %s (%s)\n",
                        o.getOrderCode(),
                        o.getCreatedAt() != null ? o.getCreatedAt().toString() : "N/A",
                        formatOrderStatus(o.getStatus() != null ? o.getStatus().name() : null),
                        o.getTotalAmount(),
                        o.getRecipientName(),
                        o.getRecipientPhone()
                ));
            }
            sb.append("\nBạn có thể hỏi thêm thông tin chi tiết từng đơn hàng bằng cách cung cấp mã đơn hàng tương ứng.");
            return sb.toString();

        } catch (Exception e) {
            log.error("Error executing getMyOrders tool: {}", e.getMessage(), e);
            return "Lỗi khi tra cứu danh sách đơn hàng: " + e.getMessage();
        }
    }

    /**
     * Tra cứu thông tin chi tiết một đơn hàng cụ thể theo mã đơn (orderCode) hoặc ID.
     */
    public String getOrderDetail(OrderDetailRequest request, ToolContext toolContext) {
        log.info("Tool getOrderDetail called with request: {}", request);

        User currentUser = resolveUserFromContext(toolContext);
        if (currentUser == null) {
            return "Bạn hiện đang ở chế độ khách vãng lai. Vui lòng đăng nhập tài khoản để xem chi tiết đơn hàng.";
        }

        if (request == null || ((request.getOrderCode() == null || request.getOrderCode().isBlank()) && request.getOrderId() == null)) {
            return "Vui lòng cung cấp mã đơn hàng (ví dụ: 'ORD-...') hoặc mã số ID đơn hàng cần xem chi tiết.";
        }

        try {
            OrderResponse order;
            if (request.getOrderCode() != null && !request.getOrderCode().isBlank()) {
                order = orderService.getOrderByOrderCode(request.getOrderCode().trim(), currentUser.getEmail());
            } else {
                order = orderService.getOrderDetail(request.getOrderId(), currentUser.getEmail());
            }

            if (order == null) {
                return "Không tìm thấy thông tin đơn hàng theo yêu cầu.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Thông tin chi tiết đơn hàng:\n");
            sb.append(String.format("- Mã đơn: %s (ID: %d)\n", order.getOrderCode(), order.getId()));
            sb.append(String.format("- Trạng thái xử lý: %s\n", formatOrderStatus(order.getStatus() != null ? order.getStatus().name() : null)));
            sb.append(String.format("- Ngày tạo: %s\n", order.getCreatedAt()));
            sb.append(String.format("- Người nhận: %s - SĐT: %s\n", order.getRecipientName(), order.getRecipientPhone()));
            sb.append(String.format("- Địa chỉ giao hàng: %s\n", order.getShippingAddress()));
            sb.append(String.format("- Phương thức thanh toán: %s (Trạng thái: %s)\n",
                    order.getPaymentMethod(), order.getPaymentStatus()));
            sb.append(String.format("- Tổng giá trị thanh toán: %,.0f đ\n", order.getTotalAmount()));

            if (order.getItems() != null && !order.getItems().isEmpty()) {
                sb.append("- Danh sách sản phẩm trong đơn:\n");
                for (OrderItemResponse item : order.getItems()) {
                    sb.append(String.format("  + %s | Số lượng: %d | Đơn giá: %,.0f đ | Thành tiền: %,.0f đ\n",
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    ));
                }
            }

            return sb.toString();

        } catch (AccessDeniedException e) {
            log.warn("Access denied when accessing order: {}", e.getMessage());
            return "Bạn không có quyền truy cập hoặc xem chi tiết đơn hàng này.";
        } catch (ResourceNotFoundException e) {
            return "Không tìm thấy đơn hàng với thông tin đã cung cấp. Vui lòng kiểm tra lại mã đơn.";
        } catch (Exception e) {
            log.error("Error executing getOrderDetail tool: {}", e.getMessage(), e);
            return "Lỗi khi tra cứu chi tiết đơn hàng: " + e.getMessage();
        }
    }

    /**
     * Trích xuất User entity an toàn từ ToolContext do backend inject.
     */
    private User resolveUserFromContext(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }

        Object isGuestObj = toolContext.getContext().get("isGuest");
        if (Boolean.TRUE.equals(isGuestObj)) {
            return null;
        }

        Object userIdObj = toolContext.getContext().get("authenticatedUserId");
        if (userIdObj == null) {
            return null;
        }

        String userIdStr = String.valueOf(userIdObj).trim();
        if (userIdStr.isBlank() || "null".equalsIgnoreCase(userIdStr)) {
            return null;
        }

        // Thử tìm theo ID số
        try {
            Long userId = Long.parseLong(userIdStr);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        } catch (NumberFormatException ignored) {
            // Không phải số -> tìm theo email
        }

        return userRepository.findByEmail(userIdStr).orElse(null);
    }

    private String formatOrderStatus(String status) {
        if (status == null) return "Chưa xác định";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Chờ xác nhận (PENDING)";
            case "CONFIRMED" -> "Đã xác nhận (CONFIRMED)";
            case "PROCESSING" -> "Đang đóng gói xử lý (PROCESSING)";
            case "SHIPPING" -> "Đang giao hàng (SHIPPING)";
            case "COMPLETED" -> "Giao hàng thành công (COMPLETED)";
            case "DELIVERED" -> "Giao hàng thành công (DELIVERED)";
            case "CANCELLED" -> "Đã hủy đơn (CANCELLED)";
            default -> status;
        };
    }
}
