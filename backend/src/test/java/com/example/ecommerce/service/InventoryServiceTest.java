package com.example.ecommerce.service;

import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.InventoryTransactionRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Tai nghe Bluetooth")
                .price(BigDecimal.valueOf(1200000))
                .stockQuantity(10)
                .active(true)
                .build();

        OrderItem item = OrderItem.builder()
                .id(100L)
                .product(testProduct)
                .productName(testProduct.getName())
                .unitPrice(testProduct.getPrice())
                .quantity(3)
                .subtotal(BigDecimal.valueOf(3600000))
                .build();

        testOrder = Order.builder()
                .id(50L)
                .orderCode("ORD-20261022-9999")
                .items(List.of(item))
                .build();
    }

    @Test
    @DisplayName("Trừ kho thành công khi tạo đơn - Stock giảm và ghi log ORDER_PLACED")
    void deductStockForOrder_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        inventoryService.deductStockForOrder(testOrder);

        assertThat(testProduct.getStockQuantity()).isEqualTo(7); // 10 - 3 = 7
        verify(productRepository).save(testProduct);

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(captor.capture());

        InventoryTransaction tx = captor.getValue();
        assertThat(tx.getChangeQuantity()).isEqualTo(-3);
        assertThat(tx.getStockAfter()).isEqualTo(7);
        assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_PLACED);
        assertThat(tx.getOrderId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Trừ kho thất bại khi số lượng yêu cầu vượt quá tồn kho")
    void deductStockForOrder_ThrowsException_WhenStockInsufficient() {
        testProduct.setStockQuantity(2); // Chỉ còn 2 mà mua 3
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> inventoryService.deductStockForOrder(testOrder))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không đủ số lượng tồn kho");

        verify(productRepository, never()).save(any());
        verify(inventoryTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hoàn kho thành công khi hủy đơn - Stock tăng và ghi log ORDER_CANCELLED")
    void restoreStockForOrder_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        inventoryService.restoreStockForOrder(testOrder);

        assertThat(testProduct.getStockQuantity()).isEqualTo(13); // 10 + 3 = 13
        verify(productRepository).save(testProduct);

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(captor.capture());

        InventoryTransaction tx = captor.getValue();
        assertThat(tx.getChangeQuantity()).isEqualTo(3);
        assertThat(tx.getStockAfter()).isEqualTo(13);
        assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_CANCELLED);
        assertThat(tx.getOrderId()).isEqualTo(50L);
    }
}
