package com.example.ecommerce.controller;

import com.example.ecommerce.dto.request.ReviewCreateRequest;
import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.response.ProductRatingSummaryResponse;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test cho ReviewController.
 * Phụ trách: Đỗ Đức Mạnh (DDM)
 * Task ID: [TSK-D11-DDM] Xây Dựng Phân Hệ Đánh Giá Sản Phẩm (Product Review & Rating Backend)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_review_ctrl;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("GET /api/v1/products/{id}/reviews: Khách xem danh sách đánh giá công khai trả 200 OK")
    void getProductReviews_public_returns200() throws Exception {
        ReviewResponse r = ReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(1L)
                .userFullName("Nguyễn Văn A")
                .rating(5)
                .comment("Hàng rất tốt")
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

        when(reviewService.getProductReviews(eq(10L), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[0].comment").value("Hàng rất tốt"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}/reviews/summary: Khách xem thống kê rating công khai trả 200 OK")
    void getProductRatingSummary_public_returns200() throws Exception {
        ProductRatingSummaryResponse summary = ProductRatingSummaryResponse.builder()
                .ratingAverage(4.8)
                .totalReviews(20)
                .ratingCounts(Map.of(5, 16L, 4, 4L, 3, 0L, 2, 0L, 1, 0L))
                .build();

        when(reviewService.getProductRatingSummary(10L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/products/10/reviews/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ratingAverage").value(4.8))
                .andExpect(jsonPath("$.data.totalReviews").value(20));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/products/{id}/reviews: Đã đăng nhập và gửi đánh giá hợp lệ trả 201 CREATED")
    void createReview_authenticated_validRequest_returns201() throws Exception {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Sản phẩm tuyệt vời, giao hàng nhanh!")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(101L)
                .productId(10L)
                .userId(1L)
                .userFullName("Nguyễn Văn A")
                .rating(5)
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(eq(10L), any(ReviewCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đánh giá sản phẩm thành công"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/products/{id}/reviews: Chưa đăng nhập bị từ chối 401 UNAUTHORIZED")
    void createReview_unauthenticated_returns401() throws Exception {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Đánh giá ẩn danh")
                .build();

        mockMvc.perform(post("/api/v1/products/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/v1/products/{id}/reviews: Rating ngoài khoảng 1-5 sao trả về 400 BAD REQUEST do validation")
    void createReview_invalidRating_returns400() throws Exception {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(6) // Rating > 5 không hợp lệ
                .comment("Đánh giá 6 sao")
                .build();

        mockMvc.perform(post("/api/v1/products/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

