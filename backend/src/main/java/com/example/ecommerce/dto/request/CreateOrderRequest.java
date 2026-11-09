package com.example.ecommerce.dto.request;

import com.example.ecommerce.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload tạo đơn hàng từ giỏ hàng.
 * Lưu ý: Giá tiền totalAmount được tính toán an toàn tại Backend từ DB, không nhận từ client.
 */
// [TSK-D06-CCT]
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Họ tên người nhận không được để trống")
    @Size(max = 100, message = "Họ tên người nhận không được vượt quá 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng (ví dụ: 0912345678)")
    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    @Size(max = 50, message = "Mã giảm giá không được vượt quá 50 ký tự")
    private String couponCode;

    @NotEmpty(message = "Giỏ hàng không có sản phẩm nào để đặt hàng")
    @Valid
    private List<OrderItemRequest> items;
}
