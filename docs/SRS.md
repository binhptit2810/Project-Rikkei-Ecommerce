# 📄 ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)
## HỆ THỐNG THƯƠNG MẠI ĐIỆN TỬ (E-COMMERCE PLATFORM)

---

### Thông tin dự án
- **Tên dự án:** Hệ thống thương mại điện tử (E-commerce Platform)
- **Đối tượng sử dụng:** Nhóm sinh viên thực hiện đồ án môn học công nghệ thông tin
- **Quy mô:** Đồ án tốt nghiệp / Đồ án môn học Web Full-stack
- **Kiến trúc đề xuất:** Client-Server Monolith (React Frontend + Spring Boot REST API + MySQL Database)

---

## 1. Giới thiệu tổng quan (Introduction)

### 1.1. Mục tiêu dự án
Xây dựng một hệ thống website thương mại điện tử trực tuyến (B2C) cơ bản nhưng hoàn chỉnh về quy trình: từ xem danh mục, tìm kiếm sản phẩm, quản lý giỏ hàng, đặt hàng, đến quản lý đơn hàng và tồn kho. Hệ thống đóng vai trò làm nền tảng (Base Project) chuẩn mực để nhóm sinh viên cùng tham gia phát triển, chia sẻ nhánh Git và tích hợp code.

### 1.2. Phạm vi hệ thống (Scope)
- **Phía người dùng (Customer / Guest):**
  - Xem danh sách sản phẩm, chi tiết sản phẩm, lọc theo danh mục, tìm kiếm từ khóa.
  - Đăng ký, đăng nhập tài khoản khách hàng, quản lý hồ sơ cá nhân.
  - Quản lý giỏ hàng (thêm, sửa số lượng, xóa sản phẩm).
  - Đặt hàng (nhập địa chỉ, số điện thoại, ghi chú) và chọn phương thức thanh toán (COD hoặc mô phỏng).
  - Xem lịch sử các đơn hàng và trạng thái từng đơn hàng.
- **Phía quản trị (Admin):**
  - Quản lý danh mục sản phẩm (thêm, sửa, ẩn/hiện danh mục).
  - Quản lý sản phẩm (thêm mới, cập nhật giá, mô tả, tồn kho, hình ảnh).
  - Quản lý đơn hàng (xác nhận đơn, chuyển trạng thái giao hàng, hủy đơn).
  - Quản lý tồn kho và lịch sử xuất/nhập kho.

---

## 2. Các tác nhân và phân quyền (Actors & Roles)

| Tác nhân | Mô tả vai trò | Quyền hạn chính |
|----------|---------------|----------------|
| **Khách vãng lai (Guest)** | Người dùng chưa đăng nhập | Xem danh sách sản phẩm, xem chi tiết, tìm kiếm, đăng ký tài khoản, đăng nhập |
| **Khách hàng (Customer)** | Người mua hàng có tài khoản | Toàn bộ quyền Guest + Thêm giỏ hàng, đặt hàng, xem lịch sử đơn, chỉnh sửa thông tin cá nhân |
| **Quản trị viên (Admin)** | Chủ cửa hàng / Người vận hành hệ thống | Quản lý danh mục, sản phẩm, kho hàng, duyệt và cập nhật trạng thái đơn hàng |

---

## 3. Yêu cầu chức năng chi tiết (Functional Requirements)

Hệ thống được chia thành 5 module tương ứng với sự phân công độc lập cho 5 sinh viên:

### Module 1: Quản lý Tài khoản & Xác thực (Auth & User Profile)
- **FR-1.1 Đăng ký:** Cho phép khách hàng đăng ký tài khoản bằng email, mật khẩu, họ tên, số điện thoại. Kiểm tra email không trùng lặp, mã hóa mật khẩu bằng BCrypt.
- **FR-1.2 Đăng nhập:** Xác thực email/mật khẩu, cấp JWT token (hoặc session) hợp lệ.
- **FR-1.3 Quản lý hồ sơ:** Người dùng có thể xem và cập nhật thông tin cá nhân (họ tên, số điện thoại, địa chỉ mặc định).
- **FR-1.4 Đổi mật khẩu:** Yêu cầu nhập mật khẩu cũ và xác nhận mật khẩu mới.

### Module 2: Quản lý Danh mục & Sản phẩm (Category & Product)
- **FR-2.1 Danh mục sản phẩm:** Hiển thị danh sách danh mục; Admin có thể thêm, sửa, xóa (hoặc ẩn) danh mục.
- **FR-2.2 Danh sách sản phẩm:** Phân trang danh sách sản phẩm, lọc theo danh mục, sắp xếp theo giá (tăng/giảm) và thời gian mới nhất.
- **FR-2.3 Tìm kiếm:** Tìm kiếm sản phẩm theo tên hoặc từ khóa mô tả.
- **FR-2.4 Chi tiết sản phẩm:** Hiển thị thông tin chi tiết, hình ảnh, giá bán, số lượng tồn kho và mô tả sản phẩm.
- **FR-2.5 Quản trị sản phẩm (Admin):** Thêm sản phẩm mới, cập nhật giá bán, thông tin chi tiết và trạng thái kinh doanh (active/inactive).

### Module 3: Giỏ hàng, Đặt hàng & Thanh toán (Cart & Orders)
- **FR-3.1 Giỏ hàng:** Khách hàng có thể thêm sản phẩm vào giỏ, cập nhật số lượng, xóa từng mục hoặc làm trống giỏ hàng.
- **FR-3.2 Kiểm tra tồn kho trước khi đặt:** Hệ thống kiểm tra số lượng tồn kho thực tế trước khi cho phép tạo đơn.
- **FR-3.3 Tạo đơn hàng (Checkout):** Người dùng nhập thông tin người nhận, địa chỉ giao hàng, số điện thoại và ghi chú đơn hàng.
- **FR-3.4 Thanh toán:** Hỗ trợ phương thức thanh toán khi nhận hàng (COD) hoặc mô phỏng thanh toán thành công (Simulated Gateway).
- **FR-3.5 Lịch sử đơn hàng:** Khách hàng theo dõi danh sách đơn hàng đã mua và trạng thái chi tiết của từng đơn (PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED).

### Module 4: Quản trị Đơn hàng & Kho hàng (Admin Order & Inventory)
- **FR-4.1 Danh sách đơn hàng Admin:** Xem toàn bộ đơn hàng trong hệ thống, lọc theo trạng thái đơn hàng và ngày tạo.
- **FR-4.2 Cập nhật trạng thái đơn:** Admin duyệt đơn (PENDING -> CONFIRMED), chuyển sang giao hàng (SHIPPING), hoàn tất (COMPLETED) hoặc hủy đơn (CANCELLED).
- **FR-4.3 Quản lý tồn kho:** Cập nhật số lượng sản phẩm nhập kho; tự động trừ tồn kho khi đơn hàng được đặt và hoàn kho nếu đơn bị hủy.
- **FR-4.4 Lịch sử biến động kho:** Lưu nhật ký giao dịch kho (bảng `inventory_transactions`) phục vụ đối soát.

### Module 5: Giao diện tổng thể, Tích hợp & DevOps (Frontend & DevOps)
- **FR-5.1 Giao diện người dùng:** Trang chủ hiển thị tiêu đề thương hiệu, banner, danh mục nổi bật, giao diện trực quan và responsive (tương thích máy tính & di động).
- **FR-5.2 Điều hướng (Routing):** Thiết lập cấu trúc đường dẫn rõ ràng giữa trang chủ, danh sách sản phẩm, giỏ hàng, thông tin cá nhân.
- **FR-5.3 Dockerization:** Cung cấp `Dockerfile` cho Backend, Frontend và `docker-compose.yml` để khởi chạy trọn gói hệ thống (MySQL + Backend + Frontend).
- **FR-5.4 CI Pipeline:** Tự động kiểm tra build mã nguồn qua GitHub Actions khi tạo Pull Request vào nhánh chính.

---

## 4. Yêu cầu phi chức năng (Non-Functional Requirements)

1. **Tính đơn giản & Dễ hiểu (Simplicity):**
   - Cấu trúc code rõ ràng, tuân thủ các quy ước tiêu chuẩn của Spring Boot và React.
   - Tránh việc áp dụng kiến trúc quá phức tạp không cần thiết (microservices, event streaming...) gây khó khăn cho sinh viên mới học.
2. **Hiệu năng & Phản hồi (Performance):**
   - API phản hồi trung bình < 300ms trong điều kiện môi trường local.
   - Frontend SPA phản hồi mượt mà, hạn chế việc reload toàn bộ trang.
3. **Bảo mật cơ bản (Security):**
   - Mật khẩu người dùng được băm an toàn bằng BCrypt trước khi lưu vào CSDL.
   - Dữ liệu nhạy cảm (mật khẩu CSDL, secret key) được cấu hình qua biến môi trường (`.env`), không commit lên repository.
4. **Tính nhất quán dữ liệu (Data Integrity):**
   - Sử dụng khóa ngoại (Foreign Keys) và ràng buộc (Constraints) trong CSDL MySQL để đảm bảo dữ liệu đơn hàng và chi tiết đơn hàng luôn hợp lệ.
   - Đảm bảo tính toàn vẹn giao dịch (Transaction) khi đặt hàng và trừ tồn kho.

---

## 5. Mô hình cơ sở dữ liệu quan hệ (Relational Schema)

Hệ thống sử dụng CSDL MySQL với các bảng chính:
- **`users`**: id, email, password, full_name, phone, address, role (CUSTOMER, ADMIN), active, created_at
- **`categories`**: id, name, description, image_url, active, created_at
- **`products`**: id, name, description, price, image_url, category_id, stock_quantity, active, created_at
- **`orders`**: id, order_code, user_id, recipient_name, recipient_phone, shipping_address, total_amount, order_status, payment_status, created_at
- **`order_items`**: id, order_id, product_id, product_name, unit_price, quantity, subtotal
- **`payments`**: id, order_id, payment_method, status, amount, transaction_note, paid_at
- **`inventory_transactions`**: id, product_id, change_quantity, stock_after, transaction_type, order_id, created_at

---

## 6. Quy trình phối hợp & Đánh giá đồ án

- Mỗi thành viên tạo nhánh tính năng riêng: `feature/<ten-module>` từ nhánh `dev`.
- Tạo Pull Request và được ít nhất 1 thành viên khác review trước khi merge.
- Báo cáo tiến độ hàng tuần theo bảng phân công trong tài liệu `docs/TEAM-WORK.md`.
