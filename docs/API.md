# 📡 API Reference — E-commerce Platform

Base URL: `http://localhost:8080/api/v1`

Tất cả response theo cấu trúc chuẩn:

```json
{
  "success": true,
  "message": "Mô tả kết quả",
  "data": { },
  "timestamp": "2024-10-15T10:30:00"
}
```

---

## Quy ước chung

| Quy tắc | Mô tả |
|---------|-------|
| **Authentication** | Bearer Token trong header `Authorization: Bearer <token>` |
| **Content-Type** | `application/json` |
| **Phân trang** | `?page=0&size=12&sortBy=createdAt&sortDir=desc` |
| **Tìm kiếm** | `?keyword=<từ khoá>&categoryId=<id>` |
| **HTTP Status** | 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Server Error |

### Phân trang — Response format

```json
{
  "success": true,
  "data": {
    "content": [ ],
    "pageNumber": 0,
    "pageSize": 12,
    "totalElements": 50,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 🔍 Health Check

### `GET /api/v1/health`
Kiểm tra backend đang hoạt động.

**Không cần xác thực.**

**Response 200:**
```json
{
  "success": true,
  "message": "Backend đang hoạt động bình thường",
  "data": {
    "status": "UP",
    "service": "E-commerce API",
    "version": "1.0.0"
  }
}
```

### `GET /actuator/health`
Spring Boot Actuator health endpoint.

---

## 🔐 Authentication

### `POST /api/v1/auth/register`
Đăng ký tài khoản mới. **Không cần xác thực.**

**Request:**
```json
{
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "password": "Password@123",
  "phone": "0912345678",
  "address": "123 Đường ABC, TP.HCM"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 3,
      "email": "user@example.com",
      "fullName": "Nguyễn Văn A",
      "phone": "0912345678",
      "address": "123 Đường ABC, TP.HCM",
      "role": "CUSTOMER",
      "active": true,
      "createdAt": "2024-10-15T10:00:00"
    }
  },
  "timestamp": "2024-10-15T10:00:00"
}
```
*Lưu ý:* `expiresIn` tính bằng **giây** (86400 giây = 24 giờ).

**Lỗi 400** — Email đã tồn tại hoặc validation lỗi:
```json
{
  "success": false,
  "message": "Email đã được sử dụng: user@example.com",
  "data": null,
  "timestamp": "2024-10-15T10:00:00"
}
```

---

### `POST /api/v1/auth/login`
Đăng nhập. **Không cần xác thực.**

**Request:**
```json
{
  "email": "user@example.com",
  "password": "Password@123"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "Nguyễn Văn A",
      "phone": "0912345678",
      "address": "123 Đường ABC, TP.HCM",
      "role": "CUSTOMER",
      "active": true,
      "createdAt": "2024-10-15T10:00:00"
    }
  },
  "timestamp": "2024-10-15T10:00:00"
}
```

**Lỗi 401** — Sai email hoặc mật khẩu:
```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không đúng",
  "data": null,
  "timestamp": "2024-10-15T10:00:00"
}
```

**Lỗi 403** — Tài khoản bị vô hiệu hóa (`active = false`):
```json
{
  "success": false,
  "message": "Tài khoản đã bị vô hiệu hóa",
  "data": null,
  "timestamp": "2024-10-15T10:00:00"
}
```

---

## 📦 Products

### `GET /api/v1/products`
Danh sách sản phẩm với tìm kiếm và lọc. **Không cần xác thực.**

**Query params:**
| Tham số | Kiểu | Mô tả |
|---------|------|-------|
| `keyword` | string | Tìm theo tên (tuỳ chọn) |
| `categoryId` | number | Lọc theo danh mục (tuỳ chọn) |
| `page` | number | Số trang (mặc định: 0) |
| `size` | number | Số phần tử/trang (mặc định: 12) |
| `sortBy` | string | Sắp xếp theo: `createdAt`, `price`, `name` |
| `sortDir` | string | `asc` hoặc `desc` |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Áo thun nam basic Cotton",
        "description": "...",
        "price": 199000,
        "imageUrl": null,
        "categoryId": 1,
        "categoryName": "Thời trang",
        "stockQuantity": 50,
        "active": true,
        "createdAt": "2024-10-15T10:00:00"
      }
    ],
    "totalElements": 14,
    "totalPages": 2
  }
}
```

---

### `GET /api/v1/products/{id}`
Chi tiết sản phẩm. **Không cần xác thực.**

**Response 200:** _(1 object ProductResponse)_

**Lỗi 404:**
```json
{ "success": false, "message": "Sản phẩm không tìm thấy với id: 99" }
```

---

## 🏷️ Categories

### `GET /api/v1/categories`
Danh sách danh mục active. **Không cần xác thực.**

**Response 200:**
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "Thời trang", "description": "...", "active": true },
    { "id": 2, "name": "Điện tử", "description": "...", "active": true }
  ]
}
```

---

## 🛒 Orders (TODO — Vũ Đức Huy Hoàng)

> **Chưa triển khai.** Sẽ bổ sung khi module Order được phát triển.

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/api/v1/orders` | CUSTOMER | Tạo đơn hàng từ giỏ hàng |
| GET | `/api/v1/orders/my` | CUSTOMER | Lịch sử đơn hàng của tôi |
| GET | `/api/v1/orders/{id}` | CUSTOMER | Chi tiết đơn hàng |
| PATCH | `/api/v1/orders/{id}/cancel` | CUSTOMER | Hủy đơn hàng |

**Request tạo đơn hàng:**
```json
{
  "recipientName": "Nguyễn Văn A",
  "recipientPhone": "0912345678",
  "shippingAddress": "123 Đường ABC, TP.HCM",
  "paymentMethod": "COD",
  "note": "Giao giờ hành chính",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 5, "quantity": 1 }
  ]
}
```

---

## 💳 Payments (TODO — Đặng Thanh Bình)

> **Chưa triển khai.** Chỉ mô phỏng, không tích hợp cổng thanh toán thật.

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/api/v1/payments/order/{orderId}` | CUSTOMER | Xử lý thanh toán mô phỏng |

---

## 👤 Users / Profile (Cao Chí Thiện)

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/v1/users/me` | CUSTOMER, ADMIN | Lấy thông tin tài khoản hiện tại |
| PUT | `/api/v1/users/me` | CUSTOMER, ADMIN | Cập nhật hồ sơ (partial update) |
| PUT | `/api/v1/users/me/password` | CUSTOMER, ADMIN | Đổi mật khẩu |

### `GET /api/v1/users/me`
Lấy thông tin tài khoản người dùng đang đăng nhập.

**Header:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0912345678",
    "address": "123 Đường ABC, TP.HCM",
    "role": "CUSTOMER",
    "active": true,
    "createdAt": "2024-10-15T10:00:00"
  },
  "timestamp": "2024-10-15T10:00:00"
}
```

### `PUT /api/v1/users/me`
Cập nhật thông tin hồ sơ (chỉ cập nhật trường không null).

**Header:** `Authorization: Bearer <accessToken>`

**Request:**
```json
{
  "fullName": "Nguyễn Văn B",
  "phone": "0987654321",
  "address": "456 Đường XYZ, Hà Nội"
}
```

**Response 200:** Cùng format với `GET /api/v1/users/me` kèm message `"Cập nhật hồ sơ thành công"`.

### `PUT /api/v1/users/me/password`
Đổi mật khẩu người dùng đang đăng nhập.

**Header:** `Authorization: Bearer <accessToken>`

**Request:**
```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewPassword@456",
  "confirmPassword": "NewPassword@456"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công",
  "data": null,
  "timestamp": "2024-10-15T10:00:00"
}
```

**Lỗi 400:** Mật khẩu cũ sai, hoặc xác nhận mật khẩu mới không khớp, hoặc mật khẩu mới trùng mật khẩu cũ:
```json
{
  "success": false,
  "message": "Mật khẩu hiện tại không chính xác",
  "data": null,
  "timestamp": "2024-10-15T10:00:00"
}
```

---

## 🔧 Admin APIs (TODO — Đỗ Đức Mạnh / Đặng Thanh Bình)

> Tất cả admin API yêu cầu role **ADMIN**.

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/v1/admin/products` | Danh sách sản phẩm (admin) |
| POST | `/api/v1/admin/products` | Thêm sản phẩm |
| PUT | `/api/v1/admin/products/{id}` | Sửa sản phẩm |
| DELETE | `/api/v1/admin/products/{id}` | Xóa sản phẩm |
| POST | `/api/v1/admin/categories` | Thêm danh mục |
| PUT | `/api/v1/admin/categories/{id}` | Sửa danh mục |
| GET | `/api/v1/admin/orders` | Tất cả đơn hàng |
| PATCH | `/api/v1/admin/orders/{id}/status` | Cập nhật trạng thái đơn |
| POST | `/api/v1/admin/inventory/{productId}/adjust` | Điều chỉnh tồn kho |
| GET | `/api/v1/admin/users` | Danh sách người dùng (phân trang PageResponse) |
| PATCH | `/api/v1/admin/users/{id}/lock` | Khóa tài khoản người dùng kèm lý do (BR-AUTH-04) |
| PATCH | `/api/v1/admin/users/{id}/unlock` | Mở khóa tài khoản người dùng |

### `GET /api/v1/admin/users`
Lấy danh sách người dùng trong hệ thống có phân trang. **Yêu cầu role ADMIN.**

**Header:** `Authorization: Bearer <accessToken>`

**Query params:**
| Tham số | Kiểu | Mặc định | Giới hạn | Mô tả |
|---------|------|----------|----------|-------|
| `page` | number | 0 | >= 0 | Số trang (0-indexed) |
| `size` | number | 10 | 1 - 100 | Số phần tử/trang (tối đa 100) |

**Response 200:**
```json
{
  "success": true,
  "message": null,
  "data": {
    "content": [
      {
        "id": 2,
        "email": "customer1@example.com",
        "fullName": "Nguyễn Văn A",
        "phone": "0912345678",
        "address": "123 Đường ABC, TP.HCM",
        "role": "CUSTOMER",
        "active": true,
        "lockReason": null,
        "lockedAt": null,
        "createdAt": "2024-10-15T10:00:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2024-10-15T10:30:00"
}
```

### `PATCH /api/v1/admin/users/{id}/lock`
Khóa tài khoản người dùng kèm lý do bắt buộc. **Yêu cầu role ADMIN.**
Áp dụng quy tắc **BR-AUTH-04**: Admin không thể tự khóa tài khoản của chính mình.

**Header:** `Authorization: Bearer <accessToken>`

**Request:**
```json
{
  "reason": "Tài khoản có dấu hiệu gian lận thanh toán"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Khóa tài khoản thành công",
  "data": {
    "id": 2,
    "email": "customer1@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0912345678",
    "address": "123 Đường ABC, TP.HCM",
    "role": "CUSTOMER",
    "active": false,
    "lockReason": "Tài khoản có dấu hiệu gian lận thanh toán",
    "lockedAt": "2024-10-15T10:30:00",
    "createdAt": "2024-10-15T10:00:00"
  },
  "timestamp": "2024-10-15T10:30:00"
}
```

**Lỗi 400 (BR-AUTH-04):** Admin tự khóa chính mình:
```json
{
  "success": false,
  "message": "Bạn không thể tự khóa tài khoản của chính mình",
  "data": null,
  "timestamp": "2024-10-15T10:30:00"
}
```

**Lỗi 404:** Không tìm thấy người dùng với ID tương ứng:
```json
{
  "success": false,
  "message": "Người dùng không tồn tại với ID: 999",
  "data": null,
  "timestamp": "2024-10-15T10:30:00"
}
```

### `PATCH /api/v1/admin/users/{id}/unlock`
Mở khóa tài khoản người dùng đã bị khóa trước đó. **Yêu cầu role ADMIN.**

**Header:** `Authorization: Bearer <accessToken>`

**Response 200:**
```json
{
  "success": true,
  "message": "Mở khóa tài khoản thành công",
  "data": {
    "id": 2,
    "email": "customer1@example.com",
    "fullName": "Nguyễn Văn A",
    "phone": "0912345678",
    "address": "123 Đường ABC, TP.HCM",
    "role": "CUSTOMER",
    "active": true,
    "lockReason": null,
    "lockedAt": null,
    "createdAt": "2024-10-15T10:00:00"
  },
  "timestamp": "2024-10-15T10:30:00"
}
```

**Lỗi 400:** Tài khoản đang hoạt động bình thường, không cần mở khóa:
```json
{
  "success": false,
  "message": "Tài khoản này đang hoạt động bình thường, không cần mở khóa",
  "data": null,
  "timestamp": "2024-10-15T10:30:00"
}
```

---

## Mã lỗi thường gặp

| HTTP Status | Ý nghĩa |
|-------------|---------|
| 400 | Request không hợp lệ (thiếu field, validation lỗi) |
| 401 | Chưa đăng nhập hoặc token hết hạn |
| 403 | Không đủ quyền truy cập |
| 404 | Không tìm thấy resource |
| 500 | Lỗi server |

**Validation error 400:**
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "data": {
    "email": "Email không đúng định dạng",
    "password": "Mật khẩu phải từ 6 đến 100 ký tự"
  }
}
```
