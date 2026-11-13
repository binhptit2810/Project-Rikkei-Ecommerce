package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.FileUploadResponse;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Service lưu trữ tệp tin tải lên (hình ảnh sản phẩm, danh mục, đánh giá, v.v.).
 * Hỗ trợ lưu trữ an toàn, kiểm tra chữ ký Magic Bytes, phòng chống Directory Traversal và Decompression Bomb.
 * Phụ trách: Cao Chí Thiện (CCT)
 * Task ID: [TSK-D14-CCT] Tích Hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB cho upload thông thường
    public static final long MAX_REVIEW_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB cho ảnh đánh giá (< 5MB)
    public static final int MAX_IMAGE_DIMENSION = 8000; // Tối đa 8000px chiều rộng/cao
    public static final long MAX_IMAGE_PIXELS = 25_000_000L; // Tối đa 25 triệu pixels

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> REVIEW_ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    private static final Set<String> REVIEW_ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    // Magic Bytes (File Signatures)
    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] PNG_MAGIC_BYTES = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    private Path rootLocation;
    private Path reviewsLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            this.reviewsLocation = this.rootLocation.resolve("reviews").normalize();
            Files.createDirectories(this.rootLocation);
            Files.createDirectories(this.reviewsLocation);
            log.info("Thư mục lưu trữ tệp tin upload khởi tạo tại: {}", this.rootLocation);
            log.info("Thư mục lưu trữ ảnh đánh giá khởi tạo tại: {}", this.reviewsLocation);
        } catch (IOException e) {
            log.error("Không thể khởi tạo thư mục upload: {}", e.getMessage());
            throw new BusinessException("Không thể tạo thư mục lưu trữ tệp tin");
        }
    }

    /**
     * Lưu trữ hình ảnh tải lên từ client (MultipartFile) thông thường (cho Admin upload sản phẩm/danh mục).
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
        String extension = getFileExtension(originalFilename);
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

    /**
     * Xác thực tệp tin ảnh đánh giá nghiêm ngặt (Magic Bytes, định dạng, dung lượng < 5MB, giải mã ảnh thực tế).
     *
     * @param file Tệp tin MultipartFile
     * @return byte[] nội dung tệp tin đã nạp để tái sử dụng
     */
    public byte[] validateReviewImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tệp tin hình ảnh cần tải lên");
        }

        // 1. Dung lượng mỗi ảnh phải nhỏ hơn 5 MB (>= 5MB bị từ chối)
        if (file.getSize() >= MAX_REVIEW_IMAGE_SIZE) {
            throw new BadRequestException("Dung lượng mỗi ảnh đánh giá phải nhỏ hơn 5 MB");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "review.jpg"
        );

        // 2. Kiểm tra phần mở rộng tệp tin: Chỉ chấp nhận .jpg, .jpeg, .png
        String extension = getFileExtension(originalFilename);
        if (!REVIEW_ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng ảnh: .jpg, .jpeg, .png");
        }

        // 3. Kiểm tra HTTP Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !REVIEW_ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Loại MIME của tệp tin không hợp lệ. Chỉ chấp nhận image/jpeg hoặc image/png");
        }

        // Đọc toàn bộ nội dung byte của file (đã đảm bảo < 5MB nên an toàn trong bộ nhớ)
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Không thể đọc dữ liệu tệp tin upload: {}", e.getMessage());
            throw new BadRequestException("Không thể đọc tệp tin tải lên");
        }

        if (fileBytes.length < 8) {
            throw new BadRequestException("Tệp tin không đủ độ dài để là hình ảnh hợp lệ");
        }

        // 4. Kiểm tra chữ ký file / Magic Bytes
        boolean isJpeg = matchesMagicBytes(fileBytes, JPEG_MAGIC_BYTES);
        boolean isPng = matchesMagicBytes(fileBytes, PNG_MAGIC_BYTES);

        if (!isJpeg && !isPng) {
            log.warn("Tệp tin {} giả mạo định dạng ảnh, không khớp Magic Bytes JPEG/PNG", originalFilename);
            throw new BadRequestException("Nội dung tệp không đúng định dạng ảnh hợp lệ (chữ ký tệp/Magic Bytes không hợp lệ)");
        }

        // Kiểm tra tính nhất quán giữa extension và Magic Bytes
        if (isJpeg && !extension.equals(".jpg") && !extension.equals(".jpeg")) {
            throw new BadRequestException("Phần mở rộng tệp tin không khớp với cấu trúc ảnh JPEG");
        }
        if (isPng && !extension.equals(".png")) {
            throw new BadRequestException("Phần mở rộng tệp tin không khớp với cấu trúc ảnh PNG");
        }

        // 5. Xác minh tệp tin thực sự giải mã được thành ảnh và phòng chống Decompression Bomb
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (image == null) {
                log.warn("ImageIO không thể giải mã hình ảnh từ tệp {}", originalFilename);
                throw new BadRequestException("Tệp tin bị hỏng hoặc không thể giải mã thành hình ảnh hợp lệ");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                throw new BadRequestException("Kích thước hình ảnh không hợp lệ");
            }

            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION || ((long) width * height) > MAX_IMAGE_PIXELS) {
                log.warn("Ảnh {} có kích thước vượt ngưỡng an toàn: {}x{} (tổng {} pixels)",
                        originalFilename, width, height, (long) width * height);
                throw new BadRequestException("Kích thước điểm ảnh vượt quá ngưỡng an toàn cho phép (tối đa 8000x8000 pixels)");
            }

        } catch (IOException e) {
            log.error("Lỗi khi giải mã hình ảnh {}: {}", originalFilename, e.getMessage());
            throw new BadRequestException("Lỗi trong quá trình giải mã tệp tin hình ảnh");
        }

        return fileBytes;
    }

    /**
     * Lưu trữ hình ảnh đánh giá vào thư mục con `uploads/reviews/` an toàn.
     *
     * @param file Tệp tin tải lên
     * @param validatedBytes Nội dung byte đã qua kiểm tra nghiêm ngặt
     * @return FileUploadResponse chứa đường dẫn tương đối và URL
     */
    public FileUploadResponse storeReviewImage(MultipartFile file, byte[] validatedBytes) {
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "review.jpg"
        );
        String extension = getFileExtension(originalFilename);

        // Sinh tên file ngẫu nhiên UUID
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;

        try {
            // Đảm bảo thư mục uploads/reviews/ tồn tại
            if (this.reviewsLocation == null) {
                this.reviewsLocation = this.rootLocation.resolve("reviews").normalize();
            }
            Files.createDirectories(this.reviewsLocation);

            Path destinationFile = this.reviewsLocation.resolve(uniqueFileName).normalize().toAbsolutePath();

            // Chặn Directory Traversal
            if (!destinationFile.startsWith(this.reviewsLocation) || !destinationFile.startsWith(this.rootLocation)) {
                throw new BusinessException("Không thể lưu trữ tệp tin ra ngoài thư mục quy định");
            }

            Files.write(destinationFile, validatedBytes);
            log.info("Lưu ảnh đánh giá thành công: reviews/{} (Kích thước: {} bytes)", uniqueFileName, validatedBytes.length);

            // Đường dẫn URL công khai và đường dẫn tương đối lưu database
            String relativePath = "reviews/" + uniqueFileName;
            String url = "/api/v1/uploads/" + relativePath;

            return FileUploadResponse.builder()
                    .url(url)
                    .fileName(relativePath)
                    .originalFileName(originalFilename)
                    .size((long) validatedBytes.length)
                    .contentType(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("Lỗi khi lưu ảnh đánh giá {}: {}", originalFilename, e.getMessage());
            throw new BusinessException("Lưu trữ ảnh đánh giá thất bại. Vui lòng thử lại!");
        }
    }

    /**
     * Xóa tệp tin khỏi Local Storage an toàn, phòng chống Path Traversal.
     * Hỗ trợ nhận đường dẫn URL (/api/v1/uploads/...) hoặc đường dẫn tương đối (reviews/...).
     *
     * @param fileUrlOrPath Đường dẫn URL hoặc đường dẫn tương đối của file
     * @return true nếu xóa thành công hoặc file không tồn tại (idempotent), false nếu lỗi IO
     */
    public boolean deleteFile(String fileUrlOrPath) {
        if (!StringUtils.hasText(fileUrlOrPath)) {
            return true;
        }

        try {
            String clean = fileUrlOrPath.trim();
            // Loại bỏ query params nếu có
            int queryIdx = clean.indexOf('?');
            if (queryIdx >= 0) {
                clean = clean.substring(0, queryIdx);
            }

            // Loại bỏ tiền tố URL nếu có
            if (clean.startsWith("/api/v1/uploads/")) {
                clean = clean.substring("/api/v1/uploads/".length());
            } else if (clean.startsWith("/uploads/")) {
                clean = clean.substring("/uploads/".length());
            } else if (clean.startsWith("api/v1/uploads/")) {
                clean = clean.substring("api/v1/uploads/".length());
            } else if (clean.startsWith("uploads/")) {
                clean = clean.substring("uploads/".length());
            }

            // Chuẩn hóa tên đường dẫn
            clean = StringUtils.cleanPath(clean);
            while (clean.startsWith("/") || clean.startsWith("\\")) {
                clean = clean.substring(1);
            }

            Path targetFile = this.rootLocation.resolve(clean).normalize().toAbsolutePath();

            // Phòng ngừa Directory Traversal Attack: mục tiêu phải nằm trong rootLocation
            if (!targetFile.startsWith(this.rootLocation)) {
                log.warn("Cảnh báo bảo mật: Phát hiện âm mưu Path Traversal khi yêu cầu xóa tệp: {}", fileUrlOrPath);
                return false;
            }

            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
                log.info("Đã xóa tệp tin thành công khỏi Local Storage: {}", targetFile);
            } else {
                log.info("Tệp tin cần xóa không tồn tại trên hệ thống (bỏ qua): {}", targetFile);
            }
            return true;

        } catch (IOException e) {
            log.error("Lỗi I/O khi thực hiện xóa tệp tin {}: {}", fileUrlOrPath, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Lỗi không xác định khi xóa tệp tin {}: {}", fileUrlOrPath, e.getMessage());
            return false;
        }
    }

    /**
     * So sánh các byte đầu tiên của mảng byte với mảng mẫu Magic Bytes.
     */
    private boolean matchesMagicBytes(byte[] fileBytes, byte[] magic) {
        if (fileBytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (fileBytes[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Trích xuất phần mở rộng của tệp tin ở dạng chữ thường (.jpg, .png,...).
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
