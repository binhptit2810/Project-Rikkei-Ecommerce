# 📋 Phân chia công việc nhóm — E-commerce Platform

> Tài liệu này mô tả phân công công việc ban đầu cho nhóm 5 thành viên. Có thể điều chỉnh khi cần cân bằng khối lượng.

---

## Tổng quan phân công

| STT | Thành viên | Vai trò chính | Module phụ trách |
|-----|-----------|--------------|-----------------|
| 1 | **Cao Chí Thiện** | Backend + Frontend | Auth, User Profile, Tích hợp |
| 2 | **Đỗ Đức Mạnh** | Backend + Frontend | Product, Category, Search |
| 3 | **Vũ Đức Huy Hoàng** | Backend + Frontend | Cart, Order, Checkout |
| 4 | **Đặng Thanh Bình** | Backend + Frontend | Inventory, Payment, Admin UI |
| 5 | **Đỗ Hoàng Anh** | Frontend + DevOps | Layout, Routing, Docker, CI/CD |

---

## Chi tiết phân công

---

### 👤 Thành viên 1: Cao Chí Thiện — Backend Core / Auth

**Vai trò:** Quản lý kỹ thuật tổng thể, xây dựng lớp nền backend.

#### Module phụ trách

| Phần | File/Thư mục chính |
|------|-------------------|
| Auth & Security | `backend/src/.../security/`, `controller/AuthController.java` |
| User Profile | `backend/src/.../service/UserService.java` (TODO), `controller/UserController.java` (TODO) |
| Cấu hình chung | `application.yml`, `SecurityConfig.java`, `GlobalExceptionHandler.java` |
| DTO & API contract | `dto/request/`, `dto/response/` |

#### Công việc cụ thể

- [x] Cấu hình Spring Boot, Spring Security, JWT
- [x] Tạo entity User, Role, cơ chế phân quyền ADMIN/CUSTOMER
- [x] API đăng ký (`POST /auth/register`), đăng nhập (`POST /auth/login`)
- [ ] API hồ sơ người dùng (`GET /users/me`, `PUT /users/me`)
- [ ] API đổi mật khẩu (`PUT /users/me/password`)
- [ ] Thiết lập quy ước DTO chung và thông báo cho nhóm
- [ ] Review code của các thành viên khi merge PR
- [ ] Cập nhật tài liệu API khi có thay đổi

#### Sản phẩm bàn giao
- Backend auth hoàn chỉnh (đăng ký, đăng nhập, JWT)
- API profile người dùng
- Tài liệu API contract cho nhóm tham khảo

#### Tiêu chí hoàn thành
- [ ] `POST /auth/register` trả 201, lưu user với BCrypt password
- [ ] `POST /auth/login` trả token JWT hợp lệ
- [ ] Token bị từ chối sau khi hết hạn
- [ ] ADMIN không thể truy cập endpoint của CUSTOMER và ngược lại
- [ ] `GET /users/me` trả đúng thông tin user đang đăng nhập

#### Phối hợp
- Thống nhất format DTO với **Đỗ Đức Mạnh** (ProductResponse, CategoryResponse)
- Thống nhất format lỗi với **Vũ Đức Huy Hoàng** (OrderResponse)
- Hỗ trợ **Đỗ Hoàng Anh** cấu hình CORS đúng

---

### 📦 Thành viên 2: Đỗ Đức Mạnh — Product / Category / Search

**Vai trò:** Phát triển module sản phẩm và danh mục (đầy đủ backend + frontend).

#### Module phụ trách

| Phần | File/Thư mục chính |
|------|-------------------|
| Backend Product | `service/ProductService.java`, `controller/ProductController.java` |
| Backend Category | `service/CategoryService.java` (TODO), `controller/CategoryController.java` |
| Admin Product/Category | `controller/AdminProductController.java` (TODO) |
| Frontend | `pages/shop/`, `pages/admin/AdminProductsPage.jsx`, `AdminCategoriesPage.jsx` |

#### Công việc cụ thể

- [x] Entity Product, Category (đã tạo)
- [x] ProductRepository, CategoryRepository (đã tạo)
- [x] API `GET /products` — tìm kiếm và phân trang (đã tạo)
- [x] API `GET /products/{id}` — chi tiết sản phẩm (đã tạo)
- [ ] API Admin CRUD sản phẩm (`POST/PUT/DELETE /admin/products`)
- [ ] API Admin CRUD danh mục (`POST/PUT /admin/categories`)
- [ ] Validate dữ liệu sản phẩm (giá > 0, tên không trống)
- [ ] Frontend ProductListPage: tích hợp API thật (xoá mock data fallback)
- [ ] Frontend ProductDetailPage: tích hợp API thật
- [ ] Frontend AdminProductsPage: hoàn thiện form thêm/sửa
- [ ] Viết ít nhất 3 unit test cho ProductService

#### Sản phẩm bàn giao
- Toàn bộ CRUD sản phẩm và danh mục
- Frontend trang danh sách, chi tiết, admin quản lý

#### Tiêu chí hoàn thành
- [ ] `GET /products?keyword=áo&categoryId=1&page=0&size=6` trả đúng kết quả
- [ ] Thêm sản phẩm mới từ Admin UI → hiển thị ngay trong danh sách
- [ ] Xóa sản phẩm → không còn hiển thị ở trang shop
- [ ] Phân trang hoạt động đúng

#### Phối hợp
- Cần **Cao Chí Thiện** hoàn thiện auth trước (để test Admin API)
- Cần **Đặng Thanh Bình** tích hợp stockQuantity khi tạo/sửa sản phẩm

---

### 🛒 Thành viên 3: Vũ Đức Huy Hoàng — Cart / Order / Checkout

**Vai trò:** Phát triển luồng đặt hàng từ giỏ hàng đến đơn hàng.

#### Module phụ trách

| Phần | File/Thư mục chính |
|------|-------------------|
| Backend Order | `service/OrderService.java` (TODO), `controller/OrderController.java` (TODO) |
| Admin Order | `controller/AdminOrderController.java` (TODO) |
| Frontend | `pages/cart/`, `pages/checkout/`, `pages/orders/` |

#### Công việc cụ thể

- [x] Entity Order, OrderItem (đã tạo)
- [x] OrderRepository (đã tạo)
- [x] Frontend CartPage (đã tạo)
- [x] Frontend CheckoutPage (đã tạo — cần test với API thật)
- [x] Frontend OrderHistoryPage, OrderDetailPage (đã tạo)
- [ ] OrderService: tạo đơn hàng với kiểm tra tồn kho
- [ ] OrderService: hủy đơn hàng (với điều kiện PENDING/CONFIRMED)
- [ ] Đảm bảo tính nhất quán: trừ kho + tạo đơn trong 1 @Transactional
- [ ] API `POST /orders` — tạo đơn hàng
- [ ] API `GET /orders/my` — lịch sử đơn hàng
- [ ] API `GET /orders/{id}` — chi tiết đơn hàng
- [ ] API `PATCH /orders/{id}/cancel` — hủy đơn
- [ ] Validate: số lượng đặt không vượt tồn kho
- [ ] Tính totalAmount ở backend, không tin frontend

#### Sản phẩm bàn giao
- Luồng đặt hàng hoàn chỉnh từ checkout → tạo đơn → xem lịch sử
- Xử lý hủy đơn hàng

#### Tiêu chí hoàn thành
- [ ] Đặt hàng thành công → đơn có status PENDING, kho bị trừ đúng
- [ ] Đặt hàng khi hết hàng → trả lỗi 400 rõ ràng
- [ ] Hủy đơn PENDING → kho được hoàn lại
- [ ] Không thể hủy đơn đang SHIPPING

#### Phối hợp
- **Đặng Thanh Bình**: phối hợp trừ tồn kho trong OrderService (hoặc InventoryService)
- **Cao Chí Thiện**: dùng @PreAuthorize("hasRole('CUSTOMER')") cho API orders

---

### 🏭 Thành viên 4: Đặng Thanh Bình — Inventory / Payment / Admin Features

**Vai trò:** Quản lý tồn kho, thanh toán mô phỏng và chức năng admin.

#### Module phụ trách

| Phần | File/Thư mục chính |
|------|-------------------|
| Inventory | `service/InventoryService.java` (TODO), `controller/AdminInventoryController.java` (TODO) |
| Payment | `service/PaymentService.java` (TODO), `controller/PaymentController.java` (TODO) |
| Admin Order Status | Tích hợp vào OrderService |
| Frontend Admin | `pages/admin/AdminInventoryPage.jsx`, `AdminOrdersPage.jsx` |

#### Công việc cụ thể

- [x] Entity InventoryTransaction, Payment (đã tạo)
- [x] InventoryTransactionRepository, PaymentRepository (đã tạo)
- [x] Frontend AdminInventoryPage (đã tạo)
- [x] Frontend AdminOrdersPage (đã tạo)
- [ ] InventoryService: trừ kho khi đặt hàng, hoàn kho khi hủy
- [ ] InventoryService: admin điều chỉnh kho thủ công
- [ ] Đảm bảo không cho stockQuantity âm (constraint DB + logic)
- [ ] PaymentService: xử lý thanh toán mô phỏng (COD/SIMULATED)
- [ ] API Admin: `POST /admin/inventory/{id}/adjust`
- [ ] API Admin: `PATCH /admin/orders/{id}/status`
- [ ] Validate chuyển trạng thái đơn hàng (PENDING→CONFIRMED, không được bỏ bước)
- [ ] API thống kê cơ bản cho dashboard: tổng đơn, doanh thu (tuỳ chọn)

#### Sản phẩm bàn giao
- InventoryService hoàn chỉnh
- PaymentService mô phỏng
- Admin UI quản lý đơn hàng và tồn kho hoạt động với API thật

#### Tiêu chí hoàn thành
- [ ] Đặt hàng → stockQuantity giảm đúng số lượng
- [ ] Hủy đơn → stockQuantity hoàn về
- [ ] Không thể đặt hàng âm tồn kho (lỗi 400)
- [ ] Admin điều chỉnh kho → lưu lịch sử InventoryTransaction
- [ ] Thanh toán COD → paymentStatus = UNPAID, paymentMethod = COD
- [ ] Thanh toán SIMULATED → mô phỏng thành công/thất bại

#### Phối hợp
- **Vũ Đức Huy Hoàng**: tích hợp InventoryService vào OrderService
- **Cao Chí Thiện**: phân quyền admin endpoint

---

### 🎨 Thành viên 5: Đỗ Hoàng Anh — Frontend Integration / DevOps / Testing

**Vai trò:** Hoàn thiện frontend, thiết lập hạ tầng và kiểm thử.

#### Module phụ trách

| Phần | File/Thư mục chính |
|------|-------------------|
| Frontend Layout | `layouts/`, `components/layout/`, `routes/` |
| Auth Frontend | `pages/auth/`, `context/AuthContext.jsx` |
| Docker & CI/CD | `Dockerfile` (frontend/backend), `docker-compose.yml`, `.github/workflows/` |
| Testing | `docs/TEST-PLAN.md`, script kiểm thử |

#### Công việc cụ thể

- [x] Cấu hình Vite, Tailwind, React Router (đã tạo)
- [x] AuthContext, CartContext (đã tạo)
- [x] Header, Footer, Loading, EmptyState, ErrorMessage (đã tạo)
- [x] ProtectedRoute (đã tạo)
- [x] Dockerfile backend, frontend (đã tạo)
- [x] docker-compose.yml (đã tạo)
- [x] GitHub Actions CI workflow (đã tạo)
- [x] Script backup/restore database (đã tạo)
- [ ] Kiểm thử luồng đăng nhập/đăng ký với API thật
- [ ] Kiểm thử ProtectedRoute: không cho vào nếu chưa đăng nhập
- [ ] Kiểm thử Admin route: không cho CUSTOMER vào /admin
- [ ] Đảm bảo frontend responsive trên mobile
- [ ] Test Docker Compose: chạy toàn bộ hệ thống, kiểm tra log
- [ ] Viết `docs/TEST-PLAN.md` với các test case cơ bản

#### Sản phẩm bàn giao
- Toàn bộ hạ tầng Docker chạy được
- CI/CD GitHub Actions hoạt động
- Frontend tích hợp auth hoàn chỉnh
- Test plan và kết quả kiểm thử

#### Tiêu chí hoàn thành
- [ ] `docker compose up -d` → cả 3 service chạy, không lỗi
- [ ] Truy cập `http://localhost:3000` → hiển thị trang chủ
- [ ] Đăng nhập → token được lưu, header hiển thị tên user
- [ ] Đăng xuất → redirect về trang chủ, token bị xóa
- [ ] Truy cập `/admin` khi chưa đăng nhập → redirect `/login`
- [ ] Truy cập `/admin` với tài khoản CUSTOMER → redirect `/`
- [ ] CI pipeline pass khi push code lên GitHub

#### Phối hợp
- **Cao Chí Thiện**: cấu hình CORS cho phép frontend gọi backend
- Phối hợp toàn nhóm kiểm tra tích hợp cuối dự án

---

## File chung — Team Lead quản lý

Các file sau **không được sửa tùy tiện** mà không thống nhất với Team Lead:

| File | Lý do |
|------|-------|
| `backend/build.gradle` | Ảnh hưởng toàn bộ dependencies |
| `backend/src/.../config/SecurityConfig.java` | Ảnh hưởng phân quyền toàn hệ thống |
| `backend/src/.../exception/GlobalExceptionHandler.java` | Format lỗi chung |
| `docker-compose.yml` | Cấu hình môi trường |
| `.env.example` | Template biến môi trường |
| `docs/API.md` | Contract API chung |

---

## Thứ tự tích hợp

```
Tuần 1:
1. Cao Chí Thiện   → Hoàn thiện Auth API (register, login, profile)
2. Đỗ Hoàng Anh  → Test Docker Compose, fix CORS
3. Đỗ Đức Mạnh   → CRUD sản phẩm/danh mục

Tuần 2:
4. Vũ Đức Huy Hoàng → OrderService, CartCheckout flow
5. Đặng Thanh Bình   → InventoryService, PaymentService

Tuần 3:
6. Đỗ Hoàng Anh  → Tích hợp frontend với API thật (xóa mock data)
7. Toàn nhóm     → Integration test, fix bug, demo
```

---

## API Contract cần thống nhất trước khi code

Nhóm cần thống nhất các điểm sau **trước khi bắt đầu code**:

1. **Format JWT**: Payload chứa gì? (email, userId, role) → **Cao Chí Thiện quyết định**
2. **Order status transitions**: PENDING → CONFIRMED → PROCESSING → SHIPPING → COMPLETED/CANCELLED → **Cao Chí Thiện + Vũ Đức Huy Hoàng + Đặng Thanh Bình thống nhất**
3. **Inventory update**: Trừ kho trong OrderService hay InventoryService? → **Vũ Đức Huy Hoàng + Đặng Thanh Bình thống nhất**
4. **Cart lưu ở đâu?** Client (localStorage) hay Server (DB)? → Đã chọn client-side (CartContext)

---

## Checklist nghiệm thu

### Backend
- [ ] Tất cả API trả đúng HTTP status (không dùng 200 cho mọi thứ)
- [ ] Mật khẩu được BCrypt trước khi lưu DB
- [ ] Không trả password/passwordHash trong response
- [ ] CORS chỉ cho phép origin của frontend
- [ ] API admin được bảo vệ (401/403 nếu không đủ quyền)
- [ ] Tồn kho không bao giờ âm
- [ ] totalAmount tính ở backend, không tin frontend

### Frontend
- [ ] Responsive trên mobile (min 375px), tablet và desktop
- [ ] Loading spinner khi đang fetch API
- [ ] Hiển thị lỗi rõ ràng khi API fail
- [ ] Hiển thị EmptyState khi danh sách rỗng
- [ ] Token hết hạn → tự động redirect đến /login

### DevOps
- [ ] `docker compose up -d` không lỗi
- [ ] MySQL healthcheck pass trước khi backend khởi động
- [ ] Dữ liệu MySQL được lưu trong Docker volume (không mất khi restart)
- [ ] CI pipeline pass trên GitHub
