package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.ReviewImageResponse;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.service.RefreshTokenService;
import com.example.ecommerce.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_review_img_ctrl;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class ReviewImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("Khách hàng đã đăng nhập tải lên ảnh review hợp lệ -> 201 Created")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void uploadReviewImages_authenticated_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        ReviewImageResponse img = ReviewImageResponse.builder()
                .id(1L)
                .reviewId(10L)
                .imageUrl("/api/v1/uploads/reviews/uuid.jpg")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.uploadReviewImages(eq(10L), anyList())).thenReturn(List.of(img));

        mockMvc.perform(multipart("/api/v1/reviews/10/images").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải lên hình ảnh đánh giá thành công"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].imageUrl").value("/api/v1/uploads/reviews/uuid.jpg"));
    }

    @Test
    @DisplayName("Khách chưa đăng nhập tải lên ảnh review -> 401 Unauthorized")
    void uploadReviewImages_unauthenticated_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/reviews/10/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Khách hàng tải ảnh vào review của người khác (IDOR) -> 403 Forbidden")
    @WithMockUser(username = "attacker@example.com", roles = {"CUSTOMER"})
    void uploadReviewImages_notOwner_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        when(reviewService.uploadReviewImages(eq(10L), anyList()))
                .thenThrow(new AccessDeniedException("Bạn không có quyền đăng tải hình ảnh cho đánh giá của người khác"));

        mockMvc.perform(multipart("/api/v1/reviews/10/images").file(file))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Tải ảnh vào review không tồn tại -> 404 Not Found")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void uploadReviewImages_reviewNotFound_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        when(reviewService.uploadReviewImages(eq(999L), anyList()))
                .thenThrow(new ResourceNotFoundException("Đánh giá không tồn tại với ID: 999"));

        mockMvc.perform(multipart("/api/v1/reviews/999/images").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Tải ảnh vượt quá số lượng 3 ảnh -> 400 Bad Request")
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void uploadReviewImages_exceedsMaxLimit_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        when(reviewService.uploadReviewImages(eq(10L), anyList()))
                .thenThrow(new BadRequestException("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh."));

        mockMvc.perform(multipart("/api/v1/reviews/10/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mỗi đánh giá chỉ được đính kèm tối đa 3 ảnh."));
    }
}

