package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.UploadResponse;
import com.example.ecommerce.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg", ".bmp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml",
            "image/bmp"
    );

    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            log.info("Thư mục lưu trữ ảnh đã sẵn sàng: {}", this.uploadPath);
        } catch (IOException e) {
            log.error("Không thể khởi tạo thư mục lưu trữ ảnh: {}", this.uploadPath, e);
            throw new BusinessException("Không thể khởi tạo thư mục lưu trữ ảnh trên máy chủ");
        }
    }

    public UploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn một tệp hình ảnh để tải lên");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Kích thước tệp vượt quá giới hạn tối đa cho phép (5MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Định dạng tệp không hợp lệ. Chỉ chấp nhận các định dạng ảnh (JPG, PNG, WEBP, GIF, SVG)");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Phần mở rộng của tệp không được hỗ trợ: " + extension);
        }

        // Tạo tên tệp ngẫu nhiên chống trùng lặp và an toàn
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path targetLocation = this.uploadPath.resolve(uniqueFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Tải lên ảnh thành công: {} -> {}", originalFilename, targetLocation);
        } catch (IOException e) {
            log.error("Lỗi khi lưu trữ tệp ảnh: {}", originalFilename, e);
            throw new BusinessException("Không thể lưu tệp ảnh lên máy chủ. Vui lòng thử lại!");
        }

        String fileUrl = "/api/v1/uploads/" + uniqueFileName;
        return UploadResponse.builder()
                .url(fileUrl)
                .fileName(uniqueFileName)
                .size(file.getSize())
                .build();
    }
}

