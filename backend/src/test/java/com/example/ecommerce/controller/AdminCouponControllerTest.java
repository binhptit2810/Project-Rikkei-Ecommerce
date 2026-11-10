package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.CreateCouponRequest;
import com.example.ecommerce.dto.request.UpdateCouponRequest;
import com.example.ecommerce.dto.response.CouponResponse;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_coupon;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private CouponResponse createSampleCouponResponse() {
        return CouponResponse.builder()
                .id(1L)
                .code("SUMMER2026")
                .discountType(DiscountType.PERCENT)
                .discountTypeDescription("Giảm theo % giá trị đơn")
                .discountValue(BigDecimal.valueOf(15))
                .minOrderValue(BigDecimal.valueOf(200000))
                .maxDiscount(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usedCount(10)
                .expireAt(LocalDateTime.now().plusDays(30))
                .active(true)
                .isExpired(false)
                .isUsageLimitReached(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/coupons: Admin lấy toàn bộ danh sách mã giảm giá thành công")
    void getAllCoupons_asAdmin_returns200() throws Exception {
        when(couponService.getAllCouponsForAdmin()).thenReturn(List.of(createSampleCouponResponse()));

        mockMvc.perform(get("/api/v1/admin/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("SUMMER2026"))
                .andExpect(jsonPath("$.data[0].discountValue").value(15));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/vouchers: Đường dẫn alias /admin/vouchers cũng trả về 200 OK")
    void getAllVouchers_alias_asAdmin_returns200() throws Exception {
        when(couponService.getAllCouponsForAdmin()).thenReturn(List.of(createSampleCouponResponse()));

        mockMvc.perform(get("/api/v1/admin/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("SUMMER2026"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/coupons: Khách vãng lai không đăng nhập bị chặn 401 hoặc 403")
    void getAllCoupons_unauthenticated_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/v1/admin/coupons: Tài khoản CUSTOMER bị chặn 403 Forbidden")
    void getAllCoupons_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/v1/admin/coupons/{id}: Lấy chi tiết mã giảm giá theo ID thành công")
    void getCouponById_asAdmin_returns200() throws Exception {
        when(couponService.getCouponById(1L)).thenReturn(createSampleCouponResponse());

        mockMvc.perform(get("/api/v1/admin/coupons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("SUMMER2026"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/coupons: Tạo mới mã giảm giá thành công trả về 201 Created")
    void createCoupon_validRequest_returns201() throws Exception {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("NEWYEAR50")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderValue(BigDecimal.valueOf(300000))
                .usageLimit(200)
                .active(true)
                .build();

        CouponResponse response = createSampleCouponResponse();
        response.setCode("NEWYEAR50");
        when(couponService.createCoupon(any(CreateCouponRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("NEWYEAR50"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/admin/coupons: Mã trống trả về 400 Bad Request")
    void createCoupon_blankCode_returns400() throws Exception {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .build();

        mockMvc.perform(post("/api/v1/admin/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/admin/coupons/{id}: Cập nhật mã giảm giá thành công trả về 200 OK")
    void updateCoupon_validRequest_returns200() throws Exception {
        UpdateCouponRequest request = UpdateCouponRequest.builder()
                .discountValue(BigDecimal.valueOf(20))
                .build();

        CouponResponse response = createSampleCouponResponse();
        response.setDiscountValue(BigDecimal.valueOf(20));
        when(couponService.updateCoupon(eq(1L), any(UpdateCouponRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discountValue").value(20));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("DELETE /api/v1/admin/coupons/{id}: Xóa mã giảm giá thành công trả về 200 OK")
    void deleteCoupon_asAdmin_returns200() throws Exception {
        doNothing().when(couponService).deleteCoupon(1L);

        mockMvc.perform(delete("/api/v1/admin/coupons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("PATCH /api/v1/admin/coupons/{id}/toggle-status: Đổi trạng thái kích hoạt thành công")
    void toggleCouponStatus_asAdmin_returns200() throws Exception {
        CouponResponse response = createSampleCouponResponse();
        response.setActive(false);
        when(couponService.toggleCouponStatus(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/coupons/1/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
