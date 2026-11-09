package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        fileStorageService.init();
    }

    @Test
    @DisplayName("Lưu tệp tin hợp lệ thành công")
    void testStoreFileSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "sample-image-bytes".getBytes()
        );

        FileUploadResponse response = fileStorageService.storeFile(file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("test-image.png");
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/");
        assertThat(response.getSize()).isEqualTo("sample-image-bytes".length());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tệp tin bị null hoặc rỗng")
    void testStoreFileNullOrEmpty() {
        assertThatThrownBy(() -> fileStorageService.storeFile(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng chọn tệp tin");

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Vui lòng chọn tệp tin");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi kích thước tệp vượt quá giới hạn 10MB")
    void testStoreFileTooLarge() {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeBytes
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(largeFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kích thước tệp tin vượt quá giới hạn 10MB");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi phần mở rộng không được hỗ trợ")
    void testStoreFileInvalidExtension() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "evil".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(exeFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Định dạng tệp không được hỗ trợ");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi contentType MIME không hợp lệ")
    void testStoreFileInvalidMimeType() {
        MockMultipartFile fakePng = new MockMultipartFile(
                "file",
                "fake.png",
                "text/html",
                "<html></html>".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.storeFile(fakePng))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Loại MIME của tệp tin không hợp lệ");
    }
}
