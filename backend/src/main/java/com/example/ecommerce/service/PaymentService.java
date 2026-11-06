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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service quản lý thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Xử lý thanh toán COD và cổng thanh toán trực tuyến mô phỏng SIMULATED.
 * Mọi giao dịch đều được cấp mã định danh duy nhất có tiền tố PAY- (định dạng PAY-YYYYMMDD-XXXXXXXX).
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

        PaymentMethod method = paymentMethod != null ? paymentMethod : PaymentMethod.COD;
        String initialNote = (method == PaymentMethod.SIMULATED)
                ? "Chờ thanh toán qua cổng mô phỏng (SIMULATED)"
                : "Thanh toán tiền mặt khi nhận hàng (COD)";

        Payment payment = Payment.builder()
                .order(order)
                .transactionCode(Payment.generateTransactionCode())
                .amount(order.getTotalAmount() != null ? order.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : null)
                .status(PaymentStatus.UNPAID)
                .paymentMethod(method)
                .transactionNote(initialNote)
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Xử lý thanh toán đơn hàng (COD hoặc SIMULATED) và sinh/cập nhật mã giao dịch duy nhất PAY-.
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processPayment(Long orderId, ProcessPaymentRequest request, String userEmail) {
        log.info("Xử lý thanh toán cho đơn hàng ID: {} bởi người dùng: {}", orderId, userEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

            if (user.getRole() != Role.ADMIN && !order.getUser().getId().equals(user.getId())) {
                throw new BusinessException("Bạn không có quyền thanh toán đơn hàng này");
            }
        }

        Long userId = (user != null) ? user.getId() : (order.getUser() != null ? order.getUser().getId() : null);

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng này đã được thanh toán thành công trước đó");
        }

        if (request == null) {
            request = ProcessPaymentRequest.builder()
                    .paymentMethod(PaymentMethod.SIMULATED)
                    .simulateSuccess(true)
                    .build();
        }

        ProcessPaymentRequest finalRequest = request;
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Payment p = Payment.builder()
                            .order(order)
                            .transactionCode(Payment.generateTransactionCode())
                            .amount(order.getTotalAmount())
                            .status(PaymentStatus.UNPAID)
                            .paymentMethod(finalRequest.getPaymentMethod() != null ? finalRequest.getPaymentMethod() : PaymentMethod.SIMULATED)
                            .build();
                    order.setPayment(p);
                    return p;
                });

        if (payment.getTransactionCode() == null || payment.getTransactionCode().trim().isEmpty()) {
            payment.setTransactionCode(Payment.generateTransactionCode());
        }

        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : payment.getPaymentMethod();
        if (method == null) {
            method = PaymentMethod.SIMULATED;
        }
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalAmount() != null ? order.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : null);

        if (method == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.UNPAID);
            payment.setPaidAt(null);
            payment.setTransactionNote(request.getTransactionNote() != null && !request.getTransactionNote().trim().isEmpty()
                    ? request.getTransactionNote().trim()
                    : "Thanh toán tiền mặt khi nhận hàng (COD)");
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (method == PaymentMethod.SIMULATED) {
            boolean success = request.isSuccessSimulation();
            if (success) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionNote(request.getTransactionNote() != null && !request.getTransactionNote().trim().isEmpty()
                        ? request.getTransactionNote().trim()
                        : "Thanh toán trực tuyến mô phỏng thành công (PAID)");
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getOrderStatus() == OrderStatus.PENDING) {
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaidAt(null);
                payment.setTransactionNote(request.getTransactionNote() != null && !request.getTransactionNote().trim().isEmpty()
                        ? request.getTransactionNote().trim()
                        : "Thanh toán trực tuyến mô phỏng thất bại (FAILED)");
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        }

        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        Long savedOrderId = order.getId();
        String txCode = savedPayment.getTransactionCode();

        if (savedPayment.getStatus() == PaymentStatus.FAILED) {
            logAfterCommitOrNow(() ->
                log.warn("userId={} | action=PROCESS_PAYMENT | result=FAILURE | orderId={} | transactionCode={}",
                        userId, savedOrderId, txCode)
            );
        } else {
            logAfterCommitOrNow(() ->
                log.info("userId={} | action=PROCESS_PAYMENT | result=SUCCESS | orderId={} | transactionCode={}",
                        userId, savedOrderId, txCode)
            );
        }

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
}
