package com.example.ecommerce.entity;

/**
 * Phương thức thanh toán:
 * COD       - Thanh toán khi nhận hàng (Cash On Delivery)
 * SIMULATED - Thanh toán mô phỏng trực tuyến (Ví điện tử / Thẻ / Chuyển khoản)
 */
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    SIMULATED("Thanh toán trực tuyến (Mô phỏng)");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
