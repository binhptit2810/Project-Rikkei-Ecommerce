package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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

    private byte[] createValidImageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Lưu tệp tin hợp lệ thành công (storeFile)")
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
    @DisplayName("Ném ngoại lệ khi kích thước tệp vượt quá giới hạn 10MB (storeFile)")
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
    @DisplayName("Ném ngoại lệ khi phần mở rộng không được hỗ trợ (storeFile)")
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
    @DisplayName("Ném ngoại lệ khi contentType MIME không hợp lệ (storeFile)")
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

    // =========================================================================
    // TESTS CHO ẢNH ĐÁNH GIÁ (TSK-D14-CCT)
    // =========================================================================

    @Test
    @DisplayName("Ảnh đánh giá JPEG hợp lệ -> validate & lưu thành công vào thư mục reviews")
    void testStoreReviewImage_jpeg_success() throws IOException {
        byte[] jpegBytes = createValidImageBytes("jpg", 200, 200);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid-photo.jpg",
                "image/jpeg",
                jpegBytes
        );

        byte[] validatedBytes = fileStorageService.validateReviewImage(file);
        assertThat(validatedBytes).isEqualTo(jpegBytes);

        FileUploadResponse response = fileStorageService.storeReviewImage(file, validatedBytes);
        assertThat(response).isNotNull();
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/reviews/");
        assertThat(response.getFileName()).startsWith("reviews/");
        assertThat(response.getOriginalFileName()).isEqualTo("valid-photo.jpg");

        // Kiểm tra file thực sự được ghi trên đĩa
        Path written = tempDir.resolve(response.getFileName());
        assertThat(Files.exists(written)).isTrue();
    }

    @Test
    @DisplayName("Ảnh đánh giá PNG hợp lệ -> validate & lưu thành công")
    void testStoreReviewImage_png_success() throws IOException {
        byte[] pngBytes = createValidImageBytes("png", 150, 150);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid-photo.png",
                "image/png",
                pngBytes
        );

        byte[] validatedBytes = fileStorageService.validateReviewImage(file);
        assertThat(validatedBytes).isEqualTo(pngBytes);

        FileUploadResponse response = fileStorageService.storeReviewImage(file, validatedBytes);
        assertThat(response).isNotNull();
        assertThat(response.getUrl()).startsWith("/api/v1/uploads/reviews/");
    }

    @Test
    @DisplayName("Ảnh đánh giá có dung lượng >= 5MB bị từ chối 400")
    void testValidateReviewImage_exceeds5MB_throwsBadRequestException() {
        byte[] largeBytes = new byte[5 * 1024 * 1024]; // Đúng 5MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-photo.jpg",
                "image/jpeg",
                largeBytes
        );

        assertThatThrownBy(() -> fileStorageService.validateReviewImage(largeFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nhỏ hơn 5 MB");
    }

    @Test
    @DisplayName("Ảnh đánh giá giả mạo Magic Bytes (text giả ảnh jpg) bị từ chối 400")
    void testValidateReviewImage_fakeMagicBytes_throwsBadRequestException() {
        byte[] fakeBytes = "This is not an image file".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                fakeBytes
        );

        assertThatThrownBy(() -> fileStorageService.validateReviewImage(fakeFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Magic Bytes");
    }

    @Test
    @DisplayName("Ảnh đánh giá định dạng GIF hoặc WEBP không nằm trong JPG/PNG bị từ chối 400")
    void testValidateReviewImage_unsupportedExtension_throwsBadRequestException() throws IOException {
        byte[] gifBytes = createValidImageBytes("gif", 50, 50);
        MockMultipartFile gifFile = new MockMultipartFile(
                "file",
                "anim.gif",
                "image/gif",
                gifBytes
        );

        assertThatThrownBy(() -> fileStorageService.validateReviewImage(gifFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ chấp nhận các định dạng ảnh: .jpg, .jpeg, .png");
    }

    @Test
    @DisplayName("Xóa tệp tin hợp lệ thành công")
    void testDeleteFile_success() throws IOException {
        Path testFile = tempDir.resolve("reviews").resolve("to-delete.jpg");
        Files.createDirectories(testFile.getParent());
        Files.write(testFile, "test-data".getBytes());
        assertThat(Files.exists(testFile)).isTrue();

        boolean deleted = fileStorageService.deleteFile("/api/v1/uploads/reviews/to-delete.jpg");
        assertThat(deleted).isTrue();
        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("Xóa tệp tin không tồn tại trả về true an toàn (idempotent)")
    void testDeleteFile_nonExistent_returnsTrue() {
        boolean result = fileStorageService.deleteFile("/api/v1/uploads/reviews/non-existent-123.jpg");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Xóa tệp tin có hành vi Path Traversal bị chặn và trả về false")
    void testDeleteFile_pathTraversal_blocked() {
        boolean result = fileStorageService.deleteFile("../../etc/passwd");
        assertThat(result).isFalse();
    }
}
