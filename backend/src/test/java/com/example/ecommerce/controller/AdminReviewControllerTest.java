package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.service.RefreshTokenService;
import com.example.ecommerce.service.ReviewService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Test bao phủ Controller AdminReviewController.
 * Phụ trách: Đỗ Hoàng Anh (DHA), Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-DHA] & [TSK-D14-CCT]
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_review_ctrl_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    // =========================================================================
    // 1. TESTS CHO GET ALL REVIEWS (ADMIN)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/admin/reviews: Admin lấy danh sách toàn bộ đánh giá có phân trang -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void getAllReviews_asAdmin_returns200() throws Exception {
        ReviewResponse r = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .productName("Sản phẩm mẫu")
                .userId(2L)
                .userFullName("Trần Thị B")
                .rating(5)
                .comment("Sản phẩm tuyệt vời, chuẩn form!")
                .images(List.of("/api/v1/uploads/img1.jpg"))
                .isHidden(false)
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();

        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(List.of(r))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(reviewService.getAllReviewsForAdmin(any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[0].images[0]").value("/api/v1/uploads/img1.jpg"))
                .andExpect(jsonPath("$.data.content[0].isVisible").value(true))
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

    // =========================================================================
    // 2. TESTS CHO REPLY REVIEW (ADMIN)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/admin/reviews/{id}/reply: Admin phản hồi đánh giá -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void replyReview_asAdmin_returns200() throws Exception {
        AdminReplyRequest request = new AdminReplyRequest("Cảm ơn bạn đã ủng hộ shop!");

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .adminReply("Cảm ơn bạn đã ủng hộ shop!")
                .adminReplyAt(LocalDateTime.now())
                .build();

        when(reviewService.replyReview(eq(1L), any(AdminReplyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/reviews/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phản hồi đánh giá thành công"))
                .andExpect(jsonPath("$.data.adminReply").value("Cảm ơn bạn đã ủng hộ shop!"));

        verify(reviewService, times(1)).replyReview(eq(1L), any(AdminReplyRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/admin/reviews/{id}/reply: Khách hàng (CUSTOMER) gửi phản hồi -> 403 Forbidden")
    @WithMockUser(username = "user@eshop.com", roles = {"CUSTOMER"})
    void replyReview_asCustomer_returns403() throws Exception {
        AdminReplyRequest request = new AdminReplyRequest("Phản hồi trái phép");

        mockMvc.perform(post("/api/v1/admin/reviews/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).replyReview(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/admin/reviews/{id}/reply: Nội dung phản hồi rỗng -> 400 Bad Request")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void replyReview_blankContent_returns400() throws Exception {
        AdminReplyRequest request = new AdminReplyRequest("   ");

        mockMvc.perform(post("/api/v1/admin/reviews/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).replyReview(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/reviews/{id}/reply: Admin cập nhật phản hồi đánh giá -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void updateReplyReview_asAdmin_returns200() throws Exception {
        AdminReplyRequest request = new AdminReplyRequest("Cảm ơn bạn rất nhiều!");

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .adminReply("Cảm ơn bạn rất nhiều!")
                .adminReplyAt(LocalDateTime.now())
                .build();

        when(reviewService.replyReview(eq(1L), any(AdminReplyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/reviews/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật phản hồi thành công"))
                .andExpect(jsonPath("$.data.adminReply").value("Cảm ơn bạn rất nhiều!"));

        verify(reviewService, times(1)).replyReview(eq(1L), any(AdminReplyRequest.class));
    }

    // =========================================================================
    // 3. TESTS CHO TOGGLE VISIBILITY (ADMIN)
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/admin/reviews/{id}/visibility: Admin chuyển đổi ẩn/hiện review -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void toggleReviewVisibility_asAdmin_returns200() throws Exception {
        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .rating(5)
                .comment("Sản phẩm tốt")
                .isHidden(true)
                .isVisible(false)
                .build();

        when(reviewService.toggleReviewVisibility(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/reviews/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isHidden").value(true));
    }

    @Test
    @DisplayName("Admin toggle ẩn review qua PUT /toggle-status thành công -> 200 OK với isVisible=false")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void toggleReviewVisibility_asAdmin_success() throws Exception {
        ReviewResponse response = ReviewResponse.builder()
                .id(10L)
                .isVisible(false)
                .isHidden(true)
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
    @DisplayName("Khách hàng (CUSTOMER) gọi API toggle review -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void toggleReviewVisibility_customer_forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).toggleReviewVisibility(anyLong());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API toggle review -> 401 Unauthorized")
    void toggleReviewVisibility_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).toggleReviewVisibility(anyLong());
    }

    // =========================================================================
    // 4. TESTS CHO DELETE REVIEW (ADMIN)
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/v1/admin/reviews/{id}: Admin xóa review -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void deleteReview_asAdmin_returns200() throws Exception {
        doNothing().when(reviewService).deleteReviewByAdmin(1L);

        mockMvc.perform(delete("/api/v1/admin/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa đánh giá thành công"));

        verify(reviewService, times(1)).deleteReviewByAdmin(1L);
    }

    @Test
    @DisplayName("Admin xóa review thành công -> 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void deleteReview_asAdmin_success() throws Exception {
        doNothing().when(reviewService).deleteReviewByAdmin(10L);

        mockMvc.perform(delete("/api/v1/admin/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa đánh giá thành công"));

        verify(reviewService, times(1)).deleteReviewByAdmin(10L);
    }

    @Test
    @DisplayName("Customer gọi API Admin xóa review -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void deleteReview_customer_forbidden() throws Exception {
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
}
