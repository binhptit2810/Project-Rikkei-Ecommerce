package com.example.ecommerce.service;

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

/**
 * Service quản lý thanh toán (Payment Module - Sprint 1: COD Initial & Lookups).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Cổng thanh toán trực tuyến mô phỏng sẽ được hiện thực trong Sprint 2 (TSK-D09-DTB).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Khởi tạo bản ghi thanh toán ban đầu cho đơn hàng (COD - UNPAID).
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createInitialPayment(Order order, PaymentMethod paymentMethod) {
        log.info("Khởi tạo bản ghi thanh toán cho đơn hàng {}", order.getOrderCode());

        Payment payment = Payment.builder()
                .order(order)
                .transactionCode(Payment.generateTransactionCode())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.UNPAID)
                .paymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD)
                .transactionNote("Thanh toán tiền mặt khi nhận hàng (COD)")
                .build();

        return paymentRepository.save(payment);
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
     * Tra cứu thông tin thanh toán theo mã giao dịch duy nhất có tiền tố PAY-.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionCode(String transactionCode, String userEmail) {
        if (transactionCode == null || transactionCode.trim().isEmpty()) {
            throw new BusinessException("Mã giao dịch không được để trống");
        }

        Payment payment = paymentRepository.findByTransactionCode(transactionCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với mã: " + transactionCode));

        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if (user.getRole() != Role.ADMIN && (payment.getOrder() == null || !payment.getOrder().getUser().getId().equals(user.getId()))) {
                throw new BusinessException("Bạn không có quyền xem thông tin của giao dịch này");
            }
        }

        return mapToPaymentResponse(payment);
    }

    /**
     * Chuyển đổi Payment Entity sang PaymentResponse DTO.
     */
    public PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionCode(payment.getTransactionCode())
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
