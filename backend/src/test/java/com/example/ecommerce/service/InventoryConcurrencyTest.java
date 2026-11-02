package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.AdjustInventoryRequest;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đồng thời (Concurrency Test) cho module Quản lý tồn kho (Inventory).
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 *
 * Kiểm tra các tình huống tranh chấp dữ liệu (Race Condition) khi có nhiều luồng
 * cùng thao tác trên tồn kho sản phẩm:
 * 1. Chống bán âm / bán vượt tồn kho (Overselling Prevention with Pessimistic Locking).
 * 2. Trừ kho đồng thời khi đủ số lượng hàng tồn.
 * 3. Đồng thời vừa đặt hàng (trừ kho) vừa hủy đơn (hoàn kho) - tính nhất quán dữ liệu.
 * 4. Admin điều chỉnh kho đồng thời (Manual Adjust) - không bị Lost Update.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_inventory_concurrency;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.hikari.maximum-pool-size=30",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = categoryRepository.save(Category.builder()
                .name("Thiết bị kiểm thử " + System.currentTimeMillis())
                .description("Danh mục test đồng thời")
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        inventoryTransactionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: 30 luồng cùng mua sản phẩm chỉ có 10 tồn kho -> Ngăn chặn bán âm (Overselling) tuyệt đối")
    void testConcurrentDeductStock_OversellingPrevented() throws InterruptedException {
        // 1. Chuẩn bị sản phẩm chỉ có 10 sản phẩm trong kho
        Product product = productRepository.save(Product.builder()
                .name("Bàn phím cơ giới hạn")
                .description("Chỉ có 10 chiếc trong kho")
                .price(BigDecimal.valueOf(2500000))
                .stockQuantity(10)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int numberOfThreads = 30; // 30 khách hàng tranh nhau mua 10 sản phẩm cùng lúc
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        // 2. Kích hoạt 30 luồng đồng thời
        for (int i = 1; i <= numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Chờ hiệu lệnh để cả 30 luồng xuất phát cùng 1 tích tắc

                    // Giả lập đơn hàng mua 1 sản phẩm
                    Product refProduct = Product.builder().id(productId).name("Bàn phím cơ giới hạn").stockQuantity(10).build();
                    OrderItem item = OrderItem.builder()
                            .product(refProduct)
                            .productName(refProduct.getName())
                            .quantity(1)
                            .unitPrice(BigDecimal.valueOf(2500000))
                            .build();

                    Order order = Order.builder()
                            .id((long) index)
                            .orderCode("ORD-CONCUR-" + index)
                            .items(List.of(item))
                            .build();

                    inventoryService.deductStockForOrder(order);
                    successOrders.incrementAndGet();
                } catch (BusinessException e) {
                    // Bị từ chối do hết tồn kho
                    failedOrders.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Bắn tín hiệu bắt đầu
        startLatch.countDown();

        // Chờ toàn bộ 30 luồng hoàn tất (tối đa 15 giây)
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();

        // 3. Xác minh kết quả chống overselling
        assertThat(successOrders.get()).isEqualTo(10); // Đúng 10 đơn thành công
        assertThat(failedOrders.get()).isEqualTo(20);  // Đúng 20 đơn bị từ chối
        assertThat(successOrders.get() + failedOrders.get()).isEqualTo(numberOfThreads);

        // Kiểm tra tồn kho trong cơ sở dữ liệu
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // Tồn kho về 0, KHÔNG ĐƯỢC ÂM

        // Kiểm tra nhật ký biến động kho (chỉ đúng 10 bản ghi thành công được lưu)
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
        assertThat(transactions).hasSize(10);
        assertThat(transactions).allMatch(tx -> tx.getTransactionType() == InventoryTransactionType.ORDER_PLACED);
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: 20 luồng cùng mua 1 sản phẩm khi kho có đủ 20 cái -> Tất cả 20 luồng đều thành công")
    void testConcurrentDeductStock_ExactStock_AllSucceed() throws InterruptedException {
        Product product = productRepository.save(Product.builder()
                .name("Tai nghe kiểm âm")
                .description("Có đúng 20 chiếc")
                .price(BigDecimal.valueOf(1800000))
                .stockQuantity(20)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        for (int i = 1; i <= numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    Product refProduct = Product.builder().id(productId).name("Tai nghe kiểm âm").stockQuantity(20).build();
                    OrderItem item = OrderItem.builder()
                            .product(refProduct)
                            .productName(refProduct.getName())
                            .quantity(1)
                            .unitPrice(BigDecimal.valueOf(1800000))
                            .build();

                    Order order = Order.builder()
                            .id((long) (100 + index))
                            .orderCode("ORD-EXACT-" + index)
                            .items(List.of(item))
                            .build();

                    inventoryService.deductStockForOrder(order);
                    successOrders.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(successOrders.get()).isEqualTo(20);
        assertThat(failedOrders.get()).isEqualTo(0);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
        assertThat(transactions).hasSize(20);
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: Vừa trừ kho (10 đơn) vừa hoàn kho (5 đơn) -> Tồn kho cuối cùng bảo toàn chính xác")
    void testConcurrentDeductAndRestore_DataConsistency() throws InterruptedException {
        // Tồn kho ban đầu: 20
        Product product = productRepository.save(Product.builder()
                .name("Chuột máy tính không dây")
                .price(BigDecimal.valueOf(450000))
                .stockQuantity(20)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int deductThreads = 10;
        int restoreThreads = 5;
        int totalThreads = deductThreads + restoreThreads;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger deductSuccess = new AtomicInteger(0);
        AtomicInteger restoreSuccess = new AtomicInteger(0);

        // 10 luồng trừ kho
        for (int i = 1; i <= deductThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Product refProduct = Product.builder().id(productId).name("Chuột máy tính không dây").build();
                    OrderItem item = OrderItem.builder().product(refProduct).productName(refProduct.getName()).quantity(1).build();
                    Order order = Order.builder().id((long) (200 + index)).orderCode("ORD-DED-" + index).items(List.of(item)).build();

                    inventoryService.deductStockForOrder(order);
                    deductSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 5 luồng hoàn kho
        for (int i = 1; i <= restoreThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Product refProduct = Product.builder().id(productId).name("Chuột máy tính không dây").build();
                    OrderItem item = OrderItem.builder().product(refProduct).productName(refProduct.getName()).quantity(1).build();
                    Order order = Order.builder().id((long) (300 + index)).orderCode("ORD-RES-" + index).items(List.of(item)).build();

                    inventoryService.restoreStockForOrder(order);
                    restoreSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(deductSuccess.get()).isEqualTo(deductThreads);
        assertThat(restoreSuccess.get()).isEqualTo(restoreThreads);

        // Tồn kho cuối cùng: 20 - 10 + 5 = 15
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: Admin điều chỉnh kho (10 luồng +5 và 10 luồng -2) -> Không bị mất mát dữ liệu (Lost Update)")
    void testConcurrentManualAdjust_NoLostUpdate() throws InterruptedException {
        // Tồn kho ban đầu: 50
        Product product = productRepository.save(Product.builder()
                .name("Màn hình Gaming 240Hz")
                .price(BigDecimal.valueOf(8900000))
                .stockQuantity(50)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int addThreads = 10;
        int subThreads = 10;
        int totalThreads = addThreads + subThreads;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        // 10 luồng nhập thêm +5
        for (int i = 0; i < addThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    AdjustInventoryRequest req = AdjustInventoryRequest.builder().changeQuantity(5).note("Nhập hàng").build();
                    inventoryService.adjustStock(productId, req);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 10 luồng xuất kho -2
        for (int i = 0; i < subThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    AdjustInventoryRequest req = AdjustInventoryRequest.builder().changeQuantity(-2).note("Xuất kho").build();
                    inventoryService.adjustStock(productId, req);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(totalThreads);

        // Tồn kho mong đợi: 50 + (10 * 5) - (10 * 2) = 50 + 50 - 20 = 80
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(80);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
        assertThat(transactions).hasSize(totalThreads);
    }
}
