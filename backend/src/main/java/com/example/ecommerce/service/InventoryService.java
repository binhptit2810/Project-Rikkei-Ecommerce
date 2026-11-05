package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.InventoryTransactionRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.ecommerce.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service quản lý tồn kho (Inventory Module).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Chức năng:
 * - Trừ kho an toàn có khóa bi quan (PESSIMISTIC_WRITE) khi tạo đơn hàng (TSK-D03-DTB / TSK-D06-DTB).
 * - Hoàn lại số lượng tồn kho khi hủy đơn hàng (TSK-D08-VDHH / TSK-D08-DTB).
 * - Admin điều chỉnh kho thủ công POST /admin/inventory/{id}/adjust (TSK-D07-DTB).
 * - Tra cứu lịch sử biến động kho có bộ lọc GET /admin/inventory/transactions (TSK-D07-DTB).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id", "changeQuantity", "stockAfter");

    /**
     * Trừ tồn kho khi đơn hàng được tạo (Order Placed).
     * Áp dụng Khóa Bi Quan (Pessimistic Write Lock) trên từng bản ghi sản phẩm
     * để chống tranh chấp dữ liệu (Race Condition) và ngăn chặn bán vượt tồn kho (Overselling).
     */
    @Transactional(rollbackFor = Exception.class)
    public void deductStockForOrder(Order order) {
        log.info("Bắt đầu trừ tồn kho cho đơn hàng: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();

            Product product = findProductForUpdateOrRegular(productId);

            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            int buyQuantity = item.getQuantity();

            if (currentStock < buyQuantity) {
                log.error("Hết hàng hoặc không đủ tồn kho cho sản phẩm #{}: '{}' (tồn: {}, yêu cầu: {})",
                        productId, product.getName(), currentStock, buyQuantity);
                throw new BusinessException(String.format(
                        "Sản phẩm '%s' (mã #%d) không đủ số lượng tồn kho (hiện còn: %d, yêu cầu mua: %d)",
                        product.getName(), productId, currentStock, buyQuantity));
            }

            int stockAfter = currentStock - buyQuantity;
            product.setStockQuantity(stockAfter);
            productRepository.save(product);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .product(product)
                    .changeQuantity(-buyQuantity)
                    .stockAfter(stockAfter)
                    .transactionType(InventoryTransactionType.ORDER_PLACED)
                    .orderId(order.getId())
                    .note("Trừ tồn kho cho đơn hàng " + order.getOrderCode())
                    .build();

            inventoryTransactionRepository.save(transaction);

            log.info("Đã trừ {} sản phẩm '{}', tồn kho mới: {}",
                    buyQuantity, product.getName(), stockAfter);
        }
    }

    /**
     * Hoàn lại số lượng tồn kho khi đơn hàng bị hủy (Order Cancelled).
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreStockForOrder(Order order) {
        log.info("Bắt đầu hoàn tồn kho cho đơn hàng bị hủy: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();

            Product product = findProductForUpdateOrRegular(productId);

            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            int stockAfter = currentStock + item.getQuantity();

            product.setStockQuantity(stockAfter);
            productRepository.save(product);

            InventoryTransaction transaction = InventoryTransaction.builder()
                    .product(product)
                    .changeQuantity(item.getQuantity())
                    .stockAfter(stockAfter)
                    .transactionType(InventoryTransactionType.ORDER_CANCELLED)
                    .orderId(order.getId())
                    .note("Hoàn tồn kho do hủy đơn hàng " + order.getOrderCode())
                    .build();

            inventoryTransactionRepository.save(transaction);

            log.info("Đã hoàn {} sản phẩm '{}', tồn kho mới: {}",
                    item.getQuantity(), product.getName(), stockAfter);
        }
    }

    /**
     * Admin điều chỉnh kho thủ công (TSK-D07-DTB).
     * - Khóa bi quan chống Lost Update / Race Condition.
     * - Hỗ trợ nhập kho (MANUAL_IMPORT) hoặc xuất kho (MANUAL_EXPORT).
     * - Chặn điều chỉnh xuất quá số lượng tồn hiện có (không cho stock âm).
     * - Ghi nhận lý do điều chỉnh đầy đủ vào nhật ký kho.
     *
     * @param productId ID sản phẩm cần điều chỉnh
     * @param request DTO chứa changeQuantity, type (nếu có), reason/note
     * @return DTO kết quả giao dịch kho
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransactionResponse adjustStock(Long productId, AdjustInventoryRequest request) {
        Long adminUserId = resolveCurrentAdminUserId();
        return adjustStock(productId, request, adminUserId);
    }

    /**
     * Admin điều chỉnh kho thủ công với định danh Admin ID rõ ràng.
     *
     * @param productId ID sản phẩm cần điều chỉnh
     * @param request DTO chứa changeQuantity, type (nếu có), reason/note
     * @param adminUserId ID của Admin thực hiện thao tác
     * @return DTO kết quả giao dịch kho
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransactionResponse adjustStock(Long productId, AdjustInventoryRequest request, Long adminUserId) {
        if (adminUserId == null) {
            adminUserId = resolveCurrentAdminUserId();
        }

        if (request.getChangeQuantity() == null || request.getChangeQuantity() == 0) {
            log.warn("userId={} | action=ADJUST_INVENTORY | result=FAILURE | productId={} | reason={}",
                    adminUserId, productId, "Số lượng điều chỉnh phải khác 0");
            throw new BusinessException("Số lượng điều chỉnh phải khác 0");
        }

        // Tính toán số lượng biến động hiệu dụng (effectiveChange)
        int effectiveChange = request.getChangeQuantity();
        if (request.getType() == InventoryTransactionType.MANUAL_EXPORT && effectiveChange > 0) {
            effectiveChange = -effectiveChange;
        } else if (request.getType() == InventoryTransactionType.MANUAL_IMPORT && effectiveChange < 0) {
            effectiveChange = Math.abs(effectiveChange);
        }

        // Xác định loại giao dịch cụ thể
        InventoryTransactionType transactionType;
        if (effectiveChange > 0) {
            transactionType = InventoryTransactionType.MANUAL_IMPORT;
        } else {
            transactionType = InventoryTransactionType.MANUAL_EXPORT;
        }

        // Khóa bi quan hàng sản phẩm để chống tranh chấp đồng thời
        Product product;
        try {
            product = findProductForUpdateOrRegular(productId);
        } catch (ResourceNotFoundException e) {
            log.warn("userId={} | action=ADJUST_INVENTORY | result=FAILURE | productId={} | reason={}",
                    adminUserId, productId, sanitize(e.getMessage()));
            throw e;
        }

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int stockAfter = currentStock + effectiveChange;

        // Chặn điều chỉnh xuất quá số lượng tồn hiện có
        if (stockAfter < 0) {
            String reason = String.format(
                    "Không thể xuất kho vượt quá số lượng tồn hiện có (Hiện tồn: %d, yêu cầu xuất: %d)",
                    currentStock, Math.abs(effectiveChange));
            log.warn("userId={} | action=ADJUST_INVENTORY | result=FAILURE | productId={} | quantityBefore={} | reason={}",
                    adminUserId, productId, currentStock, sanitize(reason));
            throw new BusinessException(reason);
        }

        // Cập nhật tồn kho sản phẩm
        product.setStockQuantity(stockAfter);
        productRepository.save(product);

        // Chuẩn hóa lý do điều chỉnh
        String reason = request.getEffectiveReason();
        if (reason == null || reason.isEmpty()) {
            reason = (effectiveChange > 0)
                    ? "Admin nhập kho thủ công (+" + effectiveChange + ")"
                    : "Admin xuất kho thủ công (" + effectiveChange + ")";
        }

        // Lưu nhật ký biến động kho
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .changeQuantity(effectiveChange)
                .stockAfter(stockAfter)
                .transactionType(transactionType)
                .note(reason)
                .build();

        InventoryTransaction savedTransaction = inventoryTransactionRepository.save(transaction);

        int quantityBefore = currentStock;
        int quantityAfter = stockAfter;
        Long pid = product.getId();
        Long finalAdminId = adminUserId;

        logAfterCommitOrNow(() ->
            log.info("userId={} | action=ADJUST_INVENTORY | result=SUCCESS | productId={} | quantityBefore={} | quantityAfter={}",
                    finalAdminId, pid, quantityBefore, quantityAfter)
        );

        return InventoryTransactionResponse.fromEntity(savedTransaction);
    }

    /**
     * Tra cứu lịch sử biến động kho có bộ lọc và phân trang (TSK-D07-DTB).
     *
     * @param productId ID sản phẩm (tùy chọn)
     * @param type Loại giao dịch kho (tùy chọn)
     * @param startDate Ngày bắt đầu (tùy chọn)
     * @param endDate Ngày kết thúc (tùy chọn)
     * @param page Số trang (0-indexed)
     * @param size Số phần tử mỗi trang
     * @param sortBy Trường sắp xếp
     * @param sortDir Hướng sắp xếp (asc / desc)
     * @return PageResponse chứa danh sách InventoryTransactionResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactions(
            Long productId,
            InventoryTransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);

        Page<InventoryTransaction> transactionPage = inventoryTransactionRepository.findWithFilters(
                productId,
                type,
                startDate,
                endDate,
                pageable
        );

        return PageResponse.from(transactionPage, InventoryTransactionResponse::fromEntity);
    }

    /**
     * Lấy toàn bộ lịch sử biến động kho của một sản phẩm.
     */
    @Transactional(readOnly = true)
    public List<InventoryTransactionResponse> getTransactionsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + productId);
        }

        return inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(InventoryTransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Tìm sản phẩm hỗ trợ khóa bi quan, nếu không tìm thấy qua khóa thì fallback tìm thông thường.
     */
    private Product findProductForUpdateOrRegular(Long productId) {
        try {
            return productRepository.findByIdForUpdate(productId)
                    .orElseGet(() -> productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + productId)));
        } catch (Exception e) {
            return productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + productId));
        }
    }

    /**
     * Tạo đối tượng Pageable chuẩn hóa.
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, validSortBy));
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

    private static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[\\r\\n]+", " ").trim();
    }

    private Long resolveCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }
        return null;
    }
}
