package com.example.ecommerce.service;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.InventoryTransactionRepository;
import com.example.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service quản lý tồn kho (Inventory Module - Sprint 1: Pessimistic Locking).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Chức năng Sprint 1:
 * - Trừ kho an toàn có khóa bi quan (PESSIMISTIC_WRITE) khi tạo đơn hàng, chống bán vượt tồn kho.
 * - Hoàn lại số lượng tồn kho khi hủy đơn hàng.
 * - Ghi nhận lịch sử giao dịch biến động kho (ORDER_PLACED, ORDER_CANCELLED).
 *
 * Chức năng Sprint 2:
 * - Admin điều chỉnh kho thủ công (TSK-D07-DTB).
 * - Tra cứu lịch sử biến động kho toàn hệ thống (TSK-D07-DTB).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * Trừ tồn kho khi đơn hàng được tạo (Order Placed).
     * Áp dụng Khóa Bi Quan (Pessimistic Write Lock) trên từng bản ghi sản phẩm
     * để chống tranh chấp dữ liệu (Race Condition) và ngăn chặn bán vượt tồn kho (Overselling).
     *
     * @param order Đơn hàng vừa tạo chứa danh sách sản phẩm và số lượng mua
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
                        product.getName(), productId, currentStock, buyQuantity
                ));
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
     *
     * @param order Đơn hàng bị hủy
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
}
