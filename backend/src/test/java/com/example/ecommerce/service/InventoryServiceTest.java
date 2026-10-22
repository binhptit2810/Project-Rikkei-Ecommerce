package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
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

    @Test
    @DisplayName("Admin tăng tồn kho - Số lượng cập nhật đúng và ghi log MANUAL_ADJUST")
    void adjustStock_Success_IncreaseStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            tx.setId(99L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(15)
                .note("Nhập hàng từ kho tổng")
                .build();

        InventoryTransactionResponse response = inventoryService.adjustStock(1L, request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(25); // 10 + 15
        verify(productRepository).save(testProduct);
        assertThat(response.getChangeQuantity()).isEqualTo(15);
        assertThat(response.getStockAfter()).isEqualTo(25);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.MANUAL_ADJUST);
    }

    @Test
    @DisplayName("Admin giảm tồn kho hợp lệ - Số lượng giảm đúng và ghi log MANUAL_ADJUST")
    void adjustStock_Success_DecreaseStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            tx.setId(100L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(-4)
                .note("Xuất mẫu trưng bày")
                .build();

        InventoryTransactionResponse response = inventoryService.adjustStock(1L, request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(6); // 10 - 4
        verify(productRepository).save(testProduct);
        assertThat(response.getChangeQuantity()).isEqualTo(-4);
        assertThat(response.getStockAfter()).isEqualTo(6);
    }

    @Test
    @DisplayName("Admin giảm tồn kho vượt quá số lượng hiện tại - Ném BusinessException")
    void adjustStock_ThrowsException_WhenStockAfterNegative() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(-15) // Tồn chỉ có 10
                .note("Xuất lỗi")
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể âm");

        verify(productRepository, never()).save(testProduct);
        verify(inventoryTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin điều chỉnh số lượng bằng 0 hoặc null - Ném BusinessException")
    void adjustStock_ThrowsException_WhenQuantityZeroOrNull() {
        AdjustInventoryRequest requestZero = AdjustInventoryRequest.builder()
                .changeQuantity(0)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, requestZero))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phải khác 0");

        AdjustInventoryRequest requestNull = AdjustInventoryRequest.builder()
                .changeQuantity(null)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, requestNull))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Admin điều chỉnh sản phẩm không tồn tại - Ném ResourceNotFoundException")
    void adjustStock_ThrowsException_WhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(5)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("Lấy lịch sử giao dịch kho của một sản phẩm thành công")
    void getTransactionsByProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        InventoryTransaction tx = InventoryTransaction.builder()
                .id(1L)
                .product(testProduct)
                .changeQuantity(10)
                .stockAfter(10)
                .transactionType(InventoryTransactionType.INITIAL_STOCK)
                .createdAt(LocalDateTime.now())
                .build();
        when(inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(tx));

        List<InventoryTransactionResponse> list = inventoryService.getTransactionsByProduct(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getProductName()).isEqualTo("Tai nghe Bluetooth");
    }
}
