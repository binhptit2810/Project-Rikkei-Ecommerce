package com.example.ecommerce.service;

import com.example.ecommerce.dto.request.CreateOrderRequest;
import com.example.ecommerce.dto.request.OrderItemRequest;
import com.example.ecommerce.dto.response.OrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ProductVariantRepository;
import com.example.ecommerce.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử toàn diện module Biến thể kích cỡ (ProductVariant Size) & Quản lý tồn kho theo size.
 * [TSK-D14-VDHH]
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceAndOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private Validator validator;
    private User testUser;
    private Product fashionProduct;
    private ProductVariant variantS;
    private ProductVariant variantM;
    private ProductVariant variantL;
    private ProductVariant variantOutOfStock;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        fashionProduct = Product.builder()
                .id(100L)
                .name("Áo thun nam basic Cotton")
                .price(BigDecimal.valueOf(199000))
                .stockQuantity(50)
                .active(true)
                .variants(new ArrayList<>())
                .build();

        variantS = ProductVariant.builder()
                .id(1L)
                .product(fashionProduct)
                .size("S")
                .stockQuantity(10)
                .sku("PROD-100-S")
                .build();

        variantM = ProductVariant.builder()
                .id(2L)
                .product(fashionProduct)
                .size("M")
                .stockQuantity(15)
                .sku("PROD-100-M")
                .build();

        variantL = ProductVariant.builder()
                .id(3L)
                .product(fashionProduct)
                .size("L")
                .stockQuantity(15)
                .sku("PROD-100-L")
                .build();

        variantOutOfStock = ProductVariant.builder()
                .id(4L)
                .product(fashionProduct)
                .size("XXL")
                .stockQuantity(0)
                .sku("PROD-100-XXL")
                .build();

        fashionProduct.addVariant(variantS);
        fashionProduct.addVariant(variantM);
        fashionProduct.addVariant(variantL);
        fashionProduct.addVariant(variantOutOfStock);
    }

    @Test
    @DisplayName("PV-01: Tạo ProductVariant hợp lệ & kiểm tra ràng buộc không lặp JSON recursion")
    void testProductVariant_Valid() {
        ProductVariant variant = ProductVariant.builder()
                .id(10L)
                .product(fashionProduct)
                .size("XL")
                .stockQuantity(20)
                .sku("PROD-100-XL")
                .build();

        assertThat(validator.validate(variant)).isEmpty();
        assertThat(variant.getSize()).isEqualTo("XL");
        assertThat(variant.getStockQuantity()).isEqualTo(20);
        assertThat(variant.getSku()).isEqualTo("PROD-100-XL");
        assertThat(variant.toString()).doesNotContain("password");
    }

    @Test
    @DisplayName("PV-02: Không cho phép tồn kho âm trong ProductVariant")
    void testProductVariant_NegativeStockQuantity_Rejected() {
        ProductVariant invalidVariant = ProductVariant.builder()
                .id(11L)
                .product(fashionProduct)
                .size("S")
                .stockQuantity(-5)
                .sku("PROD-100-NEG")
                .build();

        var violations = validator.validate(invalidVariant);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("không được nhỏ hơn 0");
    }

    @Test
    @DisplayName("PV-03: Thêm cùng 1 sản phẩm nhưng 2 size khác nhau (M và L) tạo thành 2 dòng OrderItem riêng biệt")
    void testCreateOrder_DifferentSizes_CreatesSeparateOrderItems() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(500L);
            return o;
        });

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize("M").quantity(2).build(),
                        OrderItemRequest.builder().productId(100L).selectedSize("L").quantity(1).build()
                ))
                .build();

        OrderResponse response = orderService.createOrder(request, testUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(2);

        var itemM = response.getItems().stream()
                .filter(i -> "M".equals(i.getSelectedSize()))
                .findFirst().orElse(null);
        assertThat(itemM).isNotNull();
        assertThat(itemM.getQuantity()).isEqualTo(2);

        var itemL = response.getItems().stream()
                .filter(i -> "L".equals(i.getSelectedSize()))
                .findFirst().orElse(null);
        assertThat(itemL).isNotNull();
        assertThat(itemL.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("PV-04: Thêm cùng sản phẩm và cùng size (M) sẽ gộp số lượng chính xác")
    void testCreateOrder_SameSize_ConsolidatesQuantity() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(501L);
            return o;
        });

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize("M").quantity(1).build(),
                        OrderItemRequest.builder().productId(100L).selectedSize("M").quantity(2).build()
                ))
                .build();

        OrderResponse response = orderService.createOrder(request, testUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getSelectedSize()).isEqualTo("M");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("PV-05: Bắt buộc chọn size khi đặt sản phẩm có biến thể, chưa chọn size ném BusinessException")
    void testCreateOrder_MissingSize_ThrowsBusinessException() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize(null).quantity(1).build()
                ))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, testUser.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng chọn kích cỡ trước khi tiếp tục");
    }

    @Test
    @DisplayName("PV-06: Chọn kích cỡ không tồn tại ném BusinessException")
    void testCreateOrder_InvalidSize_ThrowsBusinessException() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize("3XL").quantity(1).build()
                ))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, testUser.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("PV-07: Chọn kích cỡ đã hết hàng (stock = 0) ném BusinessException 'Kích cỡ này hiện đã hết hàng.'")
    void testCreateOrder_OutOfStockSize_ThrowsBusinessException() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize("XXL").quantity(1).build()
                ))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, testUser.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kích cỡ này hiện đã hết hàng");
    }

    @Test
    @DisplayName("PV-08: Đặt số lượng vượt tồn kho của size được chọn ném BusinessException 'Số lượng sản phẩm vượt quá tồn kho hiện có.'")
    void testCreateOrder_QuantityExceedsSizeStock_ThrowsBusinessException() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(fashionProduct));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Lê Lợi, Q1, TP.HCM")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        OrderItemRequest.builder().productId(100L).selectedSize("S").quantity(20).build()
                ))
                .build();

        assertThatThrownBy(() -> orderService.createOrder(request, testUser.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Số lượng sản phẩm vượt quá tồn kho hiện có");
    }

    @Test
    @DisplayName("PV-09: InventoryService trừ đúng tồn kho theo size và rollback khi thiếu tồn kho")
    void testInventoryService_DeductStockForVariant() {
        ProductVariantRepository mockVariantRepo = mock(ProductVariantRepository.class);
        com.example.ecommerce.repository.InventoryTransactionRepository mockTxRepo = mock(com.example.ecommerce.repository.InventoryTransactionRepository.class);

        InventoryService invService = new InventoryService(productRepository, mockTxRepo, mockVariantRepo);

        ProductVariant liveVariant = ProductVariant.builder()
                .id(2L)
                .product(fashionProduct)
                .size("M")
                .stockQuantity(15)
                .sku("PROD-100-M")
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fashionProduct));
        when(mockVariantRepo.findByProductIdAndSizeForUpdate(100L, "M")).thenReturn(Optional.of(liveVariant));

        Order order = Order.builder()
                .id(88L)
                .orderCode("ORD-20261112-TEST")
                .build();

        OrderItem item = OrderItem.builder()
                .product(fashionProduct)
                .selectedSize("M")
                .quantity(3)
                .build();
        order.addItem(item);

        invService.deductStockForOrder(order);

        assertThat(liveVariant.getStockQuantity()).isEqualTo(12);
        assertThat(fashionProduct.getStockQuantity()).isEqualTo(47);
        verify(mockVariantRepo).save(liveVariant);
        verify(productRepository).save(fashionProduct);
    }

    @Test
    @DisplayName("PV-10: InventoryService hoàn lại tồn kho đúng size khi hủy đơn hàng")
    void testInventoryService_RestoreStockForVariant() {
        ProductVariantRepository mockVariantRepo = mock(ProductVariantRepository.class);
        com.example.ecommerce.repository.InventoryTransactionRepository mockTxRepo = mock(com.example.ecommerce.repository.InventoryTransactionRepository.class);

        InventoryService invService = new InventoryService(productRepository, mockTxRepo, mockVariantRepo);

        ProductVariant liveVariant = ProductVariant.builder()
                .id(2L)
                .product(fashionProduct)
                .size("M")
                .stockQuantity(12)
                .sku("PROD-100-M")
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(fashionProduct));
        when(mockVariantRepo.findByProductIdAndSizeForUpdate(100L, "M")).thenReturn(Optional.of(liveVariant));

        Order order = Order.builder()
                .id(88L)
                .orderCode("ORD-20261112-TEST")
                .build();

        OrderItem item = OrderItem.builder()
                .product(fashionProduct)
                .selectedSize("M")
                .quantity(3)
                .build();
        order.addItem(item);

        invService.restoreStockForOrder(order);

        assertThat(liveVariant.getStockQuantity()).isEqualTo(15);
        assertThat(fashionProduct.getStockQuantity()).isEqualTo(53);
        verify(mockVariantRepo).save(liveVariant);
        verify(productRepository).save(fashionProduct);
    }
}

