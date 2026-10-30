package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long userId;
    private String userEmail;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private String orderStatusDescription;
    private PaymentStatus paymentStatus;
    private String paymentStatusDescription;
    private PaymentMethod paymentMethod;
    private String paymentMethodDescription;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}
