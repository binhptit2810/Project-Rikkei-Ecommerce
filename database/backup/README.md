# Hướng Dẫn Sao Lưu & Khôi Phục Cơ Sở Dữ Liệu Dự Phòng (Defense Contingency Plan)

> **Người phụ trách:** Đặng Thanh Bình (DTB - Inventory & Payment Backend)  
> **Người phối hợp:** Cao Chí Thiện (CCT - VNPay Payment & Security)  
> **Dự án:** Rikkei E-Commerce Platform  
> **Tiêu chí nghiệm thu (DoD):**  
> - Đánh index tối ưu truy vấn (`coupon_code`, `product_id` trên `reviews`, `vnp_txn_ref` trên `payments`) đạt latency < 50ms.  
> - Script tự động sao lưu và khôi phục hoạt động ổn định, an toàn, hỗ trợ cả Windows PowerShell và Linux/macOS/Git Bash.

---

## 1. Tổng Quan Kiến Trúc & An Toàn Dữ Liệu

Hệ thống cung cấp bộ công cụ tự động hóa hoàn chỉnh đặt tại thư mục `scripts/`:
- `scripts/backup-db.sh` & `scripts/backup-db.ps1`: Script sao lưu tự động (hỗ trợ nén gzip, gán thẻ ngày bảo vệ, tự động dọn dẹp theo thời gian lưu trữ).
- `scripts/restore-db.sh` & `scripts/restore-db.ps1`: Script khôi phục khẩn cấp (kiểm tra an toàn, xác thực xác nhận người dùng, giải nén tự động, kiểm tra tính toàn vẹn sau khi nạp).
- Thư mục lưu trữ: `database/backup/` (Các file `.sql` và `.sql.gz` đã được đưa vào `.gitignore` để tránh rò rỉ dữ liệu nhạy cảm).

---

## 2. Hướng Dẫn Sử Dụng Chi Tiết

### 2.1. Sao Lưu Cơ Sở Dữ Liệu (Backup)

#### A. Tạo bản sao lưu mốc chuẩn cho Ngày Bảo Vệ (Khuyên dùng trước giờ G)
Bản sao lưu này sẽ được gắn nhãn `DEFENSE_BACKUP` và **không bao giờ bị xóa** bởi chính sách dọn dẹp định kỳ (retention):

- **Trên Windows (PowerShell):**
  ```powershell
  .\scripts\backup-db.ps1 -Defense
  ```
- **Trên Linux / macOS / Git Bash:**
  ```bash
  chmod +x ./scripts/*.sh
  ./scripts/backup-db.sh --defense
  ```

#### B. Sao lưu định kỳ (mặc định nén Gzip, lưu 7 ngày)
- **Windows (PowerShell):**
  ```powershell
  .\scripts\backup-db.ps1
  ```
- **Linux / Git Bash:**
  ```bash
  ./scripts/backup-db.sh
  ```

#### C. Sao lưu không nén (file `.sql` thuần để mở xem/chỉnh sửa)
- **Windows (PowerShell):**
  ```powershell
  .\scripts\backup-db.ps1 -NoCompress
  ```
- **Linux / Git Bash:**
  ```bash
  ./scripts/backup-db.sh --no-compress
  ```

---

### 2.2. Khôi Phục Cơ Sở Dữ Liệu (Restore)

Khi cần khôi phục lại trạng thái CSDL ban đầu (ví dụ: sau khi demo đặt hàng, hủy đơn, thanh toán test):

#### A. Khôi phục từ bản backup mới nhất (Interactive - có xác nhận YES):
- **Windows (PowerShell):**
  ```powershell
  .\scripts\restore-db.ps1
  ```
- **Linux / Git Bash:**
  ```bash
  ./scripts/restore-db.sh
  ```

#### B. Khôi phục từ một file chỉ định (ví dụ file chuẩn bảo vệ):
- **Windows (PowerShell):**
  ```powershell
  .\scripts\restore-db.ps1 -BackupFile "database\backup\ecommerce_db_DEFENSE_BACKUP_xxxxxx.sql.gz"
  ```
- **Linux / Git Bash:**
  ```bash
  ./scripts/restore-db.sh database/backup/ecommerce_db_DEFENSE_BACKUP_xxxxxx.sql.gz
  ```

#### C. Khôi phục tự động không cần hỏi (Phục vụ CI/CD hoặc Script tự động):
- **Windows (PowerShell):**
  ```powershell
  .\scripts\restore-db.ps1 -Force
  ```
- **Linux / Git Bash:**
  ```bash
  ./scripts/restore-db.sh --yes
  ```

---

## 3. Kịch Bản Dự Phòng Xử Lý Sự Cố Trong Buổi Bảo Vệ (Defense Day Playbook)

> [!IMPORTANT]
> Trong buổi bảo vệ đồ án, nếu xảy ra tình huống: dữ liệu demo bị sai lệch, đơn hàng thử nghiệm làm hết hàng trong kho, hoặc cần đưa hệ thống về trạng thái sạch ban đầu để ban giám khảo chấm tiếp:

1. **Bước 1:** Mở terminal tại thư mục gốc dự án `Project-Rikkei-Ecommerce-develop`.
2. **Bước 2:** Chạy lệnh khôi phục nhanh:
   ```powershell
   .\scripts\restore-db.ps1 -Force
   ```
3. **Bước 3:** Script sẽ tự động:
   - Nạp lại toàn bộ danh mục, sản phẩm, tồn kho mẫu, mã giảm giá và tài khoản admin/user chuẩn.
   - In bảng kiểm tra tổng số lượng bản ghi các bảng chính (`products`, `users`, `orders`, `reviews`, `payments`) ngay trên màn hình.
4. **Bước 4:** Xóa cache Redis nếu cần đồng bộ mới nhất:
   ```bash
   docker exec -it ecommerce-redis redis-cli FLUSHALL
   ```
5. **Thời gian phục hồi:** Toàn bộ quá trình chỉ mất **dưới 5 giây**!

---

## 4. Tối Ưu Hóa Hiệu Năng & Đánh Index (DoD < 50ms)

Theo yêu cầu kỹ thuật và DoD, các index sau đã được cấu hình trong `database/migration_add_performance_indexes.sql` và JPA Entities:

1. **`idx_orders_coupon_code` (bảng `orders`):**
   - Tối ưu tra cứu lịch sử đơn hàng theo mã voucher và tính toán giới hạn sử dụng mã.
2. **`idx_coupons_code` (bảng `coupons`):**
   - Đảm bảo kiểm tra và áp dụng mã giảm giá tại Giỏ hàng / Checkout với thời gian phản hồi < 5ms.
3. **`idx_reviews_product` & `idx_reviews_product_created` (bảng `reviews`):**
   - Tối ưu tải danh sách đánh giá sản phẩm có phân trang và sắp xếp theo ngày tạo (`ORDER BY created_at DESC`).
   - Tối ưu tính rating trung bình `AVG(rating)` không cần quét toàn bộ bảng (Table Scan).
4. **`idx_payments_vnp_txn_ref` & `idx_payment_tx_txn_ref` (bảng `payments` & `payment_transactions`):**
   - Tối ưu xử lý webhook IPN VNPay dưới 10ms, chống trùng lặp giao dịch (Idempotency) và xử lý đối soát.
