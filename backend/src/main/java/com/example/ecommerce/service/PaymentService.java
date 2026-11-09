package com.example.ecommerce.service;

import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.dto.request.CreateVNPayPaymentRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.dto.response.VNPayPaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.util.VNPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service quản lý thanh toán (Payment Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Cao Chí Thiện (CCT)
 *
 * Xử lý thanh toán COD, cổng thanh toán trực tuyến mô phỏng SIMULATED và Cổng thanh toán VNPay Sandbox.
 * Mọi giao dịch đều được cấp mã định danh duy nhất có tiền tố PAY- (định dạng PAY-YYYYMMDD-XXXXXXXX).
 */
@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VNPayConfig vnPayConfig;
    private final EmailService emailService;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this(paymentRepository, orderRepository, userRepository, new VNPayConfig(), null);
    }

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, UserRepository userRepository,
                          VNPayConfig vnPayConfig) {
        this(paymentRepository, orderRepository, userRepository, vnPayConfig, null);
    }

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          VNPayConfig vnPayConfig,
                          @Autowired(required = false) EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.vnPayConfig = vnPayConfig != null ? vnPayConfig : new VNPayConfig();
        this.emailService = emailService;
    }

    /**
     * Khởi tạo bản ghi thanh toán ban đầu cho đơn hàng (COD / SIMULATED / VNPAY - UNPAID).
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createInitialPayment(Order order, PaymentMethod paymentMethod) {
        log.info("Khởi tạo bản ghi thanh toán cho đơn hàng {}", order.getOrderCode());

        PaymentMethod method = paymentMethod != null ? paymentMethod : PaymentMethod.COD;
        String initialNote;
        if (method == PaymentMethod.SIMULATED) {
            initialNote = "Chờ thanh toán qua cổng mô phỏng (SIMULATED)";
        } else if (method == PaymentMethod.VNPAY) {
            initialNote = "Chờ thanh toán qua Cổng VNPay Sandbox";
        } else {
            initialNote = "Thanh toán tiền mặt khi nhận hàng (COD)";
        }

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
     * Khởi tạo yêu cầu thanh toán VNPay và sinh URL chuyển hướng cổng thanh toán Sandbox.
     *
     * @param request   Chứa ID đơn hàng và thông tin bổ sung (ngân hàng, ghi chú)
     * @param clientIp  Địa chỉ IP của người dùng gửi yêu cầu
     * @param userEmail Email của người dùng đăng nhập hiện tại
     * @return VNPayPaymentResponse chứa URL thanh toán và mã tham chiếu giao dịch
     */
    @Transactional(rollbackFor = Exception.class)
    public VNPayPaymentResponse createVNPayPayment(CreateVNPayPaymentRequest request, String clientIp, String userEmail) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException("Vui lòng cung cấp mã đơn hàng cần thanh toán VNPay");
        }

        Long orderId = request.getOrderId();
        log.info("Khởi tạo thanh toán VNPay cho đơn hàng ID: {} bởi người dùng: {}", orderId, userEmail);

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

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Không thể thanh toán cho đơn hàng đã bị hủy");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng này đã được thanh toán thành công trước đó");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Payment p = Payment.builder()
                            .order(order)
                            .transactionCode(Payment.generateTransactionCode())
                            .amount(order.getTotalAmount())
                            .status(PaymentStatus.UNPAID)
                            .paymentMethod(PaymentMethod.VNPAY)
                            .transactionNote("Khởi tạo yêu cầu thanh toán VNPay Sandbox")
                            .build();
                    order.setPayment(p);
                    return p;
                });

        if (payment.getTransactionCode() == null || payment.getTransactionCode().trim().isEmpty()) {
            payment.setTransactionCode(Payment.generateTransactionCode());
        }

        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAmount(order.getTotalAmount() != null ? order.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        payment.setTransactionNote("Chờ khách hàng hoàn tất thanh toán trên Cổng VNPay Sandbox");
        paymentRepository.save(payment);

        // Chuẩn bị tham số VNPay 2.1.0
        BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        long vnpAmount = orderAmount.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion() != null ? vnPayConfig.getVersion() : "2.1.0");
        vnpParams.put("vnp_Command", vnPayConfig.getCommand() != null ? vnPayConfig.getCommand() : "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode() != null ? vnPayConfig.getCurrCode() : "VND");
        vnpParams.put("vnp_TxnRef", payment.getTransactionCode());

        String orderInfo = (request.getOrderInfo() != null && !request.getOrderInfo().trim().isEmpty())
                ? request.getOrderInfo().trim()
                : "Thanh toan don hang #" + (order.getOrderCode() != null ? order.getOrderCode() : order.getId());
        vnpParams.put("vnp_OrderInfo", orderInfo);

        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType() != null ? vnPayConfig.getOrderType() : "other");
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale() != null ? vnPayConfig.getLocale() : "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        vnpParams.put("vnp_CreateDate", formatter.format(now));

        // Hạn thanh toán: 15 phút
        LocalDateTime expireDate = now.plusMinutes(15);
        vnpParams.put("vnp_ExpireDate", formatter.format(expireDate));

        // Ngân hàng nếu có chỉ định (NCB, VNPAYQR, VISA, ...)
        if (request.getBankCode() != null && !request.getBankCode().trim().isEmpty()) {
            vnpParams.put("vnp_BankCode", request.getBankCode().trim());
        }

        String paymentUrl = VNPayUtil.buildPaymentUrl(vnpParams, vnPayConfig.getPayUrl(), vnPayConfig.getHashSecret());
        log.info("Đã sinh URL thanh toán VNPay thành công cho đơn hàng #{}: txnRef={}", orderId, payment.getTransactionCode());

        // Sinh mã QR chuyển khoản tượng trưng (VietQR / VNPAY-QR) cho khách hàng quét thanh toán
        String bankCode = (request.getBankCode() != null && !request.getBankCode().trim().isEmpty())
                ? request.getBankCode().trim()
                : "NCB";
        String bankName = "NCB".equalsIgnoreCase(bankCode)
                ? "Ngân hàng Quốc Dân (NCB)"
                : "Ngân hàng TMCP Ngoại thương (Vietcombank)";
        String accountNumber = "9704198526191432198";
        String accountName = "CONG TY TNHH E-COMMERCE RIKKEI";
        String memo = payment.getTransactionCode() != null ? payment.getTransactionCode() : ("ORD" + order.getId());
        long amountLong = order.getTotalAmount() != null ? order.getTotalAmount().longValue() : 0L;
        String qrCodeUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankCode,
                accountNumber,
                amountLong,
                URLEncoder.encode(memo, StandardCharsets.UTF_8),
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        );

        return VNPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .vnpTxnRef(payment.getTransactionCode())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(order.getTotalAmount())
                .qrCodeUrl(qrCodeUrl)
                .bankCode(bankCode)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .message("Tạo URL thanh toán và mã QR VNPAY thành công")
                .build();
    }

    /**
     * Xử lý callback trả về từ VNPay (kiểm tra chữ ký bảo mật và cập nhật trạng thái đơn hàng).
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processVNPayCallback(Map<String, String> queryParams) {
        log.info("Xử lý callback trả về từ VNPay: {}", queryParams);

        if (!VNPayUtil.verifyCallback(queryParams, vnPayConfig.getHashSecret())) {
            log.error("Xác thực chữ ký bảo mật VNPay thất bại! Dữ liệu có thể đã bị can thiệp.");
            throw new BusinessException("Chữ ký bảo mật VNPay không hợp lệ (Invalid SecureHash)");
        }

        String vnpTxnRef = queryParams.get("vnp_TxnRef");
        String vnpResponseCode = queryParams.get("vnp_ResponseCode");
        String vnpTransactionNo = queryParams.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByTransactionCode(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với mã: " + vnpTxnRef));

        Order order = payment.getOrder();
        if (order == null) {
            throw new BusinessException("Không tìm thấy thông tin đơn hàng gắn với giao dịch");
        }

        boolean isSuccess = "00".equals(vnpResponseCode);
        if (isSuccess) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionNote("Thanh toán thành công qua VNPay Sandbox. Mã GD VNPay: " + vnpTransactionNo);
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            log.info("Xác nhận thanh toán VNPay THÀNH CÔNG cho đơn hàng #{}, mã GD: {}", order.getId(), vnpTxnRef);

            // Gửi email xác nhận thanh toán nếu có emailService
            if (emailService != null && order.getUser() != null && order.getUser().getEmail() != null) {
                try {
                    emailService.sendOrderConfirmationEmail(order.getUser().getEmail(), order.getRecipientName(), order);
                } catch (Exception ex) {
                    log.warn("Không thể gửi email xác nhận thanh toán: {}", ex.getMessage());
                }
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setTransactionNote("Thanh toán thất bại hoặc người dùng hủy giao dịch qua VNPay. Mã phản hồi: " + vnpResponseCode);
            order.setPaymentStatus(PaymentStatus.FAILED);
            log.warn("Thanh toán VNPay THẤT BẠI cho đơn hàng #{}, mã phản hồi: {}", order.getId(), vnpResponseCode);
        }

        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        return mapToPaymentResponse(savedPayment);
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
