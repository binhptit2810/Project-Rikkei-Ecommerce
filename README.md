# Nền tảng Thương mại Điện tử Rikkei Ecommerce (Project-Rikkei-Ecommerce)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.3.4-green.svg)](https://spring.io/projects/spring-security)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose%20v2-blue.svg)](https://www.docker.com/)

---

## 📖 1. Giới thiệu Tổng quan Hệ thống

**Rikkei Ecommerce** là nền tảng thương mại điện tử B2C hiện đại, toàn diện và có tính sẵn sàng cao, được thiết kế theo mô hình **Client-Server tách rời (Decoupled Architecture)**. Hệ thống cung cấp trải nghiệm mua sắm trực tuyến mượt mà cho khách hàng cá nhân cùng cổng quản trị mạnh mẽ dành cho ban quản trị.

### Tính năng Cốt lõi của Hệ thống
- **Xác thực Đa phương thức:** Đăng ký xác thực OTP qua Email, Đăng nhập truyền thống (Email/Password), Đăng nhập một chạm **Google OAuth2 / OpenID Connect** (Authorization Code Exchange).
- **Quản lý Phiên Bảo mật Cao:** Cơ chế **Dual-Token** kết hợp Access Token ngắn hạn (15 phút) và Refresh Token dài hạn (7 ngày) quản lý tập trung trên Redis với **Refresh Token Rotation (RTR)**, **Grace Period** và **Token Reuse Detection**.
- **Phân quyền Đa tầng (RBAC):** Phân chia rõ ràng giữa `ROLE_CUSTOMER` và `ROLE_ADMIN` qua 2 lớp phòng thủ: URL Pattern Filtering (`SecurityFilterChain`) và Method-level Security (`@PreAuthorize`).
- **Phòng chống Lỗ hổng IDOR:** Bảo vệ quyền riêng tư người dùng trong quản lý Đơn hàng, Profile và Đánh giá thông qua nguyên tắc *Context-Bound Ownership Verification*.
- **Quản trị Đánh giá & Tải ảnh An toàn:** Hệ thống Review đính kèm hình ảnh với **5 lớp phòng thủ** (Apache Tika Magic Bytes, UUID Sanitization, Extension Whitelist, Size Limit, Execution Prevention) và đảm bảo tính nhất quán DB-Filesystem qua Event-driven `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- **Thanh toán Trực tuyến:** Hỗ trợ thanh toán nhận hàng (COD), mô phỏng (Simulated) và tích hợp cổng thanh toán trực tuyến **VNPay Sandbox**.
- **Tư vấn Bán hàng Thông minh:** Trợ lý ảo AI tích hợp **Spring AI** và mô hình **Google Gemini 2.5 Flash** hỗ trợ tra cứu sản phẩm, kiểm tra tồn kho và đơn hàng theo thời gian thực.

---

## 🏗️ 2. Kiến trúc Tổng thể & Công nghệ Sử dụng

### 2.1. Sơ đồ Kiến trúc 4 Tầng (4-Tier Architecture)

```
[ Khách hàng / Quản trị viên ]
             │ (HTTPS / HTTP)
             ▼
   ┌────────────────────────────────────────────────────────┐
   │         Nginx Reverse Proxy & Gateway (Port 80)        │
   │  • Routing: / -> Frontend SPA | /api/ -> Spring Boot  │
   │  • Static Serving: /uploads/ (Binary Images)           │
   └───────────────────────────┬────────────────────────────┘
                               │ (Docker Bridge: app-network)
             ┌─────────────────┴─────────────────┐
             ▼                                   ▼
┌─────────────────────────┐         ┌─────────────────────────┐
│   React 18 Frontend     │         │ Spring Boot 3 REST API  │
│ • Vite 5.4, TailwindCSS │         │ • Java 21 LTS, Port 8080│
│ • Axios Interceptors    │         │ • Spring Security 6     │
│ • Zustand State Store   │         │ • Bucket4j Rate Limit   │
└─────────────────────────┘         │ • Spring Data JPA / ORM │
                                    └────────────┬────────────┘
                                                 │
                        ┌────────────────────────┴────────────────────────┐
                        ▼                                                 ▼
             ┌─────────────────────┐                           ┌─────────────────────┐
             │   MySQL 8.0 RDBMS   │                           │    Redis 7.0 Cache  │
             │ • 14 bảng quan hệ   │                           │ • Opaque RT Store   │
             │ • InnoDB ACID       │                           │ • Grace Period TTL  │
             │ • UTF8MB4 Encoding  │                           │ • OTP & Rate Limit  │
             └─────────────────────┘                           └─────────────────────┘
```

### 2.2. Bảng Công nghệ Chi tiết

| Tầng kiến trúc | Công nghệ / Thư viện | Phiên bản | Vai trò & Mục đích sử dụng |
|:---|:---|:---|:---|
| **Language & Runtime** | Java OpenJDK | 21 LTS | Ngôn ngữ backend chính, tối ưu hóa ảo hóa luồng và GC |
| **Framework** | Spring Boot | 3.3.5 | Nền tảng microservices & RESTful API framework |
| **Security** | Spring Security | 6.3.4 | Filter chain, JWT Stateless Filter, RBAC, OAuth2 Client |
| **JWT** | JJWT (`jjwt-api`, `jjwt-impl`) | 0.12.6 | Tạo, giải mã và xác minh chữ ký số HMAC-SHA256 (512-bit) |
| **File Inspection** | Apache Tika Core | 2.9.2 | Nhận diện Magic Bytes nhị phân chống mã độc ngụy trang ảnh |
| **Rate Limiting** | Bucket4j Core | 8.14.0 | Thuật toán Token Bucket chống Brute Force và DDoS |
| **AI Assistant** | Spring AI (Gemini) | 1.0.0-M4 | Chatbot tư vấn bán hàng RAG Function Calling |
| **Database** | MySQL Server | 8.0.39 | Cơ sở dữ liệu quan hệ lưu trữ nghiệp vụ chính (14 bảng) |
| **ORM / Data Access** | Spring Data JPA / Hibernate | 6.5.x | Quản lý Entity, Transaction ACID, tối ưu hóa câu lệnh SQL |
| **In-Memory Cache** | Redis / Lettuce Client | 7.0 | Lưu trữ Refresh Token, OTP, Rate Limit counter và Chat Memory |
| **Frontend Framework**| React | 18.3.1 | Thư viện xây dựng giao diện Single Page Application (SPA) |
| **Build Tool** | Vite | 5.4.10 | Công cụ đóng gói mã nguồn frontend siêu tốc |
| **Styling** | Tailwind CSS | 3.4.14 | Utility-first CSS framework thiết kế responsive |
| **State & HTTP** | Zustand / Axios | Mới nhất | Quản lý state toàn cục và cấu hình Axios Interceptors |
| **Reverse Proxy** | Nginx Alpine | 1.25 | Cổng vào duy nhất, định tuyến request, phục vụ static assets |
| **Containerization** | Docker & Docker Compose | v2.x | Đóng gói toàn bộ hệ sinh thái chạy độc lập môi trường |

---

## 💻 3. Yêu cầu Môi trường Cài đặt

Để khởi chạy dự án, máy trạm cần đáp ứng các yêu cầu phần mềm tối thiểu sau:

| Thành phần | Phiên bản khuyến nghị | Kiểm tra phiên bản bằng lệnh |
|:---|:---|:---|
| **Hệ điều hành** | Windows 10/11, macOS 12+, Ubuntu 22.04 LTS | `systeminfo` / `uname -a` |
| **Java Development Kit** | OpenJDK 21 LTS | `java -version` |
| **Node.js & npm** | Node 18.x hoặc 20.x LTS, npm 9+ | `node -v` && `npm -v` |
| **Gradle** | Gradle 8.10.2 (hoặc dùng `./gradlew` tích hợp) | `gradle -v` |
| **Docker & Compose** | Docker Desktop 4.25+ (Compose v2.20+) | `docker --version` && `docker compose version` |
| **MySQL Client** | MySQL 8.0 Client / DBeaver / DataGrip | `mysql --version` |

---

## 🔒 4. Hướng dẫn Cấu hình Biến Môi trường An toàn

Hệ thống tuân thủ nghiêm ngặt nguyên tắc **The Twelve-Factor App**. Tuyệt đối **KHÔNG** hardcode secret, mật khẩu hay API key trong mã nguồn hoặc kho lưu trữ Git.

### 4.1. Khởi tạo file cấu hình môi trường
Sao chép file cấu hình mẫu từ `.env.example`:
```bash
cp .env.example .env
```

### 4.2. Danh mục Biến Môi trường Cần Thiết

| Tên biến | Ví dụ giá trị | Mục đích & Mô tả |
|:---|:---|:---|
| `DB_HOST` | `localhost` (dev) hoặc `mysql` (docker) | Tên host của cơ sở dữ liệu MySQL |
| `DB_PORT` | `3306` | Cổng kết nối MySQL |
| `DB_NAME` | `rikkei_ecommerce` | Tên schema cơ sở dữ liệu |
| `DB_USERNAME` | `root` | Tài khoản quản trị database |
| `DB_PASSWORD` | `your_secure_db_password` | Mật khẩu database (tự sinh, không dùng mật khẩu mặc định) |
| `REDIS_HOST` | `localhost` (dev) hoặc `redis` (docker) | Tên host máy chủ Redis |
| `REDIS_PORT` | `6379` | Cổng kết nối Redis |
| `REDIS_PASSWORD`| `your_secure_redis_password` | Mật khẩu bảo vệ kết nối Redis (nếu có) |
| `JWT_SECRET_KEY` | *Hex 512-bit bí mật* | Khóa ký HMAC-SHA256 (Tối thiểu 64 ký tự hex) |
| `JWT_ACCESS_EXP` | `900000` | Thời gian sống Access Token (15 phút tính theo ms) |
| `JWT_REFRESH_EXP`| `604800000` | Thời gian sống Refresh Token (7 ngày tính theo ms) |
| `GOOGLE_CLIENT_ID` | `xxx.apps.googleusercontent.com` | Client ID từ Google Cloud Console (OAuth Web App) |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-xxxxxx` | Client Secret bảo mật của Google OAuth2 |
| `SPRING_AI_GEMINI_API_KEY` | `AIzaSyxxxxxx` | Khóa API dịch vụ Gemini AI phục vụ chatbot |
| `VNPAY_TMN_CODE` | `VNPAYXXX` | Mã Website TMN Code do VNPay Sandbox cấp |
| `VNPAY_HASH_SECRET` | `SECRETXXX` | Chuỗi bí mật tạo chữ ký Checksum VNPay |
| `FILE_UPLOAD_DIR`| `uploads/reviews` | Thư mục vật lý lưu trữ hình ảnh tải lên |

> [!WARNING]
> Tuyệt đối không commit file `.env` lên Git! File `.gitignore` đã được cấu hình tự động loại trừ mọi file `.env`, `.env.local` và file chứa credentials.

---

## 🐳 5. Hướng dẫn Khởi chạy với Docker Compose (Production Ready)

Đây là phương thức khuyến nghị nhanh nhất và chuẩn hóa nhất để triển khai toàn bộ hệ thống (Frontend, Backend, Database, Redis, Nginx) chỉ với 1 lệnh duy nhất.

### Bước 1: Khởi động toàn bộ cụm dịch vụ
Từ thư mục gốc của dự án:
```bash
docker compose up -d --build
```

### Bước 2: Kiểm tra trạng thái các Container
```bash
docker compose ps
```
*Kết quả chuẩn yêu cầu 5 containers ở trạng thái `running` (hoặc `healthy`):*
```
NAME                          IMAGE                             COMMAND                  SERVICE      STATUS
rikkei-ecommerce-nginx-1      nginx:1.25-alpine                 "/docker-entrypoint.…"   nginx        Up (healthy) (port 0.0.0.0:80->80/tcp)
rikkei-ecommerce-backend-1    rikkei-ecommerce-backend:latest   "java -jar app.jar"      backend      Up (healthy)
rikkei-ecommerce-frontend-1   rikkei-ecommerce-frontend:latest  "nginx -g 'daemon of…"   frontend     Up
rikkei-ecommerce-mysql-1      mysql:8.0                         "docker-entrypoint.s…"   mysql        Up (healthy)
rikkei-ecommerce-redis-1      redis:7.0-alpine                  "docker-entrypoint.s…"   redis        Up (healthy)
```

### Bước 3: Xem nhật ký hoạt động (Logs)
```bash
# Xem log tổng hợp theo thời gian thực
docker compose logs -f

# Xem log riêng biệt của backend Spring Boot
docker compose logs -f backend
```

### Bước 4: Truy cập Hệ thống
- **Giao diện Mua sắm (Frontend):** `http://localhost`
- **Tài liệu Swagger API (Backend):** `http://localhost/swagger-ui/index.html` (hoặc `http://localhost:8080/swagger-ui/index.html`)
- **Kiểm tra Sức khỏe Hệ thống (Health Check):** `http://localhost/api/v1/health`

### Bước 5: Dừng hoặc Gỡ bỏ Cụm Dịch vụ
```bash
# Tạm dừng nhưng giữ nguyên dữ liệu
docker compose stop

# Gỡ bỏ container và giải phóng volume (Xóa dữ liệu DB nếu thêm -v)
docker compose down
```

---

## 🛠️ 6. Hướng dẫn Khởi chạy Môi trường Phát triển Cục bộ (Local Dev)

Nếu cần debug trực tiếp mã nguồn không qua Docker, hãy thực hiện theo các bước sau:

### 6.1. Khởi động Cơ sở dữ liệu và Cache (MySQL & Redis)
Có thể chạy nhanh MySQL và Redis qua Docker:
```bash
docker compose up -d mysql redis
```

### 6.2. Chạy Backend (Spring Boot 3.3.5)
```bash
cd backend

# Trên Windows PowerShell
.\gradlew bootRun --args='--spring.profiles.active=dev'

# Trên Linux/macOS
./gradlew bootRun --args='--spring.profiles.active=dev'
```
*Backend sẽ sẵn sàng tiếp nhận request tại `http://localhost:8080`.*

### 6.3. Chạy Frontend (React 18 & Vite)
Mở một cửa sổ Terminal mới:
```bash
cd frontend

# Cài đặt thư viện dependencies
npm install

# Khởi chạy Vite Dev Server
npm run dev
```
*Frontend sẽ chạy tại `http://localhost:5173` và tự động kết nối API qua cấu hình Vite Proxy hoặc CORS.*

---

## 🧪 7. Hướng dẫn Chạy Bộ Kiểm thử (Test Suite)

Dự án tích hợp đầy đủ kiểm thử đơn vị (Unit Test) và kiểm thử tích hợp (Integration Test) với JUnit 5, Mockito và MockMvc.

### 7.1. Chạy Toàn bộ Test Suite Backend
```bash
cd backend
./gradlew test
```

### 7.2. Chạy Các Bài Kiểm thử Riêng Biệt do Cao Chí Thiện Phụ Trách
```bash
# Kiểm thử Bộ phân quyền RBAC & Quản lý User (AdminUserControllerTest)
./gradlew test --tests com.example.ecommerce.controller.AdminUserControllerTest

# Kiểm thử Core Xác thực JWT, OTP & Login (AuthControllerTest)
./gradlew test --tests com.example.ecommerce.controller.AuthControllerTest

# Kiểm thử Đánh giá Sản phẩm & Kiểm duyệt Admin (AdminReviewControllerTest)
./gradlew test --tests com.example.ecommerce.controller.AdminReviewControllerTest

# Kiểm thử Xác thực OTP Email Staging (EmailOtpStagingIntegrationTest)
./gradlew test --tests com.example.ecommerce.integration.EmailOtpStagingIntegrationTest
```

### 7.3. Xem Báo cáo Kết quả Kiểm thử
Sau khi chạy xong, mở file báo cáo HTML trực quan:
```
backend/build/reports/tests/test/index.html
```

---

## 📚 8. Danh mục Tài liệu Tham khảo Kỹ thuật

| Tài liệu | Đường dẫn chi tiết | Nội dung chính |
|:---|:---|:---|
| **Báo cáo Kiến trúc Toàn diện** | [D17-Technical-Architecture-Report.md](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/D17/D17-Technical-Architecture-Report.md) | Khảo sát 15 tiêu chí, Kiến trúc Client-Server, Luồng JWT/Redis, Google OAuth2, RBAC, Review và ERD 14 bảng. |
| **27 Kịch bản Phản biện Bảo vệ** | [D17-Technical-Defense-QA.md](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/D17/D17-Technical-Defense-QA.md) | 27 câu hỏi - đáp chuyên sâu 6 phần bảo vệ tự tin trước hội đồng kỹ thuật. |
| **Slide Thuyết trình Bảo vệ (PPTX)** | [D17-Technical-Architecture-Presentation.pptx](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/D17/D17-Technical-Architecture-Presentation.pptx) | 8 slides tỷ lệ 16:9 thiết kế chuẩn Software Engineering phục vụ trình chiếu. |
| **Slide Thuyết trình Bảo vệ (PDF)** | [D17-Technical-Architecture-Presentation.pdf](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/D17/D17-Technical-Architecture-Presentation.pdf) | Bản preview PDF chất lượng cao đồng bộ nội dung slide trình chiếu. |
| **Đặc tả RESTful API Chi tiết** | [API.md](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/API.md) | Đặc tả 100% endpoints, HTTP Status codes, Request/Response payload chuẩn hóa. |
| **Đặc tả Yêu cầu Hệ thống (SRS)** | [SRS.md](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/SRS.md) | Tài liệu yêu cầu chức năng, phi chức năng và quy tắc nghiệp vụ. |
| **Phân công Nhiệm vụ Nhóm** | [TEAM-WORK.md](file:///d:/Doanh%20nghiep/20-10-2026/Project-Rikkei-Ecommerce/docs/TEAM-WORK.md) | Ma trận phân công công việc chi tiết giữa các thành viên dự án. |

---

## 👥 9. Đội ngũ Triển khai & Phân công Trách nhiệm

| Thành viên | Vai trò dự án | Mã nhiệm vụ phụ trách chính |
|:---|:---|:---|
| **Cao Chí Thiện (CCT)** | Senior Software Engineer & Architect | **TSK-D01 – D17**: Thiết kế Kiến trúc, Authentication Core (JWT/Redis), Google OAuth2, RBAC, An toàn thông tin, Quản trị Review, Docker & Tài liệu bảo vệ. |
| **Đỗ Đức Mạnh (DDM)** | Fullstack Developer | **TSK-D11-DDM**: Phân hệ Đánh giá sản phẩm (Product Review Backend & Frontend). |
| **Đỗ Hoàng Anh (DHA)** | Backend Developer | **TSK-D05-DHA**, **TSK-D14-DHA**: Tích hợp hình ảnh ReviewImage và kiểm duyệt đánh giá Admin. |
| **Đặng Thanh Bình (DTB)** | Backend Developer | Quản lý Đơn hàng, Tồn kho (Inventory), Thanh toán VNPay / COD và Webhook IPN. |

---
*© 2026 Rikkei Ecommerce Project. Được phát triển và hoàn thiện bởi Đội ngũ Kỹ thuật Dự án.*
