package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Test cho DTO CreateOrderRequest và OrderItemRequest.
 * Task ID: TSK-D06-CCT - Phân quyền Admin Controller & Rà soát DTO Order
 *
 * Kiểm tra:
 * 1. JSON Deserialization qua Jackson ObjectMapper.
 * 2. Bean Validation (JSR-380) các ràng buộc dữ liệu: NotBlank, Size, Pattern, Min, Max, NotEmpty, Valid.
 */
// [TSK-D06-CCT]
class CreateOrderRequestTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpAll() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0912345678")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .note("Giao giờ hành chính")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("1. JSON Deserialization Tests")
    class JsonDeserializationTests {

        @Test
        @DisplayName("DESER-01: Deserialization JSON hợp lệ từ frontend thành CreateOrderRequest thành công")
        void deserialize_ValidJson_Success() throws Exception {
            String json = """
                {
                    "recipientName": "Nguyễn Văn A",
                    "recipientPhone": "0912345678",
                    "shippingAddress": "123 Cầu Giấy, Hà Nội",
                    "paymentMethod": "COD",
                    "note": "Giao giờ hành chính",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;

            CreateOrderRequest request = objectMapper.readValue(json, CreateOrderRequest.class);

            assertThat(request).isNotNull();
            assertThat(request.getRecipientName()).isEqualTo("Nguyễn Văn A");
            assertThat(request.getRecipientPhone()).isEqualTo("0912345678");
            assertThat(request.getShippingAddress()).isEqualTo("123 Cầu Giấy, Hà Nội");
            assertThat(request.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
            assertThat(request.getNote()).isEqualTo("Giao giờ hành chính");
            assertThat(request.getItems()).hasSize(1);
            assertThat(request.getItems().get(0).getProductId()).isEqualTo(1L);
            assertThat(request.getItems().get(0).getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("DESER-02: Deserialization JSON với paymentMethod SIMULATED thành công")
        void deserialize_PaymentMethodSimulated_Success() throws Exception {
            String json = """
                {
                    "recipientName": "Trần Thị B",
                    "recipientPhone": "+84987654321",
                    "shippingAddress": "456 Hoàn Kiếm, Hà Nội",
                    "paymentMethod": "SIMULATED",
                    "items": [
                        { "productId": 10, "quantity": 1 }
                    ]
                }
                """;

            CreateOrderRequest request = objectMapper.readValue(json, CreateOrderRequest.class);

            assertThat(request).isNotNull();
            assertThat(request.getPaymentMethod()).isEqualTo(PaymentMethod.SIMULATED);
            assertThat(request.getNote()).isNull();
        }

        @Test
        @DisplayName("DESER-03: Deserialization JSON với paymentMethod không hợp lệ ném lỗi InvalidFormatException")
        void deserialize_InvalidPaymentMethod_ThrowsException() {
            String json = """
                {
                    "recipientName": "Nguyễn Văn A",
                    "recipientPhone": "0912345678",
                    "shippingAddress": "123 Cầu Giấy",
                    "paymentMethod": "INVALID_METHOD",
                    "items": [{ "productId": 1, "quantity": 1 }]
                }
                """;

            assertThatThrownBy(() -> objectMapper.readValue(json, CreateOrderRequest.class))
                    .isInstanceOf(InvalidFormatException.class);
        }
    }

    @Nested
    @DisplayName("2. Bean Validation Tests")
    class BeanValidationTests {

        @Test
        @DisplayName("VAL-01: Dữ liệu hợp lệ không có bất kỳ lỗi vi phạm nào")
        void validate_ValidRequest_NoViolations() {
            CreateOrderRequest request = buildValidRequest();
            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("VAL-02: recipientName để trống hoặc chỉ chứa khoảng trắng gây lỗi vi phạm @NotBlank")
        void validate_RecipientName_Blank_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setRecipientName("   ");

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientName")
                    && v.getMessage().contains("không được để trống"));
        }

        @Test
        @DisplayName("VAL-03: recipientName vượt quá 100 ký tự gây lỗi vi phạm @Size(max=100)")
        void validate_RecipientName_Exceeds100_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setRecipientName("A".repeat(101));

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientName")
                    && v.getMessage().contains("không được vượt quá 100 ký tự"));
        }

        @Test
        @DisplayName("VAL-04: recipientPhone không đúng định dạng số điện thoại gây lỗi vi phạm @Pattern")
        void validate_RecipientPhone_InvalidPattern_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setRecipientPhone("12345678"); // Không bắt đầu bằng 0 hoặc +84 hoặc không đủ chữ số

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipientPhone")
                    && v.getMessage().contains("không đúng định dạng"));
        }

        @Test
        @DisplayName("VAL-05: recipientPhone hợp lệ với đầu số +84 chuẩn quốc tế")
        void validate_RecipientPhone_Plus84_Valid() {
            CreateOrderRequest request = buildValidRequest();
            request.setRecipientPhone("+84912345678");

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("VAL-06: shippingAddress để trống gây lỗi vi phạm @NotBlank")
        void validate_ShippingAddress_Blank_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setShippingAddress("");

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("shippingAddress")
                    && v.getMessage().contains("không được để trống"));
        }

        @Test
        @DisplayName("VAL-07: paymentMethod null gây lỗi vi phạm @NotNull")
        void validate_PaymentMethod_Null_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setPaymentMethod(null);

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("paymentMethod")
                    && v.getMessage().contains("chọn phương thức thanh toán"));
        }

        @Test
        @DisplayName("VAL-08: note vượt quá 1000 ký tự gây lỗi vi phạm @Size(max=1000)")
        void validate_Note_Exceeds1000_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setNote("N".repeat(1001));

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("note")
                    && v.getMessage().contains("không được vượt quá 1000 ký tự"));
        }

        @Test
        @DisplayName("VAL-09: items rỗng gây lỗi vi phạm @NotEmpty")
        void validate_Items_Empty_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.setItems(new ArrayList<>());

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("items")
                    && v.getMessage().contains("Giỏ hàng không có sản phẩm nào"));
        }

        @Test
        @DisplayName("VAL-10: items có item con chứa productId null gây lỗi vi phạm lồng nhau (@Valid)")
        void validate_ItemProductId_Null_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.getItems().get(0).setProductId(null);

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("productId")
                    && v.getMessage().contains("ID sản phẩm không được để trống"));
        }

        @Test
        @DisplayName("VAL-11: items có item con chứa quantity < 1 gây lỗi vi phạm @Min(1)")
        void validate_ItemQuantity_LessThan1_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.getItems().get(0).setQuantity(0);

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("quantity")
                    && v.getMessage().contains("lớn hơn hoặc bằng 1"));
        }

        @Test
        @DisplayName("VAL-12: items có item con chứa quantity > 1000 gây lỗi vi phạm @Max(1000)")
        void validate_ItemQuantity_Exceeds1000_HasViolation() {
            CreateOrderRequest request = buildValidRequest();
            request.getItems().get(0).setQuantity(1001);

            Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("quantity")
                    && v.getMessage().contains("tối đa 1000"));
        }
    }
}
