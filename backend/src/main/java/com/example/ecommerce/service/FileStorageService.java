package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service lưu trữ tệp tin tải lên (hình ảnh sản phẩm, danh mục, v.v.).
 * Hỗ trợ lưu trữ an toàn, kiểm tra định dạng và phòng chống Directory Traversal.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Thư mục lưu trữ tệp tin upload khởi tạo tại: {}", this.rootLocation);
        } catch (IOException e) {
            log.error("Không thể khởi tạo thư mục upload: {}", e.getMessage());
            throw new BusinessException("Không thể tạo thư mục lưu trữ tệp tin");
        }
    }

    /**
     * Lưu trữ hình ảnh tải lên từ client (MultipartFile).
     *
     * @param file Tệp tin tải lên
     * @return FileUploadResponse chứa đường dẫn URL và thông tin tệp tin
     */
    public FileUploadResponse storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp tin hình ảnh cần tải lên");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Kích thước tệp tin vượt quá giới hạn 10MB");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.jpg"
        );

        // Kiểm tra phần mở rộng
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng: .jpg, .jpeg, .png, .webp, .gif");
        }

        // Kiểm tra MIME content type nếu có
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("Loại MIME của tệp tin không hợp lệ: " + contentType);
            }
        }

        // Đặt tên tệp tin duy nhất phòng ngừa trùng lặp và bảo mật
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;

        try {
            Path destinationFile = this.rootLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            // Phòng ngừa Directory Traversal Attack
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new BusinessException("Không thể lưu trữ tệp tin ra ngoài thư mục quy định");
            }

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Lưu tệp tin thành công: {} (Kích thước: {} bytes)", uniqueFileName, file.getSize());

            // Đường dẫn URL tương đối mà client và Nginx có thể truy cập qua /api/v1/uploads/**
            String url = "/api/v1/uploads/" + uniqueFileName;

            return FileUploadResponse.builder()
                    .url(url)
                    .fileName(uniqueFileName)
                    .originalFileName(originalFilename)
                    .size(file.getSize())
                    .contentType(contentType != null ? contentType : "image/jpeg")
                    .build();

        } catch (IOException e) {
            log.error("Lỗi khi lưu tệp tin {}: {}", originalFilename, e.getMessage());
            throw new BusinessException("Lưu trữ tệp tin thất bại. Vui lòng thử lại!");
        }
    }
}

