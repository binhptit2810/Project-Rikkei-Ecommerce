package com.example.ecommerce.service;

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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = categoryRepository.save(Category.builder()
                .name("Thiết bị Điện tử")
                .description("Hàng công nghệ cao cấp")
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        inventoryTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: 30 luồng cùng mua 1 sản phẩm chỉ còn 10 món -> Chính xác 10 đơn thành công, 20 đơn ném BusinessException, tồn kho về đúng 0")
    void testConcurrentDeductStock_AntiOverselling_PreventsNegativeStock() throws InterruptedException {
        // 1. Tạo sản phẩm tồn kho đúng 10 món
        Product product = productRepository.save(Product.builder()
                .name("iPhone 16 Pro Max 256GB - Flash Sale")
                .price(BigDecimal.valueOf(34990000))
                .stockQuantity(10)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int numberOfThreads = 30;
        int buyQuantityPerOrder = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failDueToOutOfStockCount = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCount = new AtomicInteger(0);

        // 2. Chuẩn bị 30 tác vụ mua hàng đồng thời
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // Chờ tín hiệu xuất phát để tất cả 30 luồng lao vào cùng 1 microsecond
                    startLatch.await();

                    // Giả lập Order với OrderItem tham chiếu đến productId
                    Product refProduct = Product.builder().id(productId).name("iPhone 16 Pro Max").build();
                    OrderItem item = OrderItem.builder()
                            .product(refProduct)
                            .productName(refProduct.getName())
                            .quantity(buyQuantityPerOrder)
                            .unitPrice(BigDecimal.valueOf(34990000))
                            .build();

                    Order order = Order.builder()
                            .id((long) (100 + index))
                            .orderCode("ORD-FLASH-" + String.format("%04d", index))
                            .items(List.of(item))
                            .build();

                    // Gọi deductStockForOrder có khóa bi quan PESSIMISTIC_WRITE
                    inventoryService.deductStockForOrder(order);
                    successCount.incrementAndGet();

                } catch (BusinessException be) {
                    if (be.getMessage().contains("không đủ số lượng tồn kho")) {
                        failDueToOutOfStockCount.incrementAndGet();
                    } else {
                        unexpectedErrorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Bắn tín hiệu xuất phát đồng thời
        startLatch.countDown();

        // Chờ toàn bộ 30 luồng hoàn tất với timeout 15 giây
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // 3. Khẳng định kết quả nghiệm thu (DoD Concurrency Test)
        assertThat(finished).as("Tất cả các luồng phải hoàn tất trong thời gian quy định").isTrue();
        assertThat(unexpectedErrorCount.get()).as("Không được có lỗi bất ngờ ngoài luồng nghiệp vụ").isZero();
        assertThat(successCount.get()).as("Số đơn hàng trừ kho thành công phải bằng đúng 10").isEqualTo(10);
        assertThat(failDueToOutOfStockCount.get()).as("Số đơn hàng bị từ chối do hết kho phải bằng 20").isEqualTo(20);

        // Kiểm tra số lượng tồn kho thực tế trong Database sau tranh chấp
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).as("Tồn kho sau flash sale phải về đúng 0, tuyệt đối không bị âm").isEqualTo(0);

        // Kiểm tra nhật ký giao dịch kho (InventoryTransaction)
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
        assertThat(transactions).hasSize(10);
        for (InventoryTransaction tx : transactions) {
            assertThat(tx.getChangeQuantity()).isEqualTo(-1);
            assertThat(tx.getTransactionType()).isEqualTo(InventoryTransactionType.ORDER_PLACED);
        }
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: 10 luồng cùng mua hàng khi kho còn dồi dào (100 món) -> 100% thành công, tồn kho còn lại 90")
    void testConcurrentDeductStock_SufficientStock_DeductsExactAmount() throws InterruptedException {
        Product product = productRepository.save(Product.builder()
                .name("Bàn phím cơ Bluetooth RGB")
                .price(BigDecimal.valueOf(1500000))
                .stockQuantity(100)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int numberOfThreads = 10;
        int buyQuantity = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Product refProduct = Product.builder().id(productId).name("Bàn phím cơ").build();
                    OrderItem item = OrderItem.builder().product(refProduct).productName(refProduct.getName()).quantity(buyQuantity).build();
                    Order order = Order.builder().id((long) (200 + index)).orderCode("ORD-KB-" + index).items(List.of(item)).build();

                    inventoryService.deductStockForOrder(order);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(10);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(90);
    }

    @Test
    @DisplayName("Kiểm thử đồng thời: Vừa đặt hàng (trừ kho) vừa hủy đơn (hoàn kho) đồng thời -> Tồn kho cuối cùng bảo toàn chính xác")
    void testConcurrentDeductAndRestore_DataConsistency() throws InterruptedException {
        // Tồn kho ban đầu: 20
        Product product = productRepository.save(Product.builder()
                .name("Chuột máy tính không dây Silent")
                .price(BigDecimal.valueOf(450000))
                .stockQuantity(20)
                .category(testCategory)
                .active(true)
                .build());

        Long productId = product.getId();
        int deductThreads = 10; // 10 đơn mua 1 món -> tổng mua 10
        int restoreThreads = 5;  // 5 đơn hủy hoàn 1 món -> tổng hoàn 5
        int totalThreads = deductThreads + restoreThreads;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger deductSuccess = new AtomicInteger(0);
        AtomicInteger restoreSuccess = new AtomicInteger(0);

        // 10 luồng trừ kho
        for (int i = 0; i < deductThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Product refProduct = Product.builder().id(productId).name("Chuột máy tính không dây").build();
                    OrderItem item = OrderItem.builder().product(refProduct).productName(refProduct.getName()).quantity(1).build();
                    Order order = Order.builder().id((long) (300 + index)).orderCode("ORD-DED-" + index).items(List.of(item)).build();

                    inventoryService.deductStockForOrder(order);
                    deductSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 5 luồng hoàn kho
        for (int i = 0; i < restoreThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Product refProduct = Product.builder().id(productId).name("Chuột máy tính không dây").build();
                    OrderItem item = OrderItem.builder().product(refProduct).productName(refProduct.getName()).quantity(1).build();
                    Order order = Order.builder().id((long) (400 + index)).orderCode("ORD-RES-" + index).items(List.of(item)).build();

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
}
