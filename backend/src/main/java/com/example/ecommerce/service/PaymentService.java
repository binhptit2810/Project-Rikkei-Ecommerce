package com.example.ecommerce.service;

import com.example.ecommerce.config.VNPayConfig;
import com.example.ecommerce.dto.request.CreateVNPayPaymentRequest;
import com.example.ecommerce.dto.request.ProcessPaymentRequest;
import com.example.ecommerce.dto.response.PaymentResponse;
import com.example.ecommerce.dto.response.VNPayIpnResponse;
import com.example.ecommerce.dto.response.VNPayPaymentResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.PaymentTransactionRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this(paymentRepository, orderRepository, userRepository, new VNPayConfig(), null, null);
    }

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, UserRepository userRepository,
                          VNPayConfig vnPayConfig) {
        this(paymentRepository, orderRepository, userRepository, vnPayConfig, null, null);
    }

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          VNPayConfig vnPayConfig,
                          EmailService emailService) {
        this(paymentRepository, orderRepository, userRepository, vnPayConfig, emailService, null);
    }

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          VNPayConfig vnPayConfig,
                          @Autowired(required = false) EmailService emailService,
                          @Autowired(required = false) PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.vnPayConfig = vnPayConfig != null ? vnPayConfig : new VNPayConfig();
        this.emailService = emailService;
        this.paymentTransactionRepository = paymentTransactionRepository;
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

        Order order = orderRepository.findByIdForUpdate(orderId)
                .or(() -> orderRepository.findById(orderId))
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

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .or(() -> paymentRepository.findByOrderId(orderId))
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

        // Mỗi lần tạo payment URL PHẢI có một mã tham chiếu giao dịch riêng (vnp_TxnRef mới)
        String newTxnRef = Payment.generateTransactionCode();
        payment.setTransactionCode(newTxnRef);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.UNPAID);
        payment.setAmount(order.getTotalAmount() != null ? order.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        payment.setTransactionNote("Chờ khách hàng hoàn tất thanh toán trên Cổng VNPay Sandbox");
        Payment savedPayment = paymentRepository.save(payment);

        if (paymentTransactionRepository != null) {
            PaymentTransaction attempt = PaymentTransaction.builder()
                    .payment(savedPayment)
                    .order(order)
                    .txnRef(newTxnRef)
                    .amount(savedPayment.getAmount())
                    .bankCode(request.getBankCode())
                    .paymentMethod(PaymentMethod.VNPAY)
                    .status(PaymentStatus.UNPAID)
                    .transactionNote("Khởi tạo yêu cầu thanh toán VNPay Sandbox")
                    .build();
            paymentTransactionRepository.save(attempt);
        }

        // Chuẩn bị tham số VNPay 2.1.0
        BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        long vnpAmount = orderAmount.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion() != null ? vnPayConfig.getVersion() : "2.1.0");
        vnpParams.put("vnp_Command", vnPayConfig.getCommand() != null ? vnPayConfig.getCommand() : "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode() != null ? vnPayConfig.getCurrCode() : "VND");
        vnpParams.put("vnp_TxnRef", newTxnRef);

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
     * Xử lý callback trả về từ VNPay (Return URL trên trình duyệt khách hàng).
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processVNPayCallback(Map<String, String> queryParams) {
        HttpServletRequest currentRequest = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                currentRequest = attributes.getRequest();
            }
        } catch (Exception ex) {
            log.debug("Không thể lấy HttpServletRequest từ RequestContextHolder: {}", ex.getMessage());
        }
        return processVNPayCallback(queryParams, currentRequest);
    }

    /**
     * Xử lý callback trả về từ VNPay (Return URL trên trình duyệt khách hàng) có kiểm tra HPP.
     */
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processVNPayCallback(Map<String, String> queryParams, HttpServletRequest httpRequest) {
        log.info("Xử lý callback trả về từ VNPay (Return URL): {}", queryParams);

        if (queryParams == null || queryParams.isEmpty()) {
            throw new BusinessException("Tham số callback không hợp lệ hoặc rỗng");
        }

        // 1. Kiểm tra Parameter Pollution (HPP)
        if (httpRequest != null && VNPayUtil.hasDuplicateVnpParams(httpRequest)) {
            log.warn("Từ chối Return URL do phát hiện HTTP Parameter Pollution (HPP)");
            throw new BusinessException("Phát hiện HTTP Parameter Pollution (HPP)");
        }

        // 2. Xác thực chữ ký HMAC-SHA512
        if (!VNPayUtil.verifyCallback(queryParams, vnPayConfig.getHashSecret())) {
            log.error("Xác thực chữ ký bảo mật VNPay thất bại! Dữ liệu có thể đã bị can thiệp.");
            throw new BusinessException("Chữ ký bảo mật VNPay không hợp lệ (Invalid SecureHash)");
        }

        String vnpTxnRef = queryParams.get("vnp_TxnRef");
        String vnpResponseCode = queryParams.get("vnp_ResponseCode");
        String vnpTransactionNo = queryParams.get("vnp_TransactionNo");
        String vnpTransactionStatus = queryParams.get("vnp_TransactionStatus");
        String vnpTmnCode = queryParams.get("vnp_TmnCode");
        String vnpAmountStr = queryParams.get("vnp_Amount");
        String vnpBankCode = queryParams.get("vnp_BankCode");

        // 3. Kiểm tra mã Merchant
        if (vnPayConfig.getTmnCode() != null && vnpTmnCode != null && !vnPayConfig.getTmnCode().equals(vnpTmnCode)) {
            log.warn("Mã merchant không khớp: expected={}, received={}", vnPayConfig.getTmnCode(), vnpTmnCode);
            throw new BusinessException("Mã định danh Merchant không khớp");
        }

        if (vnpTxnRef == null || vnpTxnRef.isBlank()) {
            throw new BusinessException("Mã giao dịch vnp_TxnRef không được để trống");
        }

        // 4. Tìm kiếm Attempt và thực hiện khóa bi quan theo thứ tự nhất quán: Attempt -> Order -> Payment
        PaymentTransaction tx = null;
        if (paymentTransactionRepository != null) {
            tx = paymentTransactionRepository.findByTxnRefForUpdate(vnpTxnRef).orElse(null);
        }

        Payment payment = null;
        Order order = null;
        if (tx != null) {
            final Long orderId = tx.getOrder().getId();
            order = orderRepository.findByIdForUpdate(orderId).orElse(tx.getOrder());
            payment = paymentRepository.findByOrderIdForUpdate(orderId)
                    .or(() -> paymentRepository.findByOrderId(orderId))
                    .orElse(tx.getPayment());
        } else {
            payment = paymentRepository.findByTransactionCodeForUpdate(vnpTxnRef)
                    .or(() -> paymentRepository.findByTransactionCode(vnpTxnRef))
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với mã: " + vnpTxnRef));
            if (payment.getOrder() != null) {
                order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElse(payment.getOrder());
            }
        }

        if (order == null) {
            throw new BusinessException("Không tìm thấy thông tin đơn hàng gắn với giao dịch");
        }

        // 5. Kiểm tra đối soát số tiền vnp_Amount
        if (vnpAmountStr != null && !vnpAmountStr.isBlank()) {
            long receivedVnpAmount;
            try {
                receivedVnpAmount = Long.parseLong(vnpAmountStr);
            } catch (NumberFormatException e) {
                throw new BusinessException("vnp_Amount không phải là số hợp lệ");
            }

            BigDecimal expectedAmountBd = (tx != null && tx.getAmount() != null)
                    ? tx.getAmount()
                    : (payment.getAmount() != null ? payment.getAmount() : (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO));
            long expectedVnpAmount = expectedAmountBd.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
            if (expectedVnpAmount != receivedVnpAmount) {
                log.warn("Số tiền Return URL không khớp: expected={}, received={}", expectedVnpAmount, receivedVnpAmount);
                throw new BusinessException("Số tiền thanh toán không khớp với đơn hàng");
            }
        }

        // 6. Kiểm tra trạng thái giao dịch: vnp_TransactionStatus nếu có phải bằng "00"
        if (vnpTransactionStatus != null && !"00".equals(vnpTransactionStatus)) {
            log.warn("Giao dịch VNPay không thành công tại cổng: transactionStatus={}", vnpTransactionStatus);
            if (tx != null) {
                tx.setStatus(PaymentStatus.FAILED);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus);
                tx.setTransactionNote("Thanh toán thất bại qua VNPay. Trạng thái giao dịch: " + vnpTransactionStatus);
                paymentTransactionRepository.save(tx);
            }
            if (payment.getStatus() != PaymentStatus.PAID && vnpTxnRef.equals(payment.getTransactionCode())) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setTransactionNote("Thanh toán thất bại qua VNPay. Trạng thái giao dịch: " + vnpTransactionStatus);
                paymentRepository.save(payment);
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
            }
            return mapToPaymentResponse(payment);
        }

        // 7. Idempotency: nếu Payment đã PAID
        if (payment.getStatus() == PaymentStatus.PAID) {
            if (!vnpTxnRef.equals(payment.getTransactionCode())) {
                // Double payment!
                log.warn("Phát hiện thanh toán trùng qua Return URL cho đơn hàng #{}: primaryTxnRef={}, duplicateTxnRef={}",
                        order.getId(), payment.getTransactionCode(), vnpTxnRef);
                if (tx != null && tx.getStatus() != PaymentStatus.PAID) {
                    tx.setStatus(PaymentStatus.PAID);
                    tx.setVnpTransactionNo(vnpTransactionNo);
                    tx.setResponseCode(vnpResponseCode);
                    tx.setTransactionStatus(vnpTransactionStatus != null ? vnpTransactionStatus : "00");
                    tx.setBankCode(vnpBankCode);
                    tx.setReconciliationStatus(ReconciliationStatus.OVERPAYMENT);
                    tx.setTransactionNote("OVERPAYMENT: Khoản thu trùng cho đơn hàng đã thanh toán. Cần hoàn tiền thừa. Mã GD VNPay: " + vnpTransactionNo);
                    paymentTransactionRepository.save(tx);
                }
            }
            log.info("Giao dịch {} cho đơn hàng #{} đã được xác nhận PAID trước đó.", vnpTxnRef, order.getId());
            return mapToPaymentResponse(payment);
        }

        // 8. Đơn hàng đã bị CANCELLED trước đó
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.warn("Đơn hàng #{} đã bị hủy trước khi nhận callback từ VNPay.", order.getId());
            if (tx != null) {
                tx.setVnpTransactionNo(vnpTransactionNo);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus != null ? vnpTransactionStatus : "00");
                tx.setBankCode(vnpBankCode);
                tx.setTransactionNote("Return URL: Đơn hàng đã bị hủy trước khi nhận callback. Chờ IPN đối soát.");
                paymentTransactionRepository.save(tx);
            }
            return mapToPaymentResponse(payment);
        }

        boolean isSuccess = "00".equals(vnpResponseCode) && (vnpTransactionStatus == null || "00".equals(vnpTransactionStatus));
        if (isSuccess) {
            if (tx != null) {
                tx.setVnpTransactionNo(vnpTransactionNo);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus != null ? vnpTransactionStatus : "00");
                tx.setBankCode(vnpBankCode);
                tx.setTransactionNote("Return URL: Khách hàng hoàn tất thao tác cổng VNPay. Chờ IPN xác nhận chính thức.");
                paymentTransactionRepository.save(tx);
            }

            // Return URL chỉ phục vụ hiển thị kết quả, KHÔNG tự ý xác nhận thanh toán tài chính (không set PAID, không CONFIRMED, không gửi email)
            // IPN Webhook là nguồn xác nhận thanh toán cuối cùng duy nhất!
            log.info("Return URL tiếp nhận kết quả thành công cho txnRef={} của đơn hàng #{}. Giữ nguyên trạng thái để IPN xác nhận.",
                    vnpTxnRef, order.getId());
        } else {
            if (tx != null) {
                tx.setStatus(PaymentStatus.FAILED);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus);
                tx.setTransactionNote("Return URL: Thanh toán thất bại hoặc người dùng hủy giao dịch qua VNPay. Mã phản hồi: " + vnpResponseCode);
                paymentTransactionRepository.save(tx);
            }
            log.warn("Return URL ghi nhận thanh toán VNPay THẤT BẠI cho attempt {}, mã phản hồi: {}", vnpTxnRef, vnpResponseCode);
        }

        return mapToPaymentResponse(payment);
    }

    /**
     * Xử lý webhook IPN (Instant Payment Notification) từ máy chủ VNPay truyền về.
     * Đáp ứng chuẩn phản hồi VNPay: {"RspCode": "xx", "Message": "xxx"} (HTTP 200 OK).
     *
     * @param queryParams Toàn bộ tham số nhận được trong query string từ VNPay
     * @param httpRequest HttpServletRequest phục vụ kiểm tra Parameter Pollution
     * @return VNPayIpnResponse chứa RspCode và Message chuẩn đặc tả VNPay
     */
    @Transactional(rollbackFor = Exception.class)
    public VNPayIpnResponse processVNPayIpn(Map<String, String> queryParams, HttpServletRequest httpRequest) {
        log.info("Bắt đầu tiếp nhận và xử lý VNPay IPN Webhook...");

        if (queryParams == null || queryParams.isEmpty()) {
            log.warn("IPN request không chứa tham số nào");
            return VNPayIpnResponse.invalidChecksum();
        }

        // 1. Kiểm tra Parameter Pollution (HPP)
        if (httpRequest != null && VNPayUtil.hasDuplicateVnpParams(httpRequest)) {
            log.warn("Từ chối IPN do phát hiện HTTP Parameter Pollution (HPP)");
            return VNPayIpnResponse.invalidChecksum();
        }

        // 2. Xác thực chữ ký HMAC-SHA512
        if (!VNPayUtil.verifyCallback(queryParams, vnPayConfig.getHashSecret())) {
            log.warn("Xác thực chữ ký IPN thất bại! Chữ ký không hợp lệ hoặc dữ liệu bị sửa đổi");
            return VNPayIpnResponse.invalidChecksum();
        }

        // 3. Trích xuất các tham số trọng yếu
        String vnpTxnRef = queryParams.get("vnp_TxnRef");
        String vnpAmountStr = queryParams.get("vnp_Amount");
        String vnpResponseCode = queryParams.get("vnp_ResponseCode");
        String vnpTransactionStatus = queryParams.get("vnp_TransactionStatus");
        String vnpTransactionNo = queryParams.get("vnp_TransactionNo");
        String vnpTmnCode = queryParams.get("vnp_TmnCode");
        String vnpBankCode = queryParams.get("vnp_BankCode");
        String vnpPayDate = queryParams.get("vnp_PayDate");

        // Kiểm tra mã merchant (vnp_TmnCode)
        if (vnPayConfig.getTmnCode() != null && !vnPayConfig.getTmnCode().equals(vnpTmnCode)) {
            log.warn("Sai mã định danh Merchant: expected={}, received={}", vnPayConfig.getTmnCode(), vnpTmnCode);
            return VNPayIpnResponse.invalidChecksum();
        }

        if (vnpTxnRef == null || vnpTxnRef.isBlank()) {
            log.warn("Thiếu tham số vnp_TxnRef trong IPN request");
            return VNPayIpnResponse.orderNotFound();
        }

        // 4. Tìm kiếm Payment Attempt và khóa bi quan theo thứ tự nhất quán: Attempt -> Order -> Payment
        PaymentTransaction tx = null;
        if (paymentTransactionRepository != null) {
            tx = paymentTransactionRepository.findByTxnRefForUpdate(vnpTxnRef).orElse(null);
        }

        Payment payment = null;
        Order order = null;
        if (tx != null) {
            final Long orderId = tx.getOrder().getId();
            order = orderRepository.findByIdForUpdate(orderId).orElse(tx.getOrder());
            payment = paymentRepository.findByOrderIdForUpdate(orderId)
                    .or(() -> paymentRepository.findByOrderId(orderId))
                    .orElse(tx.getPayment());
        } else {
            payment = paymentRepository.findByTransactionCodeForUpdate(vnpTxnRef)
                    .or(() -> paymentRepository.findByTransactionCode(vnpTxnRef))
                    .orElse(null);
            if (payment != null && payment.getOrder() != null) {
                order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElse(payment.getOrder());
            }
        }

        if (payment == null || order == null) {
            log.warn("Không tìm thấy giao dịch thanh toán hoặc đơn hàng trong hệ thống với mã: {}", vnpTxnRef);
            return VNPayIpnResponse.orderNotFound();
        }

        // 5. Đối soát số tiền thanh toán (Amount Verification)
        if (vnpAmountStr == null || vnpAmountStr.isBlank()) {
            log.warn("Thiếu tham số vnp_Amount trong IPN request");
            return VNPayIpnResponse.invalidAmount();
        }

        long receivedVnpAmount;
        try {
            receivedVnpAmount = Long.parseLong(vnpAmountStr);
        } catch (NumberFormatException e) {
            log.warn("vnp_Amount không phải là số hợp lệ: {}", vnpAmountStr);
            return VNPayIpnResponse.invalidAmount();
        }

        BigDecimal expectedAmountBd = (tx != null && tx.getAmount() != null)
                ? tx.getAmount()
                : (payment.getAmount() != null ? payment.getAmount() : (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO));

        long expectedVnpAmount = expectedAmountBd.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValue();

        if (expectedVnpAmount != receivedVnpAmount) {
            log.warn("Số tiền IPN không khớp! Expected={} ({} VND), Received={}",
                    expectedVnpAmount, expectedAmountBd, receivedVnpAmount);
            return VNPayIpnResponse.invalidAmount();
        }

        // 6. Kiểm tra Idempotency cho chính Attempt này (IPN duplicate cùng transaction)
        if (tx != null && tx.getStatus() == PaymentStatus.PAID) {
            log.info("Giao dịch attempt {} đã được xác nhận thành công trước đó (Idempotent catch)", vnpTxnRef);
            return VNPayIpnResponse.alreadyConfirmed();
        }
        if (tx == null && payment.getStatus() == PaymentStatus.PAID && vnpTxnRef.equals(payment.getTransactionCode())) {
            log.info("Giao dịch payment {} đã được xác nhận thành công trước đó (Idempotent catch)", vnpTxnRef);
            return VNPayIpnResponse.alreadyConfirmed();
        }

        // 7. Đánh giá kết quả giao dịch thanh toán
        boolean isSuccess = "00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus);

        if (isSuccess) {
            // A. Đơn hàng đã bị CANCELLED nhưng khách hàng thanh toán thành công
            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.warn("Đơn hàng #{} đã ở trạng thái CANCELLED nhưng nhận được IPN thanh toán thành công (txnRef={}).",
                        order.getId(), vnpTxnRef);

                if (tx != null) {
                    tx.setStatus(PaymentStatus.PAID);
                    tx.setVnpTransactionNo(vnpTransactionNo);
                    tx.setResponseCode(vnpResponseCode);
                    tx.setTransactionStatus(vnpTransactionStatus != null ? vnpTransactionStatus : "00");
                    tx.setBankCode(vnpBankCode);
                    tx.setReconciliationStatus(ReconciliationStatus.REFUND_PENDING);
                    tx.setTransactionNote("Đơn hàng đã hủy trước khi thanh toán. Cần đối soát hoàn tiền (REFUND_PENDING). Mã GD VNPay: " + vnpTransactionNo);
                    paymentTransactionRepository.save(tx);
                }

                payment.setStatus(PaymentStatus.PAID);
                payment.setTransactionCode(vnpTxnRef);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionNote("Đơn hàng đã hủy trước khi thanh toán. Cần hoàn tiền cho khách (REFUND_PENDING). Mã GD VNPay: " + vnpTransactionNo);
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);

                logStructuredAudit(order.getId(), payment.getId(), vnpTxnRef, vnpTransactionNo, expectedAmountBd, "REFUND_PENDING");
                return VNPayIpnResponse.success();
            }

            // B. Thanh toán trùng (Double payment / Overpayment): Payment đã PAID bởi một attempt khác
            if (payment.getStatus() == PaymentStatus.PAID && !vnpTxnRef.equals(payment.getTransactionCode())) {
                log.warn("action=VNPAY_IPN | result=OVERPAYMENT_DETECTED | orderId={} | primaryTxnRef={} | duplicateTxnRef={} | vnpTransactionNo={} | amount={}",
                        order.getId(), payment.getTransactionCode(), vnpTxnRef, vnpTransactionNo, expectedAmountBd);

                if (tx != null) {
                    tx.setStatus(PaymentStatus.PAID);
                    tx.setVnpTransactionNo(vnpTransactionNo);
                    tx.setResponseCode(vnpResponseCode);
                    tx.setTransactionStatus(vnpTransactionStatus);
                    tx.setBankCode(vnpBankCode);
                    tx.setReconciliationStatus(ReconciliationStatus.OVERPAYMENT);
                    tx.setTransactionNote("OVERPAYMENT: Khoản thu trùng cho đơn hàng đã thanh toán (mã GD chính: " + payment.getTransactionCode() + "). Cần hoàn tiền thừa. Mã GD VNPay: " + vnpTransactionNo);
                    paymentTransactionRepository.save(tx);
                }

                // Không ghi đè transactionCode chính trên Payment
                // Không gửi email xác nhận lần thứ hai
                // Không cập nhật lại OrderStatus
                logStructuredAudit(order.getId(), payment.getId(), vnpTxnRef, vnpTransactionNo, expectedAmountBd, "OVERPAYMENT");
                return VNPayIpnResponse.success();
            }

            // C. Thành công hợp lệ (Giao dịch chính)
            if (tx != null) {
                tx.setStatus(PaymentStatus.PAID);
                tx.setVnpTransactionNo(vnpTransactionNo);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus);
                tx.setBankCode(vnpBankCode);
                tx.setReconciliationStatus(ReconciliationStatus.NONE);
                tx.setTransactionNote("Thanh toán thành công qua VNPay Sandbox. Mã GD VNPay: " + vnpTransactionNo);
                paymentTransactionRepository.save(tx);
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionCode(vnpTxnRef);
            payment.setTransactionNote("Thanh toán thành công qua VNPay Sandbox. Mã GD VNPay: " + vnpTransactionNo);
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getOrderStatus() == OrderStatus.PENDING) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);

            log.info("Xác nhận IPN thanh toán VNPay THÀNH CÔNG cho đơn hàng #{}, txnRef={}, mã GD VNPay: {}",
                    order.getId(), vnpTxnRef, vnpTransactionNo);

            logStructuredAudit(order.getId(), payment.getId(), vnpTxnRef, vnpTransactionNo, expectedAmountBd, "SUCCESS");
            sendOrderConfirmationEmailSafe(order);
        } else {
            // Giao dịch thanh toán thất bại tại cổng VNPay
            if (tx != null) {
                tx.setStatus(PaymentStatus.FAILED);
                tx.setResponseCode(vnpResponseCode);
                tx.setTransactionStatus(vnpTransactionStatus);
                tx.setTransactionNote("Thanh toán VNPay thất bại hoặc bị hủy. Mã phản hồi: " + vnpResponseCode);
                paymentTransactionRepository.save(tx);
            }

            // AUDIT-02 FIX:
            // 1. Nếu payment đã PAID (bởi bất kỳ attempt nào) -> TUYỆT ĐỐI không đổi sang FAILED.
            // 2. Kiểm tra xem có attempt nào khác của đơn hàng này đang chờ thanh toán (UNPAID) hay không.
            boolean hasOtherPendingAttempt = false;
            if (paymentTransactionRepository != null && tx != null && tx.getOrder() != null) {
                List<PaymentTransaction> attempts = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(tx.getOrder().getId());
                if (attempts != null) {
                    for (PaymentTransaction other : attempts) {
                        if (!vnpTxnRef.equals(other.getTxnRef()) && other.getStatus() == PaymentStatus.UNPAID) {
                            hasOtherPendingAttempt = true;
                            break;
                        }
                    }
                }
            }

            if (payment.getStatus() != PaymentStatus.PAID) {
                if (hasOtherPendingAttempt) {
                    log.info("Attempt cũ {} thất bại nhưng đơn hàng còn attempt khác đang UNPAID/chờ thanh toán. Giữ nguyên trạng thái Payment/Order.", vnpTxnRef);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setTransactionCode(vnpTxnRef);
                    payment.setTransactionNote("Giao dịch thanh toán VNPay thất bại. Mã phản hồi: " + vnpResponseCode);
                    paymentRepository.save(payment);

                    order.setPaymentStatus(PaymentStatus.FAILED);
                    orderRepository.save(order);
                }
            } else {
                log.info("Bỏ qua cập nhật FAILED cho Payment (status={}, currentTxnRef={}) vì payment đã PAID",
                        payment.getStatus(), payment.getTransactionCode());
            }

            log.warn("Giao dịch VNPay THẤT BẠI cho đơn hàng #{}, txnRef={}, mã phản hồi: {}",
                    order.getId(), vnpTxnRef, vnpResponseCode);

            logStructuredAudit(order.getId(), payment.getId(), vnpTxnRef, vnpTransactionNo, expectedAmountBd, "FAILURE");
        }

        return VNPayIpnResponse.success();
    }

    private void sendOrderConfirmationEmailSafe(Order order) {
        if (emailService != null && order.getUser() != null && order.getUser().getEmail() != null) {
            try {
                emailService.sendOrderConfirmationEmail(order.getUser().getEmail(), order.getRecipientName(), order);
            } catch (Exception ex) {
                log.warn("Không thể gửi email xác nhận thanh toán: {}", ex.getMessage());
            }
        }
    }

    private void logStructuredAudit(Long orderId, Long paymentId, String txnRef, String vnpTransactionNo, BigDecimal amount, String result) {
        logAfterCommitOrNow(() ->
            log.info("action=VNPAY_IPN | result={} | orderId={} | paymentId={} | txnRef={} | vnpTransactionNo={} | amount={}",
                    result, orderId, paymentId, txnRef, vnpTransactionNo, amount)
        );
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
