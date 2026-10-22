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
 * Service quản lý tồn kho (Inventory).
 *
 * Thống nhất kiến trúc giữa:
 * - Vũ Đức Huy Hoàng (Cart & Order): Gọi deductStockForOrder khi đặt hàng, restoreStockForOrder khi hủy đơn.
 * - Đặng Thanh Bình (Inventory & Payment): Quản lý tồn kho, ghi nhật ký biến động kho InventoryTransaction.
 *
 * Quy tắc:
 * - Trừ kho: change_quantity < 0, transaction_type = ORDER_PLACED
 * - Hoàn kho: change_quantity > 0, transaction_type = ORDER_CANCELLED
 * - Kiểm tra stock_quantity không bao giờ được âm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    /**
     * Trừ số lượng tồn kho cho các sản phẩm trong đơn hàng.
     * Được thực thi bên trong transaction của OrderService.
     *
     * @param order Đơn hàng vừa tạo
     * @throws BusinessException nếu số lượng tồn kho không đủ
     */
    @Transactional
    public void deductStockForOrder(Order order) {
        log.info("Bắt đầu trừ tồn kho cho đơn hàng: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + item.getProduct().getId()));

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
     *
     * @param order Đơn hàng bị hủy
     */
    @Transactional
    public void restoreStockForOrder(Order order) {
        log.info("Bắt đầu hoàn trả tồn kho cho đơn hàng bị hủy: {}", order.getOrderCode());

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + item.getProduct().getId()));

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
}
