package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.InventoryTransactionRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý tồn kho (Inventory).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4)
 *
 * Chức năng:
 * - Trừ kho khi đặt hàng (deductStockForOrder) với cơ chế khóa bi quan chống overselling.
 * - Hoàn kho khi hủy đơn (restoreStockForOrder).
 * - Admin điều chỉnh kho thủ công (adjustStock): tăng (+), giảm (-), lưu nhật ký MANUAL_ADJUST.
 * - Truy vấn lịch sử biến động kho theo sản phẩm hoặc toàn hệ thống.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * Trừ số lượng tồn kho cho các sản phẩm trong đơn hàng.
     * Áp dụng khóa bi quan (PESSIMISTIC_WRITE) để ngăn race condition khi nhiều khách hàng cùng mua.
     */
    @Transactional
    public void deductStockForOrder(Order order) {
        log.info("Bắt đầu trừ tồn kho cho đơn hàng: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            Product product = findProductForUpdateOrRegular(productId);

            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Sản phẩm '{}' không đủ tồn kho: hiện còn {}, yêu cầu {}",
                        product.getName(), product.getStockQuantity(), item.getQuantity());
                throw new BusinessException(String.format(
                        "Sản phẩm '%s' không đủ số lượng tồn kho (hiện còn %d, yêu cầu %d)",
                        product.getName(), product.getStockQuantity(), item.getQuantity()
                ));
            }

            int stockAfter = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(stockAfter);
            productRepository.save(product);

            // Ghi nhật ký biến động kho
            InventoryTransaction transaction = InventoryTransaction.builder()
                    .product(product)
                    .changeQuantity(-item.getQuantity())
                    .stockAfter(stockAfter)
                    .transactionType(InventoryTransactionType.ORDER_PLACED)
                    .orderId(order.getId())
                    .note("Trừ tồn kho khi tạo đơn hàng " + order.getOrderCode())
                    .build();

            inventoryTransactionRepository.save(transaction);

            log.info("Đã trừ {} sản phẩm '{}', tồn kho còn lại: {}",
                    item.getQuantity(), product.getName(), stockAfter);
        }
    }

    /**
     * Hoàn trả số lượng tồn kho khi đơn hàng bị hủy.
     */
    @Transactional
    public void restoreStockForOrder(Order order) {
        log.info("Bắt đầu hoàn trả tồn kho cho đơn hàng bị hủy: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            Product product = findProductForUpdateOrRegular(productId);

            int stockAfter = product.getStockQuantity() + item.getQuantity();
            product.setStockQuantity(stockAfter);
            productRepository.save(product);

            // Ghi nhật ký hoàn trả kho
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
     * Dành cho Admin: Điều chỉnh tồn kho thủ công (tăng hoặc giảm).
     *
     * @param productId ID của sản phẩm cần điều chỉnh
     * @param request Dữ liệu điều chỉnh (changeQuantity, note)
     * @return InventoryTransactionResponse kết quả giao dịch kho
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransactionResponse adjustStock(Long productId, AdjustInventoryRequest request) {
        if (request.getChangeQuantity() == null || request.getChangeQuantity() == 0) {
            throw new BusinessException("Số lượng điều chỉnh phải khác 0");
        }

        Product product = findProductForUpdateOrRegular(productId);

        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int stockAfter = currentStock + request.getChangeQuantity();

        if (stockAfter < 0) {
            throw new BusinessException(String.format(
                    "Tồn kho sau khi điều chỉnh không thể âm (hiện tại: %d, điều chỉnh: %d, kết quả: %d)",
                    currentStock, request.getChangeQuantity(), stockAfter
            ));
        }

        product.setStockQuantity(stockAfter);
        productRepository.save(product);

        String note = (request.getNote() != null && !request.getNote().trim().isEmpty())
                ? request.getNote().trim()
                : (request.getChangeQuantity() > 0 ? "Admin nhập kho thủ công" : "Admin xuất kho thủ công");

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .changeQuantity(request.getChangeQuantity())
                .stockAfter(stockAfter)
                .transactionType(InventoryTransactionType.MANUAL_ADJUST)
                .note(note)
                .build();

        InventoryTransaction savedTransaction = inventoryTransactionRepository.save(transaction);

        log.info("Admin điều chỉnh kho sản phẩm #{} '{}': biến động {}, tồn kho mới: {}",
                product.getId(), product.getName(), request.getChangeQuantity(), stockAfter);

        return mapToTransactionResponse(savedTransaction);
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
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy toàn bộ lịch sử biến động kho trên toàn hệ thống (sắp xếp mới nhất lên đầu).
     */
    @Transactional(readOnly = true)
    public List<InventoryTransactionResponse> getAllTransactions() {
        return inventoryTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToTransactionResponse)
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
     * Map Entity sang DTO Response
     */
    public InventoryTransactionResponse mapToTransactionResponse(InventoryTransaction tx) {
        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .productId(tx.getProduct() != null ? tx.getProduct().getId() : null)
                .productName(tx.getProduct() != null ? tx.getProduct().getName() : null)
                .changeQuantity(tx.getChangeQuantity())
                .stockAfter(tx.getStockAfter())
                .transactionType(tx.getTransactionType())
                .transactionTypeDescription(tx.getTransactionType() != null ? tx.getTransactionType().getDescription() : null)
                .orderId(tx.getOrderId())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
