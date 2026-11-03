package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.dto.response.InventoryTransactionResponse;
import com.example.ecommerce.dto.response.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("Admin nhập kho thủ công (+15) -> Tồn kho tăng lên 25, lưu loại MANUAL_IMPORT kèm lý do")
    void adjustStock_Success_ManualImport() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            tx.setId(101L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(15)
                .reason("Nhập lô hàng mới từ nhà cung cấp")
                .build();

        InventoryTransactionResponse response = inventoryService.adjustStock(1L, request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(25); // 10 + 15
        verify(productRepository).save(testProduct);

        assertThat(response).isNotNull();
        assertThat(response.getChangeQuantity()).isEqualTo(15);
        assertThat(response.getStockAfter()).isEqualTo(25);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.MANUAL_IMPORT);
        assertThat(response.getNote()).isEqualTo("Nhập lô hàng mới từ nhà cung cấp");
    }

    @Test
    @DisplayName("Admin xuất kho thủ công (-4) -> Tồn kho giảm xuống 6, lưu loại MANUAL_EXPORT kèm lý do")
    void adjustStock_Success_ManualExport() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            tx.setId(102L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(-4)
                .reason("Xuất mẫu thử nghiệm cho showroom")
                .build();

        InventoryTransactionResponse response = inventoryService.adjustStock(1L, request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(6); // 10 - 4
        verify(productRepository).save(testProduct);

        assertThat(response).isNotNull();
        assertThat(response.getChangeQuantity()).isEqualTo(-4);
        assertThat(response.getStockAfter()).isEqualTo(6);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.MANUAL_EXPORT);
        assertThat(response.getNote()).isEqualTo("Xuất mẫu thử nghiệm cho showroom");
    }

    @Test
    @DisplayName("Admin điều chỉnh với type MANUAL_EXPORT và changeQuantity dương -> Tự động chuyển thành âm")
    void adjustStock_Success_ExplicitExportType() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class))).thenAnswer(invocation -> {
            InventoryTransaction tx = invocation.getArgument(0);
            tx.setId(103L);
            tx.setCreatedAt(LocalDateTime.now());
            return tx;
        });

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(5)
                .type(InventoryTransactionType.MANUAL_EXPORT)
                .note("Hàng hỏng do vận chuyển")
                .build();

        InventoryTransactionResponse response = inventoryService.adjustStock(1L, request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(5); // 10 - 5
        assertThat(response.getChangeQuantity()).isEqualTo(-5);
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.MANUAL_EXPORT);
        assertThat(response.getNote()).isEqualTo("Hàng hỏng do vận chuyển");
    }

    @Test
    @DisplayName("Chặn điều chỉnh xuất quá số lượng tồn hiện có -> Ném BusinessException")
    void adjustStock_ThrowsException_WhenExportExceedsCurrentStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct)); // Tồn: 10

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(-15) // Muốn xuất 15
                .reason("Xuất thanh lý toàn bộ")
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể xuất kho vượt quá số lượng tồn hiện có");

        verify(productRepository, never()).save(testProduct);
        verify(inventoryTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Báo lỗi khi số lượng điều chỉnh là 0 hoặc null -> Ném BusinessException")
    void adjustStock_ThrowsException_WhenQuantityZeroOrNull() {
        AdjustInventoryRequest requestZero = AdjustInventoryRequest.builder()
                .changeQuantity(0)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, requestZero))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng điều chỉnh phải khác 0");

        AdjustInventoryRequest requestNull = AdjustInventoryRequest.builder()
                .changeQuantity(null)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(1L, requestNull))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Báo lỗi khi sản phẩm không tồn tại -> Ném ResourceNotFoundException")
    void adjustStock_ThrowsException_WhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        AdjustInventoryRequest request = AdjustInventoryRequest.builder()
                .changeQuantity(10)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sản phẩm không tồn tại với id: 999");
    }

    @Test
    @DisplayName("Tra cứu lịch sử biến động kho có bộ lọc thành công")
    void getTransactions_WithFilters_Success() {
        InventoryTransaction tx = InventoryTransaction.builder()
                .id(1L)
                .product(testProduct)
                .changeQuantity(10)
                .stockAfter(10)
                .transactionType(InventoryTransactionType.MANUAL_IMPORT)
                .note("Nhập kho ban đầu")
                .createdAt(LocalDateTime.now())
                .build();

        Page<InventoryTransaction> page = new PageImpl<>(List.of(tx));
        when(inventoryTransactionRepository.findWithFilters(eq(1L), eq(InventoryTransactionType.MANUAL_IMPORT), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<InventoryTransactionResponse> response = inventoryService.getTransactions(
                1L, InventoryTransactionType.MANUAL_IMPORT, null, null, 0, 10, "createdAt", "desc"
        );

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getProductName()).isEqualTo("Tai nghe Bluetooth");
        assertThat(response.getContent().get(0).getTransactionType()).isEqualTo(InventoryTransactionType.MANUAL_IMPORT);
    }

    @Test
    @DisplayName("Lấy lịch sử giao dịch kho theo sản phẩm thành công")
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
