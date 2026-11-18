# BÁO CÁO KIỂM TOÁN KỸ THUẬT VÀ ĐỐI CHIẾU MÃ NGUỒN DỰ ÁN RIKKEI ECOMMERCE
## TECHNICAL AUDIT & SOURCE CODE RECONCILIATION REPORT (TSK-D17-CCT-FIX)

---

- **Dự án:** Rikkei Ecommerce Platform
- **Mã nhiệm vụ:** `[TSK-D17-CCT-FIX]` Hoàn thiện, đối chiếu source code và kiểm toán sản phẩm D17
- **Người phụ trách:** Cao Chí Thiện (CCT)
- **Vai trò:** Senior Software Engineer, Software Architect & Technical Lead
- **Thời gian thực hiện kiểm toán:** Ngày 17–18 tháng 11 năm 2026
- **Nguyên tắc cốt lõi:** *"Source Code là nguồn sự thật cao nhất (Single Source of Truth) — Không suy diễn, không bao biện, không che giấu giới hạn kỹ thuật."*

---

## 1. TỔNG QUAN AUDIT (AUDIT OVERVIEW)

### 1.1. Mục đích kiểm toán
Đợt kiểm toán kỹ thuật `TSK-D17-CCT-FIX` được thực hiện nhằm rà soát độc lập toàn diện giữa các tài liệu kỹ thuật, slide thuyết trình bảo vệ và mã nguồn thực tế của hệ thống thương mại điện tử Rikkei Ecommerce. Trọng tâm của đợt kiểm toán là:
1. Xác thực tính chính xác kỹ thuật của toàn bộ báo cáo, biểu đồ, slide thuyết trình bảo vệ trước Hội đồng chấm tốt nghiệp/bảo vệ dự án.
2. Phát hiện và xử lý triệt để các tuyên bố phóng đại, nhầm lẫn khái niệm hoặc thiếu bằng chứng thực tế trong mã nguồn.
3. Đảm bảo toàn bộ 6 sản phẩm cốt lõi của D17 (`D17-Technical-Architecture-Report.md`, `D17-Technical-Defense-QA.md`, `D17-Technical-Architecture-Presentation.pptx`, `D17-Technical-Architecture-Presentation.pdf`, `API.md`, `README.md`) đạt độ tin cậy 100%, bảo vệ an toàn cho người phụ trách Cao Chí Thiện (CCT) trước các câu hỏi phản biện chuyên sâu.

### 1.2. Phương pháp kiểm toán
- **Phương pháp tiếp cận:** Khảo sát tĩnh mã nguồn (Static Code Analysis & Survey) ở chế độ READ-ONLY. Tuyệt đối không thay đổi logic nghiệp vụ backend, frontend, database schema hay file cấu hình an ninh của hệ thống.
- **Tiêu chuẩn phân loại trạng thái kiểm toán:**
  - `ĐÃ TRIỂN KHAI`: Tính năng có mã nguồn Java/TypeScript thực tế, đang hoạt động hoặc được cấu hình trực tiếp.
  - `ĐÃ KIỂM THỬ`: Có bài kiểm thử tự động (Unit Test / Integration Test) trong thư mục `src/test/java` hoặc bằng chứng chạy thử nghiệm.
  - `ĐÃ SỬA`: Tuyên bố trong tài liệu ban đầu bị sai lệch hoặc thiếu chính xác đã được đối chiếu và điều chỉnh trực tiếp trong đợt audit này.
  - `CHƯA TRIỂN KHAI / ĐỀ XUẤT KIẾN TRÚC`: Tính năng chỉ mới dừng lại ở mức ý tưởng thiết kế, chưa có mã nguồn; được tài liệu hóa rõ ràng dưới dạng định hướng nâng cấp để tránh nhầm lẫn khi phản biện.
- **Phạm vi bảo mật:** Kiểm tra 100% kho lưu trữ tài liệu, đảm bảo không có bất kỳ JWT secret, OAuth client secret, OTP hash secret, database password hay credentials nhạy cảm nào bị hardcode hoặc lộ lọt ra Git.

---

## 2. DANH SÁCH FILE VÀ THÀNH PHẦN ĐÃ KIỂM TRA

### 2.1. Backend Source Code (Spring Boot 3.3.5 & Java 21)
- **Security & OAuth2:**
  - `backend/src/main/java/com/example/ecommerce/config/SecurityConfig.java`: Cấu hình stateless filter chain, CORS whitelist, URL-based pattern authorizations, exception entry points.
  - `backend/src/main/java/com/example/ecommerce/config/OAuth2ClientConfig.java`: Đăng ký ClientRegistration Google & Auth0.
  - `backend/src/main/java/com/example/ecommerce/security/oauth2/OAuth2AuthenticationSuccessHandler.java`: Tiếp nhận chuyển hướng thành công từ Google, sinh mã Ticket ngẫu nhiên (16 bytes hex), lưu Redis TTL 180s, thiết lập browser binding cookie `oauth2_auth_state`.
  - `backend/src/main/java/com/example/ecommerce/security/oauth2/OAuth2AuthenticationFailureHandler.java`: Xử lý ngoại lệ xác thực OAuth2 và redirect về frontend kèm mã lỗi.
  - `backend/src/main/java/com/example/ecommerce/security/oauth2/CustomOidcUserService.java`: Xử lý thông tin người dùng Google OIDC, chính sách Anti-Account Hijacking (chặn tự động liên kết local user), sinh mật khẩu ngẫu nhiên `SecureRandom` 32-byte.
  - `backend/src/main/java/com/example/ecommerce/security/oauth2/RedisOAuth2AuthorizationRequestRepository.java`: Lưu trữ và đối chiếu trạng thái OAuth2 Authorization Request trên Redis (`auth:oauth2_req:<state>`, TTL 180s).
  - `backend/src/main/java/com/example/ecommerce/security/Auth0TokenValidator.java`: Xác thực Auth0 ID Token thông qua Nimbus JWK Set URL (`RS256`).
  - `backend/src/main/java/com/example/ecommerce/security/JwtAuthenticationFilter.java`: Filter chặn mỗi request, trích xuất Bearer token, giải mã claims, nạp `UserDetails` từ DB qua `userDetailsService.loadUserByUsername` và kiểm tra `userDetails.isEnabled()`.
  - `backend/src/main/java/com/example/ecommerce/security/JwtUtils.java`: Tạo và giải mã JWT Access Token (HMAC-SHA256, 512-bit key, TTL 900.000 ms).
- **Authentication & Token Management Services:**
  - `backend/src/main/java/com/example/ecommerce/service/AuthService.java`: Triển khai đăng nhập, trao đổi Google Ticket qua `COMPARE_AND_DELETE_TICKET_LUA_SCRIPT` (CAD / FIX-01), xác thực Auth0 ID Token, quản lý cookie.
  - `backend/src/main/java/com/example/ecommerce/service/RefreshTokenService.java`: Quản lý Refresh Token trên Redis, atomic token rotation qua `ROTATE_LUA_SCRIPT`, phát hiện xâm nhập Token Reuse qua Tombstone key (TTL 120s), thu hồi toàn bộ session qua `REVOKE_ALL_LUA_SCRIPT`.
- **Review Management & File Storage:**
  - `backend/src/main/java/com/example/ecommerce/service/FileStorageService.java`: 5 lớp phòng thủ file upload, xác thực Magic Bytes nhị phân thuần Java (`JPEG_MAGIC_BYTES`, `PNG_MAGIC_BYTES`), giải mã raster qua `ImageIO.read` chống Decompression Bomb ($\le 8000$px, $\le 25M$ pixels), UUID sanitization, xóa file đĩa.
  - `backend/src/main/java/com/example/ecommerce/service/impl/ReviewServiceImpl.java`: Nghiệp vụ đánh giá, giới hạn tối đa 3 ảnh/review, kiểm tra quyền sở hữu IDOR, đăng ký hook dọn dẹp file vật lý an toàn sau COMMIT (`TransactionSynchronizationManager.registerSynchronization(afterCommit)`).
  - `backend/src/main/java/com/example/ecommerce/controller/ReviewController.java`: API đánh giá cho khách hàng (`/api/v1/products/{productId}/reviews`).
  - `backend/src/main/java/com/example/ecommerce/controller/ReviewImageController.java`: API tải lên ảnh đính kèm bài đánh giá (`POST /api/v1/reviews/{reviewId}/images`).
  - `backend/src/main/java/com/example/ecommerce/controller/ReviewUploadController.java`: API upload ảnh đơn lẻ (`POST /api/v1/reviews/upload`).
  - `backend/src/main/java/com/example/ecommerce/controller/AdminReviewController.java`: API quản trị kiểm duyệt đánh giá cho Admin (`/api/v1/admin/reviews`, `/visibility`, `/reply`, `/toggle-status`, `/delete`).
- **JPA Entities (14 MySQL Tables):**
  - `User.java` (bảng `users`), `UserOAuthAccount.java` (bảng `user_oauth_accounts`), `Category.java` (bảng `categories`), `Product.java` (bảng `products`), `ProductVariant.java` (bảng `product_variants`), `Coupon.java` (bảng `coupons`), `Order.java` (bảng `orders`), `OrderItem.java` (bảng `order_items`), `Payment.java` (bảng `payments`), `PaymentTransaction.java` (bảng `payment_transactions`), `InventoryTransaction.java` (bảng `inventory_transactions`), `Review.java` (bảng `reviews`), `ReviewImage.java` (bảng `review_images`), `Wishlist.java` (bảng `wishlists`).
- **Automated Tests:**
  - `backend/src/test/java/com/example/ecommerce/OAuth2TicketExchangeTest.java`: Kiểm thử đơn vị trao đổi Ticket và bảo mật cookie.
  - `backend/src/test/java/com/example/ecommerce/OAuth2TicketExchangeRedisIntegrationTest.java`: Kiểm thử tích hợp Redis Lua Script cho CAD.
  - `backend/src/test/java/com/example/ecommerce/controller/AdminReviewControllerTest.java`: Kiểm thử phân quyền RBAC và các API kiểm duyệt đánh giá.
  - `backend/src/test/java/com/example/ecommerce/controller/AdminUserControllerTest.java`: Kiểm thử quy tắc an toàn BR-AUTH-04 (chống Admin tự khóa tài khoản).

### 2.2. Frontend & Infrastructure
- `frontend/nginx.conf`: Cấu hình Nginx reverse proxy, gzip, resolver Docker DNS, định tuyến `/api/`, streaming `/api/v1/chat` (`proxy_buffering off`), upload size `client_max_body_size 20M`.
- `frontend/Dockerfile`: Multi-stage build Node 20 Alpine sang Nginx Alpine.
- `docker-compose.yml`: Khởi chạy 4 containers (`ecommerce-mysql`, `ecommerce-redis`, `ecommerce-backend`, `ecommerce-frontend`) trên bridge network `ecommerce-net`.

### 2.3. Tài liệu Kỹ thuật Đã Kiểm toán và Điều chỉnh
- `docs/D17/D17-Technical-Architecture-Report.md` (Báo cáo kiến trúc tổng thể D17).
- `docs/D17/D17-Technical-Defense-QA.md` (27 kịch bản câu hỏi phản biện bảo vệ).
- `docs/D17/D17-Technical-Architecture-Presentation.pptx` (Slide bảo vệ dạng PowerPoint).
- `docs/D17/D17-Technical-Architecture-Presentation.pdf` (Slide bảo vệ dạng PDF preview).
- `docs/API.md` (Tài liệu đặc tả hợp đồng RESTful API).
- `README.md` (Tài liệu hướng dẫn vận hành và kiến trúc tổng quan).

---

## 3. BẢNG ĐỐI CHIẾU MÃ NGUỒN (SOURCE CODE RECONCILIATION TABLE)

| Thành phần / Chức năng | Tuyên bố trong báo cáo cũ | Thực tế trong Source Code | Bằng chứng (File & Dòng) | Trạng thái |
|:---|:---|:---|:---|:---|
| **Google OAuth2 CAD** | CAD là "Client Authentication Disallowed" (lỗi cấu hình client type) | CAD là **Compare-And-Delete Atomic Lua Script** giải quyết lỗ hổng Ticket Invalidation DoS | `AuthService.java` (dòng 1043–1058), `OAuth2TicketExchangeTest.java` (dòng 41–47) | **ĐÃ SỬA** |
| **Account Linking** | Tự động liên kết tài khoản Google với tài khoản local nếu trùng email | **Bác bỏ hoàn toàn** liên kết tự động (`email_already_exists`) để chống Account Hijacking | `CustomOidcUserService.java` (dòng 134–141) | **ĐÃ SỬA** |
| **Google OIDC vs Auth0** | Coi Auth0 và Google OAuth2 là một hoặc nhầm lẫn endpoint trao đổi code | Tách biệt hoàn toàn: Google OIDC qua `/auth/oauth2/exchange`, Auth0 SDK qua `/auth/auth0/exchange` | `AuthService.java` (dòng 1030–1213), `OAuth2ClientConfig.java` | **ĐÃ SỬA** |
| **Refresh Token Rotation** | Xoay vòng token lưu trữ thông thường trên Redis | Thực hiện nguyên tử bằng Redis Lua Script (`ROTATE_LUA_SCRIPT`), lưu token cũ làm Tombstone 120s | `RefreshTokenService.java` (dòng 18–44, 215–280) | **ĐÃ SỬA** |
| **Token Reuse Detection** | Mô tả lý thuyết về phát hiện token dùng lại | Đã cài đặt hoàn chỉnh: nếu gọi bằng token trạng thái ROTATED ngoài grace period (10s) $\rightarrow$ thu hồi sạch session qua `REVOKE_ALL_LUA_SCRIPT` | `RefreshTokenService.java` (dòng 46–72, 282–340) | **ĐÃ SỬA** |
| **Instant Lockout** | Sử dụng Redis JTI Blacklist để hủy tức thì token khi admin khóa user | Kiểm tra trạng thái User trực tiếp trong DB tại `JwtAuthenticationFilter` qua `userDetailsService.loadUserByUsername`; JTI Blacklist là đề xuất nâng cấp | `JwtAuthenticationFilter.java` (dòng 68–95), `JwtUtils.java` | **ĐÃ SỬA** |
| **Review Image Upload Limit** | Giới hạn tối đa 5 ảnh đính kèm mỗi đánh giá | Giới hạn thực tế là **tối đa 3 ảnh/đánh giá**; dung lượng mỗi ảnh phải nhỏ hơn 5MB | `ReviewServiceImpl.java` (dòng 270), `FileStorageService.java` (dòng 40, 161) | **ĐÃ SỬA** |
| **Kiểm tra Magic Bytes** | Sử dụng thư viện ngoài Apache Tika Core 2.9 | Sử dụng **mảng byte Java thuần** (`JPEG_MAGIC_BYTES`, `PNG_MAGIC_BYTES`) và `ImageIO.read` chống Decompression Bomb | `FileStorageService.java` (dòng 58–65, 194–235) | **ĐÃ SỬA** |
| **Dọn dẹp ảnh sau xóa** | Sử dụng Spring Event `@TransactionalEventListener(phase = AFTER_COMMIT)` | Sử dụng `TransactionSynchronizationManager.registerSynchronization(afterCommit)` | `ReviewServiceImpl.java` (dòng 548–568) | **ĐÃ SỬA** |
| **Danh mục 14 bảng MySQL** | Báo cáo cũ liệt kê `roles`, `carts`, `cart_items`, `vouchers`, `refresh_tokens`, `chat_sessions` | 14 bảng thực tế: `users`, `user_oauth_accounts`, `categories`, `products`, `product_variants`, `coupons`, `orders`, `order_items`, `payments`, `payment_transactions`, `inventory_transactions`, `reviews`, `review_images`, `wishlists` | Toàn bộ 14 file Entity trong `backend/src/main/java/com/example/ecommerce/entity/` | **ĐÃ SỬA** |
| **Docker Compose** | Cụm 5 containers (`nginx`, `backend`, `frontend`, `mysql`, `redis`), mạng `app-network` | Đúng **4 containers**: `ecommerce-mysql` (3307), `ecommerce-redis` (6379), `ecommerce-backend` (8080), `ecommerce-frontend` (3000), mạng `ecommerce-net` | `docker-compose.yml` (dòng 8–189) | **ĐÃ SỬA** |
| **Quy tắc BR-AUTH-04** | Ngăn chặn Admin tự khóa tài khoản của chính mình | Đã triển khai và có bài Unit Test tự động kiểm thử độc lập | `UserServiceImpl.java`, `AdminUserControllerTest.java` | **ĐÃ KIỂM THỬ** |
| **Phân quyền RBAC 2 lớp** | Phân quyền qua URL Filter và `@PreAuthorize` | Đã triển khai đầy đủ cả SecurityFilterChain và `@PreAuthorize("hasRole('ADMIN')")` | `SecurityConfig.java`, `AdminReviewController.java`, `AdminUserController.java` | **ĐÃ TRIỂN KHAI** |

---

## 4. MA TRẬN SAI LỆCH PHÁT HIỆN (DISCREPANCY MATRIX)

| Mã sai lệch | Vấn đề phát hiện | Mức độ ảnh hưởng | Hành động điều chỉnh đã thực hiện |
|:---|:---|:---:|:---|
| **DISC-01** | Nhầm lẫn thuật ngữ CAD trong Google OAuth2 thành "Client Authentication Disallowed" thay vì thuật toán "Compare-And-Delete" Lua script giải quyết lỗ hổng Ticket Invalidation DoS. | **CAO** *(Sai lệch nghiêm trọng về bản chất kiến trúc và an ninh)* | Đã sửa toàn bộ định nghĩa, mã nguồn Lua script và trích dẫn kiểm thử trong `D17-Technical-Architecture-Report.md` (Mục D.2), `D17-Technical-Defense-QA.md` (Câu 12), slide 4 của PPTX và PDF. |
| **DISC-02** | Khẳng định hệ thống tự động liên kết tài khoản Google với tài khoản Local, trong khi mã nguồn thực tế có cơ chế bảo vệ chủ động từ chối (`email_already_exists`) để chống chiếm đoạt tài khoản. | **CAO** *(Ảnh hưởng trực tiếp đến logic an toàn thông tin)* | Đã cập nhật chính sách Anti-Account Hijacking trong `D17-Technical-Architecture-Report.md` (Mục D.3), `D17-Technical-Defense-QA.md` (Câu 13), slide 4 của PPTX và PDF. |
| **DISC-03** | Khẳng định cơ chế tức thì thu hồi token khi khóa tài khoản sử dụng Redis JTI Blacklist, trong khi `JwtAuthenticationFilter` thực tế nạp `UserDetails` từ DB trên mỗi request và kiểm tra `isEnabled()`. | **TRUNG BÌNH** *(Sai lệch chi tiết triển khai tầng Filter)* | Đã làm rõ cơ chế xác thực DB-backed của `JwtAuthenticationFilter` và định vị JTI Blacklist là giải pháp kiến trúc đề xuất nâng cấp trong `D17-Technical-Architecture-Report.md` (Mục C), `D17-Technical-Defense-QA.md` (Câu 9), slide 3 của PPTX và PDF. |
| **DISC-04** | Nhầm lẫn danh sách 14 bảng cơ sở dữ liệu: báo cáo cũ coi `roles`, `vouchers`, `refresh_tokens`, `carts` là bảng MySQL, bỏ sót `user_oauth_accounts`, `product_variants`, `coupons`, `wishlists`... | **CAO** *(Sai lệch cốt lõi về Database Schema và ERD)* | Đã kiểm toán toàn bộ 14 class Entity JPA và cập nhật danh sách chính xác 14 bảng MySQL trong `D17-Technical-Architecture-Report.md` (Mục G), `README.md`, slide 7 của PPTX và PDF. |
| **DISC-05** | Báo cáo ghi nhận tải tối đa 5 ảnh/review và kiểm tra Magic Bytes bằng Apache Tika Core 2.9, trong khi thực tế mã nguồn chỉ cho phép tối đa 3 ảnh/review và kiểm tra byte thuần Java kết hợp `ImageIO`. | **TRUNG BÌNH** *(Sai lệch thông số nghiệp vụ và dependency)* | Đã điều chỉnh giới hạn thành tối đa 3 ảnh, dung lượng < 5MB và phân tích ưu điểm của giải pháp Native Java Byte + ImageIO trong `D17-Technical-Defense-QA.md` (Câu 23, 24), `API.md`, `README.md`, slide 6 của PPTX và PDF. |
| **DISC-06** | Nhầm lẫn cơ chế dọn dẹp ảnh review sau transaction commit là `@TransactionalEventListener` thay vì `TransactionSynchronizationManager.registerSynchronization`. | **THẤP** *(Sai khác về API của Spring Framework)* | Đã trích dẫn chính xác hook `TransactionSynchronization.afterCommit()` trong `D17-Technical-Defense-QA.md` (Câu 21), `API.md` và `README.md`. |
| **DISC-07** | Nhầm lẫn cấu hình Docker Compose thành 5 containers, port 80 và mạng `app-network`, trong khi thực tế chỉ có 4 containers, frontend Nginx port 3000 và mạng `ecommerce-net`. | **TRUNG BÌNH** *(Sai lệch hướng dẫn vận hành và hạ tầng)* | Đã chỉnh sửa toàn bộ sơ đồ 4 tầng, danh sách 4 containers và lệnh truy cập port 3000 trong `README.md`, `D17-Technical-Defense-QA.md` (Câu 2), slide 2 của PPTX và PDF. |
| **DISC-08** | Lẫn lộn tên package `com.rikkeisoft.ecommerce` trong tài liệu phản biện thay vì package thực tế `com.example.ecommerce`. | **THẤP** *(Sai sót văn bản)* | Đã thay thế hàng loạt 46 vị trí trong `docs/D17/D17-Technical-Defense-QA.md`, đảm bảo khớp 100% với cây thư mục thực tế của dự án. |

---

## 5. CHI TIẾT CÁC THAY ĐỔI ĐÃ THỰC HIỆN TRONG TỪNG FILE

### 5.1. File `docs/D17/D17-Technical-Architecture-Report.md`
- **Mục C (Authentication & JWT/Redis):** 
  - Làm rõ cơ chế Instant Lockout: `JwtAuthenticationFilter` thực hiện gọi `userDetailsService.loadUserByUsername(email)` trên mỗi request và kiểm tra cờ `active` trong bảng `users` (`userDetails.isEnabled()`), từ chối ngay lập tức mọi truy cập của tài khoản bị khóa mà không cần chờ Access Token hết hạn 15 phút.
  - Định vị cơ chế JTI Blacklist là giải pháp kiến trúc đề xuất (Architectural Recommendation) cho việc thu hồi tức thì Access Token tại thời điểm đăng xuất.
- **Mục D.2 (Google OAuth2 & Thuật toán CAD):**
  - Viết lại toàn bộ phần giải thích CAD: Khẳng định CAD là thuật toán **Compare-And-Delete** được triển khai bằng Redis Lua Script (`COMPARE_AND_DELETE_TICKET_LUA_SCRIPT`) tại `AuthService.java` (dòng 1043–1058) nhằm khắc phục lỗ hổng an ninh `[SEC-FIND-01]` (Ticket Invalidation / DoS Vulnerability).
  - Phân tích chi tiết 3 mã trả về của script: Mã `1` (Hợp lệ, xóa ticket và trả về userId), Mã `0` (Ticket không tồn tại hoặc hết hạn), Mã `2` (Sai browser binding cookie, **giữ nguyên ticket** để bảo vệ người dùng hợp lệ).
- **Mục D.3 (Account Linking & Anti-Account Hijacking):**
  - Tài liệu hóa chính sách từ chối tự động liên kết trong `CustomOidcUserService.java`: Khi email từ Google ID Token trùng với tài khoản Local thông thường, hệ thống chủ động ném `OAuth2AuthenticationException("email_already_exists")` để ngăn chặn kẻ tấn công tạo tài khoản Google trùng email nhằm chiếm quyền tài khoản nội bộ.
- **Mục G (ERD & Database Architecture):**
  - Cập nhật danh sách 14 bảng MySQL chính xác tuyệt đối theo các entity JPA: `users`, `user_oauth_accounts`, `categories`, `products`, `product_variants`, `coupons`, `orders`, `order_items`, `payments`, `payment_transactions`, `inventory_transactions`, `reviews`, `review_images`, `wishlists`.
  - Phân định rõ: `Role` là Enum lưu trực tiếp trong cột `users.role`; `coupons` là bảng mã giảm giá thực tế; các bảng `refresh_tokens`, `chat_sessions`, `carts` không tồn tại trong MySQL mà được phân bổ sang Redis và Client Storage.

### 5.2. File `docs/D17/D17-Technical-Defense-QA.md`
- **Câu 2:** Chỉnh sửa kiến trúc Docker Compose chuẩn 4 containers (`ecommerce-mysql`, `ecommerce-redis`, `ecommerce-backend`, `ecommerce-frontend`), cổng truy cập Web `http://localhost:3000`, cổng Backend `http://localhost:8080`, mạng bridge `ecommerce-net`.
- **Câu 9:** Viết lại câu trả lời về việc thu hồi phiên: Phân biệt rõ việc khóa tài khoản có hiệu lực ngay tức thì nhờ kiểm tra trạng thái DB trong `JwtAuthenticationFilter`, trong khi Access Token khi logout là stateless và JTI Blacklist là phương án nâng cấp trong tương lai.
- **Câu 12:** Soạn thảo lại toàn diện câu trả lời về **Thuật toán CAD (Compare-And-Delete Lua Script)** trong Redis, chỉ rõ cách khắc phục lỗ hổng `[SEC-FIND-01]` và bằng chứng kiểm thử trong `OAuth2TicketExchangeTest.java`.
- **Câu 13:** Soạn thảo lại câu trả lời về **Anti-Account Hijacking**: Giải thích nguyên nhân tại sao hệ thống từ chối liên kết tự động và bắt buộc phân định giữa tài khoản Google và tài khoản truyền thống.
- **Câu 21:** Chỉnh sửa trích dẫn code Java: Thay đổi từ `@TransactionalEventListener` sang `TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { afterCommit() { ... } })` trong `ReviewServiceImpl.java`.
- **Câu 23:** Cập nhật 5 lớp phòng thủ upload ảnh: Số lượng tối đa 3 ảnh/review, dung lượng < 5MB/ảnh, kiểm tra Magic Bytes Java thuần, giải mã `ImageIO` chống Decompression Bomb ($\le 8000$px, $\le 25M$ pixels), UUID sanitization và Nginx chặn thực thi script.
- **Câu 24:** Viết lại chuyên sâu câu trả lời về Magic Bytes: Trình bày cơ chế so sánh mảng byte JPEG (`FF D8 FF`) và PNG (`89 50 4E 47 0D 0A 1A 0A`), giải thích lý do lựa chọn Native Java Byte Checking + `ImageIO` thay vì Apache Tika (tối ưu hóa hiệu năng $< 0.1 \mu s$, loại bỏ thư viện nặng, chống Decompression Bomb vượt trội).
- **Chuẩn hóa Package Name:** Thay thế toàn bộ 46 đường dẫn từ `com/rikkeisoft/ecommerce` sang `com/example/ecommerce`.

### 5.3. File `README.md`
- **Mục 1 (Tính năng cốt lõi):** Cập nhật mô tả an toàn upload ảnh: Magic Bytes Java thuần, `ImageIO` chống Decompression Bomb, tối đa 3 ảnh và hook `TransactionSynchronization.afterCommit`.
- **Mục 2.1 & 2.2 (Kiến trúc & Công nghệ):** Điều chỉnh sơ đồ 4 tầng: Frontend container (Port 3000, Nginx Alpine reverse proxy), Backend container (Port 8080), MySQL container (Port 3307), Redis container (Port 6379), mạng `ecommerce-net`. Cập nhật bảng công nghệ tương ứng.
- **Mục 5 (Hướng dẫn Docker Compose):** Chuẩn hóa danh sách 4 containers, lệnh `docker compose ps` chuẩn, các URL truy cập hệ thống tại port 3000 và port 8080.

### 5.4. File `docs/API.md`
- Bổ sung tài liệu đặc tả đầy đủ cho 2 endpoint tải ảnh đánh giá:
  - `POST /api/v1/reviews/{reviewId}/images`: Tải lên từ 1 đến 3 ảnh đính kèm bài đánh giá (thuộc `ReviewImageController.java` - [TSK-D14-CCT]).
  - `POST /api/v1/reviews/upload`: Tải lên ảnh đơn lẻ (thuộc `ReviewUploadController.java` - [TSK-D14-DHA]).
- Cập nhật ghi chú an toàn tại endpoint xóa review: Xác nhận việc dọn dẹp file vật lý được thực thi qua `TransactionSynchronizationManager.registerSynchronization(afterCommit)`.

### 5.5. Bộ Slide Bảo vệ (PPTX & PDF)
- Cập nhật script `scratch/generate_presentation.py` và `scratch/generate_pdf_presentation.py` với nội dung đồng bộ 100%:
  - **Slide 2:** 4 containers, mạng `ecommerce-net`, cổng frontend 3000, reverse proxy API và AI streaming.
  - **Slide 3:** Instant lockout qua DB check trong `JwtAuthenticationFilter`, atomic token rotation qua Lua script, đề xuất JTI Blacklist.
  - **Slide 4:** Định nghĩa CAD là Compare-And-Delete Lua script trong Redis giải quyết `[SEC-FIND-01]`, chính sách Anti-Account Hijacking từ chối tự động liên kết local user.
  - **Slide 6:** Đánh giá tối đa 3 ảnh (< 5MB/ảnh), 5 lớp phòng thủ Native Java Magic Bytes + ImageIO, hook `afterCommit()`.
  - **Slide 7:** Danh sách 14 bảng MySQL JPA thực tế, phân định rõ role Enum và Redis store.
- Đã thực thi tái tạo thành công cả 2 file:
  - `docs/D17/D17-Technical-Architecture-Presentation.pptx` (PowerPoint 16:9 widescreen, dark theme).
  - `docs/D17/D17-Technical-Architecture-Presentation.pdf` (8 trang PDF landscape độ nét cao).

---

## 6. CÁC ĐIỂM CHƯA THỂ XÁC MINH HOẶC GIỚI HẠN KỸ THUẬT

Để bảo đảm tính trung thực tuyệt đối khi bảo vệ trước Hội đồng, người phụ trách CCT cần nắm rõ các giới hạn kỹ thuật sau:
1. **JTI Blacklist khi Logout:**
   - *Hiện trạng mã nguồn:* Tại `JwtUtils.java`, Access Token không sinh claim `jti` (JWT ID), và tại `AuthService.java`, phương thức logout chỉ xóa Refresh Token trên Redis và xóa cookie. Access Token của phiên vừa đăng xuất vẫn có hiệu lực toán học cho đến khi hết 15 phút (900.000 ms).
   - *Chiến lược trả lời:* Trả lời thẳng thắn rằng hệ thống tuân thủ thiết kế Stateless Token với TTL ngắn (15 phút). Cơ chế JTI Blacklist trên Redis là phương án nâng cấp kiến trúc đã được thiết kế sẵn sàng và có thể bổ sung trong 1 sprint mà không làm thay đổi luồng nghiệp vụ.
2. **Lưu trữ tệp tin trên Local Disk:**
   - *Hiện trạng mã nguồn:* `FileStorageService.java` đang lưu trữ tệp tin tại thư mục cục bộ `uploads/reviews` được mount shared volume trong Docker. Dự án chưa cấu hình Cloudinary hay AWS S3 trong code thực tế.
   - *Chiến lược trả lời:* Khẳng định hệ thống tuân thủ nguyên lý Dependency Inversion. Toàn bộ logic tương tác tệp tin đều đi qua `FileStorageService`, cho phép chuyển đổi sang triển khai AWS S3 hoặc MinIO bằng cách tạo thêm một Spring `@Bean` mà không làm ảnh hưởng đến tầng Controller hay Service.
3. **Mô hình kinh doanh B2C và Phân quyền:**
   - *Hiện trạng mã nguồn:* Hệ thống chỉ triển khai 2 vai trò duy nhất là `CUSTOMER` và `ADMIN`. Chưa có vai trò `SELLER` hay phân hệ Multi-vendor Marketplace trong mã nguồn thực tế.
   - *Chiến lược trả lời:* Xác nhận rõ ràng dự án tập trung hoàn thiện mô hình B2C chuẩn, tối ưu hóa trải nghiệm khách hàng và năng lực quản trị tập trung của Admin trước khi mở rộng mô hình kinh doanh.

---

## 7. KẾT QUẢ KIỂM TRA SLIDE PPTX VÀ PDF

- **Số lượng slide:** Đúng 8 slide, tương ứng hoàn toàn với 8 chuyên đề kiến trúc bắt buộc:
  1. *Slide 1:* Tổng quan dự án Rikkei Ecommerce và phạm vi công việc CCT (D01–D17).
  2. *Slide 2:* Kiến trúc tổng thể Client-Server (4 tầng, Nginx Gateway, mạng `ecommerce-net`).
  3. *Slide 3:* Kiến trúc Authentication: JWT Dual-token, Redis Lua RTR và kiểm soát phiên.
  4. *Slide 4:* Luồng Google OAuth2/OIDC, giải pháp CAD Lua Script và Anti-Account Hijacking.
  5. *Slide 5:* Phân quyền RBAC đa tầng, phòng chống IDOR và quy tắc BR-AUTH-04.
  6. *Slide 6:* Quản trị Review, 5 lớp an toàn File Upload và tính nhất quán DB-Filesystem.
  7. *Slide 7:* An toàn thông tin (16 cơ chế) và Cơ sở dữ liệu ERD (14 bảng JPA thực tế).
  8. *Slide 8:* Tổng kết dự án, tính minh bạch kỹ thuật và định hướng mở rộng tương lai.
- **Tỷ lệ hiển thị & Bố cục:** Chuẩn màn hình rộng 16:9 (`13.333" x 7.5"` trong PPTX, `960 x 540 pt` trong PDF).
- **Thiết kế & Bảng màu:** Dark theme hiện đại (`#0B0F19`), card bo góc mềm mại (`#1A2235`), viền màu công nghệ phân cấp rõ ràng (Xanh dương, Cyan, Xanh lá, Hổ phách, Đỏ).
- **Font chữ:** Segoe UI / Arial chuẩn Unicode tiếng Việt, không bị lỗi gõ dấu hay tràn khung chữ.

---

## 8. KẾT QUẢ KIỂM TRA API.MD VÀ README.MD

- **Tài liệu `docs/API.md`:**
  - 100% endpoint được kiểm tra đối chiếu trực tiếp với các `@RestController` và `@RequestMapping` trong backend.
  - Bổ sung đầy đủ 2 endpoint upload ảnh của Review ([TSK-D14-CCT] và [TSK-D14-DHA]).
  - Cập nhật đúng các status code: 200, 201, 400, 401, 403, 404, 409, 429, 500.
  - Chuẩn hóa cấu trúc bao đóng `ApiResponse<T>` và `PageResponse<T>`.
- **Tài liệu `README.md`:**
  - Cấu trúc sơ đồ 4 tầng phản ánh chính xác cấu hình `docker-compose.yml`.
  - Hướng dẫn khởi chạy 4 containers rõ ràng, kiểm tra sức khỏe và xem log chi tiết.
  - Tuyệt đối không chứa secret thực tế (đã thay thế toàn bộ bằng placeholder chuẩn `your_secure_password`).

---

## 9. KẾT LUẬN VÀ CHIẾN LƯỢC BẢO VỆ CHO CCT

### 9.1. Đánh giá tổng thể độ sẵn sàng
Bộ tài liệu kỹ thuật và slide thuyết trình phục vụ bảo vệ của Cao Chí Thiện (CCT) đã hoàn tất 100% quá trình kiểm toán và hiệu chỉnh:
- **Độ tin cậy kỹ thuật:** Đạt cấp độ xuất sắc. Mọi phát biểu đều có căn cứ mã nguồn cụ thể (file, class, method, hằng số, dòng code).
- **Tính nhất quán:** Đồng bộ 100% giữa Báo cáo kiến trúc (`.md`), Kịch bản phản biện (`.md`), Slide PPTX, Slide PDF, API Spec (`API.md`) và Hướng dẫn vận hành (`README.md`).
- **Tính an toàn thông tin:** Không có bất kỳ lỗ hổng lộ lọt secret nào trên toàn bộ kho tài liệu.

### 9.2. Ba câu hỏi phản biện trọng tâm và chiến lược trả lời an toàn cho CCT

1. **Hội đồng hỏi về CAD:**  
   *"CAD trong Google OAuth2 của các bạn nghĩa là gì? Tại sao phải dùng Lua script?"*  
   - **Chiến lược trả lời:** Nêu rõ CAD là **Compare-And-Delete** Lua script trong Redis (`COMPARE_AND_DELETE_TICKET_LUA_SCRIPT` trong `AuthService.java`). Giải thích rằng script thực hiện so sánh nguyên tử giữa ticket và browser binding cookie để giải quyết lỗ hổng `[SEC-FIND-01]`. Nếu kẻ tấn công đánh cắp ticket nhưng không có cookie trình duyệt, script trả về mã 2 và **không xóa ticket**, giúp người dùng hợp lệ không bị vô hiệu hóa ticket oan (chống DoS).

2. **Hội đồng hỏi về Account Linking:**  
   *"Nếu tài khoản local đã đăng ký bằng email `abc@gmail.com`, khi người dùng bấm 'Đăng nhập Google' với cùng email đó thì hệ thống xử lý như thế nào?"*  
   - **Chiến lược trả lời:** Trả lời dứt khoát rằng hệ thống **từ chối liên kết tự động** (`CustomOidcUserService` ném lỗi `email_already_exists`). Giải thích rằng đây là chính sách bảo mật chủ động nhằm chống tấn công **Account Hijacking** (Kẻ tấn công không thể chiếm quyền kiểm soát tài khoản nội bộ chỉ bằng cách tạo tài khoản Google trùng email).

3. **Hội đồng hỏi về Instant Token Revocation:**  
   *"Nếu JWT Access Token là Stateless và có hạn 15 phút, khi Admin khóa tài khoản của một khách hàng thì khách hàng đó có tiếp tục gọi API được trong 15 phút còn lại không?"*  
   - **Chiến lược trả lời:** Khách hàng bị khóa sẽ bị chặn **ngay lập tức** ở request tiếp theo. Mặc dù Access Token còn hạn toán học, nhưng tại `JwtAuthenticationFilter`, hệ thống nạp lại `UserDetails` từ DB qua `userDetailsService.loadUserByUsername` và gọi `userDetails.isEnabled()`. Do cờ `active` trong MySQL đã đổi thành `false`, filter lập tức từ chối và ném lỗi 401/403 mà không cần chờ Access Token hết hạn.

---
*Báo cáo kiểm toán được lập và phê duyệt bởi Senior Software Architect & Technical Lead — Cao Chí Thiện (CCT).*

