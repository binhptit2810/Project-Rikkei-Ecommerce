package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.AdminReplyRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ReviewResponse;
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
 * Phụ trách: Đỗ Hoàng Anh (DHA)
 * Task ID: [TSK-D14-DHA] Thiết Kế UI Upload Ảnh Đánh Giá, Phản Hồi Admin & Trang Quản Lý Đánh Giá
 */
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

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("GET /api/v1/admin/reviews: Admin lấy danh sách toàn bộ đánh giá có phân trang -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void getAllReviews_asAdmin_returns200() throws Exception {
        ReviewResponse r = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(2L)
                .userFullName("Trần Thị B")
                .rating(5)
                .comment("Sản phẩm tuyệt vời, chuẩn form!")
                .images(List.of("/api/v1/uploads/img1.jpg"))
                .isHidden(false)
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
                .andExpect(jsonPath("$.data.content[0].comment").value("Sản phẩm tuyệt vời, chuẩn form!"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reviews: Người dùng role CUSTOMER bị chặn -> 403 Forbidden")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getAllReviews_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/reviews/{id}/reply: Admin gửi phản hồi đánh giá -> 200 OK")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void replyReview_asAdmin_returns200() throws Exception {
        AdminReplyRequest request = AdminReplyRequest.builder()
                .reply("Cảm ơn bạn đã tin tưởng ủng hộ shop Rikkei Store!")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .rating(5)
                .comment("Sản phẩm tốt")
                .adminReply(request.getReply())
                .adminReplyAt(LocalDateTime.now())
                .build();

        when(reviewService.replyReview(eq(1L), any(AdminReplyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/reviews/1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminReply").value("Cảm ơn bạn đã tin tưởng ủng hộ shop Rikkei Store!"));
    }

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
                .build();

        when(reviewService.toggleReviewVisibility(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/reviews/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isHidden").value(true));
    }

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
}
