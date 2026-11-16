package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Test cho ReviewUploadController [TSK-D14-DHA].
 * Người phụ trách: Đỗ Hoàng Anh (MSV: B24DTCN300).
 */
@ExtendWith(MockitoExtension.class)
class ReviewUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ReviewUploadController reviewUploadController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewUploadController).build();
    }

    @Test
    @DisplayName("uploadReviewImage: Tải lên hình ảnh đánh giá thành công")
    void uploadReviewImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "review-photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        FileUploadResponse fileResponse = FileUploadResponse.builder()
                .fileName("review-photo-uuid.jpg")
                .url("/uploads/reviews/review-photo-uuid.jpg")
                .contentType("image/jpeg")
                .size(file.getSize())
                .build();

        when(fileStorageService.storeFile(any())).thenReturn(fileResponse);

        mockMvc.perform(multipart("/api/v1/reviews/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải hình ảnh đánh giá thành công"))
                .andExpect(jsonPath("$.data.fileName").value("review-photo-uuid.jpg"))
                .andExpect(jsonPath("$.data.url").value("/uploads/reviews/review-photo-uuid.jpg"));

        verify(fileStorageService).storeFile(any());
    }
}
