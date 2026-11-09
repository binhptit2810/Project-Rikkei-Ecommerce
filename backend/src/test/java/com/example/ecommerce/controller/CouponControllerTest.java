package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ValidateCouponRequest;
import com.example.ecommerce.dto.response.CouponValidationResponse;
import com.example.ecommerce.entity.DiscountType;
import com.example.ecommerce.service.CouponService;
import com.example.ecommerce.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_coupon;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("POST /api/v1/coupons/validate: Kiểm tra mã giảm giá thành công trả về 200 OK")
    void validateCoupon_Success_Returns200() throws Exception {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.valueOf(500000))
                .build();

        CouponValidationResponse response = CouponValidationResponse.builder()
                .code("SALE10")
                .discountType(DiscountType.PERCENT)
                .discountTypeDescription(DiscountType.PERCENT.getDescription())
                .discountValue(BigDecimal.valueOf(10))
                .discountAmount(BigDecimal.valueOf(50000))
                .orderValue(BigDecimal.valueOf(500000))
                .finalAmount(BigDecimal.valueOf(450000))
                .message("Áp dụng thành công mã SALE10: Giảm 10%")
                .valid(true)
                .build();

        when(couponService.validateCoupon(any(ValidateCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("SALE10"))
                .andExpect(jsonPath("$.data.discountAmount").value(50000))
                .andExpect(jsonPath("$.data.finalAmount").value(450000))
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/coupons/validate: Lỗi 400 Bad Request khi mã giảm giá để trống")
    void validateCoupon_BlankCode_Returns400() throws Exception {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("")
                .orderValue(BigDecimal.valueOf(500000))
                .build();

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/coupons/validate: Lỗi 400 Bad Request khi giá trị đơn hàng <= 0")
    void validateCoupon_InvalidOrderValue_Returns400() throws Exception {
        ValidateCouponRequest request = ValidateCouponRequest.builder()
                .code("SALE10")
                .orderValue(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

