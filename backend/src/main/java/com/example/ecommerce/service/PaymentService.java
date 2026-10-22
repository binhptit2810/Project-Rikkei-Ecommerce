package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service quản lý thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 *
 * Hỗ trợ các phương thức:
 * - COD: Thanh toán khi nhận hàng (UNPAID)
 * - SIMULATED: Thanh toán trực tuyến mô phỏng (hỗ trợ giả lập thành công PAID hoặc thất bại FAILED)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Xử lý thanh toán cho một đơn hàng (hỗ trợ COD và SIMULATED).
     *
     * @param orderId ID đơn hàng cần thanh toán
     * @param userEmail Email của người thực hiện thanh toán (để phân quyền)
     * @param request Dữ liệu thanh toán
     * @return PaymentResponse kết quả thanh toán
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processPayment(Long orderId, String userEmail, ProcessPaymentRequest request) {
        log.info("Xử lý thanh toán cho đơn hàng #{} bởi user {}", orderId, userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        // Phân quyền: Chỉ chủ đơn hàng hoặc ADMIN mới có quyền thực hiện
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Bạn không có quyền thực hiện thanh toán cho đơn hàng này");
            }
        }

        // Không cho thanh toán đơn đã hủy
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không thể thực hiện thanh toán cho đơn hàng đã bị hủy");
        }

        // Không cho thanh toán lại đơn đã thanh toán thành công
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng này đã được thanh toán thành công trước đó");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .status(PaymentStatus.UNPAID)
                        .paymentMethod(request.getPaymentMethod())
                        .build());

        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(order.getTotalAmount());

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.UNPAID);
            payment.setPaidAt(null);
            payment.setTransactionNote("Thanh toán tiền mặt khi nhận hàng (COD)");
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (request.getPaymentMethod() == PaymentMethod.SIMULATED) {
            boolean isSuccess = request.getSimulateSuccess() == null || Boolean.TRUE.equals(request.getSimulateSuccess());
            if (isSuccess) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionNote(request.getTransactionNote() != null && !request.getTransactionNote().trim().isEmpty()
                        ? request.getTransactionNote().trim()
                        : "Thanh toán trực tuyến mô phỏng thành công");
                order.setPaymentStatus(PaymentStatus.PAID);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaidAt(null);
                payment.setTransactionNote(request.getTransactionNote() != null && !request.getTransactionNote().trim().isEmpty()
                        ? request.getTransactionNote().trim()
                        : "Thanh toán mô phỏng thất bại: Giao dịch bị từ chối hoặc lỗi số dư");
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        }

        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        orderRepository.save(order);

        log.info("Thanh toán đơn hàng #{} hoàn tất: Phương thức={}, Trạng thái={}",
                order.getOrderCode(), savedPayment.getPaymentMethod(), savedPayment.getStatus());

        return mapToPaymentResponse(savedPayment);
    }

    /**
     * Tra cứu thông tin thanh toán của một đơn hàng.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Bạn không có quyền xem thông tin thanh toán của đơn hàng này");
            }
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có thông tin thanh toán cho đơn hàng: " + orderId));

        return mapToPaymentResponse(payment);
    }

    /**
     * Chuyển đổi Payment Entity sang PaymentResponse DTO.
     */
    public PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderCode(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .paymentMethod(payment.getPaymentMethod())
                .paymentMethodDescription(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getDescription() : null)
                .status(payment.getStatus())
                .statusDescription(payment.getStatus() != null ? payment.getStatus().getDescription() : null)
                .amount(payment.getAmount())
                .transactionNote(payment.getTransactionNote())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
