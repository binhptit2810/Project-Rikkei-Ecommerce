package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.service.RefreshTokenService;
import com.example.ecommerce.service.ReviewService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_admin_review_ctrl;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    // =========================================================================
    // TESTS CHO TOGGLE STATUS
    // =========================================================================

    @Test
    @DisplayName("Admin toggle ẩn review thành công -> 200 OK với isVisible=false")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void toggleReviewVisibility_asAdmin_success() throws Exception {
        ReviewResponse response = ReviewResponse.builder()
                .id(10L)
                .isVisible(false)
                .rating(5)
                .comment("Đánh giá tốt")
                .build();

        when(reviewService.toggleReviewVisibility(10L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/reviews/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ẩn đánh giá thành công"))
                .andExpect(jsonPath("$.data.isVisible").value(false));
    }

    @Test
    @DisplayName("Customer gọi API Admin toggle review -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void toggleReviewVisibility_asCustomer_forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API Admin toggle review -> 401 Unauthorized")
    void toggleReviewVisibility_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin toggle review không tồn tại -> 404 Not Found")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void toggleReviewVisibility_notFound_returns404() throws Exception {
        when(reviewService.toggleReviewVisibility(999L))
                .thenThrow(new ResourceNotFoundException("Đánh giá không tồn tại với ID: 999"));

        mockMvc.perform(put("/api/v1/admin/reviews/999/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // TESTS CHO DELETE REVIEW
    // =========================================================================

    @Test
    @DisplayName("Admin xóa review thành công -> 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void deleteReview_asAdmin_success() throws Exception {
        doNothing().when(reviewService).deleteReviewByAdmin(10L);

        mockMvc.perform(delete("/api/v1/admin/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa đánh giá vĩnh viễn thành công"));

        verify(reviewService, times(1)).deleteReviewByAdmin(10L);
    }

    @Test
    @DisplayName("Customer gọi API Admin xóa review -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void deleteReview_asCustomer_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).deleteReviewByAdmin(anyLong());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API Admin xóa review -> 401 Unauthorized")
    void deleteReview_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).deleteReviewByAdmin(anyLong());
    }

    @Test
    @DisplayName("Admin xóa review không tồn tại -> 404 Not Found")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void deleteReview_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Đánh giá không tồn tại với ID: 999"))
                .when(reviewService).deleteReviewByAdmin(999L);

        mockMvc.perform(delete("/api/v1/admin/reviews/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // TESTS CHO GET ALL REVIEWS (ADMIN)
    // =========================================================================

    @Test
    @DisplayName("Admin lấy danh sách review thành công -> 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getAllReviews_asAdmin_success() throws Exception {
        ReviewResponse r = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .productName("Sản phẩm mẫu")
                .rating(5)
                .comment("Hài lòng")
                .isVisible(true)
                .build();

        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(List.of(r))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(reviewService.getAllReviewsForAdmin(eq(null), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].productName").value("Sản phẩm mẫu"));
    }

    @Test
    @DisplayName("Customer gọi API lấy danh sách review Admin -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getAllReviews_asCustomer_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API lấy danh sách review Admin -> 401 Unauthorized")
    void getAllReviews_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

