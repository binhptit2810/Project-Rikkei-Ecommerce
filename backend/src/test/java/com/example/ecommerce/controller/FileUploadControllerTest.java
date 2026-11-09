package com.example.ecommerce.controller;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.service.FileStorageService;
import com.example.ecommerce.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_upload;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-secret-key-for-unit-tests-must-be-at-least-32-characters-long",
    "jwt.expiration=3600000"
})
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("Admin upload ảnh hợp lệ -> 200 OK với URL ảnh")
    @WithMockUser(username = "admin@eshop.com", roles = {"ADMIN"})
    void uploadImage_asAdmin_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-product.png",
                "image/png",
                "dummy image content".getBytes()
        );

        FileUploadResponse response = FileUploadResponse.builder()
                .url("/api/v1/uploads/test-product-123.png")
                .fileName("test-product-123.png")
                .originalFileName("test-product.png")
                .size((long) file.getBytes().length)
                .contentType("image/png")
                .build();

        when(fileStorageService.storeFile(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/admin/uploads").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("/api/v1/uploads/test-product-123.png"))
                .andExpect(jsonPath("$.data.fileName").value("test-product-123.png"));
    }

    @Test
    @DisplayName("Khách hoặc người dùng không có quyền ADMIN -> 403 Forbidden")
    @WithMockUser(username = "user@eshop.com", roles = {"CUSTOMER"})
    void uploadImage_asCustomer_forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/uploads").file(file))
                .andExpect(status().isForbidden());
    }
}

