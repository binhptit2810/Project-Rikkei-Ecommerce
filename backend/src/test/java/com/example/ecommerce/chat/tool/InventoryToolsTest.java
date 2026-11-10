package com.example.ecommerce.chat.tool;

import com.example.ecommerce.chat.tool.dto.StockCheckRequest;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho InventoryTools (Spring AI Tool Calling).
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TSK-D11-CCT]
 */
@ExtendWith(MockitoExtension.class)
class InventoryToolsTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryTools inventoryTools;

    @Test
    @DisplayName("IT-01: checkProductStock báo còn hàng kèm số lượng chính xác khi stockQuantity > 0 và active=true")
    void testCheckStock_InStock() {
        Product product = Product.builder()
                .id(1L)
                .name("Chuột không dây Logitech")
                .price(new BigDecimal("450000"))
                .stockQuantity(12)
                .active(true)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        String result = inventoryTools.checkProductStock(new StockCheckRequest(1L));

        assertThat(result).contains("HIỆN CÒN HÀNG");
        assertThat(result).contains("còn 12 sản phẩm");
        assertThat(result).contains("450,000 đ");
    }

    @Test
    @DisplayName("IT-02: checkProductStock báo hết hàng khi stockQuantity <= 0")
    void testCheckStock_OutOfStock() {
        Product product = Product.builder()
                .id(2L)
                .name("Tai nghe Sony XM5")
                .price(new BigDecimal("7500000"))
                .stockQuantity(0)
                .active(true)
                .build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        String result = inventoryTools.checkProductStock(new StockCheckRequest(2L));

        assertThat(result).contains("TẠM HẾT HÀNG");
        assertThat(result).contains("số lượng tồn: 0");
    }

    @Test
    @DisplayName("IT-03: checkProductStock báo ngừng kinh doanh khi active=false")
    void testCheckStock_Inactive() {
        Product product = Product.builder()
                .id(3L)
                .name("Sản phẩm mẫu cũ")
                .stockQuantity(5)
                .active(false)
                .build();

        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        String result = inventoryTools.checkProductStock(new StockCheckRequest(3L));

        assertThat(result).contains("tạm ngừng kinh doanh");
    }

    @Test
    @DisplayName("IT-04: checkProductStock báo không tồn tại khi không tìm thấy trong DB")
    void testCheckStock_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        String result = inventoryTools.checkProductStock(new StockCheckRequest(999L));

        assertThat(result).contains("không tồn tại trong hệ thống");
    }

    @Test
    @DisplayName("IT-05: checkProductStock yêu cầu mã ID khi truyền null")
    void testCheckStock_NullRequest() {
        String result = inventoryTools.checkProductStock(null);
        assertThat(result).contains("Vui lòng cung cấp mã ID");
    }
}

