package com.example.ecommerce.chat.model;

import com.example.ecommerce.dto.response.ProductResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test toàn diện cho ChatRequestContext.
 * Phụ trách: Cao Chí Thiện
 * Task ID: [TASK-CHAT-PRODUCT-CARDS]
 */
class ChatRequestContextTest {

    @AfterEach
    void tearDown() {
        ChatRequestContext.clear();
    }

    @Test
    @DisplayName("CRC-01: Thu thập sản phẩm và giữ nguyên thứ tự xuất hiện")
    void testAddReferencedProducts_PreservesOrder() {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);

        ProductResponse p1 = ProductResponse.builder().id(1L).name("Sản phẩm 1").build();
        ProductResponse p2 = ProductResponse.builder().id(2L).name("Sản phẩm 2").build();
        ProductResponse p3 = ProductResponse.builder().id(3L).name("Sản phẩm 3").build();

        ChatRequestContext.get().addReferencedProducts(List.of(p1, p2));
        ChatRequestContext.get().addReferencedProduct(p3);

        List<ProductResponse> products = ChatRequestContext.get().getReferencedProducts();
        assertThat(products).hasSize(3);
        assertThat(products.get(0).getId()).isEqualTo(1L);
        assertThat(products.get(1).getId()).isEqualTo(2L);
        assertThat(products.get(2).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("CRC-02: Khử trùng lặp sản phẩm theo productId")
    void testAddReferencedProducts_Deduplication() {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);

        ProductResponse p1 = ProductResponse.builder().id(10L).name("Áo thun đỏ").build();
        ProductResponse p2 = ProductResponse.builder().id(10L).name("Áo thun đỏ trùng").build();
        ProductResponse p3 = ProductResponse.builder().id(20L).name("Quần jean").build();

        ChatRequestContext.get().addReferencedProduct(p1);
        ChatRequestContext.get().addReferencedProduct(p2); // trùng ID 10L
        ChatRequestContext.get().addReferencedProduct(p3);

        List<ProductResponse> products = ChatRequestContext.get().getReferencedProducts();
        assertThat(products).hasSize(2);
        assertThat(products.get(0).getName()).isEqualTo("Áo thun đỏ");
        assertThat(products.get(1).getName()).isEqualTo("Quần jean");
    }

    @Test
    @DisplayName("CRC-03: Giới hạn tối đa 10 sản phẩm để tránh phình to response payload")
    void testAddReferencedProducts_LimitMaxProducts() {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);

        List<ProductResponse> list = new ArrayList<>();
        for (long i = 1; i <= 15; i++) {
            list.add(ProductResponse.builder().id(i).name("Sản phẩm " + i).build());
        }

        ChatRequestContext.get().addReferencedProducts(list);

        List<ProductResponse> products = ChatRequestContext.get().getReferencedProducts();
        assertThat(products).hasSize(10);
        assertThat(products.get(9).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("CRC-04: Độc lập dữ liệu ThreadLocal giữa các luồng xử lý đồng thời")
    void testThreadLocalIsolation() throws InterruptedException {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);
        ChatRequestContext.get().addReferencedProduct(ProductResponse.builder().id(111L).name("Thread Main Product").build());

        AtomicBoolean otherThreadHasNull = new AtomicBoolean(false);
        AtomicBoolean otherThreadIsolated = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            try {
                // Ban đầu luồng khác không có context
                if (ChatRequestContext.get() == null) {
                    otherThreadHasNull.set(true);
                }
                // Khởi tạo context riêng cho luồng khác
                ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);
                ChatRequestContext.get().addReferencedProduct(ProductResponse.builder().id(222L).name("Thread 2 Product").build());

                List<ProductResponse> t2Products = ChatRequestContext.get().getReferencedProducts();
                if (t2Products.size() == 1 && t2Products.get(0).getId() == 222L) {
                    otherThreadIsolated.set(true);
                }
            } finally {
                ChatRequestContext.clear();
                latch.countDown();
            }
        });

        otherThread.start();
        boolean finished = latch.await(3, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(otherThreadHasNull.get()).isTrue();
        assertThat(otherThreadIsolated.get()).isTrue();

        // Luồng chính vẫn giữ nguyên sản phẩm của luồng chính
        List<ProductResponse> mainProducts = ChatRequestContext.get().getReferencedProducts();
        assertThat(mainProducts).hasSize(1);
        assertThat(mainProducts.get(0).getId()).isEqualTo(111L);
    }

    @Test
    @DisplayName("CRC-05: ChatRequestContext.clear() dọn dẹp sạch ThreadLocal")
    void testClearRemovesContext() {
        ChatRequestContext.init(System.currentTimeMillis() + 30000L, 3);
        assertThat(ChatRequestContext.get()).isNotNull();

        ChatRequestContext.clear();
        assertThat(ChatRequestContext.get()).isNull();
    }
}

