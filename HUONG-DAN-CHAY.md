# 🚀 Hướng Dẫn Khởi Chạy Dự Án E-commerce Platform

Tài liệu hướng dẫn chi tiết cách chạy toàn bộ hệ thống (Database, Redis, Backend Spring Boot và Frontend React).

---

## 📋 Mục lục
1. [Tài khoản đăng nhập mẫu](#1-tài-khoản-đăng-nhập-mẫu)
2. [Cách 1: Chạy trọn gói bằng Docker Compose (Khuyên dùng)](#2-cách-1-chạy-trọn-gói-bằng-docker-compose-khuyên-dùng)
3. [Cách 2: Chạy từng dịch vụ trên máy Local](#3-cách-2-chạy-từng-dịch-vụ-trên-máy-local)
   - [3.1 Khởi động Database & Redis](#31-khởi-động-database--redis)
   - [3.2 Khởi động Backend (Spring Boot)](#32-khởi-động-backend-spring-boot)
   - [3.3 Khởi động Frontend (React / Vite)](#33-khởi-động-frontend-react--vite)
4. [Kiểm tra các chức năng chính](#4-kiểm-tra-các-chức-năng-chính)
5. [Các lỗi thường gặp và cách xử lý](#5-các-lỗi-thường-gặp-và-cách-xử-lý)

---

## 1. Tài khoản đăng nhập mẫu

Dữ liệu mẫu đã được nạp sẵn từ file `database/init/02-sample-data.sql`:

| Vai trò | Email | Mật khẩu | Quyền hạn |
|---|---|---|---|
| **Quản trị viên (Admin)** | `admin@eshop.com` | `Admin@123` | Quản lý người dùng, Quản lý sản phẩm, Quản lý danh mục (`/admin/*`) |
| **Khách hàng 1** | `customer1@example.com` | `Customer@123` | Xem sản phẩm, giỏ hàng, đặt hàng, quản lý hồ sơ cá nhân |
| **Khách hàng 2** | `customer2@example.com` | `Customer@123` | Xem sản phẩm, giỏ hàng, đặt hàng, quản lý hồ sơ cá nhân |

---

## 2. Cách 1: Chạy trọn gói bằng Docker Compose (Khuyên dùng)

Nếu máy tính của bạn đã cài đặt **Docker Desktop**, đây là cách tiện lợi nhất vì Docker sẽ tự động dựng MySQL, nạp sẵn dữ liệu mẫu, dựng Redis, build Backend và Frontend.

### Bước 1: Tạo file `.env`
Mở terminal PowerShell tại thư mục gốc dự án:
```powershell
Copy-Item .env.example .env
```

### Bước 2: Khởi chạy Docker Compose
```powershell
docker compose up -d --build
```

### Bước 3: Truy cập ứng dụng
- **Frontend (Giao diện người dùng & Admin):** [http://localhost:3000](http://localhost:3000)
- **Backend API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **Kiểm tra sức khỏe Backend:** [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)

### Lệnh dừng hệ thống khi không dùng:
```powershell
docker compose down
```

---

## 3. Cách 2: Chạy từng dịch vụ trên máy Local

Dành cho quá trình phát triển (development), sửa code và xem cập nhật ngay (Hot Reload).

### 3.1 Khởi động Database & Redis

Bạn có 2 lựa chọn:

#### Lựa chọn A: Dùng Docker chỉ để chạy MySQL & Redis (Rất nhanh)
Tại thư mục gốc:
```powershell
docker compose up -d mysql redis
```
*(MySQL sẽ tự động import schema và dữ liệu mẫu từ thư mục `database/init/`)*.

#### Lựa chọn B: Sử dụng MySQL và Redis cài trực tiếp trên máy
1. Mở MySQL Workbench hoặc MySQL CLI:
   - Tạo database: `CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
   - Chạy script: `database/init/01-schema.sql`
   - Chạy script: `database/init/02-sample-data.sql`
2. Đảm bảo dịch vụ Redis đang chạy tại cổng `6379`.
3. Kiểm tra thông tin kết nối trong file `backend/src/main/resources/application.yml` (username: `root`, password: `password`).

---

### 3.2 Khởi động Backend (Spring Boot)

Mở một cửa sổ **PowerShell mới**:

```powershell
# 1. Điều hướng vào thư mục backend
cd d:\IT209\project-rikkei\Project-Rikkei-Ecommerce\backend

# 2. Thiết lập biến môi trường JDK 21 (sử dụng GraalVM JDK 21 có sẵn trên máy)
$env:JAVA_HOME = "C:\Users\HPC\.jdks\graalvm-jdk-21.0.7"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 3. Chạy ứng dụng Spring Boot
.\gradlew bootRun
```

Khi màn hình xuất hiện thông báo:
```text
Started EcommerceApplication in ... seconds
```
Backend đã sẵn sàng phục vụ tại: **`http://localhost:8080`**.

---

### 3.3 Khởi động Frontend (React / Vite)

Mở thêm một cửa sổ **PowerShell thứ hai**:

```powershell
# 1. Điều hướng vào thư mục frontend
cd d:\IT209\project-rikkei\Project-Rikkei-Ecommerce\frontend

# 2. Cài đặt thư viện phụ thuộc (nếu chưa cài)
npm install

# 3. Khởi chạy dev server
npm run dev
```

Khi chạy thành công, Vite sẽ cung cấp đường dẫn:
```text
  VITE v5.4.21  ready in ... ms

  ➜  Local:   http://localhost:5173/
```

Mở trình duyệt và truy cập: **[http://localhost:5173](http://localhost:5173)**.

---

## 4. Kiểm tra các chức năng chính

### Phía Khách hàng / Khách vãng lai:
- **Trang chủ:** `http://localhost:5173/` — Xem tổng quan và bấm nút "Khám phá sản phẩm".
- **Danh sách sản phẩm & Tìm kiếm:** `http://localhost:5173/products`
  - Tìm kiếm sản phẩm theo từ khóa (ví dụ: `áo`, `tai nghe`).
  - Lọc theo danh mục (Thời trang, Điện tử, Gia dụng, Sách, Thể thao).
  - Sắp xếp theo giá (Tăng/Giảm) hoặc theo thời gian mới nhất.
  - Phân trang dữ liệu.
- **Chi tiết sản phẩm:** `http://localhost:5173/products/1` — Xem ảnh, mô tả, số lượng tồn kho `stockQuantity`, tăng giảm số lượng mua.
- **Đăng nhập:** `http://localhost:5173/login`
- **Hồ sơ cá nhân:** `http://localhost:5173/profile`

### Phía Quản trị viên (Admin):
1. Đăng nhập bằng tài khoản: `admin@eshop.com` / `Admin@123`.
2. Truy cập trang quản trị: `http://localhost:5173/admin/dashboard`
3. **Quản lý sản phẩm (`/admin/products`):**
   - Xem danh sách sản phẩm và số lượng tồn kho `stockQuantity`.
   - Bấm nút **"Thêm sản phẩm"** để mở form tạo sản phẩm mới.
   - Bấm nút **"Sửa"** để cập nhật giá, tên, danh mục, số lượng kho.
   - Bấm nút **"Ẩn"** để xóa mềm sản phẩm.
4. **Quản lý danh mục (`/admin/categories`):**
   - Thêm danh mục mới, chỉnh sửa hoặc ẩn/xóa danh mục.
5. **Quản lý người dùng (`/admin/users`):**
   - Quản lý danh sách tài khoản, khóa và mở khóa tài khoản người dùng.

---

## 5. Các lỗi thường gặp và cách xử lý

### 1. `ERROR: JAVA_HOME is not set` khi chạy gradlew
Chạy 2 dòng sau trong PowerShell trước khi gõ `.\gradlew`:
```powershell
$env:JAVA_HOME = "C:\Users\HPC\.jdks\graalvm-jdk-21.0.7"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 2. Lỗi kết nối Redis (`RedisConnectionFailureException`)
Nếu không chạy Redis, các tính năng liên quan đến refresh token hoặc session có thể báo lỗi. Hãy bật Redis container bằng lệnh:
```powershell
docker compose up -d redis
```

### 3. Lỗi `vite is not recognized` ở frontend
Chạy lệnh `npm install` bên trong thư mục `frontend` trước khi chạy `npm run dev`.
