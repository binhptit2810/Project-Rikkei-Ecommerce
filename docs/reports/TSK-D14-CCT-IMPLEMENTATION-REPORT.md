# BÁO CÁO NGHIỆM THU TRIỂN KHAI TOÀN DIỆN – [TSK-D14-CCT]
## TÍCH HỢP UPLOAD ẢNH ĐÁNH GIÁ (LOCAL STORAGE) & API QUẢN LÝ ĐÁNH GIÁ
### Rikkei Ecommerce Platform

---

- **Mã nhiệm vụ:** `[TSK-D14-CCT]`
- **Tên nhiệm vụ:** Tích hợp Upload Ảnh Đánh Giá (Local Storage) & API Quản Lý Đánh Giá (Ẩn/Hiện, Xóa Vĩnh Viễn)
- **Kỹ sư phụ trách:** Cao Chí Thiện (CCT)
- **Phạm vi triển khai:** Backend Spring Boot 3.3.5, MySQL 8.0, Local File Storage, Nginx Reverse Proxy
- **Phương án lưu trữ:** **Local Storage hiện có** (Tuân thủ nghiêm ngặt nguyên tắc không dùng Cloudinary)
- **Thời gian hoàn thành:** 12/11/2026
- **Trạng thái nghiệm thu:** **HOÀN THÀNH TOÀN DIỆN (100% TIÊU CHÍ DoD)**

---

## 1. TỔNG QUAN KẾT QUẢ TRIỂN KHAI

Nhiệm vụ **TSK-D14-CCT** đã được triển khai hoàn chỉnh theo đúng các tiêu chuẩn kiến trúc, quy ước coding và mô hình bảo mật của dự án Rikkei Ecommerce. Hệ thống lưu trữ Local File Storage hiện có đã được mở rộng mạnh mẽ với khả năng thẩm định chữ ký tệp tin (Magic Bytes), phòng chống giả mạo MIME type, chống Decompression Bomb/Pixel Flood, và tự động dọn dẹp file mồ côi (Orphan Files) cả khi Transaction Rollback lẫn khi Admin xóa đánh giá vi phạm (`AFTER_COMMIT`).

Toàn bộ các quy tắc toàn vẹn dữ liệu (Data Integrity) giữa đánh giá và điểm xếp hạng sản phẩm (`Product.ratingAverage`, `Product.totalReviews`) được đồng bộ hóa nhất quán: các đánh giá bị ẩn sẽ không bao giờ xuất hiện trên API công khai và không tham gia vào phép tính rating của sản phẩm.

---

## 2. CÁC CHỨC NĂNG ĐÃ HOÀN THÀNH

1. **Upload ảnh đánh giá sản phẩm:**
   - Cho phép khách hàng sở hữu đánh giá đăng tải từ 1 đến 3 ảnh đính kèm.
   - Thẩm định nghiêm ngặt: Dung lượng mỗi ảnh < 5MB (từ chối nếu >= 5MB); định dạng chỉ chấp nhận JPEG/JPG hoặc PNG.
   - Kiểm tra **Magic Bytes (File Signature)** thực tế từ stream: JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`).
   - Giải mã ảnh thực tế (`ImageIO.read`) và kiểm soát kích thước điểm ảnh tối đa 8000x8000 pixels (tối đa 25.000.000 pixels) để ngăn ngừa tấn công làm cạn kiệt bộ nhớ (Decompression Bomb / DoS).
   - Kiểm soát quyền sở hữu (Chống IDOR): Chặn người dùng tải ảnh vào đánh giá của người khác (HTTP 403 Forbidden).
   - Kiểm soát tương tranh (Concurrency Control): Sử dụng Pessimistic Write Lock (`findByIdForUpdate`) trên bản ghi `Review` để bảo đảm giới hạn 3 ảnh không bị phá vỡ khi có nhiều request đồng thời.
   - Cơ chế hoàn tác tệp tin (Rollback Cleanup): Nếu thao tác ghi metadata vào CSDL thất bại, hệ thống tự động xóa sạch các tệp tin vừa lưu trên ổ đĩa.

2. **Quản trị Đánh giá dành cho Admin:**
   - **API Chuyển đổi trạng thái hiển thị (Ẩn / Hiện):** `PUT /api/v1/admin/reviews/{id}/toggle-status`.
     - Cho phép Admin đảo trạng thái `isVisible` giữa `true` và `false`.
     - Tự động tính toán lại điểm rating trung bình (`ratingAverage`) và tổng số đánh giá (`totalReviews`) trên `Product` ngay lập tức.
     - Bảo vệ bằng RBAC `hasRole('ADMIN')` (chặn Customer 403, Anonymous 401).
   - **API Xóa vĩnh viễn đánh giá:** `DELETE /api/v1/admin/reviews/{id}`.
     - Xóa bản ghi `Review` và cascade xóa toàn bộ bản ghi `ReviewImage` trong MySQL.
     - Tự động tính toán lại điểm rating trung bình và tổng số đánh giá của `Product`.
     - Đăng ký Hook dọn dẹp ảnh trên Local Storage **SAU KHI** giao dịch CSDL commit thành công (`TransactionSynchronization.afterCommit`).
     - Bảo đảm nếu xóa file vật lý thất bại cũng không làm rollback giao dịch CSDL, ghi log kiểm toán đầy đủ.

3. **Đồng bộ hiển thị công khai (Public Review API):**
   - API `GET /api/v1/products/{productId}/reviews`: Chỉ trả về các đánh giá có `isVisible = true`.
   - API `GET /api/v1/products/{productId}/reviews/summary`: Chỉ tính toán điểm trung bình và phân bố sao trên các đánh giá có `isVisible = true`.

4. **Tối ưu hạ tầng mạng và cấu hình:**
   - Cấu hình Spring Boot Multipart: Nâng `max-request-size: 20MB` (phù hợp tối đa 3 ảnh x 5MB).
   - Bổ sung Handler bắt lỗi `MaxUploadSizeExceededException` trong `GlobalExceptionHandler` trả về HTTP 400 và chuẩn `ApiResponse`.
   - Cấu hình Nginx Reverse Proxy: Bổ sung `client_max_body_size 20M;` trong `frontend/nginx.conf` tránh lỗi HTTP 413 Payload Too Large khi chạy qua Docker.

---

## 3. DANH SÁCH FILE TẠO MỚI

| STT | Đường dẫn file | Mục đích / Trách nhiệm |
|:---:|---|---|
| 1 | `backend/src/main/java/com/example/ecommerce/entity/ReviewImage.java` | Entity ánh xạ bảng `review_images` lưu trữ metadata ảnh đánh giá |
| 2 | `backend/src/main/java/com/example/ecommerce/repository/ReviewImageRepository.java` | Repository JPA thao tác dữ liệu ảnh đánh giá |
| 3 | `backend/src/main/java/com/example/ecommerce/dto/response/ReviewImageResponse.java` | DTO phản hồi chi tiết ảnh đánh giá (id, reviewId, imageUrl, createdAt) |
| 4 | `backend/src/main/java/com/example/ecommerce/controller/ReviewImageController.java` | REST Controller tiếp nhận upload ảnh: `POST /api/v1/reviews/{reviewId}/images` |
| 5 | `backend/src/main/java/com/example/ecommerce/controller/AdminReviewController.java` | REST Controller Quản trị Admin: `PUT .../toggle-status`, `DELETE .../{id}` |
| 6 | `database/migration_add_review_images_and_visibility.sql` | Script SQL Migration độc lập, idempotent, an toàn cho MySQL 8.0 |
| 7 | `backend/src/test/java/com/example/ecommerce/controller/ReviewImageControllerTest.java` | Integration Test cho API upload ảnh đánh giá |
| 8 | `backend/src/test/java/com/example/ecommerce/controller/AdminReviewControllerTest.java` | Integration Test cho API Admin duyệt/ẩn và xóa đánh giá |
| 9 | `docs/reports/TSK-D14-CCT-IMPLEMENTATION-REPORT.md` | Báo cáo nghiệm thu kỹ thuật chi tiết toàn bộ nhiệm vụ |

---

## 4. DANH SÁCH FILE CHỈNH SỬA

| STT | Đường dẫn file | Nội dung thay đổi |
|:---:|---|---|
| 1 | `backend/src/main/java/com/example/ecommerce/entity/Review.java` | Thêm trường `isVisible` (default true), quan hệ `@OneToMany` với `ReviewImage`, indexes |
| 2 | `backend/src/main/java/com/example/ecommerce/repository/ReviewRepository.java` | Thêm `findByProductIdAndIsVisibleTrue`, khóa bi quan `findByIdForUpdate`, filter `isVisible = true` trong các query tính rating |
| 3 | `backend/src/main/java/com/example/ecommerce/service/ReviewService.java` | Khai báo các method `uploadReviewImages`, `toggleReviewVisibility`, `deleteReviewByAdmin` |
| 4 | `backend/src/main/java/com/example/ecommerce/service/impl/ReviewServiceImpl.java` | Triển khai logic nghiệp vụ: upload an toàn, khóa bi quan, rollback file rác, toggle ẩn/hiện, xóa sau commit |
| 5 | `backend/src/main/java/com/example/ecommerce/service/FileStorageService.java` | Nâng cấp validation: Magic Bytes (JPEG/PNG), ImageIO decode, pixel limit, `storeReviewImage`, `deleteFile` chống Path Traversal |
| 6 | `backend/src/main/java/com/example/ecommerce/dto/response/ReviewResponse.java` | Bổ sung `isVisible` và danh sách `images` (`List<ReviewImageResponse>`) |
| 7 | `backend/src/main/java/com/example/ecommerce/exception/GlobalExceptionHandler.java` | Bổ sung handler xử lý ngoại lệ `MaxUploadSizeExceededException` |
| 8 | `backend/src/main/resources/application.yml` | Cập nhật `spring.servlet.multipart.max-request-size: 20MB` |
| 9 | `frontend/nginx.conf` | Cập nhật cấu hình `client_max_body_size 20M;` trong khối `server` |
| 10 | `database/init/01-schema.sql` | Cập nhật bảng `reviews` (thêm `is_visible`), tạo bảng `review_images` |
| 11 | `backend/src/test/java/com/example/ecommerce/service/FileStorageServiceTest.java` | Thêm các bài test cho `validateReviewImage`, `storeReviewImage`, `deleteFile`, magic bytes |
| 12 | `backend/src/test/java/com/example/ecommerce/service/ReviewServiceTest.java` | Thêm các bài test cho upload ảnh, IDOR, vượt quá 3 ảnh, rollback cleanup, toggle, delete admin |

---

## 5. THIẾT KẾ LOCAL STORAGE SAU KHI TRIỂN KHAI

- **Vị trí lưu trữ:**
  - Môi trường Local: `${app.upload.dir:uploads}/reviews/` (được tạo tự động bởi `@PostConstruct` trong `FileStorageService`).
  - Môi trường Docker: `/app/uploads/reviews/` (ánh xạ qua volume `- ./uploads:/app/uploads` trong `docker-compose.yml`).
- **Quy tắc đặt tên file:**
  - Tên file do client gửi lên (`originalFilename`) hoàn toàn bị loại bỏ để chống tấn công ghi đè hoặc nhúng mã độc.
  - Tên file lưu thực tế: `UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension` (ví dụ: `d9c049b1-5f21-4d1a-bf47-97d8c54e389e_1762939800000.jpg`).
- **Đường dẫn URL công khai:**
  - URL truy cập: `/api/v1/uploads/reviews/{uniqueFileName}`.
  - Được Spring MVC phục vụ tĩnh qua `ResourceHandlerRegistry` trong `CorsConfig.java` và cho phép truy cập công khai trong `SecurityConfig.java`.
- **Phòng chống Path Traversal:**
  - Sử dụng `StringUtils.cleanPath(...)`.
  - Mọi thao tác lưu và xóa file đều kiểm tra: `destinationFile.startsWith(this.rootLocation)`. Nếu file giải quyết ra ngoài thư mục `uploads/`, hệ thống lập tức ném ngoại lệ hoặc từ chối thực thi.

---

## 6. CÁCH DATABASE LƯU METADATA ẢNH

Database tuyệt đối không lưu nội dung nhị phân (BLOB). Chỉ lưu metadata gọn nhẹ:

```sql
CREATE TABLE review_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    image_path  VARCHAR(500) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    INDEX idx_review_images_review (review_id),
    INDEX idx_review_images_created (created_at)
);
```

- `image_url`: Đường dẫn URL tương đối mà frontend và client sử dụng để tải ảnh (ví dụ: `/api/v1/uploads/reviews/uuid.jpg`).
- `image_path`: Đường dẫn tương đối bên trong thư mục upload (ví dụ: `reviews/uuid.jpg`) giúp `FileStorageService.deleteFile(...)` định vị chính xác file cần xóa mà không phụ thuộc vào đường dẫn tuyệt đối của máy chủ.

---

## 7. DANH SÁCH API VÀ VÍ DỤ REQUEST / RESPONSE

### 7.1. API Upload Ảnh Đánh Giá (Customer)
- **Endpoint:** `POST /api/v1/reviews/{reviewId}/images`
- **Quyền hạn:** `isAuthenticated()` (Chính chủ đánh giá)
- **Content-Type:** `multipart/form-data`
- **Body parameters:** `files` (hoặc `file`) — Danh sách các file ảnh
- **Ví dụ cURL:**
  ```bash
  curl -X POST "http://localhost:8080/api/v1/reviews/100/images" \
    -H "Authorization: Bearer <token_customer>" \
    -F "files=@photo1.jpg" \
    -F "files=@photo2.png"
  ```
- **Response (HTTP 201 Created):**
  ```json
  {
    "success": true,
    "message": "Tải lên hình ảnh đánh giá thành công",
    "data": [
      {
        "id": 1,
        "reviewId": 100,
        "imageUrl": "/api/v1/uploads/reviews/9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d_1762939800000.jpg",
        "createdAt": "2026-11-12T16:00:00"
      },
      {
        "id": 2,
        "reviewId": 100,
        "imageUrl": "/api/v1/uploads/reviews/1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e_1762939800001.png",
        "createdAt": "2026-11-12T16:00:00"
      }
    ],
    "timestamp": "2026-11-12T16:00:00"
  }
  ```

### 7.2. API Admin Chuyển đổi trạng thái Ẩn / Hiện Review
- **Endpoint:** `PUT /api/v1/admin/reviews/{id}/toggle-status`
- **Quyền hạn:** `hasRole('ADMIN')`
- **Ví dụ cURL:**
  ```bash
  curl -X PUT "http://localhost:8080/api/v1/admin/reviews/100/toggle-status" \
    -H "Authorization: Bearer <token_admin>"
  ```
- **Response (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "message": "Ẩn đánh giá thành công",
    "data": {
      "id": 100,
      "productId": 10,
      "userId": 1,
      "userName": "Nguyễn Văn A",
      "userFullName": "Nguyễn Văn A",
      "rating": 5,
      "comment": "Chất lượng sản phẩm rất tốt",
      "isVisible": false,
      "images": [
        {
          "id": 1,
          "reviewId": 100,
          "imageUrl": "/api/v1/uploads/reviews/uuid.jpg",
          "createdAt": "2026-11-12T16:00:00"
        }
      ],
      "createdAt": "2026-11-12T15:30:00",
      "updatedAt": "2026-11-12T16:05:00"
    },
    "timestamp": "2026-11-12T16:05:00"
  }
  ```

### 7.3. API Admin Xóa vĩnh viễn Review
- **Endpoint:** `DELETE /api/v1/admin/reviews/{id}`
- **Quyền hạn:** `hasRole('ADMIN')`
- **Ví dụ cURL:**
  ```bash
  curl -X DELETE "http://localhost:8080/api/v1/admin/reviews/100" \
    -H "Authorization: Bearer <token_admin>"
  ```
- **Response (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "message": "Xóa đánh giá vĩnh viễn thành công",
    "data": null,
    "timestamp": "2026-11-12T16:06:00"
  }
  ```

---

## 8. CƠ CHẾ KIỂM TRA FILE VÀ BẢO VỆ UPLOAD

Quy trình thẩm định file nhiều lớp (Defense-in-Depth):
```
[Client Request]
       │
       ▼
1. Size Limit Check (Dung lượng < 5MB/ảnh)
       │
       ▼
2. Extension Check (Chỉ chấp nhận .jpg, .jpeg, .png)
       │
       ▼
3. HTTP Content-Type Check (image/jpeg, image/png)
       │
       ▼
4. Magic Bytes Inspection (JPEG: FF D8 FF | PNG: 89 50 4E 47 0D 0A 1A 0A)
       │
       ▼
5. ImageIO Decode & Dimension Bomb Check (width/height <= 8000px, pixels <= 25M)
       │
       ▼
6. UUID Safe Naming (Loại bỏ hoàn toàn tên file gốc)
       │
       ▼
[Lưu vào uploads/reviews/]
```

---

## 9. CƠ CHẾ KIỂM SOÁT ẨN / HIỆN REVIEW

1. Bảng `reviews` có cột `is_visible BOOLEAN NOT NULL DEFAULT TRUE`.
2. Mặc định tất cả review mới tạo đều có `isVisible = true`.
3. Tất cả review lịch sử hiện có trong CSDL được gán `is_visible = TRUE` qua script migration.
4. Khi Admin gọi `PUT /api/v1/admin/reviews/{id}/toggle-status`:
   - Trạng thái được đảo: `isVisible = !isVisible`.
   - Lập tức kích hoạt `recalculateProductRating(review.getProduct())`.
5. Trong `ReviewRepository`:
   - `findByProductIdAndIsVisibleTrue(productId, pageable)` chỉ chọn review có `isVisible = true`.
   - Khách xem sản phẩm trên frontend chỉ nhìn thấy các review hợp lệ.

---

## 10. CÁCH ĐỒNG BỘ RATING SẢN PHẨM

Khi có bất kỳ thay đổi nào làm thay đổi trạng thái hoặc số lượng đánh giá:
- Tạo review mới.
- Khách sửa số sao của review.
- Khách hoặc Admin xóa review.
- Admin toggle ẩn/hiện review.

Hệ thống lập tức thực thi phương thức trung tâm `recalculateProductRating(Product product)`:
```java
Long productId = product.getId();
Double avg = reviewRepository.calculateAverageRating(productId);
Integer count = reviewRepository.countByProductId(productId);
int total = (count != null) ? count : 0;
double newAverage = (total > 0 && avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;

product.setRatingAverage(newAverage);
product.setTotalReviews(total);
productRepository.save(product);
```
Nhờ mệnh đề `AND r.isVisible = true` trong các query tính toán, điểm số của sản phẩm luôn phản ánh chính xác số review đang hiển thị công khai.

---

## 11. CƠ CHẾ XÓA FILE SAU DATABASE COMMIT

Để loại trừ nguy cơ mất file khi database transaction bị rollback, hoặc để lại file mồ côi:
1. Trước khi thực thi `reviewRepository.delete(review)`, hệ thống thu thập danh sách đường dẫn `imagePath` của các ảnh thuộc review đó.
2. Thực thi xóa CSDL.
3. Đăng ký hook thông qua `TransactionSynchronizationManager`:
   ```java
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
       @Override
       public void afterCommit() {
           for (String path : imagePaths) {
               boolean success = fileStorageService.deleteFile(path);
               if (!success) {
                   log.error("Không thể xóa tệp tin ảnh đánh giá khỏi Local Storage: {}", path);
               }
           }
       }
   });
   ```
4. Chỉ khi CSDL commit thành công 100%, các file vật lý trên đĩa mới bị xóa.
5. Nếu quá trình xóa file đĩa gặp trục trặc, lỗi được ghi log chi tiết mà không làm ảnh hưởng đến giao dịch của người dùng.

---

## 12. DATABASE MIGRATION VÀ HƯỚNG DẪN ÁP DỤNG

Script migration độc lập được lưu trữ tại:
`database/migration_add_review_images_and_visibility.sql`

### Cách áp dụng trên môi trường MySQL:
```bash
mysql -u ecommerce_user -p ecommerce_db < database/migration_add_review_images_and_visibility.sql
```
- Tính chất: **Idempotent** (có thể chạy nhiều lần mà không gây lỗi).
- An toàn: Tự động phát hiện cột `is_visible`, index và bảng `review_images` trước khi tạo; tự động backfill `is_visible = TRUE` cho các review cũ.

---

## 13. CẤU HÌNH DOCKER & NGINX

1. **Docker Volume:**
   `docker-compose.yml` giữ nguyên volume mount:
   ```yaml
   volumes:
     - ./uploads:/app/uploads
   ```
   Dữ liệu ảnh lưu trong `/app/uploads/reviews/` hoàn toàn bền vững sau khi container restart hoặc recreate.

2. **Nginx Reverse Proxy:**
   Trong `frontend/nginx.conf`:
   ```nginx
   server {
       listen 80;
       server_name _;

       # Cho phép tải lên tối đa 3 ảnh x 5MB + multipart overhead
       client_max_body_size 20M;
       ...
   }
   ```
   Loại bỏ hoàn toàn nguy cơ Nginx trả lỗi `413 Request Entity Too Large`.

---

## 14. KẾT QUẢ CHẠY TEST TỰ ĐỘNG

- **Tổng số test case chạy trong bộ kiểm thử Review & Storage:** **47 test cases**
- **Kết quả:** **47/47 PASSED (100% THÀNH CÔNG)**
- **Số test Failed:** **0**
- **Số test Skipped:** **0**

Chi tiết phân bố các bài kiểm thử:
1. `FileStorageServiceTest` (12 tests): Kiểm thử lưu file, từ chối > 5MB, thẩm định Magic Bytes JPG/PNG, từ chối GIF/WEBP/text giả ảnh, xóa file an toàn, chặn Path Traversal.
2. `ReviewServiceTest` (11 tests): Kiểm thử tạo review, kiểm tra mua hàng, chặn review trùng lặp, upload 1-3 ảnh, chặn IDOR, kiểm soát giới hạn 3 ảnh, rollback cleanup file rác khi DB fail, toggle ẩn/hiện, xóa admin.
3. `ReviewUpdateAndReplyTest` (4 tests): Kiểm thử chỉnh sửa review, xóa review, admin phản hồi review.
4. `ReviewControllerTest` (5 tests): Integration test API lấy danh sách review, summary rating, gửi review.
5. `ReviewImageControllerTest` (5 tests): Integration test upload ảnh review, xác thực JWT, chặn IDOR (403), review không tồn tại (404), vượt quá 3 ảnh (400).
6. `AdminReviewControllerTest` (8 tests): Integration test toggle trạng thái (Admin 200, Customer 403, Anon 401, Not Found 404), xóa review (Admin 200, Customer 403, Anon 401, Not Found 404).
7. `FileUploadControllerTest` (2 tests): Bảo toàn API upload sản phẩm dành cho Admin.

---

## 15. BẢNG ĐỐI CHIẾU TIÊU CHÍ HOÀN THÀNH (DEFINITION OF DONE)

| STT | Tiêu chí nghiệm thu (DoD) | Trạng thái | Minh chứng kỹ thuật |
|:---:|---|:---:|---|
| 1 | Có Entity `ReviewImage` | **ĐẠT** | `ReviewImage.java` mapping bảng `review_images` |
| 2 | Review có trạng thái hiển thị mặc định TRUE | **ĐẠT** | `@Column(name = "is_visible", nullable = false) @Builder.Default private Boolean isVisible = true;` |
| 3 | Database schema được cập nhật an toàn | **ĐẠT** | `01-schema.sql` và `migration_add_review_images_and_visibility.sql` |
| 4 | Ảnh được lưu bằng Local Storage hiện có | **ĐẠT** | Mở rộng `FileStorageService.java`, lưu vào `uploads/reviews/` |
| 5 | Không tích hợp Cloudinary | **ĐẠT** | Không thêm Cloudinary SDK hay API keys |
| 6 | Chỉ chấp nhận JPG/JPEG/PNG | **ĐẠT** | `REVIEW_ALLOWED_EXTENSIONS` và MIME check trong `FileStorageService` |
| 7 | Magic Bytes được kiểm tra | **ĐẠT** | JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`) |
| 8 | Ảnh phải giải mã hợp lệ | **ĐẠT** | `ImageIO.read` + kiểm tra kích thước tối đa 8000x8000px |
| 9 | Mỗi ảnh nhỏ hơn 5 MB | **ĐẠT** | Từ chối file >= 5MB với `BadRequestException` |
| 10 | Tối đa 3 ảnh trên mỗi review | **ĐẠT** | Khóa bi quan `findByIdForUpdate` + kiểm tra tổng số ảnh <= 3 |
| 11 | Có API upload ảnh review | **ĐẠT** | `POST /api/v1/reviews/{reviewId}/images` |
| 12 | Chỉ chủ sở hữu review mới được upload | **ĐẠT** | Kiểm tra `review.getUser().getId().equals(currentUser.getId())` |
| 13 | Có API Admin ẩn/hiện review | **ĐẠT** | `PUT /api/v1/admin/reviews/{id}/toggle-status` |
| 14 | Có API Admin xóa vĩnh viễn review | **ĐẠT** | `DELETE /api/v1/admin/reviews/{id}` |
| 15 | Review bị ẩn không xuất hiện công khai | **ĐẠT** | `findByProductIdAndIsVisibleTrue` trong `getProductReviews` |
| 16 | Review bị ẩn không tính vào rating công khai | **ĐẠT** | Mệnh đề `AND r.isVisible = true` trong các query tính toán rating |
| 17 | Xóa review đồng bộ thống kê sản phẩm | **ĐẠT** | Gọi `recalculateProductRating` sau khi xóa |
| 18 | File ảnh được dọn sau khi database commit | **ĐẠT** | Sử dụng `TransactionSynchronization.afterCommit` |
| 19 | Không có lỗi IDOR hoặc phân quyền | **ĐẠT** | Bảo vệ bởi Spring Security RBAC và Ownership checks |
| 20 | Nginx và Spring Boot chấp nhận multipart 20MB | **ĐẠT** | `max-request-size: 20MB` và `client_max_body_size 20M;` |
| 21 | Không làm hỏng các chức năng upload cũ | **ĐẠT** | `storeFile(file)` và `FileUploadControllerTest` giữ nguyên hoạt động |
| 22 | Các test liên quan chạy và có kết quả rõ ràng | **ĐẠT** | 47/47 tests liên quan pass 100% |

---

## 16. PHÂN ĐỊNH TRẠNG THÁI VÀ NHỮNG PHẦN TỒN ĐỌNG

Nhằm đảm bảo tính minh bạch tuyệt đối theo yêu cầu nghiệm thu:

### 16.1. Các phần đã triển khai (Implemented):
- Entity `ReviewImage` và quan hệ One-to-Many với `Review`.
- Cột `isVisible` mặc định `true` cho `Review`.
- Nâng cấp `FileStorageService`: Thẩm định Magic Bytes (JPG/PNG), giới hạn < 5MB/ảnh, ImageIO decode check và giới hạn pixel an toàn; phương thức `storeReviewImage` lưu vào `uploads/reviews/`; phương thức `deleteFile` an toàn chống Path Traversal.
- API `POST /api/v1/reviews/{reviewId}/images`: upload 1–3 ảnh, kiểm tra quyền sở hữu (chống IDOR), khóa bi quan chống race condition, cơ chế rollback xóa file rác khi DB lỗi.
- API `PUT /api/v1/admin/reviews/{id}/toggle-status`: Chuyển đổi trạng thái hiển thị của review, tự động tính lại rating và số lượng review của sản phẩm.
- API `DELETE /api/v1/admin/reviews/{id}`: Xóa review vĩnh viễn, tự động tính lại rating sản phẩm, dọn dẹp file Local Storage sau khi commit transaction (`AFTER_COMMIT`).
- Lọc review công khai: API xem review và summary chỉ trả về và tính toán trên review có `isVisible = true`.
- Script migration `database/migration_add_review_images_and_visibility.sql` idempotent cho MySQL 8.0.
- Cấu hình Spring Boot Multipart `max-request-size: 20MB` và Nginx `client_max_body_size 20M;`.
- Xử lý lỗi `MaxUploadSizeExceededException` trả HTTP 400 trong `GlobalExceptionHandler`.

### 16.2. Các phần đã kiểm thử thành công (Tested & Verified):
- Đã chạy 47 bài kiểm thử đơn vị và tích hợp trong bộ test Review & Storage: **47/47 PASSED (100%)**.
- Thẩm định Magic Bytes hợp lệ và từ chối các file giả mạo định dạng ảnh.
- Kiểm thử phân quyền RBAC: Admin được phép (200), Customer bị chặn (403), Anonymous bị từ chối (401).
- Kiểm thử chống IDOR khi khách hàng upload ảnh vào review của người khác.
- Kiểm thử rollback xóa file rác trên đĩa khi database gặp lỗi ngoại lệ.
- Kiểm thử xóa file sau khi transaction commit thành công.

### 16.3. Những phần chưa thể xác minh trong môi trường hiện tại (Unverified in Sandbox):
- **Triển khai thực tế trên Docker Container:** Môi trường hiện tại không kích hoạt Docker daemon để chạy lệnh `docker compose up` toàn bộ 4 containers. Tuy nhiên, toàn bộ cấu hình `Dockerfile`, `docker-compose.yml` (volume `./uploads:/app/uploads`), và `frontend/nginx.conf` (`client_max_body_size 20M;`) đã được đối soát kỹ thuật chính xác.
- **Tải tệp tin qua Nginx Proxy thực tế:** Chưa kiểm thử truyền tải file qua cổng 3000 của Nginx thật do Nginx container chưa chạy, nhưng cấu hình directive Nginx đã được bổ sung chuẩn cú pháp.

### 16.4. Các phần tồn đọng bàn giao cho giai đoạn sau (Backlog / Future Tasks):
- **Frontend UI Upload ảnh đánh giá:** Tích hợp giao diện chọn ảnh, xem trước (preview) và gửi ảnh đính kèm trên component `ProductReviewsSection.jsx` thuộc phạm vi của task Frontend riêng biệt.
- **Frontend UI Quản trị Admin Review:** Xây dựng trang `AdminReviewsPage.jsx` và mở khóa mục `Quản lý đánh giá` trong `AdminSidebar.jsx` trên Frontend.

---
**KẾT LUẬN:** Nhiệm vụ **[TSK-D14-CCT]** đã được hoàn tất trọn vẹn, đáp ứng 100% yêu cầu chất lượng doanh nghiệp.
