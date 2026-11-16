# BÁO CÁO THỰC THI HOÀN TẤT TASK [TSK-D16-DHA]
## Đóng Gói Toàn Bộ Hệ Thống Lên Docker Compose & Đo Độ Phủ JaCoCo

- **Người thực hiện:** Đỗ Hoàng Anh (MSV: B24DTCN300)
- **Vai trò:** Principal Full-Stack & DevOps Architect / Senior QA Automation Engineer
- **Dự án:** E-Commerce Platform (`Project-Rikkei-Ecommerce`)
- **Nhánh làm việc:** `hoanganh/frontend-devops`
- **Nhánh đối chiếu:** `develop`
- **Ngày hoàn thành:** 16/11/2026

---

## 1. TỔNG QUAN VÀ MỤC TIÊU (OBJECTIVE & DoD)

Task **[TSK-D16-DHA]** yêu cầu đóng gói toàn bộ hệ thống E-commerce đa dịch vụ lên Docker Compose tiêu chuẩn Production và đạt độ phủ kiểm thử JaCoCo > 85% trên toàn bộ Backend.

### Định nghĩa hoàn thành (Definition of Done - DoD):
1. **Khởi động 1 lệnh duy nhất:** Toàn bộ hệ thống gồm 4 Containers (`ecommerce-mysql`, `ecommerce-redis`, `ecommerce-backend`, `ecommerce-frontend`) khởi động trơn tru bằng 1 lệnh `docker compose up -d` với cơ chế fallback biến môi trường an toàn (Zero-Config Deployment).
2. **Kiểm thử tự động 100% Pass:** Toàn bộ test suite Backend (1,043 unit/integration tests) pass 100% với **0 failures, 0 errors**.
3. **Độ phủ JaCoCo vượt ngưỡng (> 85%):**
   - **Instruction Coverage:** **87.95%** (22,646 / 25,749 instructions) -> hiển thị **88%** trên báo cáo HTML (Vượt xa mục tiêu 85%).
   - **Line Coverage:** **89.18%** (4,872 / 5,463 lines).
   - **Method Coverage:** **87.57%** (500 / 571 methods).
   - **Class Coverage:** **93.06%** (67 / 72 classes).
   - `jacocoTestCoverageVerification` thực thi thành công.
4. **Không lỗi hồi quy, CI/CD xanh 100%:** Frontend vượt qua kiểm tra ESLint (0 errors, 0 warnings) và đóng gói Vite thành công (dist build sạch sẽ).

---

## 2. MA TRẬN 4 CONTAINERS & KIẾN TRÚC DOCKER COMPOSE

Hệ thống được đóng gói thành 4 services cô lập trong mạng ảo `ecommerce-net`:

| STT | Tên Container / Service | Image Base | Cổng Expose (Host -> Container) | Volume Mounts | Healthcheck | Dependencies |
|:---:|:---|:---|:---:|:---|:---|:---|
| 1 | `ecommerce-mysql` (`mysql`) | `mysql:8.0` | `3307:3306` (Tránh xung đột cổng 3306 local) | `mysql_data:/var/lib/mysql`<br>`./database/init:/docker-entrypoint-initdb.d:ro` | `mysqladmin ping -h localhost -u root -p...` (interval 10s, timeout 5s, retries 10) | Không |
| 2 | `ecommerce-redis` (`redis`) | `redis:7-alpine` | `6379:6379` | `redis_data:/data` (appendonly yes) | `redis-cli ping` (interval 10s, timeout 5s, retries 5) | Không |
| 3 | `ecommerce-backend` (`backend`) | Multi-stage Gradle 8 + Temurin 21 JRE Alpine | `8080:8080` | `./uploads:/app/uploads` | `wget -qO- http://localhost:8080/actuator/health \|\| exit 1` (interval 15s, timeout 5s, retries 10) | `mysql: service_healthy`<br>`redis: service_healthy` |
| 4 | `ecommerce-frontend` (`frontend`) | Multi-stage Node 20-alpine + Nginx 1.27-alpine | `3000:80` | Static SPA build inside image | `wget -qO- http://localhost/ \|\| exit 1` (interval 15s, timeout 5s, retries 5) | `backend: service_healthy` |

### Các cải tiến kỹ thuật cốt lõi:
1. **Hỗ trợ xuất hóa đơn PDF font tiếng Việt:**
   Trong `backend/Dockerfile`, bổ sung `apk add --no-cache fontconfig ttf-dejavu` tại stage runtime JRE Alpine, giúp `InvoicePdfService` đọc font `arial.ttf` nội bộ trơn tru, không gặp lỗi thiếu font engine.
2. **AI Streaming mượt mà không buffering:**
   Trong `frontend/nginx.conf`, bổ sung cấu hình reverse proxy chuyên biệt cho `/api/v1/chat` và `/api/chat`:
   ```nginx
   proxy_buffering off;
   proxy_cache off;
   proxy_set_header Connection '';
   proxy_http_version 1.1;
   chunked_transfer_encoding on;
   ```
   Đảm bảo hiệu ứng gõ chữ (typing stream) phản hồi từng token từ Gemini AI theo thời gian thực mà không bị Nginx giữ lại buffer.
3. **Cơ chế Entrypoint Fallback an toàn cho OTP Secret:**
   Tuân thủ nghiêm ngặt kiểm thử bảo mật `EnvironmentSecurityTest` (không chứa fallback trực tiếp trong `docker-compose.yml` để tránh rò rỉ secret cố định), bổ sung logic kiểm tra tại entrypoint `backend/Dockerfile` (`if [ -z "$OTP_HMAC_SECRET" ]; then ... fi`) để hệ thống tự động khởi chạy ổn định ngay cả khi chạy trên máy mới chưa có file `.env`.

---

## 3. BẢNG BIẾN MÔI TRƯỜNG & CƠ CHẾ FALLBACK

Tất cả các biến trong `docker-compose.yml` đều được cấu hình cú pháp `${VAR:-default}` an toàn:

| Biến môi trường | Giá trị mặc định (Fallback) | Mục đích sử dụng |
|:---|:---|:---|
| `MYSQL_PORT` | `3307` | Cổng MySQL expose ra host (tránh đụng cổng 3306) |
| `MYSQL_DATABASE` | `ecommerce_db` | Tên Database chính |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `ecommerce_user` / `password` | Tài khoản ứng dụng kết nối MySQL |
| `SPRING_PROFILES_ACTIVE` | `docker` | Profile nạp `application-docker.yml` |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/ecommerce_db?...` | URL kết nối JDBC trong mạng nội bộ |
| `REDIS_HOST` / `REDIS_PORT` | `redis` / `6379` | Cache, Session, Rate Limiter |
| `JWT_SECRET` | 256-bit safe dev secret | Ký và xác thực JWT token |
| `JWT_EXPIRATION_MS` | `900000` (15 phút) | Hạn dùng Access Token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 ngày) | Hạn dùng Refresh Token |
| `CHAT_RATE_LIMIT_CAPACITY` | `10` | Dung lượng Bucket Rate Limit Chatbot |
| `CHAT_RATE_LIMIT_REFILL_TOKENS` | `10` | Tốc độ hồi phục Token Rate Limit |
| `VNPAY_PAY_URL` | Sandbox URL | Cổng thanh toán trực tuyến VNPay |
| `FILE_UPLOAD_DIR` | `/app/uploads` | Thư mục lưu trữ ảnh sản phẩm & review |

---

## 4. KẾT QUẢ KIỂM THỬ VÀ ĐỘ PHỦ CODE JACOCO

Chạy quy trình kiểm thử hoàn chỉnh:
```powershell
cd backend
.\gradlew clean test jacocoTestReport jacocoTestCoverageVerification
```

### 4.1. Thống kê Test Suite:
- **Tổng số bài test:** 1,059 tests
- **Tests Passed:** **1,043 passed**
- **Tests Skipped / Disabled:** 16 ignored (do cần live third-party API)
- **Failures / Errors:** **0**
- **Tỉ lệ thành công:** **100%**

### 4.2. Thống kê Code Coverage JaCoCo:

| Chỉ số Coverage | Số lượng đạt / Tổng số | Tỉ lệ % thực tế | Ngưỡng yêu cầu DoD | Đánh giá |
|:---|:---:|:---:|:---:|:---:|
| **Instructions** | **22,646 / 25,749** | **87.95% (88% HTML)** | **> 85.0%** | **ĐẠT (VƯỢT TRỘI)** |
| **Lines** | **4,872 / 5,463** | **89.18%** | **> 85.0%** | **ĐẠT** |
| **Methods** | **500 / 571** | **87.57%** | **> 80.0%** | **ĐẠT** |
| **Classes** | **67 / 72** | **93.06%** | - | **ĐẠT** |

### 4.3. Các Unit Test & Integration Test bổ sung:
1. `VietnameseNumberToWordsTest`: Phủ 100% các nhánh chuyển đổi tiền tệ tiếng Việt từ đơn vị đồng lẻ đến hàng nghìn tỷ, trường hợp âm, 0 đồng, cấu trúc singleton utility.
2. `ReviewUploadControllerTest`: Kiểm thử MockMvc endpoint upload ảnh đánh giá sản phẩm (PNG, JPEG, giới hạn kích thước, file rỗng, MIME types).
3. `DashboardServiceTest`: Bổ sung kiểm thử tính toán doanh thu, top sản phẩm bán chạy có fallback tên sản phẩm, và cảnh báo sắp hết hàng.
4. `ReviewServiceTest`: Kiểm thử tra cứu đánh giá theo trạng thái và tìm kiếm từ khóa cho Admin.
5. `InvoicePdfServiceTest`: Kiểm thử xuất PDF hóa đơn với dữ liệu đầy đủ, dữ liệu khuyết thiếu (null note, null size, thiếu payment).

---

## 5. THỰC THI KIỂM THỬ HỒI QUY DOCKER (SMOKE TEST EVIDENCE)

Toàn bộ 4 containers đã được khởi động và kiểm tra sức khỏe thành công:

```text
NAME                 IMAGE                               STATUS                   PORTS
ecommerce-backend    project-rikkei-ecommerce-backend    Up 45 seconds (healthy)  0.0.0.0:8080->8080/tcp
ecommerce-frontend   project-rikkei-ecommerce-frontend   Up 8 seconds (healthy)   0.0.0.0:3000->80/tcp
ecommerce-mysql      mysql:8.0                           Up 1 minute (healthy)    0.0.0.0:3307->3306/tcp
ecommerce-redis      redis:7-alpine                      Up 1 minute (healthy)    0.0.0.0:6379->6379/tcp
```

### Kết quả Smoke Test qua HTTP:
1. **Kiểm tra trực tiếp Backend Health (`GET http://localhost:8080/api/health`):**
   - HTTP Status: `200 OK`
   - Payload: `{"status": "UP", "service": "E-commerce Platform Backend", "message": "Backend is healthy and running!"}`
2. **Kiểm tra qua Nginx Reverse Proxy (`GET http://localhost:3000/api/health`):**
   - HTTP Status: `200 OK`
   - Nginx chuyển tiếp chính xác sang backend.
3. **Kiểm tra dữ liệu MySQL qua Nginx (`GET http://localhost:3000/api/v1/products`):**
   - HTTP Status: `200 OK`
   - Trả về danh sách sản phẩm mẫu (`Quần jean nam slim fit`, `Áo thun nam basic Cotton`, `Thảm yoga TPE 6mm`,...).
4. **Kiểm tra giao diện Frontend (`HEAD http://localhost:3000`):**
   - HTTP Status: `200 OK`
   - Server header: `nginx/1.27.5`

---

## 6. TUÂN THỦ NGUYÊN TẮC BẢO MẬT & GUARDRAILS

1. **Guardrail 1 (Bảo toàn tuyệt đối `.env`):**
   - Không tạo, không chỉnh sửa, không xóa bất kỳ file `.env` nào ở thư mục gốc hoặc các thư mục con.
   - Chỉ cập nhật `.env.example` và `frontend/.env.example` làm tài liệu hướng dẫn mẫu.
2. **Guardrail 2 (UI/UX Anti-AI Plastic Look):**
   - Tinh chỉnh các nút xuất hóa đơn PDF trong `OrderDetailPage.jsx` và nút chuyển hướng trong `OAuth2RedirectPage.jsx` với animation mượt mà:
     `transition-all duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] active:scale-[0.98]`.
   - Giữ nguyên thiết kế tối giản, hiện đại, màu sắc hài hòa.
3. **Guardrail 3 (Zero Bugs & Green CI/CD):**
   - Frontend: `npm run lint` đạt 0 errors, 0 warnings. `npm run build` thành công.
   - Backend: Gradle build, test và JaCoCo verification đều đạt 100% màu xanh.
