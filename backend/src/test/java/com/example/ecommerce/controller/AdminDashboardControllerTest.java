package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.DashboardStatsResponse;
import com.example.ecommerce.dto.response.ProductResponse;
import com.example.ecommerce.service.DashboardService;
import com.example.ecommerce.service.RefreshTokenService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test cho AdminDashboardController.
 * Phụ trách: Đặng Thanh Bình (Thành viên 4 - DTB)
 * Phối hợp: Vũ Đức Huy Hoàng [VDHH]
 *
 * Tiêu chí nghiệm thu (DoD):
 * - API GET /admin/dashboard/stats: Đếm tổng đơn hàng, doanh thu thực tế, đơn chờ duyệt.
 * - Truy vấn danh sách các sản phẩm có nguy cơ hết hàng (stock_quantity <= 5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_dashboard_controller;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("GET /admin/dashboard/stats: Quyền ADMIN trả 200 OK cùng đầy đủ chỉ số thống kê")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getDashboardStats_Admin_Success() throws Exception {
        ProductResponse p1 = ProductResponse.builder()
                .id(1L)
                .name("Sản phẩm sắp hết hàng 1")
                .stockQuantity(2)
                .price(BigDecimal.valueOf(100000))
                .build();

        ProductResponse p2 = ProductResponse.builder()
                .id(2L)
                .name("Sản phẩm sắp hết hàng 2")
                .stockQuantity(4)
                .price(BigDecimal.valueOf(200000))
                .build();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalOrders(50)
                .actualRevenue(BigDecimal.valueOf(25000000))
                .totalRevenue(BigDecimal.valueOf(25000000))
                .pendingOrders(8)
                .confirmedOrders(12)
                .completedOrders(20)
                .cancelledOrders(2)
                .totalProducts(120)
                .lowStockCount(2)
                .lowStockProducts(List.of(p1, p2))
                .totalUsers(200)
                .totalCustomers(190)
                .build();

        when(dashboardService.getDashboardStats(eq(5))).thenReturn(stats);

        mockMvc.perform(get("/admin/dashboard/stats?lowStockThreshold=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(50))
                .andExpect(jsonPath("$.data.actualRevenue").value(25000000))
                .andExpect(jsonPath("$.data.pendingOrders").value(8))
                .andExpect(jsonPath("$.data.lowStockCount").value(2))
                .andExpect(jsonPath("$.data.lowStockProducts[0].name").value("Sản phẩm sắp hết hàng 1"))
                .andExpect(jsonPath("$.data.lowStockProducts[0].stockQuantity").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard/stats: Đường dẫn tiền tố /api/v1/ hoạt động chuẩn xác")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getDashboardStats_ApiV1_Success() throws Exception {
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalOrders(10)
                .actualRevenue(BigDecimal.valueOf(5000000))
                .pendingOrders(2)
                .lowStockCount(0)
                .lowStockProducts(List.of())
                .build();

        when(dashboardService.getDashboardStats(eq(5))).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(10))
                .andExpect(jsonPath("$.data.actualRevenue").value(5000000))
                .andExpect(jsonPath("$.data.pendingOrders").value(2));
    }

    @Test
    @DisplayName("GET /admin/dashboard/stats: Vai trò CUSTOMER bị từ chối 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getDashboardStats_Customer_Returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/dashboard/stats: Chưa đăng nhập bị từ chối 401 Unauthorized")
    void getDashboardStats_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/dashboard/low-stock: Trả về danh sách sản phẩm có nguy cơ hết hàng")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getLowStockProducts_Success() throws Exception {
        ProductResponse p1 = ProductResponse.builder()
                .id(1L)
                .name("Sản phẩm tồn kho thấp")
                .stockQuantity(3)
                .build();

        when(dashboardService.getLowStockProducts(eq(5))).thenReturn(List.of(p1));

        mockMvc.perform(get("/admin/dashboard/low-stock?threshold=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(3));
    }
}

