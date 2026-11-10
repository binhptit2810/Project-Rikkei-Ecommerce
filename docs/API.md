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
Sau khi đăng ký thành công, hệ thống sẽ tự động tạo tài khoản ở trạng thái chưa kích hoạt (`emailVerified = false`), tạo mã OTP 6 chữ số (hiệu lực 300 giây) và gửi qua Resend SMTP. Hệ thống **không** tự động cấp JWT token hay đăng nhập người dùng.

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
  "message": "Đăng ký tài khoản thành công. Vui lòng kiểm tra email để nhận mã OTP kích hoạt tài khoản.",
  "data": {
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "emailVerified": false
  },
  "timestamp": "2026-11-07T03:00:00"
}
```

**Lỗi 409** — Email đã tồn tại hoặc đang chờ xác thực:
```json
{
  "success": false,
  "message": "Email đã được sử dụng. Vui lòng sử dụng email khác.",
  "data": null,
  "timestamp": "2026-11-07T03:00:00"
}
```

---

### `POST /api/v1/auth/verify-email`
Xác thực email bằng mã OTP 6 chữ số gửi qua email. **Không cần xác thực.**
Khi OTP hợp lệ, tài khoản được kích hoạt (`emailVerified = true`, `emailVerifiedAt = now()`), đồng thời cấp Access Token và Refresh Token để đăng nhập ngay vào hệ thống.

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Xác thực email thành công! Tài khoản của bạn đã được kích hoạt.",
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
      "emailVerified": true,
      "emailVerifiedAt": "2026-11-07T03:05:00",
      "createdAt": "2026-11-07T03:00:00"
    }
  },
  "timestamp": "2026-11-07T03:05:00"
}
```
*Lưu ý:* Hệ thống đồng thời đính kèm cookie HttpOnly `refresh_token` trong header `Set-Cookie`.

**Lỗi 400** — OTP sai, hết hạn hoặc vượt quá số lần thử:
```json
{
  "success": false,
  "message": "Mã OTP không chính xác. Bạn còn 4 lần thử.",
  "data": null,
  "timestamp": "2026-11-07T03:05:00"
}
```

---

### `POST /api/v1/auth/resend-verification`
Gửi lại mã OTP xác thực qua email cho tài khoản chưa kích hoạt. **Không cần xác thực.**
Áp dụng cơ chế Rate Limiting: Giới hạn tối thiểu cách nhau 60 giây giữa các lần gửi.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Mã OTP xác thực mới đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.",
  "data": null,
  "timestamp": "2026-11-07T03:06:00"
}
```
*Lưu ý bảo mật (Anti-enumeration):* Nếu email chưa từng đăng ký hoặc đã được xác thực thành công từ trước, endpoint vẫn trả về `200 OK` giả lập để bảo vệ quyền riêng tư người dùng.

**Lỗi 429** — Yêu cầu gửi lại quá nhanh (Rate limited):
```json
{
  "success": false,
  "message": "Vui lòng đợi 45 giây trước khi yêu cầu gửi lại mã xác thực mới.",
  "data": null,
  "timestamp": "2026-11-07T03:06:15"
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
  "message": "Tài khoản của bạn đã bị khóa.",
  "data": {
    "reason": "Lý do khóa từ quản trị viên"
  },
  "timestamp": "2026-11-07T03:00:00"
}
```

**Lỗi 403** — Tài khoản chưa xác thực email (`ACCOUNT_NOT_VERIFIED`):
```json
{
  "success": false,
  "message": "Tài khoản của bạn chưa được xác thực email. Vui lòng xác thực email để kích hoạt tài khoản.",
  "data": {
    "errorCode": "ACCOUNT_NOT_VERIFIED",
    "email": "user@example.com"
  },
  "timestamp": "2026-11-07T03:00:00"
}
```

---

### `POST /api/v1/auth/forgot-password`
Yêu cầu mã OTP đặt lại mật khẩu qua email. **Không cần xác thực.**
(Task: **TSK-D10-CCT** - Quên mật khẩu & Xác thực OTP qua Redis/Email)

**Nguyên tắc bảo mật:**
- Mã OTP gồm 6 chữ số ngẫu nhiên an toàn, có hiệu lực trong **300 giây (5 phút)**.
- Không lưu OTP dạng plaintext trong Redis mà được bảo vệ bằng mã băm **HMAC-SHA256**.
- **Rate Limit:** Giới hạn tối thiểu **60 giây** giữa hai lần yêu cầu gửi OTP.
- **User Enumeration Protection:** Phản hồi thông báo chung đồng nhất cho cả trường hợp email không tồn tại hoặc tài khoản bị khóa để chống lộ danh sách tài khoản.

**Request:**
```json
{
  "email": "customer@gmail.com"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Nếu email tồn tại trong hệ thống và hợp lệ, bạn sẽ nhận được mã OTP qua email.",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 400 — Validation hoặc Rate Limit:**
```json
{
  "success": false,
  "message": "Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

---

### `POST /api/v1/auth/reset-password`
Xác thực OTP và đặt lại mật khẩu mới. **Không cần xác thực.**
(Task: **TSK-D10-CCT** - Quên mật khẩu & Xác thực OTP qua Redis/Email)

**Nguyên tắc bảo mật:**
- Thao tác xác thực OTP nguyên tử (Atomic Verify & Claim) qua Redis Lua Script: Chống race condition khi có nhiều request đồng thời và đảm bảo OTP **chỉ sử dụng được 1 lần**.
- **Giới hạn số lần thử (Anti-Brute Force):** Tối đa **5 lần nhập sai**. Quá 5 lần, mã OTP sẽ bị hủy tự động trên Redis.
- Mật khẩu mới được băm bằng `BCryptPasswordEncoder(12)`.
- Không cho phép đặt mật khẩu mới trùng mật khẩu hiện tại.
- Sau khi đặt lại mật khẩu thành công, toàn bộ phiên Refresh Token của tài khoản bị thu hồi ngay lập tức (`RefreshTokenService.revokeAllUserSessions`).

**Request:**
```json
{
  "email": "customer@gmail.com",
  "otp": "012345",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại với mật khẩu mới.",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 400 — OTP không hợp lệ, hết hạn, hoặc đã sử dụng:**
```json
{
  "success": false,
  "message": "Mã OTP không hợp lệ hoặc đã hết hạn",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 400 — Sai OTP (kèm số lần thử còn lại):**
```json
{
  "success": false,
  "message": "Mã OTP không chính xác. Bạn còn 4 lần thử.",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 400 — Vượt quá 5 lần nhập sai:**
```json
{
  "success": false,
  "message": "Bạn đã nhập sai mã OTP quá 5 lần. Mã OTP đã bị hủy, vui lòng yêu cầu mã mới.",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 400 — Mật khẩu xác nhận không khớp:**
```json
{
  "success": false,
  "message": "Mật khẩu mới và xác nhận mật khẩu không khớp",
  "data": null,
  "timestamp": "2026-11-06T20:00:00"
}
```

**Lỗi 403 — Tài khoản bị khóa:**
```json
{
  "success": false,
  "message": "Tài khoản của bạn đã bị khóa.",
  "data": {
    "reason": "Lý do khóa tài khoản"
  },
  "timestamp": "2026-11-06T20:00:00"
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

## 🛒 Orders (Vũ Đức Huy Hoàng)

> **Đã triển khai hoàn chỉnh.** Tích hợp kiểm tra tồn kho, trừ kho và hoàn kho trong `@Transactional`.

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/api/v1/orders` | CUSTOMER, ADMIN | Tạo đơn hàng từ giỏ hàng (tính totalAmount tại Backend, trừ kho) |
| GET | `/api/v1/orders/my` | CUSTOMER, ADMIN | Lịch sử đơn hàng của tôi |
| GET | `/api/v1/orders/{id}` | CUSTOMER, ADMIN | Chi tiết đơn hàng |
| PATCH | `/api/v1/orders/{id}/cancel` | CUSTOMER, ADMIN | Hủy đơn hàng (hoàn kho, hoàn tiền nếu thanh toán trực tuyến) |
| GET | `/api/v1/admin/orders` | ADMIN | Danh sách tất cả đơn hàng |
| PATCH | `/api/v1/admin/orders/{id}/status` | ADMIN | Cập nhật trạng thái đơn hàng |

**Request tạo đơn hàng (`POST /api/v1/orders`):**
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

**Response 201:**
```json
{
  "success": true,
  "message": "Đặt hàng thành công",
  "data": {
    "id": 1,
    "orderCode": "ORD-20261022-8142",
    "userId": 2,
    "userEmail": "customer1@example.com",
    "recipientName": "Nguyễn Văn A",
    "recipientPhone": "0912345678",
    "shippingAddress": "123 Đường ABC, TP.HCM",
    "totalAmount": 1598000.00,
    "orderStatus": "PENDING",
    "orderStatusDescription": "Chờ xác nhận",
    "paymentStatus": "UNPAID",
    "paymentStatusDescription": "Chưa thanh toán",
    "paymentMethod": "COD",
    "paymentMethodDescription": "Thanh toán khi nhận hàng (COD)",
    "note": "Giao giờ hành chính",
    "createdAt": "2026-10-22T10:30:00",
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "Áo thun nam basic Cotton",
        "unitPrice": 199000.00,
        "quantity": 2,
        "subtotal": 398000.00,
        "imageUrl": "https://..."
      }
    ]
  }
}
```

---

## 💳 Payments (Đặng Thanh Bình)

> Module thanh toán mô phỏng (COD và SIMULATED). Mọi giao dịch đều được cấp mã giao dịch duy nhất định dạng `PAY-YYYYMMDD-XXXXXXXX`.

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| POST | `/api/v1/payments/{orderId}/pay` | CUSTOMER, ADMIN | Xử lý thanh toán đơn hàng (COD hoặc SIMULATED) |
| GET | `/api/v1/payments/order/{orderId}` | CUSTOMER, ADMIN | Tra cứu thông tin thanh toán theo ID đơn hàng |
| GET | `/api/v1/payments/transaction/{transactionCode}` | CUSTOMER, ADMIN | Tra cứu thông tin thanh toán theo mã giao dịch PAY-* |

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

## 🤖 Chatbot AI Tư Vấn Bán Hàng (TSK-D11-CCT)

Dịch vụ Chatbot AI tư vấn bán hàng thông minh dựa trên **Spring AI**, mô hình ngôn ngữ **Google Gemini**, **Redis Chat Memory** phân tán và cơ chế **RAG Function Calling (Product / Inventory / Order Tools)**.

### Cơ Chế Xác Thực & Phân Quyền Chatbot

| Đối tượng | Header Xác thực | Cơ chế & Quyền hạn |
|-----------|-----------------|-------------------|
| **Khách vãng lai (Guest)** | `X-Guest-Token: <guestToken>` | Tạo phiên bằng `POST /chat/sessions`. Nhận `sessionId` (UUIDv4) và `guestToken`. Chatbot chỉ truy cập thông tin sản phẩm và tồn kho công khai. TTL lịch sử: 24h. |
| **Người dùng (Customer)** | `Authorization: Bearer <JWT>` | Không cần truyền `X-Guest-Token`. Chatbot nhận diện `userId` từ SecurityContext, có quyền tra cứu đơn hàng cá nhân. TTL lịch sử: 7 ngày. |
| **Khách chuyển sang Đăng nhập** | `Authorization: Bearer <JWT>` | Sau khi đăng nhập, gọi `POST /chat/claim` gửi kèm `sessionId` và `guestToken` để chuyển giao quyền sở hữu phiên chat khách vào tài khoản người dùng mà không mất lịch sử. |

---

### `POST /api/v1/chat/sessions`
Khởi tạo phiên trò chuyện mới cho khách vãng lai (Guest).

**Không yêu cầu xác thực.** Rate limit: 10 lần tạo phiên/phút theo IP.

**Request Body:** Trống

**Response 201 Created:**
```json
{
  "success": true,
  "message": "Tạo phiên trò chuyện khách thành công",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "guestToken": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    "expiresAt": "2026-11-11T09:30:00"
  },
  "timestamp": "2026-11-10T09:30:00"
}
```

> [!IMPORTANT]
> Client cần lưu trữ `sessionId` và `guestToken` (LocalStorage/Cookie). `guestToken` chỉ được trả về **duy nhất 1 lần** tại endpoint này (trong Redis chỉ lưu hash SHA-256).

---

### `POST /api/v1/chat/messages` hoặc `POST /api/v1/chat`
Gửi tin nhắn tới chatbot AI tư vấn bán hàng. Hệ thống hỗ trợ song song cả hai đường dẫn:
- `POST /api/v1/chat/messages`: Endpoint chuẩn frontend React.
- `POST /api/v1/chat`: Endpoint tương thích chuẩn RESTful theo tài liệu đặc tả ban đầu.

**Headers:**
- Khách vãng lai: `X-Guest-Token: <guestToken>`
- Người dùng đã đăng nhập: `Authorization: Bearer <jwtAccessToken>`

**Request Body:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Tư vấn cho mình đôi giày thể thao nam chạy bộ màu đen giá dưới 2 triệu",
  "clientMessageId": "msg-uuid-client-001"
}
```

**Tham số:**
- `sessionId` (String, Bắt buộc): UUID phiên trò chuyện.
- `message` (String, Bắt buộc): Nội dung tin nhắn (1 - 1000 ký tự).
- `clientMessageId` (String, Tùy chọn): ID chống gửi trùng (Idempotency Key).

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Gửi tin nhắn thành công",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "Dạ chào bạn! Shop xin gợi ý mẫu giày Nike Pegasus 40 màu đen rất phù hợp chạy bộ với đệm êm ái, hiện đang có sẵn size 40 đến 43 với giá 1.850.000 VNĐ ạ.",
    "products": [
      {
        "id": 12,
        "name": "Giày Chạy Bộ Nam Nike Air Zoom Pegasus 40 - Đen",
        "slug": "giay-chay-bo-nam-nike-air-zoom-pegasus-40-den",
        "price": 1850000,
        "salePrice": 1690000,
        "thumbnail": "https://example.com/images/pegasus-40-black.jpg",
        "inStock": true,
        "rating": 4.8,
        "reviewCount": 124
      }
    ],
    "version": 2,
    "clientMessageId": "msg-uuid-client-001"
  },
  "timestamp": "2026-11-10T09:30:15"
}
```

**Thành phần Product Cards trong response:**
Khi AI thực hiện Tool Calling truy vấn sản phẩm trong cơ sở dữ liệu, danh sách các sản phẩm liên quan sẽ được tự động đóng gói trong mảng `products` để frontend render thẻ sản phẩm (Product Card) tương tác trực quan (hình ảnh, giá bán, nút xem chi tiết / thêm giỏ hàng).

---

### `POST /api/v1/chat/claim`
Chuyển quyền sở hữu phiên trò chuyện từ khách vãng lai sang người dùng vừa đăng nhập.

**Yêu cầu xác thực:** `Authorization: Bearer <jwtAccessToken>`

**Request Body:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "guestToken": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
}
```

**Response 200 OK:**
```json
{
  "success": true,
  "message": "Nhận quyền sở hữu phiên trò chuyện thành công",
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "ownerId": "1001",
    "version": 3,
    "expiresAt": "2026-11-17T09:30:00"
  },
  "timestamp": "2026-11-10T09:35:00"
}
```

> [!NOTE]
> Sau khi claim thành công, session chuyển từ `owner: GUEST` sang `owner: <userId>`, TTL phiên được tự động nâng từ **24 giờ** lên **7 ngày**.

---

### Giới Hạn & Độ Tin Cậy (Rate Limiting & Memory)

1. **Rate Limiting Phân Tán (Redis Sliding Window Counter):**
   - Giới hạn: **10 requests / 60 giây** cho mỗi Client IP (khách vãng lai) hoặc User ID (người dùng).
   - Khi vượt ngưỡng: Trả mã HTTP `429 Too Many Requests` kèm header chuẩn `Retry-After: <số giây cần đợi>`.
2. **Quản lý Bộ Nhớ Hội Thoại (Redis Chat Memory):**
   - Giữ tối đa **20 tin nhắn** (10 cặp User - Assistant) gần nhất theo cơ chế Sliding Window FIFO.
   - Cơ chế khóa phân tán Redis Distributed Lock (TTL 10s) chống race condition khi client gửi đồng thời.
   - Kiểm soát đồng thời lạc quan (Optimistic Concurrency Control - OCC) qua trường `version` ngăn chặn ghi đè dữ liệu.
3. **Idempotency (Chống gửi trùng):**
   - Hỗ trợ trường `clientMessageId`. Trong vòng 60 giây, nếu gửi lại cùng messageId trên phiên đó, hệ thống lập tức trả lại response trước đó mà không gọi lại LLM.

---

### Bảng Mã Lỗi Chatbot API

| Mã HTTP | Tình huống | Response mẫu |
|---------|-----------|--------------|
| **400 Bad Request** | Message trống, vượt quá 1000 ký tự, hoặc Session ID sai UUID | `{"success": false, "message": "Nội dung tin nhắn không được để trống"}` |
| **401 Unauthorized** | Khách vãng lai thiếu hoặc sai `X-Guest-Token`, hoặc Bearer JWT không hợp lệ | `{"success": false, "message": "Guest token bắt buộc đối với khách vãng lai (truyền qua header X-Guest-Token)"}` |
| **403 Forbidden** | Gọi `POST /chat/claim` khi chưa đăng nhập, hoặc cố truy cập phiên của người khác | `{"success": false, "message": "Yêu cầu đăng nhập để nhận quyền sở hữu phiên trò chuyện"}` |
| **404 Not Found** | Session ID không tồn tại hoặc đã hết hạn trong Redis | `{"success": false, "message": "Phiên trò chuyện không tồn tại hoặc đã hết hạn"}` |
| **409 Conflict** | Xung đột gửi đồng thời (Lock Contention) hoặc xung đột version OCC | `{"success": false, "message": "Phiên trò chuyện đang xử lý tin nhắn trước, vui lòng chờ giây lát"}` |
| **429 Too Many Requests** | Gửi quá 10 tin nhắn/phút (kèm header `Retry-After`) | `{"success": false, "message": "Bạn đã gửi quá nhiều tin nhắn, vui lòng chờ 45 giây"}` |
| **503 Service Unavailable** | Lỗi dịch vụ Gemini AI upstream (5xx hoặc quota) | `{"success": false, "message": "Dịch vụ tư vấn AI tạm thời không khả dụng, vui lòng thử lại sau"}` |
| **504 Gateway Timeout** | Dịch vụ Gemini AI phản hồi quá thời gian cho phép | `{"success": false, "message": "Dịch vụ tư vấn AI phản hồi quá thời gian, vui lòng thử lại sau"}` |

---

## Mã lỗi thường gặp

| HTTP Status | Ý nghĩa |
|-------------|---------|
| 400 | Request không hợp lệ (thiếu field, validation lỗi) |
| 401 | Chưa đăng nhập hoặc token hết hạn |
| 403 | Không đủ quyền truy cập |
| 404 | Không tìm thấy resource |
| 409 | Xung đột tài nguyên hoặc phiên bản dữ liệu (Conflict / OCC) |
| 429 | Vượt quá giới hạn tần suất yêu cầu (Too Many Requests) |
| 500 | Lỗi server nội bộ |
| 503 | Dịch vụ bên thứ ba tạm thời không khả dụng (Service Unavailable) |
| 504 | Hết thời gian chờ phản hồi từ dịch vụ bên thứ ba (Gateway Timeout) |

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
